package com.v2ray.ang.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HotfoxRuntimeRepairTest {
    private val self = "com.hotfox.vpn"

    @Before
    fun reset() {
        HotfoxSocketProtect.resetForTests()
        VpnProtectEvidence.resetForTests()
        HotfoxOutboundCompare.resetForTests()
    }

    @Test
    fun globalAndExcludeAlwaysKeepSelfOffTun() {
        val global = HotfoxTunSelfExclusion.forPlan(
            PerAppVpnPlan(enabled = false, bypassSelected = false, packages = emptySet()),
            self,
        )
        assertFalse(global.useAllowList)
        assertTrue(self in global.disallow)
        assertFalse(HotfoxTunSelfExclusion.capturesPackage(global, self))

        val exclude = HotfoxTunSelfExclusion.forPlan(
            PerAppVpnPlan(enabled = true, bypassSelected = true, packages = setOf("com.bank.app")),
            self,
        )
        assertTrue(self in exclude.disallow)
        assertTrue("com.bank.app" in exclude.disallow)
        assertFalse(HotfoxTunSelfExclusion.capturesPackage(exclude, self))
    }

    @Test
    fun includeDoesNotPutSelfOnAllowList() {
        val include = HotfoxTunSelfExclusion.forPlan(
            PerAppVpnPlan(enabled = true, bypassSelected = false, packages = setOf("org.mozilla.firefox", self)),
            self,
        )
        assertTrue(include.useAllowList)
        assertEquals(setOf("org.mozilla.firefox"), include.allow)
        assertFalse(HotfoxTunSelfExclusion.capturesPackage(include, self))
        assertTrue(HotfoxTunSelfExclusion.capturesPackage(include, "org.mozilla.firefox"))
    }

    @Test
    fun bindFalseAndExceptionsAreFailures() {
        assertTrue(VpnLoopPrevention.interpretBindAttempt(true, null))
        assertFalse(VpnLoopPrevention.interpretBindAttempt(false, null))
        assertFalse(VpnLoopPrevention.interpretBindAttempt(null, null))
        assertFalse(VpnLoopPrevention.interpretBindAttempt(true, SecurityException("denied")))
        assertFalse(VpnLoopPrevention.interpretBindAttempt(false, IllegalStateException("gone")))
    }

    @Test
    fun protectRecordsTcpUdpSuccessFalseExceptionAndStaleService() {
        HotfoxSocketProtect.attach(7) { fd -> fd == 11 }
        val tcpOk = HotfoxSocketProtect.protect(11, "tcp", 7)
        assertTrue(tcpOk.success)
        assertEquals("ok", tcpOk.reason)
        val udpFail = HotfoxSocketProtect.protect(12, "udp", 7)
        assertFalse(udpFail.success)
        assertEquals("protect-false", udpFail.reason)
        HotfoxSocketProtect.attach(8) { throw IllegalStateException("dead") }
        val crashed = HotfoxSocketProtect.protect(13, "tcp", 8)
        assertFalse(crashed.success)
        assertTrue(crashed.reason.startsWith("protect-exception:"))
        val staleGen = HotfoxSocketProtect.protect(11, "tcp", 7)
        assertFalse(staleGen.success)
        assertEquals("stale-generation", staleGen.reason)
        HotfoxSocketProtect.detach(8)
        val staleService = HotfoxSocketProtect.protect(11, "udp", 8)
        assertFalse(staleService.success)
        assertEquals("stale-service", staleService.reason)
        assertFalse(HotfoxSocketProtect.requireSuccess(staleService))
        assertTrue(VpnProtectEvidence.callCount >= 5)
        assertEquals(1, VpnProtectEvidence.successCount)
    }

    @Test
    fun customAndGroupAreNotRejectedForContainerVsVless() {
        val custom = com.v2ray.ang.dto.entities.ProfileItem.create(com.v2ray.ang.enums.EConfigType.CUSTOM)
        val group = com.v2ray.ang.dto.entities.ProfileItem.create(com.v2ray.ang.enums.EConfigType.POLICYGROUP)
        val chain = com.v2ray.ang.dto.entities.ProfileItem.create(com.v2ray.ang.enums.EConfigType.PROXYCHAIN)
        val json = """
            {
              "outbounds": [
                {"protocol":"vless","tag":"provider-proxy","settings":{"vnext":[{"address":"vpn.example","port":443}]}},
                {"protocol":"freedom","tag":"hotfox-direct"},
                {"protocol":"blackhole","tag":"hotfox-block"}
              ]
            }
        """.trimIndent()
        val customResult = HotfoxOutboundCompare.record(custom, json)
        assertFalse(customResult.mismatches.any { it.startsWith("protocol:") })
        assertFalse(customResult.blockingMismatch)
        assertFalse(HotfoxOutboundCompare.record(group, json).blockingMismatch)
        assertFalse(HotfoxOutboundCompare.record(chain, json).blockingMismatch)
    }

    @Test
    fun customMissingProxyOutboundIsBlocking() {
        val custom = com.v2ray.ang.dto.entities.ProfileItem.create(com.v2ray.ang.enums.EConfigType.CUSTOM)
        val json = """{"outbounds":[{"protocol":"freedom","tag":"direct"}]}"""
        val result = HotfoxOutboundCompare.record(custom, json)
        assertTrue(result.blockingMismatch)
        assertTrue(result.mismatches.any { it.contains("missing-proxy-outbound") })
    }

    @Test
    fun singleNodePortMismatchStillBlocks() {
        val profile = com.v2ray.ang.dto.entities.ProfileItem.create(com.v2ray.ang.enums.EConfigType.VLESS).apply {
            server = "vpn.example"
            serverPort = "443"
            network = "tcp"
            security = "reality"
        }
        val json = """
            {"outbounds":[{"protocol":"vless","settings":{"vnext":[{"address":"vpn.example","port":8443}]}}]}
        """.trimIndent()
        val result = HotfoxOutboundCompare.record(profile, json)
        assertTrue(result.blockingMismatch)
        assertTrue(result.mismatches.any { it.startsWith("port:") })
    }

    @Test
    fun tagResolverUsesProviderTagsNotLiterals() {
        val json = """
            {
              "outbounds": [
                {"protocol":"vless","tag":"provider-proxy"},
                {"protocol":"freedom","tag":"hotfox-direct"},
                {"protocol":"blackhole","tag":"hotfox-block"}
              ],
              "routing": {"balancers":[{"tag":"proxy-pool","selector":["provider-proxy"]}]}
            }
        """.trimIndent()
        val tags = HotfoxXrayTagResolver.resolve(json)
        assertEquals("provider-proxy", tags.proxy)
        assertEquals("hotfox-direct", tags.direct)
        assertEquals("hotfox-block", tags.block)
        assertTrue("proxy-pool" in tags.balancerTags)
    }

    @Test
    fun globalStripsLegacyRuDirectAndRewritesAdsToRealBlockTag() {
        val snap = RoutingPolicySnapshot(
            mode = HotfoxRoutingMode.GLOBAL,
            selectedApps = emptySet(),
            rules = emptyList(),
            lanAccess = false,
            adsBlocked = true,
        )
        val injected = HotfoxXrayConfigInjector.apply(
            """
            {
              "outbounds": [
                {"protocol":"vless","tag":"provider-proxy"},
                {"protocol":"freedom","tag":"hotfox-direct"},
                {"protocol":"blackhole","tag":"hotfox-block"},
                {"protocol":"dns","tag":"dns-out"}
              ],
              "routing": {
                "rules": [
                  {"type":"field","domain":["geosite:cn","domain:ru","geosite:private"],"outboundTag":"hotfox-direct"},
                  {"type":"field","outboundTag":"dns-out"}
                ]
              }
            }
            """.trimIndent(),
            snap,
        )
        assertFalse(injected.contains("geosite:cn"))
        assertFalse(injected.contains("domain:ru"))
        assertTrue(injected.contains("hotfox-block"))
        assertTrue(injected.contains("geosite:category-ads-all"))
        val report = HotfoxXrayConfigValidator.inspect(injected)
        assertTrue(report.ok)
    }

    @Test
    fun danglingOutboundTagFailsClosed() {
        val json = """
            {
              "outbounds": [{"protocol":"vless","tag":"provider-proxy"}],
              "routing": {"rules":[{"type":"field","outboundTag":"proxy"}]}
            }
        """.trimIndent()
        val report = HotfoxXrayConfigValidator.inspect(json)
        assertFalse(report.ok)
        assertTrue("proxy" in report.danglingOutboundTags)
        assertTrue(report.message().contains("HF-VPN-016"))
    }

    @Test
    fun selfPackageIsOutsideTunCapture() {
        val snap = RoutingPolicySnapshot(
            mode = HotfoxRoutingMode.GLOBAL,
            selectedApps = emptySet(),
            rules = emptyList(),
            lanAccess = false,
            adsBlocked = false,
        )
        assertTrue(snap.outsideVpnCapture(self, self))
        assertFalse(snap.outsideVpnCapture("org.mozilla.firefox", self))
    }
}
