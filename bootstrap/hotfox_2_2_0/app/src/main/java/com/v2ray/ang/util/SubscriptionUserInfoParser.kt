package com.v2ray.ang.util

import com.v2ray.ang.dto.entities.SubscriptionUserInfo
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object SubscriptionUserInfoParser {
    private val HEADER_NAMES = listOf(
        "subscription-userinfo",
        "Subscription-Userinfo",
        "profile-userinfo",
        "profile-expire",
        "profile-expires",
        "expire",
    )

    private val DATE_FORMATS = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("dd.MM.yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
    )

    fun parse(header: String?): SubscriptionUserInfo? = parse(header, null)

    fun parse(header: String?, body: String?): SubscriptionUserInfo? {
        val fromHeader = parseKeyValues(header)
        val fromBody = parseKeyValues(extractBodyUserInfo(body))
        val expireHeader = parseStandaloneExpire(header)
        val merged = LinkedHashMap<String, Long>()
        merged.putAll(fromBody)
        merged.putAll(fromHeader)
        if (expireHeader != null && (merged["expire"] ?: 0L) <= 0L) {
            merged["expire"] = expireHeader
        }
        if (merged.keys.none { it in SUPPORTED_KEYS } && expireHeader == null) return null
        return SubscriptionUserInfo(
            uploadBytes = merged["upload"] ?: 0L,
            downloadBytes = merged["download"] ?: 0L,
            totalBytes = merged["total"] ?: 0L,
            expireAtEpochSeconds = merged["expire"] ?: expireHeader ?: 0L,
        )
    }

    fun collectHeaders(lookup: (String) -> String?): String? {
        val parts = ArrayList<String>()
        for (name in HEADER_NAMES) {
            val value = lookup(name)?.trim().orEmpty()
            if (value.isEmpty()) continue
            if (name.equals("subscription-userinfo", true) ||
                name.equals("profile-userinfo", true)
            ) {
                parts.add(value)
            } else if (name.contains("expire", true) || name == "expire") {
                parts.add("expire=$value")
            } else {
                parts.add(value)
            }
        }
        return parts.firstOrNull { it.contains('=') } ?: parts.firstOrNull()
    }

    private fun extractBodyUserInfo(body: String?): String? {
        if (body.isNullOrBlank()) return null
        val lines = body.lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("#") || it.startsWith("//") }
            .map { it.removePrefix("#").removePrefix("//").trim() }
            .toList()
        for (line in lines) {
            val lowered = line.lowercase()
            if (lowered.startsWith("subscription-userinfo:")) {
                return line.substringAfter(':').trim()
            }
            if (lowered.startsWith("subscription-userinfo=")) {
                return line.substringAfter('=').trim()
            }
            if (lowered.startsWith("expire:") || lowered.startsWith("expire=")) {
                return "expire=${line.substringAfter(':').substringAfter('=').trim()}"
            }
        }
        return null
    }

    private fun parseKeyValues(raw: String?): Map<String, Long> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split(';', ',', '\n')
            .mapNotNull { part ->
                val pair = part.trim().split('=', limit = 2)
                if (pair.size != 2) return@mapNotNull null
                val key = pair[0].trim().lowercase()
                val parsed = if (key == "expire") {
                    parseNumberOrDate(pair[1].trim()) ?: return@mapNotNull null
                } else {
                    pair[1].trim().toLongOrNull()?.takeIf { it >= 0 } ?: return@mapNotNull null
                }
                key to parsed
            }
            .toMap()
    }

    private fun parseStandaloneExpire(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        if (raw.contains('=')) return null
        return parseNumberOrDate(raw.trim())
    }

    private fun parseNumberOrDate(value: String): Long? {
        val numeric = value.toLongOrNull()?.takeIf { it >= 0 } ?: run {
            return parseDate(value)
        }
        return normalizeEpochSeconds(numeric)
    }

    fun normalizeEpochSeconds(value: Long): Long {
        if (value <= 0L) return 0L
        // Milliseconds (after ~2001-09-09 in ms).
        if (value > 10_000_000_000L) return value / 1000L
        return value
    }

    private fun parseDate(value: String): Long? {
        val trimmed = value.trim().trim('"')
        runCatching { Instant.parse(trimmed).epochSecond }.getOrNull()?.let { return it }
        for (fmt in DATE_FORMATS) {
            try {
                val date = LocalDate.parse(trimmed, fmt)
                return date.atStartOfDay().toEpochSecond(ZoneOffset.UTC)
            } catch (_: DateTimeParseException) {
            }
        }
        return null
    }

    private val SUPPORTED_KEYS = setOf("upload", "download", "total", "expire")
}
