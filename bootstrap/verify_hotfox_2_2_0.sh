#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROJECT="$ROOT/V2rayNG"
fail() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }

[[ -f "$PROJECT/settings.gradle.kts" ]] || fail "V2rayNG/settings.gradle.kts missing"
[[ -f "$PROJECT/app/build.gradle.kts" ]] || fail "app/build.gradle.kts missing"

grep -q 'applicationId = "com.hotfox.vpn"' "$PROJECT/app/build.gradle.kts" || fail "applicationId com.hotfox.vpn missing"
grep -q 'minSdk = 24' "$PROJECT/app/build.gradle.kts" || fail "minSdk 24 missing"
grep -q 'versionName = "2.2.0"' "$PROJECT/app/build.gradle.kts" || fail "versionName 2.2.0 missing"
grep -q 'versionCode = 22000' "$PROJECT/app/build.gradle.kts" || fail "versionCode 22000 missing"

[[ -f "$PROJECT/app/src/main/java/com/v2ray/ang/vpn/VpnSessionCoordinator.kt" ]] || fail "VpnSessionCoordinator missing"
[[ -f "$PROJECT/app/src/main/java/com/v2ray/ang/vpn/ConnectionUiMapper.kt" ]] || fail "ConnectionUiMapper missing"
[[ -f "$PROJECT/app/src/main/java/com/v2ray/ang/vpn/VpnReadiness.kt" ]] || fail "VpnReadiness missing"
[[ -s "$PROJECT/app/src/main/res/drawable/hotfox_logo.xml" ]] || fail "hotfox_logo missing"

grep -q 'VpnReadiness.waitForLocalSocks' "$PROJECT/app/src/main/java/com/v2ray/ang/service/CoreVpnService.kt" \
  || fail "SOCKS readiness wait missing from CoreVpnService"
grep -q 'VpnSessionState.STARTING_HEV' "$PROJECT/app/src/main/java/com/v2ray/ang/service/CoreVpnService.kt" \
  || fail "STARTING_HEV state missing"
grep -q 'VpnSessionCoordinator.markConnected()' "$PROJECT/app/src/main/java/com/v2ray/ang/service/CoreVpnService.kt" \
  || fail "markConnected missing"
grep -q 'fun vpnProtect' "$PROJECT/app/src/main/java/com/v2ray/ang/service/CoreVpnService.kt" \
  || fail "vpnProtect missing"
grep -q 'addDisallowedApplication(selfPackageName)' "$PROJECT/app/src/main/java/com/v2ray/ang/service/CoreVpnService.kt" \
  || fail "self VPN exclusion missing"
grep -q 'builder.addRoute("::", 0)' "$PROJECT/app/src/main/java/com/v2ray/ang/service/CoreVpnService.kt" \
  || fail "IPv6 capture route missing"
grep -q 'hotfox_hev_stability_migrated_2_0_2' "$PROJECT/app/src/main/java/com/v2ray/ang/handler/SettingsManager.kt" \
  || fail "HEV default migration missing"
grep -q 'System.loadLibrary("hev-socks5-tunnel")' "$PROJECT/app/src/main/java/com/v2ray/ang/service/TProxyService.kt" \
  || fail "HEV JNI load path missing"
grep -q 'outboundTag = AppConfig.TAG_BLOCKED' "$PROJECT/app/src/main/java/com/v2ray/ang/core/CoreConfigManager.kt" \
  || fail "IPv6 fail-closed blackhole missing"
grep -q 'waitForLocalSocksBlocking' "$PROJECT/app/src/main/java/com/v2ray/ang/core/CoreServiceManager.kt" \
  || fail "reload SOCKS wait missing"
grep -q 'disable += "MissingTranslation"' "$PROJECT/app/build.gradle.kts" \
  || fail "MissingTranslation lint disable missing"
grep -q 'HotFox' "$PROJECT/app/src/main/res/layout/activity_main.xml" || fail "HotFox UI marker missing"

for abi in arm64-v8a armeabi-v7a x86 x86_64; do
  for lib in libhev-socks5-tunnel.so libhevsockstun.so; do
    f="$PROJECT/app/libs/$abi/$lib"
    [[ -s "$f" ]] || fail "missing/empty native library $f"
  done
done

if grep -R -I -n -E "https://nox\\.hotto-fox\\.st/|vless://[^[:space:]\"]{20,}" --exclude-dir=build --exclude-dir=test --exclude-dir=androidTest --exclude="*.md" --exclude="*.txt" "$PROJECT" >/tmp/hotfox-secret-scan.txt 2>/dev/null; then
  cat /tmp/hotfox-secret-scan.txt >&2
  fail "possible personal subscription/VLESS secret found in source tree"
fi
rm -f /tmp/hotfox-secret-scan.txt

printf 'PASS HotFox 2.2.0 overlay verification\n'
