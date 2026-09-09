package com.v2ray.ang.commerce

import android.content.Context
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.util.JsonUtil
import com.v2ray.ang.vpn.HotfoxSubscriptionPresentation
import com.v2ray.ang.vpn.SubscriptionPresentation
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * UI-facing commercial operations. Payment truth comes only from backend entitlement/order reads.
 */
class CommerceCoordinator(
    private val backend: HotfoxCommerceBackend,
    private val secrets: SecretStore,
    private val intents: CheckoutIntentStore = MmkvCheckoutIntentStore,
    private val metadata: EntitlementMetadataStore = MmkvEntitlementMetadataStore,
    private val inventory: ManagedServerStore = InMemoryManagedServerStore(),
    private val fetchManagedUrl: (String) -> String? = { null },
    private val installId: () -> String = { CommercePreferences.installId() },
    private val nowEpochSeconds: () -> Long = { System.currentTimeMillis() / 1000L },
) {
    private val checkoutLock = Mutex()

    @Volatile
    private var cachedPresentation: CommercialPresentationState = CommercialPresentationState.NO_ACCESS

    @Volatile
    private var localOrigin: String? = null

    fun presentationSnapshot(): CommercialPresentationState = cachedPresentation

    suspend fun refreshPresentation(): CommercialPresentationState {
        val state = CommerceAccessResolver.resolve(collectFacts())
        cachedPresentation = state
        return state
    }

    suspend fun loadPlans(): List<CommercePlan> {
        return when (val result = backend.listPlans()) {
            is CommerceResult.Ok -> {
                runCatching { CommercePreferences.setBackendAvailable(true) }
                runCatching { CommercePreferences.cachePlansJson(JsonUtil.toJson(result.value)) }
                result.value
            }
            is CommerceResult.Err -> {
                runCatching {
                    CommercePreferences.setBackendAvailable(false)
                    CommercePreferences.recordError(result.kind.name)
                }
                cachedPlans()
            }
        }
    }

    fun cachedPlans(): List<CommercePlan> {
        val json = runCatching { CommercePreferences.cachedPlansJson() }.getOrNull() ?: return emptyList()
        return JsonUtil.fromJsonSafeList(json)
    }

    suspend fun startCheckout(planId: String, promoCode: String? = null): CommerceResult<CommerceOrder> {
        return checkoutLock.withLock {
            val intent = CheckoutIdempotency.reuseOrMint(intents.load(), planId) { UUID.randomUUID().toString() }
            intents.save(intent.copy(state = OrderState.CREATE_REQUESTED))
            val request = CreateOrderRequest(
                idempotencyKey = intent.idempotencyKey,
                planId = planId,
                installId = installId(),
                promoCode = promoCode,
            )
            when (val result = backend.createOrder(request)) {
                is CommerceResult.Ok -> {
                    runCatching { CommercePreferences.setBackendAvailable(true) }
                    intents.save(
                        CheckoutIntent(
                            planId = result.value.planId,
                            idempotencyKey = result.value.idempotencyKey,
                            state = result.value.state,
                            orderId = result.value.id,
                        ),
                    )
                    runCatching { CommercePreferences.recordOrder(result.value) }
                    result
                }
                is CommerceResult.Err -> {
                    runCatching {
                        CommercePreferences.setBackendAvailable(result.kind != CommerceError.BACKEND_UNAVAILABLE)
                        CommercePreferences.recordError(result.kind.name)
                    }
                    result
                }
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
     * Payment-claim query markers (`success=true`, etc.) are ignored; they never skip backend polling
     * and never count as paid truth, even if [CheckoutReturnParser.isPaidProof] is true.
     */
    suspend fun handleCheckoutReturn(uriString: String?): CommerceResult<CommerceOrder?> {
        val orderId = CheckoutReturnParser.extractOrderId(uriString)
            ?: intents.load()?.orderId
            ?: runCatching { CommercePreferences.lastOrderId() }.getOrNull()
        if (orderId.isNullOrBlank()) return CommerceResult.Ok(null)
        return when (val result = backend.getOrder(orderId)) {
            is CommerceResult.Ok -> {
                runCatching { CommercePreferences.recordOrder(result.value) }
                recordObservedOrder(result.value)
                if (OrderStateMachine.isPaidTruth(result.value.state)) {
                    when (val claimed = fulfillPaidOrder(result.value)) {
                        is CommerceResult.Ok -> result
                        is CommerceResult.Err -> {
                            if (claimed.kind == CommerceError.REJECTED && claimed.message == "keystore_write_failed") {
                                claimed
                            } else {
                                result
                            }
                        }
                    }
                } else {
                    result
                }
            }
            is CommerceResult.Err -> result
        }
    }

    suspend fun fulfillPaidOrder(order: CommerceOrder): CommerceResult<CommerceEntitlement> {
        if (!OrderStateMachine.isPaidTruth(order.state)) {
            return CommerceResult.Err(CommerceError.REJECTED, "order_not_paid")
        }
        recordObservedOrder(order)
        return when (val claimed = backend.claimEntitlement(order.id, installId())) {
            is CommerceResult.Ok -> persistEntitlement(claimed.value)
            is CommerceResult.Err -> claimed
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
        if (credential.isNullOrBlank()) {
            val pending = intents.load()?.orderId
                ?: runCatching { CommercePreferences.lastOrderId() }.getOrNull()
            if (pending.isNullOrBlank()) return CommerceResult.Ok(null)
            return when (val order = backend.getOrder(pending)) {
                is CommerceResult.Ok -> {
                    recordObservedOrder(order.value)
                    if (OrderStateMachine.isPaidTruth(order.value.state)) {
                        fulfillPaidOrder(order.value)
                    } else {
                        CommerceResult.Ok(null)
                    }
                }
                is CommerceResult.Err -> order
            }
        }
        return when (val result = backend.getEntitlement(credential)) {
            is CommerceResult.Ok -> {
                val value = result.value
                if (value == null) {
                    invalidateLocalHotfoxAccess()
                    refreshPresentation()
                    CommerceResult.Ok(null)
                } else {
                    persistEntitlement(value)
                }
            }
            is CommerceResult.Err -> result
        }
    }

    suspend fun restoreAccess(recoveryCode: String?, providerTransactionId: String?): CommerceResult<CommerceEntitlement> {
        return when (
            val result = backend.restore(
                RestoreRequest(
                    installId = installId(),
                    recoveryCode = recoveryCode,
                    providerTransactionId = providerTransactionId,
                ),
            )
        ) {
            is CommerceResult.Ok -> persistEntitlement(result.value)
            is CommerceResult.Err -> result
        }
    }

    suspend fun syncManagedManifest(
        snapshot: ManifestRefreshPolicy.InventorySnapshot,
    ): CommerceResult<CommerceSubscriptionSync.Outcome> {
        val credential = when (val stored = secrets.get(SecretKeys.ENTITLEMENT_CREDENTIAL)) {
            is SecretGetResult.Value -> stored.utf8()
            is SecretGetResult.Missing -> return CommerceResult.Err(CommerceError.UNAUTHORIZED, "missing_credential")
            is SecretGetResult.Corrupt, is SecretGetResult.KeystoreInvalidated -> {
                return CommerceResult.Err(CommerceError.UNAUTHORIZED, "RESTORE_REQUIRED")
            }
        }
        runCatching { CommercePreferences.recordManifestAttempt() }
        return when (val manifest = backend.fetchManifest(credential)) {
            is CommerceResult.Ok -> {
                val subscriptionUrl = manifest.value.subscriptionUrl
                if (!subscriptionUrl.isNullOrBlank()) {
                    when (secrets.putUtf8(SecretKeys.SUBSCRIPTION_TOKEN, subscriptionUrl)) {
                        SecretPutResult.Ok -> Unit
                        SecretPutResult.KeystoreInvalidated ->
                            return CommerceResult.Err(CommerceError.UNAUTHORIZED, "RESTORE_REQUIRED")
                        SecretPutResult.Failed ->
                            return CommerceResult.Err(CommerceError.REJECTED, "keystore_write_failed")
                    }
                }
                val storedUrl = when (val stored = secrets.get(SecretKeys.SUBSCRIPTION_TOKEN)) {
                    is SecretGetResult.Value -> stored.utf8()
                    else -> subscriptionUrl
                }
                val subId = runCatching { CommercePreferences.hotfoxSubscriptionId() }.getOrNull().orEmpty()
                    .ifBlank { "hotfox-managed" }
                runCatching { CommercePreferences.setHotfoxSubscriptionId(subId) }
                val outcome = ManagedManifestApplicator(
                    store = inventory,
                    fetchUrl = fetchManagedUrl,
                    managedSubscriptionId = { subId },
                ).apply(snapshot, manifest.value, storedUrl)
                if (outcome.decision.commit) {
                    runCatching { CommercePreferences.recordManifestSuccess() }
                    CommerceResult.Ok(outcome)
                } else {
                    runCatching { CommercePreferences.recordManifestError(outcome.decision.error ?: "sync_failed") }
                    CommerceResult.Err(CommerceError.REJECTED, outcome.decision.error ?: "sync_failed")
                }
            }
            is CommerceResult.Err -> {
                runCatching { CommercePreferences.recordManifestError(manifest.message) }
                manifest
            }
        }
    }

    fun persistEntitlement(entitlement: CommerceEntitlement): CommerceResult<CommerceEntitlement> {
        val effective = entitlement.copy(
            status = EntitlementParser.effectiveStatus(entitlement, nowEpochSeconds()),
        )
        if (effective.status == EntitlementStatus.NONE) {
            return CommerceResult.Err(CommerceError.INVALID, "none_entitlement_status")
        }
        return when (secrets.putUtf8(SecretKeys.ENTITLEMENT_CREDENTIAL, entitlement.entitlementId)) {
            SecretPutResult.Ok -> {
                secrets.putUtf8(SecretKeys.RESTORE_SECRET, entitlement.entitlementId)
                metadata.write(EntitlementParser.metadataOf(effective))
                localOrigin = CommercePreferences.ORIGIN_HOTFOX
                runCatching { CommercePreferences.setAccessOrigin(CommercePreferences.ORIGIN_HOTFOX) }
                if (EntitlementStateMachine.isUsable(effective.status)) {
                    runCatching { CommercePreferences.clearTransientOrder() }
                    intents.save(
                        CheckoutIntent(
                            planId = entitlement.planId,
                            idempotencyKey = intents.load()?.idempotencyKey ?: entitlement.orderId.orEmpty(),
                            state = OrderState.FULFILLED,
                            orderId = entitlement.orderId,
                        ),
                    )
                }
                CommerceResult.Ok(effective)
            }
            SecretPutResult.KeystoreInvalidated ->
                CommerceResult.Err(CommerceError.UNAUTHORIZED, "RESTORE_REQUIRED")
            SecretPutResult.Failed ->
                CommerceResult.Err(CommerceError.REJECTED, "keystore_write_failed")
        }
    }

    fun collectFacts(): CommerceFacts {
        val credentialState = secrets.get(SecretKeys.ENTITLEMENT_CREDENTIAL)
        val credentialUnreadable =
            credentialState is SecretGetResult.Corrupt || credentialState is SecretGetResult.KeystoreInvalidated
        val trusted = metadata.read()
        val now = nowEpochSeconds()
        val effectiveStatus = trusted?.let { meta ->
            EntitlementParser.effectiveStatus(
                CommerceEntitlement(
                    entitlementId = "meta",
                    customerId = "",
                    source = EntitlementSource.HOTFOX,
                    planId = meta.planId,
                    status = meta.status,
                    startsAtEpochSeconds = meta.startsAtEpochSeconds,
                    expiresAtEpochSeconds = meta.expiresAtEpochSeconds,
                    orderId = meta.orderId,
                    graceUntilEpochSeconds = meta.graceUntilEpochSeconds,
                ),
                now,
            )
        }
        val hasCredential = credentialState is SecretGetResult.Value
        val origin = localOrigin
            ?: runCatching { CommercePreferences.accessOrigin() }.getOrDefault(CommercePreferences.ORIGIN_NONE)
        val hasHotfox = hasCredential && trusted != null && origin == CommercePreferences.ORIGIN_HOTFOX
        val entitlementRemaining = trusted?.let {
            EntitlementParser.remainingDays(it.expiresAtEpochSeconds, now)
        }
        val lastOrder = intents.load()?.state
            ?: runCatching { CommercePreferences.lastOrderState() }.getOrNull()
        val usable = hasHotfox && effectiveStatus != null && EntitlementStateMachine.isUsable(effectiveStatus)
        val subscriptions = runCatching { MmkvManager.decodeSubscriptions() }.getOrDefault(emptyList())
        val subscription = subscriptions.firstOrNull { it.subscription.url.isNotBlank() || it.subscription.remarks.isNotBlank() }
            ?.subscription
        val serverCount = runCatching { MmkvManager.decodeAllServerList().size }.getOrDefault(0)
        val shown = HotfoxSubscriptionPresentation.fromExpiryEpochSeconds(
            expireAtEpochSeconds = subscription?.expireAtEpochSeconds,
            serverCount = serverCount,
        )
        val hasExternal = subscription != null &&
            origin != CommercePreferences.ORIGIN_HOTFOX &&
            (subscription.url.isNotBlank() || serverCount > 0)
        return CommerceFacts(
            hasExternalSubscription = hasExternal,
            externalExpired = shown.status == SubscriptionPresentation.Status.EXPIRED,
            remainingDays = shown.remainingDays,
            hasHotfoxEntitlement = hasHotfox,
            entitlementStatus = if (hasHotfox) effectiveStatus else EntitlementStatus.NONE,
            entitlementRemainingDays = entitlementRemaining.takeIf { hasHotfox },
            lastOrderState = lastOrder.takeIf { !usable },
            backendAvailable = backend.available &&
                runCatching { CommercePreferences.backendAvailable() }.getOrDefault(true),
            credentialUnreadable = credentialUnreadable,
            lastSyncFailed = runCatching { CommercePreferences.lastManifestError() }.getOrNull() != null && hasHotfox,
            hasLocalServers = serverCount > 0,
        )
    }

    /**
     * Authoritative backend absence of an entitlement. Transient errors must not call this.
     */
    private fun invalidateLocalHotfoxAccess() {
        secrets.delete(SecretKeys.ENTITLEMENT_CREDENTIAL)
        secrets.delete(SecretKeys.SUBSCRIPTION_TOKEN)
        metadata.clear()
        localOrigin = CommercePreferences.ORIGIN_NONE
        runCatching { CommercePreferences.setAccessOrigin(CommercePreferences.ORIGIN_NONE) }
        val pending = intents.load()
        if (pending != null &&
            (pending.state == OrderState.FULFILLED || pending.state == OrderState.ENTITLEMENT_PROVISIONING)
        ) {
            intents.clear()
        }
        cachedPresentation = CommercialPresentationState.NO_ACCESS
    }

    /**
     * Backend PAID/FULFILLED is recorded locally as provisioning until Keystore persistence succeeds.
     * That keeps the idempotency key reusable after a lost claim/persist and avoids a false NO_ACCESS.
     */
    private fun recordObservedOrder(order: CommerceOrder) {
        val existingKey = intents.load()?.idempotencyKey
        val localState = if (OrderStateMachine.isPaidTruth(order.state)) {
            OrderState.ENTITLEMENT_PROVISIONING
        } else {
            order.state
        }
        intents.save(
            CheckoutIntent(
                planId = order.planId,
                idempotencyKey = order.idempotencyKey.ifBlank { existingKey.orEmpty() },
                state = localState,
                orderId = order.id,
            ),
        )
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
                    intents = MmkvCheckoutIntentStore,
                    metadata = MmkvEntitlementMetadataStore,
                    inventory = MmkvManagedServerStore,
                    fetchManagedUrl = { url ->
                        com.v2ray.ang.handler.AngConfigManager.fetchSubscriptionBody(url)
                    },
                ).also { instance = it }
            }
        }

        fun replaceForTests(coordinator: CommerceCoordinator?) {
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
