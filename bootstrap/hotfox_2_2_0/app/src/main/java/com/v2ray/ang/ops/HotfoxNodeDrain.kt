package com.v2ray.ang.ops

/**
 * Authoritative action to stop offering a node to new AUTO picks.
 * Existing manual selections stay sticky. Unsigned or expired metadata is ignored
 * and last-known-good drain set is kept.
 */
data class HotfoxDrainManifest(
    val schema: Int,
    val issuedAtEpochMs: Long,
    val expiresAtEpochMs: Long,
    val drainedGuids: Set<String>,
    val signatureHex: String,
) {
    fun canonicalPayload(): ByteArray {
        val body = buildString {
            append("drained=").append(drainedGuids.sorted().joinToString(",")).append('\n')
            append("expiresAtEpochMs=").append(expiresAtEpochMs).append('\n')
            append("issuedAtEpochMs=").append(issuedAtEpochMs).append('\n')
            append("schema=").append(schema).append('\n')
        }
        return body.toByteArray(Charsets.UTF_8)
    }
}

object HotfoxNodeDrain {
    const val SCHEMA = 1

    @Volatile
    private var lastGood: Set<String> = emptySet()

    fun resetForTests() {
        lastGood = emptySet()
    }

    fun activeGuids(): Set<String> = lastGood

    fun parse(raw: String?): HotfoxDrainManifest? {
        if (raw.isNullOrBlank()) return null
        val obj = raw.trim()
        if (!obj.startsWith("{")) return null
        val schema = HotfoxJsonFields.intValue(obj, "schema") ?: return null
        val issued = HotfoxJsonFields.longValue(obj, "issuedAtEpochMs") ?: return null
        val expires = HotfoxJsonFields.longValue(obj, "expiresAtEpochMs") ?: return null
        val signature = HotfoxJsonFields.string(obj, "signature").orEmpty()
        val drained = HotfoxJsonFields.stringList(obj, "drainedGuids")
        return HotfoxDrainManifest(schema, issued, expires, drained, signature)
    }

    fun apply(
        raw: String?,
        nowEpochMs: Long,
        publicKeySpkiHex: String,
    ): Set<String> {
        val parsed = parse(raw) ?: return lastGood
        if (parsed.schema != SCHEMA) return lastGood
        if (nowEpochMs > parsed.expiresAtEpochMs || nowEpochMs < parsed.issuedAtEpochMs) return lastGood
        if (publicKeySpkiHex.isBlank()) return lastGood
        if (!HotfoxOpsCrypto.verifyEcdsaP256(parsed.canonicalPayload(), parsed.signatureHex, publicKeySpkiHex)) {
            return lastGood
        }
        lastGood = parsed.drainedGuids.filter { it.isNotBlank() }.toSet()
        return lastGood
    }
}
