package com.v2ray.ang.commerce

/**
 * Transactional HotFox-managed / external subscription refresh policy.
 * Fetch into a temporary result, validate, then atomically swap. Empty or malformed
 * payloads never replace last-known-good inventory. AUTO and favorites are restored
 * from the pre-refresh snapshot.
 */
object ManifestRefreshPolicy {
    data class ServerIdentity(
        val remarks: String,
        val server: String,
        val port: String,
    )

    data class InventorySnapshot(
        val servers: List<ServerIdentity>,
        val favoriteIdentities: Set<ServerIdentity>,
        val autoMode: Boolean,
        val selectedIdentity: ServerIdentity?,
        val lastAttemptAt: Long = 0L,
        val lastSuccessAt: Long = 0L,
        val lastError: String? = null,
    )

    data class RefreshDecision(
        val commit: Boolean,
        val error: String? = null,
    )

    data class RestoredSelection(
        val autoMode: Boolean,
        val favoriteIdentities: Set<ServerIdentity>,
        val selectedIdentity: ServerIdentity?,
    )

    fun shouldCommitSwap(parsedCount: Int, malformed: Boolean = false): Boolean {
        if (malformed) return false
        return parsedCount > 0
    }

    fun decide(parsedCount: Int, malformed: Boolean, emptyPayload: Boolean): RefreshDecision {
        return when {
            malformed -> RefreshDecision(commit = false, error = "malformed_manifest")
            emptyPayload || parsedCount <= 0 -> RefreshDecision(commit = false, error = "empty_manifest")
            else -> RefreshDecision(commit = true)
        }
    }

    fun inventoryAfter(
        snapshot: InventorySnapshot,
        newInventory: List<ServerIdentity>,
        decision: RefreshDecision,
    ): List<ServerIdentity> = if (decision.commit) newInventory else snapshot.servers

    fun restoreAfterSuccessfulSwap(
        snapshot: InventorySnapshot,
        newInventory: List<ServerIdentity>,
    ): RestoredSelection {
        val newSet = newInventory.toSet()
        val favorites = snapshot.favoriteIdentities.filter { it in newSet }.toSet()
        val selected = snapshot.selectedIdentity?.takeIf { it in newSet } ?: newInventory.firstOrNull()
        return RestoredSelection(
            autoMode = snapshot.autoMode,
            favoriteIdentities = favorites,
            selectedIdentity = selected,
        )
    }
}
