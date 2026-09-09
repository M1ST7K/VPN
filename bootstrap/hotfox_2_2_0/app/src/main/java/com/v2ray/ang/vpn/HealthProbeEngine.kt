package com.v2ray.ang.vpn

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Bounded health-probe cycle. Fake ping is forbidden: a timeout or cancel
 * records failure/unknown, never 0 ms. Older generations are dropped by
 * [ServerHealthRepository.record].
 */
class HealthProbeEngine(
    private val repository: ServerHealthRepository,
    private val maxConcurrent: Int = 4,
    private val perProbeTimeoutMs: Long = 8_000L,
    private val cycleTimeoutMs: Long = 45_000L,
    private val probe: suspend (guid: String) -> ProbeSample?,
) {
    companion object {
        const val MAX_CONCURRENT = 4
        const val PER_PROBE_TIMEOUT_MS = 8_000L
        const val CYCLE_TIMEOUT_MS = 45_000L
    }
    data class CycleResult(
        val generation: Long,
        val completed: Int,
        val timedOut: Boolean,
        val cancelled: Boolean,
    )

    suspend fun run(guids: List<String>, nowEpochMs: Long): CycleResult {
        if (guids.isEmpty()) {
            return CycleResult(repository.generation, 0, timedOut = false, cancelled = false)
        }
        val generation = repository.bumpGeneration()
        guids.forEach { repository.markProbeInFlight(it, generation) }
        val semaphore = Semaphore(maxConcurrent.coerceAtLeast(1))
        return try {
            val cycle = withTimeoutOrNull(cycleTimeoutMs) {
                coroutineScope {
                    guids.map { guid ->
                        async {
                            semaphore.withPermit {
                                val sample = withTimeoutOrNull(perProbeTimeoutMs) {
                                    probe(guid)
                                }
                                val recorded = sample?.copy(
                                    generation = generation,
                                    observedAtEpochMs = sample.observedAtEpochMs,
                                ) ?: ProbeSample(
                                    guid = guid,
                                    success = false,
                                    latencyMs = null,
                                    observedAtEpochMs = nowEpochMs,
                                    generation = generation,
                                )
                                repository.record(recorded)
                            }
                        }
                    }.awaitAll()
                }
                true
            }
            if (cycle == null) {
                failRemaining(guids, generation, nowEpochMs)
            }
            CycleResult(
                generation = generation,
                completed = repository.all().values.count { it.probeGeneration == generation && !it.probeInFlight },
                timedOut = cycle == null,
                cancelled = false,
            )
        } catch (_: CancellationException) {
            failRemaining(guids, generation, nowEpochMs)
            CycleResult(
                generation = generation,
                completed = repository.all().values.count { it.probeGeneration == generation && !it.probeInFlight },
                timedOut = false,
                cancelled = true,
            )
        }
    }

    private fun failRemaining(guids: List<String>, generation: Long, nowEpochMs: Long) {
        guids.forEach { guid ->
            val current = repository.snapshot(guid)
            if (current.probeInFlight && current.probeGeneration == generation) {
                repository.record(
                    ProbeSample(
                        guid = guid,
                        success = false,
                        latencyMs = null,
                        observedAtEpochMs = nowEpochMs,
                        generation = generation,
                    )
                )
            }
        }
    }
}
