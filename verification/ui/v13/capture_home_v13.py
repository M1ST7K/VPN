#!/usr/bin/env python3
"""Capture V13 Home Disconnected/Connecting/Connected and compare to approved refs."""
from __future__ import annotations

import hashlib
import json
import subprocess
import time
from io import BytesIO
from pathlib import Path

import numpy as np
from PIL import Image, ImageChops

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "verification" / "ui" / "v13"
REF_DIR = ROOT / "design" / "hotfox_home_v13" / "design" / "references"
PKG = "com.hotfox.vpn"
HARNESS = f"{PKG}/com.v2ray.ang.ui.HotfoxUiScreenshotHarnessActivity"
CHROME_RECEIVER = f"{PKG}/com.v2ray.ang.ui.HotfoxUiScreenshotChromeReceiver"
SERIAL = "emulator-5554"
VIEWPORT = (1032, 2231)
DENSITY = 420
FONT_SCALE = "1.0"

NEEDLES = {
    "disconnected": ("Не защищено", "Подключить"),
    "connecting": ("Подключаем", "Отменить"),
    "connected": ("Вы защищены", "Отключить"),
}
REFS = {
    "disconnected": REF_DIR / "hotfox-01-disconnected-v2.png",
    "connecting": REF_DIR / "hotfox-02-connecting-v2.png",
    "connected": REF_DIR / "hotfox-03-connected-v2.png",
}


def adb(*args: str, timeout: int = 60) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        ["adb", "-s", SERIAL, *args],
        check=False,
        text=True,
        capture_output=True,
        timeout=timeout,
    )


def dismiss_anr() -> None:
    try:
        out = adb("shell", "dumpsys", "window", timeout=20).stdout
    except subprocess.TimeoutExpired:
        return
    if "Application Not Responding:" not in out:
        return
    adb("shell", "input", "tap", "300", "1320")
    time.sleep(1.2)


def resumed_activity() -> str:
    try:
        out = adb("shell", "dumpsys", "activity", "activities", timeout=45).stdout
    except subprocess.TimeoutExpired:
        return ""
    for line in out.splitlines():
        if "topResumedActivity=" in line:
            return line.strip()
    return ""


def wait_main(timeout: float = 90.0) -> str:
    deadline = time.time() + timeout
    last = ""
    while time.time() < deadline:
        dismiss_anr()
        last = resumed_activity()
        if "MainActivity" in last:
            time.sleep(1.0)
            return last
        time.sleep(1.5)
    raise RuntimeError(f"MainActivity not resumed: {last[-200:]}")


def screencap_bytes() -> bytes:
    raw = subprocess.run(
        ["adb", "-s", SERIAL, "exec-out", "screencap", "-p"],
        check=False,
        capture_output=True,
        timeout=40,
    )
    if raw.returncode != 0 or len(raw.stdout) < 40_000:
        raise RuntimeError(f"screencap failed rc={raw.returncode} bytes={len(raw.stdout)}")
    return raw.stdout


def frame_ok(png: bytes) -> tuple[bool, str]:
    image = Image.open(BytesIO(png)).convert("RGB")
    pixels = np.asarray(image)
    mean = float(pixels.mean())
    std = float(pixels.std())
    r, g, b = pixels[:, :, 0], pixels[:, :, 1], pixels[:, :, 2]
    orange = float(((r > 180) & (g > 70) & (g < 190) & (b < 130)).mean())
    cream = float(((r > 200) & (g > 200) & (b > 200)).mean())
    top_mean = float(pixels[:240].mean())
    ok = mean >= 12 and std >= 12 and orange >= 0.0008 and cream < 0.25 and top_mean < 80
    return ok, f"mean={mean:.1f} std={std:.1f} orange={orange:.5f} cream={cream:.5f} top={top_mean:.1f} {image.size}"


def start_disconnected() -> None:
    adb("shell", "am", "force-stop", PKG)
    time.sleep(1.0)
    adb(
        "shell",
        "am",
        "start",
        "-n",
        HARNESS,
        "-a",
        "com.hotfox.vpn.action.UI_SCREENSHOT",
        "--es",
        "scenario",
        "05_DISCONNECTED",
    )


def set_chrome(chrome: str) -> None:
    adb(
        "shell",
        "am",
        "broadcast",
        "-n",
        CHROME_RECEIVER,
        "-a",
        "com.hotfox.vpn.action.UI_SCREENSHOT_CHROME",
        "--es",
        "chrome",
        chrome,
    )


def capture_one(screen_id: str) -> Path:
    dest = OUT / f"actual_{screen_id}.png"
    last_err = "unknown"
    for _ in range(12):
        dismiss_anr()
        try:
            png = screencap_bytes()
        except Exception as exc:  # noqa: BLE001
            last_err = str(exc)
            time.sleep(2.0)
            continue
        ok, stats = frame_ok(png)
        dest.write_bytes(png)
        if ok:
            print(f"CAPTURED {screen_id} bytes={dest.stat().st_size} {stats}", flush=True)
            return dest
        last_err = stats
        time.sleep(2.5)
    raise RuntimeError(f"{screen_id} invalid frame: {last_err}")


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def compare(state: str, actual: Path) -> dict:
    ref = Image.open(REFS[state]).convert("RGB")
    act = Image.open(actual).convert("RGB")
    ref_r = ref.resize(act.size, Image.Resampling.LANCZOS)
    diff = ImageChops.difference(ref_r, act)
    arr = np.asarray(diff)
    max_delta = int(arr.max())
    changed = int(np.any(arr > 0, axis=2).sum())
    overlay = Image.blend(ref_r, act, 0.5)
    side = Image.new("RGB", (act.width * 2 + 16, act.height), (12, 10, 16))
    side.paste(ref_r, (0, 0))
    side.paste(act, (act.width + 16, 0))
    diff.save(OUT / f"diff_{state}.png")
    overlay.save(OUT / f"overlay_{state}.png")
    side.save(OUT / f"sbs_{state}.png")
    return {
        "state": state,
        "actual": str(actual.relative_to(ROOT)),
        "reference": str(REFS[state].relative_to(ROOT)),
        "actual_sha256": sha256(actual),
        "viewport": f"{act.width}x{act.height}",
        "density": DENSITY,
        "fontScale": FONT_SCALE,
        "changed_pixels": changed,
        "max_channel_delta": max_delta,
        "pixel_pass": changed == 0 and max_delta == 0,
    }


def side_by_side_states(paths: list[Path]) -> Path:
    images = [Image.open(p).convert("RGB") for p in paths]
    h = max(im.height for im in images)
    scaled = []
    for im in images:
        if im.height != h:
            w = int(im.width * h / im.height)
            im = im.resize((w, h), Image.Resampling.LANCZOS)
        scaled.append(im)
    gap = 16
    total_w = sum(im.width for im in scaled) + gap * (len(scaled) - 1)
    canvas = Image.new("RGB", (total_w, h), (12, 10, 16))
    x = 0
    for im in scaled:
        canvas.paste(im, (x, 0))
        x += im.width + gap
    dest = OUT / "home_v13_sbs.png"
    canvas.save(dest)
    return dest


def main() -> int:
    OUT.mkdir(parents=True, exist_ok=True)
    adb("wait-for-device")
    adb("shell", "wm", "size", f"{VIEWPORT[0]}x{VIEWPORT[1]}")
    adb("shell", "wm", "density", str(DENSITY))
    adb("shell", "settings", "put", "system", "font_scale", FONT_SCALE)
    adb("shell", "settings", "put", "global", "animator_duration_scale", "0")
    adb("shell", "settings", "put", "global", "transition_animation_scale", "0")
    adb("shell", "settings", "put", "global", "window_animation_scale", "0")
    adb("shell", "cmd", "locale", "set-app-locales", PKG, "--locales", "ru-RU")
    start_disconnected()
    wait_main()
    time.sleep(8)
    paths = []
    paths.append(capture_one("disconnected"))
    set_chrome("CONNECTING")
    time.sleep(3)
    paths.append(capture_one("connecting"))
    set_chrome("CONNECTED")
    time.sleep(3)
    paths.append(capture_one("connected"))
    sbs = side_by_side_states(paths)
    report = {
        "sha": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip(),
        "viewport": f"{VIEWPORT[0]}x{VIEWPORT[1]}",
        "density": DENSITY,
        "fontScale": FONT_SCALE,
        "locale": "ru-RU",
        "normalization": "fitStart scene; real system bars kept; refs resized with LANCZOS to actual viewport for diff only",
        "contact_sheet": str(sbs.relative_to(ROOT)),
        "states": [compare(name, path) for name, path in zip(NEEDLES, paths, strict=True)],
    }
    (OUT / "REPORT.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
    print(json.dumps(report, ensure_ascii=False, indent=2), flush=True)
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:  # noqa: BLE001
        print(f"FAIL {exc}", file=sys.stderr)
        raise SystemExit(1)
