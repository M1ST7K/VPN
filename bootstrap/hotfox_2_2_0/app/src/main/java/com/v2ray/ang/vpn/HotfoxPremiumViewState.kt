package com.v2ray.ang.vpn

/**
 * Deterministic chrome for 3.0 Premium surfaces. Used as screenshot/golden
 * fixtures in unit tests so pixel-perfect OS rendering is not required.
 */
data class HotfoxConnectionChrome(
    val headline: ConnectionUiMapper.Headline,
    val headlineResName: String,
    val isProtected: Boolean,
    val notificationTitleResName: String,
    val notificationProtected: Boolean,
    val qsAppearance: QsTileUiMapper.Appearance,
    val qsActive: Boolean,
)

object HotfoxPremiumViewState {
    fun headlineResName(headline: ConnectionUiMapper.Headline): String = when (headline) {
        ConnectionUiMapper.Headline.CONNECTED -> "hotfox_headline_connected"
        ConnectionUiMapper.Headline.SELECTING -> "hotfox_headline_selecting"
        ConnectionUiMapper.Headline.VERIFYING -> "hotfox_headline_verifying"
        ConnectionUiMapper.Headline.CONNECTING -> "hotfox_headline_connecting"
        ConnectionUiMapper.Headline.RECONNECTING -> "hotfox_headline_reconnecting"
        ConnectionUiMapper.Headline.ERROR -> "hotfox_headline_error"
        ConnectionUiMapper.Headline.DISCONNECTING -> "hotfox_headline_disconnecting"
        ConnectionUiMapper.Headline.PROXY_ONLY -> "hotfox_headline_proxy_only"
        ConnectionUiMapper.Headline.ROOT_RUNNING -> "hotfox_headline_root"
        ConnectionUiMapper.Headline.DISCONNECTED -> "hotfox_headline_disconnected"
    }

    fun connectionChrome(state: VpnSessionState): HotfoxConnectionChrome {
        val headline = ConnectionUiMapper.headline(state)
        val notification = NotificationUiMapper.from(state)
        val qs = QsTileUiMapper.from(state)
        return HotfoxConnectionChrome(
            headline = headline,
            headlineResName = headlineResName(headline),
            isProtected = ConnectionUiMapper.isProtectedHeadline(state),
            notificationTitleResName = notification.titleResName,
            notificationProtected = notification.isProtected,
            qsAppearance = qs,
            qsActive = QsTileUiMapper.isProtectedActive(state),
        )
    }

    fun fixtureLine(state: VpnSessionState): String {
        val chrome = connectionChrome(state)
        return listOf(
            state.name,
            chrome.headline.name,
            chrome.headlineResName,
            chrome.isProtected.toString(),
            chrome.notificationProtected.toString(),
            chrome.qsAppearance.name,
            chrome.qsActive.toString(),
        ).joinToString("|")
    }
}
