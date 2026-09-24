package com.v2ray.ang.vpn

/**
 * Bounded Shadow AUTO fallback.
 *
 * preferred path → alternate transport → alternate server → Shadow route
 * (only when a dedicated entry profile exists). Never infinite. Manual
 * selection stays sticky. Fallback never weakens TLS/REALITY.
 */
object HotfoxShadowPolicy {
    const val ATTEMPT_CAP = 4
    const val PREF_SHADOW_AUTO = "pref_hotfox_shadow_auto"
    const val SELECTING_COPY = "Подбираем защищённый маршрут…"
    const val MODE_LABEL = "Shadow: Авто"

    data class Decision(
        val action: FailoverAction,
        val path: ConnectionPath?,
        val reason: String,
        val attempt: Int,
    ) {
        fun toFailover(): FailoverDecision = FailoverDecision(
            action = action,
            guid = path?.serverGuid,
            reason = reason,
            attempt = attempt,
        )
    }

    fun pathsFrom(
        servers: List<HotfoxServerSelection.Candidate>,
    ): List<ConnectionPath> {
        return servers.mapNotNull { candidate ->
            HotfoxPathFactory.fromCandidate(
                guid = candidate.guid,
                network = candidate.network,
                security = candidate.security,
                remarks = candidate.remarks,
                ipv6 = candidate.ipv6,
            )
        }
    }

    fun supported(paths: List<ConnectionPath>): List<ConnectionPath> {
        return paths.filter { path ->
            HotfoxTransport.parse(path.transport.storageValue) != null
        }
    }

    fun fallback(
        paths: List<ConnectionPath>,
        failedPath: ConnectionPath?,
        cache: NetworkCapabilityCache,
        serverScores: Map<String, Double>,
        auto: Boolean,
        shadowAuto: Boolean,
        attempt: Int,
        nowEpochMs: Long,
        networkContext: Long,
        generation: Long,
        currentGeneration: Long,
    ): Decision {
        if (generation != currentGeneration) {
            return Decision(FailoverAction.STOP, failedPath, "stale_generation", attempt)
        }
        if (!auto) {
            return Decision(FailoverAction.STOP, failedPath, "manual_sticky", attempt)
        }
        if (attempt >= ATTEMPT_CAP) {
            return Decision(FailoverAction.STOP, failedPath, "attempt_cap", attempt)
        }
        val usable = supported(paths)
        if (usable.isEmpty()) {
            return Decision(FailoverAction.STOP, failedPath, "no_supported_transport", attempt)
        }
        if (failedPath != null) {
            cache.record(failedPath.transport.storageValue, success = false, nowEpochMs, networkContext)
        }
        val ranked = HotfoxPathScore.rank(usable, serverScores, cache, nowEpochMs, networkContext)
        val sameSiteAlt = ranked.firstOrNull { scored ->
            failedPath != null &&
                scored.path.transport != failedPath.transport &&
                (scored.path.serverGuid == failedPath.serverGuid ||
                    scored.path.remarks == failedPath.remarks)
        }
        if (sameSiteAlt != null) {
            return Decision(FailoverAction.SWITCH, sameSiteAlt.path, "alternate_transport", attempt + 1)
        }
        val otherServer = ranked.firstOrNull { it.path.serverGuid != failedPath?.serverGuid }
        if (otherServer != null) {
            return Decision(FailoverAction.SWITCH, otherServer.path, "alternate_server", attempt + 1)
        }
        if (shadowAuto) {
            val composed = selectShadowRoute(usable)
            if (composed != null) {
                return Decision(FailoverAction.SWITCH, composed, "shadow_route", attempt + 1)
            }
            val stealth = ranked.firstOrNull { it.path.role == PathRole.STEALTH }
            if (stealth != null) {
                return Decision(FailoverAction.SWITCH, stealth.path, "shadow_route", attempt + 1)
            }
        }
        return Decision(FailoverAction.STOP, failedPath, "budget_exhausted", attempt)
    }

    fun selectShadowRoute(paths: List<ConnectionPath>): ConnectionPath? {
        val entries = paths.filter { it.role == PathRole.SHADOW_ENTRY || it.role == PathRole.STEALTH }
        val exits = paths.filter { it.role == PathRole.SHADOW_EXIT || it.role == PathRole.EXIT }
        for (entry in entries) {
            for (exit in exits) {
                HotfoxPathFactory.shadowRoute(entry, exit)?.let { return it }
            }
        }
        return null
    }

    fun pickPreferred(
        paths: List<ConnectionPath>,
        cache: NetworkCapabilityCache,
        serverScores: Map<String, Double>,
        nowEpochMs: Long,
        networkContext: Long,
    ): ConnectionPath? {
        return HotfoxPathScore.rank(supported(paths), serverScores, cache, nowEpochMs, networkContext)
            .firstOrNull()
            ?.path
    }
}
