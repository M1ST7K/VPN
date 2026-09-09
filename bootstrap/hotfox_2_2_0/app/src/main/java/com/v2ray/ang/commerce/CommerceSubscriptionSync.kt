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
    )

    fun apply(
        snapshot: ManifestRefreshPolicy.InventorySnapshot,
        manifest: CommerceManifest,
    ): Outcome {
        val parsed = SandboxManifest.parse(manifest)
        val empty = parsed.servers.isEmpty()
        val decision = ManifestRefreshPolicy.decide(
            parsedCount = parsed.servers.size,
            malformed = parsed.malformed,
            emptyPayload = empty,
        )
        val inventory = ManifestRefreshPolicy.inventoryAfter(snapshot, parsed.servers, decision)
        val restored = if (decision.commit) {
            ManifestRefreshPolicy.restoreAfterSuccessfulSwap(snapshot, parsed.servers)
        } else {
            null
        }
        return Outcome(decision = decision, inventory = inventory, restored = restored)
    }
}
