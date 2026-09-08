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
        if "addDisallowedApplication(selfPackageName)" in vpn and "bindProcessToUnderlying" not in vpn:
            fail("self-disallow without process bind would block TUN inject")
        if "bindProcessToUnderlying" not in vpn:
            fail("process must bind to underlying network so Xray does not loop into TUN")
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
        if "AUTO_ROW_INDEX" not in text and "first server-list row" not in text:
            fail("AGENTS.md does not require AUTO as the first server row")
        if "Mandatory completion loop" not in text and "Build/test loop" not in text:
            fail("AGENTS.md is missing the mandatory completion loop")
    else:
        fail("AGENTS.md missing")

    secret_re = re.compile(r"https://nox\.hotto-fox\.st/|vless://[^\s\"]{20,}")
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
