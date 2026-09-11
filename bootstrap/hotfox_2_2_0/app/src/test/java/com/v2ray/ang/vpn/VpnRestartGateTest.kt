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

    @Test
    fun stopBetweenAuthorizeAndDispatchDoesNotStart() {
        val request = VpnRestartGate.nextRequest()
        val authorized = java.util.concurrent.CountDownLatch(1)
        val resume = java.util.concurrent.CountDownLatch(1)
        VpnRestartGate.testProbe = VpnRestartGate.DispatchProbe(authorized, resume)
        val dispatched = java.util.concurrent.atomic.AtomicBoolean(false)
        val done = java.util.concurrent.CountDownLatch(1)
        val worker = Thread({
            try {
                val started = VpnRestartGate.tryDispatchStart(request) {
                    dispatched.set(true)
                }
                assertFalse(started)
            } finally {
                done.countDown()
            }
        }, "restart-dispatch")
        worker.start()
        assertTrue(authorized.await(2, java.util.concurrent.TimeUnit.SECONDS))
        VpnRestartGate.invalidate()
        resume.countDown()
        assertTrue(done.await(2, java.util.concurrent.TimeUnit.SECONDS))
        worker.join(2_000)
        assertFalse(dispatched.get())
        assertFalse(VpnRestartGate.isCurrent(request))
    }

    @Test
    fun authorizedRestartDispatchesWhenNoNewerStop() {
        val request = VpnRestartGate.nextRequest()
        val dispatched = java.util.concurrent.atomic.AtomicBoolean(false)
        assertTrue(VpnRestartGate.tryDispatchStart(request) { dispatched.set(true) })
        assertTrue(dispatched.get())
    }

    @Test
    fun teardownActiveBlocksRestartDispatch() {
        val request = VpnRestartGate.nextRequest()
        VpnSessionCoordinator.setTeardownActive(true)
        val dispatched = java.util.concurrent.atomic.AtomicBoolean(false)
        assertFalse(VpnRestartGate.tryDispatchStart(request) { dispatched.set(true) })
        assertFalse(dispatched.get())
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
