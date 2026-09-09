package com.v2ray.ang.commerce

/**
 * Transactional HotFox-managed manifest apply. Empty or malformed payloads keep
 * last-known-good inventory and AUTO/favorites from the snapshot.
 */
object CommerceSubscriptionSync {
    data class Outcome(
        val decision: ManifestRefreshPolicy.RefreshDecision,
        val inventory: List<ManifestRefreshPolicy.ServerIdentity>,
        val restored: ManifestRefreshPolicy.RestoredSelection?,
        val profiles: List<ManagedProfile> = emptyList(),
    )

    fun apply(
        snapshot: ManifestRefreshPolicy.InventorySnapshot,
        manifest: CommerceManifest,
        store: ManagedServerStore = InMemoryManagedServerStore(),
        fetchUrl: (String) -> String? = { null },
        subscriptionUrl: String? = null,
    ): Outcome {
        return ManagedManifestApplicator(store, fetchUrl).apply(snapshot, manifest, subscriptionUrl)
    }
}
