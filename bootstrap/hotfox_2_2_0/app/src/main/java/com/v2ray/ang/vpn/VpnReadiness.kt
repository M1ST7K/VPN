package com.v2ray.ang.vpn

import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.util.LogUtil
import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.coroutines.delay
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.HttpURLConnection
import java.net.Proxy
import java.util.concurrent.TimeUnit

/**
 * Bounded operational checks for the configured production path.
 *
 * SOCKS5 handshake proves the local inbound speaks SOCKS, not merely that a TCP
 * listener accepted a connection. HEV liveness requires two valid stats samples.
 * Xray egress is an HTTP 204 through the local HTTP inbound — health of Xray
 * outbound, not TUN capture of third-party apps. Physical-device IP/DNS/IPv6
 * proof is never inferred from these checks.
 */
object VpnReadiness {
    private val egressUrls = listOf(
        "https://www.gstatic.com/generate_204",
        "https://cp.cloudflare.com/generate_204",
    )

    suspend fun waitForLocalSocks(
        port: Int,
        timeoutMillis: Long = 8_000L,
        username: String? = null,
        password: String? = null,
    ): Boolean {
        val deadline = nowMs() + timeoutMillis
        while (nowMs() < deadline) {
            if (probe(port, username, password)) return true
            delay(50L)
        }
        return false
    }

    fun waitForLocalSocksBlocking(
        port: Int,
        timeoutMillis: Long = 8_000L,
        username: String? = null,
        password: String? = null,
    ): Boolean {
        val deadline = nowMs() + timeoutMillis
        while (nowMs() < deadline) {
            if (probe(port, username, password)) return true
            try {
                Thread.sleep(50L)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return false
            }
        }
        return false
    }

    /** SOCKS5 greeting/handshake. TCP accept without a SOCKS reply is a failure. */
    fun probe(port: Int, username: String? = null, password: String? = null): Boolean =
        probeSocks5(port, username, password)

    fun probeSocks5(port: Int, username: String? = null, password: String? = null): Boolean {
        return try {
            Socket().use { socket ->
                socket.soTimeout = 400
                socket.connect(InetSocketAddress(AppConfig.LOOPBACK, port), 250)
                val out = socket.getOutputStream()
                val input = socket.getInputStream()
                val methods = if (!username.isNullOrEmpty() && !password.isNullOrEmpty()) {
                    byteArrayOf(0x05, 0x02, 0x00, 0x02)
                } else {
                    byteArrayOf(0x05, 0x01, 0x00)
                }
                out.write(methods)
                out.flush()
                val header = ByteArray(2)
                if (!readFully(input, header)) return false
                if (header[0] != 0x05.toByte()) return false
                when (header[1]) {
                    0x00.toByte() -> true
                    0x02.toByte() -> {
                        if (username.isNullOrEmpty() || password.isNullOrEmpty()) return false
                        authenticateUserPass(socket, username, password)
                    }
                    else -> false
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    fun hevStatsAlive(stats: LongArray?): Boolean {
        if (stats == null || stats.size != 4) return false
        return stats.all { it >= 0L }
    }

    fun waitHevAliveBlocking(
        getStats: () -> LongArray?,
        timeoutMillis: Long = 3_000L,
        sampleDelayMs: Long = 80L,
    ): Boolean {
        val deadline = nowMs() + timeoutMillis
        var firstOk = false
        while (nowMs() < deadline) {
            if (hevStatsAlive(getStats())) {
                if (!firstOk) {
                    firstOk = true
                    try {
                        Thread.sleep(sampleDelayMs)
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                        return false
                    }
                    continue
                }
                return true
            }
            try {
                Thread.sleep(50L)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return false
            }
        }
        return false
    }

    suspend fun waitHevAlive(
        getStats: () -> LongArray?,
        timeoutMillis: Long = 3_000L,
        sampleDelayMs: Long = 80L,
    ): Boolean = waitHevAliveBlocking(getStats, timeoutMillis, sampleDelayMs)

    suspend fun verifyConfiguredPath(
        socksPort: Int,
        socksUser: String? = null,
        socksPassword: String? = null,
        tunEstablished: Boolean,
        hevStatsProvider: (() -> LongArray?)?,
        xrayEgressMs: () -> Long? = {
            probeXrayHttp204(SettingsManager.getHttpPort(), socksUser, socksPassword)
        },
    ): VpnPathVerification {
        val hevAlive = if (hevStatsProvider != null) waitHevAlive(hevStatsProvider) else null
        return assemblePath(
            socksPort = socksPort,
            socksUser = socksUser,
            socksPassword = socksPassword,
            tunEstablished = tunEstablished,
            hevAlive = hevAlive,
            hevRequired = hevStatsProvider != null,
            xrayEgressMs = xrayEgressMs(),
        )
    }

    fun verifyConfiguredPathBlocking(
        socksPort: Int,
        socksUser: String? = null,
        socksPassword: String? = null,
        tunEstablished: Boolean,
        hevStatsProvider: (() -> LongArray?)?,
        xrayEgressMs: () -> Long? = {
            probeXrayHttp204(SettingsManager.getHttpPort(), socksUser, socksPassword)
        },
    ): VpnPathVerification {
        val hevAlive = if (hevStatsProvider != null) waitHevAliveBlocking(hevStatsProvider) else null
        return assemblePath(
            socksPort = socksPort,
            socksUser = socksUser,
            socksPassword = socksPassword,
            tunEstablished = tunEstablished,
            hevAlive = hevAlive,
            hevRequired = hevStatsProvider != null,
            xrayEgressMs = xrayEgressMs(),
        )
    }

    private fun assemblePath(
        socksPort: Int,
        socksUser: String?,
        socksPassword: String?,
        tunEstablished: Boolean,
        hevAlive: Boolean?,
        hevRequired: Boolean,
        xrayEgressMs: Long?,
    ): VpnPathVerification {
        val socks5 = probeSocks5(socksPort, socksUser, socksPassword)
        val backend = if (hevRequired) VpnPathVerification.BACKEND_HEV else VpnPathVerification.BACKEND_XRAY_TUN
        val hevOk = if (hevRequired) hevAlive == true else true
        val verified = socks5 && tunEstablished && hevOk && xrayEgressMs != null
        val reason = when {
            !tunEstablished -> "tun-not-established"
            !socks5 -> "socks5-handshake-failed"
            hevRequired && hevAlive != true -> "hev-not-alive"
            xrayEgressMs == null -> "xray-egress-failed"
            else -> null
        }
        return VpnPathVerification(
            socks5Ready = socks5,
            hevAlive = hevAlive,
            xrayEgressMs = xrayEgressMs,
            tunEstablished = tunEstablished,
            backend = backend,
            verified = verified,
            reason = reason,
        )
    }

    /**
     * One-shot Xray health check through the local HTTP inbound.
     * Does not prove TUN capture of other apps.
     */
    fun probeXrayHttp204(httpPort: Int, username: String? = null, password: String? = null): Long? {
        if (httpPort <= 0) return null
        val client = OkHttpClient.Builder()
            .proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(AppConfig.LOOPBACK, httpPort)))
            .proxyAuthenticator { _, response ->
                if (username.isNullOrEmpty() || password.isNullOrEmpty() ||
                    response.request.header("Proxy-Authorization") != null
                ) {
                    null
                } else {
                    response.request.newBuilder()
                        .header("Proxy-Authorization", Credentials.basic(username, password))
                        .build()
                }
            }
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .callTimeout(5, TimeUnit.SECONDS)
            .followRedirects(false)
            .retryOnConnectionFailure(false)
            .build()
        for (url in egressUrls) {
            val started = nowMs()
            try {
                client.newCall(
                    Request.Builder()
                        .url(url)
                        .header("Cache-Control", "no-cache")
                        .build(),
                ).execute().use { response ->
                    if (response.code == HttpURLConnection.HTTP_NO_CONTENT) {
                        return nowMs() - started
                    }
                }
            } catch (e: Exception) {
                LogUtil.w(AppConfig.TAG, "VpnReadiness: Xray egress probe failed: ${e.javaClass.simpleName}")
            }
        }
        return null
    }

    private fun authenticateUserPass(socket: Socket, username: String, password: String): Boolean {
        val userBytes = username.toByteArray(Charsets.UTF_8)
        val passBytes = password.toByteArray(Charsets.UTF_8)
        val payload = ByteArray(3 + userBytes.size + passBytes.size)
        payload[0] = 0x01
        payload[1] = userBytes.size.toByte()
        System.arraycopy(userBytes, 0, payload, 2, userBytes.size)
        payload[2 + userBytes.size] = passBytes.size.toByte()
        System.arraycopy(passBytes, 0, payload, 3 + userBytes.size, passBytes.size)
        val out = socket.getOutputStream()
        out.write(payload)
        out.flush()
        val reply = ByteArray(2)
        if (!readFully(socket.getInputStream(), reply)) return false
        return reply[0] == 0x01.toByte() && reply[1] == 0x00.toByte()
    }

    private fun readFully(input: java.io.InputStream, header: ByteArray): Boolean {
        var offset = 0
        while (offset < header.size) {
            val n = input.read(header, offset, header.size - offset)
            if (n < 0) return false
            offset += n
        }
        return true
    }

    private fun nowMs(): Long = System.nanoTime() / 1_000_000L
}
