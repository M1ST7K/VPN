#!/usr/bin/env python3
"""GUI wiring checks on the real activities. Each step launches a start screen, taps a
labelled control and records the resumed activity / visible marker afterwards.
Never grants VPN permission and never enters a subscription secret."""
import json
import re
import subprocess
import sys
import time
from pathlib import Path

ADB = ["adb", "-s", "emulator-5554"]
PKG = "com.hotfox.vpn"
HARNESS = f"{PKG}/com.v2ray.ang.ui.HotfoxUiScreenshotHarnessActivity"
MAIN = f"{PKG}/com.v2ray.ang.ui.MainActivity"
HERE = Path(__file__).resolve().parent
OUT = HERE / "interaction"


def adb(*args, timeout=90):
    return subprocess.run([*ADB, *args], capture_output=True, text=True, timeout=timeout)


def dump() -> str:
    adb("shell", "rm", "-f", "/sdcard/hf.xml")
    try:
        adb("shell", "uiautomator", "dump", "/sdcard/hf.xml", timeout=60)
    except subprocess.TimeoutExpired:
        return ""
    return adb("exec-out", "cat", "/sdcard/hf.xml").stdout


def resumed() -> str:
    out = adb("shell", "dumpsys", "activity", "activities").stdout
    m = re.search(r"mResumedActivity: ActivityRecord\{\S+ \S+ (\S+)", out) or re.search(r"topResumedActivity=ActivityRecord\{\S+ \S+ (\S+)", out)
    return m.group(1) if m else ""


def wait_for(marker: str, timeout=120) -> str:
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        xml = dump()
        if marker in xml:
            time.sleep(2)
            return dump()
        time.sleep(2)
    return ""


def center_of(xml: str, key: str):
    for attr in ("resource-id", "text", "content-desc"):
        value = f"{PKG}:id/{key}" if attr == "resource-id" else key
        m = re.search(rf'{attr}="{re.escape(value)}"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml)
        if m:
            x1, y1, x2, y2 = map(int, m.groups())
            return (x1 + x2) // 2, (y1 + y2) // 2
    return None


def start(kind: str, value: str):
    adb("shell", "am", "force-stop", PKG)
    time.sleep(1)
    if kind == "scenario":
        adb("shell", "am", "start", "-n", HARNESS, "-a", "com.hotfox.vpn.action.UI_SCREENSHOT", "--es", "scenario", value)
    else:
        adb("shell", "am", "start", "-n", MAIN, "--ez", "hotfox_skip_onboarding", "true")


def crashed() -> bool:
    log = adb("logcat", "-d", "-b", "crash").stdout
    return "com.hotfox.vpn" in log


STEPS = [
    # name, start kind, start value, ready marker, tap key, expectation(activity substring or xml marker)
    ("02_import", "scenario", "02_ONBOARD_CONNECT", "У меня уже есть подписка", "У меня уже есть подписка", {"activity": "HotfoxHttpsImportActivity"}),
    ("02_buy_access", "scenario", "02_ONBOARD_CONNECT", "Купить доступ", "Купить доступ", {"not_activity": "HotfoxOnboardingActivity"}),
    ("03_auto", "scenario", "03_ONBOARD_AUTO", "Использовать AUTO", "Использовать AUTO", {"xml": "Всё готово"}),
    ("03_manual", "scenario", "03_ONBOARD_AUTO", "Выбрать вручную", "Выбрать вручную", {"activity": "MainActivity"}),
    ("04_add_invalid", "scenario", "09_HTTPS_SUBSCRIPTION", "Добавить", "Добавить", {"activity": "HotfoxHttpsImportActivity"}),
    ("home_connect", "main", "", "connect_action", "connect_action", {"any_xml": ["Добавить подключение", "com.android.vpndialogs"]}),
    ("home_server_card", "main", "", "selected_server_card", "selected_server_card", {"xml": "Лучший сервер автоматически"}),
    ("home_shadow_row", "main", "", "row_shadow", "Shadow", {"activity": "HotfoxShadowActivity"}),
    ("home_smart", "main", "", "smart_routing_card", "smart_routing_card", {"not_activity": ".MainActivity"}),
    ("home_subscription", "main", "", "Подписка", "Управление доступом", {"not_xml": "id/connect_action"}),
    ("nav_servers", "main", "", "Серверы", "Серверы", {"xml": "Лучший сервер автоматически"}),
    ("nav_subscription", "main", "", "Подписка", "Подписка", {"not_xml": "id/connect_action"}),
    ("nav_settings", "main", "", "Настройки", "Настройки", {"not_xml": "id/connect_action"}),
]


def evaluate(expect, xml, activity):
    if "activity" in expect and expect["activity"] not in activity:
        return False
    if "not_activity" in expect and expect["not_activity"] in activity:
        return False
    if "xml" in expect and expect["xml"] not in xml:
        return False
    if "not_xml" in expect and expect["not_xml"] in xml:
        return False
    if "any_xml" in expect and not any(m in xml for m in expect["any_xml"]):
        return expect.get("fallback_activity", "\0") in activity
    return True


def main() -> int:
    OUT.mkdir(parents=True, exist_ok=True)
    adb("shell", "wm", "size", "1032x2292")
    adb("shell", "wm", "density", "420")
    adb("shell", "settings", "put", "system", "font_scale", "1.0")
    only = set(sys.argv[1:])
    previous = json.loads((HERE / "interaction.json").read_text()) if only and (HERE / "interaction.json").exists() else []
    results = [r for r in previous if r["step"] not in only]
    for name, kind, value, ready, tap, expect in STEPS:
        if only and name not in only:
            continue
        adb("logcat", "-b", "crash", "-c")
        start(kind, value)
        xml = wait_for(ready)
        point = center_of(xml, tap) if xml else None
        if point is None:
            results.append({"step": name, "pass": False, "reason": "control not found"})
            print(name, "FAIL control not found")
            continue
        if name.startswith("nav_"):
            # bottom-nav labels: pick the lowest occurrence
            matches = [tuple(map(int, m.groups())) for m in re.finditer(rf'text="{re.escape(tap)}"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml)]
            x1, y1, x2, y2 = max(matches, key=lambda b: b[1])
            point = ((x1 + x2) // 2, (y1 + y2) // 2)
        adb("shell", "input", "tap", str(point[0]), str(point[1]))
        time.sleep(4)
        after = dump()
        activity = resumed()
        ok = evaluate(expect, after, activity) and not crashed()
        png = subprocess.run([*ADB, "exec-out", "screencap", "-p"], capture_output=True, timeout=90).stdout
        (OUT / f"{name}.png").write_bytes(png)
        results.append({"step": name, "pass": ok, "tapped": tap, "resumed_activity": activity})
        print(name, "PASS" if ok else "FAIL", activity)
        if name == "home_connect":
            adb("shell", "input", "keyevent", "KEYCODE_BACK")
    order = [s[0] for s in STEPS]
    results.sort(key=lambda r: order.index(r["step"]))
    (HERE / "interaction.json").write_text(json.dumps(results, indent=2, ensure_ascii=False))
    return 0 if all(r["pass"] for r in results) else 1


if __name__ == "__main__":
    raise SystemExit(main())
