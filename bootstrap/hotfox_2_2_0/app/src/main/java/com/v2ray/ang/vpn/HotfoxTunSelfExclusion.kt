package com.v2ray.ang.vpn

/**
 * Keeps the HotFox/Xray UID off TUN so Go-created outbound sockets cannot
 * recurse into TUN → HEV → SOCKS → Xray.
 *
 * Datapath proof must use [android.net.NetworkCapabilities.TRANSPORT_VPN]
 * bindSocket (`VpnReadiness.injectThroughVpn`), never process-direct HTTPS
 * from the excluded VPN UID.
 *
 * Android allows either an allow-list or a disallow-list, not both.
 */
object HotfoxTunSelfExclusion {
    data class BuilderOps(
        val useAllowList: Boolean,
        val allow: Set<String>,
        val disallow: Set<String>,
    )

    fun forPlan(plan: PerAppVpnPlan, selfPackage: String): BuilderOps {
        val self = selfPackage.trim()
        require(self.isNotBlank()) { "self package is required" }
        val selected = plan.packages.filter { it.isNotBlank() && it != self }.toSet()
        return if (!plan.enabled) {
            BuilderOps(useAllowList = false, allow = emptySet(), disallow = setOf(self))
        } else if (plan.bypassSelected) {
            BuilderOps(useAllowList = false, allow = emptySet(), disallow = selected + self)
        } else {
            BuilderOps(useAllowList = true, allow = selected, disallow = emptySet())
        }
    }

    fun capturesPackage(ops: BuilderOps, packageName: String): Boolean {
        if (packageName.isBlank()) return !ops.useAllowList
        return if (ops.useAllowList) packageName in ops.allow else packageName !in ops.disallow
    }
}
