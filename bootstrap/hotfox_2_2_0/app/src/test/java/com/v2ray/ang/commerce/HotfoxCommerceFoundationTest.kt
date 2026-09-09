package com.v2ray.ang.commerce

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
