package com.v2ray.ang.vpn

/**
 * Single source of truth for HotFox VPN session progress.
 * UI must never show Защищено unless the state is [CONNECTED].
 *
 * [PROXY_ONLY] and [ROOT_RUNNING] mean a non-VpnService core is up. They are
 * never treated as a verified Android VPN datapath.
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
    PROXY_ONLY,
    ROOT_RUNNING,
    RECONNECTING,
    DISCONNECTING,
    ERROR,
    ;

    fun isProtected(): Boolean = this == CONNECTED

    fun isNonVpnRunning(): Boolean = this == PROXY_ONLY || this == ROOT_RUNNING

    fun isServiceActive(): Boolean = when (this) {
        CONNECTED, PROXY_ONLY, ROOT_RUNNING, RECONNECTING -> true
        else -> isBusy()
    }

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
        DISCONNECTED, PERMISSION_REQUIRED, PROXY_ONLY, ROOT_RUNNING -> ConnectionUiPhase.DISCONNECTED
        else -> ConnectionUiPhase.CONNECTING
    }
}

enum class ConnectionUiPhase {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR,
}

enum class DuplicateStartDisposition {
    REPUBLISH,
    IGNORE,
    START,
}
