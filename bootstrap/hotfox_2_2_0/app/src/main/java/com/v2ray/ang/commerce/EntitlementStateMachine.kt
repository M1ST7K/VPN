package com.v2ray.ang.commerce

object EntitlementStateMachine {
    fun canTransition(from: EntitlementStatus, to: EntitlementStatus): Boolean {
        if (from == to) return true
        return when (from) {
            EntitlementStatus.NONE -> to == EntitlementStatus.PROVISIONING || to == EntitlementStatus.ACTIVE
            EntitlementStatus.PROVISIONING -> to == EntitlementStatus.ACTIVE || to == EntitlementStatus.REVOKED
            EntitlementStatus.ACTIVE -> to == EntitlementStatus.GRACE ||
                to == EntitlementStatus.EXPIRED ||
                to == EntitlementStatus.REVOKED
            EntitlementStatus.GRACE -> to == EntitlementStatus.EXPIRED || to == EntitlementStatus.REVOKED
            EntitlementStatus.EXPIRED, EntitlementStatus.REVOKED -> false
        }
    }

    fun isUsable(status: EntitlementStatus): Boolean =
        status == EntitlementStatus.ACTIVE || status == EntitlementStatus.GRACE
}
