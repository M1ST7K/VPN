package com.v2ray.ang.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class XrayShutdownGateTest {
    private val gate = XrayShutdownGate()

    @Before
    fun reset() {
        gate.resetForTests()
    }

    @Test
    fun expectedOldShutdownDuringHandoverIsConsumed() {
        val gen = gate.onCoreLaunched()
        gate.expectShutdownOf(gen)
        assertEquals(XrayShutdownGate.Disposition.EXPECTED, gate.onCoreShutdown())
        assertTrue(gate.drain(50))
        val replacement = gate.onCoreLaunched()
        assertTrue(replacement > gen)
        assertEquals(XrayShutdownGate.Disposition.UNEXPECTED, gate.onCoreShutdown())
    }

    @Test
    fun delayedOldShutdownAfterReplacementDoesNotLookLikeReplacementDeath() {
        val oldGen = gate.onCoreLaunched()
        val startDelayed = CountDownLatch(1)
        val done = CountDownLatch(1)
        val seen = AtomicReference<XrayShutdownGate.Disposition?>(null)
        gate.expectShutdownOf(oldGen)
        Thread({
            startDelayed.await(1, TimeUnit.SECONDS)
            seen.set(gate.onCoreShutdown())
            done.countDown()
        }, "delayed-old-shutdown").start()
        assertFalse(gate.drain(20))
        val replacement = gate.onCoreLaunched()
        assertTrue(replacement > oldGen)
        startDelayed.countDown()
        assertTrue(done.await(1, TimeUnit.SECONDS))
        assertEquals(XrayShutdownGate.Disposition.EXPECTED, seen.get())
        assertEquals(XrayShutdownGate.Disposition.UNEXPECTED, gate.onCoreShutdown())
    }

    @Test
    fun replacementFailureAfterDrainIsNotSuppressed() {
        val oldGen = gate.onCoreLaunched()
        gate.expectShutdownOf(oldGen)
        assertEquals(XrayShutdownGate.Disposition.EXPECTED, gate.onCoreShutdown())
        assertTrue(gate.drain(50))
        gate.onCoreLaunched()
        assertEquals(XrayShutdownGate.Disposition.UNEXPECTED, gate.onCoreShutdown())
    }

    @Test
    fun twoRapidHandoversConsumeOnlyTheirOwnGeneration() {
        val first = gate.onCoreLaunched()
        gate.expectShutdownOf(first)
        assertEquals(XrayShutdownGate.Disposition.EXPECTED, gate.onCoreShutdown())
        val second = gate.onCoreLaunched()
        gate.expectShutdownOf(second)
        assertEquals(XrayShutdownGate.Disposition.EXPECTED, gate.onCoreShutdown())
        val third = gate.onCoreLaunched()
        assertTrue(third > second)
        assertEquals(XrayShutdownGate.Disposition.UNEXPECTED, gate.onCoreShutdown())
    }
}
