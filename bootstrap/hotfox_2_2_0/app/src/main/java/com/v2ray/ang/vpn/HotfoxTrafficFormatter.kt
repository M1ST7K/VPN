package com.v2ray.ang.vpn

import java.util.Locale

/** Compact session traffic labels. Never invents values. */
object HotfoxTrafficFormatter {
    fun formatBytes(bytes: Long?): String {
        if (bytes == null) return "—"
        val safe = if (bytes < 0L) 0L else bytes
        val locale = Locale.US
        return when {
            safe < 1000L -> "$safe B"
            safe < 1_000_000L -> String.format(locale, "%.1f KB", safe / 1000.0)
            safe < 1_000_000_000L -> String.format(locale, "%.1f MB", safe / 1_000_000.0)
            else -> String.format(locale, "%.2f GB", safe / 1_000_000_000.0)
        }
    }

    fun maskSubscriptionUrl(url: String?): String {
        val raw = url?.trim().orEmpty()
        if (raw.isEmpty()) return "—"
        return try {
            val uri = java.net.URI(raw)
            val host = uri.host?.takeIf { it.isNotBlank() } ?: return "https://••••••••"
            val scheme = uri.scheme ?: "https"
            "$scheme://$host/••••••••"
        } catch (_: Exception) {
            "https://••••••••"
        }
    }
}
