package com.v2ray.ang.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.ServerSocket
import kotlin.concurrent.thread

class HotfoxVpnRecoveryTest {
    @Before
    fun reset() {
        VpnProtectEvidence.resetForTests()
        HotfoxSocksIsolation.resetForTests()
        HotfoxOutboundCompare.resetForTests()
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
    fun socksHttpsPassHttpFailIsNotTunFailure() {
        val result = HotfoxSocksIsolation.classify(
            socksHandshake = true,
            socksHttps = true,
            socksHttpsMs = 40L,
            httpInbound = false,
            tunPresent = false,
            hevPresent = false,
        )
        assertTrue(result.outboundMayBeHealthy)
        assertTrue(result.httpInboundOptionalFailure)
        assertFalse(result.doNotBlameTunYet)
        assertTrue(result.socksOnlyPathProven)
    }

    @Test
    fun socksHttpsFailMustNotBlameTun() {
        val result = HotfoxSocksIsolation.classify(
            socksHandshake = true,
            socksHttps = false,
            socksHttpsMs = null,
            httpInbound = false,
            tunPresent = true,
            hevPresent = false,
        )
        assertTrue(result.doNotBlameTunYet)
        assertFalse(result.outboundMayBeHealthy)
        assertFalse(result.socksOnlyPathProven)
    }

    @Test
    fun deadHttpInboundIsNotUsedAsSubscriptionProxy() {
        assertEquals(0, HotfoxLocalHttpProxyPolicy.resolvePort(10809, inboundReady = false))
        assertEquals(10809, HotfoxLocalHttpProxyPolicy.resolvePort(10809, inboundReady = true))
        assertEquals(0, HotfoxLocalHttpProxyPolicy.resolvePort(0, inboundReady = true))
    }

    @Test
    fun protectEvidenceRecordsRuntimeResult() {
        assertEquals("none", VpnProtectEvidence.summary().substringAfter("lastProtect=").substringBefore(" "))
        VpnProtectEvidence.record(true)
        VpnProtectEvidence.record(false)
        VpnProtectEvidence.recordBind(true, "WIFI")
        assertEquals(2, VpnProtectEvidence.callCount)
        assertEquals(1, VpnProtectEvidence.successCount)
        assertEquals(false, VpnProtectEvidence.lastSuccess)
        assertTrue(VpnProtectEvidence.bindAttempted)
        assertTrue(VpnProtectEvidence.bindSuccess)
        assertEquals("WIFI", VpnProtectEvidence.lastUnderlyingTransport)
        assertTrue(VpnProtectEvidence.summary().contains("protectCalled=2"))
        assertTrue(VpnProtectEvidence.summary().contains("bindOk=true"))
        assertTrue(VpnProtectEvidence.summary().contains("underlyingTransport=WIFI"))
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

    @Test
    fun generatedConfigCompareRedactsUuidAndDetectsPortDrift() {
        val profile = com.v2ray.ang.dto.entities.ProfileItem.create(com.v2ray.ang.enums.EConfigType.VLESS).apply {
            remarks = "Amsterdam"
            server = "vpn.example"
            serverPort = "443"
            network = "tcp"
            security = "reality"
            password = "11111111-2222-3333-4444-555555555555"
            flow = "xtls-rprx-vision"
            sni = "www.example.com"
            fingerPrint = "chrome"
            publicKey = "public-key-material"
            shortId = "abcd"
        }
        val fromProfile = HotfoxOutboundCompare.fromProfile(profile)
        assertEquals("vless", fromProfile.protocol)
        assertEquals("vpn.example", fromProfile.address)
        assertEquals(443, fromProfile.port)
        assertTrue(fromProfile.reality)
        assertTrue(fromProfile.publicKeyPresent)
        val generated = HotfoxOutboundCompare.fromGeneratedJson(
            """
            {
              "inbounds": [{"protocol":"socks","port":10808}],
              "outbounds": [
                {
                  "protocol": "freedom",
                  "tag": "direct"
                },
                {
                  "protocol": "vless",
                  "settings": {
                    "vnext": [{
                      "address": "vpn.example",
                      "port": 8443,
                      "users": [{"id":"11111111-2222-3333-4444-555555555555","encryption":"none","flow":"xtls-rprx-vision"}]
                    }]
                  },
                  "streamSettings": {
                    "network": "tcp",
                    "security": "reality",
                    "realitySettings": {
                      "serverName": "www.example.com",
                      "fingerprint": "chrome",
                      "publicKey": "public-key-material",
                      "shortId": "abcd"
                    }
                  }
                }
              ]
            }
            """.trimIndent(),
        )
        assertTrue(generated != null)
        val diffs = HotfoxOutboundCompare.mismatches(fromProfile, generated!!)
        assertTrue(diffs.any { it.startsWith("port:") })
        val recorded = HotfoxOutboundCompare.Result(fromProfile, generated, diffs)
        assertTrue(recorded.blockingMismatch)
        val text = generated.lines().joinToString("\n")
        assertFalse(text.contains("11111111-2222-3333-4444-555555555555"))
        assertTrue(text.contains("publicKey=[REDACTED]"))
    }

    @Test
    fun generatedConfigMatchingProfileIsNotBlocking() {
        val profile = com.v2ray.ang.dto.entities.ProfileItem.create(com.v2ray.ang.enums.EConfigType.VLESS).apply {
            remarks = "Frankfurt"
            server = "vpn.example"
            serverPort = "443"
            network = "tcp"
            security = "reality"
            password = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
            sni = "www.example.com"
            publicKey = "pk"
            shortId = "ab"
        }
        val generated = HotfoxOutboundCompare.fromGeneratedJson(
            """
            {
              "outbounds": [{
                "protocol": "vless",
                "settings": {"vnext":[{"address":"vpn.example","port":443,"users":[{"id":"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"}]}]},
                "streamSettings": {
                  "network": "tcp",
                  "security": "reality",
                  "realitySettings": {"serverName":"www.example.com","publicKey":"pk","shortId":"ab"}
                }
              }]
            }
            """.trimIndent(),
        )!!
        val diffs = HotfoxOutboundCompare.mismatches(HotfoxOutboundCompare.fromProfile(profile), generated)
        assertTrue(diffs.none { it.startsWith("protocol") || it.startsWith("address") || it.startsWith("port") })
        assertFalse(HotfoxOutboundCompare.Result(HotfoxOutboundCompare.fromProfile(profile), generated, diffs).blockingMismatch)
    }

    @Test
    fun socksHttpsProbeAgainstHandshakeOnlyListenerIsNull() {
        val server = ServerSocket(0)
        val port = server.localPort
        val worker = thread(name = "socks-handshake-only") {
            val client = try {
                server.accept()
            } catch (_: Exception) {
                return@thread
            }
            client.use {
                val input = it.getInputStream()
                val header = ByteArray(3)
                var n = 0
                while (n < 3) {
                    val r = input.read(header, n, 3 - n)
                    if (r < 0) return@use
                    n += r
                }
                it.getOutputStream().write(byteArrayOf(0x05, 0x00))
                it.getOutputStream().flush()
                Thread.sleep(200)
            }
        }
        try {
            assertNull(VpnReadiness.probeSocksHttps204(port))
        } finally {
            server.close()
            worker.join(2_000)
        }
    }
}
