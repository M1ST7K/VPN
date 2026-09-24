package com.v2ray.ang.vpn

/**
 * Maps canonical VPN state to editorial UI copy. Never maps incomplete
 * startup states (WAITING_SOCKS / STARTING_HEV / VERIFYING_PATH) to CONNECTED.
 * Boolean service-running flags are not an input; only [VpnSessionState] is.
 */
object ConnectionUiMapper {
    enum class Headline {
        DISCONNECTED,
        SELECTING,
        CONNECTING,
        VERIFYING,
        CONNECTED,
        RECONNECTING,
        ERROR,
        DISCONNECTING,
        PROXY_ONLY,
        ROOT_RUNNING,
    }

    enum class PrimaryAction {
        CONNECT,
        CANCEL_OR_WAIT,
        DISCONNECT,
        RETRY,
    }

    fun headline(state: VpnSessionState): Headline = when (state) {
        VpnSessionState.CONNECTED -> Headline.CONNECTED
        VpnSessionState.RECONNECTING -> Headline.RECONNECTING
        VpnSessionState.ERROR -> Headline.ERROR
        VpnSessionState.DISCONNECTING -> Headline.DISCONNECTING
        VpnSessionState.PROXY_ONLY -> Headline.PROXY_ONLY
        VpnSessionState.ROOT_RUNNING -> Headline.ROOT_RUNNING
        VpnSessionState.DISCONNECTED, VpnSessionState.PERMISSION_REQUIRED -> Headline.DISCONNECTED
        VpnSessionState.PREPARING -> Headline.SELECTING
        VpnSessionState.VERIFYING_PATH -> Headline.VERIFYING
        else -> Headline.CONNECTING
    }

    fun headline(
        state: VpnSessionState,
        stage: VpnConnectionStage,
        autoSelecting: Boolean,
    ): Headline {
        val mapped = headline(state)
        if (mapped == Headline.CONNECTED || mapped == Headline.ERROR || mapped == Headline.DISCONNECTING) {
            return mapped
        }
        if (autoSelecting && (state == VpnSessionState.PREPARING || state == VpnSessionState.DISCONNECTED)) {
            return Headline.SELECTING
        }
        if (stage == VpnConnectionStage.RESOLVE_SERVER && state.isBusy()) {
            return Headline.SELECTING
        }
        if (stage == VpnConnectionStage.VERIFIED && state != VpnSessionState.CONNECTED) {
            return Headline.VERIFYING
        }
        return mapped
    }

    fun resolveHeadline(state: VpnSessionState, isLoading: Boolean): Headline {
        return when {
            isLoading && (
                state.isProtected() ||
                    state.isNonVpnRunning() ||
                    state == VpnSessionState.RECONNECTING
                ) -> Headline.DISCONNECTING
            else -> headline(state).let { mapped ->
                if (isLoading && mapped == Headline.DISCONNECTED) Headline.CONNECTING else mapped
            }
        }
    }

    fun resolveHeadline(
        state: VpnSessionState,
        stage: VpnConnectionStage,
        isLoading: Boolean,
        autoSelecting: Boolean,
    ): Headline {
        val loadingAdjusted = resolveHeadline(state, isLoading)
        if (loadingAdjusted == Headline.DISCONNECTING || loadingAdjusted == Headline.CONNECTED) {
            return loadingAdjusted
        }
        return headline(state, stage, autoSelecting).let { mapped ->
            if (isLoading && mapped == Headline.DISCONNECTED) Headline.CONNECTING else mapped
        }
    }

    fun primaryAction(state: VpnSessionState, hasServers: Boolean): PrimaryAction = when (state) {
        VpnSessionState.CONNECTED,
        VpnSessionState.RECONNECTING,
        VpnSessionState.PROXY_ONLY,
        VpnSessionState.ROOT_RUNNING,
        -> PrimaryAction.DISCONNECT
        VpnSessionState.ERROR -> PrimaryAction.RETRY
        VpnSessionState.DISCONNECTING -> PrimaryAction.CANCEL_OR_WAIT
        else -> if (state.isBusy()) PrimaryAction.CANCEL_OR_WAIT else PrimaryAction.CONNECT
    }.let { action ->
        if (!hasServers && action == PrimaryAction.CONNECT) PrimaryAction.CONNECT else action
    }

    fun isProtectedHeadline(state: VpnSessionState): Boolean =
        headline(state) == Headline.CONNECTED

    fun timerShouldRun(state: VpnSessionState): Boolean =
        state == VpnSessionState.CONNECTED || state == VpnSessionState.RECONNECTING
}
