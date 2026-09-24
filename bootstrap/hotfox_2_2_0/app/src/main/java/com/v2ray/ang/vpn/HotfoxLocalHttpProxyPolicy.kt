package com.v2ray.ang.vpn

import com.v2ray.ang.handler.SettingsManager

/**
 * Subscription / control-plane HTTPS must not treat a dead local HTTP inbound
 * (10809) as a readiness mechanism. Proxy only when that inbound actually answers.
 */
object HotfoxLocalHttpProxyPolicy {
    fun resolvePort(configuredPort: Int, inboundReady: Boolean): Int {
        if (configuredPort <= 0) return 0
        return if (inboundReady) configuredPort else 0
    }

    fun httpPortIfReady(): Int {
        val port = SettingsManager.getHttpPort()
        return resolvePort(port, VpnReadiness.probeHttpProxy(port))
    }
}
