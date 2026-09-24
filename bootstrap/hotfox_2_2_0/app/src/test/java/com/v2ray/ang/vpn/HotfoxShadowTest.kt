package com.v2ray.ang.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HotfoxShadowTest {
    private val cache = NetworkCapabilityCache()
    private val now = 1_700_000_000L
    private val net = 3L

    private fun path(
        guid: String,
        network: String,
        security: String = "reality",
        remarks: String = guid,
        ipv6: Boolean = false,
    ): ConnectionPath = requireNotNull(
        HotfoxPathFactory.fromCandidate(guid, network, security, remarks, ipv6),
    )

    private fun scores(vararg ids: String): Map<String, Double> = ids.associateWith { 40.0 }

    @Before
    fun reset() {
        cache.clear()
        VpnRestartGate.resetForTests()
        VpnSessionCoordinator.resetForTests()
        HotfoxShadowStore.resetForTests()
        HotfoxShadowFailover.reset()
    }

    @Test
    fun unsupportedTransportIsRejected() {
        assertNull(HotfoxTransport.parse("kcp"))
        assertNull(HotfoxTransport.parse("quic"))
        assertNull(HotfoxPathFactory.fromCandidate("a", "kcp", "reality", "a"))
        val supported = HotfoxShadowPolicy.supported(listOf(path("ok", "tcp")))
        assertEquals(1, supported.size)
    }

    @Test
    fun pathScoreIsDeterministicAndIgnoresHashOrder() {
        val a = path("a", "tcp")
        val b = path("b", "xhttp")
        val ranked1 = HotfoxPathScore.rank(listOf(a, b), scores("a", "b"), cache, now, net)
        val ranked2 = HotfoxPathScore.rank(listOf(b, a), scores("a", "b"), cache, now, net)
        assertEquals(ranked1.map { it.path.id }, ranked2.map { it.path.id })
        assertEquals(ranked1.map { it.score }, ranked2.map { it.score })
    }

    @Test
    fun primarySuccessPicksLowestScore() {
        cache.record("tcp", true, now, net)
        val preferred = HotfoxShadowPolicy.pickPreferred(
            listOf(path("slow", "xhttp"), path("fast", "tcp")),
            cache,
            mapOf("slow" to 90.0, "fast" to 20.0),
            now,
            net,
        )
        assertEquals("fast", preferred?.serverGuid)
    }

    @Test
    fun primaryFailureFallsBackToAlternateTransport() {
        val failed = path("hel", "tcp", remarks = "Хельсинки")
        val alt = path("hel-x", "xhttp", remarks = "Хельсинки")
        val decision = HotfoxShadowPolicy.fallback(
            paths = listOf(failed, alt),
            failedPath = failed,
            cache = cache,
            serverScores = scores("hel", "hel-x"),
            auto = true,
            shadowAuto = true,
            attempt = 0,
            nowEpochMs = now,
            networkContext = net,
            generation = 1L,
            currentGeneration = 1L,
        )
        assertEquals(FailoverAction.SWITCH, decision.action)
        assertEquals("alternate_transport", decision.reason)
        assertEquals(HotfoxTransport.XHTTP, decision.path?.transport)
    }

    @Test
    fun alternateServerFallbackWhenTransportExhausted() {
        val failed = path("a", "tcp", remarks = "A")
        val other = path("b", "tcp", remarks = "B")
        val decision = HotfoxShadowPolicy.fallback(
            paths = listOf(failed, other),
            failedPath = failed,
            cache = cache,
            serverScores = scores("a", "b"),
            auto = true,
            shadowAuto = true,
            attempt = 0,
            nowEpochMs = now,
            networkContext = net,
            generation = 1L,
            currentGeneration = 1L,
        )
        assertEquals("alternate_server", decision.reason)
        assertEquals("b", decision.path?.serverGuid)
    }

    @Test
    fun retryBudgetExhaustionStops() {
        val failed = path("a", "tcp")
        val decision = HotfoxShadowPolicy.fallback(
            paths = listOf(failed),
            failedPath = failed,
            cache = cache,
            serverScores = scores("a"),
            auto = true,
            shadowAuto = true,
            attempt = HotfoxShadowPolicy.ATTEMPT_CAP,
            nowEpochMs = now,
            networkContext = net,
            generation = 1L,
            currentGeneration = 1L,
        )
        assertEquals(FailoverAction.STOP, decision.action)
        assertEquals("attempt_cap", decision.reason)
    }

    @Test
    fun cancellationAndStaleGenerationStopFallback() {
        val failed = path("a", "tcp")
        val other = path("b", "ws")
        val stale = HotfoxShadowPolicy.fallback(
            paths = listOf(failed, other),
            failedPath = failed,
            cache = cache,
            serverScores = scores("a", "b"),
            auto = true,
            shadowAuto = true,
            attempt = 0,
            nowEpochMs = now,
            networkContext = net,
            generation = 1L,
            currentGeneration = 2L,
        )
        assertEquals("stale_generation", stale.reason)
        val manual = HotfoxShadowPolicy.fallback(
            paths = listOf(failed, other),
            failedPath = failed,
            cache = cache,
            serverScores = scores("a", "b"),
            auto = false,
            shadowAuto = true,
            attempt = 0,
            nowEpochMs = now,
            networkContext = net,
            generation = 1L,
            currentGeneration = 1L,
        )
        assertEquals("manual_sticky", manual.reason)
    }

    @Test
    fun networkCacheInvalidationDropsOldTransportSuccess() {
        cache.record("tcp", true, now, networkContext = 1L)
        assertTrue(cache.recentlySucceeded("tcp", now + 1000, 1L))
        cache.invalidate(2L)
        assertFalse(cache.recentlySucceeded("tcp", now + 1000, 1L))
        assertTrue(cache.live(now + 1000, 2L).isEmpty())
    }

    @Test
    fun staleCacheExpires() {
        cache.record("ws", true, now, net)
        assertFalse(cache.recentlySucceeded("ws", now + NetworkCapabilityCache.TTL_MS + 1, net))
    }

    @Test
    fun shadowAutoSelectsValidEntryExitAndRejectsLoops() {
        val entry = path("e1", "xhttp", remarks = "gate [entry]")
        val exit = path("x1", "tcp", remarks = "exit [exit]")
        assertNull(HotfoxPathFactory.shadowRoute(entry, entry.copy(role = PathRole.SHADOW_EXIT)))
        assertFalse(HotfoxShadowRoute.validate(entry, entry.copy(role = PathRole.EXIT)))
        val route = HotfoxPathFactory.shadowRoute(entry, exit)
        assertNotNull(route)
        assertEquals("e1", route!!.entryGuid)
        assertEquals("x1", route.exitGuid)
        val decision = HotfoxShadowPolicy.fallback(
            paths = listOf(entry, exit),
            failedPath = path("dead", "tcp", remarks = "dead"),
            cache = cache,
            serverScores = emptyMap(),
            auto = true,
            shadowAuto = true,
            attempt = 0,
            nowEpochMs = now,
            networkContext = net,
            generation = 1L,
            currentGeneration = 1L,
        )
        assertEquals("shadow_route", decision.reason)
        assertEquals("e1", decision.path?.entryGuid)
    }

    @Test
    fun shadowRouteNotForcedWhenShadowAutoOff() {
        val entry = path("e1", "xhttp", remarks = "gate [entry]")
        val exit = path("x1", "tcp", remarks = "exit [exit]")
        val failed = path("dead", "tcp", remarks = "dead")
        val decision = HotfoxShadowPolicy.fallback(
            paths = listOf(entry, exit, failed),
            failedPath = failed,
            cache = cache,
            serverScores = emptyMap(),
            auto = true,
            shadowAuto = false,
            attempt = 0,
            nowEpochMs = now,
            networkContext = net,
            generation = 1L,
            currentGeneration = 1L,
        )
        assertNotEquals("shadow_route", decision.reason)
    }

    @Test
    fun selfHealNeedsThresholdAndHonorsCooldown() {
        var state = HotfoxSelfHeal.State()
        state = HotfoxSelfHeal.recordDeath(state)
        assertFalse(
            HotfoxSelfHeal.shouldHeal(state, true, now, connected = true, generationCurrent = true),
        )
        state = HotfoxSelfHeal.recordDeath(state)
        assertTrue(
            HotfoxSelfHeal.shouldHeal(state, true, now, connected = true, generationCurrent = true),
        )
        state = HotfoxSelfHeal.recordHeal(state, now)
        assertFalse(
            HotfoxSelfHeal.shouldHeal(state.copy(consecutivePathDeaths = 2), true, now + 1_000, connected = true, generationCurrent = true),
        )
        assertTrue(
            HotfoxSelfHeal.shouldHeal(
                state.copy(consecutivePathDeaths = 2),
                true,
                now + HotfoxSelfHeal.COOLDOWN_MS + 1,
                connected = true,
                generationCurrent = true,
            ),
        )
        assertFalse(
            HotfoxSelfHeal.shouldHeal(state.copy(consecutivePathDeaths = 2), true, now, connected = false, generationCurrent = true),
        )
    }

    @Test
    fun dnsBootstrapHonorsTtlAndNetworkContext() {
        val dns = DnsBootstrapCache()
        dns.store("api.hotfox.example", "1.2.3.4", "2001:db8::1", now, net)
        assertEquals("1.2.3.4", dns.lookup("api.hotfox.example", now + 1000, net)?.ipv4)
        assertNull(dns.lookup("api.hotfox.example", now + 1000, networkContext = 99L))
        assertNull(dns.lookup("api.hotfox.example", now + DnsBootstrapCache.TTL_MS + 1, net))
    }

    @Test
    fun ipv6FailureDoesNotHideIpv4() {
        assertEquals(AddressFamily.IPV4, HotfoxAddressFamilyPolicy.usableFamily(ipv4Usable = true, ipv6Usable = false))
        assertEquals(AddressFamily.DUAL, HotfoxAddressFamilyPolicy.prefer(ipv4Usable = true, ipv6Usable = true))
        assertNull(HotfoxAddressFamilyPolicy.usableFamily(false, false))
    }

    @Test
    fun doctorAutoFixUsesBoundedShadowFallback() {
        val finding = ConnectionDoctor.diagnose(
            hasUnderlyingInternet = true,
            vpnPermissionGranted = true,
            entitlementUsable = true,
            primaryPathAvailable = false,
            alternatePathAvailable = true,
            dnsBootstrapReady = true,
            configSupported = true,
            localPipelineOk = true,
        )
        assertEquals(DoctorCategory.ALTERNATE_AVAILABLE, finding.category)
        assertEquals(DoctorAction.AUTO_FIX, finding.action)
        val request = VpnRestartGate.nextRequest()
        val failed = path("a", "tcp")
        val other = path("b", "xhttp")
        val decision = ConnectionDoctor.autoFix(
            finding = finding,
            paths = listOf(failed, other),
            failedPath = failed,
            cache = cache,
            serverScores = scores("a", "b"),
            auto = true,
            shadowAuto = true,
            attempt = 0,
            nowEpochMs = now,
            networkContext = net,
            generation = request,
            currentGeneration = request,
        )
        assertEquals(FailoverAction.SWITCH, decision.action)
        VpnRestartGate.invalidate()
        val cancelled = ConnectionDoctor.autoFix(
            finding = finding,
            paths = listOf(failed, other),
            failedPath = failed,
            cache = cache,
            serverScores = scores("a", "b"),
            auto = true,
            shadowAuto = true,
            attempt = 0,
            nowEpochMs = now,
            networkContext = net,
            generation = request,
            currentGeneration = request,
        )
        assertEquals("doctor_stale_restart", cancelled.reason)
    }

    @Test
    fun doctorDoesNotExposeSecrets() {
        val finding = ConnectionDoctor.diagnose(
            hasUnderlyingInternet = false,
            vpnPermissionGranted = true,
            entitlementUsable = true,
            primaryPathAvailable = true,
            alternatePathAvailable = false,
            dnsBootstrapReady = true,
            configSupported = true,
            localPipelineOk = true,
        )
        assertFalse(finding.detail.contains("vless://"))
        assertFalse(finding.title.contains("uuid", ignoreCase = true))
    }

    @Test
    fun fallbackNeverSelectsNoneSecurityOverReality() {
        val reality = path("r", "tcp", security = "reality")
        assertEquals(HotfoxSecurity.REALITY, reality.security)
        val none = path("n", "tcp", security = "none")
        assertFalse(HotfoxShadowRoute.validate(none.copy(role = PathRole.SHADOW_ENTRY), reality.copy(role = PathRole.SHADOW_EXIT)))
    }

    @Test
    fun selectingCopyIsUserFacingNotProtocolDump() {
        assertEquals("Подбираем защищённый маршрут…", HotfoxShadowPolicy.SELECTING_COPY)
        assertEquals(
            HotfoxResolvedTargetDisplay.SHADOW_SELECTING_COPY,
            HotfoxResolvedTargetDisplay.serverLabel(
                auto = true,
                connecting = true,
                city = null,
                shadowAuto = true,
            ),
        )
        assertFalse(HotfoxShadowPolicy.SELECTING_COPY.contains("VLESS"))
        assertFalse(HotfoxShadowPolicy.SELECTING_COPY.contains("REALITY"))
    }

    @Test
    fun noSupportedTransportStops() {
        val decision = HotfoxShadowPolicy.fallback(
            paths = emptyList(),
            failedPath = path("a", "tcp"),
            cache = cache,
            serverScores = emptyMap(),
            auto = true,
            shadowAuto = true,
            attempt = 0,
            nowEpochMs = now,
            networkContext = net,
            generation = 1L,
            currentGeneration = 1L,
        )
        assertEquals("no_supported_transport", decision.reason)
    }
}
