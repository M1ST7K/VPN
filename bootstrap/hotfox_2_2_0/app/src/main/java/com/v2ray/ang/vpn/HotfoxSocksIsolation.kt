package com.v2ray.ang.vpn

import com.v2ray.ang.AppConfig
import com.v2ray.ang.util.LogUtil

/**
 * Isolation Test E: Xray through local SOCKS 10808 without treating HTTP 10809
 * as outbound proof. SOCKS HTTPS FAIL means do not blame TUN/HEV yet.
 */
object HotfoxSocksIsolation {
    data class Result(
        val socksHandshake: Boolean,
        val socksHttps: Boolean,
        val socksHttpsMs: Long?,
        val httpInbound: Boolean,
        val tunPresent: Boolean,
        val hevPresent: Boolean,
    ) {
        val outboundMayBeHealthy: Boolean get() = socksHttps
        val httpInboundOptionalFailure: Boolean get() = socksHttps && !httpInbound
        val doNotBlameTunYet: Boolean get() = !socksHttps
        val socksOnlyPathProven: Boolean get() = socksHttps && !tunPresent && !hevPresent

        fun summary(): String = buildString {
            append("socksHandshake=$socksHandshake")
            append(" socksHttps=$socksHttps")
            append(" socksHttpsMs=${socksHttpsMs ?: "none"}")
            append(" httpInbound=$httpInbound")
            append(" tunPresent=$tunPresent")
            append(" hevPresent=$hevPresent")
            append(" outboundMayBeHealthy=$outboundMayBeHealthy")
            append(" httpInboundOptionalFailure=$httpInboundOptionalFailure")
            append(" doNotBlameTunYet=$doNotBlameTunYet")
        }
    }

    @Volatile
    var last: Result? = null
        private set

    fun resetForTests() {
        last = null
    }

    fun classify(
        socksHandshake: Boolean,
        socksHttps: Boolean,
        socksHttpsMs: Long?,
        httpInbound: Boolean,
        tunPresent: Boolean,
        hevPresent: Boolean,
    ): Result = Result(
        socksHandshake = socksHandshake,
        socksHttps = socksHttps,
        socksHttpsMs = socksHttpsMs,
        httpInbound = httpInbound,
        tunPresent = tunPresent,
        hevPresent = hevPresent,
    )

    fun probe(
        socksPort: Int,
        socksUser: String? = null,
        socksPassword: String? = null,
        httpPort: Int = HotfoxInboundIsolation.HTTP_PORT,
        tunPresent: Boolean,
        hevPresent: Boolean,
    ): Result {
        val handshake = VpnReadiness.probeSocks5(socksPort, socksUser, socksPassword)
        val httpsMs = if (handshake) {
            VpnReadiness.probeSocksHttps204(socksPort, socksUser, socksPassword)
        } else {
            null
        }
        val httpReady = VpnReadiness.probeHttpProxy(httpPort)
        val result = classify(
            socksHandshake = handshake,
            socksHttps = httpsMs != null,
            socksHttpsMs = httpsMs,
            httpInbound = httpReady,
            tunPresent = tunPresent,
            hevPresent = hevPresent,
        )
        last = result
        LogUtil.i(AppConfig.TAG, "HotfoxSocksIsolation: ${result.summary()}")
        return result
    }
}
