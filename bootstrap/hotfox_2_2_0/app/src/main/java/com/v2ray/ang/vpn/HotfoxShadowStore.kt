package com.v2ray.ang.vpn

import com.v2ray.ang.handler.MmkvManager

object HotfoxShadowStore {
    val cache = NetworkCapabilityCache()
    val dns = DnsBootstrapCache()

    @Volatile
    var selfHeal: HotfoxSelfHeal.State = HotfoxSelfHeal.State()
        private set

    @Volatile
    var lastPathId: String = ""
        private set

    @Volatile
    var lastDecisionReason: String = ""
        private set

    fun isShadowAuto(): Boolean = runCatching {
        MmkvManager.decodeSettingsBool(HotfoxShadowPolicy.PREF_SHADOW_AUTO, true)
    }.getOrDefault(true)

    fun setShadowAuto(enabled: Boolean) {
        MmkvManager.encodeSettings(HotfoxShadowPolicy.PREF_SHADOW_AUTO, enabled)
    }

    fun rememberPath(path: ConnectionPath, reason: String) {
        lastPathId = path.id
        lastDecisionReason = reason
    }

    fun recordTransport(transport: HotfoxTransport, success: Boolean, nowEpochMs: Long, networkContext: Long) {
        cache.record(transport.storageValue, success, nowEpochMs, networkContext)
    }

    fun invalidateNetwork(networkContext: Long) {
        cache.invalidate(networkContext)
        dns.invalidateNetwork(networkContext)
    }

    fun notePathDeath(nowEpochMs: Long) {
        selfHeal = HotfoxSelfHeal.recordDeath(selfHeal)
    }

    fun notePathSuccess() {
        selfHeal = HotfoxSelfHeal.recordSuccess(selfHeal)
    }

    fun noteHeal(nowEpochMs: Long) {
        selfHeal = HotfoxSelfHeal.recordHeal(selfHeal, nowEpochMs)
    }

    fun resetForTests() {
        cache.clear()
        dns.clear()
        selfHeal = HotfoxSelfHeal.State()
        lastPathId = ""
        lastDecisionReason = ""
    }
}
