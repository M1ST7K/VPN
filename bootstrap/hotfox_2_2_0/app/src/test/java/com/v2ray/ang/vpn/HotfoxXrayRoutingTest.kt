package com.v2ray.ang.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HotfoxXrayRoutingTest {
    private val smart = RoutingPolicySnapshot(
        mode = HotfoxRoutingMode.SMART,
        selectedApps = emptySet(),
        rules = emptyList(),
        lanAccess = false,
        adsBlocked = false,
    )

    @Before
    fun resetApply() {
        HotfoxRoutingApply.resetForTests()
    }

    @Test
    fun blockAndAdsPrecedeProxyAndDirectRules() {
        val snap = smart.copy(
            adsBlocked = true,
            rules = listOf(
                RoutingRule("p", RoutingRuleKind.DOMAIN_SUFFIX, "ok.example", RouteAction.VPN),
                RoutingRule("b", RoutingRuleKind.DOMAIN_EXACT, "tracker.example", RouteAction.BLOCK),
                RoutingRule("d", RoutingRuleKind.CIDR, "10.1.0.0/16", RouteAction.DIRECT),
            ),
        )
        val rules = HotfoxXrayRouting.rules(snap)
        assertEquals(HotfoxXrayRouting.TAG_BLOCKED, rules[0].outboundTag)
        assertEquals(listOf(HotfoxXrayRouting.ADS_GEOSITE), rules[0].domain)
        assertEquals(listOf("full:tracker.example"), rules[1].domain)
        assertEquals(HotfoxXrayRouting.TAG_BLOCKED, rules[1].outboundTag)
        assertEquals(listOf("domain:ok.example"), rules[2].domain)
        assertEquals(listOf("10.1.0.0/16"), rules[3].ip)
        assertTrue(rules.none { it.source == "app" })
    }

    @Test
    fun appAndLanRulesAreNotEmittedToXray() {
        val snap = smart.copy(
            rules = listOf(
                RoutingRule("app", RoutingRuleKind.APP, "org.telegram.messenger", RouteAction.DIRECT),
                RoutingRule("lan", RoutingRuleKind.LAN, "lan", RouteAction.DIRECT),
            ),
        )
        assertTrue(HotfoxXrayRouting.rules(snap).isEmpty())
    }

    @Test
    fun globalDropsPresetDirectRulesAndKeepsDns() {
        val merged = HotfoxXrayConfigInjector.merge(
            listOf(
                HotfoxXrayConfigInjector.ExistingRule("direct"),
                HotfoxXrayConfigInjector.ExistingRule("dns-out"),
            ),
            smart.copy(mode = HotfoxRoutingMode.GLOBAL),
        )
        assertEquals(listOf("dns-out"), merged.map { it.outboundTag })
        assertFalse(HotfoxXrayConfigInjector.keepExisting(HotfoxRoutingMode.GLOBAL, "direct"))
        assertTrue(HotfoxXrayConfigInjector.keepExisting(HotfoxRoutingMode.GLOBAL, "dns-out"))
    }

    @Test
    fun customKeepsPresetDirectAfterHotfoxPrefix() {
        val snap = smart.copy(
            mode = HotfoxRoutingMode.CUSTOM,
            adsBlocked = true,
        )
        val merged = HotfoxXrayConfigInjector.merge(
            listOf(HotfoxXrayConfigInjector.ExistingRule("direct")),
            snap,
        )
        assertEquals(2, merged.size)
        assertEquals(HotfoxXrayRouting.TAG_BLOCKED, merged[0].outboundTag)
        assertEquals("direct", merged[1].outboundTag)
        assertTrue(HotfoxXrayConfigInjector.keepExisting(HotfoxRoutingMode.CUSTOM, "direct"))
    }

    @Test
    fun malformedJsonIsReturnedUnchanged() {
        val raw = "not-json{"
        assertEquals(raw, HotfoxXrayConfigInjector.apply(raw, smart))
    }

    @Test
    fun staleGenerationDoesNotApplyOverNewerPolicy() {
        val first = HotfoxRoutingApply.bump()
        val second = HotfoxRoutingApply.bump()
        assertFalse(HotfoxRoutingApply.tryApply(first))
        assertTrue(HotfoxRoutingApply.tryApply(second))
        assertFalse(HotfoxRoutingApply.tryApply(second))
        assertEquals(second, HotfoxRoutingApply.appliedGeneration())
    }

    @Test
    fun reconnectAndServerChangeKeepInjectedRules() {
        val snap = smart.copy(
            rules = listOf(
                RoutingRule("b", RoutingRuleKind.DOMAIN_SUFFIX, "ads.example", RouteAction.BLOCK),
            ),
        )
        val existing = listOf(HotfoxXrayConfigInjector.ExistingRule("dns-out"))
        val afterReconnect = HotfoxXrayConfigInjector.merge(existing, snap)
        val afterServerChange = HotfoxXrayConfigInjector.merge(existing, snap)
        assertEquals(afterReconnect, afterServerChange)
        assertEquals("domain:ads.example", afterServerChange[0].domain.single())
        assertEquals("block", afterServerChange[0].outboundTag)
        assertEquals("dns-out", afterServerChange.last().outboundTag)
    }
}
