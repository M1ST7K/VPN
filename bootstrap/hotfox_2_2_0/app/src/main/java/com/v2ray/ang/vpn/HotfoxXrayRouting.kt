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
 * These rules apply only to captured traffic. EXCLUDE selected / INCLUDE miss
 * never reach Xray, so BLOCK/ads cannot apply to them.
 *
 * First-match buckets match [HotfoxRoutingPolicy.decide] for captured traffic:
 * `BLOCK, exact-domain, suffix-domain, CIDR`. User order is kept only inside
 * a bucket.
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
        snapshot.rules.mapNotNull(HotfoxRoutingPolicy::sanitizeRule).forEach { rule ->
            val xray = toXray(rule) ?: return@forEach
            when {
                rule.action == RouteAction.BLOCK -> blocked.add(xray)
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
            val root = JSONObject(content)
            val routing = root.optJSONObject("routing") ?: JSONObject().also { root.put("routing", it) }
            val existing = routing.optJSONArray("rules") ?: JSONArray()
            val existingRules = ArrayList<ExistingRule>()
            for (index in 0 until existing.length()) {
                val rule = existing.optJSONObject(index) ?: continue
                existingRules.add(ExistingRule(rule.optString("outboundTag")))
            }
            val planned = merge(existingRules, snapshot)
            val merged = JSONArray()
            var existingIndex = 0
            planned.forEach { rule ->
                if (rule.source == "existing") {
                    while (existingIndex < existing.length()) {
                        val raw = existing.optJSONObject(existingIndex++)
                        if (raw != null && keepExisting(snapshot.mode, raw.optString("outboundTag"))) {
                            merged.put(raw)
                            return@forEach
                        }
                    }
                } else {
                    merged.put(toJson(rule))
                }
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
