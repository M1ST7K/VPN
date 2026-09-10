#!/usr/bin/env bash
# Invoked as a single command by android-emulator-runner.
# Engineering-runtime VPN E2E only. MUST NOT be treated as physical-device acceptance.
set -euo pipefail
set +x

apk="$(ls dist/HotFox_Proxy_2.2.0_universal.apk dist/*universal*.apk dist/*.apk 2>/dev/null | head -n 1 || true)"
if [[ -z "${apk}" || ! -f "$apk" ]]; then
  echo "ci_run_vpn_e2e: no debug APK in dist/" >&2
  ls -la dist 2>/dev/null || true
  exit 2
fi

chmod +x verification/emulator_vpn_e2e.sh
bash verification/emulator_vpn_e2e.sh "$apk"
