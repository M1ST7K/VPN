package com.v2ray.ang.vpn

/**
 * HEV/tun2socks must target the same local SOCKS inbound Xray actually listens on.
 * A mismatch is a datapath bug, not a remote-node failure.
 */
data class HotfoxHevSocksTarget(
    val host: String,
    val port: Int,
    val udp: String,
    val mtu: Int,
) {
    fun matchesXraySocks(inboundPort: Int?, inboundListen: String): Boolean {
        if (inboundPort == null) return true
        if (port != inboundPort) return false
        if (inboundListen.isBlank()) return true
        return inboundListen == host || inboundListen == "127.0.0.1" || inboundListen == "::1"
    }

    fun summary(): String = "hevSocks=$host:$port udp=$udp mtu=$mtu"
}

object HotfoxDatapathContract {
    fun assertHevMatchesXray(hev: HotfoxHevSocksTarget, inboundPort: Int?, inboundListen: String): Boolean {
        return hev.matchesXraySocks(inboundPort, inboundListen)
    }
}
