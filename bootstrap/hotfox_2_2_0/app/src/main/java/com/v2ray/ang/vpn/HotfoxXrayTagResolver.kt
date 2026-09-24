package com.v2ray.ang.vpn

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser

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
        val root = runCatching { JsonParser.parseString(json).asJsonObject }.getOrNull()
            ?: return HotfoxXrayTagMap(null, null, null, emptySet(), emptySet())
        val outboundTags = LinkedHashSet<String>()
        var proxy: String? = null
        var direct: String? = null
        var block: String? = null
        val outbounds = root.arr("outbounds")
        for (index in 0 until outbounds.size()) {
            val outbound = outbounds[index].takeIf { it.isJsonObject }?.asJsonObject ?: continue
            val tag = outbound.str("tag")
            val protocol = outbound.str("protocol").lowercase()
            if (tag.isNotBlank()) outboundTags.add(tag)
            when {
                protocol in DIRECT_PROTOCOLS || tag.lowercase() in DIRECT_TAGS ->
                    if (direct == null && tag.isNotBlank()) direct = tag
                protocol in BLOCK_PROTOCOLS || tag.lowercase() in BLOCK_TAGS ->
                    if (block == null && tag.isNotBlank()) block = tag
                protocol.isNotBlank() && protocol !in SKIP_PROTOCOLS ->
                    if (proxy == null && tag.isNotBlank()) proxy = tag
                tag.lowercase() in PROXY_TAGS ->
                    if (proxy == null && tag.isNotBlank()) proxy = tag
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
        val balancers = root.obj("routing")?.arr("balancers") ?: JsonArray()
        for (index in 0 until balancers.size()) {
            val balancer = balancers[index].takeIf { it.isJsonObject }?.asJsonObject ?: continue
            val tag = balancer.str("tag")
            if (tag.isNotBlank()) balancerTags.add(tag)
        }
        return HotfoxXrayTagMap(proxy, direct, block, outboundTags, balancerTags)
    }

    internal fun JsonObject.str(name: String): String =
        get(name)?.takeIf { it.isJsonPrimitive }?.asString?.trim().orEmpty()

    internal fun JsonObject.obj(name: String): JsonObject? =
        get(name)?.takeIf { it.isJsonObject }?.asJsonObject

    internal fun JsonObject.arr(name: String): JsonArray =
        get(name)?.takeIf { it.isJsonArray }?.asJsonArray ?: JsonArray()
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
        val root = runCatching { JsonParser.parseString(json).asJsonObject }.getOrNull()
        val rules = root?.get("routing")?.takeIf { it.isJsonObject }
            ?.asJsonObject?.get("rules")?.takeIf { it.isJsonArray }?.asJsonArray
        if (rules != null) {
            for (index in 0 until rules.size()) {
                val rule = rules[index].takeIf { it.isJsonObject }?.asJsonObject ?: continue
                val outbound = rule.get("outboundTag")?.takeIf { it.isJsonPrimitive }?.asString?.trim().orEmpty()
                val balancer = rule.get("balancerTag")?.takeIf { it.isJsonPrimitive }?.asString?.trim().orEmpty()
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
