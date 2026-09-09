package com.v2ray.ang.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class BoundedProcessTest {
    @Test
    fun timeoutDoesNotWaitForStdoutEof() {
        val process = ProcessBuilder("sleep", "20").start()
        val started = System.currentTimeMillis()
        try {
            val result = BoundedProcess.collect(process, 1, TimeUnit.SECONDS)
            val elapsed = System.currentTimeMillis() - started
            assertFalse(result.finished)
            assertTrue("timeout took ${elapsed}ms", elapsed < 5_000L)
        } finally {
            BoundedProcess.destroyQuietly(process)
        }
    }

    @Test
    fun capturesOutputOfQuickCommand() {
        val process = ProcessBuilder("echo", "0").start()
        val result = BoundedProcess.collect(process, 5, TimeUnit.SECONDS)
        assertTrue(result.finished)
        assertEquals(0, result.exitCode)
        assertEquals("0", result.output.trim())
    }
}
