#!/usr/bin/env python3
"""Capture Splash / Connect HotFox / AUTO (01–03) from the real debug APK."""
from __future__ import annotations

import hashlib
import json
import subprocess
import sys
import time
from io import BytesIO
from pathlib import Path

import numpy as np
from PIL import Image, ImageChops

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "verification" / "ui" / "onboarding_01_03_20260914"
REF = ROOT / "design" / "hotfox_onboarding_01_03_20260914" / "reference_triptych.png"
PKG = "com.hotfox.vpn"
HARNESS = f"{PKG}/com.v2ray.ang.ui.HotfoxUiScreenshotHarnessActivity"
SERIAL = "emulator-5554"
VIEWPORT = (1032, 2231)
DENSITY = 420
FONT_SCALE = "1.0"
# Prompt crop of the 1448×1086 triptych (labels/gutters excluded).
CROPS = {
    "01": (24, 50, 474, 1050),
    "02": (499, 50, 948, 1050),
    "03": (974, 50, 1424, 1050),
}
SCENARIOS = {
    "01": ("01_SPLASH", "HotfoxSplashActivity"),
    "02": ("02_ONBOARD_CONNECT", "HotfoxOnboardingActivity"),
    "03": ("03_ONBOARD_AUTO", "HotfoxOnboardingActivity"),
}
NEEDLES = {
    "01": ("HotFox", "БОЛЬШЕ, ЧЕМ VPN"),
    "02": ("Подключите", "У меня уже есть подписка"),
    "03": ("Лучший сервер", "Использовать AUTO"),
}


def adb(*args: str, timeout: int = 90) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        ["adb", "-s", SERIAL, *args],
        check=False,
        text=True,
        capture_output=True,
        timeout=timeout,
    )


def has_anr() -> bool:
    try:
        out = adb("shell", "dumpsys", "window", timeout=30).stdout
    except subprocess.TimeoutExpired:
        return False
    return any(
        token in out
        for token in (
            "Application Not Responding:",
            "AppErrorDialog",
            "aerr_application",
            "aerr_close",
        )
    )


def dismiss_anr() -> None:
    if not has_anr():
        return
    # Wait button on the TCG System UI ANR dialog (1080-class portrait).
    for x, y in ((516, 1306), (300, 1320), (540, 1400), (700, 1260)):
        adb("shell", "input", "tap", str(x), str(y))
        time.sleep(0.4)
    time.sleep(1.0)


def unlock() -> None:
    adb("shell", "input", "keyevent", "224")
    adb("shell", "wm", "dismiss-keyguard")
    adb("shell", "settings", "put", "secure", "lockscreen.disabled", "1")
    adb("shell", "locksettings", "set-disabled", "true")
    adb("shell", "input", "swipe", "540", "1800", "540", "400", "200")


def resumed_activity() -> str:
    try:
        out = adb("shell", "dumpsys", "activity", "activities", timeout=90).stdout
    except subprocess.TimeoutExpired:
        return ""
    for line in out.splitlines():
        if "topResumedActivity=" in line or "ResumedActivity:" in line or "mResumedActivity:" in line:
            return line.strip()
    return out[-400:] if out else ""


def wait_activity(token: str, timeout: float = 180.0) -> str:
    deadline = time.time() + timeout
    last = ""
    while time.time() < deadline:
        dismiss_anr()
        last = resumed_activity()
        if token in last:
            time.sleep(1.2)
            return last
        time.sleep(1.5)
    raise RuntimeError(f"{token} not resumed: {last[-200:]}")


def screencap_bytes() -> bytes:
    raw = subprocess.run(
        ["adb", "-s", SERIAL, "exec-out", "screencap", "-p"],
        check=False,
        capture_output=True,
        timeout=80,
    )
    if raw.returncode != 0 or len(raw.stdout) < 40_000:
        raise RuntimeError(f"screencap failed rc={raw.returncode} bytes={len(raw.stdout)}")
    return raw.stdout


def frame_ok(screen_id: str, png: bytes) -> tuple[bool, str]:
    image = Image.open(BytesIO(png)).convert("RGB")
    pixels = np.asarray(image)
    mean = float(pixels.mean())
    std = float(pixels.std())
    r, g, b = pixels[:, :, 0], pixels[:, :, 1], pixels[:, :, 2]
    orange = float(((r > 180) & (g > 70) & (g < 190) & (b < 130)).mean())
    cream = float(((r > 200) & (g > 200) & (b > 200)).mean())
    top_mean = float(pixels[:240].mean())
    ok = mean >= 8 and std >= 8 and orange >= 0.0004 and cream < 0.35 and top_mean < 90
    return ok, f"mean={mean:.1f} std={std:.1f} orange={orange:.5f} cream={cream:.5f} top={top_mean:.1f} {image.size}"


def launch(scenario: str) -> None:
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
        scenario,
    )


def capture_one(screen_id: str) -> Path:
    dest = OUT / "actual" / f"{screen_id}.png"
    dest.parent.mkdir(parents=True, exist_ok=True)
    last_err = "unknown"
    for _ in range(14):
        dismiss_anr()
        unlock()
        try:
            png = screencap_bytes()
        except Exception as exc:  # noqa: BLE001
            last_err = str(exc)
            time.sleep(2.0)
            continue
        ok, stats = frame_ok(screen_id, png)
        dest.write_bytes(png)
        if ok and not has_anr():
            print(f"CAPTURED {screen_id} bytes={dest.stat().st_size} {stats}", flush=True)
            return dest
        last_err = stats if ok else stats
        if has_anr():
            last_err = f"ANR dialog visible ({stats})"
        time.sleep(2.5)
    raise RuntimeError(f"{screen_id} invalid frame: {last_err}")


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def crop_refs() -> dict[str, Path]:
    triptych = Image.open(REF).convert("RGB")
    out = {}
    dest_dir = OUT / "reference_crop"
    dest_dir.mkdir(parents=True, exist_ok=True)
    for sid, box in CROPS.items():
        crop = triptych.crop(box)
        path = dest_dir / f"{sid}.png"
        crop.save(path)
        out[sid] = path
    return out


def compare(screen_id: str, actual: Path, ref: Path) -> dict:
    act = Image.open(actual).convert("RGB")
    ref_im = Image.open(ref).convert("RGB")
    ref_r = ref_im.resize(act.size, Image.Resampling.LANCZOS)
    diff = ImageChops.difference(ref_r, act)
    arr = np.asarray(diff)
    max_delta = int(arr.max())
    changed = int(np.any(arr > 0, axis=2).sum())
    overlay = Image.blend(ref_r, act, 0.5)
    side = Image.new("RGB", (act.width * 2 + 16, act.height), (12, 10, 16))
    side.paste(ref_r, (0, 0))
    side.paste(act, (act.width + 16, 0))
    cmp = OUT / "comparison"
    cmp.mkdir(parents=True, exist_ok=True)
    diff.save(cmp / f"diff_{screen_id}.png")
    overlay.save(cmp / f"overlay_{screen_id}.png")
    side.save(cmp / f"sbs_{screen_id}.png")
    return {
        "screen": screen_id,
        "actual": str(actual.relative_to(ROOT)),
        "reference_crop": str(ref.relative_to(ROOT)),
        "actual_sha256": sha256(actual),
        "viewport": f"{act.width}x{act.height}",
        "density": DENSITY,
        "fontScale": FONT_SCALE,
        "changed_pixels": changed,
        "max_channel_delta": max_delta,
        "pixel_pass": changed == 0 and max_delta == 0,
    }


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
    unlock()
    refs = crop_refs()
    results = []
    paths = []
    for sid, (scenario, token) in SCENARIOS.items():
        launch(scenario)
        wait_activity(token)
        time.sleep(4.0)
        path = capture_one(sid)
        paths.append(path)
        results.append(compare(sid, path, refs[sid]))
    report = {
        "task_id": "ONBOARDING-01-03-20260914",
        "sha": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip(),
        "viewport": f"{VIEWPORT[0]}x{VIEWPORT[1]}",
        "density": DENSITY,
        "fontScale": FONT_SCALE,
        "locale": "ru-RU",
        "reference": str(REF.relative_to(ROOT)),
        "reference_crop": CROPS,
        "normalization": "LANCZOS resize of iOS-framed crop to Android actual; system bars kept; not EXACT",
        "screens": results,
    }
    (OUT / "capture_manifest.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
    print(json.dumps(report, ensure_ascii=False, indent=2), flush=True)
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:  # noqa: BLE001
        print(f"FAIL {exc}", file=sys.stderr)
        raise SystemExit(1)
