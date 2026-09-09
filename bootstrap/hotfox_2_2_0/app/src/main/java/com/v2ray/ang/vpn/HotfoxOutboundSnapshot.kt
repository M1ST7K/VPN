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

object HotfoxOutboundCompare {
    data class Result(
        val profile: HotfoxOutboundSnapshot?,
        val generated: HotfoxOutboundSnapshot?,
        val mismatches: List<String>,
    ) {
        val blockingMismatch: Boolean
            get() = mismatches.any { it.startsWith("protocol") || it.startsWith("address") || it.startsWith("port") }

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

    fun fromProfile(profile: ProfileItem): HotfoxOutboundSnapshot {
        val network = profile.network.orEmpty()
        val security = profile.security.orEmpty()
        return HotfoxOutboundSnapshot(
            protocol = profile.configType.name.lowercase(),
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
        cmp("network", profile.network, generated.network)
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

    fun record(profile: ProfileItem, generatedJson: String): Result {
        val fromProfile = fromProfile(profile)
        val generated = fromGeneratedJson(generatedJson)
        val diffs = if (generated == null) emptyList() else mismatches(fromProfile, generated)
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
