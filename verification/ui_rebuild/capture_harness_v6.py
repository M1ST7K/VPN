#!/usr/bin/env python3
"""Capture HotFox presentation states via the debug screenshot harness (V6, ru-RU)."""
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
LOCALE_PROOF = ROOT / "verification" / "ui_rebuild" / "V6_LOCALE_PROOF.txt"


def run(cmd: list[str], check: bool = True) -> subprocess.CompletedProcess[str]:
    return subprocess.run(cmd, check=check, text=True, capture_output=True)


def adb(*args: str, check: bool = True) -> subprocess.CompletedProcess[str]:
    return run(["adb", *args], check=check)


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
    pkg = adb("shell", "dumpsys", "package", PKG, check=False).stdout
    locale_lines = "\n".join(
        line for line in pkg.splitlines() if "locale" in line.lower()
    )[:2000]
    LOCALE_PROOF.write_text(
        "cmd locale get-app-locales:\n"
        f"{app_locales.strip()}\n\n"
        "persist.sys.locale:\n"
        f"{persist.strip()}\n\n"
        "dumpsys package locale lines:\n"
        f"{locale_lines}\n",
        encoding="utf-8",
    )
    print(f"locale lock written to {LOCALE_PROOF}")
    print(app_locales.strip() or "(empty get-app-locales)")


def resumed_activity() -> str:
    out = adb("shell", "dumpsys", "activity", "activities", check=False).stdout
    for line in out.splitlines():
        if "topResumedActivity=" in line:
            return line.strip()
    return out[-400:]


def anr_window_count() -> int:
    out = adb("shell", "dumpsys", "window", check=False).stdout
    return out.count("Application Not Responding:")


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
    adb("shell", "cmd", "locale", "set-app-locales", PKG, "--locales", "ru-RU", check=False)
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
    extra = 8.0 if screen_id in {"05", "06", "07", "08", "10", "11", "12", "15"} else 4.0
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
            if float(pixels.mean()) < 14 or float(pixels.std()) < 12:
                last_err = "android splash or blank frame"
                time.sleep(3.0)
                continue
            adb("shell", "rm", remote, check=False)
            return dest
        last_err = "too small"
        time.sleep(1.5)
    raise RuntimeError(f"screencap failed: {last_err}")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--mode", choices=("smoke", "full"), default="smoke")
    args = parser.parse_args()
    out_dir = ROOT / "verification" / "ui_rebuild" / (
        "actual_v6_smoke" if args.mode == "smoke" else "actual_v6"
    )
    out_dir.mkdir(parents=True, exist_ok=True)
    wait_for_device()
    adb("shell", "settings", "put", "global", "animator_duration_scale", "0", check=False)
    adb("shell", "settings", "put", "global", "transition_animation_scale", "0", check=False)
    adb("shell", "settings", "put", "global", "window_animation_scale", "0", check=False)
    adb("shell", "settings", "put", "secure", "ui_night_mode", "2", check=False)
    adb("shell", "cmd", "uimode", "night", "yes", check=False)
    adb("shell", "pm", "grant", PKG, "android.permission.POST_NOTIFICATIONS", check=False)
    lock_ru_locale()
    wanted = SMOKE_IDS if args.mode == "smoke" else tuple(f"{i:02d}" for i in range(1, 19))
    missing: list[str] = []
    for idx, scenario in enumerate(SCENARIOS, start=1):
        sid = f"{idx:02d}"
        if sid not in wanted:
            continue
        try:
            path = capture(out_dir, sid, scenario)
            print(f"CAPTURED {sid} {scenario} {path.stat().st_size}")
        except Exception as exc:  # noqa: BLE001
            missing.append(f"{sid}:{scenario}:{exc}")
            print(f"FAIL {sid} {scenario} {exc}", file=sys.stderr)
    if missing:
        print("MISSING", *missing, sep="\n")
        return 1
    print(f"{len(wanted)}/{len(wanted)} captured into {out_dir}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
