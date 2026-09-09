package com.v2ray.ang.ops

enum class HotfoxComponentHealth {
    OK,
    DEGRADED,
    DOWN,
    UNKNOWN,
}

data class HotfoxIncidentBanner(
    val title: String,
    val expiresAtEpochMs: Long,
) {
    fun visible(nowEpochMs: Long): Boolean =
        title.isNotBlank() && nowEpochMs <= expiresAtEpochMs

    companion object {
        fun sanitizeTitle(raw: String): String =
            raw.replace(Regex("[<>\\r\\n]"), " ").replace(Regex("\\s+"), " ").trim().take(120)
    }
}

data class HotfoxServiceStatus(
    val api: HotfoxComponentHealth,
    val billing: HotfoxComponentHealth,
    val entitlement: HotfoxComponentHealth,
    val manifest: HotfoxComponentHealth,
    val nodes: HotfoxComponentHealth,
    val shadowEntries: HotfoxComponentHealth,
    val incident: HotfoxIncidentBanner?,
    val issuedAtEpochMs: Long,
    val expiresAtEpochMs: Long,
) {
    fun fresh(nowEpochMs: Long): Boolean =
        nowEpochMs <= expiresAtEpochMs && nowEpochMs >= issuedAtEpochMs
}

object HotfoxServiceHealth {
    fun parse(raw: String?, nowEpochMs: Long): HotfoxServiceStatus? {
        if (raw.isNullOrBlank()) return null
        val obj = raw.trim()
        if (!obj.startsWith("{")) return null
        val issued = HotfoxJsonFields.longValue(obj, "issuedAtEpochMs") ?: return null
        val expires = HotfoxJsonFields.longValue(obj, "expiresAtEpochMs") ?: return null
        val incidentTitle = HotfoxIncidentBanner.sanitizeTitle(
            HotfoxJsonFields.string(obj, "incidentTitle").orEmpty(),
        )
        val incidentExpires = HotfoxJsonFields.longValue(obj, "incidentExpiresAtEpochMs") ?: 0L
        val banner = if (incidentTitle.isNotBlank()) {
            HotfoxIncidentBanner(incidentTitle, incidentExpires)
        } else {
            null
        }
        val status = HotfoxServiceStatus(
            api = health(obj, "api"),
            billing = health(obj, "billing"),
            entitlement = health(obj, "entitlement"),
            manifest = health(obj, "manifest"),
            nodes = health(obj, "nodes"),
            shadowEntries = health(obj, "shadowEntries"),
            incident = banner,
            issuedAtEpochMs = issued,
            expiresAtEpochMs = expires,
        )
        if (!status.fresh(nowEpochMs)) return null
        // Incident/health is display-only. This parser never writes VpnSessionCoordinator.
        return status
    }

    private fun health(obj: String, key: String): HotfoxComponentHealth =
        when (HotfoxJsonFields.string(obj, key)?.lowercase()) {
            "ok" -> HotfoxComponentHealth.OK
            "degraded" -> HotfoxComponentHealth.DEGRADED
            "down" -> HotfoxComponentHealth.DOWN
            else -> HotfoxComponentHealth.UNKNOWN
        }
}

object HotfoxRemoteFlags {
    const val SCHEMA = 1

    private val allowlist = setOf(
        "ops.incident_banner",
        "ops.update_check",
        "ops.node_drain",
    )
    private val denylist = setOf(
        "vpn.skip_tls",
        "vpn.disable_reality",
        "vpn.allow_insecure",
        "commerce.trust_checkout",
    )

    @Volatile
    private var lastGood: Map<String, Boolean> = emptyMap()

    fun resetForTests() {
        lastGood = emptyMap()
    }

    fun current(): Map<String, Boolean> = lastGood

    fun get(key: String, default: Boolean): Boolean = lastGood[key] ?: default

    fun canonicalPayload(
        flags: Map<String, Boolean>,
        issuedAtEpochMs: Long,
        expiresAtEpochMs: Long,
    ): ByteArray {
        val body = buildString {
            append("expiresAtEpochMs=").append(expiresAtEpochMs).append('\n')
            flags.toSortedMap().forEach { (key, value) ->
                append(key).append('=').append(value).append('\n')
            }
            append("issuedAtEpochMs=").append(issuedAtEpochMs).append('\n')
            append("schema=").append(SCHEMA).append('\n')
        }
        return body.toByteArray(Charsets.UTF_8)
    }

    fun apply(raw: String?, nowEpochMs: Long, publicKeySpkiHex: String): Map<String, Boolean> {
        if (raw.isNullOrBlank()) return lastGood
        val obj = raw.trim()
        if (!obj.startsWith("{")) return lastGood
        val schema = HotfoxJsonFields.intValue(obj, "schema") ?: return lastGood
        if (schema != SCHEMA) return lastGood
        val expires = HotfoxJsonFields.longValue(obj, "expiresAtEpochMs") ?: return lastGood
        val issued = HotfoxJsonFields.longValue(obj, "issuedAtEpochMs") ?: 0L
        if (nowEpochMs > expires || nowEpochMs < issued) return lastGood
        if (publicKeySpkiHex.isBlank()) return lastGood
        if (denylist.any { HotfoxJsonFields.raw(obj, it) != null }) {
            return lastGood
        }
        val parsed = LinkedHashMap<String, Boolean>()
        allowlist.forEach { key ->
            val token = HotfoxJsonFields.raw(obj, key) ?: return@forEach
            parsed[key] = token.equals("true", ignoreCase = true)
        }
        val signature = HotfoxJsonFields.string(obj, "signature").orEmpty()
        if (!HotfoxOpsCrypto.verifyEcdsaP256(canonicalPayload(parsed, issued, expires), signature, publicKeySpkiHex)) {
            return lastGood
        }
        lastGood = parsed
        return lastGood
    }
}
