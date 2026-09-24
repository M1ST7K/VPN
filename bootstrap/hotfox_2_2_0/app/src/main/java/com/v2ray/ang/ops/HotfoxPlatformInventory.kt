package com.v2ray.ang.ops

/**
 * 3.1 privacy + security architecture inventory. Collection stays limited to
 * what operations and entitlement actually need. This is reviewable code, not
 * a marketing claim.
 */
object HotfoxPlatformInventory {
    val localClientData = listOf(
        "AUTO/manual selection intent",
        "last-good AUTO guid",
        "routing mode / LAN / ads / selected apps",
        "Keystore-backed entitlement credential",
        "generated scoped device id",
        "hashed opaque trusted-Wi-Fi ids",
        "last-known-good signed drain/control-plane cache",
    )

    val backendData = listOf(
        "entitlement status and expiry",
        "order/payment identifiers under backend control",
        "authorized generated device ids",
        "signed inventory/capacity/maintenance metadata",
        "signed update manifests",
    )

    val neverCollect = listOf(
        "hardware Android ID / IMEI / serial / MAC as identity",
        "browsing content or destination URLs",
        "VPN secrets, VLESS UUIDs, subscription URLs in telemetry",
        "checkout return success=true as payment proof",
        "account password sync of VPN credentials",
    )

    val remoteConfigForbidden = listOf(
        "vpn.skip_tls",
        "vpn.disable_reality",
        "vpn.allow_insecure",
        "vpn.disable_dns_protection",
        "vpn.disable_ipv6_fail_closed",
        "vpn.fake_connected",
        "vpn.skip_path_verify",
        "commerce.trust_checkout",
    )

    val securityBoundaries = listOf(
        "ECDSA P-256 signatures for drain, flags, updates, control plane",
        "invalid signature fails closed to last-known-good",
        "VpnSessionCoordinator is the only CONNECTED publisher",
        "UI observes HotfoxEngineFacade; Activity is not a session owner",
        "device limit/revoke uses generated ids only",
        "API minClientProtocol rejects incompatible clients without wiping cache",
    )
}
