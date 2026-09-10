package com.v2ray.ang.vpn

import org.json.JSONArray
import org.json.JSONObject

data class HotfoxXrayTagMap(
    val proxy: String?,
    val direct: String?,
    val block: String?,
    val outboundTags: Set<String>,
    val balancerTags: Set<String>,
) {
    fun roleTag(action: RouteAction): String? = when (action) {
        RouteAction.VPN -> proxy
        RouteAction.DIRECT -> direct
        RouteAction.BLOCK -> block
    }

    fun known(tag: String): Boolean = tag in outboundTags || tag in balancerTags
}

object HotfoxXrayTagResolver {
    private val DIRECT_PROTOCOLS = setOf("freedom")
    private val BLOCK_PROTOCOLS = setOf("blackhole")
    private val SKIP_PROTOCOLS = setOf("freedom", "blackhole", "dns")
    private val DIRECT_TAGS = setOf("direct", "hotfox-direct", "out-direct", "freedom")
    private val BLOCK_TAGS = setOf("block", "blocked", "hotfox-block", "blackhole")
    private val PROXY_TAGS = setOf("proxy", "hotfox-proxy", "provider-proxy", "out-proxy")

    fun resolve(json: String): HotfoxXrayTagMap {
        if (json.isBlank()) {
            return HotfoxXrayTagMap(null, null, null, emptySet(), emptySet())
        }
        return runCatching {
            val root = JSONObject(json)
            val outboundTags = LinkedHashSet<String>()
            var proxy: String? = null
            var direct: String? = null
            var block: String? = null
            val outbounds = root.optJSONArray("outbounds") ?: JSONArray()
            for (index in 0 until outbounds.length()) {
                val outbound = outbounds.optJSONObject(index) ?: continue
                val tag = outbound.optString("tag").trim()
                val protocol = outbound.optString("protocol").trim().lowercase()
                if (tag.isNotBlank()) outboundTags.add(tag)
                when {
                    protocol in DIRECT_PROTOCOLS || tag.lowercase() in DIRECT_TAGS ->
                        if (direct == null && tag.isNotBlank()) direct = tag
                    protocol in BLOCK_PROTOCOLS || tag.lowercase() in BLOCK_TAGS ->
                        if (block == null && tag.isNotBlank()) block = tag
                    protocol.isNotBlank() && protocol !in SKIP_PROTOCOLS ->
                        if (proxy == null && tag.isNotBlank()) proxy = tag
                    tag.lowercase() in PROXY_TAGS ->
                        if (proxy == null) proxy = tag
                }
            }
            if (proxy == null) {
                proxy = outboundTags.firstOrNull { it.lowercase() in PROXY_TAGS }
            }
            if (direct == null) {
                direct = outboundTags.firstOrNull { it.lowercase() in DIRECT_TAGS }
            }
            if (block == null) {
                block = outboundTags.firstOrNull { it.lowercase() in BLOCK_TAGS }
            }
            val balancerTags = LinkedHashSet<String>()
            val routing = root.optJSONObject("routing")
            val balancers = routing?.optJSONArray("balancers") ?: JSONArray()
            for (index in 0 until balancers.length()) {
                val balancer = balancers.optJSONObject(index) ?: continue
                val tag = balancer.optString("tag").trim()
                if (tag.isNotBlank()) balancerTags.add(tag)
            }
            HotfoxXrayTagMap(proxy, direct, block, outboundTags, balancerTags)
        }.getOrDefault(HotfoxXrayTagMap(null, null, null, emptySet(), emptySet()))
    }
}

object HotfoxLegacyDirectBypass {
    private val DOMAIN_MARKERS = listOf(
        "geosite:cn",
        "geoip:cn",
        "geosite:ru",
        "geoip:ru",
        "geosite:category-ru",
        "geosite:private",
        "geoip:private",
        "domain:ru",
        "domain:su",
        "domain:xn--p1ai",
        "full:ru",
        "domain:local",
        "domain:localhost",
        "geosite:tld-ru",
    )

    fun isLegacyDirectBypass(domains: List<String>, ips: List<String>, outboundTag: String, directTag: String?): Boolean {
        val tag = outboundTag.trim().lowercase()
        val pointsDirect = tag == "direct" ||
            tag == "freedom" ||
            (directTag != null && outboundTag == directTag)
        if (!pointsDirect) return false
        val haystack = (domains + ips).joinToString(" ").lowercase()
        if (haystack.isBlank()) return tag == "direct" || tag == "freedom"
        if (DOMAIN_MARKERS.any { haystack.contains(it) }) return true
        if (haystack.contains(".ru") || haystack.contains(".su") || haystack.contains(".рф")) return true
        return false
    }
}

object HotfoxXrayConfigValidator {
    data class Report(
        val danglingOutboundTags: List<String>,
        val danglingBalancerTags: List<String>,
        val missingProxyTag: Boolean,
    ) {
        val ok: Boolean
            get() = danglingOutboundTags.isEmpty() && danglingBalancerTags.isEmpty() && !missingProxyTag

        fun message(): String = buildString {
            append("HF-VPN-016 xray-tags")
            if (missingProxyTag) append(" missing-proxy")
            if (danglingOutboundTags.isNotEmpty()) {
                append(" dangling-outbound=${danglingOutboundTags.joinToString(",")}")
            }
            if (danglingBalancerTags.isNotEmpty()) {
                append(" dangling-balancer=${danglingBalancerTags.joinToString(",")}")
            }
        }
    }

    fun inspect(json: String): Report {
        val tags = HotfoxXrayTagResolver.resolve(json)
        val danglingOut = ArrayList<String>()
        val danglingBal = ArrayList<String>()
        runCatching {
            val root = JSONObject(json)
            val rules = root.optJSONObject("routing")?.optJSONArray("rules") ?: JSONArray()
            for (index in 0 until rules.length()) {
                val rule = rules.optJSONObject(index) ?: continue
                val outbound = rule.optString("outboundTag").trim()
                val balancer = rule.optString("balancerTag").trim()
                if (outbound.isNotBlank() && !tags.known(outbound)) danglingOut.add(outbound)
                if (balancer.isNotBlank() && balancer !in tags.balancerTags) danglingBal.add(balancer)
            }
        }
        return Report(
            danglingOutboundTags = danglingOut.distinct(),
            danglingBalancerTags = danglingBal.distinct(),
            missingProxyTag = tags.proxy.isNullOrBlank(),
        )
    }

    fun requireValid(json: String): Report {
        val report = inspect(json)
        if (!report.ok) {
            error(report.message())
        }
        return report
    }
}
