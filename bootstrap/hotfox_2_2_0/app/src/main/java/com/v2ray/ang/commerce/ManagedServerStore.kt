package com.v2ray.ang.commerce

import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.handler.AngConfigManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.vpn.HotfoxServerSelection

data class ManagedProfile(
    val guid: String,
    val remarks: String,
    val server: String,
    val port: String,
    val protocol: String,
    val network: String,
    val security: String,
    val fingerprint: String,
    val subscriptionId: String,
) {
    fun identity(): ManifestRefreshPolicy.ServerIdentity = ManifestRefreshPolicy.ServerIdentity(
        remarks = remarks,
        server = server,
        port = port,
        protocol = protocol,
        network = network,
        security = security,
        fingerprint = fingerprint,
    )
}

/**
 * Persisted VPN inventory used by HotFox-managed sync. Production writes ProfileItem
 * records through MMKV; JVM tests use the in-memory implementation.
 */
interface ManagedServerStore {
    fun snapshot(): ManifestRefreshPolicy.InventorySnapshot
    fun profiles(): List<ManagedProfile>
    fun replaceManaged(subscriptionId: String, profiles: List<ManagedProfile>): List<ManagedProfile>
    fun importConfigText(body: String, subscriptionId: String): Int
    fun restoreSelection(snapshot: ManifestRefreshPolicy.InventorySnapshot)
}

class InMemoryManagedServerStore : ManagedServerStore {
    private val servers = LinkedHashMap<String, ManagedProfile>()
    @Volatile var autoMode: Boolean = true
    @Volatile var selectedGuid: String? = null
    private val favoriteGuids = LinkedHashSet<String>()

    override fun snapshot(): ManifestRefreshPolicy.InventorySnapshot {
        val identities = servers.values.map { it.identity() }
        return ManifestRefreshPolicy.InventorySnapshot(
            servers = identities,
            favoriteIdentities = favoriteGuids.mapNotNull { guid -> servers[guid]?.identity() }.toSet(),
            autoMode = autoMode,
            selectedIdentity = selectedGuid?.let { servers[it]?.identity() },
        )
    }

    override fun profiles(): List<ManagedProfile> = servers.values.toList()

    override fun replaceManaged(subscriptionId: String, profiles: List<ManagedProfile>): List<ManagedProfile> {
        val stale = servers.filterValues { it.subscriptionId == subscriptionId }.keys.toList()
        stale.forEach { servers.remove(it) }
        val written = profiles.map { profile ->
            val stored = profile.copy(subscriptionId = subscriptionId)
            servers[stored.guid] = stored
            stored
        }
        return written
    }

    override fun importConfigText(body: String, subscriptionId: String): Int {
        val parsed = ManagedManifestParser.parse(
            CommerceManifest(format = ManagedManifestParser.FORMAT, payload = body),
        )
        if (parsed.malformed || parsed.servers.isEmpty()) return 0
        replaceManaged(subscriptionId, parsed.servers.map { ManagedManifestParser.toProfile(it, subscriptionId) })
        return parsed.servers.size
    }

    override fun restoreSelection(snapshot: ManifestRefreshPolicy.InventorySnapshot) {
        val restored = ManifestRefreshPolicy.restoreAfterSuccessfulSwap(snapshot, profiles().map { it.identity() })
        autoMode = restored.autoMode
        restored.selectedIdentity?.let { identity ->
            selectedGuid = profiles().firstOrNull { it.identity() == identity }?.guid
        }
        favoriteGuids.clear()
        restored.favoriteIdentities.forEach { identity ->
            profiles().firstOrNull { it.identity() == identity }?.guid?.let { favoriteGuids.add(it) }
        }
    }
}

object MmkvManagedServerStore : ManagedServerStore {
    override fun snapshot(): ManifestRefreshPolicy.InventorySnapshot = HotfoxManifestRefresh.captureSnapshot()

    override fun profiles(): List<ManagedProfile> {
        return MmkvManager.decodeAllServerList().mapNotNull { guid ->
            val profile = MmkvManager.decodeServerConfig(guid) ?: return@mapNotNull null
            val identity = HotfoxManifestRefresh.identityOf(profile)
            ManagedProfile(
                guid = guid,
                remarks = identity.remarks,
                server = identity.server,
                port = identity.port,
                protocol = identity.protocol,
                network = identity.network,
                security = identity.security,
                fingerprint = identity.fingerprint,
                subscriptionId = profile.subscriptionId,
            )
        }
    }

    override fun replaceManaged(subscriptionId: String, profiles: List<ManagedProfile>): List<ManagedProfile> {
        MmkvManager.removeServerViaSubid(subscriptionId)
        return profiles.map { profile ->
            val item = ProfileItem.create(EConfigType.VLESS).apply {
                this.subscriptionId = subscriptionId
                remarks = profile.remarks
                server = profile.server
                serverPort = profile.port
                network = profile.network.ifBlank { "tcp" }
                security = profile.security
                fingerPrint = profile.fingerprint
            }
            val guid = MmkvManager.encodeServerConfig(profile.guid, item)
            profile.copy(guid = guid, subscriptionId = subscriptionId)
        }
    }

    override fun importConfigText(body: String, subscriptionId: String): Int {
        return AngConfigManager.importManagedSubscriptionText(body, subscriptionId)
    }

    override fun restoreSelection(snapshot: ManifestRefreshPolicy.InventorySnapshot) {
        HotfoxManifestRefresh.restoreAfterSuccess(snapshot)
    }
}
