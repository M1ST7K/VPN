package com.v2ray.ang.vpn

import org.junit.Assert.assertEquals
import org.junit.Test

class HotfoxTrafficAccumulatorTest {
    @Test
    fun sessionRelativeValuesSubtractBaseline() {
        val acc = HotfoxTrafficAccumulator()
        acc.startSession(100L, 200L)
        val first = acc.sample(150L, 260L)
        assertEquals(50L, first.uploadedBytes)
        assertEquals(60L, first.downloadedBytes)
    }

    @Test
    fun nativeResetAccumulatesPreviousSegment() {
        val acc = HotfoxTrafficAccumulator()
        acc.startSession(0L, 0L)
        acc.sample(40L, 80L)
        val afterReset = acc.sample(5L, 10L)
        assertEquals(45L, afterReset.uploadedBytes)
        assertEquals(90L, afterReset.downloadedBytes)
    }

    @Test
    fun negativeNativeValuesClampToZero() {
        val acc = HotfoxTrafficAccumulator()
        acc.startSession(-10L, -4L)
        val sample = acc.sample(-1L, 8L)
        assertEquals(0L, sample.uploadedBytes)
        assertEquals(8L, sample.downloadedBytes)
    }
}
