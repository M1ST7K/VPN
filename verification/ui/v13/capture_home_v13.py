#!/usr/bin/env python3
"""Capture V13 Home Disconnected/Connecting/Connected and compare to approved refs."""
from __future__ import annotations

import hashlib
import json
import re
import subprocess
import sys
import time
from io import BytesIO
from pathlib import Path

import numpy as np
from PIL import Image, ImageChops, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
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


def dismiss_anr() -> bool:
    dump = Path("/tmp/hotfox_uidump.xml")
    adb("shell", "uiautomator", "dump", "/sdcard/uidump.xml", timeout=25)
    pulled = adb("pull", "/sdcard/uidump.xml", str(dump), timeout=20)
    if pulled.returncode != 0 or not dump.is_file():
        return False
    xml = dump.read_text(encoding="utf-8", errors="ignore")
    if "isn't responding" not in xml and "не отвечает" not in xml.lower():
        return False
    match = re.search(r'text="Wait"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml)
    if match is None:
        match = re.search(r'text="Подождите"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml)
    if match is None:
        return False
    x1, y1, x2, y2 = (int(g) for g in match.groups())
    adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
    time.sleep(1.2)
    return True


def ui_text() -> str:
    try:
        adb("shell", "uiautomator", "dump", "/sdcard/uidump.xml", timeout=60)
        adb("pull", "/sdcard/uidump.xml", "/tmp/hotfox_uidump.xml", timeout=20)
    except subprocess.TimeoutExpired:
        return ""
    path = Path("/tmp/hotfox_uidump.xml")
    if not path.is_file():
        return ""
    return path.read_text(encoding="utf-8", errors="ignore")


def screencap(dest: Path) -> tuple[float, float]:
    raw = subprocess.run(
        ["adb", "-s", SERIAL, "exec-out", "screencap", "-p"],
        check=False,
        capture_output=True,
        timeout=40,
    )
    if raw.returncode != 0 or len(raw.stdout) < 10_000:
        raise RuntimeError(f"screencap failed rc={raw.returncode} bytes={len(raw.stdout)}")
    dest.write_bytes(raw.stdout)
    image = Image.open(BytesIO(raw.stdout)).convert("RGB")
    pixels = np.asarray(image)
    return float(pixels.mean()), float(pixels.std())


def wait_needles(screen_id: str, timeout: float = 90.0) -> str:
    needles = NEEDLES[screen_id]
    deadline = time.time() + timeout
    last = ""
    while time.time() < deadline:
        dismiss_anr()
        last = ui_text()
        if all(n in last for n in needles) and "isn't responding" not in last:
            return last
        time.sleep(2.0)
    raise RuntimeError(f"{screen_id} missing {needles}: {last[-800:]}")


def start_disconnected() -> None:
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
    wait_needles(screen_id)
    dest = OUT / f"actual_{screen_id}.png"
    last_err = "unknown"
    for _ in range(8):
        dismiss_anr()
        mean, std = screencap(dest)
        dump = ui_text()
        needles_ok = all(n in dump for n in NEEDLES[screen_id])
        anr = "isn't responding" in dump
        if mean > 10 and std > 6 and needles_ok and not anr:
            print(f"CAPTURED {screen_id} bytes={dest.stat().st_size} mean={mean:.1f}", flush=True)
            return dest
        last_err = f"mean={mean:.1f} std={std:.1f} needles={needles_ok} anr={anr}"
        time.sleep(3.0)
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
    time.sleep(8)
    paths = []
    paths.append(capture_one("disconnected"))
    set_chrome("CONNECTING")
    time.sleep(2)
    paths.append(capture_one("connecting"))
    set_chrome("CONNECTED")
    time.sleep(2)
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
