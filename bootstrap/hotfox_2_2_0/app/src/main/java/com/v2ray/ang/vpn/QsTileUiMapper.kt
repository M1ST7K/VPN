package com.v2ray.ang.vpn

/**
 * Quick Settings Tile appearance derived only from [VpnSessionState].
 * [Appearance.ACTIVE] is reserved for a canonical protected session.
 */
object QsTileUiMapper {
    enum class Appearance {
        INACTIVE,
        CONNECTING,
        ACTIVE,
        ERROR,
    }

    fun from(state: VpnSessionState): Appearance = when (ConnectionUiMapper.headline(state)) {
        ConnectionUiMapper.Headline.CONNECTED -> Appearance.ACTIVE
        ConnectionUiMapper.Headline.ERROR -> Appearance.ERROR
        ConnectionUiMapper.Headline.DISCONNECTED -> Appearance.INACTIVE
        ConnectionUiMapper.Headline.PROXY_ONLY,
        ConnectionUiMapper.Headline.ROOT_RUNNING,
        -> Appearance.INACTIVE
        else -> Appearance.CONNECTING
    }

    fun isProtectedActive(state: VpnSessionState): Boolean =
        from(state) == Appearance.ACTIVE

    fun labelResName(appearance: Appearance): String = when (appearance) {
        Appearance.ACTIVE -> "hotfox_headline_connected"
        Appearance.CONNECTING -> "hotfox_headline_connecting"
        Appearance.ERROR -> "hotfox_headline_error"
        Appearance.INACTIVE -> "hotfox_headline_disconnected"
    }

    fun shouldStartOnClick(state: VpnSessionState): Boolean =
        from(state) == Appearance.INACTIVE

    fun shouldStopOnClick(state: VpnSessionState): Boolean =
        from(state) == Appearance.ACTIVE
}
