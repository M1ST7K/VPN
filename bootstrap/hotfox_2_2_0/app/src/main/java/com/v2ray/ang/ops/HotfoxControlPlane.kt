package com.v2ray.ang.ops

/**
 * Signed HotFox control-plane metadata: inventory, capacity, maintenance, weight,
 * region, role and policy version. Invalid/expired signatures fail closed and keep
 * last-known-good. A control-plane outage does not wipe that cache.
 */
enum class HotfoxLoadBucket {
    LOW,
    MEDIUM,
    HIGH,
    SATURATED,
    UNKNOWN,
}

enum class HotfoxNodeRole {
    EXIT,
    ENTRY,
    SHADOW,
    UNKNOWN,
}

data class HotfoxControlNode(
    val guid: String,
    val region: String,
    val weight: Int,
    val load: HotfoxLoadBucket,
    val maintenance: Boolean,
    val drained: Boolean,
    val role: HotfoxNodeRole,
) {
    fun unavailableForAuto(): Boolean = maintenance || drained
}

data class HotfoxControlSnapshot(
    val schema: Int,
    val issuedAtEpochMs: Long,
    val expiresAtEpochMs: Long,
    val policyVersion: Int,
    val minClientProtocol: Int,
    val nodes: List<HotfoxControlNode>,
    val signatureHex: String,
    val stale: Boolean = false,
) {
    fun node(guid: String): HotfoxControlNode? = nodes.firstOrNull { it.guid == guid }

    fun maintenanceGuids(): Set<String> =
        nodes.filter { it.maintenance }.map { it.guid }.toSet()

    fun drainedGuids(): Set<String> =
        nodes.filter { it.drained }.map { it.guid }.toSet()

    fun unavailableForAutoGuids(): Set<String> =
        nodes.filter { it.unavailableForAuto() }.map { it.guid }.toSet()

    fun canonicalPayload(): ByteArray = HotfoxControlPlane.canonicalPayload(
        nodes = nodes,
        issuedAtEpochMs = issuedAtEpochMs,
        expiresAtEpochMs = expiresAtEpochMs,
        policyVersion = policyVersion,
        minClientProtocol = minClientProtocol,
        schema = schema,
    )
}

object HotfoxControlPlane {
    const val SCHEMA = 1
    const val STALE_GRACE_MS = 24L * 60L * 60L * 1000L
    const val LOAD_LOW_PENALTY_MS = 0.0
    const val LOAD_MEDIUM_PENALTY_MS = 55.0
    const val LOAD_HIGH_PENALTY_MS = 140.0
    const val LOAD_SATURATED_PENALTY_MS = 320.0
    const val MAX_WEIGHT = 10
    const val WEIGHT_UNIT_MS = 6.0

    @Volatile
    private var lastGood: HotfoxControlSnapshot? = null

    fun resetForTests() {
        lastGood = null
    }

    fun current(): HotfoxControlSnapshot? = lastGood

    fun policyVersion(): Int = lastGood?.policyVersion ?: 0

    fun maintenanceGuids(): Set<String> = lastGood?.maintenanceGuids().orEmpty()

    fun unavailableForAutoGuids(): Set<String> = lastGood?.unavailableForAutoGuids().orEmpty()

    fun loadOf(guid: String): HotfoxLoadBucket =
        lastGood?.node(guid)?.load ?: HotfoxLoadBucket.UNKNOWN

    fun weightOf(guid: String): Int =
        lastGood?.node(guid)?.weight?.coerceIn(1, MAX_WEIGHT) ?: 1

    fun loadPenaltyMs(guid: String): Double =
        when (loadOf(guid)) {
            HotfoxLoadBucket.LOW -> LOAD_LOW_PENALTY_MS
            HotfoxLoadBucket.MEDIUM -> LOAD_MEDIUM_PENALTY_MS
            HotfoxLoadBucket.HIGH -> LOAD_HIGH_PENALTY_MS
            HotfoxLoadBucket.SATURATED -> LOAD_SATURATED_PENALTY_MS
            HotfoxLoadBucket.UNKNOWN -> LOAD_LOW_PENALTY_MS
        }

    fun weightBiasMs(guid: String): Double {
        val weight = weightOf(guid)
        return (MAX_WEIGHT - weight) * WEIGHT_UNIT_MS
    }

    fun canonicalPayload(
        nodes: List<HotfoxControlNode>,
        issuedAtEpochMs: Long,
        expiresAtEpochMs: Long,
        policyVersion: Int,
        minClientProtocol: Int,
        schema: Int = SCHEMA,
    ): ByteArray {
        val body = buildString {
            append("expiresAtEpochMs=").append(expiresAtEpochMs).append('\n')
            append("issuedAtEpochMs=").append(issuedAtEpochMs).append('\n')
            append("minClientProtocol=").append(minClientProtocol).append('\n')
            append("nodes=").append(
                nodes.sortedBy { it.guid }.joinToString(";") { node ->
                    listOf(
                        node.guid,
                        node.region,
                        node.weight.toString(),
                        node.load.name,
                        node.maintenance.toString(),
                        node.drained.toString(),
                        node.role.name,
                    ).joinToString(":")
                },
            ).append('\n')
            append("policyVersion=").append(policyVersion).append('\n')
            append("schema=").append(schema).append('\n')
        }
        return body.toByteArray(Charsets.UTF_8)
    }

    fun parse(raw: String?): HotfoxControlSnapshot? {
        if (raw.isNullOrBlank()) return null
        val obj = raw.trim()
        if (!obj.startsWith("{")) return null
        val schema = HotfoxJsonFields.intValue(obj, "schema") ?: return null
        val issued = HotfoxJsonFields.longValue(obj, "issuedAtEpochMs") ?: return null
        val expires = HotfoxJsonFields.longValue(obj, "expiresAtEpochMs") ?: return null
        val policyVersion = HotfoxJsonFields.intValue(obj, "policyVersion") ?: return null
        val minClient = HotfoxJsonFields.intValue(obj, "minClientProtocol") ?: 1
        val signature = HotfoxJsonFields.string(obj, "signature").orEmpty()
        val nodes = HotfoxJsonFields.objectArray(obj, "nodes").mapNotNull { parseNode(it) }
        return HotfoxControlSnapshot(
            schema = schema,
            issuedAtEpochMs = issued,
            expiresAtEpochMs = expires,
            policyVersion = policyVersion,
            minClientProtocol = minClient,
            nodes = nodes,
            signatureHex = signature,
        )
    }

    fun apply(
        raw: String?,
        nowEpochMs: Long,
        publicKeySpkiHex: String,
        clientProtocol: Int = HotfoxApiCompatibility.CLIENT_PROTOCOL,
    ): HotfoxControlSnapshot? {
        if (raw.isNullOrBlank()) {
            return lastGood?.copy(stale = isStale(lastGood, nowEpochMs))
        }
        val parsed = parse(raw) ?: return keepLast(nowEpochMs)
        if (parsed.schema != SCHEMA) return keepLast(nowEpochMs)
        if (nowEpochMs < parsed.issuedAtEpochMs) return keepLast(nowEpochMs)
        if (nowEpochMs > parsed.expiresAtEpochMs) return keepLast(nowEpochMs)
        if (publicKeySpkiHex.isBlank()) return keepLast(nowEpochMs)
        if (!HotfoxOpsCrypto.verifyEcdsaP256(parsed.canonicalPayload(), parsed.signatureHex, publicKeySpkiHex)) {
            return keepLast(nowEpochMs)
        }
        when (HotfoxApiCompatibility.evaluate(clientProtocol, parsed.minClientProtocol)) {
            HotfoxApiCompat.OK, HotfoxApiCompat.UNKNOWN_FIELDS_OK -> Unit
            HotfoxApiCompat.CLIENT_TOO_OLD, HotfoxApiCompat.SERVER_TOO_OLD -> return keepLast(nowEpochMs)
        }
        lastGood = parsed.copy(stale = false)
        return lastGood
    }

    fun usableForAuto(nowEpochMs: Long): Boolean {
        val snap = lastGood ?: return false
        if (nowEpochMs < snap.issuedAtEpochMs) return false
        return nowEpochMs <= snap.expiresAtEpochMs + STALE_GRACE_MS
    }

    private fun keepLast(nowEpochMs: Long): HotfoxControlSnapshot? =
        lastGood?.copy(stale = isStale(lastGood, nowEpochMs))

    private fun isStale(snapshot: HotfoxControlSnapshot?, nowEpochMs: Long): Boolean {
        if (snapshot == null) return false
        return nowEpochMs > snapshot.expiresAtEpochMs
    }

    private fun parseNode(obj: String): HotfoxControlNode? {
        val guid = HotfoxJsonFields.string(obj, "guid")?.trim().orEmpty()
        if (guid.isBlank()) return null
        val region = HotfoxJsonFields.string(obj, "region")?.trim().orEmpty().ifBlank { "unknown" }
        val weight = (HotfoxJsonFields.intValue(obj, "weight") ?: 1).coerceIn(1, MAX_WEIGHT)
        val load = when (HotfoxJsonFields.string(obj, "loadBucket")?.uppercase()) {
            "LOW" -> HotfoxLoadBucket.LOW
            "MEDIUM" -> HotfoxLoadBucket.MEDIUM
            "HIGH" -> HotfoxLoadBucket.HIGH
            "SATURATED" -> HotfoxLoadBucket.SATURATED
            else -> HotfoxLoadBucket.UNKNOWN
        }
        val maintenance = HotfoxJsonFields.boolValue(obj, "maintenance") ?: false
        val drained = HotfoxJsonFields.boolValue(obj, "drained") ?: false
        val role = when (HotfoxJsonFields.string(obj, "role")?.uppercase()) {
            "EXIT" -> HotfoxNodeRole.EXIT
            "ENTRY" -> HotfoxNodeRole.ENTRY
            "SHADOW" -> HotfoxNodeRole.SHADOW
            else -> HotfoxNodeRole.UNKNOWN
        }
        return HotfoxControlNode(guid, region, weight, load, maintenance, drained, role)
    }
}
