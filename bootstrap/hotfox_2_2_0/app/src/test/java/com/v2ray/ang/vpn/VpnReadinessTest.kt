package com.v2ray.ang.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class VpnReadinessTest {
    @Before
    fun reset() {
        VpnSessionCoordinator.resetForTests()
    }

    @Test
    fun socks5HandshakeSucceedsAgainstProtocolListener() {
        val server = ServerSocket(0)
        val port = server.localPort
        val worker = thread(name = "socks5-ok") {
            server.accept().use { client ->
                val input = client.getInputStream()
                val header = ByteArray(3)
                var n = 0
                while (n < 3) {
                    val r = input.read(header, n, 3 - n)
                    if (r < 0) return@thread
                    n += r
                }
                client.getOutputStream().write(byteArrayOf(0x05, 0x00))
                client.getOutputStream().flush()
            }
        }
        try {
            assertTrue(VpnReadiness.probeSocks5(port))
        } finally {
            worker.join(2_000)
            server.close()
        }
    }

    @Test
    fun tcpAcceptWithoutSocksReplyFails() {
        val server = ServerSocket(0)
        val port = server.localPort
        val worker = thread(name = "tcp-only") {
            server.accept().use { _ ->
                Thread.sleep(500)
            }
        }
        try {
            assertFalse(VpnReadiness.probeSocks5(port))
        } finally {
            worker.join(2_000)
            server.close()
        }
    }

    @Test
    fun hevStatsRequireFourNonNegativeSamplesTwice() {
        assertFalse(VpnReadiness.hevStatsAlive(null))
        assertFalse(VpnReadiness.hevStatsAlive(longArrayOf(1, 2, 3)))
        assertFalse(VpnReadiness.hevStatsAlive(longArrayOf(1, 2, 3, -1)))
        assertTrue(VpnReadiness.hevStatsAlive(longArrayOf(0, 0, 0, 0)))
        assertTrue(VpnReadiness.hevStatsAlive(longArrayOf(1, 2, 3, 4)))

        var calls = 0
        val alive = VpnReadiness.waitHevAliveBlocking(
            getStats = {
                calls++
                longArrayOf(0, 10, 0, 20)
            },
            timeoutMillis = 1_000L,
            sampleDelayMs = 10L,
        )
        assertTrue(alive)
        assertTrue(calls >= 2)
    }

    @Test
    fun configuredPathFailsClosedWithoutEgress() {
        val server = ServerSocket(0)
        val port = server.localPort
        val worker = thread {
            while (!server.isClosed) {
                val client: Socket = try {
                    server.accept()
                } catch (_: Exception) {
                    break
                }
                client.use {
                    val input = it.getInputStream()
                    val header = ByteArray(3)
                    var n = 0
                    while (n < 3) {
                        val r = input.read(header, n, 3 - n)
                        if (r < 0) return@use
                        n += r
                    }
                    it.getOutputStream().write(byteArrayOf(0x05, 0x00))
                    it.getOutputStream().flush()
                }
            }
        }
        try {
            val path = VpnReadiness.verifyConfiguredPathBlocking(
                socksPort = port,
                tunEstablished = true,
                hevStatsProvider = { longArrayOf(0, 0, 0, 0) },
                xrayEgressMs = { null },
            )
            assertTrue(path.socks5Ready)
            assertTrue(path.hevAlive == true)
            assertNull(path.xrayEgressMs)
            assertFalse(path.verified)
            assertEquals("xray-egress-failed", path.reason)
            assertEquals(VpnPathVerification.PHYSICAL_E2E_NOT_EXECUTED, path.physicalDeviceE2e)
        } finally {
            server.close()
            worker.join(2_000)
        }
    }

    @Test
    fun configuredPathPassesWithSocksHevAndEgress() {
        val server = ServerSocket(0)
        val port = server.localPort
        val worker = thread {
            while (!server.isClosed) {
                val client: Socket = try {
                    server.accept()
                } catch (_: Exception) {
                    break
                }
                client.use {
                    val input = it.getInputStream()
                    val header = ByteArray(3)
                    var n = 0
                    while (n < 3) {
                        val r = input.read(header, n, 3 - n)
                        if (r < 0) return@use
                        n += r
                    }
                    it.getOutputStream().write(byteArrayOf(0x05, 0x00))
                    it.getOutputStream().flush()
                }
            }
        }
        try {
            val path = VpnReadiness.verifyConfiguredPathBlocking(
                socksPort = port,
                tunEstablished = true,
                hevStatsProvider = { longArrayOf(0, 1, 0, 2) },
                xrayEgressMs = { 42L },
            )
            assertTrue(path.verified)
            assertEquals(42L, path.xrayEgressMs)
            assertEquals(VpnPathVerification.BACKEND_HEV, path.backend)
        } finally {
            server.close()
            worker.join(2_000)
        }
    }
}
