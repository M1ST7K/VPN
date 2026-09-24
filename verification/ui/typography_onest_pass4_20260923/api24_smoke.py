#!/usr/bin/env python3
"""minSdk smoke on an API 24 emulator: Splash, Home, HTTPS render with static Onest, no crash."""
import json
import subprocess
import time
from pathlib import Path

ADB = ["adb", "-s", "emulator-5556"]
PKG = "com.hotfox.vpn"
HARNESS = f"{PKG}/com.v2ray.ang.ui.HotfoxUiScreenshotHarnessActivity"
MAIN = f"{PKG}/com.v2ray.ang.ui.MainActivity"
HERE = Path(__file__).resolve().parent
OUT = HERE / "api24"

STEPS = [
    ("01_splash", ["-n", HARNESS, "-a", "com.hotfox.vpn.action.UI_SCREENSHOT", "--es", "scenario", "01_SPLASH"], "HotFox"),
    ("05_home_harness", ["-n", HARNESS, "-a", "com.hotfox.vpn.action.UI_SCREENSHOT", "--es", "scenario", "05_DISCONNECTED"], "Не защищено"),
    ("04_https", ["-n", HARNESS, "-a", "com.hotfox.vpn.action.UI_SCREENSHOT", "--es", "scenario", "09_HTTPS_SUBSCRIPTION"], "HTTPS-подписка"),
    ("05_home_real_main", ["-n", MAIN, "--ez", "hotfox_skip_onboarding", "true"], "Подключить"),
]


def adb(*args, timeout=90):
    return subprocess.run([*ADB, *args], capture_output=True, text=True, timeout=timeout)


def dump() -> str:
    adb("shell", "rm", "-f", "/sdcard/hf.xml")
    try:
        adb("shell", "uiautomator", "dump", "/sdcard/hf.xml", timeout=60)
    except subprocess.TimeoutExpired:
        return ""
    return adb("exec-out", "cat", "/sdcard/hf.xml").stdout


def main() -> int:
    OUT.mkdir(parents=True, exist_ok=True)
    sdk = adb("shell", "getprop", "ro.build.version.sdk").stdout.strip()
    results = []
    for name, start, marker in STEPS:
        adb("logcat", "-b", "crash", "-c")
        adb("shell", "am", "force-stop", PKG)
        time.sleep(1)
        adb("shell", "am", "start", *start)
        seen = False
        deadline = time.monotonic() + 150
        while time.monotonic() < deadline:
            if marker in dump():
                seen = True
                break
            time.sleep(2)
        time.sleep(3)
        png = subprocess.run([*ADB, "exec-out", "screencap", "-p"], capture_output=True, timeout=90).stdout
        (OUT / f"{name}.png").write_bytes(png)
        crash = PKG in adb("logcat", "-d", "-b", "crash").stdout
        results.append({"step": name, "marker": marker, "rendered": seen, "crash": crash, "pass": seen and not crash})
        print(name, "PASS" if seen and not crash else "FAIL", "rendered" if seen else "marker-missing", "crash" if crash else "")
    (HERE / "api24_smoke.json").write_text(json.dumps({"sdk": sdk, "results": results}, indent=2, ensure_ascii=False))
    return 0 if all(r["pass"] for r in results) else 1


if __name__ == "__main__":
    raise SystemExit(main())
