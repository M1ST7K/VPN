package com.v2ray.ang.commerce

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser

data class ParsedManagedManifest(
    val servers: List<ManifestRefreshPolicy.ServerIdentity>,
    val malformed: Boolean,
)

/**
 * Inventory JSON used by HotFox-managed sync. This is not a payment backend.
 * Payloads must not contain production VLESS URLs or provider credentials.
 */
object ManagedManifestParser {
    const val FORMAT = "hotfox-sandbox-v1"

    fun encode(servers: List<ManifestRefreshPolicy.ServerIdentity>): CommerceManifest {
        val array = JsonArray()
        servers.forEach { identity ->
            val item = JsonObject()
            item.addProperty("remarks", identity.remarks)
            item.addProperty("server", identity.server)
            item.addProperty("port", identity.port)
            item.addProperty("protocol", identity.protocol)
            item.addProperty("network", identity.network)
            item.addProperty("security", identity.security)
            item.addProperty("fingerprint", identity.fingerprint)
            array.add(item)
        }
        val root = JsonObject()
        root.addProperty("format", FORMAT)
        root.add("servers", array)
        return CommerceManifest(format = FORMAT, payload = root.toString())
    }

    fun parse(manifest: CommerceManifest): ParsedManagedManifest {
        val payload = manifest.payload
        if (payload.isNullOrBlank()) {
            return ParsedManagedManifest(emptyList(), malformed = false)
        }
        return try {
            val root = JsonParser.parseString(payload).asJsonObject
            val format = root.get("format")?.asString ?: manifest.format
            if (format.isNotBlank() && format != FORMAT) {
                return ParsedManagedManifest(emptyList(), malformed = true)
            }
            val array = root.getAsJsonArray("servers") ?: JsonArray()
            val servers = ArrayList<ManifestRefreshPolicy.ServerIdentity>(array.size())
            for (element in array) {
                val item = element.asJsonObject
                val remarks = item.get("remarks")?.asString.orEmpty()
                val server = item.get("server")?.asString.orEmpty()
                val port = item.get("port")?.asString.orEmpty()
                if (remarks.isBlank() || server.isBlank() || port.isBlank()) {
                    return ParsedManagedManifest(emptyList(), malformed = true)
                }
                servers.add(
                    ManifestRefreshPolicy.ServerIdentity(
                        remarks = remarks,
                        server = server,
                        port = port,
                        protocol = item.get("protocol")?.asString.orEmpty(),
                        network = item.get("network")?.asString.orEmpty(),
                        security = item.get("security")?.asString.orEmpty(),
                        fingerprint = item.get("fingerprint")?.asString.orEmpty(),
                    ),
                )
            }
            ParsedManagedManifest(servers, malformed = false)
        } catch (_: Exception) {
            ParsedManagedManifest(emptyList(), malformed = true)
        }
    }

    fun toProfile(
        identity: ManifestRefreshPolicy.ServerIdentity,
        subscriptionId: String,
    ): ManagedProfile {
        val guid = "hf-" + HotfoxManifestRefresh.fingerprintOf(
            identity.remarks,
            identity.server,
            identity.port,
            identity.protocol,
            identity.network,
            identity.security,
            identity.fingerprint,
        ).take(16)
        return ManagedProfile(
            guid = guid,
            remarks = identity.remarks,
            server = identity.server,
            port = identity.port,
            protocol = identity.protocol,
            network = identity.network,
            security = identity.security,
            fingerprint = identity.fingerprint,
            subscriptionId = subscriptionId,
        )
    }
}
