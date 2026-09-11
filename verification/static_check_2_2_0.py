#!/usr/bin/env python3
"""Static acceptance checks for HotFox Proxy 2.2.0 reconstructed source."""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PROJECT = ROOT / "V2rayNG"
failures: list[str] = []


def fail(message: str) -> None:
    failures.append(message)


def read(rel: str) -> str:
    path = PROJECT / rel
    if not path.is_file():
        fail(f"missing file: {rel}")
        return ""
    return path.read_text(encoding="utf-8", errors="replace")


def extract_balanced_block(src: str, needle: str) -> str:
    idx = src.find(needle)
    if idx < 0:
        return ""
    i = src.find("{", idx)
    if i < 0:
        return ""
    depth = 0
    for j in range(i, len(src)):
        if src[j] == "{":
            depth += 1
        elif src[j] == "}":
            depth -= 1
            if depth == 0:
                return src[i : j + 1]
    return ""


def must_contain(rel: str, needle: str, label: str | None = None) -> None:
    text = read(rel)
    if text and needle not in text:
        fail(f"{label or needle} not found in {rel}")


def main() -> int:
    gradle = read("app/build.gradle.kts")
    if gradle:
        if 'applicationId = "com.hotfox.vpn"' not in gradle:
            fail("applicationId is not com.hotfox.vpn")
        if "minSdk = 24" not in gradle:
            fail("minSdk is not 24")
        if 'versionName = "2.2.0"' not in gradle:
            fail("versionName is not 2.2.0")
        if "versionCode = 22000" not in gradle:
            fail("versionCode is not 22000")

    vpn = read("app/src/main/java/com/v2ray/ang/service/CoreVpnService.kt")
    if vpn:
        if "VpnReadiness.waitForLocalSocks" not in vpn:
            fail("CoreVpnService does not wait for local SOCKS before HEV")
        if "STARTING_HEV" not in vpn:
            fail("STARTING_HEV is missing")
        if "VERIFYING_PATH" not in vpn:
            fail("VERIFYING_PATH is missing")
        if "markConnected(" not in vpn:
            fail("CONNECTED is not published via coordinator")
        if "pathVerified" not in vpn:
            fail("CONNECTED is not gated on pathVerified")
        if "permitAll" in vpn:
            fail("CoreVpnService must not use StrictMode.permitAll")
        if "Thread.sleep(100)" in vpn:
            fail("CoreVpnService still uses Thread.sleep as stop synchronization")
        if "protect(" not in vpn and "vpnProtect" not in vpn:
            fail("VpnService.protect path missing")
        if 'addRoute("::", 0)' not in vpn:
            fail("IPv6 ::/0 capture missing")
        if 'addRoute("2000::", 3)' in vpn:
            fail("IPv6 2000::/3 LAN split omits NAT64 and is not fail-closed")
        if "HotfoxTunSelfExclusion.forPlan" not in vpn:
            fail("TUN self-exclusion is not applied from CoreVpnService")
        if "HotfoxSocketProtect.attach" not in vpn:
            fail("VpnService.protect broker is not attached")
        if "addDisallowedApplication(selfPackageName)" in vpn and "bindProcessToUnderlying" not in vpn:
            fail("self-disallow without process bind would block TUN inject")
        if "bindProcessToUnderlying" not in vpn:
            fail("process must bind to underlying network so Xray does not loop into TUN")
        if "interpretBindAttempt" not in read("app/src/main/java/com/v2ray/ang/vpn/VpnLoopPrevention.kt"):
            fail("bindProcessToNetwork Boolean result is not interpreted")
        if "HF-VPN-012" not in vpn:
            fail("loop-prevention bind failure is not fail-closed")
        if "injectThroughVpn" not in vpn:
            fail("TUN inject is not invoked from CoreVpnService")

    must_contain(
        "app/src/main/java/com/v2ray/ang/core/CoreConfigManager.kt",
        'ip = listOf("::/0")',
        "IPv6 blackhole rule",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/handler/SettingsManager.kt",
        "hotfox_hev_stability_migrated_2_0_2",
        "HEV migration",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/service/TProxyService.kt",
        'System.loadLibrary("hev-socks5-tunnel")',
        "HEV JNI",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/core/CoreServiceManager.kt",
        "waitForLocalSocksBlocking",
        "reload SOCKS wait",
    )
    manager = read("app/src/main/java/com/v2ray/ang/core/CoreServiceManager.kt")
    if manager:
        if "Thread.sleep(500L)" in manager:
            fail("CoreServiceManager still uses Thread.sleep as restart synchronization")
        if "CoroutineScope(Dispatchers.IO).launch" in manager and "stopLoop()" in manager:
            if "awaitCoreStop" not in manager:
                fail("core stop is still fire-and-forget")
        if "tryBeginReload" not in manager:
            fail("handover is not serialized against startup")
        if "XrayShutdownGate" not in manager:
            fail("core shutdown re-entry guard missing")
        if "joinExisting = isCoreStopActive()" in manager:
            fail("stop join is still sampled from the worker before the coordinator lock")
        if "!xrayShutdownGate.drain" not in manager:
            fail("handover must fail closed when old-core shutdown drain times out")
        if "VpnRestartGate" not in manager:
            fail("restart work is not generation-scoped")
        if "tryDispatchStart" not in manager:
            fail("restart start is not serialized with stop invalidation")
        if "startVService(app)" in manager:
            fail("MSG_STATE_RESTART still calls startVService after a TOCTOU isCurrent check")
        if "HF-VPN-012" not in manager:
            fail("handover loop-prevention bind failure is not fail-closed")
        if "private fun awaitCoreStop(epoch: Long, attempt: Long): Boolean" not in manager:
            fail("awaitCoreStop must report a Boolean outcome")
        if "completeStopOutcome" not in manager:
            fail("timed-out core stop is not fail-closed")
        if "completeLateStopSuccess" not in manager:
            fail("late core-stop success is not finalized")
        if "resolveForHandover" not in manager:
            fail("network handover does not re-resolve AUTO server")

    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/ConnectionUiMapper.kt",
        "WAITING_SOCKS",
        "UI mapper startup states",
    )
    main = read("app/src/main/java/com/v2ray/ang/ui/MainActivity.kt")
    if main and "isRunning && !session.isBusy()" in main:
        fail("MainActivity still promotes isRunning to Защищено")
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/VpnReadiness.kt",
        "probeSocks5",
        "SOCKS5 handshake readiness",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/VpnReadiness.kt",
        "injectThroughVpn",
        "TUN inject through VPN network",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/VpnReadiness.kt",
        "tun-not-forwarded",
        "TUN progress fail-closed reason",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/VpnSessionCoordinator.kt",
        "isTeardownActive",
        "teardown start barrier",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/VpnReadiness.kt",
        "isValidDnsReply",
        "validated DNS reply through TUN",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/VpnReadiness.kt",
        "probeHttpThroughVpn",
        "HTTP through VPN network",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/VpnSessionCoordinator.kt",
        "generationLock",
        "atomic generation-scoped mutations",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/VpnSessionCoordinator.kt",
        "completeLateStopSuccess",
        "late stop success",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/service/CoreVpnService.kt",
        "stopAllService()",
        "onDestroy full teardown",
    )
    must_contain(
        "app/src/main/res/layout/activity_main.xml",
        "HotfoxRouteBarsView",
        "connection route visualization",
    )
    must_contain(
        "app/src/main/res/layout/activity_main.xml",
        "include_hf_bottom_nav",
        "mobile-first connection nav include",
    )
    must_contain(
        "app/src/main/res/layout/include_hf_bottom_nav.xml",
        "nav_connection",
        "mobile-first connection nav",
    )
    must_contain(
        "app/src/main/res/layout/activity_main.xml",
        "HotFox",
        "editorial HotFox UI",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/HotfoxServerSelection.kt",
        "fun resolveForHandover",
        "AUTO handover re-resolution",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/ui/MainRecyclerAdapter.kt",
        "HotfoxServerListContract.AUTO_ROW_INDEX",
        "AUTO row is first in the server list",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/HotfoxServerSelection.kt",
        "fun adapterPositionForSelection",
        "AUTO adapter offset",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/ui/GroupServerFragment.kt",
        "adapterPositionForSelection",
        "scroll accounts for AUTO row",
    )
    must_contain(
        "app/src/main/res/values-ru/strings.xml",
        "Авто-выбор сервера",
        "AUTO row label",
    )

    logo = PROJECT / "app/src/main/res/drawable/hotfox_logo.xml"
    if not logo.is_file() or logo.stat().st_size == 0:
        fail("drawable/hotfox_logo.xml missing")

    agents = ROOT / "AGENTS.md"
    if agents.is_file():
        text = agents.read_text(encoding="utf-8", errors="replace")
        if "vertical navigation" in text:
            fail("AGENTS.md still requires vertical/rail navigation")
        if "bottom bar" not in text and "bottom nav" not in text and "bottom destinations" not in text:
            fail("AGENTS.md does not record the phone bottom navigation")
        if "first server-list row" not in text:
            fail("AGENTS.md does not require AUTO as the first server row")
        if "Mandatory completion loop" not in text and "Build/test loop" not in text:
            fail("AGENTS.md is missing the mandatory completion loop")
    else:
        fail("AGENTS.md missing")

    must_contain(
        "app/src/main/java/com/v2ray/ang/commerce/CommercialPresentationState.kt",
        "BACKEND_UNAVAILABLE",
        "2.3 commercial presentation states",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/commerce/CheckoutReturnParser.kt",
        "fun isPaidProof",
        "checkout return is never payment proof",
    )
    checkout = read("app/src/main/java/com/v2ray/ang/commerce/CheckoutReturnParser.kt")
    if checkout and "fun isPaidProof" in checkout and "false" not in checkout:
        fail("isPaidProof must remain unconditionally false")
    must_contain(
        "app/src/main/java/com/v2ray/ang/commerce/SecretStore.kt",
        "interface SecretStore",
        "Keystore-backed SecretStore",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/commerce/ManifestRefreshPolicy.kt",
        "fun shouldCommitSwap",
        "transactional manifest swap",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/commerce/UnavailableCommerceBackend.kt",
        "manualImportAllowed",
        "manual import when commercial backend is down",
    )
    must_contain(
        "app/src/main/res/layout/activity_main.xml",
        "layout_premium_onboarding",
        "HotFox Premium no-access onboarding",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/commerce/WebhookReconciliation.kt",
        "fun apply",
        "webhook reconciliation",
    )
    must_contain(
        "app/src/debug/java/com/v2ray/ang/commerce/SandboxPaymentE2e.kt",
        "object SandboxPaymentE2e",
        "sandbox payment E2E orchestrator",
    )
    gradle = read("app/build.gradle.kts")
    if gradle and "HOTFOX_SANDBOX_COMMERCE cannot be enabled for release builds" not in gradle:
        fail("release sandbox commerce Gradle guard missing")
    if gradle and "gradle.taskGraph.whenReady" not in gradle:
        fail("sandbox commerce release rejection must be gated on the task graph")
    if gradle:
        build_types = extract_balanced_block(gradle, "buildTypes")
        release_block = extract_balanced_block(build_types, "release {")
        if "GradleException" in release_block or "throw " in release_block:
            fail("release buildType must not throw when HOTFOX_SANDBOX_COMMERCE is set")
        if 'buildConfigField("boolean", "HOTFOX_SANDBOX_COMMERCE", "false")' not in release_block:
            fail("release must force HOTFOX_SANDBOX_COMMERCE false")
        debug_block = extract_balanced_block(build_types, "debug {")
        if "HOTFOX_SANDBOX_COMMERCE" not in debug_block:
            fail("debug must honor HOTFOX_SANDBOX_COMMERCE")
    if gradle and 'src/main' in gradle and "SandboxCommerceBackend" in read(
        "app/src/main/java/com/v2ray/ang/commerce/HotfoxCommerceFactory.kt"
    ):
        fail("release factory must not reference SandboxCommerceBackend")
    factory = read("app/src/main/java/com/v2ray/ang/commerce/HotfoxCommerceFactory.kt")
    if factory and "SandboxCommerceBackend()" in factory:
        fail("main HotfoxCommerceFactory must not instantiate SandboxCommerceBackend")
    release_stub = read("app/src/release/java/com/v2ray/ang/commerce/HotfoxDebugCommerce.kt")
    if release_stub and "SandboxCommerceBackend" in release_stub:
        fail("release HotfoxDebugCommerce must not reference SandboxCommerceBackend")
    must_contain(
        "app/src/main/java/com/v2ray/ang/commerce/ManagedManifestApplicator.kt",
        "class ManagedManifestApplicator",
        "managed manifest applicator",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/commerce/ManagedConfigParser.kt",
        "fun isXrayUsable",
        "managed configs must be Xray-usable",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/commerce/ManagedServerStore.kt",
        "failPutsAfter",
        "managed replace failure injection",
    )
    parser = read("app/src/main/java/com/v2ray/ang/commerce/ManagedManifestParser.kt")
    if parser and "fun toProfile(" in parser:
        fail("identity-only manifests must not synthesize ProfileItem/VLESS profiles")
    applicator = read("app/src/main/java/com/v2ray/ang/commerce/ManagedManifestApplicator.kt")
    if applicator and "ManagedManifestParser.toProfile" in applicator:
        fail("applicator must not persist identity-only managed profiles")
    store = read("app/src/main/java/com/v2ray/ang/commerce/ManagedServerStore.kt")
    if store and "EConfigType.VLESS" in store:
        fail("managed store must not synthesize VLESS from identity fields")
    if store and "removeServerViaSubid" in store:
        fail("managed replace must not delete live inventory before staging")

    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/HotfoxAutoFailover.kt",
        "object HotfoxAutoFailover",
        "2.4 AUTO failover policy",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/core/CoreServiceManager.kt",
        "fun scheduleAuthorizedRestart",
        "generation-owned AUTO failover restart",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/AutoSelectionPolicy.kt",
        "fun significantlyBetter",
        "AUTO hysteresis thresholds",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/ServerHealthRepository.kt",
        "fun invalidateForNetworkChange",
        "2.4 network-context health invalidation",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/AutoCandidateFilter.kt",
        "ENTITLEMENT_BLOCKED",
        "2.4 AUTO eligibility filter",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/HotfoxServerSelection.kt",
        "fun candidateFrom",
        "AUTO candidates built from config and entitlement state",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/HotfoxServerSelection.kt",
        "delayNetworkScoped",
        "unscoped persisted delay must not rank as current-network health",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/HotfoxRouting.kt",
        "fun bypassLanOnTun",
        "2.5 explicit LAN TUN policy",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/HotfoxRoutingDataPlane.kt",
        "object HotfoxRoutingDataPlane",
        "2.5 routing data-plane projection",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/HotfoxRoutingDataPlane.kt",
        "IPV6_FAIL_CLOSED_ROUTES",
        "IPv6 TUN capture is fail-closed ::/0 including NAT64",
    )
    must_contain(
        "app/src/test/java/com/v2ray/ang/vpn/HotfoxRoutingTest.kt",
        "64:ff9b::1",
        "IPv6 NAT64 stays captured when LAN access is enabled",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/HotfoxRouting.kt",
        "RoutingRuleKind.APP, RoutingRuleKind.LAN -> null",
        "unsupported APP/LAN custom rules are rejected at sanitize",
    )
    vpn_routing = read("app/src/main/java/com/v2ray/ang/service/CoreVpnService.kt")
    if vpn_routing and "SettingsManager.routingRulesetsBypassLan()" in vpn_routing:
        fail("CoreVpnService must not infer LAN bypass from geosite presets")
    must_contain(
        "app/src/main/java/com/v2ray/ang/core/CoreServiceManager.kt",
        "HotfoxXrayConfigInjector.apply",
        "Xray routing injection from HotFox policy",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/core/CoreServiceManager.kt",
        "HotfoxXrayConfigValidator.requireValid",
        "final Xray config rejects dangling outbound/balancer tags",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/HotfoxOutboundSnapshot.kt",
        "isContainerConfigType",
        "CUSTOM/POLICYGROUP/PROXYCHAIN are not compared as network protocols",
    )
    must_contain(
        "app/src/test/java/com/v2ray/ang/vpn/HotfoxRuntimeRepairTest.kt",
        "includeDoesNotPutSelfOnAllowList",
        "runtime-repair regression: self stays off TUN in INCLUDE",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/HotfoxRoutingApply.kt",
        "fun tryApply",
        "routing reconfiguration generation",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/core/CoreServiceManager.kt",
        "fun restartForRouting",
        "routing restart is generation-scoped",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/core/CoreServiceManager.kt",
        "HotfoxRoutingRestart.tryDispatch",
        "routing restart is bound to VpnRestartGate",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/HotfoxRoutingStore.kt",
        "PREF_SMART_ROUTING_MODE",
        "legacy smart-routing mode is migrated",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/HotfoxRouting.kt",
        "fun outsideVpnCapture",
        "TUN capture membership is explicit",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/HotfoxXrayRouting.kt",
        "blocked + exact + suffix + cidr",
        "Xray rules are bucketed to match decide() precedence",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/HotfoxXrayRouting.kt",
        "funnelsCapturedTrafficByApp",
        "app-split modes must not emit non-block DOMAIN/CIDR to Xray",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/HotfoxRouting.kt",
        "fun funnelsCapturedTrafficByApp",
        "APP precedence is explicit for INCLUDE/EXCLUDE",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/ui/MainActivity.kt",
        "HotfoxRoutingStore.load()",
        "routing UI is derived from the active policy snapshot",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/HotfoxResolvedTargetDisplay.kt",
        "Подбираем сервер",
        "2.4 truthful AUTO selecting label",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/HotfoxShadowPolicy.kt",
        "fun fallback",
        "2.6 Shadow bounded fallback",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/service/CoreVpnService.kt",
        "HotfoxShadowFailover.considerLive",
        "VPN failover uses Shadow/path policy",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/HotfoxShadowPolicy.kt",
        "Подбираем защищённый маршрут",
        "Shadow AUTO selecting label",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/ConnectionDoctor.kt",
        "object ConnectionDoctor",
        "2.6 Connection Doctor",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/DnsBootstrapCache.kt",
        "class DnsBootstrapCache",
        "2.6 DNS bootstrap cache",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/ops/HotfoxUpdate.kt",
        "object HotfoxUpdatePolicy",
        "2.7 sideload update policy",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/ops/HotfoxUpdate.kt",
        "fun maySilentlyInstall",
        "2.7 no silent APK install",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/ops/HotfoxNodeDrain.kt",
        "object HotfoxNodeDrain",
        "2.7 signed node drain",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/AutoCandidateFilter.kt",
        "DRAINED",
        "AUTO excludes drained nodes",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/HotfoxAutopilot.kt",
        "object ConnectionIntentEngine",
        "2.8 ConnectionIntentEngine",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/HotfoxAutopilot.kt",
        "WAIT_FOR_CAPTIVE_PORTAL",
        "2.8 captive portal intent",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/core/CoreServiceManager.kt",
        "startVServiceFromAutopilot",
        "Autopilot start uses VpnRestartGate",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/HotfoxAutopilotBootReceiver.kt",
        "class HotfoxAutopilotBootReceiver",
        "2.8 boot Autopilot receiver",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/HotfoxAutopilotPauseReceiver.kt",
        "class HotfoxAutopilotPauseReceiver",
        "2.8 timed pause resume receiver",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/HotfoxAutopilot.kt",
        "fun expiryEpochMs",
        "2.8 pause expiry is alarm-driven",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/HotfoxXrayCapability.kt",
        "object HotfoxXrayCapability",
        "2.9 Xray capability is package-independent",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/VpnReadiness.kt",
        "fun probeHttpProxy",
        "2.9 HTTP inbound probed separately from SOCKS",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/VpnProtectEvidence.kt",
        "object VpnProtectEvidence",
        "2.9 protect runtime evidence",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/VpnReadiness.kt",
        "fun probeSocksHttps204",
        "2.9 SOCKS HTTPS probed independently of HTTP inbound",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/HotfoxLocalHttpProxyPolicy.kt",
        "object HotfoxLocalHttpProxyPolicy",
        "2.9 dead HTTP proxy is not a subscription readiness mechanism",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/HotfoxOutboundSnapshot.kt",
        "fun fromGeneratedJson",
        "2.9 generated Xray outbound snapshot",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/service/CoreVpnService.kt",
        "HF-VPN-014",
        "SOCKS outbound isolation is fail-closed before HEV",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/service/HotfoxHealthMonitor.kt",
        "probeSocksHttps204",
        "connected health probes SOCKS outbound not HTTP inbound",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/HotfoxImportUiRefresh.kt",
        "object HotfoxImportUiRefresh",
        "2.9 import UI refresh without process restart",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/core/CoreServiceManager.kt",
        "HEV SOCKS target drifted",
        "HEV SOCKS target must match Xray inbound",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/HotfoxSubscriptionTitle.kt",
        "object HotfoxSubscriptionTitle",
        "2.9 subscription title is not the secret URL",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/TunFdEvidence.kt",
        "object TunFdEvidence",
        "2.9 TUN fd lifetime evidence",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/HotfoxTunLayerEvidence.kt",
        "object HotfoxTunLayerEvidence",
        "2.9 TUN HTTP and DNS layers are recorded separately",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/service/CoreVpnService.kt",
        "PREF_APPEND_HTTP_PROXY, false",
        "Android HTTP proxy is opt-in; HEV uses SOCKS",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/HotfoxPath.kt",
        "fun addressesForProbe",
        "2.9 TUN HTTPS isolates IPv4 from IPv6",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/HotfoxTunLayerEvidence.kt",
        "tunHttp4",
        "2.9 TUN HTTP IPv4 evidence",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/HotfoxOutboundSnapshot.kt",
        "BLOCKING_PREFIXES",
        "2.9 generated Reality/network drift is fail-closed",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/HotfoxOutboundSnapshot.kt",
        "fun normalizeNetwork",
        "tcp and raw are the same stream network",
    )
    must_contain(
        "app/src/debug/java/com/v2ray/ang/vpn/HotfoxEngineeringRuntimeE2e.kt",
        "object HotfoxEngineeringRuntimeE2e",
        "2.9 engineering-runtime VPN E2E harness",
    )
    e2e_workflow = (ROOT / ".github/workflows/hotfox-vpn-e2e.yml").read_text(encoding="utf-8")
    if "ci_run_vpn_e2e.sh" not in e2e_workflow:
        fail("VPN E2E workflow must invoke ci_run_vpn_e2e.sh")
    if "Connect/IP probes are not implemented yet" in e2e_workflow:
        fail("VPN E2E workflow is still a placeholder")
    if "install_hotfox_android_sdk.sh" not in e2e_workflow:
        fail("VPN E2E workflow must install compileSdk 37 via install_hotfox_android_sdk.sh")
    sdk_install = (ROOT / ".github/scripts/install_hotfox_android_sdk.sh").read_text(
        encoding="utf-8"
    )
    if not re.search(r'"platforms;android-37"', sdk_install):
        fail("SDK install must request platforms;android-37 for compileSdk 37")
    if not re.search(r"platforms/android-37(?!\.0)", sdk_install):
        fail("SDK install must assert platforms/android-37")
    if "build-tools;37.0.0" not in sdk_install:
        fail("SDK install must keep build-tools;37.0.0 separate from the platform package")
    if sdk_install.count("sdkmanager --channel=3") < 3:
        fail("SDK install must pass --channel=3 to sdkmanager for API 37 packages")
    bootstrap = (ROOT / "bootstrap/bootstrap_source.sh").read_text(encoding="utf-8")
    if "apply_hotfox_android_manifest.py" not in bootstrap:
        fail("bootstrap must patch AndroidManifest for Autopilot boot receiver")
    if "apply_hotfox_utils_isxray.py" not in bootstrap:
        fail("bootstrap must patch Utils.isXray for HotFox package")
    gradle = read("app/build.gradle.kts")
    if "HOTFOX_REQUIRE_RELEASE_SIGNING" not in gradle:
        fail("honest release-signing failure gate missing")
    if "HOTFOX_GIT_SHA" not in gradle:
        fail("git SHA BuildConfig field missing")
    workflow = (ROOT / ".github/workflows/hotfox-bootstrap-ci.yml").read_text(encoding="utf-8")
    if "Record APK SHA-256" not in workflow or "sha256sum" not in workflow:
        fail("CI must record APK SHA-256")
    must_contain(
        "app/src/main/java/com/v2ray/ang/core/CoreServiceManager.kt",
        "invalidateForNetworkChange",
        "handover invalidates previous-network health",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/HotfoxLatencyDisplay.kt",
        "Недоступен",
        "truthful dead-server latency label",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/ui/MainRecyclerAdapter.kt",
        "HotfoxLatencyDisplay.format",
        "server list uses truthful latency labels",
    )
    adapter = read("app/src/main/java/com/v2ray/ang/ui/MainRecyclerAdapter.kt")
    if adapter and '"✕"' in adapter:
        fail("server list must not show ✕ for dead servers")
    main_activity = read("app/src/main/java/com/v2ray/ang/ui/MainActivity.kt")
    if main_activity and "2600L" in main_activity:
        fail("server ping must not use a magic 2600ms delay")

    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/ConnectionUiMapper.kt",
        "Headline.SELECTING",
        "3.0 selecting headline",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/ConnectionUiMapper.kt",
        "Headline.VERIFYING",
        "3.0 verifying headline",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/ConnectionErrorUiMapper.kt",
        "diagnosticCode",
        "3.0 error mapper diagnostic code",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/NotificationUiMapper.kt",
        "isProtected",
        "3.0 notification mapper",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/QsTileUiMapper.kt",
        "Appearance.ACTIVE",
        "3.0 QS tile mapper",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/HotfoxOnboarding.kt",
        "VPN_PERMISSION",
        "3.0 onboarding VPN permission step",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/HotfoxMotion.kt",
        "fun reducedMotion",
        "3.0 reduced-motion helper",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/HotfoxPremiumViewState.kt",
        "fun fixtureLine",
        "3.0 screenshot/golden fixtures",
    )
    must_contain(
        "app/src/main/res/values/strings.xml",
        "hotfox_headline_selecting",
        "selecting string resource",
    )
    must_contain(
        "app/src/main/res/values-ru/strings.xml",
        "Подбираем маршрут",
        "Russian selecting headline",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/handler/NotificationManager.kt",
        "NotificationUiMapper.from",
        "foreground notification uses session mapper",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/handler/NotificationManager.kt",
        "import android.app.NotificationManager as AndroidNotificationManager",
        "Android NotificationManager must be aliased to avoid object name collision",
    )
    notification_manager = read("app/src/main/java/com/v2ray/ang/handler/NotificationManager.kt")
    if re.search(r"^import android\.app\.NotificationManager\s*$", notification_manager, re.M):
        fail("unaliased android.app.NotificationManager import collides with object NotificationManager")
    must_contain(
        "app/src/main/java/com/v2ray/ang/service/QSTileService.kt",
        "QsTileUiMapper.from",
        "QS tile uses session mapper",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/ui/MainActivity.kt",
        "HotfoxOnboardingStore.shouldPrompt",
        "first-run onboarding launch",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/ui/HotfoxAutopilotActivity.kt",
        "HotfoxAutopilotStore",
        "dedicated Autopilot surface",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/ui/HotfoxRoutingPrivacyActivity.kt",
        "HotfoxRoutingStore",
        "dedicated routing/privacy surface",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/HotfoxRouteBarsView.kt",
        "IMPORTANT_FOR_ACCESSIBILITY_NO",
        "route bars are decorative for TalkBack",
    )
    qs_tile = read("app/src/main/java/com/v2ray/ang/service/QSTileService.kt")
    if qs_tile and "shouldStartOnClick" in qs_tile and "shouldStopOnClick" in qs_tile:
        pass
    else:
        fail("QS tile click must be gated by QsTileUiMapper")
    if "HotfoxOnboardingActivity" not in (ROOT / "bootstrap/apply_hotfox_android_manifest.py").read_text(encoding="utf-8"):
        fail("AndroidManifest patch must register HotfoxOnboardingActivity")

    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/HotfoxEngineFacade.kt",
        "object HotfoxEngineFacade",
        "3.1 engine/UI facade",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/ui/MainActivity.kt",
        "HotfoxEngineFacade.snapshot",
        "MainActivity observes engine via facade",
    )
    main_activity_31 = read("app/src/main/java/com/v2ray/ang/ui/MainActivity.kt")
    if "VpnSessionCoordinator." in main_activity_31:
        fail("MainActivity must not call VpnSessionCoordinator directly")
    if "markConnected" in main_activity_31 or "beginAttempt" in main_activity_31:
        fail("MainActivity must not mint or complete VPN sessions")
    facade = read("app/src/main/java/com/v2ray/ang/vpn/HotfoxEngineFacade.kt")
    if "fun markConnected" in facade or "fun beginAttempt" in facade or "fun setState" in facade:
        fail("HotfoxEngineFacade must remain read-only")
    must_contain(
        "app/src/main/java/com/v2ray/ang/ops/HotfoxControlPlane.kt",
        "verifyEcdsaP256",
        "3.1 signed control plane",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/ops/HotfoxControlPlane.kt",
        "STALE_GRACE_MS",
        "3.1 control-plane last-known-good TTL",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/AutoSelectionPolicy.kt",
        "capacityPenalty",
        "3.1 capacity-aware AUTO",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/AutoCandidateFilter.kt",
        "MAINTENANCE",
        "3.1 AUTO excludes maintenance nodes",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/ops/HotfoxDeviceRegistry.kt",
        "generateDeviceId",
        "3.1 generated device ids",
    )
    device_registry = read("app/src/main/java/com/v2ray/ang/ops/HotfoxDeviceRegistry.kt")
    if "ANDROID_ID" in device_registry or "TELEPHONY" in device_registry or "getSerial" in device_registry:
        fail("device registry must not use hardware identifiers")
    must_contain(
        "app/src/main/java/com/v2ray/ang/ops/HotfoxApiCompatibility.kt",
        "CLIENT_TOO_OLD",
        "3.1 API version fail-graceful",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/ops/HotfoxOfflinePolicy.kt",
        "fabricateEntitlementOnBillingOutage",
        "3.1 billing outage does not fabricate entitlement",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/ops/HotfoxPrivacyTelemetry.kt",
        "SHADOW_FALLBACK",
        "3.1 privacy-safe observability",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/ops/HotfoxStateMigration.kt",
        "fun migrate",
        "3.1 2.x state migration",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/ops/HotfoxServiceHealth.kt",
        "vpn.fake_connected",
        "remote flags cannot fake CONNECTED",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/handler/NotificationManager.kt",
        "HotfoxEngineFacade.currentState",
        "notification observes facade, not a second session owner",
    )

    skip_dirs = {"build", "test", "androidTest", "debug"}
    celestial_needles = ("hf_fox_planet", "hf_shadow_orbits")
    for path in (PROJECT / "app" / "src" / "main").rglob("*"):
        if not path.is_file():
            continue
        if path.suffix.lower() not in {".kt", ".java", ".xml"}:
            continue
        if path.name.startswith("hf_fox_planet") or "hf_shadow_orbits" in path.name:
            continue
        try:
            text = path.read_text(encoding="utf-8", errors="ignore")
        except OSError:
            continue
        for needle in celestial_needles:
            if needle in text:
                fail(f"forbidden celestial production ref {needle} in {path.relative_to(PROJECT)}")
        if "HotfoxUiScreenshotHarness" in text or "HotfoxUiScreenshotScenario" in text:
            fail(f"screenshot harness leaked into main source: {path.relative_to(PROJECT)}")

    secret_re = re.compile(
        r"https://nox\.hotto-fox\.st/|vless://[^\s\"]{20,}|"
        r"sk_live_[A-Za-z0-9]+|sk_test_[A-Za-z0-9]+|rk_live_[A-Za-z0-9]+|"
        r"whsec_[A-Za-z0-9]+|yookassa[_-]?secret",
        re.IGNORECASE,
    )
    skip_dirs = {"build", "test", "androidTest"}
    for path in PROJECT.rglob("*"):
        if not path.is_file():
            continue
        if any(part in skip_dirs for part in path.parts):
            continue
        if path.suffix.lower() in {".md", ".txt", ".so", ".aar", ".apk"}:
            continue
        try:
            text = path.read_text(encoding="utf-8", errors="ignore")
        except OSError:
            continue
        if secret_re.search(text):
            fail(f"possible embedded subscription/VLESS secret in {path.relative_to(PROJECT)}")

    if failures:
        print("FAIL static_check_2_2_0")
        for item in failures:
            print(f" - {item}")
        return 1
    print("PASS static_check_2_2_0")
    return 0


if __name__ == "__main__":
    sys.exit(main())
