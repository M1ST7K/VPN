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
                tunInjector = { true },
            )
            assertTrue(path.socks5Ready)
            assertTrue(path.hevAlive == true)
            assertEquals(false, path.tunForwarded)
            assertNull(path.xrayEgressMs)
            assertFalse(path.verified)
            assertEquals("tun-not-forwarded", path.reason)
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
            var stats = longArrayOf(0, 1, 0, 2)
            val path = VpnReadiness.verifyConfiguredPathBlocking(
                socksPort = port,
                tunEstablished = true,
                hevStatsProvider = { stats },
                xrayEgressMs = { 42L },
                tunInjector = {
                    stats = longArrayOf(2, 80, 1, 40)
                    true
                },
            )
            assertTrue(path.verified)
            assertEquals(42L, path.xrayEgressMs)
            assertEquals(true, path.tunForwarded)
            assertEquals(true, path.hevProgressed)
            assertEquals(VpnPathVerification.BACKEND_HEV, path.backend)
        } finally {
            server.close()
            worker.join(2_000)
        }
    }

    @Test
    fun http204WithoutCounterProgressIsNotVerified() {
        withSocks5 { port ->
            val path = VpnReadiness.verifyConfiguredPathBlocking(
                socksPort = port,
                tunEstablished = true,
                hevStatsProvider = { longArrayOf(0, 0, 0, 0) },
                xrayEgressMs = { 12L },
                tunInjector = { true },
            )
            assertTrue(path.socks5Ready)
            assertEquals(12L, path.xrayEgressMs)
            assertEquals(false, path.tunForwarded)
            assertFalse(path.verified)
            assertEquals("tun-not-forwarded", path.reason)
        }
    }

    @Test
    fun missingTunInjectorFailsClosedEvenWithHttp204() {
        withSocks5 { port ->
            val path = VpnReadiness.verifyConfiguredPathBlocking(
                socksPort = port,
                tunEstablished = true,
                hevStatsProvider = { longArrayOf(1, 2, 3, 4) },
                xrayEgressMs = { 12L },
            )
            assertFalse(path.verified)
            assertEquals("tun-not-forwarded", path.reason)
        }
    }

    @Test
    fun xrayComponentCheckStillRequiredAfterTunProgress() {
        withSocks5 { port ->
            var stats = longArrayOf(0, 1, 0, 2)
            val path = VpnReadiness.verifyConfiguredPathBlocking(
                socksPort = port,
                tunEstablished = true,
                hevStatsProvider = { stats },
                xrayEgressMs = { null },
                tunInjector = {
                    stats = longArrayOf(2, 80, 1, 40)
                    true
                },
            )
            assertEquals(true, path.tunForwarded)
            assertNull(path.xrayEgressMs)
            assertFalse(path.verified)
            assertEquals("xray-egress-failed", path.reason)
        }
    }

    @Test
    fun countersAdvancedRequiresIncreaseOrResetWindow() {
        assertFalse(VpnReadiness.countersAdvanced(null, longArrayOf(1, 2, 3, 4)))
        assertFalse(VpnReadiness.countersAdvanced(longArrayOf(0, 0, 0, 0), longArrayOf(0, 0, 0, 0)))
        assertFalse(VpnReadiness.countersAdvanced(longArrayOf(1, 2, 3, 4), longArrayOf(1, 2, 3, 4)))
        assertTrue(VpnReadiness.countersAdvanced(longArrayOf(0, 0, 0, 0), longArrayOf(0, 1, 0, 0)))
        assertTrue(VpnReadiness.countersAdvanced(longArrayOf(10, 100, 10, 100), longArrayOf(0, 4, 0, 8)))
    }

    private fun withSocks5(block: (Int) -> Unit) {
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
            block(port)
        } finally {
            server.close()
            worker.join(2_000)
        }
    }
}
