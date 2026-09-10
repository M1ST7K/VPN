package com.v2ray.ang.ops

/**
 * 2.x → 3.1 persisted-state migration. Preserve entitlement, manual subscriptions,
 * AUTO/manual intent and routing. New Autopilot/Shadow fields stay at safe defaults.
 */
data class HotfoxV2PersistedState(
    val autoMode: Boolean,
    val selectedGuid: String?,
    val routingMode: String?,
    val entitlementStatus: String?,
    val entitlementExpiresAtEpochSeconds: Long?,
    val hasManualSubscription: Boolean,
    val shadowAuto: Boolean? = null,
    val existingDeviceId: String? = null,
)

data class HotfoxV31PersistedState(
    val autoMode: Boolean,
    val selectedGuid: String?,
    val routingMode: String?,
    val entitlementStatus: String?,
    val entitlementExpiresAtEpochSeconds: Long?,
    val hasManualSubscription: Boolean,
    val shadowAuto: Boolean,
    val deviceId: String,
    val controlPlanePolicyVersion: Int,
    val schema: Int = HotfoxStateMigration.SCHEMA,
)

object HotfoxStateMigration {
    const val SCHEMA = 1

    fun migrate(from: HotfoxV2PersistedState): HotfoxV31PersistedState {
        val guid = from.selectedGuid?.takeIf { it.isNotBlank() }
        val deviceId = from.existingDeviceId?.takeIf { it.isNotBlank() && !HotfoxDeviceRegistry.looksLikeHardwareId(it) }
            ?: HotfoxDeviceRegistry.generateDeviceId()
        return HotfoxV31PersistedState(
            autoMode = from.autoMode,
            selectedGuid = guid,
            routingMode = from.routingMode?.takeIf { it.isNotBlank() } ?: "smart",
            entitlementStatus = from.entitlementStatus,
            entitlementExpiresAtEpochSeconds = from.entitlementExpiresAtEpochSeconds,
            hasManualSubscription = from.hasManualSubscription,
            shadowAuto = from.shadowAuto ?: false,
            deviceId = deviceId,
            controlPlanePolicyVersion = 0,
            schema = SCHEMA,
        )
    }
}
