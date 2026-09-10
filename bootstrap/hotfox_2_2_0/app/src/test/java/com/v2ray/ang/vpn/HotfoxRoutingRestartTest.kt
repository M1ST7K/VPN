package com.v2ray.ang.vpn

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class HotfoxRoutingRestartTest {
    @Before
    fun reset() {
        VpnRestartGate.resetForTests()
        VpnSessionCoordinator.resetForTests()
        HotfoxRoutingApply.resetForTests()
    }

    @Test
    fun disconnectInvalidatesInFlightRoutingRestart() {
        HotfoxRoutingApply.bump()
        val plan = HotfoxRoutingRestart.begin()
        val dispatched = AtomicBoolean(false)
        VpnRestartGate.invalidate()
        assertFalse(HotfoxRoutingRestart.tryDispatch(plan) { dispatched.set(true) })
        assertFalse(dispatched.get())
    }

    @Test
    fun newerRoutingGenerationCancelsStaleRestart() {
        HotfoxRoutingApply.bump()
        val plan = HotfoxRoutingRestart.begin()
        HotfoxRoutingApply.bump()
        val dispatched = AtomicBoolean(false)
        assertFalse(HotfoxRoutingRestart.tryDispatch(plan) { dispatched.set(true) })
        assertFalse(dispatched.get())
    }

    @Test
    fun teardownActiveBlocksRoutingRestart() {
        HotfoxRoutingApply.bump()
        val plan = HotfoxRoutingRestart.begin()
        VpnSessionCoordinator.setTeardownActive(true)
        val dispatched = AtomicBoolean(false)
        assertFalse(HotfoxRoutingRestart.tryDispatch(plan) { dispatched.set(true) })
        assertFalse(dispatched.get())
    }

    @Test
    fun liveRoutingRestartDispatchesWhenNoNewerStop() {
        HotfoxRoutingApply.bump()
        val plan = HotfoxRoutingRestart.begin()
        val dispatched = AtomicBoolean(false)
        assertTrue(HotfoxRoutingRestart.tryDispatch(plan) { dispatched.set(true) })
        assertTrue(dispatched.get())
    }
}
