package com.v2ray.ang.commerce

data class CommercePlan(
    val id: String,
    val displayName: String,
    val subtitle: String = "",
    val durationDays: Int,
    val priceMinor: Long,
    val currency: String,
    val oldPriceMinor: Long? = null,
    val badge: String? = null,
    val features: List<String> = emptyList(),
    val maxDevices: Int? = null,
    val isRecommended: Boolean = false,
    val availability: PlanAvailability = PlanAvailability.AVAILABLE,
    val sortOrder: Int = 0,
    val renewalType: RenewalType = RenewalType.MANUAL,
    val version: Int = 1,
)

enum class PlanAvailability { AVAILABLE, UNAVAILABLE, HIDDEN }

enum class RenewalType { MANUAL, PROVIDER_RECURRING }

data class CreateOrderRequest(
    val idempotencyKey: String,
    val planId: String,
    val installId: String,
    val promoCode: String? = null,
)

enum class OrderState {
    CREATE_REQUESTED,
    CREATED,
    CHECKOUT_OPEN,
    PENDING,
    PAID,
    ENTITLEMENT_PROVISIONING,
    FULFILLED,
    CANCELLED,
    FAILED,
    EXPIRED,
    REFUND_PENDING,
    REFUNDED,
}

data class CommerceOrder(
    val id: String,
    val planId: String,
    val state: OrderState,
    val checkoutUrl: String? = null,
    val idempotencyKey: String,
    val createdAtEpochSeconds: Long,
    val paidAtEpochSeconds: Long? = null,
    val providerPaymentId: String? = null,
    val failureReason: String? = null,
)

enum class EntitlementStatus {
    NONE,
    PROVISIONING,
    ACTIVE,
    GRACE,
    REVOKED,
    EXPIRED,
}

data class CommerceEntitlement(
    val entitlementId: String,
    val customerId: String,
    val source: EntitlementSource,
    val planId: String,
    val status: EntitlementStatus,
    val startsAtEpochSeconds: Long,
    val expiresAtEpochSeconds: Long,
    val orderId: String?,
    val credentialVersion: Int = 1,
    val graceUntilEpochSeconds: Long? = null,
    val revocationReason: String? = null,
)

enum class EntitlementSource { HOTFOX, EXTERNAL }

data class RestoreRequest(
    val installId: String,
    val recoveryCode: String? = null,
    val providerTransactionId: String? = null,
)

data class CommerceManifest(
    val format: String,
    val payload: String? = null,
    val subscriptionUrl: String? = null,
)

data class PromoQuote(
    val code: String,
    val planId: String,
    val finalPriceMinor: Long,
    val currency: String,
    val valid: Boolean,
    val reason: String? = null,
)

sealed class CommerceResult<out T> {
    data class Ok<T>(val value: T) : CommerceResult<T>()
    data class Err(val kind: CommerceError, val message: String = kind.name) : CommerceResult<Nothing>()
}

enum class CommerceError {
    BACKEND_UNAVAILABLE,
    UNAUTHORIZED,
    NOT_FOUND,
    CONFLICT,
    INVALID,
    REJECTED,
}

data class CommerceFacts(
    val hasExternalSubscription: Boolean,
    val externalExpired: Boolean,
    val remainingDays: Int?,
    val hasHotfoxEntitlement: Boolean,
    val entitlementStatus: EntitlementStatus?,
    val entitlementRemainingDays: Int?,
    val lastOrderState: OrderState?,
    val backendAvailable: Boolean,
    val credentialUnreadable: Boolean,
    val lastSyncFailed: Boolean,
    val hasLocalServers: Boolean,
)
