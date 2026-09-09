package com.v2ray.ang.vpn

/**
 * Per-server health owned by 2.4 Smart Connection. Time-sensitive fields carry
 * timestamps. Unmeasured servers stay UNKNOWN — never pretend they are 0 ms.
 */
enum class ServerAvailability {
    UNKNOWN,
    HEALTHY,
    DEGRADED,
    DEAD,
}

data class ServerHealth(
    val guid: String,
    val subscriptionId: String = "",
    val lastProbeAtEpochMs: Long = 0L,
    val lastSuccessAtEpochMs: Long = 0L,
    val lastFailureAtEpochMs: Long = 0L,
    val latestLatencyMs: Long? = null,
    val ewmaLatencyMs: Double? = null,
    val jitterMs: Double? = null,
    val consecutiveFailures: Int = 0,
    val availability: ServerAvailability = ServerAvailability.UNKNOWN,
    val probeGeneration: Long = 0L,
    val probeInFlight: Boolean = false,
    val networkContext: Long = 0L,
) {
    fun isMeasured(): Boolean = availability != ServerAvailability.UNKNOWN

    fun hasFreshLatency(currentNetworkContext: Long): Boolean =
        isMeasured() && networkContext == currentNetworkContext && latestLatencyMs != null
}

data class ProbeSample(
    val guid: String,
    val success: Boolean,
    val latencyMs: Long? = null,
    val observedAtEpochMs: Long,
    val generation: Long,
)

object ServerHealthMath {
    const val EWMA_ALPHA = 0.3
    const val DEGRADED_LATENCY_MS = 180L
    const val STALE_AFTER_MS = 60_000L

    fun applySample(previous: ServerHealth, sample: ProbeSample): ServerHealth {
        if (sample.generation < previous.probeGeneration) {
            return previous
        }
        if (!sample.success || sample.latencyMs == null || sample.latencyMs <= 0L) {
            return previous.copy(
                lastProbeAtEpochMs = sample.observedAtEpochMs,
                lastFailureAtEpochMs = sample.observedAtEpochMs,
                consecutiveFailures = previous.consecutiveFailures + 1,
                availability = ServerAvailability.DEAD,
                probeGeneration = maxOf(previous.probeGeneration, sample.generation),
                probeInFlight = false,
            )
        }
        val latency = sample.latencyMs.toDouble()
        val ewma = previous.ewmaLatencyMs?.let { EWMA_ALPHA * latency + (1.0 - EWMA_ALPHA) * it } ?: latency
        val jitter = previous.ewmaLatencyMs?.let { kotlin.math.abs(latency - it) } ?: 0.0
        val availability = if (sample.latencyMs >= DEGRADED_LATENCY_MS) {
            ServerAvailability.DEGRADED
        } else {
            ServerAvailability.HEALTHY
        }
        return previous.copy(
            lastProbeAtEpochMs = sample.observedAtEpochMs,
            lastSuccessAtEpochMs = sample.observedAtEpochMs,
            latestLatencyMs = sample.latencyMs,
            ewmaLatencyMs = ewma,
            jitterMs = jitter,
            consecutiveFailures = 0,
            availability = availability,
            probeGeneration = maxOf(previous.probeGeneration, sample.generation),
            probeInFlight = false,
        )
    }

    fun fromCachedDelay(
        guid: String,
        delayMs: Long,
        nowEpochMs: Long = 0L,
        networkContext: Long = 0L,
    ): ServerHealth {
        return when {
            delayMs < 0L -> ServerHealth(
                guid = guid,
                lastProbeAtEpochMs = nowEpochMs,
                lastFailureAtEpochMs = nowEpochMs,
                consecutiveFailures = 1,
                availability = ServerAvailability.DEAD,
                networkContext = networkContext,
            )
            delayMs == 0L -> ServerHealth(guid = guid, networkContext = networkContext)
            else -> ServerHealth(
                guid = guid,
                lastProbeAtEpochMs = nowEpochMs,
                lastSuccessAtEpochMs = nowEpochMs,
                latestLatencyMs = delayMs,
                ewmaLatencyMs = delayMs.toDouble(),
                jitterMs = 0.0,
                availability = if (delayMs >= DEGRADED_LATENCY_MS) {
                    ServerAvailability.DEGRADED
                } else {
                    ServerAvailability.HEALTHY
                },
                networkContext = networkContext,
            )
        }
    }
}
