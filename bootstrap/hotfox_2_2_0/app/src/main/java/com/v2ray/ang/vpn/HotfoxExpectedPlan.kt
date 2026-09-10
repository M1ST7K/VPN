package com.v2ray.ang.vpn

import android.content.Context
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.v2ray.ang.core.CoreConfigContextBuilder
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.handler.MmkvManager

/**
 * Secret-safe expected outbound plan for composite profiles.
 *
 * CUSTOM uses the stored raw Xray JSON. POLICYGROUP / PROXYCHAIN emit a
 * synthetic outbound list from the same resolved members/hops that
 * [CoreConfigContextBuilder] feeds into config generation.
 */
object HotfoxExpectedPlan {
    fun forSelected(context: Context, guid: String, profile: ProfileItem): String? {
        return when (profile.configType) {
            EConfigType.CUSTOM -> MmkvManager.decodeServerRaw(guid)?.takeIf { it.isNotBlank() }
            EConfigType.POLICYGROUP, EConfigType.PROXYCHAIN -> {
                val members = CoreConfigContextBuilder.build(context, guid)
                    ?.resolvedOutbounds
                    ?.firstOrNull()
                    ?.resolvedProfiles
                    .orEmpty()
                if (members.isEmpty()) null else fromProfiles(members)
            }
            else -> null
        }
    }

    fun fromProfiles(members: List<ProfileItem>): String {
        val root = JsonObject()
        val outbounds = JsonArray()
        for (member in members) {
            outbounds.add(outboundFromProfile(member))
        }
        root.add("outbounds", outbounds)
        return root.toString()
    }

    fun outboundFromProfile(profile: ProfileItem): JsonObject {
        val snap = HotfoxOutboundCompare.fromProfile(profile)
        val outbound = JsonObject()
        outbound.addProperty("protocol", snap.protocol)
        val settings = JsonObject()
        val endpoint = JsonObject()
        endpoint.addProperty("address", snap.address)
        if (snap.port != null) {
            endpoint.addProperty("port", snap.port)
        }
        val useVnext = snap.protocol.equals("vless", ignoreCase = true) ||
            snap.protocol.equals("vmess", ignoreCase = true)
        if (useVnext) {
            val vnext = JsonArray()
            vnext.add(endpoint)
            settings.add("vnext", vnext)
        } else {
            val servers = JsonArray()
            servers.add(endpoint)
            settings.add("servers", servers)
        }
        outbound.add("settings", settings)
        val stream = JsonObject()
        if (snap.network.isNotBlank()) {
            stream.addProperty("network", snap.network)
        }
        if (snap.security.isNotBlank()) {
            stream.addProperty("security", snap.security)
        }
        if (snap.reality || snap.publicKeyPresent) {
            val reality = JsonObject()
            if (snap.sni.isNotBlank()) {
                reality.addProperty("serverName", snap.sni)
            }
            if (snap.fingerprint.isNotBlank()) {
                reality.addProperty("fingerprint", snap.fingerprint)
            }
            if (snap.publicKeyPresent) {
                reality.addProperty("publicKey", HotfoxOutboundSanitizer.REDACTED)
            }
            if (snap.shortIdPresent) {
                reality.addProperty("shortId", HotfoxOutboundSanitizer.REDACTED)
            }
            stream.add("realitySettings", reality)
        } else if (snap.sni.isNotBlank() || snap.security.equals("tls", ignoreCase = true)) {
            val tls = JsonObject()
            if (snap.sni.isNotBlank()) {
                tls.addProperty("serverName", snap.sni)
            }
            if (snap.fingerprint.isNotBlank()) {
                tls.addProperty("fingerprint", snap.fingerprint)
            }
            stream.add("tlsSettings", tls)
        }
        if (snap.path.isNotBlank() || snap.host.isNotBlank()) {
            val ws = JsonObject()
            if (snap.path.isNotBlank()) {
                ws.addProperty("path", snap.path)
            }
            if (snap.host.isNotBlank()) {
                val headers = JsonObject()
                headers.addProperty("Host", snap.host)
                ws.add("headers", headers)
            }
            stream.add("wsSettings", ws)
        }
        if (snap.serviceName.isNotBlank()) {
            val grpc = JsonObject()
            grpc.addProperty("serviceName", snap.serviceName)
            stream.add("grpcSettings", grpc)
        }
        if (stream.entrySet().isNotEmpty()) {
            outbound.add("streamSettings", stream)
        }
        return outbound
    }
}
