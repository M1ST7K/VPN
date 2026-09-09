package com.v2ray.ang.vpn

/**
 * Deterministic AUTO score. Lower is better. Not AI.
 *
 * score = EWMA latency
 *        + 0.5 * jitter
 *        + 80 * consecutive failures
 *        + stale penalty
 *        + degraded penalty
 */
object AutoSelectionPolicy {
    const val SWITCH_ABS_MS = 50.0
    const val SWITCH_REL = 0.35
    const val FAILURE_PENALTY_MS = 80.0
    const val DEGRADED_PENALTY_MS = 40.0
    const val STALE_PENALTY_CAP_MS = 200.0
    const val FAILOVER_ATTEMPT_CAP = 3

    data class Scored(
        val guid: String,
        val score: Double,
        val health: ServerHealth,
        val reason: String,
    )

    fun score(health: ServerHealth, nowEpochMs: Long, networkContext: Long = 0L): Double? {
        if (health.networkContext != networkContext) return null
        return when (health.availability) {
            ServerAvailability.DEAD -> null
            ServerAvailability.UNKNOWN -> null
            ServerAvailability.HEALTHY, ServerAvailability.DEGRADED -> {
                val latency = health.ewmaLatencyMs ?: health.latestLatencyMs?.toDouble() ?: return null
                val jitter = health.jitterMs ?: 0.0
                val stale = stalePenalty(health, nowEpochMs)
                val degraded = if (health.availability == ServerAvailability.DEGRADED) DEGRADED_PENALTY_MS else 0.0
                latency + 0.5 * jitter + FAILURE_PENALTY_MS * health.consecutiveFailures + stale + degraded
            }
        }
    }

    fun stalePenalty(health: ServerHealth, nowEpochMs: Long): Double {
        if (health.lastProbeAtEpochMs <= 0L || nowEpochMs <= 0L) return 0.0
        val age = nowEpochMs - health.lastProbeAtEpochMs
        if (age <= ServerHealthMath.STALE_AFTER_MS) return 0.0
        val extra = (age - ServerHealthMath.STALE_AFTER_MS) / 1000.0 * 10.0
        return extra.coerceAtMost(STALE_PENALTY_CAP_MS)
    }

    fun isStale(health: ServerHealth, nowEpochMs: Long): Boolean {
        if (health.lastProbeAtEpochMs <= 0L || nowEpochMs <= 0L) return false
        return nowEpochMs - health.lastProbeAtEpochMs > ServerHealthMath.STALE_AFTER_MS
    }

    fun significantlyBetter(challengerScore: Double, currentScore: Double): Boolean {
        val abs = currentScore - challengerScore
        if (abs < SWITCH_ABS_MS) return false
        if (currentScore <= 0.0) return abs >= SWITCH_ABS_MS
        return abs / currentScore >= SWITCH_REL
    }

    fun pick(
        servers: List<HotfoxServerSelection.Candidate>,
        healthByGuid: Map<String, ServerHealth> = emptyMap(),
        auto: Boolean,
        selectedGuid: String?,
        nowEpochMs: Long = 0L,
        lastGoodGuid: String? = null,
        networkContext: Long = 0L,
    ): HotfoxServerSelection.ResolveResult {
        val eligible = AutoCandidateFilter.eligible(servers)
        if (eligible.isEmpty()) {
            return HotfoxServerSelection.ResolveResult.Failure("HF-VPN-010 Нет серверов")
        }
        if (!auto) {
            return manualPick(eligible, selectedGuid)
        }
        val scored = scoreHealthy(eligible, healthByGuid, nowEpochMs, networkContext)
        val best = scored.minByOrNull { it.score }
        if (best != null) {
            return HotfoxServerSelection.ResolveResult.Success(best.guid, resolvedFromAuto = true)
        }
        val untested = eligible.filter { !isDead(it, healthByGuid) && !hasFreshScore(it, healthByGuid, nowEpochMs, networkContext) }
        if (untested.isNotEmpty()) {
            val lastGood = lastGoodGuid?.let { id -> untested.firstOrNull { it.guid == id } }
            return HotfoxServerSelection.ResolveResult.Success(
                (lastGood ?: untested.first()).guid,
                resolvedFromAuto = true,
            )
        }
        return HotfoxServerSelection.ResolveResult.Failure("HF-VPN-011 Нет доступных серверов")
    }

    fun handover(
        servers: List<HotfoxServerSelection.Candidate>,
        healthByGuid: Map<String, ServerHealth> = emptyMap(),
        auto: Boolean,
        selectedGuid: String?,
        nowEpochMs: Long = 0L,
        lastGoodGuid: String? = null,
        networkChanged: Boolean = false,
        networkContext: Long = 0L,
    ): HotfoxServerSelection.ResolveResult {
        val eligible = AutoCandidateFilter.eligible(servers)
        if (!auto) {
            return pick(
                eligible,
                healthByGuid,
                auto = false,
                selectedGuid = selectedGuid,
                nowEpochMs = nowEpochMs,
                lastGoodGuid = lastGoodGuid,
                networkContext = networkContext,
            )
        }
        if (networkChanged) {
            val sticky = eligible.firstOrNull { it.guid == selectedGuid }
            if (sticky != null && !isDead(sticky, healthByGuid)) {
                return HotfoxServerSelection.ResolveResult.Success(sticky.guid, resolvedFromAuto = true)
            }
            return pick(
                eligible,
                healthByGuid,
                auto = true,
                selectedGuid = selectedGuid,
                nowEpochMs = nowEpochMs,
                lastGoodGuid = lastGoodGuid ?: selectedGuid,
                networkContext = networkContext,
            )
        }
        val current = eligible.firstOrNull { it.guid == selectedGuid } ?: return pick(
            eligible,
            healthByGuid,
            auto = true,
            selectedGuid = selectedGuid,
            nowEpochMs = nowEpochMs,
            lastGoodGuid = lastGoodGuid,
            networkContext = networkContext,
        )
        val currentHealth = healthByGuid[current.guid]
            ?: ServerHealthMath.fromCachedDelay(current.guid, current.delay, nowEpochMs, networkContext)
        val currentUsable = currentHealth.availability == ServerAvailability.HEALTHY ||
            currentHealth.availability == ServerAvailability.DEGRADED
        if (currentUsable) {
            val currentScore = score(currentHealth, nowEpochMs, networkContext)
            val scored = scoreHealthy(eligible, healthByGuid, nowEpochMs, networkContext)
            val best = scored.minByOrNull { it.score }
            if (currentScore != null && best != null && best.guid != current.guid &&
                significantlyBetter(best.score, currentScore)
            ) {
                return HotfoxServerSelection.ResolveResult.Success(best.guid, resolvedFromAuto = true)
            }
            return HotfoxServerSelection.ResolveResult.Success(current.guid, resolvedFromAuto = true)
        }
        return pick(
            eligible,
            healthByGuid,
            auto = true,
            selectedGuid = selectedGuid,
            nowEpochMs = nowEpochMs,
            lastGoodGuid = lastGoodGuid,
            networkContext = networkContext,
        )
    }

    fun failover(
        servers: List<HotfoxServerSelection.Candidate>,
        healthByGuid: Map<String, ServerHealth>,
        auto: Boolean,
        selectedGuid: String?,
        attempt: Int,
        nowEpochMs: Long = 0L,
        lastGoodGuid: String? = null,
        networkContext: Long = 0L,
    ): FailoverDecision {
        if (!auto) {
            return FailoverDecision(FailoverAction.STOP, selectedGuid, "manual_sticky", attempt)
        }
        if (attempt >= FAILOVER_ATTEMPT_CAP) {
            return FailoverDecision(FailoverAction.STOP, selectedGuid, "attempt_cap", attempt)
        }
        val next = pick(
            servers,
            healthByGuid,
            auto = true,
            selectedGuid = selectedGuid,
            nowEpochMs = nowEpochMs,
            lastGoodGuid = lastGoodGuid,
            networkContext = networkContext,
        )
        return when (next) {
            is HotfoxServerSelection.ResolveResult.Success -> {
                if (next.guid == selectedGuid) {
                    FailoverDecision(FailoverAction.RETRY_SAME, next.guid, "retry_same", attempt + 1)
                } else {
                    FailoverDecision(FailoverAction.SWITCH, next.guid, "failover_previous_unreachable", attempt + 1)
                }
            }
            is HotfoxServerSelection.ResolveResult.Failure ->
                FailoverDecision(FailoverAction.STOP, selectedGuid, "all_unhealthy", attempt)
        }
    }

    fun diagnosticReason(
        result: HotfoxServerSelection.ResolveResult,
        healthByGuid: Map<String, ServerHealth>,
        nowEpochMs: Long,
    ): String {
        return when (result) {
            is HotfoxServerSelection.ResolveResult.Failure -> result.message
            is HotfoxServerSelection.ResolveResult.Success -> {
                val health = healthByGuid[result.guid]
                val latency = health?.latestLatencyMs
                val failures = health?.consecutiveFailures ?: 0
                val stale = health?.let { isStale(it, nowEpochMs) } == true
                when {
                    result.resolvedFromAuto && stale ->
                        "selected: ${latency ?: "—"} ms stale, $failures recent failures"
                    latency != null ->
                        "selected: $latency ms fresh, $failures recent failures"
                    else -> "selected: untested fallback"
                }
            }
        }
    }

    private fun scoreHealthy(
        servers: List<HotfoxServerSelection.Candidate>,
        healthByGuid: Map<String, ServerHealth>,
        nowEpochMs: Long,
        networkContext: Long,
    ): List<Scored> {
        return servers.mapNotNull { candidate ->
            val health = healthByGuid[candidate.guid]
                ?: ServerHealthMath.fromCachedDelay(candidate.guid, candidate.delay, nowEpochMs, networkContext)
            val value = score(health, nowEpochMs, networkContext) ?: return@mapNotNull null
            Scored(candidate.guid, value, health, "score")
        }
    }

    private fun hasFreshScore(
        candidate: HotfoxServerSelection.Candidate,
        healthByGuid: Map<String, ServerHealth>,
        nowEpochMs: Long,
        networkContext: Long,
    ): Boolean {
        val health = healthByGuid[candidate.guid]
            ?: ServerHealthMath.fromCachedDelay(candidate.guid, candidate.delay, nowEpochMs, networkContext)
        return score(health, nowEpochMs, networkContext) != null
    }

    private fun manualPick(
        servers: List<HotfoxServerSelection.Candidate>,
        selectedGuid: String?,
    ): HotfoxServerSelection.ResolveResult {
        val match = servers.firstOrNull { it.guid == selectedGuid }
        if (match == null) {
            return HotfoxServerSelection.ResolveResult.Success(servers.first().guid, resolvedFromAuto = false)
        }
        if (match.delay < 0L) {
            return HotfoxServerSelection.ResolveResult.Failure("HF-VPN-011 Сервер недоступен")
        }
        return HotfoxServerSelection.ResolveResult.Success(match.guid, resolvedFromAuto = false)
    }

    private fun isDead(candidate: HotfoxServerSelection.Candidate, healthByGuid: Map<String, ServerHealth>): Boolean {
        val health = healthByGuid[candidate.guid]
        return health?.availability == ServerAvailability.DEAD || candidate.delay < 0L
    }
}

enum class FailoverAction { RETRY_SAME, SWITCH, STOP }

data class FailoverDecision(
    val action: FailoverAction,
    val guid: String?,
    val reason: String,
    val attempt: Int,
)
