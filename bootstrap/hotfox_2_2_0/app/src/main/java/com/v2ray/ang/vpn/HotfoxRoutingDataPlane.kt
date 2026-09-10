package com.v2ray.ang.vpn

/**
 * Data-plane projection of a routing snapshot.
 *
 * App split is `selectedApps` + INCLUDE/EXCLUDE at VpnService. LAN is the
 * `lanAccess` preference at the IPv4 TUN layer. IPv6 always uses fail-closed
 * `::/0` so NAT64/global prefixes cannot bypass TUN. Custom `RoutingRuleKind.APP`
 * / `LAN` entries are rejected at parse/sanitize because they cannot be enforced
 * here or in Xray without lying about BLOCK/DIRECT.
 *
 * In INCLUDE/EXCLUDE, Xray field rules are BLOCK-only so they cannot override
 * the app-capture decision with DOMAIN/CIDR DIRECT.
 */
object HotfoxRoutingDataPlane {
    data class Ipv6TunRoute(val address: String, val prefix: Int)

    data class TunEnforcement(
        val captureIpv4Default: Boolean,
        val captureIpv6Default: Boolean,
        val ipv6CaptureRoutes: List<Ipv6TunRoute>,
        val perApp: PerAppVpnPlan,
    ) {
        /**
         * True when Android would send [ip] into TUN given [ipv6CaptureRoutes].
         * `::/0` captures NAT64 (`64:ff9b::/96`) and every other IPv6 prefix.
         */
        fun capturesIpv6Destination(ip: String): Boolean {
            if (!ip.contains(':')) return false
            return ipv6CaptureRoutes.any { route -> ipv6RouteContains(route, ip) }
        }
    }

    val IPV6_FAIL_CLOSED_ROUTES: List<Ipv6TunRoute> = listOf(Ipv6TunRoute("::", 0))

    fun tunEnforcement(
        snapshot: RoutingPolicySnapshot,
        ipv6ProxyEnabled: Boolean,
        selfPackage: String,
    ): TunEnforcement {
        val bypassLan = snapshot.bypassLanOnTun()
        return TunEnforcement(
            captureIpv4Default = !bypassLan,
            captureIpv6Default = snapshot.ipv6TunCapturesAll(ipv6ProxyEnabled),
            ipv6CaptureRoutes = IPV6_FAIL_CLOSED_ROUTES,
            perApp = snapshot.perAppPlan(selfPackage),
        )
    }

    private fun ipv6RouteContains(route: Ipv6TunRoute, ip: String): Boolean {
        if (route.prefix == 0) return true
        val parsed = CidrRouting.parse("${route.address}/${route.prefix}") ?: return false
        return CidrRouting.contains(parsed, ip)
    }

    fun xrayRules(snapshot: RoutingPolicySnapshot): List<XrayFieldRule> =
        HotfoxXrayRouting.rules(snapshot)

    fun capturedOnTun(
        snapshot: RoutingPolicySnapshot,
        packageName: String?,
        selfPackage: String = "",
    ): Boolean = !snapshot.outsideVpnCapture(packageName, selfPackage)
}
