package com.v2ray.ang.commerce

/**
 * Applies an authenticated HotFox-managed manifest to the real server repository.
 * Empty/malformed payloads keep last-known-good inventory. Subscription URLs are
 * fetched through [fetchUrl] and must not be written to MMKV or logs.
 */
class ManagedManifestApplicator(
    private val store: ManagedServerStore,
    private val fetchUrl: (String) -> String? = { null },
    private val managedSubscriptionId: () -> String = { "hotfox-managed" },
) {
    fun apply(
        snapshot: ManifestRefreshPolicy.InventorySnapshot,
        manifest: CommerceManifest,
        subscriptionUrl: String? = null,
    ): CommerceSubscriptionSync.Outcome {
        val parsed = ManagedManifestParser.parse(manifest)
        val jsonDecision = ManifestRefreshPolicy.decide(
            parsedCount = parsed.servers.size,
            malformed = parsed.malformed,
            emptyPayload = parsed.servers.isEmpty(),
        )
        if (parsed.malformed) {
            return keep(snapshot, jsonDecision)
        }
        val subId = managedSubscriptionId().ifBlank { "hotfox-managed" }
        if (parsed.servers.isNotEmpty()) {
            val written = store.replaceManaged(
                subId,
                parsed.servers.map { ManagedManifestParser.toProfile(it, subId) },
            )
            store.restoreSelection(snapshot)
            return CommerceSubscriptionSync.Outcome(
                decision = ManifestRefreshPolicy.RefreshDecision(commit = true),
                inventory = written.map { it.identity() },
                restored = ManifestRefreshPolicy.restoreAfterSuccessfulSwap(snapshot, written.map { it.identity() }),
                profiles = store.profiles(),
            )
        }
        val url = subscriptionUrl?.takeIf { it.isNotBlank() } ?: manifest.subscriptionUrl
        if (!url.isNullOrBlank()) {
            val body = fetchUrl(url)
            if (body.isNullOrBlank()) {
                return keep(snapshot, ManifestRefreshPolicy.RefreshDecision(commit = false, error = "empty_manifest"))
            }
            val count = store.importConfigText(body, subId)
            val decision = ManifestRefreshPolicy.decide(
                parsedCount = count,
                malformed = count <= 0,
                emptyPayload = count <= 0,
            )
            if (!decision.commit) {
                return keep(snapshot, decision)
            }
            store.restoreSelection(snapshot)
            val profiles = store.profiles()
            return CommerceSubscriptionSync.Outcome(
                decision = ManifestRefreshPolicy.RefreshDecision(commit = true),
                inventory = profiles.map { it.identity() },
                restored = ManifestRefreshPolicy.restoreAfterSuccessfulSwap(snapshot, profiles.map { it.identity() }),
                profiles = profiles,
            )
        }
        return keep(snapshot, jsonDecision)
    }

    private fun keep(
        snapshot: ManifestRefreshPolicy.InventorySnapshot,
        decision: ManifestRefreshPolicy.RefreshDecision,
    ): CommerceSubscriptionSync.Outcome {
        return CommerceSubscriptionSync.Outcome(
            decision = decision,
            inventory = snapshot.servers,
            restored = null,
            profiles = store.profiles(),
        )
    }
}
