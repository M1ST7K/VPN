package com.v2ray.ang.vpn

import com.v2ray.ang.commerce.CommercePreferences
import com.v2ray.ang.commerce.EntitlementMetadata
import com.v2ray.ang.commerce.EntitlementStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

class HotfoxAutoSelectionTest {
    @Before
    fun resetHealth() {
        HotfoxServerSelection.health.resetForTests()
        HotfoxServerSelection.resetLastGoodForTests()
        com.v2ray.ang.ops.HotfoxNodeDrain.resetForTests()
        com.v2ray.ang.ops.HotfoxControlPlane.resetForTests()
    }

    @Test
    fun noMeasuredServersFallsBackToFirstUntested() {
        val servers = listOf(
            HotfoxServerSelection.Candidate("a", "A", 0L),
            HotfoxServerSelection.Candidate("b", "B", 0L),
        )
        val result = HotfoxServerSelection.pick(servers, auto = true, selectedGuid = null)
        assertEquals(HotfoxServerSelection.ResolveResult.Success("a", true), result)
        val reason = AutoSelectionPolicy.diagnosticReason(result, emptyMap(), 0L)
        assertTrue(reason.contains("untested fallback"))
    }

    @Test
    fun oneHealthyServerIsSelected() {
        val servers = listOf(
            HotfoxServerSelection.Candidate("dead", "Dead", -1L),
            HotfoxServerSelection.Candidate("ok", "Ok", 41L),
        )
        val result = HotfoxServerSelection.pick(servers, auto = true, selectedGuid = "dead")
        assertEquals(HotfoxServerSelection.ResolveResult.Success("ok", true), result)
    }

    @Test
    fun clearWinnerIsTheLowestScore() {
        val servers = listOf(
            HotfoxServerSelection.Candidate("slow", "Slow", 90L),
            HotfoxServerSelection.Candidate("fast", "Fast", 28L),
            HotfoxServerSelection.Candidate("dead", "Dead", -1L),
        )
        val result = HotfoxServerSelection.pick(servers, auto = true, selectedGuid = "slow")
        assertEquals(HotfoxServerSelection.ResolveResult.Success("fast", true), result)
        val health = mapOf("fast" to ServerHealthMath.fromCachedDelay("fast", 28L, 1_000L))
        val reason = AutoSelectionPolicy.diagnosticReason(result, health, 1_000L)
        assertTrue(reason.contains("28 ms"))
        assertTrue(reason.contains("0 recent failures"))
    }

    @Test
    fun tinyLatencyDifferenceStaysUnderHysteresis() {
        val servers = listOf(
            HotfoxServerSelection.Candidate("current", "Current", 80L),
            HotfoxServerSelection.Candidate("near", "Near", 70L),
        )
        val result = HotfoxServerSelection.resolveForHandover(servers, auto = true, selectedGuid = "current")
        assertEquals(HotfoxServerSelection.ResolveResult.Success("current", true), result)
    }

    @Test
    fun staleFastResultLosesToFreshSlightlySlower() {
        val now = 200_000L
        val servers = listOf(
            HotfoxServerSelection.Candidate("stale-fast", "Stale", 20L),
            HotfoxServerSelection.Candidate("fresh", "Fresh", 40L),
        )
        val health = mapOf(
            "stale-fast" to ServerHealth(
                guid = "stale-fast",
                lastProbeAtEpochMs = now - 90_000L,
                lastSuccessAtEpochMs = now - 90_000L,
                latestLatencyMs = 20L,
                ewmaLatencyMs = 20.0,
                jitterMs = 0.0,
                availability = ServerAvailability.HEALTHY,
            ),
            "fresh" to ServerHealth(
                guid = "fresh",
                lastProbeAtEpochMs = now - 1_000L,
                lastSuccessAtEpochMs = now - 1_000L,
                latestLatencyMs = 40L,
                ewmaLatencyMs = 40.0,
                jitterMs = 0.0,
                availability = ServerAvailability.HEALTHY,
            ),
        )
        val result = HotfoxServerSelection.pick(
            servers,
            auto = true,
            selectedGuid = "stale-fast",
            healthByGuid = health,
            nowEpochMs = now,
        )
        assertEquals(HotfoxServerSelection.ResolveResult.Success("fresh", true), result)
        assertTrue(AutoSelectionPolicy.isStale(health.getValue("stale-fast"), now))
        assertFalse(AutoSelectionPolicy.isStale(health.getValue("fresh"), now))
    }

    @Test
    fun currentHealthyHandoverKeepsTarget() {
        val servers = listOf(
            HotfoxServerSelection.Candidate("amsterdam", "Amsterdam", 80L),
            HotfoxServerSelection.Candidate("frankfurt", "Frankfurt", 70L),
        )
        val result = HotfoxServerSelection.resolveForHandover(servers, auto = true, selectedGuid = "amsterdam")
        assertEquals(HotfoxServerSelection.ResolveResult.Success("amsterdam", true), result)
    }

    @Test
    fun currentDeadHandoverPicksNextHealthy() {
        val servers = listOf(
            HotfoxServerSelection.Candidate("amsterdam", "Amsterdam", -1L),
            HotfoxServerSelection.Candidate("frankfurt", "Frankfurt", 20L),
        )
        val result = HotfoxServerSelection.resolveForHandover(servers, auto = true, selectedGuid = "amsterdam")
        assertEquals(HotfoxServerSelection.ResolveResult.Success("frankfurt", true), result)
    }

    @Test
    fun repeatedFailuresMarkDeadAndAreSkipped() {
        val repo = ServerHealthRepository()
        var health = ServerHealth(guid = "a")
        repeat(3) { index ->
            health = ServerHealthMath.applySample(
                health,
                ProbeSample(
                    guid = "a",
                    success = false,
                    latencyMs = null,
                    observedAtEpochMs = 1_000L + index,
                    generation = 1L,
                ),
            )
            repo.record(
                ProbeSample(
                    guid = "a",
                    success = false,
                    latencyMs = null,
                    observedAtEpochMs = 1_000L + index,
                    generation = 1L,
                ),
            )
        }
        assertEquals(ServerAvailability.DEAD, health.availability)
        assertEquals(3, health.consecutiveFailures)
        assertNull(AutoSelectionPolicy.score(health, 2_000L))
        val servers = listOf(
            HotfoxServerSelection.Candidate("a", "A", -1L),
            HotfoxServerSelection.Candidate("b", "B", 40L),
        )
        val result = HotfoxServerSelection.pick(servers, auto = true, selectedGuid = "a", healthByGuid = repo.all())
        assertEquals(HotfoxServerSelection.ResolveResult.Success("b", true), result)
    }

    @Test
    fun recoveryAfterFailuresResetsConsecutiveCount() {
        var health = ServerHealth(guid = "a")
        health = ServerHealthMath.applySample(
            health,
            ProbeSample("a", success = false, observedAtEpochMs = 1L, generation = 1L),
        )
        health = ServerHealthMath.applySample(
            health,
            ProbeSample("a", success = true, latencyMs = 35L, observedAtEpochMs = 2L, generation = 1L),
        )
        assertEquals(0, health.consecutiveFailures)
        assertEquals(ServerAvailability.HEALTHY, health.availability)
        assertEquals(35L, health.latestLatencyMs)
    }

    @Test
    fun probeTimeoutRecordsFailureNeverZeroMs() = runBlocking {
        val repo = ServerHealthRepository()
        val engine = HealthProbeEngine(
            repository = repo,
            maxConcurrent = 1,
            perProbeTimeoutMs = 40L,
            cycleTimeoutMs = 1_000L,
        ) {
            delay(500)
            ProbeSample(it, success = true, latencyMs = 12L, observedAtEpochMs = 5L, generation = 0L)
        }
        val result = engine.run(listOf("slow"), nowEpochMs = 10L)
        val health = repo.snapshot("slow")
        assertFalse(result.cancelled)
        assertEquals(ServerAvailability.DEAD, health.availability)
        assertNull(health.latestLatencyMs)
        assertNotEquals(0L, health.latestLatencyMs ?: -1L)
        assertTrue(health.consecutiveFailures >= 1)
    }

    @Test
    fun probeCancellationDoesNotInventZeroMs() = runBlocking {
        val repo = ServerHealthRepository()
        val engine = HealthProbeEngine(
            repository = repo,
            maxConcurrent = 1,
            perProbeTimeoutMs = 5_000L,
            cycleTimeoutMs = 5_000L,
        ) {
            delay(10_000)
            ProbeSample(it, success = true, latencyMs = 12L, observedAtEpochMs = 5L, generation = 0L)
        }
        val job = launch { engine.run(listOf("hang"), nowEpochMs = 10L) }
        delay(40)
        job.cancel()
        job.join()
        val health = repo.snapshot("hang")
        assertTrue(health.latestLatencyMs == null || health.latestLatencyMs < 0L)
        assertNotEquals(0L, health.latestLatencyMs ?: -1L)
    }

    @Test
    fun moreThanOneHundredServersStayWithinBoundedConcurrency() = runBlocking {
        val repo = ServerHealthRepository()
        val inFlight = AtomicInteger(0)
        val peak = AtomicInteger(0)
        val engine = HealthProbeEngine(
            repository = repo,
            maxConcurrent = HealthProbeEngine.MAX_CONCURRENT,
            perProbeTimeoutMs = 2_000L,
            cycleTimeoutMs = 20_000L,
        ) { guid ->
            val now = inFlight.incrementAndGet()
            peak.accumulateAndGet(now) { current, incoming -> max(current, incoming) }
            delay(15)
            inFlight.decrementAndGet()
            ProbeSample(guid, success = true, latencyMs = 30L, observedAtEpochMs = 20L, generation = 0L)
        }
        val guids = (1..120).map { "s$it" }
        val result = engine.run(guids, nowEpochMs = 20L)
        assertFalse(result.timedOut)
        assertFalse(result.cancelled)
        assertEquals(120, result.completed)
        assertTrue(peak.get() <= HealthProbeEngine.MAX_CONCURRENT)
        assertTrue(peak.get() >= 1)
        guids.forEach { guid ->
            assertEquals(30L, repo.snapshot(guid).latestLatencyMs)
        }
    }

    @Test
    fun olderGenerationCannotOverwriteNewerHealth() {
        val repo = ServerHealthRepository()
        repo.bumpGeneration()
        repo.record(
            ProbeSample("a", success = true, latencyMs = 40L, observedAtEpochMs = 10L, generation = 1L),
        )
        repo.bumpGeneration()
        repo.markProbeInFlight("a", 2L)
        val ignored = repo.record(
            ProbeSample("a", success = true, latencyMs = 9L, observedAtEpochMs = 11L, generation = 1L),
        )
        assertEquals(40L, ignored.latestLatencyMs)
        assertEquals(2L, ignored.probeGeneration)
    }

    @Test
    fun networkLossMidProbeIsFailureNotZero() = runBlocking {
        val repo = ServerHealthRepository()
        val engine = HealthProbeEngine(
            repository = repo,
            maxConcurrent = 2,
            perProbeTimeoutMs = 30L,
            cycleTimeoutMs = 80L,
        ) {
            delay(10_000)
            ProbeSample(it, success = true, latencyMs = 1L, observedAtEpochMs = 1L, generation = 0L)
        }
        val result = engine.run(listOf("a", "b"), nowEpochMs = 3L)
        assertTrue(result.timedOut || repo.snapshot("a").availability == ServerAvailability.DEAD)
        assertNull(repo.snapshot("a").latestLatencyMs)
        assertNull(repo.snapshot("b").latestLatencyMs)
        assertNotEquals(0L, repo.snapshot("a").latestLatencyMs ?: -1L)
    }

    @Test
    fun probeEngineRunsASingleBoundedCycle() = runBlocking {
        val repo = ServerHealthRepository()
        val cycles = AtomicInteger(0)
        val engine = HealthProbeEngine(
            repository = repo,
            maxConcurrent = 2,
            perProbeTimeoutMs = 200L,
            cycleTimeoutMs = 1_000L,
        ) {
            cycles.incrementAndGet()
            ProbeSample(it, success = true, latencyMs = 22L, observedAtEpochMs = 4L, generation = 0L)
        }
        engine.run(listOf("a", "b"), nowEpochMs = 4L)
        assertEquals(2, cycles.get())
        assertEquals(1L, repo.generation)
    }

    @Test
    fun manualSelectionIsImmuneToAutoFailover() {
        val servers = listOf(
            HotfoxServerSelection.Candidate("slow", "Slow", 120L),
            HotfoxServerSelection.Candidate("fast", "Fast", 12L),
        )
        val pick = HotfoxServerSelection.pick(servers, auto = false, selectedGuid = "slow")
        assertEquals(HotfoxServerSelection.ResolveResult.Success("slow", false), pick)
        val failover = AutoSelectionPolicy.failover(
            servers = servers,
            healthByGuid = emptyMap(),
            auto = false,
            selectedGuid = "slow",
            attempt = 0,
        )
        assertEquals(FailoverAction.STOP, failover.action)
        assertEquals("manual_sticky", failover.reason)
        assertEquals("slow", failover.guid)
    }

    @Test
    fun switchingBackToAutoKeepsModeWithoutCopyingManualGuidAsAutoRow() {
        val restored = HotfoxServerSelection.persistAfterTap(
            tapGuid = HotfoxServerSelection.AUTO_GUID,
            previousGuid = "frankfurt",
            firstUsableGuid = "amsterdam",
        )
        assertTrue(restored.auto)
        assertEquals("frankfurt", restored.selectedGuid)
        assertNotEquals(HotfoxServerSelection.AUTO_GUID, restored.selectedGuid)
    }

    @Test
    fun resolvedServerRemovedOnRefreshFallsBackWithoutLeavingAuto() {
        val kept = HotfoxServerSelection.persistAfterEnsureValid(
            auto = true,
            selectedGuid = "gone",
            inventory = listOf("paris", "berlin"),
        )
        assertTrue(kept.auto)
        assertEquals("paris", kept.selectedGuid)
    }

    @Test
    fun allUnhealthyYieldsActionableFailure() {
        val servers = listOf(
            HotfoxServerSelection.Candidate("a", "A", -1L),
            HotfoxServerSelection.Candidate("b", "B", -1L),
        )
        val result = HotfoxServerSelection.pick(servers, auto = true, selectedGuid = "a")
        assertTrue(result is HotfoxServerSelection.ResolveResult.Failure)
        assertTrue((result as HotfoxServerSelection.ResolveResult.Failure).message.contains("HF-VPN-011"))
        val failover = AutoSelectionPolicy.failover(
            servers = servers,
            healthByGuid = emptyMap(),
            auto = true,
            selectedGuid = "a",
            attempt = 0,
        )
        assertEquals(FailoverAction.STOP, failover.action)
        assertEquals("all_unhealthy", failover.reason)
    }

    @Test
    fun failoverAttemptCapStopsAfterBound() {
        val servers = listOf(
            HotfoxServerSelection.Candidate("a", "A", 40L),
            HotfoxServerSelection.Candidate("b", "B", 50L),
        )
        val atCap = AutoSelectionPolicy.failover(
            servers = servers,
            healthByGuid = emptyMap(),
            auto = true,
            selectedGuid = "a",
            attempt = AutoSelectionPolicy.FAILOVER_ATTEMPT_CAP,
        )
        assertEquals(FailoverAction.STOP, atCap.action)
        assertEquals("attempt_cap", atCap.reason)
        val first = AutoSelectionPolicy.failover(
            servers = listOf(
                HotfoxServerSelection.Candidate("a", "A", -1L),
                HotfoxServerSelection.Candidate("b", "B", 20L),
            ),
            healthByGuid = emptyMap(),
            auto = true,
            selectedGuid = "a",
            attempt = 0,
        )
        assertEquals(FailoverAction.SWITCH, first.action)
        assertEquals("b", first.guid)
        assertEquals(1, first.attempt)
        assertTrue(first.reason.contains("failover_previous_unreachable"))
    }

    @Test
    fun ewmaAndDegradedScoreAreDeterministic() {
        val healthy = ServerHealthMath.fromCachedDelay("ok", 40L, 1_000L)
        val degraded = ServerHealthMath.fromCachedDelay("slow", 200L, 1_000L)
        assertEquals(40.0, AutoSelectionPolicy.score(healthy, 1_000L)!!, 0.01)
        assertEquals(200.0 + AutoSelectionPolicy.DEGRADED_PENALTY_MS, AutoSelectionPolicy.score(degraded, 1_000L)!!, 0.01)
        val afterJitter = ServerHealthMath.applySample(
            healthy,
            ProbeSample("ok", success = true, latencyMs = 70L, observedAtEpochMs = 1_100L, generation = 1L),
        )
        val expectedEwma = 0.3 * 70.0 + 0.7 * 40.0
        val expectedJitter = kotlin.math.abs(70.0 - 40.0)
        assertEquals(expectedEwma + 0.5 * expectedJitter, AutoSelectionPolicy.score(afterJitter, 1_100L)!!, 0.01)
    }

    @Test
    fun latencyDisplayNeverShowsZeroMs() {
        assertEquals("—", HotfoxLatencyDisplay.format(delayMs = 0L))
        assertEquals("—", HotfoxLatencyDisplay.format(delayMs = null))
        assertEquals("…", HotfoxLatencyDisplay.format(probing = true))
        assertEquals("Недоступен", HotfoxLatencyDisplay.format(delayMs = -1L))
        assertEquals("31 ms", HotfoxLatencyDisplay.format(delayMs = 31L))
        assertEquals(
            "Недоступен",
            HotfoxLatencyDisplay.format(health = ServerHealth(guid = "a", availability = ServerAvailability.DEAD)),
        )
        assertFalse(HotfoxLatencyDisplay.format(delayMs = 0L).contains("0 ms"))
    }

    @Test
    fun coldStartBiasesToLastGoodEligibleCandidate() {
        val servers = listOf(
            HotfoxServerSelection.Candidate("paris", "Paris", 0L),
            HotfoxServerSelection.Candidate("berlin", "Berlin", 0L),
            HotfoxServerSelection.Candidate("helsinki", "Helsinki", 0L),
        )
        val result = HotfoxServerSelection.pick(
            servers,
            auto = true,
            selectedGuid = null,
            lastGoodGuid = "berlin",
        )
        assertEquals(HotfoxServerSelection.ResolveResult.Success("berlin", true), result)
    }

    @Test
    fun invalidCandidateDoesNotPoisonEligibleSet() {
        val servers = listOf(
            HotfoxServerSelection.Candidate("bad", "Bad", 12L, hasConfig = false),
            HotfoxServerSelection.Candidate(
                guid = "paid",
                remarks = "Paid",
                delay = 18L,
                requiresEntitlement = true,
                entitlementUsable = false,
            ),
            HotfoxServerSelection.Candidate("ok", "Ok", 40L),
        )
        val result = HotfoxServerSelection.pick(servers, auto = true, selectedGuid = "bad")
        assertEquals(HotfoxServerSelection.ResolveResult.Success("ok", true), result)
        val counts = AutoCandidateFilter.filteredCounts(servers)
        assertEquals(1, counts[AutoFilterReason.ELIGIBLE])
        assertEquals(1, counts[AutoFilterReason.MISSING_CONFIG])
        assertEquals(1, counts[AutoFilterReason.ENTITLEMENT_BLOCKED])
    }

    @Test
    fun manualHttpsServerIsNotEntitlementGated() {
        val servers = listOf(
            HotfoxServerSelection.Candidate(
                guid = "manual",
                remarks = "Manual",
                delay = 33L,
                requiresEntitlement = false,
                entitlementUsable = false,
            ),
        )
        val result = HotfoxServerSelection.pick(servers, auto = true, selectedGuid = null)
        assertEquals(HotfoxServerSelection.ResolveResult.Success("manual", true), result)
    }

    @Test
    fun networkChangeDoesNotUseOldLatencyToFlap() {
        val servers = listOf(
            HotfoxServerSelection.Candidate("current", "Current", 90L),
            HotfoxServerSelection.Candidate("wifi-fast", "Fast", 12L),
        )
        val health = mapOf(
            "current" to ServerHealthMath.fromCachedDelay("current", 90L, 1_000L, networkContext = 0L),
            "wifi-fast" to ServerHealthMath.fromCachedDelay("wifi-fast", 12L, 1_000L, networkContext = 0L),
        )
        val kept = HotfoxServerSelection.resolveForHandover(
            servers,
            auto = true,
            selectedGuid = "current",
            healthByGuid = health,
            nowEpochMs = 1_000L,
            networkChanged = true,
            networkContext = 1L,
        )
        assertEquals(HotfoxServerSelection.ResolveResult.Success("current", true), kept)
        assertNull(AutoSelectionPolicy.score(health.getValue("wifi-fast"), 1_000L, networkContext = 1L))
    }

    @Test
    fun networkInvalidationDropsLatencyAndKeepsLastSuccess() {
        val repo = HotfoxServerSelection.health
        repo.record(ProbeSample("a", success = true, latencyMs = 22L, observedAtEpochMs = 5L, generation = 1L))
        val before = repo.snapshot("a")
        assertEquals(22L, before.latestLatencyMs)
        repo.invalidateForNetworkChange()
        val after = repo.snapshot("a")
        assertEquals(ServerAvailability.UNKNOWN, after.availability)
        assertNull(after.latestLatencyMs)
        assertEquals(5L, after.lastSuccessAtEpochMs)
        assertEquals(1L, repo.networkContext)
        val resurrected = HotfoxServerSelection.healthSnapshot(
            listOf(HotfoxServerSelection.Candidate("a", "A", 22L)),
            nowEpochMs = 9L,
        )
        assertEquals(ServerAvailability.UNKNOWN, resurrected.getValue("a").availability)
        assertNull(resurrected.getValue("a").latestLatencyMs)
    }

    @Test
    fun selectingLabelDoesNotClaimProtected() {
        assertEquals(
            HotfoxResolvedTargetDisplay.SELECTING_COPY,
            HotfoxResolvedTargetDisplay.serverLabel(
                auto = true,
                connecting = true,
                city = null,
                stage = VpnConnectionStage.RESOLVE_SERVER,
            ),
        )
        assertEquals(
            "Авто · Хельсинки",
            HotfoxResolvedTargetDisplay.serverLabel(
                auto = true,
                connecting = false,
                city = "Хельсинки",
            ),
        )
        assertEquals(
            HotfoxResolvedTargetDisplay.Phase.RESOLVED,
            HotfoxResolvedTargetDisplay.phase(
                auto = true,
                connecting = true,
                resolvedRemark = "Хельсинки",
                stage = VpnConnectionStage.RESOLVE_SERVER,
            ),
        )
        assertFalse(
            ConnectionUiMapper.isProtectedHeadline(VpnSessionState.PREPARING),
        )
    }

    @Test
    fun candidateFromUsesRealConfigAndEntitlementState() {
        val now = 1_700_000_000L
        val blocked = AutoCommercialEligibility.from(
            accessOrigin = CommercePreferences.ORIGIN_HOTFOX,
            managedSubscriptionId = "hotfox-sub",
            hasCredential = false,
            metadata = null,
            nowEpochSeconds = now,
        )
        val usable = AutoCommercialEligibility.from(
            accessOrigin = CommercePreferences.ORIGIN_HOTFOX,
            managedSubscriptionId = "hotfox-sub",
            hasCredential = true,
            metadata = EntitlementMetadata(
                status = EntitlementStatus.ACTIVE,
                startsAtEpochSeconds = now - 10,
                expiresAtEpochSeconds = now + 86_400L,
                planId = "plan_1m",
                orderId = "ord-1",
            ),
            nowEpochSeconds = now,
        )
        val invalid = vlessProfile("bad", server = "", subscriptionId = "hotfox-sub")
        val disabled = vlessProfile("off", subscriptionId = "hotfox-sub")
        val paid = vlessProfile("Paid", subscriptionId = "hotfox-sub")
        val ok = vlessProfile("Ok", subscriptionId = "hotfox-sub")
        val manual = vlessProfile("Manual", subscriptionId = "https-manual")
        val servers = listOf(
            HotfoxServerSelection.candidateFrom("bad", invalid, 12L, subscriptionEnabled = true, eligibility = usable),
            HotfoxServerSelection.candidateFrom("off", disabled, 15L, subscriptionEnabled = false, eligibility = usable),
            HotfoxServerSelection.candidateFrom("paid", paid, 18L, subscriptionEnabled = true, eligibility = blocked),
            HotfoxServerSelection.candidateFrom("ok", ok, 40L, subscriptionEnabled = true, eligibility = usable),
            HotfoxServerSelection.candidateFrom("manual", manual, 33L, subscriptionEnabled = true, eligibility = blocked),
        )
        assertFalse(servers[0].hasConfig)
        assertTrue(servers[1].disabled)
        assertTrue(servers[2].requiresEntitlement)
        assertFalse(servers[2].entitlementUsable)
        assertTrue(servers[3].hasConfig)
        assertTrue(servers[3].requiresEntitlement)
        assertTrue(servers[3].entitlementUsable)
        assertFalse(servers[4].requiresEntitlement)
        val result = HotfoxServerSelection.pick(servers, auto = true, selectedGuid = "paid")
        assertEquals(HotfoxServerSelection.ResolveResult.Success("ok", true), result)
        val counts = AutoCandidateFilter.filteredCounts(servers)
        assertEquals(2, counts[AutoFilterReason.ELIGIBLE])
        assertEquals(1, counts[AutoFilterReason.MISSING_CONFIG])
        assertEquals(1, counts[AutoFilterReason.DISABLED])
        assertEquals(1, counts[AutoFilterReason.ENTITLEMENT_BLOCKED])
    }

    @Test
    fun unscopedPersistedDelayDoesNotSelectChallengerAfterNetworkChange() {
        val servers = listOf(
            HotfoxServerSelection.Candidate("current", "Current", 90L, delayNetworkScoped = false),
            HotfoxServerSelection.Candidate("wifi-fast", "Fast", 12L, delayNetworkScoped = false),
        )
        HotfoxServerSelection.health.record(
            ProbeSample("current", success = true, latencyMs = 90L, observedAtEpochMs = 1_000L, generation = 1L),
        )
        HotfoxServerSelection.health.record(
            ProbeSample("wifi-fast", success = true, latencyMs = 12L, observedAtEpochMs = 1_000L, generation = 1L),
        )
        HotfoxServerSelection.invalidateForNetworkChange()
        val snapshot = HotfoxServerSelection.healthSnapshot(servers, nowEpochMs = 2_000L)
        assertEquals(ServerAvailability.UNKNOWN, snapshot.getValue("wifi-fast").availability)
        assertNull(snapshot.getValue("wifi-fast").latestLatencyMs)
        assertNull(AutoSelectionPolicy.score(snapshot.getValue("wifi-fast"), 2_000L, networkContext = 1L))
        val ranked = HotfoxServerSelection.pick(
            servers,
            auto = true,
            selectedGuid = "current",
            healthByGuid = snapshot,
            nowEpochMs = 2_000L,
            lastGoodGuid = "current",
            networkContext = HotfoxServerSelection.health.networkContext,
        )
        assertEquals(HotfoxServerSelection.ResolveResult.Success("current", true), ranked)
        val sticky = HotfoxServerSelection.resolveForHandover(
            servers,
            auto = true,
            selectedGuid = "current",
            healthByGuid = snapshot,
            nowEpochMs = 2_000L,
            lastGoodGuid = "current",
            networkChanged = true,
            networkContext = HotfoxServerSelection.health.networkContext,
        )
        assertEquals(HotfoxServerSelection.ResolveResult.Success("current", true), sticky)
        val emptyRepo = HotfoxServerSelection.healthSnapshot(
            servers,
            nowEpochMs = 2_000L,
            stored = emptyMap(),
        )
        assertEquals(ServerAvailability.UNKNOWN, emptyRepo.getValue("wifi-fast").availability)
        assertNull(emptyRepo.getValue("wifi-fast").latestLatencyMs)
        val notByStaleDelay = HotfoxServerSelection.pick(
            servers,
            auto = true,
            selectedGuid = null,
            healthByGuid = emptyRepo,
            nowEpochMs = 2_000L,
            lastGoodGuid = "current",
            networkContext = HotfoxServerSelection.health.networkContext,
        )
        assertEquals(HotfoxServerSelection.ResolveResult.Success("current", true), notByStaleDelay)
    }

    private fun vlessProfile(
        remarks: String,
        server: String = "vpn.example",
        subscriptionId: String,
    ): com.v2ray.ang.dto.entities.ProfileItem {
        return com.v2ray.ang.dto.entities.ProfileItem.create(com.v2ray.ang.enums.EConfigType.VLESS).apply {
            this.remarks = remarks
            this.server = server
            this.serverPort = "443"
            this.password = "credential-a"
            this.security = "reality"
            this.publicKey = "pubkey"
            this.network = "tcp"
            this.subscriptionId = subscriptionId
        }
    }
}
