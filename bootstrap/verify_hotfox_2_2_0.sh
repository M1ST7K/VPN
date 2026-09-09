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
grep -q 'VpnSessionCoordinator.markConnected(' "$PROJECT/app/src/main/java/com/v2ray/ang/service/CoreVpnService.kt" \
  || fail "markConnected missing"
grep -q 'pathVerified' "$PROJECT/app/src/main/java/com/v2ray/ang/service/CoreVpnService.kt" \
  || fail "pathVerified gate missing"
grep -q 'injectThroughVpn' "$PROJECT/app/src/main/java/com/v2ray/ang/vpn/VpnReadiness.kt" \
  || fail "TUN inject probe missing"
grep -q 'tun-not-forwarded' "$PROJECT/app/src/main/java/com/v2ray/ang/vpn/VpnReadiness.kt" \
  || fail "TUN progress fail-closed reason missing"
grep -q 'isTeardownActive' "$PROJECT/app/src/main/java/com/v2ray/ang/vpn/VpnSessionCoordinator.kt" \
  || fail "teardown barrier missing"
grep -q 'completeStopOutcome' "$PROJECT/app/src/main/java/com/v2ray/ang/core/CoreServiceManager.kt" \
  || fail "stop timeout fail-closed missing"
grep -q 'completeLateStopSuccess' "$PROJECT/app/src/main/java/com/v2ray/ang/core/CoreServiceManager.kt" \
  || fail "late core-stop success is not finalized"
grep -q 'fun vpnProtect' "$PROJECT/app/src/main/java/com/v2ray/ang/service/CoreVpnService.kt" \
  || fail "vpnProtect missing"
grep -q 'bindProcessToUnderlying' "$PROJECT/app/src/main/java/com/v2ray/ang/service/CoreVpnService.kt" \
  || fail "process bind to underlying network missing"
grep -q 'HF-VPN-012' "$PROJECT/app/src/main/java/com/v2ray/ang/service/CoreVpnService.kt" \
  || fail "loop-prevention bind failure is not fail-closed"
grep -q 'VpnRestartGate' "$PROJECT/app/src/main/java/com/v2ray/ang/core/CoreServiceManager.kt" \
  || fail "restart work is not generation-scoped"
grep -q 'tryDispatchStart' "$PROJECT/app/src/main/java/com/v2ray/ang/core/CoreServiceManager.kt" \
  || fail "restart start is not serialized with stop invalidation"
grep -q '!xrayShutdownGate.drain' "$PROJECT/app/src/main/java/com/v2ray/ang/core/CoreServiceManager.kt" \
  || fail "handover drain timeout is not fail-closed"
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
grep -q 'resolveForHandover' "$PROJECT/app/src/main/java/com/v2ray/ang/core/CoreServiceManager.kt" \
  || fail "handover AUTO re-resolution missing"
grep -q 'HotfoxServerListContract.AUTO_ROW_INDEX' "$PROJECT/app/src/main/java/com/v2ray/ang/ui/MainRecyclerAdapter.kt" \
  || fail "AUTO row is not first in the server list"
grep -q 'adapterPositionForSelection' "$PROJECT/app/src/main/java/com/v2ray/ang/ui/GroupServerFragment.kt" \
  || fail "server list scroll does not account for AUTO row"
grep -q 'Авто-выбор сервера' "$PROJECT/app/src/main/res/values-ru/strings.xml" \
  || fail "AUTO row label missing"
grep -q 'disable += "MissingTranslation"' "$PROJECT/app/build.gradle.kts" \
  || fail "MissingTranslation lint disable missing"
grep -q 'HotFox' "$PROJECT/app/src/main/res/layout/activity_main.xml" || fail "HotFox UI marker missing"
grep -q 'BACKEND_UNAVAILABLE' "$PROJECT/app/src/main/java/com/v2ray/ang/commerce/CommercialPresentationState.kt" \
  || fail "2.3 commercial presentation states missing"
grep -q 'fun isPaidProof' "$PROJECT/app/src/main/java/com/v2ray/ang/commerce/CheckoutReturnParser.kt" \
  || fail "checkout return parser missing"
grep -q 'interface SecretStore' "$PROJECT/app/src/main/java/com/v2ray/ang/commerce/SecretStore.kt" \
  || fail "SecretStore missing"
grep -q 'layout_premium_onboarding' "$PROJECT/app/src/main/res/layout/activity_main.xml" \
  || fail "Premium onboarding layout missing"
grep -q 'HotfoxManifestRefresh.restoreAfterSuccess' "$PROJECT/app/src/main/java/com/v2ray/ang/handler/AngConfigManager.kt" \
  || fail "transactional manifest restore missing"
grep -q 'fun apply' "$PROJECT/app/src/main/java/com/v2ray/ang/commerce/WebhookReconciliation.kt" \
  || fail "webhook reconciliation missing"
grep -q 'object SandboxPaymentE2e' "$PROJECT/app/src/debug/java/com/v2ray/ang/commerce/SandboxPaymentE2e.kt" \
  || fail "sandbox payment E2E orchestrator missing"
grep -q 'HOTFOX_SANDBOX_COMMERCE cannot be enabled for release builds' "$PROJECT/app/build.gradle.kts" \
  || fail "release sandbox commerce Gradle guard missing"
grep -q 'gradle.taskGraph.whenReady' "$PROJECT/app/build.gradle.kts" \
  || fail "sandbox commerce release rejection must be task-graph gated"
grep -q 'fun isXrayUsable' "$PROJECT/app/src/main/java/com/v2ray/ang/commerce/ManagedConfigParser.kt" \
  || fail "managed Xray-usable validation missing"
grep -q 'failPutsAfter' "$PROJECT/app/src/main/java/com/v2ray/ang/commerce/ManagedServerStore.kt" \
  || fail "managed replace failure injection missing"
if grep -q 'fun toProfile(' "$PROJECT/app/src/main/java/com/v2ray/ang/commerce/ManagedManifestParser.kt"; then
  fail "identity-only manifests must not synthesize profiles"
fi
if grep -q 'removeServerViaSubid' "$PROJECT/app/src/main/java/com/v2ray/ang/commerce/ManagedServerStore.kt"; then
  fail "managed replace must not delete live inventory before staging"
fi
grep -q 'class ManagedManifestApplicator' "$PROJECT/app/src/main/java/com/v2ray/ang/commerce/ManagedManifestApplicator.kt" \
  || fail "managed manifest applicator missing"
if grep -q 'SandboxCommerceBackend()' "$PROJECT/app/src/main/java/com/v2ray/ang/commerce/HotfoxCommerceFactory.kt"; then
  fail "main factory must not instantiate SandboxCommerceBackend"
fi

grep -q 'object HotfoxAutoFailover' "$PROJECT/app/src/main/java/com/v2ray/ang/vpn/HotfoxAutoFailover.kt" \
  || fail "HotfoxAutoFailover missing"
grep -q 'fun scheduleAuthorizedRestart' "$PROJECT/app/src/main/java/com/v2ray/ang/core/CoreServiceManager.kt" \
  || fail "generation-owned AUTO failover restart missing"
grep -q 'class ServerHealthRepository' "$PROJECT/app/src/main/java/com/v2ray/ang/vpn/ServerHealthRepository.kt" \
  || fail "ServerHealthRepository missing"
grep -q 'fun significantlyBetter' "$PROJECT/app/src/main/java/com/v2ray/ang/vpn/AutoSelectionPolicy.kt" \
  || fail "AUTO hysteresis missing"
grep -q 'object HotfoxRoutingPolicy' "$PROJECT/app/src/main/java/com/v2ray/ang/vpn/HotfoxRouting.kt" \
  || fail "HotFox 2.5 routing policy missing"
grep -q 'fun bypassLanOnTun' "$PROJECT/app/src/main/java/com/v2ray/ang/vpn/HotfoxRouting.kt" \
  || fail "explicit LAN TUN policy missing"
grep -q 'HotfoxRoutingStore.load().perAppPlan' "$PROJECT/app/src/main/java/com/v2ray/ang/service/CoreVpnService.kt" \
  || fail "VpnService per-app plan is not bound to HotFox routing"
grep -q 'HotfoxXrayConfigInjector.apply' "$PROJECT/app/src/main/java/com/v2ray/ang/core/CoreServiceManager.kt" \
  || fail "Xray config is not injected from HotFox routing policy"
grep -q 'fun tryApply' "$PROJECT/app/src/main/java/com/v2ray/ang/vpn/HotfoxRoutingApply.kt" \
  || fail "routing reconfiguration generation missing"
grep -q 'fun restartForRouting' "$PROJECT/app/src/main/java/com/v2ray/ang/core/CoreServiceManager.kt" \
  || fail "routing restart is not generation-scoped"
grep -q 'HotfoxRoutingRestart.tryDispatch' "$PROJECT/app/src/main/java/com/v2ray/ang/core/CoreServiceManager.kt" \
  || fail "routing restart is not bound to VpnRestartGate"
grep -q 'PREF_SMART_ROUTING_MODE' "$PROJECT/app/src/main/java/com/v2ray/ang/vpn/HotfoxRoutingStore.kt" \
  || fail "legacy smart-routing mode key is not migrated"
grep -q 'fun outsideVpnCapture' "$PROJECT/app/src/main/java/com/v2ray/ang/vpn/HotfoxRouting.kt" \
  || fail "TUN capture membership is not explicit in routing policy"
grep -q 'blocked + exact + suffix + cidr' "$PROJECT/app/src/main/java/com/v2ray/ang/vpn/HotfoxXrayRouting.kt" \
  || fail "Xray field rules are not bucketed to match decide() precedence"
grep -q 'object HotfoxShadowPolicy' "$PROJECT/app/src/main/java/com/v2ray/ang/vpn/HotfoxShadowPolicy.kt" \
  || fail "HotFox 2.6 Shadow policy missing"
grep -q 'fun fallback' "$PROJECT/app/src/main/java/com/v2ray/ang/vpn/HotfoxShadowPolicy.kt" \
  || fail "Shadow bounded fallback missing"
grep -q 'HotfoxShadowFailover.considerLive' "$PROJECT/app/src/main/java/com/v2ray/ang/service/CoreVpnService.kt" \
  || fail "connected failover is not Shadow/path aware"
grep -q 'Подбираем защищённый маршрут' "$PROJECT/app/src/main/java/com/v2ray/ang/vpn/HotfoxShadowPolicy.kt" \
  || fail "Shadow AUTO selecting label missing"
grep -q 'object ConnectionDoctor' "$PROJECT/app/src/main/java/com/v2ray/ang/vpn/ConnectionDoctor.kt" \
  || fail "Connection Doctor missing"
grep -q 'class DnsBootstrapCache' "$PROJECT/app/src/main/java/com/v2ray/ang/vpn/DnsBootstrapCache.kt" \
  || fail "DNS bootstrap cache missing"
grep -q 'ENTITLEMENT_BLOCKED' "$PROJECT/app/src/main/java/com/v2ray/ang/vpn/AutoCandidateFilter.kt" \
  || fail "AUTO eligibility filter missing"
grep -q 'fun candidateFrom' "$PROJECT/app/src/main/java/com/v2ray/ang/vpn/HotfoxServerSelection.kt" \
  || fail "AUTO candidates are not built from config/entitlement state"
grep -q 'delayNetworkScoped' "$PROJECT/app/src/main/java/com/v2ray/ang/vpn/HotfoxServerSelection.kt" \
  || fail "unscoped persisted delay ranking guard missing"
grep -q 'Подбираем сервер' "$PROJECT/app/src/main/java/com/v2ray/ang/vpn/HotfoxResolvedTargetDisplay.kt" \
  || fail "AUTO selecting label missing"
grep -q 'invalidateForNetworkChange' "$PROJECT/app/src/main/java/com/v2ray/ang/core/CoreServiceManager.kt" \
  || fail "handover does not invalidate previous-network health"
grep -q 'HotfoxLatencyDisplay.format' "$PROJECT/app/src/main/java/com/v2ray/ang/ui/MainRecyclerAdapter.kt" \
  || fail "server list does not use truthful latency labels"
if grep -q '2600L' "$PROJECT/app/src/main/java/com/v2ray/ang/ui/MainActivity.kt"; then
  fail "server ping still uses a magic 2600ms delay"
fi
if grep -q '"✕"' "$PROJECT/app/src/main/java/com/v2ray/ang/ui/MainRecyclerAdapter.kt"; then
  fail "server list still shows ✕ for dead servers"
fi

for abi in arm64-v8a armeabi-v7a x86 x86_64; do
  for lib in libhev-socks5-tunnel.so libhevsockstun.so; do
    f="$PROJECT/app/libs/$abi/$lib"
    [[ -s "$f" ]] || fail "missing/empty native library $f"
  done
done

if grep -R -I -n -E "https://nox\\.hotto-fox\\.st/|vless://[^[:space:]\"]{20,}|sk_live_[A-Za-z0-9]+|sk_test_[A-Za-z0-9]+|rk_live_[A-Za-z0-9]+|whsec_[A-Za-z0-9]+" --exclude-dir=build --exclude-dir=test --exclude-dir=androidTest --exclude="*.md" --exclude="*.txt" "$PROJECT" >/tmp/hotfox-secret-scan.txt 2>/dev/null; then
  cat /tmp/hotfox-secret-scan.txt >&2
  fail "possible personal subscription/VLESS secret found in source tree"
fi
rm -f /tmp/hotfox-secret-scan.txt

printf 'PASS HotFox 2.2.0 overlay verification\n'
