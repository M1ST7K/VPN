package com.v2ray.ang.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HotfoxVpnRecoveryTest {
    @Before
    fun reset() {
        VpnProtectEvidence.resetForTests()
    }

    @Test
    fun hotfoxPackageSelectsXrayCoreUnlikeLegacyBrandingCheck() {
        assertTrue(HotfoxXrayCapability.isXrayCore("com.hotfox.vpn"))
        assertTrue(HotfoxXrayCapability.isXrayCore("com.hotfox.vpn.debug"))
        assertTrue(HotfoxXrayCapability.isXrayCore("com.v2ray.ang"))
        assertTrue("com.hotfox.vpn".startsWith("com.v2ray.ang").not())
    }

    @Test
    fun socksPassHttpFailDoesNotMeanOutboundBroken() {
        val pair = HotfoxInboundIsolation.ProbePair(socksPass = true, httpPass = false)
        assertTrue(pair.outboundMayBeHealthy)
        assertTrue(pair.httpInboundOptionalFailure)
        assertEquals(10808, HotfoxInboundIsolation.SOCKS_PORT)
        assertEquals(10809, HotfoxInboundIsolation.HTTP_PORT)
    }

    @Test
    fun protectEvidenceRecordsRuntimeResult() {
        assertEquals("none", VpnProtectEvidence.summary().substringAfter("lastProtect="))
        VpnProtectEvidence.record(true)
        VpnProtectEvidence.record(false)
        assertEquals(2, VpnProtectEvidence.callCount)
        assertEquals(1, VpnProtectEvidence.successCount)
        assertEquals(false, VpnProtectEvidence.lastSuccess)
        assertTrue(VpnProtectEvidence.summary().contains("protectCalled=2"))
    }

    @Test
    fun outboundSnapshotRedactsCredentials() {
        assertEquals("[REDACTED]", HotfoxOutboundSanitizer.redactSecret("uuid-or-token"))
        assertEquals("", HotfoxOutboundSanitizer.redactSecret(""))
        assertFalse(HotfoxOutboundSanitizer.present(null))
        val snap = HotfoxOutboundSnapshot(
            protocol = "vless",
            address = "vpn.example",
            port = 443,
            network = "tcp",
            security = "reality",
            flow = "xtls-rprx-vision",
            sni = "www.example.com",
            fingerprint = "chrome",
            alpn = "h2",
            reality = true,
            publicKeyPresent = true,
            shortIdPresent = true,
            path = "/x",
            host = "www.example.com",
            serviceName = "",
            xhttp = false,
            grpc = false,
            packetEncoding = "xudp",
            mux = false,
            ipv4 = true,
            ipv6 = false,
        )
        val text = snap.lines().joinToString("\n")
        assertTrue(text.contains("publicKey=[REDACTED]"))
        assertTrue(text.contains("shortId=[REDACTED]"))
        assertFalse(text.contains("uuid"))
        assertTrue(text.contains("protocol=vless"))
    }
}
