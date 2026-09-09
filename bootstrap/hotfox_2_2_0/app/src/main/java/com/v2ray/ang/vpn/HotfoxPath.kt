package com.v2ray.ang.vpn

/**
 * 2.6 connection path: server + transport + security + optional entry/exit.
 *
 * Only transports the shipped Xray build can actually configure are advertised.
 * Fallback never downgrades TLS/REALITY to increase success rate.
 */
enum class HotfoxTransport {
    TCP,
    RAW,
    WS,
    GRPC,
    XHTTP,
    ;

    val storageValue: String
        get() = name.lowercase()

    companion object {
        fun parse(raw: String?): HotfoxTransport? = when (raw?.trim()?.lowercase()) {
            "tcp" -> TCP
            "raw" -> RAW
            "ws", "websocket" -> WS
            "grpc" -> GRPC
            "xhttp" -> XHTTP
            else -> null
        }
    }
}

enum class HotfoxSecurity {
    TLS,
    REALITY,
    NONE,
    ;

    companion object {
        fun parse(raw: String?): HotfoxSecurity = when (raw?.trim()?.lowercase()) {
            "reality" -> REALITY
            "tls", "xtls" -> TLS
            else -> NONE
        }
    }
}

enum class PathRole {
    EXIT,
    SHADOW_ENTRY,
    SHADOW_EXIT,
    STEALTH,
    ;

    companion object {
        fun fromRemarks(remarks: String?): PathRole {
            val text = remarks.orEmpty().lowercase()
            return when {
                text.contains("[entry]") || text.contains("shadow-entry") -> SHADOW_ENTRY
                text.contains("[exit]") || text.contains("shadow-exit") -> SHADOW_EXIT
                text.contains("[stealth]") || text.contains("shadow") -> STEALTH
                else -> EXIT
            }
        }
    }
}

enum class AddressFamily {
    IPV4,
    IPV6,
    DUAL,
}

data class ConnectionPath(
    val id: String,
    val serverGuid: String,
    val transport: HotfoxTransport,
    val security: HotfoxSecurity,
    val family: AddressFamily = AddressFamily.IPV4,
    val role: PathRole = PathRole.EXIT,
    val entryGuid: String? = null,
    val exitGuid: String? = null,
    val remarks: String = "",
) {
    val isMultihop: Boolean = !entryGuid.isNullOrBlank() && !exitGuid.isNullOrBlank()
}

object HotfoxPathFactory {
    fun fromCandidate(
        guid: String,
        network: String?,
        security: String?,
        remarks: String?,
        ipv6: Boolean = false,
    ): ConnectionPath? {
        val transport = HotfoxTransport.parse(network) ?: return null
        return ConnectionPath(
            id = "$guid:${transport.storageValue}",
            serverGuid = guid,
            transport = transport,
            security = HotfoxSecurity.parse(security),
            family = if (ipv6) AddressFamily.DUAL else AddressFamily.IPV4,
            role = PathRole.fromRemarks(remarks),
            remarks = remarks.orEmpty(),
        )
    }

    fun shadowRoute(entry: ConnectionPath, exit: ConnectionPath): ConnectionPath? {
        if (!HotfoxShadowRoute.validate(entry, exit)) return null
        return ConnectionPath(
            id = "shadow:${entry.serverGuid}->${exit.serverGuid}",
            serverGuid = entry.serverGuid,
            transport = entry.transport,
            security = entry.security,
            family = entry.family,
            role = PathRole.SHADOW_ENTRY,
            entryGuid = entry.serverGuid,
            exitGuid = exit.serverGuid,
            remarks = "${entry.remarks} → ${exit.remarks}",
        )
    }
}

object HotfoxShadowRoute {
    fun validate(entry: ConnectionPath, exit: ConnectionPath): Boolean {
        if (entry.serverGuid.isBlank() || exit.serverGuid.isBlank()) return false
        if (entry.serverGuid == exit.serverGuid) return false
        if (entry.role != PathRole.SHADOW_ENTRY && entry.role != PathRole.STEALTH) return false
        if (exit.role != PathRole.SHADOW_EXIT && exit.role != PathRole.EXIT) return false
        if (entry.security == HotfoxSecurity.NONE && exit.security == HotfoxSecurity.REALITY) {
            return false
        }
        return true
    }
}

object HotfoxAddressFamilyPolicy {
    fun prefer(ipv4Usable: Boolean, ipv6Usable: Boolean): AddressFamily {
        if (ipv4Usable && ipv6Usable) return AddressFamily.DUAL
        if (ipv6Usable && !ipv4Usable) return AddressFamily.IPV6
        return AddressFamily.IPV4
    }

    /**
     * A dead IPv6 observation must not hide a working IPv4 protected path.
     * IPv6 still fail-closes on TUN unless the existing 2.2/2.5 capture policy
     * explicitly routes it.
     */
    fun usableFamily(ipv4Usable: Boolean, ipv6Usable: Boolean): AddressFamily? {
        if (!ipv4Usable && !ipv6Usable) return null
        return prefer(ipv4Usable, ipv6Usable)
    }
}
