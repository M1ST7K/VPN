package com.v2ray.ang.vpn

/**
 * Extends 2.4 AUTO failover with transport/path fallback. Manual stays sticky.
 * Restart remains generation-owned via [VpnRestartGate].
 */
object HotfoxShadowFailover {
    @Volatile
    var attempts: Int = 0
        private set

    fun reset() {
        attempts = 0
        HotfoxAutoFailover.reset()
    }

    fun consider(
        code: String,
        auto: Boolean,
        shadowAuto: Boolean,
        failedGuid: String?,
        servers: List<HotfoxServerSelection.Candidate>,
        healthByGuid: Map<String, ServerHealth> = emptyMap(),
        nowEpochMs: Long = 0L,
        networkContext: Long = 0L,
        generation: Long = 0L,
        currentGeneration: Long = 0L,
    ): FailoverDecision {
        val base = HotfoxAutoFailover.consider(
            code = code,
            auto = auto,
            failedGuid = failedGuid,
            servers = servers,
            healthByGuid = healthByGuid,
            nowEpochMs = nowEpochMs,
            networkContext = networkContext,
        )
        if (!auto || !shadowAuto) return base
        if (base.action == FailoverAction.SWITCH) {
            attempts = base.attempt
            return base
        }
        val scores = healthByGuid.mapNotNull { (guid, health) ->
            AutoSelectionPolicy.score(health, nowEpochMs, networkContext)?.let { guid to it }
        }.toMap()
        val failedPath = servers.firstOrNull { it.guid == failedGuid }?.let {
            HotfoxPathFactory.fromCandidate(it.guid, it.network, it.security, it.remarks, it.ipv6)
        }
        val decision = HotfoxShadowPolicy.fallback(
            paths = HotfoxShadowPolicy.pathsFrom(servers),
            failedPath = failedPath,
            cache = HotfoxShadowStore.cache,
            serverScores = scores,
            auto = auto,
            shadowAuto = shadowAuto,
            attempt = attempts,
            nowEpochMs = nowEpochMs,
            networkContext = networkContext,
            generation = generation,
            currentGeneration = currentGeneration,
        )
        if (decision.action != FailoverAction.STOP) {
            attempts = decision.attempt
            decision.path?.let { HotfoxShadowStore.rememberPath(it, decision.reason) }
        }
        return decision.toFailover()
    }

    fun considerLive(code: String, nowEpochMs: Long = System.currentTimeMillis()): FailoverDecision {
        val failed = com.v2ray.ang.handler.MmkvManager.getSelectServer()
        val servers = HotfoxServerSelection.currentCandidates()
        val restart = VpnRestartGate.current()
        return consider(
            code = code,
            auto = HotfoxServerSelection.isAutoMode(),
            shadowAuto = HotfoxShadowStore.isShadowAuto(),
            failedGuid = failed,
            servers = servers,
            healthByGuid = HotfoxServerSelection.healthSnapshot(servers, nowEpochMs),
            nowEpochMs = nowEpochMs,
            networkContext = HotfoxServerSelection.health.networkContext,
            generation = restart,
            currentGeneration = restart,
        ).also { HotfoxAutoFailover.apply(it) }
    }
}
