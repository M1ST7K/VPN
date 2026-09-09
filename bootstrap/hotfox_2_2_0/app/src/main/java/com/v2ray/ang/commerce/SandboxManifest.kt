package com.v2ray.ang.commerce

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser

data class ParsedSandboxManifest(
    val servers: List<ManifestRefreshPolicy.ServerIdentity>,
    val malformed: Boolean,
)

/**
 * Sandbox/CI manifest format. Payloads describe synthetic hosts only.
 * Production VLESS URLs and provider credentials must not appear here.
 */
object SandboxManifest {
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

    fun parse(manifest: CommerceManifest): ParsedSandboxManifest {
        val payload = manifest.payload
        if (payload.isNullOrBlank()) {
            return ParsedSandboxManifest(emptyList(), malformed = false)
        }
        return try {
            val root = JsonParser.parseString(payload).asJsonObject
            val format = root.get("format")?.asString ?: manifest.format
            if (format.isNotBlank() && format != FORMAT) {
                return ParsedSandboxManifest(emptyList(), malformed = true)
            }
            val array = root.getAsJsonArray("servers") ?: JsonArray()
            val servers = ArrayList<ManifestRefreshPolicy.ServerIdentity>(array.size())
            for (element in array) {
                val item = element.asJsonObject
                val remarks = item.get("remarks")?.asString.orEmpty()
                val server = item.get("server")?.asString.orEmpty()
                val port = item.get("port")?.asString.orEmpty()
                if (remarks.isBlank() || server.isBlank() || port.isBlank()) {
                    return ParsedSandboxManifest(emptyList(), malformed = true)
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
            ParsedSandboxManifest(servers, malformed = false)
        } catch (_: Exception) {
            ParsedSandboxManifest(emptyList(), malformed = true)
        }
    }

    fun sandboxInventory(): List<ManifestRefreshPolicy.ServerIdentity> = listOf(
        ManifestRefreshPolicy.ServerIdentity(
            remarks = "Amsterdam",
            server = "ams.sandbox.hotfox.invalid",
            port = "443",
            protocol = "VLESS",
            network = "xhttp",
            security = "reality",
            fingerprint = "chrome",
        ),
        ManifestRefreshPolicy.ServerIdentity(
            remarks = "Frankfurt",
            server = "fra.sandbox.hotfox.invalid",
            port = "443",
            protocol = "VLESS",
            network = "xhttp",
            security = "reality",
            fingerprint = "chrome",
        ),
    )
}
