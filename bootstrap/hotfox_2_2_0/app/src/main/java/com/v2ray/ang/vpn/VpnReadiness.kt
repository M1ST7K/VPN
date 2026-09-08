package com.v2ray.ang.vpn

import android.os.SystemClock
import com.v2ray.ang.AppConfig
import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.coroutines.delay

/** Bounded local SOCKS readiness used before HEV start and after core reload. */
object VpnReadiness {
    suspend fun waitForLocalSocks(port: Int, timeoutMillis: Long = 8_000L): Boolean {
        val deadline = SystemClock.elapsedRealtime() + timeoutMillis
        while (SystemClock.elapsedRealtime() < deadline) {
            if (probe(port)) return true
            delay(50L)
        }
        return false
    }

    fun waitForLocalSocksBlocking(port: Int, timeoutMillis: Long = 8_000L): Boolean {
        val deadline = SystemClock.elapsedRealtime() + timeoutMillis
        while (SystemClock.elapsedRealtime() < deadline) {
            if (probe(port)) return true
            try {
                Thread.sleep(50L)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return false
            }
        }
        return false
    }

    fun probe(port: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(AppConfig.LOOPBACK, port), 200)
                true
            }
        } catch (_: Exception) {
            false
        }
    }
}
