package com.v2ray.ang.commerce

/**
 * Subscription-screen presentation states for HotFox 2.3.
 * Each failure class has its own value; do not reuse UNKNOWN for all errors.
 */
enum class CommercialPresentationState {
    NO_ACCESS,
    EXTERNAL_ACTIVE,
    HOTFOX_ACTIVE,
    EXPIRING_SOON,
    EXPIRED,
    PAYMENT_PENDING,
    PAYMENT_FAILED,
    PAYMENT_CANCELLED,
    ENTITLEMENT_PROVISIONING,
    ENTITLEMENT_ACTIVE_SYNC_FAILED,
    BACKEND_UNAVAILABLE,
    RESTORE_REQUIRED,
}
