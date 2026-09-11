#!/usr/bin/env bash
# Invoked as a single command by android-emulator-runner (line-by-line /bin/sh).
# UI/install smoke only. MUST NOT be treated as VPN E2E.
set -euo pipefail

apk="$(ls dist/HotFox_Proxy_2.2.0_universal.apk dist/*universal*.apk dist/*.apk 2>/dev/null | head -n 1 || true)"
if [[ -z "${apk}" || ! -f "$apk" ]]; then
  echo "ci_run_emulator_ui_smoke: no debug APK in dist/" >&2
  ls -la dist 2>/dev/null || true
  exit 2
fi

chmod +x verification/emulator_ui_smoke.sh
bash verification/emulator_ui_smoke.sh "$apk"
