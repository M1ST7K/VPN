package com.v2ray.ang.vpn

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser

data class XrayFieldRule(
    val domain: List<String> = emptyList(),
    val ip: List<String> = emptyList(),
    val outboundTag: String,
    val source: String,
)

/**
 * Deterministic Xray field rules derived from the HotFox routing snapshot.
 *
 * App-split and LAN are applied at the Android VpnService/TUN layer
 * (`selectedApps`, `lanAccess`), not as Xray field rules. Custom APP/LAN
 * routing rules are rejected at parse/sanitize.
 *
 * First-match buckets match [HotfoxRoutingPolicy.decide] for captured traffic:
 * `BLOCK, exact-domain, suffix-domain, CIDR`. User order is kept only inside
 * a bucket.
 *
 * INCLUDE/EXCLUDE modes preserve `APP > DOMAIN > CIDR` by emitting only BLOCK
 * (and ads) field rules. Non-block DOMAIN/CIDR cannot be enforced after TUN
 * because Xray has no Android package identity.
 *
 * Tags match `AppConfig.TAG_PROXY` / `TAG_DIRECT` / `TAG_BLOCKED`.
 */
object HotfoxXrayRouting {
    const val TAG_PROXY = "proxy"
    const val TAG_DIRECT = "direct"
    const val TAG_BLOCKED = "block"
    const val ADS_GEOSITE = "geosite:category-ads-all"

    fun rules(snapshot: RoutingPolicySnapshot): List<XrayFieldRule> {
        val blocked = ArrayList<XrayFieldRule>()
        val exact = ArrayList<XrayFieldRule>()
        val suffix = ArrayList<XrayFieldRule>()
        val cidr = ArrayList<XrayFieldRule>()
        if (snapshot.adsBlocked) {
            blocked.add(
                XrayFieldRule(
                    domain = listOf(ADS_GEOSITE),
                    outboundTag = TAG_BLOCKED,
                    source = "ads",
                ),
            )
        }
        val appSplit = snapshot.funnelsCapturedTrafficByApp()
        snapshot.rules.mapNotNull(HotfoxRoutingPolicy::sanitizeRule).forEach { rule ->
            val xray = toXray(rule) ?: return@forEach
            when {
                rule.action == RouteAction.BLOCK -> blocked.add(xray)
                appSplit -> Unit
                rule.kind == RoutingRuleKind.DOMAIN_EXACT -> exact.add(xray)
                rule.kind == RoutingRuleKind.DOMAIN_SUFFIX -> suffix.add(xray)
                rule.kind == RoutingRuleKind.CIDR -> cidr.add(xray)
                else -> Unit
            }
        }
        return blocked + exact + suffix + cidr
    }

    private fun toXray(rule: RoutingRule): XrayFieldRule? {
        val tag = when (rule.action) {
            RouteAction.VPN -> TAG_PROXY
            RouteAction.DIRECT -> TAG_DIRECT
            RouteAction.BLOCK -> TAG_BLOCKED
        }
        return when (rule.kind) {
            RoutingRuleKind.DOMAIN_EXACT -> XrayFieldRule(
                domain = listOf("full:${rule.value}"),
                outboundTag = tag,
                source = rule.id,
            )
            RoutingRuleKind.DOMAIN_SUFFIX -> XrayFieldRule(
                domain = listOf("domain:${rule.value}"),
                outboundTag = tag,
                source = rule.id,
            )
            RoutingRuleKind.CIDR -> XrayFieldRule(
                ip = listOf(rule.value.trim()),
                outboundTag = tag,
                source = rule.id,
            )
            RoutingRuleKind.APP, RoutingRuleKind.LAN -> null
        }
    }
}

object HotfoxXrayConfigInjector {
    data class ExistingRule(val outboundTag: String)

    /**
     * Prepends HotFox field rules. SMART/GLOBAL/include/exclude drop preset
     * `direct` rules so geosite:cn / geoip:private cannot silently bypass VPN.
     * CUSTOM keeps editor/subscription DIRECT rules after the HotFox prefix.
     */
    fun merge(existing: List<ExistingRule>, snapshot: RoutingPolicySnapshot): List<XrayFieldRule> {
        val prefix = HotfoxXrayRouting.rules(snapshot)
        val kept = existing.mapNotNull { rule ->
            if (!keepExisting(snapshot.mode, rule.outboundTag)) return@mapNotNull null
            XrayFieldRule(outboundTag = rule.outboundTag, source = "existing")
        }
        return prefix + kept
    }

    fun keepExisting(mode: HotfoxRoutingMode, outboundTag: String): Boolean {
        if (mode != HotfoxRoutingMode.CUSTOM && outboundTag == HotfoxXrayRouting.TAG_DIRECT) {
            return false
        }
        return true
    }

    /**
     * Malformed JSON is returned unchanged so optional routing cannot break the core.
     */
    fun apply(content: String, snapshot: RoutingPolicySnapshot): String {
        if (content.isBlank()) return content
        return runCatching {
            val root = JsonParser.parseString(content).asJsonObject
            val tags = HotfoxXrayTagResolver.resolve(content)
            val routing = root.get("routing")?.takeIf { it.isJsonObject }?.asJsonObject
                ?: JsonObject().also { root.add("routing", it) }
            val existing = routing.get("rules")?.takeIf { it.isJsonArray }?.asJsonArray ?: JsonArray()
            val existingRules = ArrayList<ExistingRule>()
            for (index in 0 until existing.size()) {
                val rule = existing[index].takeIf { it.isJsonObject }?.asJsonObject ?: continue
                existingRules.add(ExistingRule(primitive(rule, "outboundTag")))
            }
            val planned = merge(existingRules, snapshot)
            val merged = JsonArray()
            var existingIndex = 0
            planned.forEach { rule ->
                if (rule.source == "existing") {
                    while (existingIndex < existing.size()) {
                        val raw = existing[existingIndex++].takeIf { it.isJsonObject }?.asJsonObject
                        if (raw != null && keepRaw(snapshot, raw, tags)) {
                            rewriteRuleTags(raw, tags)
                            merged.add(raw)
                            return@forEach
                        }
                    }
                } else {
                    merged.add(toJson(rule, tags))
                }
            }
            routing.add("rules", merged)
            root.toString()
        }.getOrDefault(content)
    }

    private fun keepRaw(
        snapshot: RoutingPolicySnapshot,
        raw: JsonObject,
        tags: HotfoxXrayTagMap,
    ): Boolean {
        val outbound = primitive(raw, "outboundTag")
        if (!keepExisting(snapshot.mode, outbound)) return false
        if (snapshot.mode == HotfoxRoutingMode.CUSTOM) return true
        val domains = stringList(raw, "domain")
        val ips = stringList(raw, "ip")
        return !HotfoxLegacyDirectBypass.isLegacyDirectBypass(domains, ips, outbound, tags.direct)
    }

    private fun stringList(obj: JsonObject, name: String): List<String> {
        val array = obj.get(name)?.takeIf { it.isJsonArray }?.asJsonArray ?: return emptyList()
        val out = ArrayList<String>()
        for (index in 0 until array.size()) {
            val item = array[index].takeIf { it.isJsonPrimitive }?.asString?.trim().orEmpty()
            if (item.isNotBlank()) out.add(item)
        }
        return out
    }

    private fun primitive(obj: JsonObject, name: String): String =
        obj.get(name)?.takeIf { it.isJsonPrimitive }?.asString?.trim().orEmpty()

    private fun rewriteRuleTags(raw: JsonObject, tags: HotfoxXrayTagMap) {
        val outbound = primitive(raw, "outboundTag")
        val mapped = when (outbound) {
            HotfoxXrayRouting.TAG_PROXY -> tags.proxy
            HotfoxXrayRouting.TAG_DIRECT -> tags.direct
            HotfoxXrayRouting.TAG_BLOCKED -> tags.block
            else -> null
        }
        if (!mapped.isNullOrBlank()) raw.addProperty("outboundTag", mapped)
    }

    private fun toJson(rule: XrayFieldRule, tags: HotfoxXrayTagMap): JsonObject {
        val obj = JsonObject()
        obj.addProperty("type", "field")
        if (rule.domain.isNotEmpty()) {
            val domain = JsonArray()
            rule.domain.forEach { domain.add(it) }
            obj.add("domain", domain)
        }
        if (rule.ip.isNotEmpty()) {
            val ip = JsonArray()
            rule.ip.forEach { ip.add(it) }
            obj.add("ip", ip)
        }
        val resolved = when (rule.outboundTag) {
            HotfoxXrayRouting.TAG_PROXY -> tags.proxy ?: rule.outboundTag
            HotfoxXrayRouting.TAG_DIRECT -> tags.direct ?: rule.outboundTag
            HotfoxXrayRouting.TAG_BLOCKED -> tags.block ?: rule.outboundTag
            else -> rule.outboundTag
        }
        obj.addProperty("outboundTag", resolved)
        return obj
    }
}
