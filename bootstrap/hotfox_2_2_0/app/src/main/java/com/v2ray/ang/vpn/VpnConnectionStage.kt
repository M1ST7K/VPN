package com.v2ray.ang.vpn

/**
 * Precise failed/current stage for diagnostics. Never inferred from a Boolean
 * "running" flag. Physical-device IP proof is a separate e2e field.
 */
enum class VpnConnectionStage(val code: String) {
    IDLE("idle"),
    RESOLVE_SERVER("server-resolve"),
    VPN_PREPARE("vpnservice-prepare"),
    TUN_ESTABLISH("tun-establish"),
    LOOP_BIND("process-bind-underlying"),
    XRAY_START("xray-start"),
    SOCKS("socks-handshake"),
    HEV("hev-start"),
    TUN_INJECT("tun-datapath"),
    HEV_PROGRESS("hev-progress"),
    XRAY_EGRESS("xray-outbound"),
    VERIFIED("verified"),
    STOPPING("stopping"),
}
