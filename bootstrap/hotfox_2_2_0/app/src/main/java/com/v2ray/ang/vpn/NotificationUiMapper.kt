package com.v2ray.ang.vpn

/**
 * Foreground notification copy derived only from [VpnSessionState].
 * Protected wording is used only when the canonical session is CONNECTED.
 */
object NotificationUiMapper {
    data class Copy(
        val titleResName: String,
        val textResName: String,
        val showDisconnectAction: Boolean,
        val isProtected: Boolean,
    )

    fun from(state: VpnSessionState): Copy {
        val headline = ConnectionUiMapper.headline(state)
        val protected = headline == ConnectionUiMapper.Headline.CONNECTED
        val title = when (headline) {
            ConnectionUiMapper.Headline.CONNECTED -> "hotfox_headline_connected"
            ConnectionUiMapper.Headline.SELECTING -> "hotfox_headline_selecting"
            ConnectionUiMapper.Headline.VERIFYING -> "hotfox_headline_verifying"
            ConnectionUiMapper.Headline.CONNECTING,
            ConnectionUiMapper.Headline.RECONNECTING,
            -> "hotfox_headline_connecting"
            ConnectionUiMapper.Headline.DISCONNECTING -> "hotfox_headline_disconnecting"
            ConnectionUiMapper.Headline.ERROR -> "hotfox_headline_error"
            ConnectionUiMapper.Headline.PROXY_ONLY -> "hotfox_headline_proxy_only"
            ConnectionUiMapper.Headline.ROOT_RUNNING -> "hotfox_headline_root"
            ConnectionUiMapper.Headline.DISCONNECTED -> "hotfox_headline_disconnected"
        }
        val text = when (headline) {
            ConnectionUiMapper.Headline.CONNECTED -> "hotfox_notification_protected_text"
            ConnectionUiMapper.Headline.ERROR -> "hotfox_error_generic_detail"
            else -> "hotfox_notification_progress_text"
        }
        return Copy(
            titleResName = title,
            textResName = text,
            showDisconnectAction = state.isServiceActive() || protected,
            isProtected = protected,
        )
    }
}
