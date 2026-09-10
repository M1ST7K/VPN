#!/usr/bin/env bash
# Engineering-runtime VPN E2E on an emulator/runtime. NOT physical-device acceptance.
# The subscription URL must already be in HOTFOX_TEST_SUBSCRIPTION_URL.
# Never print the URL, UUID, or token.
set -euo pipefail
set +x

APK="${1:-}"
if [[ -z "$APK" || ! -f "$APK" ]]; then
  echo "usage: emulator_vpn_e2e.sh <universal.apk>" >&2
  exit 2
fi

if [[ -z "${HOTFOX_TEST_SUBSCRIPTION_URL:-}" ]]; then
  echo "NOT EXECUTED: HOTFOX_TEST_SUBSCRIPTION_URL is not set"
  echo "Physical-device E2E remains NOT EXECUTED."
  exit 0
fi

ADB="${ADB:-adb}"
PACKAGE="com.hotfox.vpn"
ACTIVITY="com.v2ray.ang.vpn.HotfoxEngineeringE2eActivity"
ACTION="com.hotfox.vpn.action.ENGINEERING_RUNTIME_E2E"
REPORT_HOST="/tmp/hotfox-e2e-report.txt"
REPORT_NAME="hotfox-e2e-report.txt"
LOGCAT="/tmp/hotfox-e2e-logcat.txt"
DUMP="/tmp/hotfox-e2e-ui.xml"
SCREEN="/tmp/hotfox-e2e.png"
REMOTE_DUMP="/data/local/tmp/hotfox_window_dump.xml"
SUB_REMOTE="/data/local/tmp/hotfox-e2e-sub.url"
CYCLES="${HOTFOX_E2E_CYCLES:-3}"

fail() {
  echo "ENGINEERING E2E FAIL: $*" >&2
  "$ADB" logcat -d -t 800 >"$LOGCAT" 2>/dev/null || true
  "$ADB" exec-out screencap -p >"$SCREEN" 2>/dev/null || true
  echo "logcat: $LOGCAT screenshot: $SCREEN dump: $DUMP report: $REPORT_HOST" >&2
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
  if ! "$ADB" shell uiautomator dump "$REMOTE_DUMP" >/dev/null 2>&1; then
    return 1
  fi
  "$ADB" pull "$REMOTE_DUMP" "$DUMP" >/dev/null 2>&1
}

tap_text() {
  local label="$1"
  python3 - "$DUMP" "$label" <<'PY'
import re, sys
path, label = sys.argv[1], sys.argv[2]
text = open(path, encoding="utf-8", errors="replace").read()
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

tap_vpn_consent() {
  local i
  for i in $(seq 1 12); do
    dump_ui || { sleep 1; continue; }
    local xy=""
    xy="$(tap_text "OK" || true)"
    [[ -z "$xy" ]] && xy="$(tap_text "Allow" || true)"
    [[ -z "$xy" ]] && xy="$(tap_text "Разрешить" || true)"
    if [[ -n "$xy" ]]; then
      # shellcheck disable=SC2086
      "$ADB" shell input tap $xy
      sleep 1
    fi
    sleep 1
  done
}

write_sub_file() {
  python3 - <<'PY'
import os, pathlib
url = os.environ.get("HOTFOX_TEST_SUBSCRIPTION_URL", "").strip()
path = pathlib.Path("/tmp/hotfox-e2e-sub.url")
if not url:
    raise SystemExit("missing HOTFOX_TEST_SUBSCRIPTION_URL")
path.write_text(url, encoding="utf-8")
print(f"subscription_file_bytes={path.stat().st_size}")
PY
  "$ADB" push /tmp/hotfox-e2e-sub.url "$SUB_REMOTE" >/dev/null
  rm -f /tmp/hotfox-e2e-sub.url
  "$ADB" shell chmod 644 "$SUB_REMOTE" >/dev/null 2>&1 || true
}

pull_report() {
  rm -f "$REPORT_HOST"
  if "$ADB" shell run-as "$PACKAGE" cat "files/$REPORT_NAME" >"$REPORT_HOST" 2>/dev/null; then
    return 0
  fi
  local ext
  ext="$("$ADB" shell echo /sdcard/Android/data/$PACKAGE/files/hotfox-e2e-report.txt | tr -d '\r')"
  "$ADB" pull "$ext" "$REPORT_HOST" >/dev/null 2>&1 || true
  [[ -s "$REPORT_HOST" ]]
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

"$ADB" shell appops set "$PACKAGE" ACTIVATE_VPN allow >/dev/null 2>&1 || true
"$ADB" shell am force-stop "$PACKAGE" >/dev/null 2>&1 || true
"$ADB" logcat -c || true
write_sub_file

"$ADB" shell am start -n "$PACKAGE/$ACTIVITY" --ei cycles "$CYCLES" || fail "e2e activity launch failed"

tap_vpn_consent

echo "waiting for engineering-runtime E2E report"
found=0
for i in $(seq 1 180); do
  if pull_report; then
    found=1
    break
  fi
  if [[ $((i % 10)) -eq 0 ]]; then
    tap_vpn_consent
  fi
  sleep 2
done
[[ "$found" -eq 1 ]] || fail "E2E report was never written"

"$ADB" logcat -d -t 400 >"$LOGCAT" 2>/dev/null || true

python3 - "$REPORT_HOST" <<'PY'
import pathlib, sys
text = pathlib.Path(sys.argv[1]).read_text(encoding="utf-8", errors="replace")
print(text)
if "engineeringRuntimeE2e=PASS" not in text:
    raise SystemExit("engineeringRuntimeE2e did not report PASS")
if "physicalDeviceE2e=NOT_EXECUTED" not in text:
    raise SystemExit("physical-device E2E marker missing")
PY

echo "PASS engineering-runtime VPN E2E (not physical-device)"
