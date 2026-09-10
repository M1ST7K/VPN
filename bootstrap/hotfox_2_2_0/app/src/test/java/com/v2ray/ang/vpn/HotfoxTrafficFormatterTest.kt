package com.v2ray.ang.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HotfoxTrafficFormatterTest {
    @Test
    fun unknownBytesStayEmDash() {
        assertEquals("—", HotfoxTrafficFormatter.formatBytes(null))
    }

    @Test
    fun smallValuesStayInBytes() {
        assertEquals("12 B", HotfoxTrafficFormatter.formatBytes(12L))
    }

    @Test
    fun masksPathAndQueryOfSubscriptionUrl() {
        val shown = HotfoxTrafficFormatter.maskSubscriptionUrl(
            "https://provider.example/sub/sUq75ktDSV7LmQuB?token=abc"
        )
        assertTrue(shown.startsWith("https://provider.example/"))
        assertFalse(shown.contains("sUq75ktDSV7LmQuB"))
        assertFalse(shown.contains("token=abc"))
    }
}
