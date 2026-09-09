package com.v2ray.ang.util

/** Removes credentials and endpoint material before anything reaches logcat. */
object SecretRedactor {
    private val proxyUri = Regex("(?i)\\b(?:vless|vmess|trojan|ss|socks|hysteria2?|hy2|wireguard)://\\S+")
    private val webUri = Regex("(?i)https?://\\S+")
    private val jsonSecret = Regex(
        "(?i)(\\\"(?:id|uuid|password|publicKey|shortId|privateKey|secretKey|url|address)\\\"\\s*:\\s*\\\")[^\\\"]*(\\\")"
    )
    private val querySecret = Regex("(?i)(?:pbk|sid|id|uuid|token|key|password)=([^&\\s]+)")
    private val authorization = Regex("(?i)(authorization\\s*[:=]\\s*)\\S+")
    private val uuid = Regex("(?i)\\b[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}\\b")
    private val keystore = Regex("(?i)(?:HOTFOX_KEYSTORE_PASSWORD|HOTFOX_KEY_PASSWORD|storePassword|keyPassword)\\s*[=:]\\s*\\S+")
    private val longToken = Regex("(?i)\\b[0-9a-z_/+-]{24,}={0,2}\\b")

    fun redact(value: String): String = value
        .replace(proxyUri, "<secret-uri>")
        .replace(webUri, "<url>")
        .replace(jsonSecret, "\$1<redacted>\$2")
        .replace(querySecret) { it.value.substringBefore('=') + "=<redacted>" }
        .replace(authorization, "\$1<redacted>")
        .replace(keystore, "<signing-secret>")
        .replace(uuid, "<uuid>")
        .replace(longToken, "<secret>")

    fun throwable(source: Throwable): Throwable {
        val safeMessage = source.message?.let(::redact).orEmpty()
        return SecurityException("${source.javaClass.simpleName}: $safeMessage").also {
            it.stackTrace = source.stackTrace
        }
    }
}
