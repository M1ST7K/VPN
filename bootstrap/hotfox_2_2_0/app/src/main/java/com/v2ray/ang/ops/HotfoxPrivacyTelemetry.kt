package com.v2ray.ang.ops

/**
 * Privacy-safe operational counters. No browsing content, no secrets, no URLs.
 */
enum class HotfoxPrivacySafeEvent {
    REGION_UNHEALTHY,
    TRANSPORT_FAIL,
    BILLING_FAIL,
    UPDATE_VERIFY_FAIL,
    SHADOW_FALLBACK,
    CONTROL_PLANE_REJECT,
}

data class HotfoxPrivacySafeDatum(
    val event: HotfoxPrivacySafeEvent,
    val region: String? = null,
    val transport: String? = null,
)

object HotfoxPrivacyTelemetry {
    @Volatile
    private var counts: LinkedHashMap<HotfoxPrivacySafeEvent, Int> = LinkedHashMap()

    fun resetForTests() {
        counts = LinkedHashMap()
    }

    fun current(): Map<HotfoxPrivacySafeEvent, Int> = counts.toMap()

    fun record(datum: HotfoxPrivacySafeDatum): Boolean {
        if (!accept(datum)) return false
        counts[datum.event] = (counts[datum.event] ?: 0) + 1
        return true
    }

    fun accept(datum: HotfoxPrivacySafeDatum): Boolean {
        if (!safeToken(datum.region)) return false
        if (!safeToken(datum.transport)) return false
        return true
    }

    fun safeToken(value: String?): Boolean {
        if (value.isNullOrBlank()) return true
        val trimmed = value.trim()
        if (trimmed.length > 32) return false
        val lower = trimmed.lowercase()
        if (lower.contains("://") || lower.contains("vless") || lower.contains("uuid")) return false
        if (lower.contains("sk_live") || lower.contains("whsec") || lower.contains("password")) return false
        if (trimmed.contains('@') || trimmed.contains('/')) return false
        return trimmed.matches(Regex("[A-Za-z0-9._-]+"))
    }
}
