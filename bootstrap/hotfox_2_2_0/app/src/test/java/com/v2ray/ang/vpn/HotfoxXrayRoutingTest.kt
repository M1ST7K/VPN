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
    fun bucketsIgnoreReversedUserOrderForConflictingKinds() {
        val snap = smart.copy(
            rules = listOf(
                RoutingRule("cidr", RoutingRuleKind.CIDR, "1.2.3.0/24", RouteAction.DIRECT),
                RoutingRule("suffix", RoutingRuleKind.DOMAIN_SUFFIX, "example.com", RouteAction.VPN),
                RoutingRule("exact", RoutingRuleKind.DOMAIN_EXACT, "app.example.com", RouteAction.VPN),
                RoutingRule("block", RoutingRuleKind.DOMAIN_EXACT, "tracker.example.com", RouteAction.BLOCK),
            ),
        )
        val rules = HotfoxXrayRouting.rules(snap)
        assertEquals(listOf("full:tracker.example.com"), rules[0].domain)
        assertEquals(HotfoxXrayRouting.TAG_BLOCKED, rules[0].outboundTag)
        assertEquals(listOf("full:app.example.com"), rules[1].domain)
        assertEquals(HotfoxXrayRouting.TAG_PROXY, rules[1].outboundTag)
        assertEquals(listOf("domain:example.com"), rules[2].domain)
        assertEquals(listOf("1.2.3.0/24"), rules[3].ip)
        assertEquals(HotfoxXrayRouting.TAG_DIRECT, rules[3].outboundTag)
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
        assertTrue(HotfoxRoutingDataPlane.xrayRules(snap).isEmpty())
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

    @Test
    fun appSplitModesEmitOnlyBlockFieldRules() {
        val include = smart.copy(
            mode = HotfoxRoutingMode.INCLUDE_APPS,
            selectedApps = setOf("org.mozilla.firefox"),
            adsBlocked = true,
            rules = listOf(
                RoutingRule("host", RoutingRuleKind.DOMAIN_EXACT, "example.com", RouteAction.DIRECT),
                RoutingRule("net", RoutingRuleKind.CIDR, "1.2.3.0/24", RouteAction.DIRECT),
                RoutingRule("block", RoutingRuleKind.DOMAIN_EXACT, "tracker.example", RouteAction.BLOCK),
                RoutingRule("proxy", RoutingRuleKind.DOMAIN_SUFFIX, "ok.example", RouteAction.VPN),
            ),
        )
        val rules = HotfoxXrayRouting.rules(include)
        assertEquals(2, rules.size)
        assertEquals(listOf(HotfoxXrayRouting.ADS_GEOSITE), rules[0].domain)
        assertEquals(HotfoxXrayRouting.TAG_BLOCKED, rules[0].outboundTag)
        assertEquals(listOf("full:tracker.example"), rules[1].domain)
        assertEquals(HotfoxXrayRouting.TAG_BLOCKED, rules[1].outboundTag)
        assertTrue(rules.none { it.outboundTag == HotfoxXrayRouting.TAG_DIRECT })
        assertEquals(
            RouteAction.VPN,
            HotfoxRoutingPolicy.decide(
                include,
                RoutingQuery(packageName = "org.mozilla.firefox", domain = "example.com", ip = "1.2.3.4"),
            ).action,
        )
        val exclude = include.copy(
            mode = HotfoxRoutingMode.EXCLUDE_APPS,
            selectedApps = setOf("com.bank.app"),
        )
        assertTrue(HotfoxXrayRouting.rules(exclude).none { it.outboundTag == HotfoxXrayRouting.TAG_DIRECT })
        assertEquals(
            RouteAction.VPN,
            HotfoxRoutingPolicy.decide(
                exclude,
                RoutingQuery(packageName = "org.mozilla.firefox", domain = "example.com"),
            ).action,
        )
    }

    @Test
    fun smartModeStillEmitsDirectDomainAndCidr() {
        val snap = smart.copy(
            rules = listOf(
                RoutingRule("host", RoutingRuleKind.DOMAIN_EXACT, "example.com", RouteAction.DIRECT),
                RoutingRule("net", RoutingRuleKind.CIDR, "1.2.3.0/24", RouteAction.DIRECT),
            ),
        )
        val rules = HotfoxXrayRouting.rules(snap)
        assertEquals(2, rules.size)
        assertEquals(HotfoxXrayRouting.TAG_DIRECT, rules[0].outboundTag)
        assertEquals(HotfoxXrayRouting.TAG_DIRECT, rules[1].outboundTag)
        assertEquals(
            RouteAction.DIRECT,
            HotfoxRoutingPolicy.decide(snap, RoutingQuery(domain = "example.com")).action,
        )
    }
}
