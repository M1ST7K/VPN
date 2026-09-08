package com.v2ray.ang.vpn

/**
 * Single source of truth for HotFox VPN session progress.
 * UI must never show Защищено unless the state is [CONNECTED].
 */
enum class VpnSessionState {
    DISCONNECTED,
    PERMISSION_REQUIRED,
    PREPARING,
    STARTING_CORE,
    WAITING_SOCKS,
    ESTABLISHING_TUN,
    STARTING_HEV,
    VERIFYING_PATH,
    CONNECTED,
    RECONNECTING,
    DISCONNECTING,
    ERROR,
    ;

    fun isProtected(): Boolean = this == CONNECTED

    fun isBusy(): Boolean = when (this) {
        PREPARING,
        STARTING_CORE,
        WAITING_SOCKS,
        ESTABLISHING_TUN,
        STARTING_HEV,
        VERIFYING_PATH,
        RECONNECTING,
        DISCONNECTING,
        -> true
        else -> false
    }

    fun uiPhase(): ConnectionUiPhase = when (this) {
        CONNECTED -> ConnectionUiPhase.CONNECTED
        ERROR -> ConnectionUiPhase.ERROR
        DISCONNECTED, PERMISSION_REQUIRED -> ConnectionUiPhase.DISCONNECTED
        else -> ConnectionUiPhase.CONNECTING
    }
}

enum class ConnectionUiPhase {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR,
}
