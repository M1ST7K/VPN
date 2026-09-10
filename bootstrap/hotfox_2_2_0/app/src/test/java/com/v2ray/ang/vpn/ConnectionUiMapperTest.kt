package com.v2ray.ang.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionUiMapperTest {
    @Test
    fun incompleteStartupNeverMapsToProtected() {
        val incomplete = listOf(
            VpnSessionState.PREPARING,
            VpnSessionState.STARTING_CORE,
            VpnSessionState.WAITING_SOCKS,
            VpnSessionState.ESTABLISHING_TUN,
            VpnSessionState.STARTING_HEV,
            VpnSessionState.VERIFYING_PATH,
        )
        incomplete.forEach { state ->
            assertFalse(state.name, ConnectionUiMapper.isProtectedHeadline(state))
            assertTrue(
                state.name,
                ConnectionUiMapper.headline(state) != ConnectionUiMapper.Headline.CONNECTED,
            )
        }
        assertEquals(ConnectionUiMapper.Headline.SELECTING, ConnectionUiMapper.headline(VpnSessionState.PREPARING))
        assertEquals(ConnectionUiMapper.Headline.VERIFYING, ConnectionUiMapper.headline(VpnSessionState.VERIFYING_PATH))
        assertEquals(ConnectionUiMapper.Headline.CONNECTING, ConnectionUiMapper.headline(VpnSessionState.WAITING_SOCKS))
        assertEquals(ConnectionUiPhase.SELECTING, VpnSessionState.PREPARING.uiPhase())
        assertEquals(ConnectionUiPhase.VERIFYING, VpnSessionState.VERIFYING_PATH.uiPhase())
        assertEquals(ConnectionUiPhase.CONNECTING, VpnSessionState.STARTING_HEV.uiPhase())
    }

    @Test
    fun selectingAndVerifyingStayDistinctFromProtected() {
        assertEquals(
            ConnectionUiMapper.Headline.SELECTING,
            ConnectionUiMapper.headline(
                VpnSessionState.PREPARING,
                VpnConnectionStage.RESOLVE_SERVER,
                autoSelecting = true,
            ),
        )
        assertEquals(
            ConnectionUiMapper.Headline.VERIFYING,
            ConnectionUiMapper.headline(
                VpnSessionState.VERIFYING_PATH,
                VpnConnectionStage.TUN_INJECT,
                autoSelecting = false,
            ),
        )
        assertFalse(
            ConnectionUiMapper.isProtectedHeadline(VpnSessionState.VERIFYING_PATH),
        )
    }

    @Test
    fun connectedIsTheOnlyProtectedHeadline() {
        assertTrue(ConnectionUiMapper.isProtectedHeadline(VpnSessionState.CONNECTED))
        assertFalse(ConnectionUiMapper.isProtectedHeadline(VpnSessionState.RECONNECTING))
        assertFalse(ConnectionUiMapper.isProtectedHeadline(VpnSessionState.ERROR))
        assertFalse(ConnectionUiMapper.isProtectedHeadline(VpnSessionState.DISCONNECTED))
        assertFalse(ConnectionUiMapper.isProtectedHeadline(VpnSessionState.PROXY_ONLY))
        assertFalse(ConnectionUiMapper.isProtectedHeadline(VpnSessionState.ROOT_RUNNING))
        assertFalse(VpnSessionState.PROXY_ONLY.isProtected())
        assertFalse(VpnSessionState.ROOT_RUNNING.isProtected())
        assertEquals(ConnectionUiMapper.Headline.PROXY_ONLY, ConnectionUiMapper.headline(VpnSessionState.PROXY_ONLY))
        assertEquals(ConnectionUiMapper.Headline.ROOT_RUNNING, ConnectionUiMapper.headline(VpnSessionState.ROOT_RUNNING))
    }

    @Test
    fun resolveHeadlineNeverPromotesBusyOrProxyToProtected() {
        assertEquals(
            ConnectionUiMapper.Headline.CONNECTING,
            ConnectionUiMapper.resolveHeadline(VpnSessionState.WAITING_SOCKS, isLoading = false),
        )
        assertEquals(
            ConnectionUiMapper.Headline.PROXY_ONLY,
            ConnectionUiMapper.resolveHeadline(VpnSessionState.PROXY_ONLY, isLoading = false),
        )
        assertEquals(
            ConnectionUiMapper.Headline.CONNECTED,
            ConnectionUiMapper.resolveHeadline(VpnSessionState.CONNECTED, isLoading = false),
        )
        assertFalse(ConnectionUiMapper.isProtectedHeadline(VpnSessionState.PROXY_ONLY))
    }

    @Test
    fun reconnectingKeepsTimer() {
        assertTrue(ConnectionUiMapper.timerShouldRun(VpnSessionState.CONNECTED))
        assertTrue(ConnectionUiMapper.timerShouldRun(VpnSessionState.RECONNECTING))
        assertFalse(ConnectionUiMapper.timerShouldRun(VpnSessionState.WAITING_SOCKS))
        assertFalse(ConnectionUiMapper.timerShouldRun(VpnSessionState.VERIFYING_PATH))
    }
}
