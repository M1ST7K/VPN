package com.v2ray.ang.vpn

/**
 * Data-plane projection of a routing snapshot.
 *
 * App split is `selectedApps` + INCLUDE/EXCLUDE at VpnService. LAN is the
 * `lanAccess` preference at TUN. Custom `RoutingRuleKind.APP` / `LAN` entries
 * are rejected at parse/sanitize because they cannot be enforced here or in
 * Xray without lying about BLOCK/DIRECT.
 */
object HotfoxRoutingDataPlane {
    data class TunEnforcement(
        val captureIpv4Default: Boolean,
        val captureIpv6Default: Boolean,
        val perApp: PerAppVpnPlan,
    )

    fun tunEnforcement(
        snapshot: RoutingPolicySnapshot,
        ipv6ProxyEnabled: Boolean,
        selfPackage: String,
    ): TunEnforcement {
        val bypassLan = snapshot.bypassLanOnTun()
        return TunEnforcement(
            captureIpv4Default = !bypassLan,
            captureIpv6Default = snapshot.ipv6TunCapturesAll(ipv6ProxyEnabled),
            perApp = snapshot.perAppPlan(selfPackage),
        )
    }

    fun xrayRules(snapshot: RoutingPolicySnapshot): List<XrayFieldRule> =
        HotfoxXrayRouting.rules(snapshot)

    fun capturedOnTun(
        snapshot: RoutingPolicySnapshot,
        packageName: String?,
        selfPackage: String = "",
    ): Boolean = !snapshot.outsideVpnCapture(packageName, selfPackage)
}
