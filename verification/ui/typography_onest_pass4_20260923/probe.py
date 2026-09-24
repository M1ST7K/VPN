#!/usr/bin/env python3
"""Launch one debug-harness scenario at a viewport, wait for render, capture PNG + UI tree."""
import re
import subprocess
import sys
import time
from pathlib import Path

ADB = ["adb", "-s", "emulator-5554"]
PKG = "com.hotfox.vpn"
HARNESS = f"{PKG}/com.v2ray.ang.ui.HotfoxUiScreenshotHarnessActivity"


def adb(*args, timeout=90):
    return subprocess.run([*ADB, *args], capture_output=True, text=True, timeout=timeout)


def dump() -> str:
    # A failed dump leaves the previous file behind; never read a stale tree.
    adb("shell", "rm", "-f", "/sdcard/hf.xml")
    try:
        adb("shell", "uiautomator", "dump", "/sdcard/hf.xml", timeout=60)
    except subprocess.TimeoutExpired:
        return ""
    result = adb("exec-out", "cat", "/sdcard/hf.xml")
    return result.stdout if result.returncode == 0 else ""


def main() -> int:
    scenario, size, font, marker, out = sys.argv[1:6]
    restart = len(sys.argv) < 7 or sys.argv[6] != "noreset"
    adb("shell", "wm", "size", size)
    adb("shell", "wm", "density", "420")
    adb("shell", "settings", "put", "system", "font_scale", font)
    if restart:
        adb("shell", "am", "force-stop", PKG)
        time.sleep(1)
        adb("shell", "am", "start", "-n", HARNESS, "-a", "com.hotfox.vpn.action.UI_SCREENSHOT",
            "--es", "scenario", scenario)
    deadline = time.monotonic() + 150
    xml = ""
    while time.monotonic() < deadline:
        xml = dump()
        if marker in xml:
            break
        time.sleep(2)
    else:
        print("MARKER NOT FOUND", marker)
        return 1
    time.sleep(5)
    xml = dump()
    path = Path(out)
    path.parent.mkdir(parents=True, exist_ok=True)
    png = subprocess.run([*ADB, "exec-out", "screencap", "-p"], capture_output=True, timeout=90).stdout
    path.write_bytes(png)
    path.with_suffix(".xml").write_text(xml)
    for node in re.finditer(r'text="([^"]*)" resource-id="([^"]*)"[^>]*?checked="(\w+)"[^>]*bounds="([^"]+)"', xml):
        text, rid, checked, bounds = node.groups()
        if rid.split("/")[-1] in {"connect_action", "switch_home_shadow", "row_shadow", "tv_home_shadow_caption"} or text in {
            "Shadow", "Доп. защита", "Защита"}:
            print(rid.split("/")[-1] or "-", repr(text), "checked=" + checked, bounds)
    print("SAVED", path)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
