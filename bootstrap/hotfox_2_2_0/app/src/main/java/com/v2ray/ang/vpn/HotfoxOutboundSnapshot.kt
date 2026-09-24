package com.v2ray.ang.vpn

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.SecretRedactor

/**
 * Sanitized field-by-field snapshot of a selected outbound.
 * UUID/password/token/private keys are always [REDACTED].
 */
data class HotfoxOutboundSnapshot(
    val protocol: String,
    val address: String,
    val port: Int?,
    val network: String,
    val security: String,
    val flow: String,
    val sni: String,
    val fingerprint: String,
    val alpn: String,
    val reality: Boolean,
    val publicKeyPresent: Boolean,
    val shortIdPresent: Boolean,
    val path: String,
    val host: String,
    val serviceName: String,
    val xhttp: Boolean,
    val grpc: Boolean,
    val packetEncoding: String,
    val mux: Boolean,
    val ipv4: Boolean,
    val ipv6: Boolean,
) {
    fun lines(): List<String> = listOf(
        "protocol=$protocol",
        "address=$address",
        "port=${port ?: ""}",
        "network=$network",
        "security=$security",
        "flow=$flow",
        "sni=$sni",
        "fingerprint=$fingerprint",
        "alpn=$alpn",
        "reality=$reality",
        "publicKey=${if (publicKeyPresent) HotfoxOutboundSanitizer.REDACTED else ""}",
        "shortId=${if (shortIdPresent) HotfoxOutboundSanitizer.REDACTED else ""}",
        "path=$path",
        "host=$host",
        "serviceName=$serviceName",
        "xhttp=$xhttp",
        "grpc=$grpc",
        "packetEncoding=$packetEncoding",
        "mux=$mux",
        "ipv4=$ipv4",
        "ipv6=$ipv6",
    )
}

object HotfoxOutboundSanitizer {
    const val REDACTED = "[REDACTED]"

    fun redactSecret(value: String?): String {
        if (value.isNullOrBlank()) return ""
        return REDACTED
    }

    fun present(value: String?): Boolean = !value.isNullOrBlank()
}

data class HotfoxInboundPorts(
    val socksPort: Int?,
    val socksListen: String,
    val httpPort: Int?,
)

object HotfoxOutboundCompare {
    private val BLOCKING_PREFIXES = listOf(
        "protocol",
        "address",
        "port",
        "network",
        "security",
        "reality",
        "publicKeyPresent",
        "generated",
    )

    data class Result(
        val profile: HotfoxOutboundSnapshot?,
        val generated: HotfoxOutboundSnapshot?,
        val mismatches: List<String>,
    ) {
        val blockingMismatch: Boolean
            get() = mismatches.any { field ->
                BLOCKING_PREFIXES.any { field.startsWith(it) }
            }

        fun summary(): String = buildString {
            append("generatedPresent=${generated != null}")
            append(" mismatches=${mismatches.size}")
            append(" blocking=$blockingMismatch")
            if (mismatches.isNotEmpty()) {
                append(" fields=${mismatches.joinToString(",")}")
            }
        }
    }

    @Volatile
    var last: Result? = null
        private set

    fun resetForTests() {
        last = null
    }

    fun isContainerConfigType(typeName: String): Boolean {
        return when (typeName.uppercase()) {
            "CUSTOM", "POLICYGROUP", "PROXYCHAIN" -> true
            else -> false
        }
    }

    fun logicalProtocol(profile: ProfileItem): String {
        val name = profile.configType.name
        if (isContainerConfigType(name)) return ""
        return name.lowercase()
    }

    fun fromProfile(profile: ProfileItem): HotfoxOutboundSnapshot {
        val network = profile.network.orEmpty()
        val security = profile.security.orEmpty()
        return HotfoxOutboundSnapshot(
            protocol = logicalProtocol(profile),
            address = profile.server.orEmpty(),
            port = profile.serverPort?.toIntOrNull(),
            network = network,
            security = security,
            flow = profile.flow.orEmpty(),
            sni = profile.sni.orEmpty(),
            fingerprint = profile.fingerPrint.orEmpty(),
            alpn = profile.alpn.orEmpty(),
            reality = security.equals("reality", ignoreCase = true),
            publicKeyPresent = HotfoxOutboundSanitizer.present(profile.publicKey),
            shortIdPresent = HotfoxOutboundSanitizer.present(profile.shortId),
            path = profile.path.orEmpty(),
            host = profile.host.orEmpty(),
            serviceName = profile.serviceName.orEmpty(),
            xhttp = network.equals("xhttp", ignoreCase = true),
            grpc = network.equals("grpc", ignoreCase = true),
            packetEncoding = "",
            mux = false,
            ipv4 = true,
            ipv6 = false,
        )
    }

    fun inboundPorts(json: String): HotfoxInboundPorts {
        return runCatching {
            val root = JsonParser.parseString(json).asJsonObject
            val inbounds = root.get("inbounds")?.takeIf { it.isJsonArray }?.asJsonArray
                ?: return HotfoxInboundPorts(null, "", null)
            var socksPort: Int? = null
            var socksListen = ""
            var httpPort: Int? = null
            for (element in inbounds) {
                if (!element.isJsonObject) continue
                val obj = element.asJsonObject
                val protocol = obj.get("protocol")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty().lowercase()
                val port = obj.get("port")?.takeIf { it.isJsonPrimitive }?.asInt
                val listen = obj.get("listen")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty()
                when (protocol) {
                    "socks", "socks4", "socks5" -> {
                        if (socksPort == null) {
                            socksPort = port
                            socksListen = listen
                        }
                    }
                    "http" -> if (httpPort == null) httpPort = port
                }
            }
            HotfoxInboundPorts(socksPort, socksListen, httpPort)
        }.getOrDefault(HotfoxInboundPorts(null, "", null))
    }

    fun fromGeneratedJson(json: String): HotfoxOutboundSnapshot? {
        return runCatching {
            val root = JsonParser.parseString(json).asJsonObject
            val outbounds = root.getAsJsonArray("outbounds") ?: return null
            val proxy = firstProxyOutbound(outbounds) ?: return null
            snapshotFromOutbound(proxy)
        }.getOrNull()
    }

    fun mismatches(profile: HotfoxOutboundSnapshot, generated: HotfoxOutboundSnapshot): List<String> {
        val found = mutableListOf<String>()
        fun cmp(name: String, left: String, right: String) {
            if (left.isBlank() || right.isBlank()) return
            if (!left.equals(right, ignoreCase = true)) found += "$name:$left!=$right"
        }
        cmp("protocol", profile.protocol, generated.protocol)
        cmp("address", profile.address, generated.address)
        if (profile.port != null && generated.port != null && profile.port != generated.port) {
            found += "port:${profile.port}!=${generated.port}"
        }
        cmp("network", normalizeNetwork(profile.network), normalizeNetwork(generated.network))
        cmp("security", profile.security, generated.security)
        cmp("flow", profile.flow, generated.flow)
        cmp("sni", profile.sni, generated.sni)
        cmp("fingerprint", profile.fingerprint, generated.fingerprint)
        if (profile.reality != generated.reality && (profile.reality || generated.security.equals("reality", true))) {
            found += "reality:${profile.reality}!=${generated.reality}"
        }
        if (profile.publicKeyPresent != generated.publicKeyPresent && profile.reality) {
            found += "publicKeyPresent:${profile.publicKeyPresent}!=${generated.publicKeyPresent}"
        }
        cmp("path", profile.path, generated.path)
        cmp("host", profile.host, generated.host)
        cmp("serviceName", profile.serviceName, generated.serviceName)
        return found
    }

    fun normalizeNetwork(raw: String): String {
        val network = raw.trim().lowercase()
        return if (network == "raw" || network == "tcp") "tcp" else network
    }

    fun record(profile: ProfileItem, generatedJson: String, expectedJson: String? = null): Result {
        val fromProfile = fromProfile(profile)
        val generated = fromGeneratedJson(generatedJson)
        val diffs = ArrayList<String>()
        val typeName = profile.configType.name
        if (isContainerConfigType(typeName)) {
            diffs += containerMismatches(typeName, profile, generatedJson, expectedJson)
        } else if (generated == null) {
            diffs += "generated:missing-proxy-outbound"
        } else {
            diffs += mismatches(fromProfile, generated)
        }
        val result = Result(fromProfile, generated, diffs)
        last = result
        LogUtil.i(AppConfig.TAG, SecretRedactor.redact("HotfoxOutboundCompare: ${result.summary()}"))
        if (generated != null) {
            LogUtil.i(
                AppConfig.TAG,
                SecretRedactor.redact("HotfoxOutboundCompare generated: ${generated.lines().joinToString(" ")}"),
            )
        }
        return result
    }

    fun proxyOutboundCount(json: String): Int = proxyHops(json).size

    fun proxyHops(json: String): List<HotfoxOutboundSnapshot> {
        return runCatching {
            val root = JsonParser.parseString(json).asJsonObject
            val outbounds = root.getAsJsonArray("outbounds") ?: return emptyList()
            val hops = ArrayList<HotfoxOutboundSnapshot>()
            val skip = setOf("freedom", "blackhole", "dns", "block", "direct")
            for (element in outbounds) {
                if (!element.isJsonObject) continue
                val obj = element.asJsonObject
                val protocol = obj.str("protocol").lowercase()
                if (protocol.isBlank() || protocol in skip) continue
                hops += snapshotFromOutbound(obj)
            }
            hops
        }.getOrDefault(emptyList())
    }

    fun hopIdentity(snapshot: HotfoxOutboundSnapshot): String {
        return listOf(
            snapshot.protocol.trim().lowercase(),
            snapshot.address.trim().lowercase(),
            snapshot.port?.toString().orEmpty(),
            normalizeNetwork(snapshot.network),
            snapshot.security.trim().lowercase(),
            if (snapshot.reality) "1" else "0",
            if (snapshot.publicKeyPresent) "1" else "0",
        ).joinToString("|")
    }

    private fun containerMismatches(
        typeName: String,
        profile: ProfileItem,
        generatedJson: String,
        expectedJson: String?,
    ): List<String> {
        val source = expectedJson?.trim().orEmpty()
        if (source.isEmpty()) {
            return listOf("generated:missing-expected-plan")
        }
        val expectedHops = proxyHops(source)
        val generatedHops = proxyHops(generatedJson)
        return when (typeName.uppercase()) {
            "CUSTOM" -> customMismatches(source, generatedJson, expectedHops, generatedHops)
            "POLICYGROUP" -> groupMismatches(profile, expectedHops, generatedHops)
            "PROXYCHAIN" -> chainMismatches(expectedHops, generatedHops)
            else -> emptyList()
        }
    }

    private fun customMismatches(
        expectedJson: String,
        generatedJson: String,
        expectedHops: List<HotfoxOutboundSnapshot>,
        generatedHops: List<HotfoxOutboundSnapshot>,
    ): List<String> {
        val found = ArrayList<String>()
        if (expectedHops.isEmpty()) {
            found += "generated:missing-custom-source"
            return found
        }
        if (generatedHops.isEmpty()) {
            found += "generated:missing-proxy-outbound"
            return found
        }
        val expectedIds = expectedHops.map(::hopIdentity)
        val generatedIds = generatedHops.map(::hopIdentity)
        for (id in expectedIds) {
            if (id !in generatedIds) {
                found += "generated:custom-unrelated-or-missing-hop"
            }
        }
        for (id in generatedIds) {
            if (id !in expectedIds) {
                found += "generated:custom-unexpected-hop"
            }
        }
        if (expectedIds.count { it == expectedIds.first() } != generatedIds.count { it == expectedIds.first() } &&
            found.none { it.startsWith("generated:") }
        ) {
            found += "generated:custom-hop-count:${expectedHops.size}!=${generatedHops.size}"
        }
        val expectedFirst = expectedHops.first()
        val generatedFirst = generatedHops.first()
        if (hopIdentity(expectedFirst) != hopIdentity(generatedFirst)) {
            val fieldDiffs = mismatches(expectedFirst, generatedFirst)
            found += if (fieldDiffs.isNotEmpty()) fieldDiffs else listOf("generated:custom-default-hop-changed")
        }
        found += customTagMismatches(expectedJson, generatedJson)
        return found
    }

    private fun customTagMismatches(expectedJson: String, generatedJson: String): List<String> {
        val expectedTags = proxyOutboundTags(expectedJson)
        if (expectedTags.isEmpty()) return emptyList()
        val generatedTags = proxyOutboundTags(generatedJson)
        return expectedTags
            .filter { tag -> tag.isNotBlank() && tag !in generatedTags }
            .map { "generated:custom-missing-outbound-tag" }
    }

    private fun proxyOutboundTags(json: String): List<String> {
        return runCatching {
            val root = JsonParser.parseString(json).asJsonObject
            val outbounds = root.getAsJsonArray("outbounds") ?: return emptyList()
            val skip = setOf("freedom", "blackhole", "dns", "block", "direct")
            val tags = ArrayList<String>()
            for (element in outbounds) {
                if (!element.isJsonObject) continue
                val obj = element.asJsonObject
                val protocol = obj.str("protocol").lowercase()
                if (protocol.isBlank() || protocol in skip) continue
                val tag = obj.str("tag")
                if (tag.isNotBlank()) tags += tag
            }
            tags
        }.getOrDefault(emptyList())
    }

    private fun groupMismatches(
        profile: ProfileItem,
        expectedHops: List<HotfoxOutboundSnapshot>,
        generatedHops: List<HotfoxOutboundSnapshot>,
    ): List<String> {
        val found = ArrayList<String>()
        if (expectedHops.isEmpty()) {
            found += "generated:missing-expected-plan"
            return found
        }
        if (generatedHops.isEmpty()) {
            found += "generated:missing-group-member"
            return found
        }
        val expectedIds = expectedHops.map(::hopIdentity)
        val generatedIds = generatedHops.map(::hopIdentity)
        for ((index, id) in expectedIds.withIndex()) {
            if (id !in generatedIds) {
                val expected = expectedHops[index]
                found += "generated:missing-group-member"
                if (expected.address.isNotBlank()) {
                    val generatedAddress = generatedHops.getOrNull(index)?.address.orEmpty()
                    if (generatedAddress.isNotBlank() && !expected.address.equals(generatedAddress, true)) {
                        found += "address:${expected.address}!=$generatedAddress"
                    }
                }
            }
        }
        for (id in generatedIds.distinct()) {
            if (id !in expectedIds) {
                found += "generated:unexpected-group-member"
            }
        }
        val selectedAddress = profile.server.orEmpty()
        if (selectedAddress.isNotBlank()) {
            val selectedPort = profile.serverPort?.toIntOrNull()
            val hit = generatedHops.any { hop ->
                hop.address.equals(selectedAddress, ignoreCase = true) &&
                    (selectedPort == null || hop.port == null || hop.port == selectedPort)
            }
            if (!hit) {
                found += "address:$selectedAddress!=${generatedHops.first().address}"
            }
        }
        return found
    }

    private fun chainMismatches(
        expectedHops: List<HotfoxOutboundSnapshot>,
        generatedHops: List<HotfoxOutboundSnapshot>,
    ): List<String> {
        val found = ArrayList<String>()
        if (expectedHops.isEmpty()) {
            found += "generated:missing-expected-plan"
            return found
        }
        if (generatedHops.isEmpty()) {
            found += "generated:missing-chain-hop"
            return found
        }
        if (expectedHops.size != generatedHops.size) {
            found += "generated:chain-hop-count:${expectedHops.size}!=${generatedHops.size}"
        }
        if (generatedHops.size < expectedHops.size) {
            found += "generated:missing-chain-hop"
        }
        val limit = minOf(expectedHops.size, generatedHops.size)
        var orderDrift = false
        for (index in 0 until limit) {
            val fieldDiffs = mismatches(expectedHops[index], generatedHops[index])
            found += fieldDiffs
            if (hopIdentity(expectedHops[index]) != hopIdentity(generatedHops[index])) {
                orderDrift = true
            }
        }
        if (orderDrift && found.none { it.startsWith("generated:chain-hop") }) {
            found += "generated:chain-hop-order"
        }
        return found
    }

    private fun firstProxyOutbound(outbounds: JsonArray): JsonObject? {
        val skip = setOf("freedom", "blackhole", "dns", "block", "direct")
        for (element in outbounds) {
            if (!element.isJsonObject) continue
            val obj = element.asJsonObject
            val protocol = obj.get("protocol")?.asString.orEmpty().lowercase()
            if (protocol.isBlank() || protocol in skip) continue
            return obj
        }
        return null
    }

    private fun JsonObject.obj(name: String): JsonObject? =
        get(name)?.takeIf { it.isJsonObject }?.asJsonObject

    private fun JsonObject.arr(name: String): JsonArray? =
        get(name)?.takeIf { it.isJsonArray }?.asJsonArray

    private fun JsonObject.str(name: String): String =
        get(name)?.takeIf { it.isJsonPrimitive }?.asString.orEmpty()

    private fun JsonObject.intOrNull(name: String): Int? =
        get(name)?.takeIf { it.isJsonPrimitive }?.asInt

    private fun snapshotFromOutbound(outbound: JsonObject): HotfoxOutboundSnapshot {
        val protocol = outbound.str("protocol")
        val settings = outbound.obj("settings")
        val stream = outbound.obj("streamSettings")
        val vnext = settings?.arr("vnext")?.firstOrNull()?.takeIf { it.isJsonObject }?.asJsonObject
        val server = settings?.arr("servers")?.firstOrNull()?.takeIf { it.isJsonObject }?.asJsonObject
        val address = vnext?.str("address")?.takeIf { it.isNotBlank() }
            ?: server?.str("address")?.takeIf { it.isNotBlank() }
            ?: settings?.str("address").orEmpty()
        val port = vnext?.intOrNull("port")
            ?: server?.intOrNull("port")
            ?: settings?.intOrNull("port")
        val users = vnext?.arr("users")?.firstOrNull()?.takeIf { it.isJsonObject }?.asJsonObject
        val flow = users?.str("flow").orEmpty()
        val packetEncoding = settings?.str("packetEncoding").orEmpty()
        val network = stream?.str("network").orEmpty()
        val security = stream?.str("security").orEmpty()
        val reality = stream?.obj("realitySettings")
        val tls = stream?.obj("tlsSettings")
        val ws = stream?.obj("wsSettings")
        val grpc = stream?.obj("grpcSettings")
        val xhttp = stream?.obj("xhttpSettings")
        val sockopt = stream?.obj("sockopt")
        val sni = reality?.str("serverName")?.takeIf { it.isNotBlank() }
            ?: tls?.str("serverName").orEmpty()
        val fingerprint = reality?.str("fingerprint")?.takeIf { it.isNotBlank() }
            ?: tls?.str("fingerprint").orEmpty()
        val alpn = tls?.get("alpn")?.let { el ->
            when {
                el.isJsonArray -> el.asJsonArray.joinToString(",") { it.asString }
                el.isJsonPrimitive -> el.asString
                else -> ""
            }
        }.orEmpty()
        val host = ws?.obj("headers")?.str("Host")?.takeIf { it.isNotBlank() }
            ?: ws?.str("host").orEmpty()
        val path = xhttp?.str("path")?.takeIf { it.isNotBlank() }
            ?: ws?.str("path").orEmpty()
        val serviceName = grpc?.str("serviceName").orEmpty()
        val domainStrategy = sockopt?.str("domainStrategy").orEmpty()
        val mux = outbound.obj("mux")?.get("enabled")?.takeIf { it.isJsonPrimitive }?.asBoolean == true
        return HotfoxOutboundSnapshot(
            protocol = protocol,
            address = address,
            port = port,
            network = network,
            security = security,
            flow = flow,
            sni = sni,
            fingerprint = fingerprint,
            alpn = alpn,
            reality = security.equals("reality", ignoreCase = true) || reality != null,
            publicKeyPresent = HotfoxOutboundSanitizer.present(reality?.str("publicKey")),
            shortIdPresent = HotfoxOutboundSanitizer.present(reality?.str("shortId")),
            path = path,
            host = host,
            serviceName = serviceName,
            xhttp = network.equals("xhttp", ignoreCase = true) || xhttp != null,
            grpc = network.equals("grpc", ignoreCase = true) || grpc != null,
            packetEncoding = packetEncoding,
            mux = mux,
            ipv4 = !domainStrategy.contains("IPv6", ignoreCase = true) || domainStrategy.contains("IPv4", ignoreCase = true),
            ipv6 = domainStrategy.contains("IPv6", ignoreCase = true) || domainStrategy.equals("UseIP", ignoreCase = true),
        )
    }
}
