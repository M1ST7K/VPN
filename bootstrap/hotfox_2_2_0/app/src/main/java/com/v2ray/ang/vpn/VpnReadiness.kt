package com.v2ray.ang.vpn

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.util.LogUtil
import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Bounded operational checks for the configured production path.
 *
 * SOCKS5 handshake proves the local inbound speaks SOCKS, not merely that a TCP
 * listener accepted a connection. HEV liveness is JNI stats shape only — not TUN
 * proof. TUN proof requires a bound inject through [android.net.NetworkCapabilities.TRANSPORT_VPN]
 * plus observed counter progress on the selected backend. Xray HTTP-204 is an Xray
 * component check, not TUN capture of third-party apps. Physical-device IP/DNS/IPv6
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

    /** True when any HEV/xray-tun counter strictly increased, including a reset-on-read window. */
    fun countersAdvanced(before: LongArray?, after: LongArray?): Boolean {
        if (!hevStatsAlive(before) || !hevStatsAlive(after)) return false
        val prior = before!!
        val next = after!!
        if ((0 until 4).any { next[it] > prior[it] }) return true
        val afterSum = next.sum()
        val beforeSum = prior.sum()
        return afterSum > 0L && afterSum < beforeSum && !next.contentEquals(prior)
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

    fun waitBackendProgressBlocking(
        snapshot: () -> LongArray?,
        inject: () -> Boolean,
        timeoutMillis: Long = 4_000L,
        sampleDelayMs: Long = 80L,
    ): Boolean {
        val before = snapshot()?.copyOf() ?: return false
        if (!hevStatsAlive(before)) return false
        if (!inject()) return false
        val deadline = nowMs() + timeoutMillis
        while (nowMs() < deadline) {
            if (countersAdvanced(before, snapshot())) return true
            try {
                Thread.sleep(sampleDelayMs)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return false
            }
        }
        return false
    }

    /**
     * Sends UDP DNS and TCP through the Android VPN network so packets enter TUN.
     * Does not use the local HTTP inbound. Fail-closed if no VPN network can be bound.
     */
    fun injectThroughVpn(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val vpn = findVpnNetwork(cm) ?: run {
            LogUtil.w(AppConfig.TAG, "VpnReadiness: no TRANSPORT_VPN network to bind")
            return false
        }
        val udp = injectUdpDns(vpn)
        val tcp = injectTcp(vpn)
        if (!udp && !tcp) {
            LogUtil.w(AppConfig.TAG, "VpnReadiness: TUN inject bind/send failed")
            return false
        }
        return true
    }

    fun findVpnNetwork(cm: ConnectivityManager): Network? {
        val networks = runCatching { cm.allNetworks }.getOrDefault(emptyArray())
        for (network in networks) {
            val caps = cm.getNetworkCapabilities(network) ?: continue
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return network
        }
        val active = cm.activeNetwork ?: return null
        val caps = cm.getNetworkCapabilities(active) ?: return null
        return active.takeIf { caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) }
    }

    suspend fun verifyConfiguredPath(
        socksPort: Int,
        socksUser: String? = null,
        socksPassword: String? = null,
        tunEstablished: Boolean,
        hevStatsProvider: (() -> LongArray?)?,
        xrayEgressMs: () -> Long? = {
            probeXrayHttp204(SettingsManager.getHttpPort(), socksUser, socksPassword)
        },
        tunInjector: (() -> Boolean)? = null,
        xrayTunSnapshot: (() -> LongArray?)? = null,
    ): VpnPathVerification {
        val hevAlive = if (hevStatsProvider != null) waitHevAlive(hevStatsProvider) else null
        val progressed = measureTunProgress(
            hevRequired = hevStatsProvider != null,
            hevStatsProvider = hevStatsProvider,
            tunInjector = tunInjector,
            xrayTunSnapshot = xrayTunSnapshot,
        )
        return assemblePath(
            socksPort = socksPort,
            socksUser = socksUser,
            socksPassword = socksPassword,
            tunEstablished = tunEstablished,
            hevAlive = hevAlive,
            hevRequired = hevStatsProvider != null,
            hevProgressed = progressed,
            tunForwarded = progressed,
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
        tunInjector: (() -> Boolean)? = null,
        xrayTunSnapshot: (() -> LongArray?)? = null,
    ): VpnPathVerification {
        val hevAlive = if (hevStatsProvider != null) waitHevAliveBlocking(hevStatsProvider) else null
        val progressed = measureTunProgress(
            hevRequired = hevStatsProvider != null,
            hevStatsProvider = hevStatsProvider,
            tunInjector = tunInjector,
            xrayTunSnapshot = xrayTunSnapshot,
        )
        return assemblePath(
            socksPort = socksPort,
            socksUser = socksUser,
            socksPassword = socksPassword,
            tunEstablished = tunEstablished,
            hevAlive = hevAlive,
            hevRequired = hevStatsProvider != null,
            hevProgressed = progressed,
            tunForwarded = progressed,
            xrayEgressMs = xrayEgressMs(),
        )
    }

    private fun measureTunProgress(
        hevRequired: Boolean,
        hevStatsProvider: (() -> LongArray?)?,
        tunInjector: (() -> Boolean)?,
        xrayTunSnapshot: (() -> LongArray?)?,
    ): Boolean? {
        val snapshot: (() -> LongArray?)? = when {
            hevRequired -> hevStatsProvider
            xrayTunSnapshot != null -> xrayTunSnapshot
            else -> null
        }
        if (snapshot == null || tunInjector == null) return false
        if (!hevRequired) {
            runCatching { snapshot.invoke() }
        }
        return waitBackendProgressBlocking(snapshot, tunInjector)
    }

    private fun assemblePath(
        socksPort: Int,
        socksUser: String?,
        socksPassword: String?,
        tunEstablished: Boolean,
        hevAlive: Boolean?,
        hevRequired: Boolean,
        hevProgressed: Boolean?,
        tunForwarded: Boolean?,
        xrayEgressMs: Long?,
    ): VpnPathVerification {
        val socks5 = probeSocks5(socksPort, socksUser, socksPassword)
        val backend = if (hevRequired) VpnPathVerification.BACKEND_HEV else VpnPathVerification.BACKEND_XRAY_TUN
        val hevOk = if (hevRequired) hevAlive == true else true
        val tunOk = tunForwarded == true
        val verified = socks5 && tunEstablished && hevOk && tunOk && xrayEgressMs != null
        val reason = when {
            !tunEstablished -> "tun-not-established"
            !socks5 -> "socks5-handshake-failed"
            hevRequired && hevAlive != true -> "hev-not-alive"
            !tunOk -> "tun-not-forwarded"
            xrayEgressMs == null -> "xray-egress-failed"
            else -> null
        }
        return VpnPathVerification(
            socks5Ready = socks5,
            hevAlive = hevAlive,
            hevProgressed = if (hevRequired) hevProgressed else null,
            tunForwarded = tunForwarded,
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

    private fun injectUdpDns(vpn: Network): Boolean {
        return try {
            DatagramSocket().use { socket ->
                vpn.bindSocket(socket)
                socket.soTimeout = 400
                val payload = dnsQuery("one.one.one.one")
                val packet = DatagramPacket(
                    payload,
                    payload.size,
                    InetAddress.getByName("1.1.1.1"),
                    53,
                )
                socket.send(packet)
                runCatching {
                    val buf = ByteArray(512)
                    socket.receive(DatagramPacket(buf, buf.size))
                }
                true
            }
        } catch (e: Exception) {
            LogUtil.w(AppConfig.TAG, "VpnReadiness: UDP TUN inject failed: ${e.javaClass.simpleName}")
            false
        }
    }

    private fun injectTcp(vpn: Network): Boolean {
        return try {
            Socket().use { socket ->
                vpn.bindSocket(socket)
                socket.soTimeout = 800
                socket.connect(InetSocketAddress("1.1.1.1", 443), 1_200)
                runCatching { socket.getOutputStream().write(0x16) }
                true
            }
        } catch (e: Exception) {
            LogUtil.w(AppConfig.TAG, "VpnReadiness: TCP TUN inject failed: ${e.javaClass.simpleName}")
            false
        }
    }

    private fun dnsQuery(host: String): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(byteArrayOf(0x12, 0x34, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00))
        for (label in host.split('.')) {
            val bytes = label.toByteArray(Charsets.US_ASCII)
            out.write(bytes.size)
            out.write(bytes)
        }
        out.write(0)
        out.write(byteArrayOf(0x00, 0x01, 0x00, 0x01))
        return out.toByteArray()
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
