package com.v2ray.ang.vpn

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
        "publicKey=${if (publicKeyPresent) "[REDACTED]" else ""}",
        "shortId=${if (shortIdPresent) "[REDACTED]" else ""}",
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
