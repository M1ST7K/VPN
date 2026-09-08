package com.v2ray.ang.vpn

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Generation-aware Xray shutdown ownership. A temporal global Boolean cannot
 * distinguish an expected old-core stop from a replacement-core death.
 *
 * Call [onCoreLaunched] after a successful startLoop. Before stopLoop of that
 * generation, [expectShutdownOf] that generation, then [drain] before publishing
 * a replacement. A delayed callback for the expected generation is consumed and
 * does not tear down the replacement.
 */
class XrayShutdownGate {
    enum class Disposition { EXPECTED, UNEXPECTED }

    private val coreGeneration = AtomicLong(0L)
    private val expectedShutdownGeneration = AtomicLong(NONE)
    private val drainLatch = AtomicReference<CountDownLatch?>(null)

    fun currentGeneration(): Long = coreGeneration.get()

    fun onCoreLaunched(): Long = coreGeneration.incrementAndGet()

    fun expectShutdownOf(generation: Long): CountDownLatch {
        val latch = CountDownLatch(1)
        drainLatch.set(latch)
        expectedShutdownGeneration.set(generation)
        return latch
    }

    fun drain(timeoutMs: Long): Boolean {
        val latch = drainLatch.get() ?: return true
        return latch.await(timeoutMs, TimeUnit.MILLISECONDS)
    }

    fun onCoreShutdown(): Disposition {
        val expected = expectedShutdownGeneration.get()
        if (expected != NONE && expectedShutdownGeneration.compareAndSet(expected, NONE)) {
            drainLatch.get()?.countDown()
            return Disposition.EXPECTED
        }
        return Disposition.UNEXPECTED
    }

    fun clearExpected() {
        expectedShutdownGeneration.set(NONE)
        drainLatch.get()?.countDown()
        drainLatch.set(null)
    }

    fun resetForTests() {
        coreGeneration.set(0L)
        expectedShutdownGeneration.set(NONE)
        drainLatch.set(null)
    }

    companion object {
        const val NONE = -1L
    }
}
