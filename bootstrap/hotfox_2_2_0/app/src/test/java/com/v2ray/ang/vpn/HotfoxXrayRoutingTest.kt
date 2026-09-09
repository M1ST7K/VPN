package com.v2ray.ang.vpn

import org.json.JSONObject
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
        val input = JSONObject()
            .put(
                "routing",
                JSONObject().put(
                    "rules",
                    org.json.JSONArray()
                        .put(JSONObject().put("outboundTag", "direct").put("domain", org.json.JSONArray().put("geosite:cn")))
                        .put(JSONObject().put("outboundTag", "dns-out").put("port", "53")),
                ),
            )
            .toString()
        val out = JSONObject(HotfoxXrayConfigInjector.apply(input, smart.copy(mode = HotfoxRoutingMode.GLOBAL)))
        val rules = out.getJSONObject("routing").getJSONArray("rules")
        assertEquals(1, rules.length())
        assertEquals("dns-out", rules.getJSONObject(0).getString("outboundTag"))
    }

    @Test
    fun customKeepsPresetDirectAfterHotfoxPrefix() {
        val snap = smart.copy(
            mode = HotfoxRoutingMode.CUSTOM,
            adsBlocked = true,
        )
        val input = JSONObject()
            .put(
                "routing",
                JSONObject().put(
                    "rules",
                    org.json.JSONArray().put(
                        JSONObject().put("outboundTag", "direct").put("domain", org.json.JSONArray().put("geosite:cn")),
                    ),
                ),
            )
            .toString()
        val out = JSONObject(HotfoxXrayConfigInjector.apply(input, snap))
        val rules = out.getJSONObject("routing").getJSONArray("rules")
        assertEquals(2, rules.length())
        assertEquals(HotfoxXrayRouting.TAG_BLOCKED, rules.getJSONObject(0).getString("outboundTag"))
        assertEquals("direct", rules.getJSONObject(1).getString("outboundTag"))
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
        val first = JSONObject().put("outbounds", org.json.JSONArray().put(JSONObject().put("tag", "proxy"))).toString()
        val afterReconnect = HotfoxXrayConfigInjector.apply(first, snap)
        val afterServerChange = HotfoxXrayConfigInjector.apply(
            JSONObject(afterReconnect).put("outbounds", org.json.JSONArray().put(JSONObject().put("tag", "proxy-2"))).toString(),
            snap,
        )
        val rules = JSONObject(afterServerChange).getJSONObject("routing").getJSONArray("rules")
        assertEquals("domain:ads.example", rules.getJSONObject(0).getJSONArray("domain").getString(0))
        assertEquals("block", rules.getJSONObject(0).getString("outboundTag"))
        assertEquals("proxy-2", JSONObject(afterServerChange).getJSONArray("outbounds").getJSONObject(0).getString("tag"))
    }
}
