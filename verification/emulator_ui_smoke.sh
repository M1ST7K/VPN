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
REMOTE_DUMP="/data/local/tmp/hotfox_window_dump.xml"

fail() {
  echo "SMOKE FAIL: $*" >&2
  "$ADB" logcat -d -t 400 >"$LOGCAT" 2>/dev/null || true
  "$ADB" exec-out screencap -p >"$SCREEN" 2>/dev/null || true
  echo "logcat: $LOGCAT screenshot: $SCREEN dump: $DUMP" >&2
  exit 1
}

wait_for_pm() {
  local i
  for i in $(seq 1 60); do
    if "$ADB" shell service check package 2>/dev/null | grep -q "found"; then
      if "$ADB" shell pm path android >/dev/null 2>&1; then
        return 0
      fi
    fi
    sleep 2
  done
  return 1
}

unlock_emulator() {
  "$ADB" shell wm dismiss-keyguard >/dev/null 2>&1 || true
  "$ADB" shell input keyevent 224 >/dev/null 2>&1 || true
  "$ADB" shell input keyevent 82 >/dev/null 2>&1 || true
  "$ADB" shell settings put global stay_on_while_plugged_in 3 >/dev/null 2>&1 || true
}

dump_ui() {
  "$ADB" shell rm -f "$REMOTE_DUMP" >/dev/null 2>&1 || true
  # Default uiautomator dump writes /sdcard, which can I/O-fail on a
  # freshly booted CI emulator. Keep the dump on /data/local/tmp.
  if ! "$ADB" shell uiautomator dump "$REMOTE_DUMP" >/dev/null 2>&1; then
    return 1
  fi
  "$ADB" pull "$REMOTE_DUMP" "$DUMP" >/dev/null 2>&1
}

wait_dump_contains() {
  local needle="$1"
  local tries="${2:-20}"
  local i
  for i in $(seq 1 "$tries"); do
    if dump_ui && grep -q "$needle" "$DUMP"; then
      return 0
    fi
    sleep 2
  done
  return 1
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
wait_for_pm || fail "package manager never became ready"
unlock_emulator

install_ok=0
for i in $(seq 1 8); do
  if "$ADB" install -r -t -g "$APK"; then
    install_ok=1
    break
  fi
  echo "apk install retry $i" >&2
  sleep 5
done
[[ "$install_ok" -eq 1 ]] || fail "apk install failed"

"$ADB" shell am force-stop "$PACKAGE" >/dev/null 2>&1 || true
"$ADB" logcat -c || true
# Do not use am start -W: first-frame idle can time out on a software-renderer
# emulator even when MainActivity is alive (LaunchState UNKNOWN).
"$ADB" shell am start -n "$PACKAGE/$ACTIVITY" || fail "launch failed"

pid=""
for i in $(seq 1 30); do
  pid="$("$ADB" shell pidof "$PACKAGE" | tr -d '\r' || true)"
  if [[ -n "$pid" ]]; then
    break
  fi
  sleep 1
done
[[ -n "$pid" ]] || fail "process died after launch"

unlock_emulator
wait_dump_contains "Соединение" 20 || fail "Connection destination missing after launch"

servers_xy="$(tap_text "Серверы" || true)"
[[ -n "$servers_xy" ]] || fail "Servers nav not found"
# shellcheck disable=SC2086
"$ADB" shell input tap $servers_xy
wait_dump_contains "Авто-выбор сервера" 15 || fail "AUTO row is not first/visible on Servers"

sub_xy="$(tap_text "Подписка" || true)"
[[ -n "$sub_xy" ]] || fail "Subscription nav not found"
# shellcheck disable=SC2086
"$ADB" shell input tap $sub_xy
sleep 2
pid2="$("$ADB" shell pidof "$PACKAGE" | tr -d '\r' || true)"
[[ -n "$pid2" ]] || fail "process died after navigation"

echo "PASS emulator UI smoke (not VPN E2E)"
