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

    @Test
    fun stalePathFailureDoesNotErrorNewerAttempt() {
        val first = VpnSessionCoordinator.beginAttempt()
        val second = VpnSessionCoordinator.beginAttempt()
        assertFalse(VpnSessionCoordinator.markError(first, "HF-VPN-004", "socks timeout"))
        assertEquals(VpnSessionState.PREPARING, VpnSessionCoordinator.currentState())
        assertTrue(VpnSessionCoordinator.isCurrent(second))
        assertTrue(VpnSessionCoordinator.markError(second, "HF-VPN-004", "socks timeout"))
        assertEquals(VpnSessionState.ERROR, VpnSessionCoordinator.currentState())
        assertFalse(VpnSessionCoordinator.isCurrent(second))
    }

    @Test
    fun staleSetStateAndRecordPathAreIgnored() {
        val first = VpnSessionCoordinator.beginAttempt()
        val second = VpnSessionCoordinator.beginAttempt()
        assertFalse(VpnSessionCoordinator.setState(first, VpnSessionState.WAITING_SOCKS))
        assertEquals(VpnSessionState.PREPARING, VpnSessionCoordinator.currentState())
        val path = VpnPathVerification(
            socks5Ready = true,
            hevAlive = true,
            tunForwarded = true,
            xrayEgressMs = 1L,
            tunEstablished = true,
            backend = VpnPathVerification.BACKEND_HEV,
            verified = true,
        )
        assertFalse(VpnSessionCoordinator.recordPath(first, path))
        assertEquals(null, VpnSessionCoordinator.lastPath())
        assertTrue(VpnSessionCoordinator.recordPath(second, path))
        assertEquals(true, VpnSessionCoordinator.lastPath()?.verified)
    }

    @Test
    fun shutdownTimeoutRejectsRestartUntilBarrierClears() {
        VpnSessionCoordinator.markStopIncomplete("Ядро не остановилось")
        assertEquals(VpnSessionState.ERROR, VpnSessionCoordinator.currentState())
        assertTrue(VpnSessionCoordinator.isTeardownActive())
        assertEquals(0L, VpnSessionCoordinator.beginAttempt())
        assertEquals(
            DuplicateStartDisposition.IGNORE,
            VpnSessionCoordinator.duplicateStartDisposition(),
        )
        kotlinx.coroutines.runBlocking {
            assertFalse(VpnSessionCoordinator.awaitIdle(120L))
        }
        VpnSessionCoordinator.setTeardownActive(false)
        val attempt = VpnSessionCoordinator.beginAttempt()
        assertTrue(attempt > 0L)
        assertEquals(VpnSessionState.PREPARING, VpnSessionCoordinator.currentState())
    }

    @Test
    fun completeStopOutcomeFalseKeepsRestartBarrier() {
        VpnSessionCoordinator.completeStopOutcome(false)
        assertEquals(VpnSessionState.ERROR, VpnSessionCoordinator.currentState())
        assertTrue(VpnSessionCoordinator.isTeardownActive())
        assertFalse(VpnSessionCoordinator.lastStopSucceeded())
        assertEquals(0L, VpnSessionCoordinator.beginAttempt())
        kotlinx.coroutines.runBlocking {
            assertFalse(VpnSessionCoordinator.awaitIdle(80L))
        }
    }

    @Test
    fun staleWriterCannotOverwriteNewerGeneration() {
        val first = VpnSessionCoordinator.beginAttempt()
        val second = VpnSessionCoordinator.beginAttempt()
        val path = VpnPathVerification(
            socks5Ready = true,
            hevAlive = true,
            tunForwarded = true,
            xrayEgressMs = 1L,
            tunEstablished = true,
            backend = VpnPathVerification.BACKEND_HEV,
            verified = true,
        )
        val started = java.util.concurrent.CountDownLatch(1)
        val done = java.util.concurrent.CountDownLatch(2)
        Thread({
            started.countDown()
            VpnSessionCoordinator.recordPath(first, path)
            VpnSessionCoordinator.markError(first, "HF-VPN-004", "stale")
            done.countDown()
        }, "stale-writer").start()
        Thread({
            started.await()
            VpnSessionCoordinator.recordPath(second, path)
            VpnSessionCoordinator.markConnected(second, pathVerified = true)
            done.countDown()
        }, "current-writer").start()
        assertTrue(done.await(2, java.util.concurrent.TimeUnit.SECONDS))
        assertEquals(VpnSessionState.CONNECTED, VpnSessionCoordinator.currentState())
        assertTrue(VpnSessionCoordinator.isCurrent(second))
        assertFalse(VpnSessionCoordinator.lastError()?.contains("HF-VPN-004") == true)
    }
}
