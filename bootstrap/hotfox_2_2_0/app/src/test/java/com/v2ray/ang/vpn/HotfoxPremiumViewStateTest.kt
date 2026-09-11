package com.v2ray.ang.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HotfoxPremiumViewStateTest {
    @Test
    fun protectedChromeOnlyWhenConnected() {
        VpnSessionState.entries.forEach { state ->
            val chrome = HotfoxPremiumViewState.connectionChrome(state)
            val protected = state == VpnSessionState.CONNECTED
            assertEquals(state.name, protected, chrome.isProtected)
            assertEquals(state.name, protected, chrome.notificationProtected)
            assertEquals(state.name, protected, chrome.qsActive)
            if (protected) {
                assertEquals("hotfox_headline_connected", chrome.headlineResName)
                assertEquals(QsTileUiMapper.Appearance.ACTIVE, chrome.qsAppearance)
            } else {
                assertTrue(state.name, chrome.headline != ConnectionUiMapper.Headline.CONNECTED)
                assertTrue(state.name, chrome.qsAppearance != QsTileUiMapper.Appearance.ACTIVE)
            }
        }
    }

    @Test
    fun goldenFixtureLinesStayStable() {
        val expected = listOf(
            "DISCONNECTED|DISCONNECTED|hotfox_headline_disconnected|false|false|INACTIVE|false",
            "PREPARING|SELECTING|hotfox_headline_selecting|false|false|CONNECTING|false",
            "WAITING_SOCKS|CONNECTING|hotfox_headline_connecting|false|false|CONNECTING|false",
            "VERIFYING_PATH|VERIFYING|hotfox_headline_verifying|false|false|CONNECTING|false",
            "CONNECTED|CONNECTED|hotfox_headline_connected|true|true|ACTIVE|true",
            "PROXY_ONLY|PROXY_ONLY|hotfox_headline_proxy_only|false|false|INACTIVE|false",
            "ERROR|ERROR|hotfox_headline_error|false|false|ERROR|false",
        )
        expected.forEach { line ->
            val state = VpnSessionState.valueOf(line.substringBefore('|'))
            assertEquals(line, HotfoxPremiumViewState.fixtureLine(state))
        }
    }

    @Test
    fun reducedMotionUsesAnimatorScale() {
        assertTrue(HotfoxMotion.reducedMotion(animatorDurationScale = 0f))
        assertTrue(HotfoxMotion.reducedMotion(animatorDurationScale = 1f, transitionAnimationScale = 0f))
        assertFalse(HotfoxMotion.reducedMotion(animatorDurationScale = 1f, transitionAnimationScale = 1f))
        assertEquals(0L, HotfoxMotion.durationMs(220L, reduced = true))
        assertEquals(220L, HotfoxMotion.durationMs(220L, reduced = false))
    }
}
