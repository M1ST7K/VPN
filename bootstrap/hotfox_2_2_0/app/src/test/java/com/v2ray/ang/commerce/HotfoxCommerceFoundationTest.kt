package com.v2ray.ang.commerce

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HotfoxCommerceFoundationTest {
    @Test
    fun duplicateIdempotencyKeyReturnsSameOrderWithoutDoubleCreate() = runBlocking {
        val backend = SandboxCommerceBackend()
        val request = CreateOrderRequest(
            idempotencyKey = "idem-1",
            planId = "plan_1m",
            installId = "install-a",
        )
        val first = (backend.createOrder(request) as CommerceResult.Ok).value
        val second = (backend.createOrder(request) as CommerceResult.Ok).value
        assertEquals(first.id, second.id)
        assertEquals(1, backend.createdOrderCount())
        backend.markPaidFromVerifiedWebhook(first.id, "pay_1")
        backend.markPaidFromVerifiedWebhook(first.id, "pay_1")
        val paid = (backend.getOrder(first.id) as CommerceResult.Ok).value
        assertEquals(OrderState.FULFILLED, paid.state)
    }

    @Test
    fun fakeBrowserSuccessDoesNotGrantEntitlement() = runBlocking {
        val backend = SandboxCommerceBackend()
        val order = (backend.createOrder(
            CreateOrderRequest("idem-return", "plan_3m", "install-b"),
        ) as CommerceResult.Ok).value
        val returnUrl = "https://hotfox.example/checkout/return?success=true&orderId=${order.id}"
        assertFalse(CheckoutReturnParser.isPaidProof(returnUrl))
        assertEquals(order.id, CheckoutReturnParser.extractOrderId(returnUrl))
        backend.noteCheckoutReturn(order.id)
        val afterReturn = (backend.getOrder(order.id) as CommerceResult.Ok).value
        assertFalse(OrderStateMachine.isPaidTruth(afterReturn.state))
        val entitlement = (backend.getEntitlement(null) as CommerceResult.Ok).value
        assertNull(entitlement)
        assertNotEquals(OrderState.PAID, afterReturn.state)
        assertNotEquals(OrderState.FULFILLED, afterReturn.state)
    }

    @Test
    fun backendUnavailableKeepsManualImportAndDoesNotLookUnknown() {
        assertTrue(UnavailableCommerceBackend.manualImportAllowed)
        assertFalse(UnavailableCommerceBackend.available)
        val none = CommerceAccessResolver.resolve(
            CommerceFacts(
                hasExternalSubscription = false,
                externalExpired = false,
                remainingDays = null,
                hasHotfoxEntitlement = false,
                entitlementStatus = EntitlementStatus.NONE,
                entitlementRemainingDays = null,
                lastOrderState = null,
                backendAvailable = false,
                credentialUnreadable = false,
                lastSyncFailed = false,
                hasLocalServers = false,
            ),
        )
        assertEquals(CommercialPresentationState.BACKEND_UNAVAILABLE, none)
        assertTrue(CommerceAccessResolver.manualImportAllowed(facts = unusedFacts()))
        val external = CommerceAccessResolver.resolve(
            CommerceFacts(
                hasExternalSubscription = true,
                externalExpired = false,
                remainingDays = 40,
                hasHotfoxEntitlement = false,
                entitlementStatus = EntitlementStatus.NONE,
                entitlementRemainingDays = null,
                lastOrderState = null,
                backendAvailable = false,
                credentialUnreadable = false,
                lastSyncFailed = false,
                hasLocalServers = true,
            ),
        )
        assertEquals(CommercialPresentationState.EXTERNAL_ACTIVE, external)
    }

    @Test
    fun keystoreCorruptionRequiresRestoreAndDoesNotCrash() {
        val store = InMemorySecretStore()
        assertEquals(SecretPutResult.Ok, store.putUtf8(SecretKeys.ENTITLEMENT_CREDENTIAL, "ent_secret"))
        store.corrupt(SecretKeys.ENTITLEMENT_CREDENTIAL)
        assertTrue(store.get(SecretKeys.ENTITLEMENT_CREDENTIAL) is SecretGetResult.Corrupt)
        store.invalidateAll()
        assertTrue(store.get(SecretKeys.ENTITLEMENT_CREDENTIAL) is SecretGetResult.KeystoreInvalidated)
        val state = CommerceAccessResolver.resolve(
            unusedFacts().copy(credentialUnreadable = true, hasHotfoxEntitlement = true),
        )
        assertEquals(CommercialPresentationState.RESTORE_REQUIRED, state)
    }

    @Test
    fun aesGcmRejectsCorruptCiphertext() {
        val generator = javax.crypto.KeyGenerator.getInstance("AES")
        generator.init(256)
        val box = AesGcmSecretBox(generator.generateKey())
        val sealed = box.seal("restore-secret".toByteArray())
        assertEquals("restore-secret", box.open(sealed).decodeToString())
        try {
            box.open(ByteArray(32) { 7 })
            org.junit.Assert.fail("corrupt blob should not decrypt")
        } catch (_: Exception) {
            // expected
        }
    }

    @Test
    fun emptyOrMalformedManifestKeepsLastKnownGoodAndAuto() {
        val amsterdam = ManifestRefreshPolicy.ServerIdentity("Amsterdam", "ams.example", "443")
        val frankfurt = ManifestRefreshPolicy.ServerIdentity("Frankfurt", "fra.example", "443")
        val snapshot = ManifestRefreshPolicy.InventorySnapshot(
            servers = listOf(amsterdam, frankfurt),
            favoriteIdentities = setOf(amsterdam),
            autoMode = true,
            selectedIdentity = amsterdam,
        )
        val empty = ManifestRefreshPolicy.decide(parsedCount = 0, malformed = false, emptyPayload = true)
        val malformed = ManifestRefreshPolicy.decide(parsedCount = 0, malformed = true, emptyPayload = false)
        assertFalse(empty.commit)
        assertFalse(malformed.commit)
        assertFalse(ManifestRefreshPolicy.shouldCommitSwap(0, malformed = true))
        assertEquals(
            snapshot.servers,
            ManifestRefreshPolicy.inventoryAfter(snapshot, emptyList(), empty),
        )
        val paris = ManifestRefreshPolicy.ServerIdentity("Paris", "par.example", "443")
        val restored = ManifestRefreshPolicy.restoreAfterSuccessfulSwap(
            snapshot,
            newInventory = listOf(amsterdam, paris),
        )
        assertTrue(restored.autoMode)
        assertEquals(setOf(amsterdam), restored.favoriteIdentities)
        assertEquals(amsterdam, restored.selectedIdentity)
    }

    @Test
    fun successfulSyncPreservesAutoMode() {
        val snapshot = ManifestRefreshPolicy.InventorySnapshot(
            servers = listOf(ManifestRefreshPolicy.ServerIdentity("A", "a.example", "443")),
            favoriteIdentities = emptySet(),
            autoMode = true,
            selectedIdentity = ManifestRefreshPolicy.ServerIdentity("A", "a.example", "443"),
        )
        val swapped = ManifestRefreshPolicy.restoreAfterSuccessfulSwap(
            snapshot,
            listOf(
                ManifestRefreshPolicy.ServerIdentity("A", "a.example", "443"),
                ManifestRefreshPolicy.ServerIdentity("B", "b.example", "443"),
            ),
        )
        assertTrue(swapped.autoMode)
        assertTrue(ManifestRefreshPolicy.shouldCommitSwap(2))
    }

    @Test
    fun presentationStatesAreExplicit() {
        assertEquals(
            CommercialPresentationState.PAYMENT_FAILED,
            CommerceAccessResolver.resolve(unusedFacts().copy(lastOrderState = OrderState.FAILED)),
        )
        assertEquals(
            CommercialPresentationState.PAYMENT_CANCELLED,
            CommerceAccessResolver.resolve(unusedFacts().copy(lastOrderState = OrderState.CANCELLED)),
        )
        assertEquals(
            CommercialPresentationState.PAYMENT_PENDING,
            CommerceAccessResolver.resolve(unusedFacts().copy(lastOrderState = OrderState.PENDING)),
        )
        assertEquals(
            CommercialPresentationState.ENTITLEMENT_PROVISIONING,
            CommerceAccessResolver.resolve(unusedFacts().copy(lastOrderState = OrderState.PAID)),
        )
        assertEquals(
            CommercialPresentationState.ENTITLEMENT_PROVISIONING,
            CommerceAccessResolver.resolve(unusedFacts().copy(lastOrderState = OrderState.FULFILLED)),
        )
        assertEquals(
            CommercialPresentationState.ENTITLEMENT_ACTIVE_SYNC_FAILED,
            CommerceAccessResolver.resolve(
                unusedFacts().copy(
                    hasHotfoxEntitlement = true,
                    entitlementStatus = EntitlementStatus.ACTIVE,
                    lastSyncFailed = true,
                    entitlementRemainingDays = 20,
                ),
            ),
        )
        assertEquals(
            CommercialPresentationState.EXPIRING_SOON,
            CommerceAccessResolver.resolve(
                unusedFacts().copy(
                    hasExternalSubscription = true,
                    remainingDays = 3,
                    backendAvailable = true,
                ),
            ),
        )
    }

    @Test
    fun verifiedPaidOrderFulfillsIntoPersistedEntitlement() = runBlocking {
        val backend = SandboxCommerceBackend()
        val secrets = InMemorySecretStore()
        val intents = InMemoryCheckoutIntentStore()
        val metadata = InMemoryEntitlementMetadataStore()
        val coordinator = testCoordinator(backend, secrets, intents, metadata)
        val order = (coordinator.startCheckout("plan_1m") as CommerceResult.Ok).value
        backend.markPaidFromVerifiedWebhook(order.id, "pay_verified")
        val fakeReturn = "https://hotfox.example/return?success=true&orderId=${order.id}"
        assertFalse(CheckoutReturnParser.isPaidProof(fakeReturn))
        val handled = coordinator.handleCheckoutReturn(fakeReturn)
        assertTrue(handled is CommerceResult.Ok)
        assertTrue(secrets.get(SecretKeys.ENTITLEMENT_CREDENTIAL) is SecretGetResult.Value)
        assertEquals(EntitlementStatus.ACTIVE, metadata.read()?.status)
        assertEquals(
            CommercialPresentationState.HOTFOX_ACTIVE,
            CommerceAccessResolver.resolve(coordinator.collectFacts()),
        )

        val secrets2 = InMemorySecretStore()
        val metadata2 = InMemoryEntitlementMetadataStore()
        val intents2 = InMemoryCheckoutIntentStore().apply {
            save(
                CheckoutIntent(
                    planId = order.planId,
                    idempotencyKey = order.idempotencyKey,
                    state = OrderState.CREATED,
                    orderId = order.id,
                ),
            )
        }
        val second = testCoordinator(backend, secrets2, intents2, metadata2)
        val refreshed = second.refreshEntitlement()
        assertTrue(refreshed is CommerceResult.Ok)
        assertTrue(secrets2.get(SecretKeys.ENTITLEMENT_CREDENTIAL) is SecretGetResult.Value)
        assertEquals(EntitlementStatus.ACTIVE, metadata2.read()?.status)
        assertEquals(
            CommercialPresentationState.HOTFOX_ACTIVE,
            CommerceAccessResolver.resolve(second.collectFacts()),
        )
    }

    @Test
    fun keystoreWriteFailureDoesNotClearOrderOrClaimSuccess() = runBlocking {
        val backend = SandboxCommerceBackend()
        val secrets = InMemorySecretStore().apply { failPuts = true }
        val intents = InMemoryCheckoutIntentStore()
        val metadata = InMemoryEntitlementMetadataStore()
        val coordinator = testCoordinator(backend, secrets, intents, metadata)
        val order = (coordinator.startCheckout("plan_1m") as CommerceResult.Ok).value
        val originalKey = intents.load()?.idempotencyKey
        backend.markPaidFromVerifiedWebhook(order.id, "pay_persist")
        val result = coordinator.fulfillPaidOrder((backend.getOrder(order.id) as CommerceResult.Ok).value)
        assertTrue(result is CommerceResult.Err)
        assertEquals("keystore_write_failed", (result as CommerceResult.Err).message)
        assertTrue(secrets.get(SecretKeys.ENTITLEMENT_CREDENTIAL) is SecretGetResult.Missing)
        assertNull(metadata.read())
        assertEquals(order.id, intents.load()?.orderId)
        assertEquals(originalKey, intents.load()?.idempotencyKey)
        assertEquals(OrderState.ENTITLEMENT_PROVISIONING, intents.load()?.state)
        assertEquals(
            CommercialPresentationState.ENTITLEMENT_PROVISIONING,
            CommerceAccessResolver.resolve(coordinator.collectFacts()),
        )
        val retry = coordinator.startCheckout("plan_1m") as CommerceResult.Ok
        assertEquals(originalKey, retry.value.idempotencyKey)
        assertEquals(1, backend.createdOrderCount())
    }

    @Test
    fun expiredRevokedAndMalformedEntitlementResponsesAreRejected() {
        assertTrue(
            EntitlementParser.fromFields(
                entitlementId = "ent_1",
                customerId = "cust",
                source = "HOTFOX",
                planId = "plan_1m",
                status = null,
                startsAtEpochSeconds = 100,
                expiresAtEpochSeconds = 200,
                orderId = "ord",
            ) is CommerceResult.Err,
        )
        assertTrue(
            EntitlementParser.fromFields(
                entitlementId = "ent_2",
                customerId = "cust",
                source = "HOTFOX",
                planId = "plan_1m",
                status = "ACTIVE",
                startsAtEpochSeconds = 100,
                expiresAtEpochSeconds = null,
                orderId = "ord",
            ) is CommerceResult.Err,
        )
        assertTrue(
            EntitlementParser.fromFields(
                entitlementId = "ent_3",
                customerId = "cust",
                source = "HOTFOX",
                planId = "plan_1m",
                status = "NOT_A_STATUS",
                startsAtEpochSeconds = 100,
                expiresAtEpochSeconds = 200,
                orderId = "ord",
            ) is CommerceResult.Err,
        )
        val expired = (EntitlementParser.fromFields(
            entitlementId = "ent_exp",
            customerId = "cust",
            source = "HOTFOX",
            planId = "plan_1m",
            status = "EXPIRED",
            startsAtEpochSeconds = 1,
            expiresAtEpochSeconds = 2,
            orderId = "ord",
        ) as CommerceResult.Ok).value
        val coordinator = testCoordinator(
            secrets = InMemorySecretStore(),
            metadata = InMemoryEntitlementMetadataStore(),
        )
        assertTrue(coordinator.persistEntitlement(expired) is CommerceResult.Ok)
        assertFalse(EntitlementStateMachine.isUsable(expired.status))
        assertEquals(
            CommercialPresentationState.EXPIRED,
            CommerceAccessResolver.resolve(coordinator.collectFacts()),
        )
        val revoked = expired.copy(entitlementId = "ent_rev", status = EntitlementStatus.REVOKED)
        coordinator.persistEntitlement(revoked)
        assertEquals(
            CommercialPresentationState.EXPIRED,
            CommerceAccessResolver.resolve(coordinator.collectFacts()),
        )
        assertFalse(EntitlementStateMachine.isUsable(EntitlementStatus.REVOKED))
        assertFalse(EntitlementStateMachine.isUsable(EntitlementStatus.EXPIRED))
        assertTrue(EntitlementStateMachine.isUsable(EntitlementStatus.ACTIVE))
        assertTrue(EntitlementStateMachine.isUsable(EntitlementStatus.GRACE))
    }

    @Test
    fun checkoutConcurrencyAndLostResponseReuseTheSameIdempotencyKey() = runBlocking {
        val backend = SandboxCommerceBackend()
        val dropping = DropFirstSuccessfulCreate(backend)
        val intents = InMemoryCheckoutIntentStore()
        val coordinator = testCoordinator(backend = dropping, intents = intents)
        val first = coordinator.startCheckout("plan_3m")
        assertTrue(first is CommerceResult.Err)
        val persistedKey = intents.load()?.idempotencyKey
        org.junit.Assert.assertNotNull(persistedKey)
        val retry = coordinator.startCheckout("plan_3m") as CommerceResult.Ok
        assertEquals(persistedKey, retry.value.idempotencyKey)
        assertEquals(1, backend.createdOrderCount())

        val concurrentBackend = SandboxCommerceBackend()
        val concurrent = testCoordinator(backend = concurrentBackend)
        val one = async { concurrent.startCheckout("plan_1m") }
        val two = async { concurrent.startCheckout("plan_1m") }
        val results = listOf(one.await(), two.await())
        assertTrue(results.all { it is CommerceResult.Ok })
        val ids = results.map { (it as CommerceResult.Ok).value.id }.toSet()
        assertEquals(1, ids.size)
        assertEquals(1, concurrentBackend.createdOrderCount())
    }

    @Test
    fun refreshIdentityDistinguishesSameHostPortDifferentTransportOrCredential() {
        val grpc = com.v2ray.ang.dto.entities.ProfileItem.create(com.v2ray.ang.enums.EConfigType.VLESS).apply {
            remarks = "Amsterdam"
            server = "vpn.example"
            serverPort = "443"
            network = "grpc"
            security = "reality"
            password = "credential-a"
        }
        val xhttp = com.v2ray.ang.dto.entities.ProfileItem.create(com.v2ray.ang.enums.EConfigType.VLESS).apply {
            remarks = "Amsterdam"
            server = "vpn.example"
            serverPort = "443"
            network = "xhttp"
            security = "reality"
            password = "credential-b"
        }
        val grpcOtherCredential = com.v2ray.ang.dto.entities.ProfileItem.create(com.v2ray.ang.enums.EConfigType.VLESS).apply {
            remarks = "Amsterdam"
            server = "vpn.example"
            serverPort = "443"
            network = "grpc"
            security = "reality"
            password = "credential-b"
        }
        val left = HotfoxManifestRefresh.identityOf(grpc)
        val right = HotfoxManifestRefresh.identityOf(xhttp)
        val sameTransportDifferentSecret = HotfoxManifestRefresh.identityOf(grpcOtherCredential)
        assertNotEquals(left, right)
        assertNotEquals(left, sameTransportDifferentSecret)
        assertEquals(left.server, right.server)
        assertEquals(left.port, right.port)
        val snapshot = ManifestRefreshPolicy.InventorySnapshot(
            servers = listOf(left),
            favoriteIdentities = setOf(left),
            autoMode = false,
            selectedIdentity = left,
        )
        val restored = ManifestRefreshPolicy.restoreAfterSuccessfulSwap(snapshot, listOf(left, right))
        assertEquals(left, restored.selectedIdentity)
        assertEquals(setOf(left), restored.favoriteIdentities)
        assertEquals(right, ManifestRefreshPolicy.restoreAfterSuccessfulSwap(snapshot, listOf(right)).selectedIdentity)
    }

    private fun testCoordinator(
        backend: HotfoxCommerceBackend = SandboxCommerceBackend(),
        secrets: SecretStore = InMemorySecretStore(),
        intents: CheckoutIntentStore = InMemoryCheckoutIntentStore(),
        metadata: EntitlementMetadataStore = InMemoryEntitlementMetadataStore(),
    ) = CommerceCoordinator(
        backend = backend,
        secrets = secrets,
        intents = intents,
        metadata = metadata,
        installId = { "install-test" },
        nowEpochSeconds = { 1_700_000_000L },
    )

    private class DropFirstSuccessfulCreate(
        private val inner: SandboxCommerceBackend,
    ) : HotfoxCommerceBackend by inner {
        @Volatile
        var dropped = false

        override suspend fun createOrder(request: CreateOrderRequest): CommerceResult<CommerceOrder> {
            val result = inner.createOrder(request)
            if (!dropped && result is CommerceResult.Ok) {
                dropped = true
                return CommerceResult.Err(CommerceError.BACKEND_UNAVAILABLE)
            }
            return result
        }
    }

    private fun unusedFacts() = CommerceFacts(
        hasExternalSubscription = false,
        externalExpired = false,
        remainingDays = null,
        hasHotfoxEntitlement = false,
        entitlementStatus = EntitlementStatus.NONE,
        entitlementRemainingDays = null,
        lastOrderState = null,
        backendAvailable = true,
        credentialUnreadable = false,
        lastSyncFailed = false,
        hasLocalServers = false,
    )
}
