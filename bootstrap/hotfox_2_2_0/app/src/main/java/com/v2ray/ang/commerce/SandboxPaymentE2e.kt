package com.v2ray.ang.commerce

import com.v2ray.ang.vpn.HotfoxServerSelection

/**
 * Deterministic CI proof of the 2.3 commercial exit path.
 * Does not start Android VpnService and does not use a real payment provider.
 */
object SandboxPaymentE2e {
    data class Report(
        val plans: List<CommercePlan>,
        val order: CommerceOrder,
        val browserClaimedPaid: Boolean,
        val presentation: CommercialPresentationState,
        val credentialStored: Boolean,
        val syncCommitted: Boolean,
        val autoMode: Boolean,
        val connect: HotfoxServerSelection.ResolveResult,
        val restoredAfterReinstall: Boolean,
    )

    suspend fun run(
        backend: SandboxCommerceBackend,
        coordinator: CommerceCoordinator,
        secrets: SecretStore,
        intents: CheckoutIntentStore,
        metadata: EntitlementMetadataStore,
        relaunch: (secrets: SecretStore, intents: CheckoutIntentStore, metadata: EntitlementMetadataStore) -> CommerceCoordinator,
        nowEpochSeconds: Long,
    ): Report {
        val plans = coordinator.loadPlans()
        val recommended = plans.first { it.isRecommended }
        val created = (coordinator.startCheckout(recommended.id) as CommerceResult.Ok).value
        HostedCheckoutFixture.open(backend, created.id)
        val browserReturn = HostedCheckoutFixture.returnUri(created, claimSuccess = true)
        val browserClaimedPaid = CheckoutReturnParser.isPaidProof(browserReturn)
        coordinator.handleCheckoutReturn(browserReturn)
        val unpaidFacts = CommerceAccessResolver.resolve(coordinator.collectFacts())
        check(!EntitlementStateMachine.isUsable(coordinator.collectFacts().entitlementStatus ?: EntitlementStatus.NONE)) {
            "browser return must not grant entitlement (state=$unpaidFacts)"
        }
        backend.ingestWebhook(
            backend.signedEvent(
                orderId = created.id,
                type = WebhookEventType.PAID,
                providerPaymentId = "pay_sandbox_${created.id}",
                eventId = "evt_sandbox_${created.id}",
                occurredAtEpochSeconds = nowEpochSeconds,
            ),
        )
        val fulfilled = coordinator.handleCheckoutReturn(browserReturn)
        check(fulfilled is CommerceResult.Ok)
        val snapshot = ManifestRefreshPolicy.InventorySnapshot(
            servers = emptyList(),
            favoriteIdentities = emptySet(),
            autoMode = true,
            selectedIdentity = null,
        )
        val sync = coordinator.syncManagedManifest(snapshot) as CommerceResult.Ok
        check(sync.value.decision.commit)
        val restored = checkNotNull(sync.value.restored)
        check(restored.autoMode)
        val connect = VpnConnectHandoff.resolve(
            auto = restored.autoMode,
            inventory = sync.value.inventory,
            delaysByRemarks = mapOf("Amsterdam" to 42L, "Frankfurt" to 18L),
        )
        val emptySecrets = InMemorySecretStore()
        val emptyMetadata = InMemoryEntitlementMetadataStore()
        val emptyIntents = InMemoryCheckoutIntentStore()
        val reinstall = relaunch(emptySecrets, emptyIntents, emptyMetadata)
        val recovered = reinstall.restoreAccess(
            recoveryCode = (secrets.get(SecretKeys.ENTITLEMENT_CREDENTIAL) as SecretGetResult.Value).utf8(),
            providerTransactionId = "pay_sandbox_${created.id}",
        )
        return Report(
            plans = plans,
            order = (backend.getOrder(created.id) as CommerceResult.Ok).value,
            browserClaimedPaid = browserClaimedPaid,
            presentation = CommerceAccessResolver.resolve(coordinator.collectFacts()),
            credentialStored = secrets.get(SecretKeys.ENTITLEMENT_CREDENTIAL) is SecretGetResult.Value,
            syncCommitted = sync.value.decision.commit,
            autoMode = restored.autoMode,
            connect = connect,
            restoredAfterReinstall = recovered is CommerceResult.Ok &&
                CommerceAccessResolver.resolve(reinstall.collectFacts()) ==
                CommercialPresentationState.HOTFOX_ACTIVE,
        )
    }
}
