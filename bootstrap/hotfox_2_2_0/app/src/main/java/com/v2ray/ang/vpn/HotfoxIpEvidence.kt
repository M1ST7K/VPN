package com.v2ray.ang.vpn

/**
 * Public-IP evidence for engineering-runtime E2E. Last octet / trailing
 * IPv6 hextets are redacted so reports never look like credentials.
 */
object HotfoxIpEvidence {
    fun redact(raw: String?): String {
        val ip = raw?.trim().orEmpty()
        if (ip.isEmpty()) return "none"
        Regex("""^(\d{1,3}(?:\.\d{1,3}){2})\.\d{1,3}$""").matchEntire(ip)?.let {
            return it.groupValues[1] + ".x"
        }
        if (ip.contains(':')) {
            val parts = ip.split(':').filter { it.isNotEmpty() }
            if (parts.size >= 2) return parts.take(3).joinToString(":") + ":x"
        }
        return "redacted"
    }

    fun changed(before: String?, during: String?): Boolean {
        val left = before?.trim().orEmpty()
        val right = during?.trim().orEmpty()
        if (left.isEmpty() || right.isEmpty()) return false
        return left != right
    }

    /**
     * After disconnect, the direct path must be reachable and must no longer
     * expose the VPN egress observed through the TUN path. Requiring an exact
     * before==after match would be brittle on networks with rotating NAT IPs.
     */
    fun leftVpnEgress(during: String?, after: String?): Boolean {
        val vpn = during?.trim().orEmpty()
        val direct = after?.trim().orEmpty()
        if (vpn.isEmpty() || direct.isEmpty()) return false
        return vpn != direct
    }
}

/**
 * Release engineering E2E gate.
 *
 * A PASS requires evidence from the actual VPN/TUN path, not merely a working
 * local SOCKS proxy or a CONNECTED state. DNS/IPv6 leak acceptance remains a
 * separate final-release gate because availability is environment-dependent.
 */
object HotfoxEngineeringE2eGate {
    fun outcome(
        socksOnlyHttps: Boolean,
        protectedCycles: Int,
        requestedCycles: Int,
        tunHttp4Cycles: Int,
        ipChangedThroughTun: Boolean,
        leftVpnEgressAfterDisconnect: Boolean,
    ): Pair<String, String> {
        val required = requestedCycles.coerceAtLeast(1)
        if (!socksOnlyHttps) return "FAIL" to "socks-only-https"
        if (protectedCycles < required) return "FAIL" to "not-protected"
        if (tunHttp4Cycles < required) return "FAIL" to "tun-http4"
        if (!ipChangedThroughTun) return "FAIL" to "tun-public-ip-unchanged"
        if (!leftVpnEgressAfterDisconnect) return "FAIL" to "disconnect-egress-not-restored"
        return "PASS" to "ok"
    }
}
