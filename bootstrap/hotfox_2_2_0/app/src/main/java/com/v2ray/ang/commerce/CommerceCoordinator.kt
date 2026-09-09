package com.v2ray.ang.commerce

import android.content.Context
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.util.JsonUtil
import com.v2ray.ang.vpn.HotfoxSubscriptionPresentation

/**
 * UI-facing commercial operations. Payment truth comes only from backend entitlement/order reads.
 */
class CommerceCoordinator(
    private val backend: HotfoxCommerceBackend,
    private val secrets: SecretStore,
    private val nowEpochSeconds: () -> Long = { System.currentTimeMillis() / 1000L },
) {
    suspend fun loadPlans(): List<CommercePlan> {
        return when (val result = backend.listPlans()) {
            is CommerceResult.Ok -> {
                CommercePreferences.setBackendAvailable(true)
                CommercePreferences.cachePlansJson(JsonUtil.toJson(result.value))
                result.value
            }
            is CommerceResult.Err -> {
                CommercePreferences.setBackendAvailable(false)
                CommercePreferences.recordError(result.kind.name)
                cachedPlans()
            }
        }
    }

    fun cachedPlans(): List<CommercePlan> {
        val json = CommercePreferences.cachedPlansJson() ?: return emptyList()
        return JsonUtil.fromJsonSafeList(json)
    }

    suspend fun startCheckout(planId: String, promoCode: String? = null): CommerceResult<CommerceOrder> {
        val request = CreateOrderRequest(
            idempotencyKey = CommercePreferences.pendingIdempotencyKey(planId),
            planId = planId,
            installId = CommercePreferences.installId(),
            promoCode = promoCode,
        )
        return when (val result = backend.createOrder(request)) {
            is CommerceResult.Ok -> {
                CommercePreferences.recordOrder(order = result.value)
                CommercePreferences.setBackendAvailable(true)
                result
            }
            is CommerceResult.Err -> {
                CommercePreferences.setBackendAvailable(result.kind != CommerceError.BACKEND_UNAVAILABLE)
                CommercePreferences.recordError(result.kind.name)
                result
            }
        }
    }

    suspend fun quotePromo(planId: String, code: String): PromoQuote? {
        return when (val result = backend.validatePromo(code, planId)) {
            is CommerceResult.Ok -> result.value
            is CommerceResult.Err -> null
        }
    }

    /**
     * Browser return: extract order id for polling only. Never grant entitlement from the URI.
     */
    suspend fun handleCheckoutReturn(uriString: String?): CommerceResult<CommerceOrder?> {
        // isPaidProof is always false; keep the read so a future change cannot silently grant.
        if (CheckoutReturnParser.isPaidProof(uriString)) {
            return CommerceResult.Err(CommerceError.REJECTED, "browser_return_is_not_payment_truth")
        }
        val orderId = CheckoutReturnParser.extractOrderId(uriString) ?: CommercePreferences.lastOrderId()
        if (orderId.isNullOrBlank()) return CommerceResult.Ok(null)
        if (backend is SandboxCommerceBackend) {
            backend.noteCheckoutReturn(orderId)
        }
        return when (val result = backend.getOrder(orderId)) {
            is CommerceResult.Ok -> {
                CommercePreferences.recordOrder(result.value)
                result
            }
            is CommerceResult.Err -> result
        }
    }

    suspend fun refreshEntitlement(): CommerceResult<CommerceEntitlement?> {
        val credential = when (val stored = secrets.get(SecretKeys.ENTITLEMENT_CREDENTIAL)) {
            is SecretGetResult.Value -> stored.utf8()
            is SecretGetResult.Missing -> null
            is SecretGetResult.Corrupt, is SecretGetResult.KeystoreInvalidated -> {
                return CommerceResult.Err(CommerceError.UNAUTHORIZED, "RESTORE_REQUIRED")
            }
        }
        return when (val result = backend.getEntitlement(credential)) {
            is CommerceResult.Ok -> {
                result.value?.let { storeEntitlement(it) }
                result
            }
            is CommerceResult.Err -> result
        }
    }

    suspend fun restoreAccess(recoveryCode: String?, providerTransactionId: String?): CommerceResult<CommerceEntitlement> {
        return when (
            val result = backend.restore(
                RestoreRequest(
                    installId = CommercePreferences.installId(),
                    recoveryCode = recoveryCode,
                    providerTransactionId = providerTransactionId,
                ),
            )
        ) {
            is CommerceResult.Ok -> {
                storeEntitlement(result.value)
                result
            }
            is CommerceResult.Err -> result
        }
    }

    fun storeEntitlement(entitlement: CommerceEntitlement) {
        secrets.putUtf8(SecretKeys.ENTITLEMENT_CREDENTIAL, entitlement.entitlementId)
        CommercePreferences.setAccessOrigin(CommercePreferences.ORIGIN_HOTFOX)
        CommercePreferences.clearTransientOrder()
    }

    fun collectFacts(): CommerceFacts {
        val credentialState = secrets.get(SecretKeys.ENTITLEMENT_CREDENTIAL)
        val credentialUnreadable =
            credentialState is SecretGetResult.Corrupt || credentialState is SecretGetResult.KeystoreInvalidated
        val entitlement = when (credentialState) {
            is SecretGetResult.Value -> {
                // Local presence of a HotFox credential is not payment proof; expiry still comes
                // from last trusted entitlement metadata stored as non-secret timestamps if present.
                true
            }
            else -> false
        }
        val subscriptions = MmkvManager.decodeSubscriptions()
        val subscription = subscriptions.firstOrNull { it.subscription.url.isNotBlank() || it.subscription.remarks.isNotBlank() }
            ?.subscription
        val serverCount = MmkvManager.decodeAllServerList().size
        val shown = HotfoxSubscriptionPresentation.fromExpiryEpochSeconds(
            expireAtEpochSeconds = subscription?.expireAtEpochSeconds,
            serverCount = serverCount,
        )
        val remaining = shown.remainingDays
        val origin = CommercePreferences.accessOrigin()
        val hasHotfox = origin == CommercePreferences.ORIGIN_HOTFOX &&
            credentialState is SecretGetResult.Value
        val hasExternal = subscription != null && origin != CommercePreferences.ORIGIN_HOTFOX &&
            (subscription.url.isNotBlank() || serverCount > 0)
        return CommerceFacts(
            hasExternalSubscription = hasExternal,
            externalExpired = shown.status == com.v2ray.ang.vpn.SubscriptionPresentation.Status.EXPIRED,
            remainingDays = remaining,
            hasHotfoxEntitlement = hasHotfox,
            entitlementStatus = if (hasHotfox) EntitlementStatus.ACTIVE else EntitlementStatus.NONE,
            entitlementRemainingDays = remaining.takeIf { hasHotfox },
            lastOrderState = CommercePreferences.lastOrderState()?.takeIf {
                !OrderStateMachine.isPaidTruth(it) && !hasHotfox
            },
            backendAvailable = backend.available && CommercePreferences.backendAvailable(),
            credentialUnreadable = credentialUnreadable,
            lastSyncFailed = CommercePreferences.lastManifestError() != null &&
                hasHotfox,
            hasLocalServers = serverCount > 0,
        )
    }

    fun presentation(): CommercialPresentationState = CommerceAccessResolver.resolve(collectFacts())

    fun secretUnreadableRequiresRestore(): Boolean {
        val result = secrets.get(SecretKeys.ENTITLEMENT_CREDENTIAL)
        return result is SecretGetResult.Corrupt || result is SecretGetResult.KeystoreInvalidated
    }

    companion object {
        @Volatile
        private var instance: CommerceCoordinator? = null

        fun get(context: Context): CommerceCoordinator {
            instance?.let { return it }
            return synchronized(this) {
                instance ?: CommerceCoordinator(
                    backend = HotfoxCommerceFactory.create(),
                    secrets = AndroidKeystoreSecretStore(context.applicationContext),
                ).also { instance = it }
            }
        }

        fun replaceForTests(coordinator: CommerceCoordinator) {
            instance = coordinator
        }
    }
}

private fun JsonUtil.fromJsonSafeList(json: String): List<CommercePlan> {
    return try {
        val array = com.google.gson.JsonParser.parseString(json).asJsonArray
        array.mapNotNull { element ->
            fromJson(element.toString(), CommercePlan::class.java)
        }
    } catch (_: Exception) {
        emptyList()
    }
}
