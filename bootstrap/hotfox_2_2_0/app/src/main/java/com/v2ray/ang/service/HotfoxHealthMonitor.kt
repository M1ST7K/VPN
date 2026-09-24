package com.v2ray.ang.service

import android.content.Context
import android.os.SystemClock
import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.MessageUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

/**
 * Checks that Xray itself can reach the Internet through its local HTTP proxy.
 *
 * A successful result means "Xray/outbound is healthy"; it is not TUN E2E proof.
 * Repeated core failures may notify AUTO failover. This class does not start VpnService.
 */
class HotfoxHealthMonitor(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    private val probeUrls = listOf(
        "https://www.gstatic.com/generate_204",
        "https://cp.cloudflare.com/generate_204",
    )

    fun start() {
        start(onRepeatedCoreFailure = null)
    }

    fun start(onRepeatedCoreFailure: (() -> Unit)?) {
        job?.cancel()
        var consecutiveFailures = 0
        var failoverSignaled = false
        job = scope.launch {
            val client = buildProxyClient()
            delay(800)
            while (isActive) {
                val elapsed = probe(client)
                if (isActive) {
                    val status = elapsed?.let { "core-ok:$it" } ?: "core-failed"
                    MessageUtil.sendMsg2UI(context, AppConfig.MSG_HOTFOX_HEALTH, status)
                    if (elapsed == null) {
                        consecutiveFailures += 1
                        if (consecutiveFailures >= 2 && !failoverSignaled) {
                            failoverSignaled = true
                            onRepeatedCoreFailure?.invoke()
                        }
                    } else {
                        consecutiveFailures = 0
                    }
                }
                delay(60_000)
            }
        }
    }

    private fun buildProxyClient(): OkHttpClient = OkHttpClient.Builder()
        .proxy(
            Proxy(
                Proxy.Type.HTTP,
                InetSocketAddress(AppConfig.LOOPBACK, SettingsManager.getHttpPort()),
            ),
        )
        .proxyAuthenticator { _, response ->
            val user = SettingsManager.getSocksUsername()
            val password = SettingsManager.getSocksPassword()
            if (user.isNullOrEmpty() || password.isNullOrEmpty() || response.request.header("Proxy-Authorization") != null) {
                null
            } else {
                response.request.newBuilder()
                    .header("Proxy-Authorization", Credentials.basic(user, password))
                    .build()
            }
        }
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .callTimeout(8, TimeUnit.SECONDS)
        .followRedirects(false)
        .retryOnConnectionFailure(false)
        .build()

    private fun probe(client: OkHttpClient): Long? {
        for (url in probeUrls) {
            val started = SystemClock.elapsedRealtime()
            try {
                client.newCall(
                    Request.Builder()
                        .url(url)
                        .header("Cache-Control", "no-cache")
                        .build(),
                ).execute().use { response ->
                    if (response.code == HttpURLConnection.HTTP_NO_CONTENT) {
                        return SystemClock.elapsedRealtime() - started
                    }
                }
            } catch (e: Exception) {
                LogUtil.w(AppConfig.TAG, "HotFox health: Xray proxy probe failed: ${e.javaClass.simpleName}")
            }
        }
        return null
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun close() {
        scope.cancel()
    }
}
