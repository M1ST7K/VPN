package com.v2ray.ang.commerce

import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.util.JsonUtil
import com.v2ray.ang.util.Utils
import java.util.UUID

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
    val profileItem: ProfileItem? = null,
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

    companion object {
        fun from(guid: String, item: ProfileItem): ManagedProfile {
            val identity = HotfoxManifestRefresh.identityOf(item)
            return ManagedProfile(
                guid = guid,
                remarks = identity.remarks,
                server = identity.server,
                port = identity.port,
                protocol = identity.protocol,
                network = identity.network,
                security = identity.security,
                fingerprint = identity.fingerprint,
                subscriptionId = item.subscriptionId,
                profileItem = item,
            )
        }
    }
}

/**
 * Persisted VPN inventory used by HotFox-managed sync. Production writes ProfileItem
 * records through MMKV; JVM tests use the in-memory implementation.
 *
 * Replacement is staged: new profiles are parsed and written before the live
 * subscription list is swapped. Any failure restores the previous inventory.
 */
interface ManagedServerStore {
    fun snapshot(): ManifestRefreshPolicy.InventorySnapshot
    fun profiles(): List<ManagedProfile>
    fun profileItems(): List<ProfileItem>
    fun replaceManaged(subscriptionId: String, items: List<ProfileItem>): List<ManagedProfile>
    fun importConfigText(body: String, subscriptionId: String): Int
    fun restoreSelection(snapshot: ManifestRefreshPolicy.InventorySnapshot)
}

class InMemoryManagedServerStore : ManagedServerStore {
    private val items = LinkedHashMap<String, ProfileItem>()
    @Volatile var autoMode: Boolean = true
    @Volatile var selectedGuid: String? = null
    private val favoriteGuids = LinkedHashSet<String>()

    /** Fail after this many successful staged puts. Null disables injection. */
    @Volatile var failPutsAfter: Int? = null
    @Volatile var failBeforeSwap: Boolean = false

    override fun snapshot(): ManifestRefreshPolicy.InventorySnapshot {
        val identities = items.map { (_, item) -> HotfoxManifestRefresh.identityOf(item) }
        return ManifestRefreshPolicy.InventorySnapshot(
            servers = identities,
            favoriteIdentities = favoriteGuids.mapNotNull { guid -> items[guid]?.let { HotfoxManifestRefresh.identityOf(it) } }.toSet(),
            autoMode = autoMode,
            selectedIdentity = selectedGuid?.let { guid -> items[guid]?.let { HotfoxManifestRefresh.identityOf(it) } },
        )
    }

    override fun profiles(): List<ManagedProfile> =
        items.map { (guid, item) -> ManagedProfile.from(guid, item) }

    override fun profileItems(): List<ProfileItem> = items.values.toList()

    override fun replaceManaged(subscriptionId: String, items: List<ProfileItem>): List<ManagedProfile> {
        if (items.isEmpty() || items.any { !ManagedConfigParser.isXrayUsable(it) }) {
            return emptyList()
        }
        val previous = LinkedHashMap(this.items)
        val previousAuto = autoMode
        val previousSelected = selectedGuid
        val previousFavorites = LinkedHashSet(favoriteGuids)
        return try {
            if (failBeforeSwap) {
                error("injected replace failure")
            }
            val staged = LinkedHashMap<String, ProfileItem>()
            previous.filterValues { it.subscriptionId != subscriptionId }.forEach { (guid, item) ->
                staged[guid] = item
            }
            items.forEachIndexed { index, item ->
                val limit = failPutsAfter
                if (limit != null && index >= limit) {
                    error("injected put failure")
                }
                staged[newGuid()] = item.copy(subscriptionId = subscriptionId)
            }
            this.items.clear()
            this.items.putAll(staged)
            profiles().filter { it.subscriptionId == subscriptionId }
        } catch (_: Exception) {
            this.items.clear()
            this.items.putAll(previous)
            autoMode = previousAuto
            selectedGuid = previousSelected
            favoriteGuids.clear()
            favoriteGuids.addAll(previousFavorites)
            emptyList()
        }
    }

    override fun importConfigText(body: String, subscriptionId: String): Int {
        return when (val parsed = ManagedConfigParser.parse(body, subscriptionId)) {
            is ManagedConfigParser.Result.Complete -> {
                val written = replaceManaged(subscriptionId, parsed.profiles)
                if (written.isEmpty()) 0 else written.size
            }
            else -> 0
        }
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

    private fun newGuid(): String = UUID.randomUUID().toString().replace("-", "")
}

object MmkvManagedServerStore : ManagedServerStore {
    override fun snapshot(): ManifestRefreshPolicy.InventorySnapshot = HotfoxManifestRefresh.captureSnapshot()

    override fun profiles(): List<ManagedProfile> {
        return profileItemsByGuid().map { (guid, item) -> ManagedProfile.from(guid, item) }
    }

    override fun profileItems(): List<ProfileItem> = profileItemsByGuid().values.toList()

    override fun replaceManaged(subscriptionId: String, items: List<ProfileItem>): List<ManagedProfile> {
        if (items.isEmpty() || items.any { !ManagedConfigParser.isXrayUsable(it) }) {
            return emptyList()
        }
        val previousGuids = MmkvManager.decodeServerList(subscriptionId).toList()
        val previousSelected = MmkvManager.getSelectServer()
        val previousItems = previousGuids.mapNotNull { guid ->
            MmkvManager.decodeServerConfig(guid)?.let { guid to it }
        }
        val stagedGuids = mutableListOf<String>()
        return try {
            items.forEach { item ->
                val stored = item.copy(subscriptionId = subscriptionId)
                val guid = Utils.getUuid().ifBlank { UUID.randomUUID().toString().replace("-", "") }
                MmkvManager.encodeProfileDirect(guid, JsonUtil.toJson(stored))
                stagedGuids.add(guid)
            }
            MmkvManager.encodeServerList(stagedGuids.toMutableList(), subscriptionId)
            previousGuids.forEach { oldGuid ->
                if (oldGuid !in stagedGuids) {
                    MmkvManager.removeServer(oldGuid)
                }
            }
            stagedGuids.mapNotNull { guid ->
                MmkvManager.decodeServerConfig(guid)?.let { ManagedProfile.from(guid, it) }
            }
        } catch (_: Exception) {
            stagedGuids.forEach { guid ->
                runCatching { MmkvManager.removeServer(guid) }
            }
            previousItems.forEach { (guid, item) ->
                runCatching { MmkvManager.encodeProfileDirect(guid, JsonUtil.toJson(item)) }
            }
            runCatching { MmkvManager.encodeServerList(previousGuids.toMutableList(), subscriptionId) }
            previousSelected?.takeIf { it.isNotBlank() }?.let { MmkvManager.setSelectServer(it) }
            emptyList()
        }
    }

    override fun importConfigText(body: String, subscriptionId: String): Int {
        return when (val parsed = ManagedConfigParser.parse(body, subscriptionId)) {
            is ManagedConfigParser.Result.Complete -> {
                val written = replaceManaged(subscriptionId, parsed.profiles)
                if (written.isEmpty()) 0 else written.size
            }
            else -> 0
        }
    }

    override fun restoreSelection(snapshot: ManifestRefreshPolicy.InventorySnapshot) {
        HotfoxManifestRefresh.restoreAfterSuccess(snapshot)
    }

    private fun profileItemsByGuid(): Map<String, ProfileItem> {
        val result = LinkedHashMap<String, ProfileItem>()
        MmkvManager.decodeAllServerList().forEach { guid ->
            val profile = MmkvManager.decodeServerConfig(guid) ?: return@forEach
            result[guid] = profile
        }
        return result
    }
}
