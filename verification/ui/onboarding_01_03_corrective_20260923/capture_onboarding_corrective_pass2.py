#!/usr/bin/env python3
"""Capture Splash / Connect / AUTO for onboarding corrective pass 2."""
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
OUT = ROOT / "verification" / "ui" / "onboarding_01_03_corrective_20260923"
REF = ROOT / "design" / "hotfox_onboarding_01_03_20260914" / "reference_triptych.png"
PKG = "com.hotfox.vpn"
HARNESS = f"{PKG}/com.v2ray.ang.ui.HotfoxUiScreenshotHarnessActivity"
SERIAL = "emulator-5554"
DENSITY = 420
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
PROFILES = {
    "standard": (1032, 2231),  # ~393x851 @2.625
    "compact": (945, 2100),    # ~360x800 @2.625
    "large": (1082, 2402),     # ~412x915 @2.625
}


def adb(*args: str, timeout: int = 90) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        ["adb", "-s", SERIAL, *args],
        check=False,
        text=True,
        capture_output=True,
        timeout=timeout,
    )


def dismiss_anr() -> None:
    adb("shell", "settings", "put", "global", "hide_error_dialogs", "1")
    for x, y in ((320, 1180), (516, 1306), (400, 1240), (540, 1400)):
        adb("shell", "input", "tap", str(x), str(y))


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


def frame_ok(png: bytes) -> tuple[bool, str]:
    image = Image.open(BytesIO(png)).convert("RGB")
    pixels = np.asarray(image)
    mean = float(pixels.mean())
    std = float(pixels.std())
    r, g, b = pixels[:, :, 0], pixels[:, :, 1], pixels[:, :, 2]
    orange = float(((r > 180) & (g > 70) & (g < 190) & (b < 130)).mean())
    cream = float(((r > 200) & (g > 200) & (b > 200)).mean())
    top_mean = float(pixels[:240].mean())
    ok = mean >= 8 and std >= 8 and orange >= 0.0004 and cream < 0.35 and top_mean < 110
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


def capture_named(name: str) -> Path:
    dest = OUT / "actual" / f"{name}.png"
    dest.parent.mkdir(parents=True, exist_ok=True)
    last_err = "unknown"
    for _ in range(16):
        dismiss_anr()
        unlock()
        try:
            png = screencap_bytes()
        except Exception as exc:  # noqa: BLE001
            last_err = str(exc)
            time.sleep(2.0)
            continue
        ok, stats = frame_ok(png)
        dest.write_bytes(png)
        if ok:
            print(f"CAPTURED {name} bytes={dest.stat().st_size} {stats}", flush=True)
            return dest
        last_err = stats
        time.sleep(2.5)
    raise RuntimeError(f"{name} invalid frame: {last_err}")


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def crop_refs() -> dict[str, Path]:
    triptych = Image.open(REF).convert("RGB")
    dest_dir = OUT / "reference_crop"
    dest_dir.mkdir(parents=True, exist_ok=True)
    out = {}
    for sid, box in CROPS.items():
        crop = triptych.crop(box)
        path = dest_dir / f"{sid}.png"
        crop.save(path)
        out[sid] = path
    return out


def compare(screen_id: str, actual: Path, ref: Path, suffix: str) -> dict:
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
    key = f"{screen_id}_{suffix}" if suffix != "standard" else screen_id
    diff.save(cmp / f"diff_{key}.png")
    overlay.save(cmp / f"overlay_{key}.png")
    side.save(cmp / f"sbs_{key}.png")
    return {
        "screen": screen_id,
        "profile": suffix,
        "actual": str(actual.relative_to(ROOT)),
        "actual_sha256": sha256(actual),
        "viewport": f"{act.width}x{act.height}",
        "changed_pixels": changed,
        "max_channel_delta": max_delta,
        "pixel_pass": changed == 0 and max_delta == 0,
    }


def apply_profile(name: str, font_scale: str) -> None:
    w, h = PROFILES[name]
    adb("shell", "wm", "size", f"{w}x{h}")
    adb("shell", "wm", "density", str(DENSITY))
    adb("shell", "settings", "put", "system", "font_scale", font_scale)
    time.sleep(1.5)


def main() -> int:
    OUT.mkdir(parents=True, exist_ok=True)
    adb("wait-for-device")
    adb("shell", "settings", "put", "global", "animator_duration_scale", "0")
    adb("shell", "settings", "put", "global", "transition_animation_scale", "0")
    adb("shell", "settings", "put", "global", "window_animation_scale", "0")
    adb("shell", "cmd", "locale", "set-app-locales", PKG, "--locales", "ru-RU")
    unlock()
    refs = crop_refs()
    results = []
    jobs = [
        ("01", "standard", "1.0"),
        ("02", "standard", "1.0"),
        ("03", "standard", "1.0"),
        ("03", "compact", "1.0"),
        ("03", "large", "1.0"),
        ("02", "standard", "1.15"),
        ("03", "standard", "1.15"),
        ("01", "compact", "1.0"),
        ("02", "compact", "1.0"),
        ("05", "standard", "1.0"),
    ]
    extra = {
        "05": ("05_DISCONNECTED", "MainActivity"),
    }
    last_profile = None
    last_font = None
    for sid, profile, font in jobs:
        if (profile, font) != (last_profile, last_font):
            apply_profile(profile, font)
            last_profile, last_font = profile, font
        scenario, token = extra.get(sid, SCENARIOS[sid])
        launch(scenario)
        wait_activity(token)
        time.sleep(5.0 if sid == "01" else 3.5)
        suffix = profile if font == "1.0" else f"{profile}_fs{font.replace('.', '')}"
        name = f"{sid}_{suffix}"
        path = capture_named(name)
        if sid in refs:
            results.append(compare(sid, path, refs[sid], suffix))
        else:
            results.append({
                "screen": sid,
                "profile": suffix,
                "actual": str(path.relative_to(ROOT)),
                "actual_sha256": sha256(path),
                "viewport": f"{Image.open(path).size[0]}x{Image.open(path).size[1]}",
                "note": "Home V13 regression smoke; no onboarding reference crop",
            })
    report = {
        "task_id": "ONBOARDING-01-03-CORRECTIVE-20260923",
        "sha": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip(),
        "density": DENSITY,
        "locale": "ru-RU",
        "reference": str(REF.relative_to(ROOT)),
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
