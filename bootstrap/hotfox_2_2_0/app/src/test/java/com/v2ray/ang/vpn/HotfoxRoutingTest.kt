package com.v2ray.ang.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HotfoxRoutingTest {
    private val smart = RoutingPolicySnapshot(
        mode = HotfoxRoutingMode.SMART,
        selectedApps = emptySet(),
        rules = emptyList(),
        lanAccess = false,
        adsBlocked = false,
    )

    @Test
    fun defaultSmartAndGlobalSendInternetThroughVpn() {
        val global = smart.copy(mode = HotfoxRoutingMode.GLOBAL)
        assertEquals(RouteAction.VPN, HotfoxRoutingPolicy.decide(smart, RoutingQuery(domain = "example.com")).action)
        assertEquals(RouteAction.VPN, HotfoxRoutingPolicy.decide(global, RoutingQuery(ip = "8.8.8.8")).action)
    }

    @Test
    fun includeAppsSendsOnlySelectedPackagesThroughVpn() {
        val snap = smart.copy(
            mode = HotfoxRoutingMode.INCLUDE_APPS,
            selectedApps = setOf("org.telegram.messenger"),
        )
        assertEquals(
            RouteAction.VPN,
            HotfoxRoutingPolicy.decide(snap, RoutingQuery(packageName = "org.telegram.messenger")).action,
        )
        assertEquals(
            RouteAction.DIRECT,
            HotfoxRoutingPolicy.decide(snap, RoutingQuery(packageName = "com.android.vending")).action,
        )
        val plan = snap.perAppPlan("com.hotfox.vpn")
        assertTrue(plan.enabled)
        assertFalse(plan.bypassSelected)
        assertEquals(setOf("org.telegram.messenger"), plan.packages)
    }

    @Test
    fun excludeAppsSendsSelectedPackagesDirect() {
        val snap = smart.copy(
            mode = HotfoxRoutingMode.EXCLUDE_APPS,
            selectedApps = setOf("com.bank.app"),
        )
        assertEquals(RouteAction.DIRECT, HotfoxRoutingPolicy.decide(snap, RoutingQuery(packageName = "com.bank.app")).action)
        assertEquals(RouteAction.VPN, HotfoxRoutingPolicy.decide(snap, RoutingQuery(packageName = "org.mozilla.firefox")).action)
        assertTrue(snap.perAppPlan("com.hotfox.vpn").bypassSelected)
    }

    @Test
    fun missingPackageDoesNotPoisonSelection() {
        val snap = smart.copy(
            mode = HotfoxRoutingMode.INCLUDE_APPS,
            selectedApps = setOf("gone.app", "alive.app"),
        )
        val plan = snap.perAppPlan("com.hotfox.vpn")
        assertTrue("alive.app" in plan.packages)
        assertEquals(
            RouteAction.DIRECT,
            HotfoxRoutingPolicy.decide(snap, RoutingQuery(packageName = "uninstalled.now")).action,
        )
    }

    @Test
    fun blockBeatsDomainAndDefault() {
        val snap = smart.copy(
            rules = listOf(
                RoutingRule("b1", RoutingRuleKind.DOMAIN_SUFFIX, "ads.example", RouteAction.BLOCK),
            ),
        )
        val blocked = HotfoxRoutingPolicy.decide(
            snap,
            RoutingQuery(packageName = "org.mozilla.firefox", domain = "tracker.ads.example"),
        )
        assertEquals(RouteAction.BLOCK, blocked.action)
        assertTrue(blocked.reason.startsWith("block:"))
    }

    @Test
    fun domainExactBeatsSuffixAndNormalizesHost() {
        val snap = smart.copy(
            rules = listOf(
                RoutingRule("s", RoutingRuleKind.DOMAIN_SUFFIX, "example.com", RouteAction.DIRECT),
                RoutingRule("e", RoutingRuleKind.DOMAIN_EXACT, "app.example.com", RouteAction.VPN),
            ),
        )
        assertEquals(
            RouteAction.VPN,
            HotfoxRoutingPolicy.decide(snap, RoutingQuery(domain = "APP.EXAMPLE.COM.")).action,
        )
        assertEquals(
            RouteAction.DIRECT,
            HotfoxRoutingPolicy.decide(snap, RoutingQuery(domain = "other.example.com")).action,
        )
    }

    @Test
    fun overlappingSuffixUsesFirstExactThenSuffixInListOrder() {
        val snap = smart.copy(
            rules = listOf(
                RoutingRule("wide", RoutingRuleKind.DOMAIN_SUFFIX, "example.com", RouteAction.DIRECT),
                RoutingRule("narrow", RoutingRuleKind.DOMAIN_SUFFIX, "mail.example.com", RouteAction.BLOCK),
            ),
        )
        assertEquals(
            RouteAction.BLOCK,
            HotfoxRoutingPolicy.decide(snap, RoutingQuery(domain = "inbox.mail.example.com")).action,
        )
    }

    @Test
    fun malformedCidrAndDefaultRouteDirectAreRejected() {
        assertNull(CidrRouting.parse("not-a-cidr"))
        assertNull(CidrRouting.parse("8.8.8.8/33"))
        assertNull(CidrRouting.parse("0.0.0.0/0"))
        assertNull(CidrRouting.parse("::/0"))
        assertNull(
            HotfoxRoutingPolicy.sanitizeRule(
                RoutingRule("bad", RoutingRuleKind.CIDR, "0.0.0.0/0", RouteAction.DIRECT),
            ),
        )
        val ok = CidrRouting.parse("10.0.0.0/8")
        assertTrue(ok != null && CidrRouting.contains(ok, "10.1.2.3"))
        assertFalse(CidrRouting.contains(ok!!, "11.0.0.1"))
    }

    @Test
    fun lanPermitIsExplicitAndGlobalNeverBypassesLan() {
        val lanOn = smart.copy(lanAccess = true)
        val lanOff = smart.copy(lanAccess = false)
        val globalLan = smart.copy(mode = HotfoxRoutingMode.GLOBAL, lanAccess = true)
        assertEquals(RouteAction.DIRECT, HotfoxRoutingPolicy.decide(lanOn, RoutingQuery(ip = "192.168.1.5", lan = true)).action)
        assertEquals(RouteAction.VPN, HotfoxRoutingPolicy.decide(lanOff, RoutingQuery(ip = "192.168.1.5", lan = true)).action)
        assertFalse(globalLan.bypassLanOnTun())
        assertEquals(RouteAction.VPN, HotfoxRoutingPolicy.decide(globalLan, RoutingQuery(ip = "10.0.0.2", lan = true)).action)
        assertTrue(CidrRouting.isLanIpv4("172.16.0.1"))
        assertFalse(CidrRouting.isLanIpv4("1.1.1.1"))
    }

    @Test
    fun dnsPolicyDefaultsThroughVpn() {
        assertTrue(smart.dnsThroughVpn)
        assertEquals(DnsPolicy.THROUGH_VPN, smart.dnsPolicy())
        assertEquals(DnsPolicy.THROUGH_VPN, smart.copy(mode = HotfoxRoutingMode.GLOBAL).dnsPolicy())
        assertEquals(
            DnsPolicy.THROUGH_VPN,
            smart.copy(mode = HotfoxRoutingMode.INCLUDE_APPS, lanAccess = true, dnsThroughVpn = false).dnsPolicy(),
        )
    }

    @Test
    fun ipv6CaptureStaysFailClosedWhenProxyDisabled() {
        val lanOn = smart.copy(lanAccess = true)
        val globalLan = smart.copy(mode = HotfoxRoutingMode.GLOBAL, lanAccess = true)
        assertTrue(lanOn.ipv6TunCapturesAll(ipv6ProxyEnabled = false))
        assertTrue(globalLan.ipv6TunCapturesAll(ipv6ProxyEnabled = true))
        assertFalse(lanOn.ipv6TunCapturesAll(ipv6ProxyEnabled = true))
    }

    @Test
    fun ipv6LanFollowsExplicitLanPolicy() {
        val lanOn = smart.copy(lanAccess = true)
        assertEquals(
            RouteAction.DIRECT,
            HotfoxRoutingPolicy.decide(lanOn, RoutingQuery(ip = "fe80::1", lan = false)).action,
        )
        assertEquals(
            RouteAction.VPN,
            HotfoxRoutingPolicy.decide(smart, RoutingQuery(ip = "2001:db8::1")).action,
        )
        assertTrue(CidrRouting.isLanIpv6("fd12:3456::1"))
        assertFalse(CidrRouting.isLanIpv6("2001:4860:4860::8888"))
    }

    @Test
    fun uiLabelIsDerivedFromActivePolicy() {
        assertEquals("Весь трафик", smart.copy(mode = HotfoxRoutingMode.GLOBAL).uiLabel())
        assertEquals(
            "2 приложения через VPN",
            smart.copy(mode = HotfoxRoutingMode.INCLUDE_APPS, selectedApps = setOf("a", "b")).uiLabel(),
        )
        assertEquals(
            "1 приложения напрямую",
            smart.copy(mode = HotfoxRoutingMode.EXCLUDE_APPS, selectedApps = setOf("bank")).uiLabel(),
        )
    }

    @Test
    fun fromStorageMigratesUnknownToSmart() {
        assertEquals(HotfoxRoutingMode.SMART, HotfoxRoutingMode.fromStorage(null))
        assertEquals(HotfoxRoutingMode.GLOBAL, HotfoxRoutingMode.fromStorage("global"))
        assertEquals(HotfoxRoutingMode.INCLUDE_APPS, HotfoxRoutingMode.fromStorage("include_apps"))
        assertEquals(HotfoxRoutingMode.EXCLUDE_APPS, HotfoxRoutingMode.fromStorage("bypass"))
        assertEquals(HotfoxRoutingMode.CUSTOM, HotfoxRoutingMode.fromStorage("custom"))
        assertEquals(HotfoxRoutingMode.SMART, HotfoxRoutingMode.fromStorage("not-a-mode"))
    }

    @Test
    fun resolveStoredModeMigratesLegacyKeyWhenCanonicalEmpty() {
        assertEquals(
            HotfoxRoutingMode.GLOBAL,
            HotfoxRoutingPolicy.resolveStoredMode(canonical = null, legacy = "global").mode,
        )
        assertTrue(HotfoxRoutingPolicy.resolveStoredMode(null, "global").persistCanonical)
        assertEquals(
            HotfoxRoutingMode.CUSTOM,
            HotfoxRoutingPolicy.resolveStoredMode("", "custom").mode,
        )
        assertEquals(
            HotfoxRoutingMode.INCLUDE_APPS,
            HotfoxRoutingPolicy.resolveStoredMode(null, "selected").mode,
        )
        assertEquals(
            HotfoxRoutingMode.EXCLUDE_APPS,
            HotfoxRoutingPolicy.resolveStoredMode("   ", "bypass").mode,
        )
        val canonicalWins = HotfoxRoutingPolicy.resolveStoredMode("smart", "global")
        assertEquals(HotfoxRoutingMode.SMART, canonicalWins.mode)
        assertFalse(canonicalWins.persistCanonical)
        val empty = HotfoxRoutingPolicy.resolveStoredMode(null, null)
        assertEquals(HotfoxRoutingMode.SMART, empty.mode)
        assertFalse(empty.persistCanonical)
    }

    @Test
    fun excludeSelectedStaysDirectEvenWhenAdsWouldBlock() {
        val snap = smart.copy(
            mode = HotfoxRoutingMode.EXCLUDE_APPS,
            selectedApps = setOf("com.bank.app"),
            adsBlocked = true,
            rules = listOf(
                RoutingRule("ads", RoutingRuleKind.DOMAIN_SUFFIX, "ads.example", RouteAction.BLOCK),
            ),
        )
        val excluded = HotfoxRoutingPolicy.decide(
            snap,
            RoutingQuery(packageName = "com.bank.app", domain = "tracker.ads.example"),
        )
        assertEquals(RouteAction.DIRECT, excluded.action)
        assertEquals("exclude_apps", excluded.reason)
        assertTrue(snap.outsideVpnCapture("com.bank.app"))
        val captured = HotfoxRoutingPolicy.decide(
            snap,
            RoutingQuery(packageName = "org.mozilla.firefox", domain = "tracker.ads.example"),
        )
        assertEquals(RouteAction.BLOCK, captured.action)
        assertFalse(snap.outsideVpnCapture("org.mozilla.firefox"))
    }

    @Test
    fun includeMissStaysDirectEvenWhenDomainWouldBlock() {
        val snap = smart.copy(
            mode = HotfoxRoutingMode.INCLUDE_APPS,
            selectedApps = setOf("org.telegram.messenger"),
            rules = listOf(
                RoutingRule("ads", RoutingRuleKind.DOMAIN_EXACT, "tracker.example", RouteAction.BLOCK),
            ),
        )
        assertEquals(
            RouteAction.DIRECT,
            HotfoxRoutingPolicy.decide(
                snap,
                RoutingQuery(packageName = "com.android.vending", domain = "tracker.example"),
            ).action,
        )
        assertEquals(
            RouteAction.BLOCK,
            HotfoxRoutingPolicy.decide(
                snap,
                RoutingQuery(packageName = "org.telegram.messenger", domain = "tracker.example"),
            ).action,
        )
    }

    @Test
    fun rulesJsonRoundTripDropsMalformedAndDefaultRouteDirect() {
        val rules = listOf(
            RoutingRule("ok", RoutingRuleKind.DOMAIN_SUFFIX, "ads.example", RouteAction.BLOCK),
            RoutingRule("bad", RoutingRuleKind.CIDR, "0.0.0.0/0", RouteAction.DIRECT),
            RoutingRule("app", RoutingRuleKind.APP, "org.telegram.messenger", RouteAction.VPN),
        )
        val encoded = HotfoxRoutingPolicy.encodeRules(rules)
        val parsed = HotfoxRoutingPolicy.parseRules(encoded)
        assertEquals(1, parsed.size)
        assertEquals("ads.example", parsed[0].value)
        assertTrue(HotfoxRoutingPolicy.parseRules("{not-json}").isEmpty())
        assertTrue(HotfoxRoutingPolicy.parseRules(null).isEmpty())
    }

    @Test
    fun idnDomainsNormalizeToPunycodeAndMatchSuffixRules() {
        val rule = HotfoxRoutingPolicy.sanitizeRule(
            RoutingRule("idn", RoutingRuleKind.DOMAIN_SUFFIX, "пример.рф", RouteAction.BLOCK),
        )
        requireNotNull(rule)
        assertTrue(rule.value.startsWith("xn--"))
        assertEquals("xn--e1afmkfd.xn--p1ai", DomainRouting.normalize("Пример.РФ"))
        val snap = smart.copy(rules = listOf(rule))
        assertEquals(
            RouteAction.BLOCK,
            HotfoxRoutingPolicy.decide(snap, RoutingQuery(domain = "tracker.пример.рф")).action,
        )
    }

    @Test
    fun emptyIncludeDoesNotEnableSplitTunnel() {
        val snap = smart.copy(mode = HotfoxRoutingMode.INCLUDE_APPS, selectedApps = emptySet())
        assertFalse(snap.perAppPlan("com.hotfox.vpn").enabled)
    }

    @Test
    fun appAndLanRulesAreRejectedFromParseAndSanitize() {
        assertNull(
            HotfoxRoutingPolicy.sanitizeRule(
                RoutingRule("app", RoutingRuleKind.APP, "org.telegram.messenger", RouteAction.BLOCK),
            ),
        )
        assertNull(
            HotfoxRoutingPolicy.sanitizeRule(
                RoutingRule("lan", RoutingRuleKind.LAN, "lan", RouteAction.DIRECT),
            ),
        )
        val parsed = HotfoxRoutingPolicy.parseRules(
            HotfoxRoutingPolicy.encodeRules(
                listOf(
                    RoutingRule("app", RoutingRuleKind.APP, "org.telegram.messenger", RouteAction.DIRECT),
                    RoutingRule("lan", RoutingRuleKind.LAN, "lan", RouteAction.BLOCK),
                    RoutingRule("ok", RoutingRuleKind.DOMAIN_SUFFIX, "ads.example", RouteAction.BLOCK),
                ),
            ),
        )
        assertEquals(1, parsed.size)
        assertEquals(RoutingRuleKind.DOMAIN_SUFFIX, parsed[0].kind)
    }

    @Test
    fun decideIgnoresInjectedAppAndLanRulesAndFollowsTunControls() {
        val snap = smart.copy(
            lanAccess = true,
            rules = listOf(
                RoutingRule("app", RoutingRuleKind.APP, "org.mozilla.firefox", RouteAction.BLOCK),
                RoutingRule("lan", RoutingRuleKind.LAN, "lan", RouteAction.BLOCK),
            ),
        )
        assertEquals(
            RouteAction.DIRECT,
            HotfoxRoutingPolicy.decide(snap, RoutingQuery(ip = "192.168.0.4", lan = true)).action,
        )
        assertEquals(
            RouteAction.VPN,
            HotfoxRoutingPolicy.decide(snap, RoutingQuery(packageName = "org.mozilla.firefox")).action,
        )
        val include = snap.copy(
            mode = HotfoxRoutingMode.INCLUDE_APPS,
            selectedApps = setOf("org.telegram.messenger"),
        )
        assertEquals(
            RouteAction.DIRECT,
            HotfoxRoutingPolicy.decide(include, RoutingQuery(packageName = "org.mozilla.firefox")).action,
        )
        assertEquals(
            RouteAction.VPN,
            HotfoxRoutingPolicy.decide(include, RoutingQuery(packageName = "org.telegram.messenger")).action,
        )
    }

    @Test
    fun tunEnforcementFollowsLanAccessAndSelectedAppsNotCustomAppLanRules() {
        val self = "com.hotfox.vpn"
        val lanOn = smart.copy(
            lanAccess = true,
            rules = listOf(RoutingRule("lan", RoutingRuleKind.LAN, "lan", RouteAction.BLOCK)),
        )
        val lanTun = HotfoxRoutingDataPlane.tunEnforcement(lanOn, ipv6ProxyEnabled = false, self)
        assertFalse(lanTun.captureIpv4Default)
        assertTrue(lanTun.captureIpv6Default)
        assertFalse(lanTun.perApp.enabled)
        assertTrue(HotfoxRoutingDataPlane.xrayRules(lanOn).isEmpty())

        val globalLan = smart.copy(
            mode = HotfoxRoutingMode.GLOBAL,
            lanAccess = true,
            rules = listOf(RoutingRule("lan", RoutingRuleKind.LAN, "lan", RouteAction.DIRECT)),
        )
        val globalTun = HotfoxRoutingDataPlane.tunEnforcement(globalLan, ipv6ProxyEnabled = true, self)
        assertTrue(globalTun.captureIpv4Default)
        assertTrue(globalTun.captureIpv6Default)

        val split = smart.copy(
            mode = HotfoxRoutingMode.EXCLUDE_APPS,
            selectedApps = setOf("com.bank.app"),
            rules = listOf(
                RoutingRule("app", RoutingRuleKind.APP, "org.mozilla.firefox", RouteAction.DIRECT),
            ),
        )
        val splitTun = HotfoxRoutingDataPlane.tunEnforcement(split, ipv6ProxyEnabled = false, self)
        assertTrue(splitTun.perApp.enabled)
        assertTrue(splitTun.perApp.bypassSelected)
        assertEquals(setOf("com.bank.app"), splitTun.perApp.packages)
        assertFalse(HotfoxRoutingDataPlane.capturedOnTun(split, "com.bank.app", self))
        assertTrue(HotfoxRoutingDataPlane.capturedOnTun(split, "org.mozilla.firefox", self))
        assertTrue(HotfoxRoutingDataPlane.xrayRules(split).isEmpty())
        assertEquals(
            RouteAction.DIRECT,
            HotfoxRoutingPolicy.decide(split, RoutingQuery(packageName = "com.bank.app")).action,
        )
        assertEquals(
            RouteAction.VPN,
            HotfoxRoutingPolicy.decide(split, RoutingQuery(packageName = "org.mozilla.firefox")).action,
        )
    }
}
