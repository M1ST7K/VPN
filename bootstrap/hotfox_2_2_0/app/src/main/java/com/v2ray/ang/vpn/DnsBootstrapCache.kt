package com.v2ray.ang.vpn

/**
 * Cached validated addresses for backend/VPN endpoints under difficult DNS.
 * Traffic still follows the 2.5 THROUGH_VPN policy; this is not a leak path.
 */
class DnsBootstrapCache(
    private val ttlMs: Long = TTL_MS,
    private val maxEntries: Int = MAX_ENTRIES,
) {
    data class CachedAddress(
        val host: String,
        val ipv4: String?,
        val ipv6: String?,
        val storedAtEpochMs: Long,
        val networkContext: Long,
    ) {
        fun expired(nowEpochMs: Long, ttl: Long): Boolean = nowEpochMs - storedAtEpochMs > ttl
    }

    private val byHost = LinkedHashMap<String, CachedAddress>()

    fun store(host: String, ipv4: String?, ipv6: String?, nowEpochMs: Long, networkContext: Long) {
        if (host.isBlank()) return
        if (ipv4.isNullOrBlank() && ipv6.isNullOrBlank()) return
        byHost[host.lowercase()] = CachedAddress(
            host = host.lowercase(),
            ipv4 = ipv4?.takeIf { it.isNotBlank() },
            ipv6 = ipv6?.takeIf { it.isNotBlank() },
            storedAtEpochMs = nowEpochMs,
            networkContext = networkContext,
        )
        while (byHost.size > maxEntries) {
            val first = byHost.keys.first()
            byHost.remove(first)
        }
    }

    fun lookup(host: String, nowEpochMs: Long, networkContext: Long): CachedAddress? {
        val cached = byHost[host.lowercase()] ?: return null
        if (cached.networkContext != networkContext) return null
        if (cached.expired(nowEpochMs, ttlMs)) {
            byHost.remove(host.lowercase())
            return null
        }
        return cached
    }

    fun invalidateNetwork(networkContext: Long) {
        val stale = byHost.filterValues { it.networkContext != networkContext }.keys
        stale.forEach { byHost.remove(it) }
    }

    fun clear() {
        byHost.clear()
    }

    companion object {
        const val TTL_MS = 10 * 60 * 1000L
        const val MAX_ENTRIES = 16
    }
}
