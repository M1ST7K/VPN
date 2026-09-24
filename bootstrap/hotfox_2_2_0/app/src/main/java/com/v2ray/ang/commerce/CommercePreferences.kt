package com.v2ray.ang.commerce

import com.v2ray.ang.handler.MmkvManager
import java.util.UUID

/**
 * Non-sensitive commercial metadata. Entitlement/subscription tokens never live here.
 */
object CommercePreferences {
    const val KEY_INSTALL_ID = "pref_hotfox_install_id"
    const val KEY_ACCESS_ORIGIN = "pref_hotfox_access_origin"
    const val KEY_LAST_ORDER_ID = "pref_hotfox_last_order_id"
    const val KEY_LAST_ORDER_STATE = "pref_hotfox_last_order_state"
    const val KEY_LAST_PLAN_ID = "pref_hotfox_last_plan_id"
    const val KEY_BACKEND_AVAILABLE = "pref_hotfox_backend_available"
    const val KEY_LAST_ERROR = "pref_hotfox_commerce_last_error"
    const val KEY_MANIFEST_ATTEMPT = "pref_hotfox_manifest_last_attempt"
    const val KEY_MANIFEST_SUCCESS = "pref_hotfox_manifest_last_success"
    const val KEY_MANIFEST_ERROR = "pref_hotfox_manifest_last_error"
    const val KEY_CACHED_PLANS = "pref_hotfox_cached_plans_json"
    const val KEY_SELECTED_PLAN = "pref_hotfox_selected_plan_id"
    const val KEY_HOTFOX_SUB_ID = "pref_hotfox_managed_sub_id"
    const val KEY_PROMO_CODE = "pref_hotfox_promo_code"
    const val KEY_IDEMPOTENCY = "pref_hotfox_order_idempotency"

    const val ORIGIN_NONE = "NONE"
    const val ORIGIN_EXTERNAL = "EXTERNAL"
    const val ORIGIN_HOTFOX = "HOTFOX"

    fun installId(): String {
        val existing = MmkvManager.decodeSettingsString(KEY_INSTALL_ID).orEmpty()
        if (existing.isNotBlank()) return existing
        val created = UUID.randomUUID().toString()
        MmkvManager.encodeSettings(KEY_INSTALL_ID, created)
        return created
    }

    fun accessOrigin(): String = MmkvManager.decodeSettingsString(KEY_ACCESS_ORIGIN, ORIGIN_NONE) ?: ORIGIN_NONE

    fun setAccessOrigin(origin: String) {
        MmkvManager.encodeSettings(KEY_ACCESS_ORIGIN, origin)
    }

    fun lastOrderState(): OrderState? =
        MmkvManager.decodeSettingsString(KEY_LAST_ORDER_STATE)?.let {
            runCatching { OrderState.valueOf(it) }.getOrNull()
        }

    fun lastOrderId(): String? = MmkvManager.decodeSettingsString(KEY_LAST_ORDER_ID)?.takeIf { it.isNotBlank() }

    fun recordOrder(order: CommerceOrder) {
        MmkvManager.encodeSettings(KEY_LAST_ORDER_ID, order.id)
        MmkvManager.encodeSettings(KEY_LAST_ORDER_STATE, order.state.name)
        MmkvManager.encodeSettings(KEY_LAST_PLAN_ID, order.planId)
        MmkvManager.encodeSettings(KEY_IDEMPOTENCY, order.idempotencyKey)
    }

    fun clearTransientOrder() {
        MmkvManager.encodeSettings(KEY_LAST_ORDER_ID, "")
        MmkvManager.encodeSettings(KEY_LAST_ORDER_STATE, "")
    }

    fun setBackendAvailable(available: Boolean) {
        MmkvManager.encodeSettings(KEY_BACKEND_AVAILABLE, available)
    }

    fun backendAvailable(): Boolean = MmkvManager.decodeSettingsBool(KEY_BACKEND_AVAILABLE, true)

    fun recordError(error: String) {
        MmkvManager.encodeSettings(KEY_LAST_ERROR, error)
    }

    fun lastError(): String? = MmkvManager.decodeSettingsString(KEY_LAST_ERROR)?.takeIf { it.isNotBlank() }

    fun recordManifestAttempt(at: Long = System.currentTimeMillis()) {
        MmkvManager.encodeSettings(KEY_MANIFEST_ATTEMPT, at)
    }

    fun recordManifestSuccess(at: Long = System.currentTimeMillis()) {
        MmkvManager.encodeSettings(KEY_MANIFEST_SUCCESS, at)
        MmkvManager.encodeSettings(KEY_MANIFEST_ERROR, "")
    }

    fun recordManifestError(error: String, at: Long = System.currentTimeMillis()) {
        MmkvManager.encodeSettings(KEY_MANIFEST_ATTEMPT, at)
        MmkvManager.encodeSettings(KEY_MANIFEST_ERROR, error)
    }

    fun lastManifestAttempt(): Long = MmkvManager.decodeSettingsLong(KEY_MANIFEST_ATTEMPT, 0L)
    fun lastManifestSuccess(): Long = MmkvManager.decodeSettingsLong(KEY_MANIFEST_SUCCESS, 0L)
    fun lastManifestError(): String? = MmkvManager.decodeSettingsString(KEY_MANIFEST_ERROR)?.takeIf { it.isNotBlank() }

    fun cachedPlansJson(): String? = MmkvManager.decodeSettingsString(KEY_CACHED_PLANS)?.takeIf { it.isNotBlank() }

    fun cachePlansJson(json: String) {
        MmkvManager.encodeSettings(KEY_CACHED_PLANS, json)
    }

    fun selectedPlanId(): String? = MmkvManager.decodeSettingsString(KEY_SELECTED_PLAN)?.takeIf { it.isNotBlank() }

    fun setSelectedPlanId(id: String?) {
        MmkvManager.encodeSettings(KEY_SELECTED_PLAN, id.orEmpty())
    }

    fun hotfoxSubscriptionId(): String? =
        MmkvManager.decodeSettingsString(KEY_HOTFOX_SUB_ID)?.takeIf { it.isNotBlank() }

    fun setHotfoxSubscriptionId(id: String) {
        MmkvManager.encodeSettings(KEY_HOTFOX_SUB_ID, id)
    }

    fun isHotfoxManaged(subscriptionId: String): Boolean {
        if (accessOrigin() != ORIGIN_HOTFOX) return false
        val managed = hotfoxSubscriptionId()
        return managed.isNullOrBlank() || managed == subscriptionId
    }

    fun markExternalIfNeeded() {
        if (accessOrigin() == ORIGIN_NONE) setAccessOrigin(ORIGIN_EXTERNAL)
    }

    fun pendingIdempotencyKey(planId: String): String {
        val pending = lastOrderState()
        val reusable = pending != null &&
            pending != OrderState.FAILED &&
            pending != OrderState.CANCELLED &&
            pending != OrderState.EXPIRED &&
            pending != OrderState.REFUNDED &&
            pending != OrderState.FULFILLED &&
            MmkvManager.decodeSettingsString(KEY_LAST_PLAN_ID) == planId
        val existing = MmkvManager.decodeSettingsString(KEY_IDEMPOTENCY)?.takeIf { it.isNotBlank() }
        if (reusable && existing != null) return existing
        return UUID.randomUUID().toString()
    }
}
