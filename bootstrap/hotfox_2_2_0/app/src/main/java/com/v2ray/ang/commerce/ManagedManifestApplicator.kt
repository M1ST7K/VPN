package com.v2ray.ang.commerce

import com.v2ray.ang.dto.entities.ProfileItem

/**
 * Applies an authenticated HotFox-managed payload to the server repository.
 * Identity-only JSON is rejected. Share-link / subscription text is parsed and
 * validated before any live inventory write. Empty, malformed, incomplete, or
 * failed replacements keep last-known-good inventory. Subscription URLs are
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
        val subId = managedSubscriptionId().ifBlank { "hotfox-managed" }
        val fromPayload = ManagedConfigParser.parse(manifest.payload, subId)
        when (fromPayload) {
            is ManagedConfigParser.Result.Complete -> return commit(snapshot, subId, fromPayload.profiles)
            is ManagedConfigParser.Result.Incomplete -> {
                return keep(snapshot, ManifestRefreshPolicy.decide(0, malformed = false, emptyPayload = false, incomplete = true))
            }
            is ManagedConfigParser.Result.Malformed -> {
                return keep(snapshot, ManifestRefreshPolicy.decide(0, malformed = true, emptyPayload = false))
            }
            is ManagedConfigParser.Result.Empty -> Unit
        }
        val url = subscriptionUrl?.takeIf { it.isNotBlank() } ?: manifest.subscriptionUrl
        if (!url.isNullOrBlank()) {
            val body = fetchUrl(url)
            if (body.isNullOrBlank()) {
                return keep(snapshot, ManifestRefreshPolicy.decide(0, malformed = false, emptyPayload = true))
            }
            return when (val fetched = ManagedConfigParser.parse(body, subId)) {
                is ManagedConfigParser.Result.Complete -> commit(snapshot, subId, fetched.profiles)
                is ManagedConfigParser.Result.Incomplete ->
                    keep(snapshot, ManifestRefreshPolicy.decide(0, malformed = false, emptyPayload = false, incomplete = true))
                is ManagedConfigParser.Result.Malformed ->
                    keep(snapshot, ManifestRefreshPolicy.decide(0, malformed = true, emptyPayload = false))
                is ManagedConfigParser.Result.Empty ->
                    keep(snapshot, ManifestRefreshPolicy.decide(0, malformed = false, emptyPayload = true))
            }
        }
        return keep(snapshot, ManifestRefreshPolicy.decide(0, malformed = false, emptyPayload = true))
    }

    private fun commit(
        snapshot: ManifestRefreshPolicy.InventorySnapshot,
        subId: String,
        items: List<ProfileItem>,
    ): CommerceSubscriptionSync.Outcome {
        val written = store.replaceManaged(subId, items)
        if (written.isEmpty()) {
            return keep(
                snapshot,
                ManifestRefreshPolicy.decide(items.size, malformed = false, emptyPayload = false, replaceFailed = true),
            )
        }
        store.restoreSelection(snapshot)
        return CommerceSubscriptionSync.Outcome(
            decision = ManifestRefreshPolicy.RefreshDecision(commit = true),
            inventory = written.map { it.identity() },
            restored = ManifestRefreshPolicy.restoreAfterSuccessfulSwap(snapshot, written.map { it.identity() }),
            profiles = store.profiles(),
        )
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
