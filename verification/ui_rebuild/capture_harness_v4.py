#!/usr/bin/env python3
"""Capture 18 HotFox presentation states via the debug screenshot harness."""
from __future__ import annotations

import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "verification" / "ui_rebuild" / "actual_v4"
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
    "11": "HotfoxServerDetailsActivity",
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


def run(cmd: list[str], check: bool = True) -> subprocess.CompletedProcess[str]:
    return subprocess.run(cmd, check=check, text=True, capture_output=True)


def adb(*args: str, check: bool = True) -> subprocess.CompletedProcess[str]:
    return run(["adb", *args], check=check)


def wait_for_device() -> None:
    adb("wait-for-device")
    for _ in range(60):
        boot = adb("shell", "getprop", "sys.boot_completed", check=False).stdout.strip()
        if boot == "1":
            return
        time.sleep(2)
    raise SystemExit("emulator did not boot")


def resumed_activity() -> str:
    out = adb("shell", "dumpsys", "activity", "activities", check=False).stdout
    for line in out.splitlines():
        if "topResumedActivity=" in line:
            return line.strip()
    return out[-400:]


def wait_resumed(needle: str, timeout: float = 12.0) -> str:
    deadline = time.time() + timeout
    last = ""
    while time.time() < deadline:
        last = resumed_activity()
        if needle in last:
            time.sleep(0.4)
            return last
        time.sleep(0.25)
    raise SystemExit(f"timeout waiting for {needle}: {last}")


def capture(screen_id: str, scenario: str) -> Path:
    adb("shell", "am", "force-stop", PKG, check=False)
    time.sleep(0.4)
    adb(
        "shell",
        "am",
        "start",
        "-W",
        "-n",
        HARNESS,
        "--es",
        "scenario",
        scenario,
    )
    wait_resumed(EXPECTED[screen_id])
    if screen_id == "08":
        time.sleep(1.5)
    remote = f"/sdcard/hotfox_ui_{screen_id}.png"
    last_err = None
    for attempt in range(3):
        pulled = adb("shell", "screencap", "-p", remote, check=False)
        if pulled.returncode == 0:
            break
        last_err = pulled.stderr
        time.sleep(0.5)
    else:
        raise RuntimeError(f"screencap failed: {last_err}")
    dest = OUT / f"{screen_id}.png"
    adb("pull", remote, str(dest))
    adb("shell", "rm", remote, check=False)
    if dest.stat().st_size < 10_000:
        raise SystemExit(f"capture too small: {dest} ({dest.stat().st_size} bytes)")
    return dest


def main() -> int:
    OUT.mkdir(parents=True, exist_ok=True)
    wait_for_device()
    adb("shell", "settings", "put", "secure", "ui_night_mode", "2", check=False)
    adb("shell", "cmd", "uimode", "night", "yes", check=False)
    adb("shell", "pm", "grant", PKG, "android.permission.POST_NOTIFICATIONS", check=False)
    missing: list[str] = []
    for idx, scenario in enumerate(SCENARIOS, start=1):
        sid = f"{idx:02d}"
        try:
            path = capture(sid, scenario)
            print(f"CAPTURED {sid} {scenario} {path.stat().st_size}")
        except Exception as exc:  # noqa: BLE001
            missing.append(f"{sid}:{scenario}:{exc}")
            print(f"FAIL {sid} {scenario} {exc}", file=sys.stderr)
    if missing:
        print("MISSING", *missing, sep="\n")
        return 1
    print("18/18 captured")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
