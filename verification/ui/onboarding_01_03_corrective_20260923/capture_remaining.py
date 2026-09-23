#!/usr/bin/env python3
"""Sleep-based remaining captures (no dumpsys window)."""
from __future__ import annotations

import subprocess
import time
from io import BytesIO
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "verification" / "ui" / "onboarding_01_03_corrective_20260923" / "actual"
PKG = "com.hotfox.vpn"
HARNESS = f"{PKG}/com.v2ray.ang.ui.HotfoxUiScreenshotHarnessActivity"
SERIAL = "emulator-5554"
DENSITY = 420
JOBS = [
    ("03", "03_ONBOARD_AUTO", "compact", "1.0", (945, 2100), 10),
    ("03", "03_ONBOARD_AUTO", "large", "1.0", (1082, 2402), 10),
    ("03", "03_ONBOARD_AUTO", "standard_fs115", "1.15", (1032, 2231), 10),
    ("02", "02_ONBOARD_CONNECT", "standard_fs115", "1.15", (1032, 2231), 10),
    ("01", "01_SPLASH", "compact", "1.0", (945, 2100), 14),
    ("02", "02_ONBOARD_CONNECT", "compact", "1.0", (945, 2100), 10),
    ("05", "05_DISCONNECTED", "standard", "1.0", (1032, 2231), 12),
]


def adb(*args: str, timeout: int = 40) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        ["adb", "-s", SERIAL, *args],
        check=False,
        text=True,
        capture_output=True,
        timeout=timeout,
    )


def dismiss() -> None:
    adb("shell", "settings", "put", "global", "hide_error_dialogs", "1")
    adb("shell", "input", "tap", "516", "1306")


def capture(name: str) -> None:
    last = 0
    for attempt in range(12):
        raw = subprocess.run(
            ["adb", "-s", SERIAL, "exec-out", "screencap", "-p"],
            check=False,
            capture_output=True,
            timeout=80,
        )
        last = len(raw.stdout)
        if raw.returncode == 0 and last >= 40_000:
            dest = OUT / f"{name}.png"
            dest.write_bytes(raw.stdout)
            im = Image.open(BytesIO(raw.stdout))
            print(f"CAPTURED {name} {im.size} bytes={dest.stat().st_size}", flush=True)
            return
        time.sleep(2.5)
        dismiss()
    raise RuntimeError(f"{name} screencap failed {last}")


def main() -> int:
    OUT.mkdir(parents=True, exist_ok=True)
    adb("wait-for-device")
    adb("shell", "settings", "put", "global", "animator_duration_scale", "0")
    adb("shell", "cmd", "locale", "set-app-locales", PKG, "--locales", "ru-RU")
    for sid, scenario, suffix, font, size, wait in JOBS:
        dismiss()
        adb("shell", "wm", "size", f"{size[0]}x{size[1]}")
        adb("shell", "wm", "density", str(DENSITY))
        adb("shell", "settings", "put", "system", "font_scale", font)
        time.sleep(3.0)
        adb("shell", "am", "force-stop", PKG)
        time.sleep(0.8)
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
        time.sleep(wait)
        dismiss()
        capture(f"{sid}_{suffix}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
