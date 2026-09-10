package com.v2ray.ang.vpn

/**
 * 2.5 Privacy Controls / Smart Routing.
 *
 * Traffic that Android leaves outside TUN (EXCLUDE selected apps, INCLUDE miss)
 * is DIRECT at the VpnService layer, including ads/trackers. BLOCK/domain/CIDR
 * apply only to captured traffic.
 *
 * Precedence for captured traffic (deterministic, never hash-map order):
 * `BLOCK > APP-SPECIFIC > DOMAIN-SPECIFIC > CIDR > GLOBAL MODE`
 *
 * LAN is an explicit user policy, not a silent private-range bypass.
 * Malformed CIDR never becomes `0.0.0.0/0` or `::/0` DIRECT.
 */
enum class HotfoxRoutingMode {
    SMART,
    GLOBAL,
    INCLUDE_APPS,
    EXCLUDE_APPS,
    CUSTOM,
    ;

    val storageValue: String
        get() = when (this) {
            SMART -> "smart"
            GLOBAL -> "global"
            INCLUDE_APPS -> "include_apps"
            EXCLUDE_APPS -> "exclude_apps"
            CUSTOM -> "custom"
        }

    companion object {
        fun fromStorage(raw: String?): HotfoxRoutingMode = when (raw?.lowercase()) {
            "global" -> GLOBAL
            "include_apps", "selected" -> INCLUDE_APPS
            "exclude_apps", "bypass" -> EXCLUDE_APPS
            "custom" -> CUSTOM
            else -> SMART
        }
    }
}

enum class RouteAction { VPN, DIRECT, BLOCK }

enum class RoutingRuleKind { APP, DOMAIN_EXACT, DOMAIN_SUFFIX, CIDR, LAN }

data class RoutingRule(
    val id: String,
    val kind: RoutingRuleKind,
    val value: String,
    val action: RouteAction,
)

data class RoutingQuery(
    val packageName: String? = null,
    val domain: String? = null,
    val ip: String? = null,
    val lan: Boolean = false,
)

data class RoutingDecision(
    val action: RouteAction,
    val reason: String,
)

data class PerAppVpnPlan(
    val enabled: Boolean,
    val bypassSelected: Boolean,
    val packages: Set<String>,
)

object DomainRouting {
    fun normalize(raw: String): String? {
        var value = raw.trim().lowercase().trim('.')
        if (value.startsWith("*.")) {
            value = value.removePrefix("*.")
        }
        if (value.isBlank() || value.contains(' ') || value.contains('/')) return null
        if (value.any { it.code < 32 }) return null
        val ascii = runCatching {
            java.net.IDN.toASCII(value, java.net.IDN.ALLOW_UNASSIGNED)
        }.getOrNull()?.lowercase()?.trim('.') ?: return null
        if (ascii.isBlank() || ascii.contains(' ') || ascii.contains('/')) return null
        return ascii
    }

    fun matchesExact(rule: String, host: String): Boolean = rule == host

    fun matchesSuffix(rule: String, host: String): Boolean {
        if (host == rule) return true
        return host.endsWith(".$rule")
    }
}

object CidrRouting {
    data class Parsed(val network: ByteArray, val prefix: Int, val ipv6: Boolean)

    fun parse(raw: String): Parsed? {
        val trimmed = raw.trim()
        val slash = trimmed.indexOf('/')
        if (slash <= 0 || slash == trimmed.lastIndex) return null
        val addr = trimmed.substring(0, slash)
        val prefix = trimmed.substring(slash + 1).toIntOrNull() ?: return null
        val ipv6 = addr.contains(':')
        val bytes = (if (ipv6) ipv6Bytes(addr) else ipv4Bytes(addr)) ?: return null
        val max = if (ipv6) 128 else 32
        if (prefix < 0 || prefix > max) return null
        if (!ipv6 && prefix == 0) return null
        if (ipv6 && prefix == 0) return null
        return Parsed(bytes, prefix, ipv6)
    }

    fun contains(parsed: Parsed, ip: String): Boolean {
        val target = (if (parsed.ipv6) ipv6Bytes(ip) else ipv4Bytes(ip)) ?: return false
        if (target.size != parsed.network.size) return false
        var remaining = parsed.prefix
        var i = 0
        while (remaining > 0 && i < target.size) {
            val bits = minOf(8, remaining)
            val mask = (0xFF shl (8 - bits)) and 0xFF
            if ((target[i].toInt() and mask) != (parsed.network[i].toInt() and mask)) return false
            remaining -= bits
            i++
        }
        return true
    }

    fun isLanIpv4(ip: String): Boolean {
        val b = ipv4Bytes(ip) ?: return false
        val a = b[0].toInt() and 0xFF
        val c = b[1].toInt() and 0xFF
        if (a == 10) return true
        if (a == 192 && c == 168) return true
        if (a == 172 && c in 16..31) return true
        if (a == 127) return true
        if (a == 169 && c == 254) return true
        return false
    }

    fun isLanIpv6(ip: String): Boolean {
        val b = ipv6Bytes(ip) ?: return false
        if (b.size != 16) return false
        val loopback = b.slice(0 until 15).all { it.toInt() == 0 } && (b[15].toInt() and 0xFF) == 1
        if (loopback) return true
        val b0 = b[0].toInt() and 0xFF
        val b1 = b[1].toInt() and 0xFF
        if (b0 == 0xFE && (b1 and 0xC0) == 0x80) return true
        if ((b0 and 0xFE) == 0xFC) return true
        return false
    }

    private fun ipv4Bytes(addr: String): ByteArray? {
        val parts = addr.split('.')
        if (parts.size != 4) return null
        val out = ByteArray(4)
        parts.forEachIndexed { index, part ->
            val n = part.toIntOrNull() ?: return null
            if (n !in 0..255) return null
            out[index] = n.toByte()
        }
        return out
    }

    private fun ipv6Bytes(addr: String): ByteArray? {
        if (!addr.contains(':')) return null
        val halves = addr.split("::", limit = 2)
        if (halves.size > 2) return null
        fun groups(side: String): List<Int>? {
            if (side.isEmpty()) return emptyList()
            return side.split(':').map { token ->
                if (token.isEmpty() || token.length > 4) return null
                token.toIntOrNull(16) ?: return null
            }
        }
        val head = groups(halves[0]) ?: return null
        val tail = if (halves.size == 2) groups(halves[1]) ?: return null else emptyList()
        val missing = 8 - head.size - tail.size
        if (halves.size == 2) {
            if (missing < 0) return null
        } else if (head.size != 8) {
            return null
        }
        val words = ArrayList<Int>(8)
        words.addAll(head)
        repeat(if (halves.size == 2) missing else 0) { words.add(0) }
        words.addAll(tail)
        if (words.size != 8) return null
        val out = ByteArray(16)
        words.forEachIndexed { i, w ->
            out[i * 2] = ((w shr 8) and 0xFF).toByte()
            out[i * 2 + 1] = (w and 0xFF).toByte()
        }
        return out
    }
}

data class RoutingPolicySnapshot(
    val mode: HotfoxRoutingMode,
    val selectedApps: Set<String>,
    val rules: List<RoutingRule>,
    val lanAccess: Boolean,
    val adsBlocked: Boolean,
    val dnsThroughVpn: Boolean = true,
) {
    fun perAppPlan(selfPackage: String): PerAppVpnPlan {
        val installed = selectedApps.filter { it.isNotBlank() && it != selfPackage }.toSet()
        return when (mode) {
            HotfoxRoutingMode.INCLUDE_APPS -> PerAppVpnPlan(
                enabled = installed.isNotEmpty(),
                bypassSelected = false,
                packages = installed,
            )
            HotfoxRoutingMode.EXCLUDE_APPS -> PerAppVpnPlan(
                enabled = installed.isNotEmpty(),
                bypassSelected = true,
                packages = installed,
            )
            else -> PerAppVpnPlan(enabled = false, bypassSelected = false, packages = emptySet())
        }
    }

    /**
     * True when [packageName] is intentionally left off TUN. That traffic never
     * reaches Xray, so BLOCK/ads/domain/CIDR cannot apply to it.
     */
    fun outsideVpnCapture(packageName: String?, selfPackage: String = ""): Boolean {
        if (packageName.isNullOrBlank()) return false
        val plan = perAppPlan(selfPackage)
        if (!plan.enabled) return false
        return if (plan.bypassSelected) {
            packageName in plan.packages
        } else {
            packageName !in plan.packages
        }
    }

    fun bypassLanOnTun(): Boolean {
        if (mode == HotfoxRoutingMode.GLOBAL) return false
        return lanAccess
    }

    /**
     * Default DNS policy for 2.5: queries are captured on TUN and resolved
     * through the protected path. LAN bypass never implies system-DNS leak.
     */
    fun dnsPolicy(): DnsPolicy = DnsPolicy.THROUGH_VPN

    /**
     * INCLUDE/EXCLUDE capture remaining apps as a whole at VpnService.
     * After TUN, Xray has no package identity, so non-block DOMAIN/CIDR field
     * rules cannot preserve `APP > DOMAIN > CIDR` and must not be emitted.
     */
    fun funnelsCapturedTrafficByApp(): Boolean =
        mode == HotfoxRoutingMode.INCLUDE_APPS || mode == HotfoxRoutingMode.EXCLUDE_APPS

    /**
     * IPv6 Internet is always captured with `::/0`.
     *
     * LAN bypass is IPv4-only at the TUN layer. A partial IPv6 capture
     * (`2000::/3` + `fc00::/18`) omits NAT64 (`64:ff9b::/96`) and any
     * operator-specific prefix outside those ranges, which would leak
     * IPv6-only traffic off TUN while the session can still reach CONNECTED.
     */
    @Suppress("UNUSED_PARAMETER")
    fun ipv6TunCapturesAll(ipv6ProxyEnabled: Boolean): Boolean = true

    fun uiLabel(): String = when (mode) {
        HotfoxRoutingMode.SMART -> "Весь трафик · Smart"
        HotfoxRoutingMode.GLOBAL -> "Весь трафик"
        HotfoxRoutingMode.INCLUDE_APPS ->
            if (selectedApps.isEmpty()) "Только выбранные приложения" else "${selectedApps.size} приложения через VPN"
        HotfoxRoutingMode.EXCLUDE_APPS ->
            if (selectedApps.isEmpty()) "Исключить приложения" else "${selectedApps.size} приложения напрямую"
        HotfoxRoutingMode.CUSTOM -> "Мои правила"
    }
}

enum class DnsPolicy {
    THROUGH_VPN,
}

object HotfoxRoutingPolicy {
    const val PREF_MODE = "pref_hotfox_smart_routing_mode"
    const val PREF_LAN = "pref_hotfox_lan_access"
    const val PREF_RULES = "pref_hotfox_routing_rules_json"
    const val PREF_DNS_VPN = "pref_hotfox_dns_through_vpn"

    data class StoredMode(
        val mode: HotfoxRoutingMode,
        val persistCanonical: Boolean,
    )

    /**
     * Canonical key wins. If it is empty, migrate the 2.1 overlay key
     * (`AppConfig.PREF_SMART_ROUTING_MODE` / selected/bypass aliases).
     */
    fun resolveStoredMode(canonical: String?, legacy: String?): StoredMode {
        val canonicalValue = canonical?.takeIf { it.isNotBlank() }
        if (canonicalValue != null) {
            return StoredMode(HotfoxRoutingMode.fromStorage(canonicalValue), persistCanonical = false)
        }
        val legacyValue = legacy?.takeIf { it.isNotBlank() }
        if (legacyValue != null) {
            return StoredMode(HotfoxRoutingMode.fromStorage(legacyValue), persistCanonical = true)
        }
        return StoredMode(HotfoxRoutingMode.SMART, persistCanonical = false)
    }

    fun decide(snapshot: RoutingPolicySnapshot, query: RoutingQuery): RoutingDecision {
        if (snapshot.outsideVpnCapture(query.packageName)) {
            val reason = when (snapshot.mode) {
                HotfoxRoutingMode.INCLUDE_APPS -> "include_apps_miss"
                HotfoxRoutingMode.EXCLUDE_APPS -> "exclude_apps"
                else -> "outside_tun"
            }
            return RoutingDecision(RouteAction.DIRECT, reason)
        }
        val blocked = matching(snapshot.rules, query, RouteAction.BLOCK)
        if (blocked != null) {
            return RoutingDecision(RouteAction.BLOCK, "block:${blocked.kind}:${blocked.id}")
        }
        if (snapshot.funnelsCapturedTrafficByApp()) {
            if (!query.packageName.isNullOrBlank()) {
                val reason = when (snapshot.mode) {
                    HotfoxRoutingMode.INCLUDE_APPS -> "include_apps"
                    else -> "exclude_apps_rest"
                }
                return RoutingDecision(RouteAction.VPN, reason)
            }
            // No package identity: DOMAIN/CIDR DIRECT would disagree with the
            // captured-app VPN path that Xray can actually enforce.
        } else {
            val domainHit = matchingDomain(snapshot.rules, query.domain)
            if (domainHit != null) {
                return RoutingDecision(domainHit.action, "domain:${domainHit.kind}:${domainHit.id}")
            }
            val cidrHit = matchingCidr(snapshot.rules, query.ip)
            if (cidrHit != null) {
                return RoutingDecision(cidrHit.action, "cidr:${cidrHit.id}")
            }
        }
        val lanHit = query.lan ||
            query.ip?.let { CidrRouting.isLanIpv4(it) || CidrRouting.isLanIpv6(it) } == true
        if (lanHit) {
            if (snapshot.bypassLanOnTun()) {
                return RoutingDecision(RouteAction.DIRECT, "lan_permit")
            }
            return RoutingDecision(RouteAction.VPN, "lan_captured")
        }
        return when (snapshot.mode) {
            HotfoxRoutingMode.GLOBAL, HotfoxRoutingMode.SMART,
            HotfoxRoutingMode.INCLUDE_APPS, HotfoxRoutingMode.EXCLUDE_APPS,
            HotfoxRoutingMode.CUSTOM,
            -> RoutingDecision(RouteAction.VPN, "default:${snapshot.mode.storageValue}")
        }
    }

    fun sanitizeRule(rule: RoutingRule): RoutingRule? {
        return when (rule.kind) {
            RoutingRuleKind.DOMAIN_EXACT, RoutingRuleKind.DOMAIN_SUFFIX -> {
                val host = DomainRouting.normalize(rule.value) ?: return null
                rule.copy(value = host)
            }
            RoutingRuleKind.CIDR -> {
                val parsed = CidrRouting.parse(rule.value) ?: return null
                if (rule.action == RouteAction.DIRECT && parsed.prefix == 0) return null
                rule.copy(value = rule.value.trim())
            }
            // APP/LAN are VpnService-layer via selectedApps + lanAccess.
            // Xray cannot match Android packages; a LAN rule would fight lanAccess.
            RoutingRuleKind.APP, RoutingRuleKind.LAN -> null
        }
    }

    private fun matching(
        rules: List<RoutingRule>,
        query: RoutingQuery,
        action: RouteAction,
    ): RoutingRule? {
        rules.filter { it.action == action }.forEach { rule ->
            when (rule.kind) {
                RoutingRuleKind.APP, RoutingRuleKind.LAN -> return@forEach
                RoutingRuleKind.DOMAIN_EXACT, RoutingRuleKind.DOMAIN_SUFFIX -> {
                    val host = query.domain?.let(DomainRouting::normalize) ?: return@forEach
                    val hit = if (rule.kind == RoutingRuleKind.DOMAIN_EXACT) {
                        DomainRouting.matchesExact(rule.value, host)
                    } else {
                        DomainRouting.matchesSuffix(rule.value, host)
                    }
                    if (hit) return rule
                }
                RoutingRuleKind.CIDR -> {
                    val parsed = CidrRouting.parse(rule.value) ?: return@forEach
                    val ip = query.ip ?: return@forEach
                    if (CidrRouting.contains(parsed, ip)) return rule
                }
            }
        }
        return null
    }

    private fun matchingDomain(rules: List<RoutingRule>, domain: String?): RoutingRule? {
        val host = domain?.let(DomainRouting::normalize) ?: return null
        val exact = rules.firstOrNull {
            it.kind == RoutingRuleKind.DOMAIN_EXACT && DomainRouting.matchesExact(it.value, host)
        }
        if (exact != null) return exact
        return rules.firstOrNull {
            it.kind == RoutingRuleKind.DOMAIN_SUFFIX && DomainRouting.matchesSuffix(it.value, host)
        }
    }

    private fun matchingCidr(rules: List<RoutingRule>, ip: String?): RoutingRule? {
        if (ip.isNullOrBlank()) return null
        return rules.firstOrNull { rule ->
            rule.kind == RoutingRuleKind.CIDR && CidrRouting.parse(rule.value)?.let { CidrRouting.contains(it, ip) } == true
        }
    }

    fun encodeRules(rules: List<RoutingRule>): String {
        return rules.mapNotNull(::sanitizeRule).joinToString(prefix = "[", postfix = "]") { rule ->
            buildString {
                append('{')
                append("\"id\":").append(jsonString(rule.id)).append(',')
                append("\"kind\":").append(jsonString(rule.kind.name)).append(',')
                append("\"value\":").append(jsonString(rule.value)).append(',')
                append("\"action\":").append(jsonString(rule.action.name))
                append('}')
            }
        }
    }

    fun parseRules(raw: String?): List<RoutingRule> {
        if (raw.isNullOrBlank()) return emptyList()
        val trimmed = raw.trim()
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) return emptyList()
        val body = trimmed.substring(1, trimmed.lastIndex).trim()
        if (body.isEmpty()) return emptyList()
        return splitTopObjects(body).mapIndexedNotNull { index, obj ->
            val id = jsonField(obj, "id")?.ifBlank { "r$index" } ?: return@mapIndexedNotNull null
            val kind = jsonField(obj, "kind")?.let { runCatching { RoutingRuleKind.valueOf(it) }.getOrNull() }
                ?: return@mapIndexedNotNull null
            val action = jsonField(obj, "action")?.let { runCatching { RouteAction.valueOf(it) }.getOrNull() }
                ?: return@mapIndexedNotNull null
            val value = jsonField(obj, "value") ?: return@mapIndexedNotNull null
            sanitizeRule(RoutingRule(id = id, kind = kind, value = value, action = action))
        }
    }

    private fun jsonString(value: String): String {
        return buildString {
            append('"')
            value.forEach { ch ->
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    else -> append(ch)
                }
            }
            append('"')
        }
    }

    private fun jsonField(obj: String, key: String): String? {
        val needle = "\"$key\""
        val idx = obj.indexOf(needle)
        if (idx < 0) return null
        val colon = obj.indexOf(':', idx + needle.length)
        if (colon < 0) return null
        var i = colon + 1
        while (i < obj.length && obj[i].isWhitespace()) i++
        if (i >= obj.length || obj[i] != '"') return null
        i++
        val sb = StringBuilder()
        while (i < obj.length) {
            val ch = obj[i]
            if (ch == '\\' && i + 1 < obj.length) {
                sb.append(obj[i + 1])
                i += 2
                continue
            }
            if (ch == '"') break
            sb.append(ch)
            i++
        }
        return sb.toString()
    }

    private fun splitTopObjects(body: String): List<String> {
        val out = ArrayList<String>()
        var depth = 0
        var start = -1
        body.forEachIndexed { index, ch ->
            if (ch == '{') {
                if (depth == 0) start = index
                depth++
            } else if (ch == '}') {
                depth--
                if (depth == 0 && start >= 0) {
                    out.add(body.substring(start, index + 1))
                    start = -1
                }
            }
        }
        return out
    }
}
