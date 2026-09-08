#!/usr/bin/env bash
# UI/install smoke only. MUST NOT be treated as VPN E2E.
set -euo pipefail

APK="${1:-}"
if [[ -z "$APK" || ! -f "$APK" ]]; then
  echo "usage: emulator_ui_smoke.sh <universal.apk>" >&2
  exit 2
fi

ADB="${ADB:-adb}"
PACKAGE="com.hotfox.vpn"
ACTIVITY="com.v2ray.ang.ui.MainActivity"
DUMP="/tmp/hotfox-ui-dump.xml"
LOGCAT="/tmp/hotfox-smoke-logcat.txt"
SCREEN="/tmp/hotfox-smoke.png"

fail() {
  echo "SMOKE FAIL: $*" >&2
  "$ADB" logcat -d -t 200 >"$LOGCAT" 2>/dev/null || true
  "$ADB" exec-out screencap -p >"$SCREEN" 2>/dev/null || true
  echo "logcat: $LOGCAT screenshot: $SCREEN" >&2
  exit 1
}

dump_ui() {
  "$ADB" shell uiautomator dump >/dev/null
  "$ADB" pull /sdcard/window_dump.xml "$DUMP" >/dev/null
}

tap_text() {
  local label="$1"
  python3 - "$DUMP" "$label" <<'PY'
import re, sys
path, label = sys.argv[1], sys.argv[2]
text = open(path, encoding="utf-8", errors="replace").read()
# Match bounds of a node whose text contains the label.
pat = re.compile(r'text="%s"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"' % re.escape(label))
m = pat.search(text)
if not m:
    pat = re.compile(r'text="[^"]*%s[^"]*"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"' % re.escape(label))
    m = pat.search(text)
if not m:
    sys.exit(1)
x = (int(m.group(1)) + int(m.group(3))) // 2
y = (int(m.group(2)) + int(m.group(4))) // 2
print(f"{x} {y}")
PY
}

"$ADB" wait-for-device
"$ADB" install -r -t "$APK" || fail "apk install failed"
"$ADB" shell am force-stop "$PACKAGE" >/dev/null 2>&1 || true
"$ADB" logcat -c || true
"$ADB" shell am start -W -n "$PACKAGE/$ACTIVITY" || fail "launch failed"
sleep 4
pid="$("$ADB" shell pidof "$PACKAGE" | tr -d '\r')"
[[ -n "$pid" ]] || fail "process died after launch"

dump_ui
grep -q "Соединение" "$DUMP" || fail "Connection destination missing after launch"

servers_xy="$(tap_text "Серверы" || true)"
[[ -n "$servers_xy" ]] || fail "Servers nav not found"
# shellcheck disable=SC2086
"$ADB" shell input tap $servers_xy
sleep 2
dump_ui
grep -q "Авто-выбор сервера" "$DUMP" || fail "AUTO row is not first/visible on Servers"

sub_xy="$(tap_text "Подписка" || true)"
[[ -n "$sub_xy" ]] || fail "Subscription nav not found"
# shellcheck disable=SC2086
"$ADB" shell input tap $sub_xy
sleep 2
pid2="$("$ADB" shell pidof "$PACKAGE" | tr -d '\r')"
[[ -n "$pid2" ]] || fail "process died after navigation"

echo "PASS emulator UI smoke (not VPN E2E)"
