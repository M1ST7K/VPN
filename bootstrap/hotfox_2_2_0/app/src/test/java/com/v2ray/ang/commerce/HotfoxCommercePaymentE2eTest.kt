package com.v2ray.ang.commerce

import com.v2ray.ang.vpn.HotfoxServerSelection
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HotfoxCommercePaymentE2eTest {
    private val now = 1_700_000_000L

    @Test
    fun sandboxPaymentE2eProvesCatalogThroughAutoConnectHandoffAndRestore() = runBlocking {
        val backend = sandbox()
        val secrets = InMemorySecretStore()
        val intents = InMemoryCheckoutIntentStore()
        val metadata = InMemoryEntitlementMetadataStore()
        val coordinator = coordinator(backend, secrets, intents, metadata)
        val report = SandboxPaymentE2e.run(
            backend = backend,
            coordinator = coordinator,
            secrets = secrets,
            intents = intents,
            metadata = metadata,
            relaunch = { s, i, m -> coordinator(backend, s, i, m) },
            nowEpochSeconds = now,
        )
        assertEquals(3, report.plans.size)
        assertTrue(report.plans.all { it.priceMinor > 0L && it.currency == "RUB" })
        assertFalse(report.browserClaimedPaid)
        assertEquals(OrderState.FULFILLED, report.order.state)
        assertTrue(report.credentialStored)
        assertEquals(CommercialPresentationState.HOTFOX_ACTIVE, report.presentation)
        assertTrue(report.syncCommitted)
        assertTrue(report.autoMode)
        assertTrue(report.connect is HotfoxServerSelection.ResolveResult.Success)
        val success = report.connect as HotfoxServerSelection.ResolveResult.Success
        assertTrue(success.resolvedFromAuto)
        assertEquals("hotfox-managed-1", success.guid)
        assertTrue(report.restoredAfterReinstall)
    }

    @Test
    fun duplicateWebhookDoesNotGrantTwice() = runBlocking {
        val backend = sandbox()
        val order = (backend.createOrder(CreateOrderRequest("idem-dup", "plan_1m", "install")) as CommerceResult.Ok).value
        val event = backend.signedEvent(order.id, WebhookEventType.PAID, "pay_dup", "evt_dup", now)
        val first = backend.ingestWebhook(event)
        val second = backend.ingestWebhook(event)
        assertTrue(first is WebhookApplyResult.Applied)
        assertTrue(second is WebhookApplyResult.Duplicate)
        val paid = (backend.getOrder(order.id) as CommerceResult.Ok).value
        assertEquals(OrderState.FULFILLED, paid.state)
        val claimed = backend.claimEntitlement(order.id, "install") as CommerceResult.Ok
        assertEquals("ent_${order.id}", claimed.value.entitlementId)
        assertEquals(claimed.value.entitlementId, (backend.claimEntitlement(order.id, "install") as CommerceResult.Ok).value.entitlementId)
    }

    @Test
    fun outOfOrderPendingAfterPaidIsIgnored() = runBlocking {
        val backend = sandbox()
        val order = (backend.createOrder(CreateOrderRequest("idem-ooo", "plan_3m", "install")) as CommerceResult.Ok).value
        backend.ingestWebhook(backend.signedEvent(order.id, WebhookEventType.PAID, "pay_ooo", "evt_paid", now))
        val pending = backend.ingestWebhook(backend.signedEvent(order.id, WebhookEventType.PENDING, "pay_ooo", "evt_pending_late", now + 1))
        assertTrue(pending is WebhookApplyResult.Duplicate)
        assertEquals(OrderState.FULFILLED, (backend.getOrder(order.id) as CommerceResult.Ok).value.state)
    }

    @Test
    fun invalidWebhookSignatureDoesNotChangeOrder() = runBlocking {
        val backend = sandbox()
        val order = (backend.createOrder(CreateOrderRequest("idem-sig", "plan_1m", "install")) as CommerceResult.Ok).value
        val valid = backend.signedEvent(order.id, WebhookEventType.PAID, "pay_sig", "evt_sig", now)
        val forged = valid.copy(signature = "deadbeef")
        val rejected = backend.ingestWebhook(forged)
        assertTrue(rejected is WebhookApplyResult.Rejected)
        assertEquals("invalid_signature", (rejected as WebhookApplyResult.Rejected).reason)
        assertEquals(OrderState.CREATED, (backend.getOrder(order.id) as CommerceResult.Ok).value.state)
        assertTrue(backend.claimEntitlement(order.id, "install") is CommerceResult.Err)
    }

    @Test
    fun lostCreateOrderResponseReusesIdempotencyKey() = runBlocking {
        val backend = sandbox()
        val dropping = object : HotfoxCommerceBackend by backend {
            @Volatile var dropped = false
            override suspend fun createOrder(request: CreateOrderRequest): CommerceResult<CommerceOrder> {
                val result = backend.createOrder(request)
                if (!dropped && result is CommerceResult.Ok) {
                    dropped = true
                    return CommerceResult.Err(CommerceError.BACKEND_UNAVAILABLE)
                }
                return result
            }
        }
        val intents = InMemoryCheckoutIntentStore()
        val coordinator = coordinator(dropping, intents = intents)
        assertTrue(coordinator.startCheckout("plan_1m") is CommerceResult.Err)
        val key = intents.load()?.idempotencyKey
        assertNotNull(key)
        val retry = coordinator.startCheckout("plan_1m") as CommerceResult.Ok
        assertEquals(key, retry.value.idempotencyKey)
        assertEquals(1, backend.createdOrderCount())
    }

    @Test
    fun checkoutCancelPresentsPaymentCancelled() = runBlocking {
        val backend = sandbox()
        val secrets = InMemorySecretStore()
        val intents = InMemoryCheckoutIntentStore()
        val coordinator = coordinator(backend, secrets, intents)
        val order = (coordinator.startCheckout("plan_1m") as CommerceResult.Ok).value
        HostedCheckoutFixture.open(backend, order.id)
        HostedCheckoutFixture.cancel(backend, order.id)
        coordinator.handleCheckoutReturn(HostedCheckoutFixture.returnUri(order, claimSuccess = true))
        assertEquals(OrderState.CANCELLED, (backend.getOrder(order.id) as CommerceResult.Ok).value.state)
        assertEquals(
            CommercialPresentationState.PAYMENT_CANCELLED,
            CommerceAccessResolver.resolve(coordinator.collectFacts()),
        )
        assertTrue(secrets.get(SecretKeys.ENTITLEMENT_CREDENTIAL) is SecretGetResult.Missing)
    }

    @Test
    fun appKilledDuringCheckoutResumesFromPersistedIntent() = runBlocking {
        val backend = sandbox()
        val secrets = InMemorySecretStore()
        val intents = InMemoryCheckoutIntentStore()
        val metadata = InMemoryEntitlementMetadataStore()
        val first = coordinator(backend, secrets, intents, metadata)
        val order = (first.startCheckout("plan_12m") as CommerceResult.Ok).value
        HostedCheckoutFixture.open(backend, order.id)
        val relaunch = coordinator(backend, secrets, intents, metadata)
        val pending = relaunch.handleCheckoutReturn(null)
        assertTrue(pending is CommerceResult.Ok)
        assertEquals(order.id, (pending as CommerceResult.Ok).value?.id)
        assertEquals(
            CommercialPresentationState.PAYMENT_PENDING,
            CommerceAccessResolver.resolve(relaunch.collectFacts()),
        )
        backend.ingestWebhook(backend.signedEvent(order.id, WebhookEventType.PAID, "pay_killed", "evt_killed", now))
        relaunch.handleCheckoutReturn(null)
        assertEquals(
            CommercialPresentationState.HOTFOX_ACTIVE,
            CommerceAccessResolver.resolve(relaunch.collectFacts()),
        )
    }

    @Test
    fun paymentWhileAppOfflineIsPickedUpOnRelaunch() = runBlocking {
        val backend = sandbox()
        val secrets = InMemorySecretStore()
        val intents = InMemoryCheckoutIntentStore()
        val metadata = InMemoryEntitlementMetadataStore()
        val first = coordinator(backend, secrets, intents, metadata)
        val order = (first.startCheckout("plan_1m") as CommerceResult.Ok).value
        backend.ingestWebhook(backend.signedEvent(order.id, WebhookEventType.PAID, "pay_offline", "evt_offline", now))
        val relaunch = coordinator(backend, secrets, intents, metadata)
        relaunch.handleCheckoutReturn(null)
        assertTrue(secrets.get(SecretKeys.ENTITLEMENT_CREDENTIAL) is SecretGetResult.Value)
        assertEquals(
            CommercialPresentationState.HOTFOX_ACTIVE,
            CommerceAccessResolver.resolve(relaunch.collectFacts()),
        )
    }

    @Test
    fun entitlementProvisioningRetriesUntilFulfillment() = runBlocking {
        val backend = sandbox()
        backend.autoFulfill = false
        val secrets = InMemorySecretStore()
        val intents = InMemoryCheckoutIntentStore()
        val metadata = InMemoryEntitlementMetadataStore()
        val coordinator = coordinator(backend, secrets, intents, metadata)
        val order = (coordinator.startCheckout("plan_1m") as CommerceResult.Ok).value
        backend.ingestWebhook(backend.signedEvent(order.id, WebhookEventType.PAID, "pay_retry", "evt_retry", now))
        coordinator.handleCheckoutReturn(HostedCheckoutFixture.returnUri(order, claimSuccess = false))
        assertTrue(secrets.get(SecretKeys.ENTITLEMENT_CREDENTIAL) is SecretGetResult.Missing)
        assertEquals(
            CommercialPresentationState.ENTITLEMENT_PROVISIONING,
            CommerceAccessResolver.resolve(coordinator.collectFacts()),
        )
        backend.completeFulfillment(order.id)
        coordinator.refreshEntitlement()
        assertTrue(secrets.get(SecretKeys.ENTITLEMENT_CREDENTIAL) is SecretGetResult.Value)
        assertEquals(
            CommercialPresentationState.HOTFOX_ACTIVE,
            CommerceAccessResolver.resolve(coordinator.collectFacts()),
        )
    }

    @Test
    fun restoreAfterReinstallUsesRecoveryCode() = runBlocking {
        val backend = sandbox()
        val original = InMemorySecretStore()
        val coordinator = coordinator(backend, original)
        val order = (coordinator.startCheckout("plan_3m") as CommerceResult.Ok).value
        backend.markPaidFromVerifiedWebhook(order.id, "pay_restore")
        coordinator.handleCheckoutReturn(HostedCheckoutFixture.returnUri(order, claimSuccess = true))
        val recovery = (original.get(SecretKeys.ENTITLEMENT_CREDENTIAL) as SecretGetResult.Value).utf8()
        val replacement = InMemorySecretStore()
        val replacementMeta = InMemoryEntitlementMetadataStore()
        val reinstall = coordinator(backend, replacement, metadata = replacementMeta)
        val restored = reinstall.restoreAccess(recovery, "pay_restore")
        assertTrue(restored is CommerceResult.Ok)
        assertEquals(recovery, (replacement.get(SecretKeys.ENTITLEMENT_CREDENTIAL) as SecretGetResult.Value).utf8())
        assertEquals(
            CommercialPresentationState.HOTFOX_ACTIVE,
            CommerceAccessResolver.resolve(reinstall.collectFacts()),
        )
    }

    @Test
    fun malformedManifestKeepsLastKnownGoodAndAuto() = runBlocking {
        val backend = sandbox()
        val secrets = InMemorySecretStore()
        val coordinator = coordinator(backend, secrets)
        val order = (coordinator.startCheckout("plan_1m") as CommerceResult.Ok).value
        backend.markPaidFromVerifiedWebhook(order.id, "pay_manifest")
        coordinator.handleCheckoutReturn(HostedCheckoutFixture.returnUri(order, claimSuccess = true))
        val lastKnown = SandboxManifest.sandboxInventory()
        val snapshot = ManifestRefreshPolicy.InventorySnapshot(
            servers = lastKnown,
            favoriteIdentities = setOf(lastKnown.first()),
            autoMode = true,
            selectedIdentity = lastKnown.first(),
        )
        val good = coordinator.syncManagedManifest(snapshot) as CommerceResult.Ok
        assertTrue(good.value.decision.commit)
        assertTrue(good.value.restored?.autoMode == true)
        backend.forceManifestFailure = true
        val failed = coordinator.syncManagedManifest(snapshot)
        assertTrue(failed is CommerceResult.Err)
        val kept = CommerceSubscriptionSync.apply(
            snapshot,
            CommerceManifest(format = "hotfox-sandbox-v1", payload = "{not-json"),
        )
        assertFalse(kept.decision.commit)
        assertEquals(lastKnown, kept.inventory)
        assertEquals("malformed_manifest", kept.decision.error)
        val empty = CommerceSubscriptionSync.apply(
            snapshot,
            SandboxManifest.encode(emptyList()),
        )
        assertFalse(empty.decision.commit)
        assertEquals(lastKnown, empty.inventory)
        val connect = VpnConnectHandoff.resolve(
            auto = true,
            inventory = lastKnown,
            delaysByRemarks = mapOf("Amsterdam" to 30L, "Frankfurt" to 12L),
        )
        assertTrue(connect is HotfoxServerSelection.ResolveResult.Success)
        assertTrue((connect as HotfoxServerSelection.ResolveResult.Success).resolvedFromAuto)
        assertEquals("hotfox-managed-1", connect.guid)
    }

    @Test
    fun conflictingPaymentIdDoesNotMoveASecondOrder() = runBlocking {
        val backend = sandbox()
        val first = (backend.createOrder(CreateOrderRequest("idem-pay-a", "plan_1m", "install")) as CommerceResult.Ok).value
        val second = (backend.createOrder(CreateOrderRequest("idem-pay-b", "plan_3m", "install")) as CommerceResult.Ok).value
        backend.ingestWebhook(backend.signedEvent(first.id, WebhookEventType.PAID, "pay_shared", "evt_first", now))
        val conflict = backend.ingestWebhook(
            backend.signedEvent(second.id, WebhookEventType.PAID, "pay_shared", "evt_second", now),
        )
        assertTrue(conflict is WebhookApplyResult.Rejected)
        assertEquals("payment_id_conflict", (conflict as WebhookApplyResult.Rejected).reason)
        assertEquals(OrderState.CREATED, (backend.getOrder(second.id) as CommerceResult.Ok).value.state)
        assertEquals(OrderState.FULFILLED, (backend.getOrder(first.id) as CommerceResult.Ok).value.state)
    }

    @Test
    fun browserSuccessStillPollsBackendOrder() = runBlocking {
        val backend = sandbox()
        val coordinator = coordinator(backend)
        val order = (coordinator.startCheckout("plan_1m") as CommerceResult.Ok).value
        val before = backend.getOrderCount()
        coordinator.handleCheckoutReturn(HostedCheckoutFixture.returnUri(order, claimSuccess = true))
        assertTrue(backend.getOrderCount() > before)
        assertFalse(CheckoutReturnParser.isPaidProof(HostedCheckoutFixture.returnUri(order, claimSuccess = true)))
        assertEquals(OrderState.CREATED, (backend.getOrder(order.id) as CommerceResult.Ok).value.state)
    }

    @Test
    fun authenticatedUrlManifestStoresTokenWithoutTrustingBrowser() = runBlocking {
        val backend = sandbox()
        val secrets = InMemorySecretStore()
        val coordinator = coordinator(backend, secrets)
        val order = (coordinator.startCheckout("plan_1m") as CommerceResult.Ok).value
        backend.markPaidFromVerifiedWebhook(order.id, "pay_token")
        coordinator.handleCheckoutReturn(HostedCheckoutFixture.returnUri(order, claimSuccess = true))
        val wrapping = object : HotfoxCommerceBackend by backend {
            override suspend fun fetchManifest(credential: String): CommerceResult<CommerceManifest> {
                return CommerceResult.Ok(
                    CommerceManifest(
                        format = "opaque",
                        payload = null,
                        subscriptionUrl = "https://manifest.sandbox.hotfox.invalid/sub",
                    ),
                )
            }
        }
        val syncing = coordinator(wrapping, secrets)
        val snapshot = ManifestRefreshPolicy.InventorySnapshot(
            servers = emptyList(),
            favoriteIdentities = emptySet(),
            autoMode = true,
            selectedIdentity = null,
        )
        val synced = syncing.syncManagedManifest(snapshot)
        assertTrue(synced is CommerceResult.Ok)
        assertEquals(
            "https://manifest.sandbox.hotfox.invalid/sub",
            (secrets.get(SecretKeys.SUBSCRIPTION_TOKEN) as SecretGetResult.Value).utf8(),
        )
        assertFalse(CheckoutReturnParser.isPaidProof(HostedCheckoutFixture.returnUri(order, claimSuccess = true)))
    }

    @Test
    fun createOrderDoesNotAcceptClientSuppliedPrice() = runBlocking {
        val backend = sandbox()
        val catalog = (backend.listPlans() as CommerceResult.Ok).value
        val plan = catalog.first { it.id == "plan_1m" }
        val order = (backend.createOrder(CreateOrderRequest("idem-price", plan.id, "install")) as CommerceResult.Ok).value
        assertNotEquals(1L, plan.priceMinor)
        assertEquals("plan_1m", order.planId)
        assertFalse(order.checkoutUrl.orEmpty().contains(plan.priceMinor.toString()))
    }

    private fun sandbox() = SandboxCommerceBackend(
        clock = { now },
        webhookHmacSecret = SandboxCommerceBackend.CI_WEBHOOK_HMAC,
    )

    private fun coordinator(
        backend: HotfoxCommerceBackend,
        secrets: SecretStore = InMemorySecretStore(),
        intents: CheckoutIntentStore = InMemoryCheckoutIntentStore(),
        metadata: EntitlementMetadataStore = InMemoryEntitlementMetadataStore(),
    ) = CommerceCoordinator(
        backend = backend,
        secrets = secrets,
        intents = intents,
        metadata = metadata,
        installId = { "install-e2e" },
        nowEpochSeconds = { now },
    )
}
