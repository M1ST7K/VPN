package com.v2ray.ang.vpn

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HotfoxDiagnosticsBuilderTest {
    @Before
    fun reset() {
        VpnSessionCoordinator.resetForTests()
        HotfoxAutopilotStore.resetForTests()
        VpnProtectEvidence.resetForTests()
        HotfoxSocksIsolation.resetForTests()
        HotfoxOutboundCompare.resetForTests()
        TunFdEvidence.resetForTests()
    }

    @Test
    fun reportsUnknownInsteadOfInferringFromProtectedState() {
        val attempt = VpnSessionCoordinator.beginAttempt()
        assertTrue(VpnSessionCoordinator.markConnected(attempt, pathVerified = true))
        val report = HotfoxDiagnosticsBuilder.build(
            androidRelease = "14",
            api = 34,
            abi = "arm64-v8a",
            socksPort = 10808,
            socksReady = null,
            hevRunning = null,
            ipv4Captured = null,
            ipv6Captured = null,
            ipv6Policy = "fail-closed-blackhole",
            routingMode = "smart",
            serverRemark = "Amsterdam",
            uploaded = 1L,
            downloaded = 2L,
            lastError = null,
            serverCount = 3,
            path = null,
        )
        assertTrue(report.contains("hev=unknown"))
        assertTrue(report.contains("ipv4Captured=unknown"))
        assertTrue(report.contains("ipv6Captured=unknown"))
        assertTrue(report.contains("autoCandidates="))
        assertTrue(report.contains("autoEligible="))
        assertTrue(report.contains("autoLastGood=none"))
        assertTrue(report.contains("e2e=${VpnPathVerification.PHYSICAL_E2E_NOT_EXECUTED}"))
        assertTrue(report.contains("channel="))
        assertTrue(report.contains("gitSha="))
        assertTrue(report.contains("artifact="))
        assertTrue(report.contains("autopilot="))
        assertTrue(report.contains("protectCalled="))
        assertTrue(report.contains("bindAttempted="))
        assertTrue(report.contains("tunEstablished="))
        assertTrue(report.contains("hevReceivedFd="))
        assertTrue(report.contains("socksHttps=none") || report.contains("socksHttps="))
        assertFalse(report.contains("hev=true"))
        assertFalse(Regex("ipv6Captured=true").containsMatchIn(report))
    }

    @Test
    fun redactsSubscriptionUrlsAndTokens() {
        val report = HotfoxDiagnosticsBuilder.build(
            androidRelease = "14",
            api = 34,
            abi = "arm64-v8a",
            socksPort = 10808,
            socksReady = true,
            hevRunning = true,
            ipv4Captured = null,
            ipv6Captured = null,
            ipv6Policy = "proxy",
            routingMode = "smart",
            serverRemark = "https://provider.example/sub/sUq75ktDSV7LmQuB",
            uploaded = null,
            downloaded = null,
            lastError = "vless://11111111-2222-3333-4444-555555555555@vpn.example:443",
            serverCount = 1,
            path = VpnPathVerification(
                socks5Ready = true,
                hevAlive = true,
                xrayEgressMs = 12L,
                tunEstablished = true,
                backend = VpnPathVerification.BACKEND_HEV,
                verified = true,
            ),
        )
        assertFalse(report.contains("sUq75ktDSV7LmQuB"))
        assertFalse(report.contains("11111111-2222-3333-4444-555555555555"))
        assertTrue(report.contains("pathVerified=true"))
    }

    @Test
    fun reportsConfiguredInboundPortsNotProductDefaults() {
        val report = HotfoxDiagnosticsBuilder.build(
            androidRelease = "14",
            api = 34,
            abi = "arm64-v8a",
            socksPort = 11808,
            socksReady = true,
            hevRunning = false,
            ipv4Captured = null,
            ipv6Captured = null,
            ipv6Policy = "fail-closed-blackhole",
            routingMode = "smart",
            serverRemark = "Amsterdam",
            uploaded = null,
            downloaded = null,
            lastError = null,
            serverCount = 1,
            path = null,
            httpPort = 11809,
        )
        assertTrue(report.contains("socksPort=11808"))
        assertTrue(report.contains("httpPort=11809"))
        assertFalse(report.contains("socksPort=10808"))
        assertFalse(report.contains("httpPort=10809"))
    }
}
