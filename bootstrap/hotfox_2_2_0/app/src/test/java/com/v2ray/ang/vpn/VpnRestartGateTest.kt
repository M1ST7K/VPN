package com.v2ray.ang.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VpnRestartGateTest {
    @Before
    fun reset() {
        VpnRestartGate.resetForTests()
        VpnSessionCoordinator.resetForTests()
    }

    @Test
    fun disconnectInvalidatesInFlightRestart() {
        val request = VpnRestartGate.nextRequest()
        assertTrue(VpnRestartGate.isCurrent(request))
        VpnRestartGate.invalidate()
        assertFalse(VpnRestartGate.isCurrent(request))
    }

    @Test
    fun laterStartInvalidatesEarlierRestart() {
        val restart = VpnRestartGate.nextRequest()
        VpnRestartGate.invalidate()
        val newer = VpnRestartGate.nextRequest()
        assertFalse(VpnRestartGate.isCurrent(restart))
        assertTrue(VpnRestartGate.isCurrent(newer))
    }
}

class VpnLoopPreventionPolicyTest {
    @Before
    fun reset() {
        VpnSessionCoordinator.resetForTests()
    }

    @Test
    fun initialBindFailureFailsClosed() {
        val attempt = VpnSessionCoordinator.beginAttempt()
        assertFalse(VpnLoopPrevention.requireBindSuccess(false))
        assertTrue(
            VpnSessionCoordinator.markError(
                attempt,
                "HF-VPN-012",
                "Не удалось привязать процесс к внешней сети",
            ),
        )
        assertEquals(VpnSessionState.ERROR, VpnSessionCoordinator.currentState())
        assertFalse(VpnSessionCoordinator.markConnected(attempt, pathVerified = true))
        assertFalse(ConnectionUiMapper.isProtectedHeadline(VpnSessionCoordinator.currentState()))
    }

    @Test
    fun handoverBindFailureFailsClosed() {
        val attempt = VpnSessionCoordinator.beginAttempt()
        assertTrue(VpnSessionCoordinator.markConnected(attempt, pathVerified = true))
        assertFalse(VpnLoopPrevention.requireBindSuccess(false))
        assertTrue(
            VpnSessionCoordinator.markError(
                attempt,
                "HF-VPN-012",
                "Не удалось привязать процесс к внешней сети",
            ),
        )
        assertEquals(VpnSessionState.ERROR, VpnSessionCoordinator.currentState())
        assertFalse(VpnSessionCoordinator.markConnected(attempt, pathVerified = true))
        assertFalse(ConnectionUiMapper.isProtectedHeadline(VpnSessionCoordinator.currentState()))
    }

    @Test
    fun successfulBindAllowsPathVerification() {
        val attempt = VpnSessionCoordinator.beginAttempt()
        assertTrue(VpnLoopPrevention.requireBindSuccess(true))
        assertTrue(VpnSessionCoordinator.markConnected(attempt, pathVerified = true))
        assertEquals(VpnSessionState.CONNECTED, VpnSessionCoordinator.currentState())
    }
}
