package com.v2ray.ang.commerce

data class EntitlementMetadata(
    val status: EntitlementStatus,
    val startsAtEpochSeconds: Long,
    val expiresAtEpochSeconds: Long,
    val planId: String,
    val orderId: String?,
    val graceUntilEpochSeconds: Long? = null,
)

interface EntitlementMetadataStore {
    fun read(): EntitlementMetadata?
    fun write(metadata: EntitlementMetadata)
    fun clear()
}

class InMemoryEntitlementMetadataStore : EntitlementMetadataStore {
    @Volatile
    var metadata: EntitlementMetadata? = null

    override fun read(): EntitlementMetadata? = metadata

    override fun write(metadata: EntitlementMetadata) {
        this.metadata = metadata
    }

    override fun clear() {
        metadata = null
    }
}

object EntitlementParser {
    fun fromFields(
        entitlementId: String?,
        customerId: String?,
        source: String?,
        planId: String?,
        status: String?,
        startsAtEpochSeconds: Long?,
        expiresAtEpochSeconds: Long?,
        orderId: String?,
        credentialVersion: Int? = 1,
        graceUntilEpochSeconds: Long? = null,
        revocationReason: String? = null,
    ): CommerceResult<CommerceEntitlement> {
        if (entitlementId.isNullOrBlank() || planId.isNullOrBlank()) {
            return CommerceResult.Err(CommerceError.INVALID, "missing_entitlement_identity")
        }
        if (status.isNullOrBlank()) {
            return CommerceResult.Err(CommerceError.INVALID, "missing_entitlement_status")
        }
        val parsedStatus = runCatching { EntitlementStatus.valueOf(status) }.getOrNull()
            ?: return CommerceResult.Err(CommerceError.INVALID, "unknown_entitlement_status")
        if (parsedStatus == EntitlementStatus.NONE) {
            return CommerceResult.Err(CommerceError.INVALID, "none_entitlement_status")
        }
        if (startsAtEpochSeconds == null || startsAtEpochSeconds <= 0L) {
            return CommerceResult.Err(CommerceError.INVALID, "missing_entitlement_start")
        }
        if (expiresAtEpochSeconds == null || expiresAtEpochSeconds <= 0L) {
            return CommerceResult.Err(CommerceError.INVALID, "missing_entitlement_expiry")
        }
        if (expiresAtEpochSeconds <= startsAtEpochSeconds) {
            return CommerceResult.Err(CommerceError.INVALID, "inverted_entitlement_window")
        }
        if (graceUntilEpochSeconds != null && graceUntilEpochSeconds < expiresAtEpochSeconds) {
            return CommerceResult.Err(CommerceError.INVALID, "invalid_grace_boundary")
        }
        val parsedSource = source?.let { runCatching { EntitlementSource.valueOf(it) }.getOrNull() }
            ?: EntitlementSource.HOTFOX
        return CommerceResult.Ok(
            CommerceEntitlement(
                entitlementId = entitlementId,
                customerId = customerId.orEmpty(),
                source = parsedSource,
                planId = planId,
                status = parsedStatus,
                startsAtEpochSeconds = startsAtEpochSeconds,
                expiresAtEpochSeconds = expiresAtEpochSeconds,
                orderId = orderId?.takeIf { it.isNotBlank() },
                credentialVersion = credentialVersion ?: 1,
                graceUntilEpochSeconds = graceUntilEpochSeconds,
                revocationReason = revocationReason,
            ),
        )
    }

    fun effectiveStatus(entitlement: CommerceEntitlement, nowEpochSeconds: Long): EntitlementStatus {
        if (entitlement.status == EntitlementStatus.REVOKED) return EntitlementStatus.REVOKED
        if (entitlement.status == EntitlementStatus.EXPIRED) return EntitlementStatus.EXPIRED
        if (entitlement.status == EntitlementStatus.PROVISIONING) return EntitlementStatus.PROVISIONING
        if (nowEpochSeconds < entitlement.startsAtEpochSeconds) {
            return EntitlementStatus.PROVISIONING
        }
        if (nowEpochSeconds > entitlement.expiresAtEpochSeconds) {
            val grace = entitlement.graceUntilEpochSeconds
            if (grace != null && nowEpochSeconds <= grace) return EntitlementStatus.GRACE
            return EntitlementStatus.EXPIRED
        }
        return entitlement.status
    }

    fun remainingDays(expiresAtEpochSeconds: Long, nowEpochSeconds: Long): Int =
        ((expiresAtEpochSeconds - nowEpochSeconds) / 86_400L).toInt()

    fun metadataOf(entitlement: CommerceEntitlement): EntitlementMetadata =
        EntitlementMetadata(
            status = entitlement.status,
            startsAtEpochSeconds = entitlement.startsAtEpochSeconds,
            expiresAtEpochSeconds = entitlement.expiresAtEpochSeconds,
            planId = entitlement.planId,
            orderId = entitlement.orderId,
            graceUntilEpochSeconds = entitlement.graceUntilEpochSeconds,
        )
}
