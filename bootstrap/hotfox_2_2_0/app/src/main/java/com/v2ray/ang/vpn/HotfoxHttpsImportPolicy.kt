package com.v2ray.ang.vpn

object HotfoxHttpsImportPolicy {
    fun isHttpsSubscriptionUrl(raw: String?): Boolean {
        val value = raw?.trim().orEmpty()
        return value.startsWith("https://", ignoreCase = true) && value.length > "https://".length
    }
}
