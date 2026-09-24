package com.v2ray.ang.service

import android.content.Context
import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.MessageUtil
import com.v2ray.ang.vpn.VpnReadiness
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Checks that Xray can reach the Internet through local SOCKS 10808.
 *
 * HTTP inbound 10809 is not this probe. A successful result means the Xray
 * outbound may be healthy; it is not TUN E2E proof. Repeated SOCKS HTTPS
 * failures may notify AUTO failover. This class does not start VpnService.
 */
class HotfoxHealthMonitor(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    fun start() {
        start(onRepeatedCoreFailure = null)
    }

    fun start(onRepeatedCoreFailure: (() -> Unit)?) {
        job?.cancel()
        var consecutiveFailures = 0
        var failoverSignaled = false
        job = scope.launch {
            delay(800)
            while (isActive) {
                val elapsed = probeSocksOutbound()
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

    private fun probeSocksOutbound(): Long? {
        val elapsed = VpnReadiness.probeSocksHttps204(
            SettingsManager.getSocksPort(),
            SettingsManager.getSocksUsername(),
            SettingsManager.getSocksPassword(),
        )
        if (elapsed == null) {
            LogUtil.w(AppConfig.TAG, "HotFox health: SOCKS outbound HTTPS probe failed")
        }
        return elapsed
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun close() {
        scope.cancel()
    }
}
