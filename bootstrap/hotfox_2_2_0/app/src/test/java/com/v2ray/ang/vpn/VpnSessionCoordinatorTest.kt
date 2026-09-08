package com.v2ray.ang.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VpnSessionCoordinatorTest {
    @Before
    fun reset() {
        VpnSessionCoordinator.resetForTests()
    }

    @Test
    fun markConnectedRequiresCurrentAttemptAndPathVerification() {
        val stale = 1L
        assertFalse(VpnSessionCoordinator.markConnected(stale, pathVerified = true))
        assertEquals(VpnSessionState.DISCONNECTED, VpnSessionCoordinator.currentState())

        val attempt = VpnSessionCoordinator.beginAttempt()
        assertFalse(VpnSessionCoordinator.markConnected(attempt, pathVerified = false))
        assertEquals(VpnSessionState.PREPARING, VpnSessionCoordinator.currentState())

        assertTrue(VpnSessionCoordinator.markConnected(attempt, pathVerified = true))
        assertEquals(VpnSessionState.CONNECTED, VpnSessionCoordinator.currentState())
    }

    @Test
    fun setStateCannotBypassVerifiedConnected() {
        VpnSessionCoordinator.beginAttempt()
        VpnSessionCoordinator.setState(VpnSessionState.CONNECTED)
        assertEquals(VpnSessionState.PREPARING, VpnSessionCoordinator.currentState())
        assertFalse(ConnectionUiMapper.isProtectedHeadline(VpnSessionCoordinator.currentState()))
    }

    @Test
    fun staleGenerationCannotComplete() {
        val first = VpnSessionCoordinator.beginAttempt()
        VpnSessionCoordinator.beginAttempt()
        assertFalse(VpnSessionCoordinator.markConnected(first, pathVerified = true))
        assertFalse(VpnSessionCoordinator.markProxyOnly(first))
        assertFalse(VpnSessionCoordinator.markReconnecting(first))
        assertEquals(VpnSessionState.PREPARING, VpnSessionCoordinator.currentState())
    }

    @Test
    fun duplicateStartDuringStartupIsIgnored() {
        VpnSessionCoordinator.beginAttempt()
        VpnSessionCoordinator.setState(VpnSessionState.WAITING_SOCKS)
        assertEquals(DuplicateStartDisposition.IGNORE, VpnSessionCoordinator.duplicateStartDisposition())
        VpnSessionCoordinator.setState(VpnSessionState.VERIFYING_PATH)
        assertEquals(DuplicateStartDisposition.IGNORE, VpnSessionCoordinator.duplicateStartDisposition())
    }

    @Test
    fun duplicateStartRepublishesOnlyWhenAlreadyOperational() {
        val attempt = VpnSessionCoordinator.beginAttempt()
        assertTrue(VpnSessionCoordinator.markConnected(attempt, pathVerified = true))
        assertEquals(DuplicateStartDisposition.REPUBLISH, VpnSessionCoordinator.duplicateStartDisposition())

        VpnSessionCoordinator.resetForTests()
        val proxyAttempt = VpnSessionCoordinator.beginAttempt()
        assertTrue(VpnSessionCoordinator.markProxyOnly(proxyAttempt))
        assertEquals(DuplicateStartDisposition.REPUBLISH, VpnSessionCoordinator.duplicateStartDisposition())
        assertFalse(VpnSessionCoordinator.currentState().isProtected())
    }

    @Test
    fun reloadIsRejectedDuringStartup() {
        VpnSessionCoordinator.beginAttempt()
        VpnSessionCoordinator.setState(VpnSessionState.WAITING_SOCKS)
        assertFalse(VpnSessionCoordinator.tryBeginReload())
    }

    @Test
    fun reloadIsRejectedWhenLifecycleLockHeld() {
        val attempt = VpnSessionCoordinator.beginAttempt()
        assertTrue(VpnSessionCoordinator.markConnected(attempt, pathVerified = true))
        val started = java.util.concurrent.CountDownLatch(1)
        val release = java.util.concurrent.CountDownLatch(1)
        val holder = Thread({
            VpnSessionCoordinator.beginStart()
            started.countDown()
            release.await()
            VpnSessionCoordinator.endStart()
        }, "hotfox-lock-holder")
        holder.start()
        assertTrue(started.await(2, java.util.concurrent.TimeUnit.SECONDS))
        try {
            assertFalse(VpnSessionCoordinator.tryBeginReload())
        } finally {
            release.countDown()
            holder.join(2_000)
        }
        assertTrue(VpnSessionCoordinator.tryBeginReload())
        VpnSessionCoordinator.endReload()
    }

    @Test
    fun stopInvalidatesInFlightCompletion() {
        val attempt = VpnSessionCoordinator.beginAttempt()
        VpnSessionCoordinator.markDisconnected()
        assertFalse(VpnSessionCoordinator.isCurrent(attempt))
        assertFalse(VpnSessionCoordinator.markConnected(attempt, pathVerified = true))
        assertEquals(VpnSessionState.DISCONNECTED, VpnSessionCoordinator.currentState())
    }
}
