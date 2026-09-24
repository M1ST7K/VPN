package com.v2ray.ang.vpn

import java.util.Base64

/**
 * Subscription tab title must come from profile metadata, never from the secret URL/token.
 */
object HotfoxSubscriptionTitle {
    const val FALLBACK = "Подписка"
    const val GENERIC_IMPORT = "import sub"

    fun fromImport(fragment: String?): String {
        return resolve(fragment = fragment, profileTitle = null, contentDisposition = null, current = null)
    }

    fun shouldReplace(current: String?): Boolean {
        val value = current?.trim().orEmpty()
        if (value.isEmpty()) return true
        if (value.equals(GENERIC_IMPORT, ignoreCase = true)) return true
        return looksLikeSecretOrUrl(value)
    }

    fun resolve(
        fragment: String?,
        profileTitle: String?,
        contentDisposition: String?,
        current: String?,
    ): String {
        safeLabel(decodeFragment(fragment))?.let { return it }
        safeLabel(decodeProfileTitle(profileTitle))?.let { return it }
        safeLabel(filenameFromDisposition(contentDisposition))?.let { return it }
        if (!shouldReplace(current)) return current!!.trim()
        return FALLBACK
    }

    fun decodeProfileTitle(raw: String?): String? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        if (value.startsWith("base64:", ignoreCase = true)) {
            return decodeBase64Utf8(value.substringAfter(':').trim())
        }
        return value
    }

    fun filenameFromDisposition(raw: String?): String? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        val star = Regex("filename\\*=(?:UTF-8'')?([^;]+)", RegexOption.IGNORE_CASE).find(value)
        if (star != null) {
            return star.groupValues[1].trim().trim('"')
        }
        val plain = Regex("filename=\"?([^\";]+)\"?", RegexOption.IGNORE_CASE).find(value)
        return plain?.groupValues?.get(1)?.trim()?.trim('"')
    }

    private fun decodeFragment(fragment: String?): String? {
        val value = fragment?.trim().orEmpty()
        if (value.isEmpty()) return null
        return runCatching { java.net.URLDecoder.decode(value, Charsets.UTF_8.name()) }.getOrDefault(value)
    }

    private fun safeLabel(value: String?): String? {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        if (looksLikeSecretOrUrl(trimmed)) return null
        if (trimmed.length > 64) return trimmed.take(64)
        return trimmed
    }

    fun looksLikeSecretOrUrl(value: String): Boolean {
        val trimmed = value.trim()
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            return true
        }
        if (trimmed.contains("://")) return true
        if (trimmed.length >= 32 && trimmed.none { it.isWhitespace() } &&
            trimmed.all { it.isLetterOrDigit() || it == '-' || it == '_' || it == '/' || it == '+' || it == '=' }
        ) {
            return true
        }
        return false
    }

    fun decodeBase64Utf8(value: String): String? {
        val padded = value.replace('-', '+').replace('_', '/')
        val pad = (4 - padded.length % 4) % 4
        val raw = padded + "=".repeat(pad)
        val decoded = runCatching {
            String(android.util.Base64.decode(raw, android.util.Base64.DEFAULT), Charsets.UTF_8)
        }.getOrNull() ?: runCatching {
            String(Base64.getDecoder().decode(raw), Charsets.UTF_8)
        }.getOrNull()
        return decoded?.trim()?.takeIf { it.isNotEmpty() }
    }
}
