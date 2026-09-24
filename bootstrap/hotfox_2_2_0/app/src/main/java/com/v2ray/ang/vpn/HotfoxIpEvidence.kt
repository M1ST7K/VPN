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
}

/**
 * 2.9 Test E must pass before TUN/HEV is blamed. SOCKS-only HTTPS FAIL
 * is HF-VPN-014 / do-not-blame-TUN, not tun-not-forwarded.
 */
object HotfoxEngineeringE2eGate {
    fun outcome(socksOnlyHttps: Boolean, protectedCycles: Int, requestedCycles: Int): Pair<String, String> {
        if (!socksOnlyHttps) return "FAIL" to "socks-only-https"
        if (protectedCycles < requestedCycles.coerceAtLeast(1)) return "FAIL" to "not-protected"
        return "PASS" to "ok"
    }
}

