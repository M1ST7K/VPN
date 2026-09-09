package com.v2ray.ang.commerce

import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.vpn.HotfoxServerSelection
import java.security.MessageDigest

/**
 * Captures last-known-good inventory and restores AUTO / favorites / manual selection
 * after a successful validated swap. Identity includes connection-defining semantics so
 * two profiles that share host/port but differ in protocol, transport, or credentials
 * are not treated as the same server.
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

    fun identityOf(profile: ProfileItem): ManifestRefreshPolicy.ServerIdentity {
        val protocol = profile.configType.name
        val network = profile.network.orEmpty()
        val security = profile.security.orEmpty()
        val fingerprint = fingerprintOf(
            protocol,
            profile.server.orEmpty(),
            profile.serverPort.orEmpty(),
            network,
            security,
            profile.flow.orEmpty(),
            profile.path.orEmpty(),
            profile.host.orEmpty(),
            profile.sni.orEmpty(),
            profile.serviceName.orEmpty(),
            profile.xhttpMode.orEmpty(),
            profile.method.orEmpty(),
            profile.password.orEmpty(),
            profile.publicKey.orEmpty(),
            profile.shortId.orEmpty(),
        )
        return ManifestRefreshPolicy.ServerIdentity(
            remarks = profile.remarks,
            server = profile.server.orEmpty(),
            port = profile.serverPort.orEmpty(),
            protocol = protocol,
            network = network,
            security = security,
            fingerprint = fingerprint,
        )
    }

    internal fun fingerprintOf(vararg parts: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        parts.forEach { part ->
            digest.update(part.toByteArray())
            digest.update(0)
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }
}
