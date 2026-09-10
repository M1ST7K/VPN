package com.v2ray.ang.vpn

import java.util.concurrent.ConcurrentHashMap

/**
 * Explicit owner of per-server health. Probe results from an older generation
 * never overwrite a newer generation. Latency from a previous network context
 * is not reused for AUTO ranking.
 */
class ServerHealthRepository {
    private val byGuid = ConcurrentHashMap<String, ServerHealth>()

    @Volatile
    var generation: Long = 0L
        private set

    @Volatile
    var networkContext: Long = 0L
        private set

    fun snapshot(guid: String): ServerHealth = byGuid[guid] ?: ServerHealth(guid, networkContext = networkContext)

    fun all(): Map<String, ServerHealth> = HashMap(byGuid)

    fun bumpGeneration(): Long {
        val next = generation + 1L
        generation = next
        return next
    }

    /**
     * Wi-Fi/cellular/loss/restore: drop in-flight probes and forget latency from
     * the previous network. Last-success timestamps stay for last-good bias.
     */
    fun invalidateForNetworkChange(): Long {
        val nextGeneration = bumpGeneration()
        val nextContext = networkContext + 1L
        networkContext = nextContext
        byGuid.replaceAll { _, health ->
            health.copy(
                latestLatencyMs = null,
                ewmaLatencyMs = null,
                jitterMs = null,
                consecutiveFailures = 0,
                availability = ServerAvailability.UNKNOWN,
                probeGeneration = nextGeneration,
                probeInFlight = false,
                networkContext = nextContext,
            )
        }
        return nextContext
    }

    fun markProbeInFlight(guid: String, generation: Long) {
        val current = snapshot(guid)
        if (generation < current.probeGeneration) return
        byGuid[guid] = current.copy(probeInFlight = true, probeGeneration = generation)
    }

    fun record(sample: ProbeSample): ServerHealth {
        val updated = ServerHealthMath.applySample(snapshot(sample.guid), sample)
            .copy(networkContext = networkContext)
        byGuid[guidOrKeep(updated)] = updated
        return updated
    }

    fun clearMeasured(guid: String) {
        byGuid[guid] = ServerHealth(guid = guid, probeGeneration = generation, networkContext = networkContext)
    }

    fun clearInFlight(guid: String) {
        val current = snapshot(guid)
        if (!current.probeInFlight) return
        byGuid[guid] = current.copy(probeInFlight = false)
    }

    fun remove(guid: String) {
        byGuid.remove(guid)
    }

    fun retain(guids: Set<String>) {
        byGuid.keys.removeAll { it !in guids }
    }

    fun resetForTests() {
        byGuid.clear()
        generation = 0L
        networkContext = 0L
    }

    private fun guidOrKeep(health: ServerHealth): String = health.guid
}
