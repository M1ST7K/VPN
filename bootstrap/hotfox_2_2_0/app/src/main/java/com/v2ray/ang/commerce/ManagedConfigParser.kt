package com.v2ray.ang.commerce

import com.google.gson.JsonParser
import com.v2ray.ang.AppConfig
import com.v2ray.ang.core.CoreOutboundBuilder
import com.v2ray.ang.dto.V2rayConfig.OutboundBean
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.fmt.Hysteria2Fmt
import com.v2ray.ang.fmt.ShadowsocksFmt
import com.v2ray.ang.fmt.SocksFmt
import com.v2ray.ang.fmt.TrojanFmt
import com.v2ray.ang.fmt.VlessFmt
import com.v2ray.ang.fmt.VmessFmt
import com.v2ray.ang.fmt.WireguardFmt
import com.v2ray.ang.util.Utils

/**
 * Parses HotFox-managed payloads into complete, Xray-usable [ProfileItem]s.
 * Identity-only JSON is never turned into a VPN profile.
 */
object ManagedConfigParser {
    private val shareLinkParsers: List<Pair<String, (String) -> ProfileItem?>> = listOf(
        EConfigType.VMESS.protocolScheme to VmessFmt::parse,
        EConfigType.SHADOWSOCKS.protocolScheme to ShadowsocksFmt::parse,
        EConfigType.SOCKS.protocolScheme to SocksFmt::parse,
        AppConfig.SOCKS4 to SocksFmt::parse,
        AppConfig.SOCKS5 to SocksFmt::parse,
        EConfigType.TROJAN.protocolScheme to TrojanFmt::parse,
        EConfigType.VLESS.protocolScheme to VlessFmt::parse,
        EConfigType.WIREGUARD.protocolScheme to WireguardFmt::parse,
        EConfigType.HYSTERIA2.protocolScheme to Hysteria2Fmt::parse,
        AppConfig.HY2 to Hysteria2Fmt::parse,
    )

    sealed class Result {
        data class Complete(val profiles: List<ProfileItem>) : Result()
        data class Incomplete(val error: String = "incomplete_manifest") : Result()
        data class Malformed(val error: String = "malformed_manifest") : Result()
        data object Empty : Result()
    }

    fun parse(body: String?, subid: String): Result {
        if (body.isNullOrBlank()) return Result.Empty
        val trimmed = body.trim()
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return parseJsonPayload(trimmed)
        }
        val profiles = parseShareLinks(body, subid)
        if (profiles.isEmpty()) {
            return if (looksLikeShareLink(trimmed)) {
                Result.Incomplete()
            } else {
                Result.Malformed()
            }
        }
        if (profiles.any { !isXrayUsable(it) }) {
            return Result.Incomplete()
        }
        return Result.Complete(profiles)
    }

    fun parseShareLinks(body: String, subid: String): List<ProfileItem> {
        val attempts = LinkedHashSet<String>()
        attempts.add(body)
        val decoded = Utils.decode(body)
        if (decoded.isNotBlank()) {
            attempts.add(decoded)
        }
        for (source in attempts) {
            val configs = parseLines(source, subid)
            if (configs.isNotEmpty()) return configs
        }
        return emptyList()
    }

    fun isXrayUsable(profile: ProfileItem): Boolean {
        if (profile.server.isNullOrBlank()) return false
        val port = profile.serverPort?.toIntOrNull() ?: return false
        if (port <= 0) return false
        if (CoreOutboundBuilder.createInitOutbound(profile.configType) == null) return false
        return when (profile.configType) {
            EConfigType.SOCKS, EConfigType.HTTP -> true
            EConfigType.SHADOWSOCKS ->
                !profile.password.isNullOrBlank() && !profile.method.isNullOrBlank()
            EConfigType.VMESS, EConfigType.VLESS -> {
                if (profile.password.isNullOrBlank()) return false
                if (profile.security.equals("reality", ignoreCase = true) &&
                    profile.publicKey.isNullOrBlank()
                ) {
                    return false
                }
                if (profile.network.equals("xhttp", ignoreCase = true) &&
                    profile.path.isNullOrBlank()
                ) {
                    return false
                }
                true
            }
            EConfigType.TROJAN, EConfigType.HYSTERIA2 -> !profile.password.isNullOrBlank()
            EConfigType.WIREGUARD ->
                !profile.secretKey.isNullOrBlank() && !profile.publicKey.isNullOrBlank()
            else -> false
        }
    }

    fun xrayOutbound(profile: ProfileItem): OutboundBean? {
        if (!isXrayUsable(profile)) return null
        runCatching { CoreOutboundBuilder.convert(profile) }.getOrNull()?.let { return it }
        val outbound = CoreOutboundBuilder.createInitOutbound(profile.configType) ?: return null
        when (profile.configType) {
            EConfigType.SOCKS, EConfigType.HTTP, EConfigType.SHADOWSOCKS, EConfigType.TROJAN -> {
                outbound.settings?.servers?.firstOrNull()?.let { server ->
                    server.address = profile.server.orEmpty()
                    server.port = profile.serverPort!!.toInt()
                    server.password = profile.password
                    server.method = profile.method
                }
            }
            EConfigType.VMESS, EConfigType.VLESS -> {
                outbound.settings?.vnext?.firstOrNull()?.let { vnext ->
                    vnext.address = profile.server.orEmpty()
                    vnext.port = profile.serverPort!!.toInt()
                    vnext.users[0].id = profile.password.orEmpty()
                    vnext.users[0].encryption = profile.method
                    vnext.users[0].flow = profile.flow
                }
            }
            EConfigType.WIREGUARD -> {
                outbound.settings?.let { wireguard ->
                    wireguard.secretKey = profile.secretKey
                    wireguard.peers?.firstOrNull()?.publicKey = profile.publicKey.orEmpty()
                }
            }
            EConfigType.HYSTERIA2 -> {
                outbound.settings?.let { server ->
                    server.address = profile.server
                    server.port = profile.serverPort!!.toInt()
                }
            }
            else -> return null
        }
        return outbound
    }

    private fun parseJsonPayload(trimmed: String): Result {
        return try {
            val element = JsonParser.parseString(trimmed)
            if (!element.isJsonObject) {
                return Result.Malformed()
            }
            val root = element.asJsonObject
            val format = root.get("format")?.asString.orEmpty()
            val servers = root.get("servers")?.takeIf { it.isJsonArray }?.asJsonArray
            if (format == ManagedManifestParser.FORMAT) {
                return if (servers == null || servers.size() == 0) {
                    Result.Empty
                } else {
                    Result.Incomplete()
                }
            }
            if (root.has("inbounds") || root.has("outbounds")) {
                return Result.Malformed("unsupported_manifest")
            }
            if (servers != null) {
                return if (servers.size() == 0) Result.Empty else Result.Incomplete()
            }
            Result.Malformed()
        } catch (_: Exception) {
            Result.Malformed()
        }
    }

    private fun parseLines(source: String, subid: String): List<ProfileItem> {
        return source.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .mapNotNull { line -> parseShareLine(line, subid) }
    }

    private fun parseShareLine(str: String, subid: String): ProfileItem? {
        val config = shareLinkParsers.firstNotNullOfOrNull { (scheme, parser) ->
            if (str.startsWith(scheme, ignoreCase = true)) parser(str) else null
        } ?: return null
        config.subscriptionId = subid
        return config
    }

    private fun looksLikeShareLink(body: String): Boolean {
        return body.lineSequence().any { line ->
            val trimmed = line.trim()
            shareLinkParsers.any { trimmed.startsWith(it.first, ignoreCase = true) }
        }
    }
}
