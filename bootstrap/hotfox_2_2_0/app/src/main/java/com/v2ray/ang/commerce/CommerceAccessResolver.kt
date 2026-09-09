package com.v2ray.ang.commerce

/**
 * Maps local facts to a single commercial presentation state.
 * Manual import remains allowed in every state, including BACKEND_UNAVAILABLE.
 */
object CommerceAccessResolver {
    const val EXPIRING_SOON_DAYS = 7

    fun resolve(facts: CommerceFacts): CommercialPresentationState {
        if (facts.credentialUnreadable) {
            return CommercialPresentationState.RESTORE_REQUIRED
        }

        when (facts.lastOrderState) {
            OrderState.CREATE_REQUESTED,
            OrderState.CREATED,
            OrderState.CHECKOUT_OPEN,
            OrderState.PENDING,
            -> return CommercialPresentationState.PAYMENT_PENDING
            OrderState.FAILED -> return CommercialPresentationState.PAYMENT_FAILED
            OrderState.CANCELLED -> return CommercialPresentationState.PAYMENT_CANCELLED
            OrderState.PAID,
            OrderState.ENTITLEMENT_PROVISIONING,
            OrderState.FULFILLED,
            -> if (!facts.hasHotfoxEntitlement) {
                return CommercialPresentationState.ENTITLEMENT_PROVISIONING
            }
            else -> Unit
        }

        if (facts.hasHotfoxEntitlement) {
            when (facts.entitlementStatus) {
                EntitlementStatus.PROVISIONING ->
                    return CommercialPresentationState.ENTITLEMENT_PROVISIONING
                EntitlementStatus.EXPIRED, EntitlementStatus.REVOKED ->
                    return CommercialPresentationState.EXPIRED
                EntitlementStatus.ACTIVE, EntitlementStatus.GRACE -> {
                    if (facts.lastSyncFailed) {
                        return CommercialPresentationState.ENTITLEMENT_ACTIVE_SYNC_FAILED
                    }
                    val remaining = facts.entitlementRemainingDays
                    if (remaining != null && remaining <= EXPIRING_SOON_DAYS) {
                        return CommercialPresentationState.EXPIRING_SOON
                    }
                    return CommercialPresentationState.HOTFOX_ACTIVE
                }
                else -> Unit
            }
        }

        if (facts.hasExternalSubscription) {
            if (facts.externalExpired) return CommercialPresentationState.EXPIRED
            val remaining = facts.remainingDays
            if (remaining != null && remaining <= EXPIRING_SOON_DAYS) {
                return CommercialPresentationState.EXPIRING_SOON
            }
            return CommercialPresentationState.EXTERNAL_ACTIVE
        }

        if (!facts.backendAvailable) {
            return CommercialPresentationState.BACKEND_UNAVAILABLE
        }
        return CommercialPresentationState.NO_ACCESS
    }

    fun manualImportAllowed(@Suppress("UNUSED_PARAMETER") facts: CommerceFacts): Boolean = true

    fun showPremiumOnboarding(state: CommercialPresentationState): Boolean =
        state == CommercialPresentationState.NO_ACCESS ||
            state == CommercialPresentationState.BACKEND_UNAVAILABLE ||
            state == CommercialPresentationState.PAYMENT_PENDING ||
            state == CommercialPresentationState.PAYMENT_FAILED ||
            state == CommercialPresentationState.PAYMENT_CANCELLED ||
            state == CommercialPresentationState.RESTORE_REQUIRED ||
            state == CommercialPresentationState.ENTITLEMENT_PROVISIONING

    fun labelKey(state: CommercialPresentationState): String = when (state) {
        CommercialPresentationState.NO_ACCESS -> "Нет доступа"
        CommercialPresentationState.EXTERNAL_ACTIVE -> "Внешняя подписка"
        CommercialPresentationState.HOTFOX_ACTIVE -> "HotFox Premium"
        CommercialPresentationState.EXPIRING_SOON -> "Скоро истекает"
        CommercialPresentationState.EXPIRED -> "Истекла"
        CommercialPresentationState.PAYMENT_PENDING -> "Оплата ожидается"
        CommercialPresentationState.PAYMENT_FAILED -> "Оплата не прошла"
        CommercialPresentationState.PAYMENT_CANCELLED -> "Оплата отменена"
        CommercialPresentationState.ENTITLEMENT_PROVISIONING -> "Выдаём доступ"
        CommercialPresentationState.ENTITLEMENT_ACTIVE_SYNC_FAILED -> "Доступ есть, синхронизация не удалась"
        CommercialPresentationState.BACKEND_UNAVAILABLE -> "Коммерческий сервис недоступен"
        CommercialPresentationState.RESTORE_REQUIRED -> "Нужно восстановить доступ"
    }
}
