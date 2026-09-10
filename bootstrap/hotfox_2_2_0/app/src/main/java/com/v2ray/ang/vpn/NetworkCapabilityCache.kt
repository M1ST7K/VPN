package com.v2ray.ang.vpn

/**
 * Bounded technical observations about the current network environment.
 * Not a user-identity fingerprint: no SSID/BSSID, no browsing history.
 */
class NetworkCapabilityCache(
    private val ttlMs: Long = TTL_MS,
    private val maxEntries: Int = MAX_ENTRIES,
) {
    data class Observation(
        val key: String,
        val success: Boolean,
        val observedAtEpochMs: Long,
        val networkContext: Long,
        val latencyMs: Long? = null,
    )

    private val items = ArrayList<Observation>()

    fun record(
        key: String,
        success: Boolean,
        nowEpochMs: Long,
        networkContext: Long,
        latencyMs: Long? = null,
    ) {
        items.removeAll { it.key == key && it.networkContext == networkContext }
        items.add(Observation(key, success, nowEpochMs, networkContext, latencyMs))
        trim(nowEpochMs, networkContext)
    }

    fun recentlySucceeded(key: String, nowEpochMs: Long, networkContext: Long): Boolean {
        return live(nowEpochMs, networkContext).any { it.key == key && it.success }
    }

    fun consecutiveFailures(key: String, nowEpochMs: Long, networkContext: Long): Int {
        val relevant = items.filter { it.key == key && it.networkContext == networkContext }
            .sortedByDescending { it.observedAtEpochMs }
        var count = 0
        for (item in relevant) {
            if (nowEpochMs - item.observedAtEpochMs > ttlMs) break
            if (item.success) break
            count++
        }
        return count
    }

    fun invalidate(networkContext: Long) {
        items.removeAll { it.networkContext != networkContext }
    }

    fun clear() {
        items.clear()
    }

    fun live(nowEpochMs: Long, networkContext: Long): List<Observation> {
        return items.filter {
            it.networkContext == networkContext && nowEpochMs - it.observedAtEpochMs <= ttlMs
        }
    }

    private fun trim(nowEpochMs: Long, networkContext: Long) {
        items.removeAll {
            it.networkContext != networkContext || nowEpochMs - it.observedAtEpochMs > ttlMs
        }
        while (items.size > maxEntries) {
            items.removeAt(0)
        }
    }

    companion object {
        const val TTL_MS = 15 * 60 * 1000L
        const val MAX_ENTRIES = 32
    }
}
