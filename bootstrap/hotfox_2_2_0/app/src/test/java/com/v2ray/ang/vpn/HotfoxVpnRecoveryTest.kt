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
        TunFdEvidence.resetForTests()
        HotfoxTunLayerEvidence.resetForTests()
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
    fun importUiReloadsAfterSubscriptionUrlEvenIfLaterFetchIsEmpty() {
        assertTrue(HotfoxImportUiRefresh.shouldReloadAfterBatch(0, 1))
        assertTrue(HotfoxImportUiRefresh.shouldReloadAfterBatch(3, 0))
        assertFalse(HotfoxImportUiRefresh.shouldReloadAfterBatch(0, 0))
        assertTrue(HotfoxImportUiRefresh.shouldReloadAfterSubUpdate(successCount = 1, configCount = 0))
        assertTrue(HotfoxImportUiRefresh.shouldReloadAfterSubUpdate(successCount = 0, configCount = 4))
        assertFalse(HotfoxImportUiRefresh.shouldReloadAfterSubUpdate(successCount = 0, configCount = 0))
    }

    @Test
    fun hevSocksTargetMustMatchGeneratedXrayInbound() {
        val json = """
            {
              "inbounds": [
                {"protocol":"socks","listen":"127.0.0.1","port":10808},
                {"protocol":"http","listen":"127.0.0.1","port":10809}
              ],
              "outbounds": [{"protocol":"freedom"}]
            }
        """.trimIndent()
        val ports = HotfoxOutboundCompare.inboundPorts(json)
        assertEquals(10808, ports.socksPort)
        assertEquals(10809, ports.httpPort)
        val hev = HotfoxHevSocksTarget(host = "127.0.0.1", port = 10808, udp = "udp", mtu = 1500)
        assertTrue(HotfoxDatapathContract.assertHevMatchesXray(hev, ports.socksPort, ports.socksListen))
        assertFalse(
            HotfoxDatapathContract.assertHevMatchesXray(
                hev.copy(port = 10809),
                ports.socksPort,
                ports.socksListen,
            ),
        )
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

    @Test
    fun subscriptionTitleNeverUsesUrlOrToken() {
        assertEquals("Amsterdam", HotfoxSubscriptionTitle.fromImport("Amsterdam"))
        assertEquals("Подписка", HotfoxSubscriptionTitle.fromImport(null))
        assertEquals("Подписка", HotfoxSubscriptionTitle.fromImport("https://provider.example/sub/sUq75ktDSV7LmQuB"))
        val titled = HotfoxSubscriptionTitle.resolve(
            fragment = null,
            profileTitle = "base64:" + java.util.Base64.getEncoder().encodeToString("FoxNet".toByteArray()),
            contentDisposition = null,
            current = "import sub",
        )
        assertEquals("FoxNet", titled)
        val fromFile = HotfoxSubscriptionTitle.resolve(
            fragment = null,
            profileTitle = null,
            contentDisposition = "attachment; filename=\"Home VPN\"",
            current = "import sub",
        )
        assertEquals("Home VPN", fromFile)
        assertTrue(HotfoxSubscriptionTitle.shouldReplace("import sub"))
        assertFalse(HotfoxSubscriptionTitle.shouldReplace("Amsterdam"))
    }

    @Test
    fun tunFdEvidenceRequiresHevHandoffBeforeClose() {
        TunFdEvidence.resetForTests()
        TunFdEvidence.recordEstablish()
        assertTrue(TunFdEvidence.established)
        assertFalse(TunFdEvidence.hevReceived)
        TunFdEvidence.recordHevReceived()
        assertTrue(TunFdEvidence.hevReceived)
        TunFdEvidence.recordHevStopped()
        assertTrue(TunFdEvidence.hevStoppedBeforeClose)
        TunFdEvidence.recordClosed()
        assertTrue(TunFdEvidence.closed)
        assertTrue(TunFdEvidence.summary().contains("hevReceivedFd=true"))
    }

    @Test
    fun tunHttpPassWithDnsFailStillCountsAsTunInject() {
        assertTrue(HotfoxTunLayerEvidence.injectSucceeded(http = true, dns = false))
        assertTrue(HotfoxTunLayerEvidence.injectSucceeded(http = true, dns = null))
        assertTrue(HotfoxTunLayerEvidence.injectSucceeded(http = false, dns = true))
        assertFalse(HotfoxTunLayerEvidence.injectSucceeded(http = false, dns = false))
        assertFalse(HotfoxTunLayerEvidence.injectSucceeded(http = null, dns = null))
        HotfoxTunLayerEvidence.record(http = true, dns = false)
        assertTrue(HotfoxTunLayerEvidence.summary().contains("tunHttp=true"))
        assertTrue(HotfoxTunLayerEvidence.summary().contains("tunDns=false"))
        HotfoxTunLayerEvidence.record(http = true, dns = null, http4 = true, http6 = false)
        assertTrue(HotfoxTunLayerEvidence.summary().contains("tunHttp4=true"))
        assertTrue(HotfoxTunLayerEvidence.summary().contains("tunHttp6=false"))
    }

    @Test
    fun tunHttpsProbeKeepsIpv4IndependentOfIpv6() {
        val v4 = java.net.InetAddress.getByName("1.1.1.1")
        val v6 = java.net.InetAddress.getByName("2001:4860:4860::8888")
        assertEquals(listOf(v4), HotfoxAddressFamilyPolicy.addressesForProbe(listOf(v6, v4), ipv4Only = true))
        assertEquals(listOf(v6), HotfoxAddressFamilyPolicy.addressesForProbe(listOf(v6, v4), ipv4Only = false))
        assertTrue(HotfoxAddressFamilyPolicy.addressesForProbe(listOf(v6), ipv4Only = true).isEmpty())
    }

    @Test
    fun realityOrNetworkDriftIsBlockingGeneratedMismatch() {
        val profile = com.v2ray.ang.dto.entities.ProfileItem.create(com.v2ray.ang.enums.EConfigType.VLESS).apply {
            remarks = "Amsterdam"
            server = "vpn.example"
            serverPort = "443"
            network = "tcp"
            security = "reality"
            password = "11111111-2222-3333-4444-555555555555"
            sni = "www.example.com"
            publicKey = "public-key-material"
            shortId = "abcd"
        }
        val generated = HotfoxOutboundCompare.fromGeneratedJson(
            """
            {
              "outbounds": [{
                "protocol": "vless",
                "settings": {"vnext":[{"address":"vpn.example","port":443,"users":[{"id":"11111111-2222-3333-4444-555555555555"}]}]},
                "streamSettings": {
                  "network": "ws",
                  "security": "tls",
                  "tlsSettings": {"serverName":"www.example.com"}
                }
              }]
            }
            """.trimIndent(),
        )!!
        val diffs = HotfoxOutboundCompare.mismatches(HotfoxOutboundCompare.fromProfile(profile), generated)
        assertTrue(diffs.any { it.startsWith("network:") })
        assertTrue(diffs.any { it.startsWith("security:") || it.startsWith("reality:") })
        assertTrue(HotfoxOutboundCompare.Result(HotfoxOutboundCompare.fromProfile(profile), generated, diffs).blockingMismatch)
    }

    @Test
    fun tcpAndRawNetworksAreNotBlockingDrift() {
        val profile = com.v2ray.ang.dto.entities.ProfileItem.create(com.v2ray.ang.enums.EConfigType.VLESS).apply {
            remarks = "Amsterdam"
            server = "vpn.example"
            serverPort = "443"
            network = "tcp"
            security = "reality"
            publicKey = "pk"
            shortId = "ab"
            sni = "www.example.com"
        }
        val generated = HotfoxOutboundCompare.fromGeneratedJson(
            """
            {
              "outbounds": [{
                "protocol": "vless",
                "settings": {"vnext":[{"address":"vpn.example","port":443,"users":[{"id":"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"}]}]},
                "streamSettings": {
                  "network": "raw",
                  "security": "reality",
                  "realitySettings": {"serverName":"www.example.com","publicKey":"pk","shortId":"ab"}
                }
              }]
            }
            """.trimIndent(),
        )!!
        val diffs = HotfoxOutboundCompare.mismatches(HotfoxOutboundCompare.fromProfile(profile), generated)
        assertTrue(diffs.none { it.startsWith("network:") })
        assertFalse(HotfoxOutboundCompare.Result(HotfoxOutboundCompare.fromProfile(profile), generated, diffs).blockingMismatch)
    }

    @Test
    fun publicIpEvidenceRedactsAndCompares() {
        assertEquals("203.0.113.x", HotfoxIpEvidence.redact("203.0.113.77"))
        assertTrue(HotfoxIpEvidence.redact("2001:db8:1:2:3:4:5:6").endsWith(":x"))
        assertTrue(HotfoxIpEvidence.changed("1.1.1.1", "8.8.8.8"))
        assertFalse(HotfoxIpEvidence.changed("1.1.1.1", "1.1.1.1"))
        assertFalse(HotfoxIpEvidence.changed("", "8.8.8.8"))
    }

    @Test
    fun engineeringE2eGateRequiresSocksOnlyBeforeTun() {
        assertEquals("FAIL" to "socks-only-https", HotfoxEngineeringE2eGate.outcome(false, 3, 3))
        assertEquals("FAIL" to "not-protected", HotfoxEngineeringE2eGate.outcome(true, 1, 3))
        assertEquals("PASS" to "ok", HotfoxEngineeringE2eGate.outcome(true, 3, 3))
    }
}
