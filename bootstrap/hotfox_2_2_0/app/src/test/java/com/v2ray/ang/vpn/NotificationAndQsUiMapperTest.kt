package com.v2ray.ang.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationAndQsUiMapperTest {
    @Test
    fun notificationProtectedOnlyWhenConnected() {
        val protectedCopy = NotificationUiMapper.from(VpnSessionState.CONNECTED)
        assertTrue(protectedCopy.isProtected)
        assertEquals("hotfox_headline_connected", protectedCopy.titleResName)

        val verifying = NotificationUiMapper.from(VpnSessionState.VERIFYING_PATH)
        assertFalse(verifying.isProtected)
        assertEquals("hotfox_headline_verifying", verifying.titleResName)

        val selecting = NotificationUiMapper.from(VpnSessionState.PREPARING)
        assertFalse(selecting.isProtected)
        assertEquals("hotfox_headline_selecting", selecting.titleResName)
    }

    @Test
    fun qsTileActiveOnlyWhenProtected() {
        assertTrue(QsTileUiMapper.isProtectedActive(VpnSessionState.CONNECTED))
        assertEquals(QsTileUiMapper.Appearance.ACTIVE, QsTileUiMapper.from(VpnSessionState.CONNECTED))
        assertEquals(QsTileUiMapper.Appearance.CONNECTING, QsTileUiMapper.from(VpnSessionState.VERIFYING_PATH))
        assertEquals(QsTileUiMapper.Appearance.CONNECTING, QsTileUiMapper.from(VpnSessionState.STARTING_HEV))
        assertEquals(QsTileUiMapper.Appearance.INACTIVE, QsTileUiMapper.from(VpnSessionState.DISCONNECTED))
        assertEquals(QsTileUiMapper.Appearance.INACTIVE, QsTileUiMapper.from(VpnSessionState.PROXY_ONLY))
        assertFalse(QsTileUiMapper.shouldStartOnClick(VpnSessionState.VERIFYING_PATH))
        assertTrue(QsTileUiMapper.shouldStopOnClick(VpnSessionState.CONNECTED))
        assertTrue(QsTileUiMapper.shouldStartOnClick(VpnSessionState.DISCONNECTED))
        assertFalse(QsTileUiMapper.shouldStopOnClick(VpnSessionState.PROXY_ONLY))
        assertFalse(NotificationUiMapper.from(VpnSessionState.PROXY_ONLY).isProtected)
        assertFalse(NotificationUiMapper.from(VpnSessionState.STARTING_HEV).isProtected)
    }
}
