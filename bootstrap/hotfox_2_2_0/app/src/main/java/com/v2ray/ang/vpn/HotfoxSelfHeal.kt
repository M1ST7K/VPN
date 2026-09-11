package com.v2ray.ang.vpn

/**
 * Self-healing after verified path death. One noisy probe is ignored.
 * Threshold + hysteresis + cooldown. Cancelled by explicit disconnect
 * via [VpnRestartGate.invalidate].
 */
object HotfoxSelfHeal {
    const val FAILURE_THRESHOLD = 2
    const val COOLDOWN_MS = 30_000L

    data class State(
        val consecutivePathDeaths: Int = 0,
        val lastHealAtEpochMs: Long = 0L,
    )

    fun shouldHeal(
        state: State,
        pathVerifiedDeath: Boolean,
        nowEpochMs: Long,
        connected: Boolean,
        generationCurrent: Boolean,
    ): Boolean {
        if (!connected || !generationCurrent) return false
        if (!pathVerifiedDeath) return false
        if (state.consecutivePathDeaths < FAILURE_THRESHOLD) return false
        if (state.lastHealAtEpochMs > 0L && nowEpochMs - state.lastHealAtEpochMs < COOLDOWN_MS) {
            return false
        }
        return true
    }

    fun recordDeath(state: State): State = state.copy(consecutivePathDeaths = state.consecutivePathDeaths + 1)

    fun recordSuccess(state: State): State = state.copy(consecutivePathDeaths = 0)

    fun recordHeal(state: State, nowEpochMs: Long): State =
        state.copy(consecutivePathDeaths = 0, lastHealAtEpochMs = nowEpochMs)
}
