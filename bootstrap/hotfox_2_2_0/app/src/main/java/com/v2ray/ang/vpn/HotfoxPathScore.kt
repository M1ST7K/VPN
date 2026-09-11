package com.v2ray.ang.vpn

/**
 * Deterministic PathScore. Lower is better. Isolated from AUTO ServerScore.
 *
 * pathScore = serverScore
 *           + transport failure penalty
 *           + family penalty
 *           + hop penalty
 */
object HotfoxPathScore {
    const val TRANSPORT_FAILURE_MS = 90.0
    const val UNKNOWN_TRANSPORT_MS = 25.0
    const val IPV6_ONLY_PENALTY_MS = 15.0
    const val MULTIHOP_PENALTY_MS = 80.0
    const val STEALTH_PENALTY_MS = 20.0

    data class Scored(
        val path: ConnectionPath,
        val score: Double,
        val reason: String,
    )

    fun score(
        path: ConnectionPath,
        serverScore: Double?,
        cache: NetworkCapabilityCache,
        nowEpochMs: Long,
        networkContext: Long,
    ): Double? {
        if (serverScore == null) return null
        val transportKey = path.transport.storageValue
        val failures = cache.consecutiveFailures(transportKey, nowEpochMs, networkContext)
        val transportPenalty = when {
            failures > 0 -> TRANSPORT_FAILURE_MS * failures
            cache.recentlySucceeded(transportKey, nowEpochMs, networkContext) -> 0.0
            else -> UNKNOWN_TRANSPORT_MS
        }
        val familyPenalty = if (path.family == AddressFamily.IPV6) IPV6_ONLY_PENALTY_MS else 0.0
        val hopPenalty = if (path.isMultihop) MULTIHOP_PENALTY_MS else 0.0
        val stealthPenalty = if (path.role == PathRole.STEALTH || path.role == PathRole.SHADOW_ENTRY) {
            STEALTH_PENALTY_MS
        } else {
            0.0
        }
        return serverScore + transportPenalty + familyPenalty + hopPenalty + stealthPenalty
    }

    fun rank(
        paths: List<ConnectionPath>,
        serverScores: Map<String, Double>,
        cache: NetworkCapabilityCache,
        nowEpochMs: Long,
        networkContext: Long,
    ): List<Scored> {
        return paths.mapNotNull { path ->
            val value = score(
                path,
                serverScores[path.serverGuid],
                cache,
                nowEpochMs,
                networkContext,
            ) ?: return@mapNotNull null
            Scored(path, value, "path_score")
        }.sortedWith(compareBy({ it.score }, { it.path.id }))
    }
}
