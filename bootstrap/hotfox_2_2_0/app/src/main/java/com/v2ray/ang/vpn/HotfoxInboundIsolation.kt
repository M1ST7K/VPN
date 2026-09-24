package com.v2ray.ang.vpn

/**
 * SOCKS 10808 and HTTP 10809 must be judged independently.
 * HEV uses SOCKS. HTTP inbound failure is not Xray-outbound failure.
 */
object HotfoxInboundIsolation {
    const val SOCKS_PORT = 10808
    const val HTTP_PORT = 10809

    data class ProbePair(
        val socksPass: Boolean,
        val httpPass: Boolean,
    ) {
        val outboundMayBeHealthy: Boolean get() = socksPass
        val httpInboundOptionalFailure: Boolean get() = socksPass && !httpPass
        val bothDown: Boolean get() = !socksPass && !httpPass
    }
}
