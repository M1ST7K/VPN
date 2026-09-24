#!/usr/bin/env python3
"""Real MainActivity Shadow toggle test: tap, then force-stop + relaunch, checking the
switch is re-read from HotfoxShadowStore each time. Writes shadow_toggle.json + frames."""
import json
import re
import subprocess
import sys
import time
from pathlib import Path

ADB = ["adb", "-s", "emulator-5554"]
PKG = "com.hotfox.vpn"
MAIN = f"{PKG}/com.v2ray.ang.ui.MainActivity"
OUT = Path(__file__).resolve().parent / "matrix"


def adb(*args, timeout=90):
    return subprocess.run([*ADB, *args], capture_output=True, text=True, timeout=timeout)


def dump() -> str:
    adb("shell", "rm", "-f", "/sdcard/hf.xml")
    adb("shell", "uiautomator", "dump", "/sdcard/hf.xml", timeout=60)
    return adb("exec-out", "cat", "/sdcard/hf.xml").stdout


def switch_state(xml: str):
    m = re.search(r'resource-id="com\.hotfox\.vpn:id/switch_home_shadow"[^>]*?checked="(\w+)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml)
    if not m:
        return None, None
    checked = m.group(1) == "true"
    x1, y1, x2, y2 = map(int, m.groups()[1:])
    return checked, ((x1 + x2) // 2, (y1 + y2) // 2)


def wait_home(timeout=150):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        xml = dump()
        state, center = switch_state(xml)
        if state is not None:
            time.sleep(3)
            xml = dump()
            return xml
        time.sleep(2)
    raise SystemExit("switch_home_shadow not found")


def launch():
    adb("shell", "am", "force-stop", PKG)
    time.sleep(1)
    adb("shell", "am", "start", "-n", MAIN, "--ez", "hotfox_skip_onboarding", "true")
    return wait_home()


def shot(name, xml):
    OUT.mkdir(parents=True, exist_ok=True)
    png = subprocess.run([*ADB, "exec-out", "screencap", "-p"], capture_output=True, timeout=90).stdout
    (OUT / f"{name}.png").write_bytes(png)
    (OUT / f"{name}.xml").write_text(xml)


def main() -> int:
    adb("shell", "wm", "size", sys.argv[1] if len(sys.argv) > 1 else "1032x2292")
    adb("shell", "wm", "density", "420")
    adb("shell", "settings", "put", "system", "font_scale", "1.0")
    steps = []
    xml = launch()
    initial, center = switch_state(xml)
    steps.append({"step": "initial_launch", "checked": initial})
    shot("shadow_initial", xml)
    expected = initial
    for label in ("tap_1", "tap_2"):
        adb("shell", "input", "tap", str(center[0]), str(center[1]))
        time.sleep(2)
        xml = dump()
        expected = not expected
        now, center = switch_state(xml)
        steps.append({"step": label, "expected": expected, "checked": now})
        shot(f"shadow_{'on' if now else 'off'}", xml)
        xml = launch()
        after, center = switch_state(xml)
        steps.append({"step": f"{label}_recreate", "expected": expected, "checked": after})
        shot(f"shadow_{'on' if after else 'off'}_recreate", xml)
    ok = all(s.get("expected", s["checked"]) == s["checked"] for s in steps)
    (OUT.parent / "shadow_toggle.json").write_text(json.dumps({"pass": ok, "steps": steps}, indent=2, ensure_ascii=False))
    print(json.dumps({"pass": ok, "steps": steps}, ensure_ascii=False))
    return 0 if ok else 1


if __name__ == "__main__":
    raise SystemExit(main())
