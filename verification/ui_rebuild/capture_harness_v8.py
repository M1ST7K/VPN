#!/usr/bin/env python3
"""Capture HotFox presentation states via the debug screenshot harness (V8, ru-RU)."""
from __future__ import annotations

import argparse
import subprocess
import sys
import time
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
PKG = "com.hotfox.vpn"
HARNESS = f"{PKG}/com.v2ray.ang.ui.HotfoxUiScreenshotHarnessActivity"
EXPECTED = {
    "01": "HotfoxSplashActivity",
    "02": "HotfoxOnboardingActivity",
    "03": "HotfoxOnboardingActivity",
    "04": "HotfoxOnboardingActivity",
    "05": "MainActivity",
    "06": "MainActivity",
    "07": "MainActivity",
    "08": "MainActivity",
    "09": "HotfoxHttpsImportActivity",
    "10": "MainActivity",
    "11": "HotfoxUiQaServerDetailsActivity",
    "12": "MainActivity",
    "13": "HotfoxSettingsActivity",
    "14": "HotfoxRoutingPrivacyActivity",
    "15": "PerAppProxyActivity",
    "16": "HotfoxAutopilotActivity",
    "17": "HotfoxShadowActivity",
    "18": "HotfoxAlwaysOnActivity",
}
SCENARIOS = [
    "01_SPLASH",
    "02_ONBOARD_CONNECT",
    "03_ONBOARD_AUTO",
    "04_ONBOARD_READY",
    "05_DISCONNECTED",
    "06_CONNECTING_VISUAL",
    "07_PROTECTED_VISUAL",
    "08_ADD_CONNECTION_SHEET",
    "09_HTTPS_SUBSCRIPTION",
    "10_SERVERS",
    "11_SERVER_DETAILS",
    "12_SUBSCRIPTION",
    "13_SETTINGS",
    "14_SMART_ROUTING",
    "15_APPS_RULES",
    "16_AUTOPILOT",
    "17_SHADOW",
    "18_ALWAYS_ON",
]
SMOKE_IDS = ("01", "02", "03", "04", "05", "06", "07", "09", "10", "13", "15", "17")
LOCALE_PROOF = ROOT / "verification" / "ui_rebuild" / "V8_LOCALE_PROOF.txt"


def adb(*args: str, check: bool = True, timeout: int | None = None) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        ["adb", *args],
        check=check,
        text=True,
        capture_output=True,
        timeout=timeout,
    )


def anr_window_count() -> int:
    try:
        out = adb("shell", "dumpsys", "window", check=False, timeout=20).stdout
    except subprocess.TimeoutExpired:
        return 0
    return out.count("Application Not Responding:")


def wait_for_device() -> None:
    adb("wait-for-device")
    for _ in range(90):
        boot = adb("shell", "getprop", "sys.boot_completed", check=False).stdout.strip()
        if boot == "1":
            return
        time.sleep(2)
    raise SystemExit("emulator did not boot")


def lock_ru_locale() -> None:
    adb("shell", "cmd", "locale", "set-app-locales", PKG, "--locales", "ru-RU", check=False)
    adb("shell", "am", "force-stop", PKG, check=False)
    time.sleep(0.6)
    app_locales = adb("shell", "cmd", "locale", "get-app-locales", PKG, check=False).stdout
    persist = adb("shell", "getprop", "persist.sys.locale", check=False).stdout
    LOCALE_PROOF.write_text(
        "cmd locale get-app-locales:\n"
        f"{app_locales.strip()}\n\n"
        "persist.sys.locale:\n"
        f"{persist.strip()}\n",
        encoding="utf-8",
    )
    print(f"locale lock written to {LOCALE_PROOF}", flush=True)
    print(app_locales.strip() or "(empty get-app-locales)", flush=True)


def resumed_activity() -> str:
    try:
        out = adb("shell", "dumpsys", "activity", "activities", check=False, timeout=20).stdout
    except subprocess.TimeoutExpired:
        return ""
    for line in out.splitlines():
        if "topResumedActivity=" in line:
            return line.strip()
    return out[-400:]


def dismiss_system_anr(max_tries: int = 10) -> None:
    for _ in range(max_tries):
        if anr_window_count() == 0:
            return
        adb("shell", "input", "tap", "300", "1320", check=False)
        time.sleep(1.5)
    adb("shell", "input", "tap", "300", "1320", check=False)
    time.sleep(2.0)


def wait_resumed(needle: str, timeout: float = 120.0) -> str:
    deadline = time.time() + timeout
    last = ""
    while time.time() < deadline:
        last = resumed_activity()
        if needle in last:
            time.sleep(0.6)
            return last
        time.sleep(0.4)
    raise SystemExit(f"timeout waiting for {needle}: {last}")


def capture(out_dir: Path, screen_id: str, scenario: str) -> Path:
    adb("shell", "am", "force-stop", PKG, check=False)
    time.sleep(0.8)
    started = adb(
        "shell",
        "am",
        "start",
        "-n",
        HARNESS,
        "--es",
        "scenario",
        scenario,
        check=False,
    )
    if started.returncode != 0:
        raise RuntimeError(started.stderr or started.stdout)
    wait_resumed(EXPECTED[screen_id])
    extra = 12.0 if screen_id in {"05", "06", "07", "08", "10", "11", "12", "15"} else 4.5
    if screen_id == "08":
        extra = 14.0
    time.sleep(extra)
    dismiss_system_anr()
    remote = f"/sdcard/hotfox_ui_{screen_id}.png"
    dest = out_dir / f"{screen_id}.png"
    last_err = None
    for _ in range(10):
        pulled = adb("shell", "screencap", "-p", remote, check=False)
        if pulled.returncode != 0:
            last_err = pulled.stderr
            time.sleep(1.5)
            continue
        adb("pull", remote, str(dest))
        if dest.is_file() and dest.stat().st_size >= 10_000:
            if anr_window_count() > 0:
                dismiss_system_anr()
                continue
            image = Image.open(dest).convert("RGB")
            pixels = np.asarray(image)
            mean = float(pixels.mean())
            std = float(pixels.std())
            r, g, b = pixels[:, :, 0], pixels[:, :, 1], pixels[:, :, 2]
            orange = ((r > 180) & (g > 70) & (g < 190) & (b < 130)).mean()
            cream = ((r > 180) & (g > 170) & (b > 150)).mean()
            top_mean = float(pixels[:240].mean())
            brandish = orange >= 0.0003 or (screen_id == "08" and cream >= 0.008)
            if mean < 14 or std < 12 or (not brandish) or top_mean > 70:
                last_err = (
                    f"android splash or blank frame mean={mean:.1f} std={std:.1f} "
                    f"orange={orange:.5f} cream={cream:.5f} top_mean={top_mean:.1f}"
                )
                time.sleep(3.0)
                continue
            adb("shell", "rm", remote, check=False)
            return dest
        last_err = "too small"
        time.sleep(1.5)
    raise RuntimeError(f"screencap failed: {last_err}")


def parse_wanted(mode: str, screens: str) -> tuple[str, ...]:
    if screens.strip():
        return tuple(s.strip().zfill(2) for s in screens.split(",") if s.strip())
    if mode == "smoke":
        return SMOKE_IDS
    return tuple(f"{i:02d}" for i in range(1, 19))


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--mode", choices=("smoke", "full"), default="full")
    parser.add_argument("--screens", default="", help="comma-separated ids, e.g. 01,02,03,04")
    parser.add_argument(
        "--out",
        default="",
        help="output directory name under verification/ui_rebuild, default actual_v8",
    )
    args = parser.parse_args()
    out_name = args.out or ("actual_v8_smoke" if args.mode == "smoke" and not args.screens else "actual_v8")
    out_dir = ROOT / "verification" / "ui_rebuild" / out_name
    out_dir.mkdir(parents=True, exist_ok=True)
    wait_for_device()
    adb("shell", "settings", "put", "global", "animator_duration_scale", "0", check=False)
    adb("shell", "settings", "put", "global", "transition_animation_scale", "0", check=False)
    adb("shell", "settings", "put", "global", "window_animation_scale", "0", check=False)
    adb("shell", "settings", "put", "secure", "ui_night_mode", "2", check=False)
    adb("shell", "cmd", "uimode", "night", "yes", check=False)
    adb("shell", "pm", "grant", PKG, "android.permission.POST_NOTIFICATIONS", check=False)
    lock_ru_locale()
    wanted = parse_wanted(args.mode, args.screens)
    missing: list[str] = []
    for idx, scenario in enumerate(SCENARIOS, start=1):
        sid = f"{idx:02d}"
        if sid not in wanted:
            continue
        try:
            path = capture(out_dir, sid, scenario)
            print(f"CAPTURED {sid} {scenario} {path.stat().st_size}", flush=True)
        except Exception as exc:  # noqa: BLE001
            missing.append(f"{sid}:{scenario}:{exc}")
            print(f"FAIL {sid} {scenario} {exc}", file=sys.stderr, flush=True)
    if missing:
        print("MISSING", *missing, sep="\n")
        return 1
    print(f"{len(wanted)}/{len(wanted)} captured into {out_dir}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
