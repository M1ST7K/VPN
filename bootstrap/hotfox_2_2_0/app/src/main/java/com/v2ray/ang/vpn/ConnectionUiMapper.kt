package com.v2ray.ang.vpn

/**
 * Maps canonical VPN state to editorial UI copy. Never maps incomplete
 * startup states (WAITING_SOCKS / STARTING_HEV) to CONNECTED.
 */
object ConnectionUiMapper {
    enum class Headline {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        RECONNECTING,
        ERROR,
        DISCONNECTING,
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
        VpnSessionState.DISCONNECTED, VpnSessionState.PERMISSION_REQUIRED -> Headline.DISCONNECTED
        else -> Headline.CONNECTING
    }

    fun primaryAction(state: VpnSessionState, hasServers: Boolean): PrimaryAction = when (state) {
        VpnSessionState.CONNECTED, VpnSessionState.RECONNECTING -> PrimaryAction.DISCONNECT
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
