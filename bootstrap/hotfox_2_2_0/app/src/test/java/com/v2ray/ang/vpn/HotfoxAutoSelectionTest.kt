package com.v2ray.ang.vpn

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
}
