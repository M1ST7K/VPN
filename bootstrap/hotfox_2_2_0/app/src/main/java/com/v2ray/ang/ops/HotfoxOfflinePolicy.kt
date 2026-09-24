package com.v2ray.ang.ops

import com.v2ray.ang.commerce.EntitlementStatus

/**
 * Partial-outage rules. Billing outage never fabricates entitlement.
 * Control-plane outage never wipes last-known-good servers.
 * Cached signed metadata remains AUTO-usable only within TTL + grace.
 */
object HotfoxOfflinePolicy {
    fun metadataUsable(
        expiresAtEpochMs: Long,
        nowEpochMs: Long,
        graceMs: Long = HotfoxControlPlane.STALE_GRACE_MS,
    ): Boolean {
        if (nowEpochMs <= 0L || expiresAtEpochMs <= 0L) return false
        return nowEpochMs <= expiresAtEpochMs + graceMs
    }

    fun preserveServersOnControlPlaneOutage(): Boolean = true

    fun fabricateEntitlementOnBillingOutage(): Boolean = false

    fun entitlementContinues(
        status: EntitlementStatus?,
        expiresAtEpochSeconds: Long?,
        nowEpochSeconds: Long,
        billingDown: Boolean,
    ): Boolean {
        if (status == null) return false
        if (status == EntitlementStatus.REVOKED || status == EntitlementStatus.NONE) return false
        if (status == EntitlementStatus.EXPIRED) return false
        val expiry = expiresAtEpochSeconds ?: return false
        if (nowEpochSeconds >= expiry) return false
        if (billingDown && status != EntitlementStatus.ACTIVE && status != EntitlementStatus.GRACE) {
            return false
        }
        return status == EntitlementStatus.ACTIVE || status == EntitlementStatus.GRACE
    }

    fun wipeServersOnUnsignedControlPlane(): Boolean = false
}
