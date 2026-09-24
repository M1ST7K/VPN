package com.v2ray.ang.vpn

/**
 * AUTO failover after a server-target failure. Manual selection is never
 * rewritten. TUN/bind/lifecycle failures are not treated as a peer to switch to.
 */
object HotfoxAutoFailover {
    val SERVER_TARGET_CODES = setOf("HF-VPN-003", "HF-VPN-004", "HF-VPN-006", "HF-VPN-014")

    @Volatile
    var attempts: Int = 0
        private set

    fun reset() {
        attempts = 0
    }

    fun isServerTargetFailure(code: String): Boolean = code in SERVER_TARGET_CODES

    fun consider(
        code: String,
        auto: Boolean,
        failedGuid: String?,
        servers: List<HotfoxServerSelection.Candidate>,
        healthByGuid: Map<String, ServerHealth> = emptyMap(),
        nowEpochMs: Long = 0L,
        networkContext: Long = 0L,
    ): FailoverDecision {
        if (!isServerTargetFailure(code)) {
            return FailoverDecision(FailoverAction.STOP, failedGuid, "not_server_failure", attempts)
        }
        val health = healthByGuid.toMutableMap()
        if (failedGuid != null) {
            val dead = ServerHealthMath.fromCachedDelay(failedGuid, -1L, nowEpochMs, networkContext)
            health[failedGuid] = dead
        }
        val decision = AutoSelectionPolicy.failover(
            servers = servers,
            healthByGuid = health,
            auto = auto,
            selectedGuid = failedGuid,
            attempt = attempts,
            nowEpochMs = nowEpochMs,
            networkContext = networkContext,
        )
        if (decision.action != FailoverAction.STOP) {
            attempts = decision.attempt
        }
        return decision
    }

    fun apply(decision: FailoverDecision) {
        if (decision.action == FailoverAction.SWITCH && !decision.guid.isNullOrBlank()) {
            HotfoxServerSelection.persistAutoTarget(decision.guid)
            HotfoxServerSelection.recordAutoReason("failover: previous unreachable")
        }
    }

    fun considerLive(code: String, nowEpochMs: Long = System.currentTimeMillis()): FailoverDecision {
        val failed = com.v2ray.ang.handler.MmkvManager.getSelectServer()
        if (failed != null && isServerTargetFailure(code)) {
            HotfoxServerSelection.ingestProbeResult(
                guid = failed,
                delayMs = -1L,
                generation = HotfoxServerSelection.health.generation,
                nowEpochMs = nowEpochMs,
            )
        }
        val servers = HotfoxServerSelection.currentCandidates()
        val decision = consider(
            code = code,
            auto = HotfoxServerSelection.isAutoMode(),
            failedGuid = failed,
            servers = servers,
            healthByGuid = HotfoxServerSelection.healthSnapshot(servers, nowEpochMs),
            nowEpochMs = nowEpochMs,
            networkContext = HotfoxServerSelection.health.networkContext,
        )
        apply(decision)
        return decision
    }
}
