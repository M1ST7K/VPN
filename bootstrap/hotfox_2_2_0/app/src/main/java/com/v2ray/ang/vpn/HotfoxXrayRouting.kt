package com.v2ray.ang.vpn

import org.json.JSONArray
import org.json.JSONObject

data class XrayFieldRule(
    val domain: List<String> = emptyList(),
    val ip: List<String> = emptyList(),
    val outboundTag: String,
    val source: String,
)

/**
 * Deterministic Xray field rules derived from the HotFox routing snapshot.
 *
 * App-split and LAN are applied at the Android VpnService/TUN layer, not here.
 * Domain/CIDR/BLOCK and optional geosite ads use Xray first-match rules.
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
        val rest = ArrayList<XrayFieldRule>()
        if (snapshot.adsBlocked) {
            blocked.add(
                XrayFieldRule(
                    domain = listOf(ADS_GEOSITE),
                    outboundTag = TAG_BLOCKED,
                    source = "ads",
                ),
            )
        }
        snapshot.rules.mapNotNull(HotfoxRoutingPolicy::sanitizeRule).forEach { rule ->
            val xray = toXray(rule) ?: return@forEach
            if (rule.action == RouteAction.BLOCK) blocked.add(xray) else rest.add(xray)
        }
        return blocked + rest
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
    /**
     * Prepends HotFox field rules. SMART/GLOBAL/include/exclude drop preset
     * `direct` rules so geosite:cn / geoip:private cannot silently bypass VPN.
     * CUSTOM keeps editor/subscription DIRECT rules after the HotFox prefix.
     * Malformed JSON is returned unchanged so optional routing cannot break the core.
     */
    fun apply(content: String, snapshot: RoutingPolicySnapshot): String {
        if (content.isBlank()) return content
        return runCatching {
            val root = JSONObject(content)
            val routing = root.optJSONObject("routing") ?: JSONObject().also { root.put("routing", it) }
            val existing = routing.optJSONArray("rules") ?: JSONArray()
            val dropDirect = snapshot.mode != HotfoxRoutingMode.CUSTOM
            val kept = JSONArray()
            for (index in 0 until existing.length()) {
                val rule = existing.optJSONObject(index) ?: continue
                val tag = rule.optString("outboundTag")
                if (dropDirect && tag == HotfoxXrayRouting.TAG_DIRECT) continue
                kept.put(rule)
            }
            val merged = JSONArray()
            HotfoxXrayRouting.rules(snapshot).forEach { merged.put(toJson(it)) }
            for (index in 0 until kept.length()) {
                merged.put(kept.get(index))
            }
            routing.put("rules", merged)
            root.toString()
        }.getOrDefault(content)
    }

    private fun toJson(rule: XrayFieldRule): JSONObject {
        val obj = JSONObject()
        obj.put("type", "field")
        if (rule.domain.isNotEmpty()) obj.put("domain", JSONArray(rule.domain))
        if (rule.ip.isNotEmpty()) obj.put("ip", JSONArray(rule.ip))
        obj.put("outboundTag", rule.outboundTag)
        return obj
    }
}
