package com.v2ray.ang.commerce

import com.v2ray.ang.handler.MmkvManager

object MmkvCheckoutIntentStore : CheckoutIntentStore {
    override fun load(): CheckoutIntent? {
        val key = MmkvManager.decodeSettingsString(CommercePreferences.KEY_IDEMPOTENCY)?.takeIf { it.isNotBlank() }
            ?: return null
        val planId = MmkvManager.decodeSettingsString(CommercePreferences.KEY_LAST_PLAN_ID)?.takeIf { it.isNotBlank() }
            ?: return null
        val state = CommercePreferences.lastOrderState() ?: OrderState.CREATE_REQUESTED
        return CheckoutIntent(
            planId = planId,
            idempotencyKey = key,
            state = state,
            orderId = CommercePreferences.lastOrderId(),
        )
    }

    override fun save(intent: CheckoutIntent) {
        MmkvManager.encodeSettings(CommercePreferences.KEY_IDEMPOTENCY, intent.idempotencyKey)
        MmkvManager.encodeSettings(CommercePreferences.KEY_LAST_PLAN_ID, intent.planId)
        MmkvManager.encodeSettings(CommercePreferences.KEY_LAST_ORDER_STATE, intent.state.name)
        MmkvManager.encodeSettings(CommercePreferences.KEY_LAST_ORDER_ID, intent.orderId.orEmpty())
    }
}

object MmkvEntitlementMetadataStore : EntitlementMetadataStore {
    const val KEY_STATUS = "pref_hotfox_entitlement_status"
    const val KEY_STARTS = "pref_hotfox_entitlement_starts"
    const val KEY_EXPIRES = "pref_hotfox_entitlement_expires"
    const val KEY_PLAN = "pref_hotfox_entitlement_plan"
    const val KEY_ORDER = "pref_hotfox_entitlement_order"
    const val KEY_GRACE = "pref_hotfox_entitlement_grace"

    override fun read(): EntitlementMetadata? {
        val statusRaw = MmkvManager.decodeSettingsString(KEY_STATUS)?.takeIf { it.isNotBlank() } ?: return null
        val status = runCatching { EntitlementStatus.valueOf(statusRaw) }.getOrNull() ?: return null
        val starts = MmkvManager.decodeSettingsLong(KEY_STARTS, 0L)
        val expires = MmkvManager.decodeSettingsLong(KEY_EXPIRES, 0L)
        if (starts <= 0L || expires <= 0L) return null
        val planId = MmkvManager.decodeSettingsString(KEY_PLAN)?.takeIf { it.isNotBlank() } ?: return null
        val grace = MmkvManager.decodeSettingsLong(KEY_GRACE, 0L).takeIf { it > 0L }
        return EntitlementMetadata(
            status = status,
            startsAtEpochSeconds = starts,
            expiresAtEpochSeconds = expires,
            planId = planId,
            orderId = MmkvManager.decodeSettingsString(KEY_ORDER)?.takeIf { it.isNotBlank() },
            graceUntilEpochSeconds = grace,
        )
    }

    override fun write(metadata: EntitlementMetadata) {
        MmkvManager.encodeSettings(KEY_STATUS, metadata.status.name)
        MmkvManager.encodeSettings(KEY_STARTS, metadata.startsAtEpochSeconds)
        MmkvManager.encodeSettings(KEY_EXPIRES, metadata.expiresAtEpochSeconds)
        MmkvManager.encodeSettings(KEY_PLAN, metadata.planId)
        MmkvManager.encodeSettings(KEY_ORDER, metadata.orderId.orEmpty())
        MmkvManager.encodeSettings(KEY_GRACE, metadata.graceUntilEpochSeconds ?: 0L)
    }

    override fun clear() {
        MmkvManager.encodeSettings(KEY_STATUS, "")
        MmkvManager.encodeSettings(KEY_STARTS, 0L)
        MmkvManager.encodeSettings(KEY_EXPIRES, 0L)
        MmkvManager.encodeSettings(KEY_PLAN, "")
        MmkvManager.encodeSettings(KEY_ORDER, "")
        MmkvManager.encodeSettings(KEY_GRACE, 0L)
    }
}
