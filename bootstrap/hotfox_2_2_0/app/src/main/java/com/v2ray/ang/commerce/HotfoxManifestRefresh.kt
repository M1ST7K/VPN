package com.v2ray.ang.commerce

import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.vpn.HotfoxServerSelection

/**
 * Captures last-known-good inventory and restores AUTO / favorites / manual selection
 * after a successful validated swap. Callers must not swap when [ManifestRefreshPolicy.shouldCommitSwap]
 * is false — [com.v2ray.ang.handler.AngConfigManager.updateConfigViaSub] already keeps the previous
 * inventory on empty/malformed payloads.
 */
object HotfoxManifestRefresh {
    fun captureSnapshot(): ManifestRefreshPolicy.InventorySnapshot {
        val identities = LinkedHashMap<String, ManifestRefreshPolicy.ServerIdentity>()
        val favorites = LinkedHashSet<ManifestRefreshPolicy.ServerIdentity>()
        MmkvManager.decodeAllServerList().forEach { guid ->
            val profile = MmkvManager.decodeServerConfig(guid) ?: return@forEach
            val identity = identityOf(profile)
            identities[guid] = identity
            if (MmkvManager.isServerFavorite(guid)) {
                favorites.add(identity)
            }
        }
        val selectedGuid = MmkvManager.getSelectServer()
        val selected = selectedGuid?.let { identities[it] }
        return ManifestRefreshPolicy.InventorySnapshot(
            servers = identities.values.toList(),
            favoriteIdentities = favorites,
            autoMode = HotfoxServerSelection.isAutoMode(),
            selectedIdentity = selected,
            lastAttemptAt = CommercePreferences.lastManifestAttempt(),
            lastSuccessAt = CommercePreferences.lastManifestSuccess(),
            lastError = CommercePreferences.lastManifestError(),
        )
    }

    fun restoreAfterSuccess(snapshot: ManifestRefreshPolicy.InventorySnapshot) {
        val current = LinkedHashMap<ManifestRefreshPolicy.ServerIdentity, String>()
        MmkvManager.decodeAllServerList().forEach { guid ->
            val profile = MmkvManager.decodeServerConfig(guid) ?: return@forEach
            current[identityOf(profile)] = guid
        }
        val restored = ManifestRefreshPolicy.restoreAfterSuccessfulSwap(snapshot, current.keys.toList())
        HotfoxServerSelection.setAutoMode(restored.autoMode)
        restored.favoriteIdentities.forEach { identity ->
            current[identity]?.let { guid -> MmkvManager.setServerFavorite(guid, true) }
        }
        restored.selectedIdentity?.let { identity ->
            current[identity]?.let { guid ->
                if (!restored.autoMode) {
                    HotfoxServerSelection.selectManual(guid)
                } else {
                    MmkvManager.setSelectServer(guid)
                    HotfoxServerSelection.setAutoMode(true)
                }
            }
        }
        if (restored.autoMode) {
            HotfoxServerSelection.setAutoMode(true)
        }
        HotfoxServerSelection.ensureValidSelection()
    }

    fun identityOf(profile: ProfileItem): ManifestRefreshPolicy.ServerIdentity =
        ManifestRefreshPolicy.ServerIdentity(
            remarks = profile.remarks,
            server = profile.server.orEmpty(),
            port = profile.serverPort.orEmpty(),
        )
}
