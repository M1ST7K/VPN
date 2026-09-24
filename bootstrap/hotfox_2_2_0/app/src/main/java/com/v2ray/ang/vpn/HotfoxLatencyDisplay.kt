package com.v2ray.ang.vpn

/**
 * Truthful latency labels. Unmeasured is em-dash, never "0 ms".
 */
object HotfoxLatencyDisplay {
    fun format(
        health: ServerHealth? = null,
        delayMs: Long? = null,
        probing: Boolean = false,
    ): String {
        if (health?.probeInFlight == true || probing) return "…"
        val availability = health?.availability
        val delay = health?.latestLatencyMs ?: delayMs
        return when {
            availability == ServerAvailability.DEAD || (availability == null && delay != null && delay < 0L) ->
                "Недоступен"
            availability == ServerAvailability.UNKNOWN || delay == null || delay == 0L ->
                "—"
            else -> "$delay ms"
        }
    }
}
