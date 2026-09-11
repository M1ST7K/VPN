package com.v2ray.ang.vpn

/**
 * Read-only UI/control-plane observation of the process-scoped VPN engine.
 *
 * Activity destruction must not imply VPN loss. Recreated UI re-reads [snapshot]
 * instead of minting a second session. Mutations stay on [VpnSessionCoordinator]
 * and [com.v2ray.ang.core.CoreServiceManager] / [VpnRestartGate] only.
 */
data class HotfoxEngineSnapshot(
    val state: VpnSessionState,
    val stage: VpnConnectionStage,
    val lastError: String?,
    val path: VpnPathVerification?,
    val sessionStartedAtElapsed: Long?,
    val attempt: Long,
    val teardownActive: Boolean,
) {
    fun isProtected(): Boolean = state.isProtected()
}

object HotfoxEngineFacade {
    fun snapshot(): HotfoxEngineSnapshot = HotfoxEngineSnapshot(
        state = VpnSessionCoordinator.currentState(),
        stage = VpnSessionCoordinator.lastStage(),
        lastError = VpnSessionCoordinator.lastError(),
        path = VpnSessionCoordinator.lastPath(),
        sessionStartedAtElapsed = VpnSessionCoordinator.sessionStartedAtElapsed(),
        attempt = VpnSessionCoordinator.currentAttempt(),
        teardownActive = VpnSessionCoordinator.isTeardownActive(),
    )

    fun currentState(): VpnSessionState = snapshot().state

    fun lastStage(): VpnConnectionStage = snapshot().stage

    fun lastError(): String? = snapshot().lastError

    fun lastPath(): VpnPathVerification? = snapshot().path

    fun sessionStartedAtElapsed(): Long? = snapshot().sessionStartedAtElapsed

    suspend fun awaitIdle(timeoutMillis: Long = 8_000L): Boolean =
        VpnSessionCoordinator.awaitIdle(timeoutMillis)
}
