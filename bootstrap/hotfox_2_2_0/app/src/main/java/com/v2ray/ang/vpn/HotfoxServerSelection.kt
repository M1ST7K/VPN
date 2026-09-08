package com.v2ray.ang.vpn

import com.v2ray.ang.handler.MmkvManager

/**
 * AUTO vs manual server selection. AUTO is a persisted mode, not a fake ProfileItem.
 * Manual taps must not be overwritten by import/refresh.
 */
object HotfoxServerSelection {
    const val AUTO_GUID = "hotfox-auto-server"
    const val PREF_AUTO_SERVER = "pref_hotfox_auto_server"

    fun isAutoMode(): Boolean = MmkvManager.decodeSettingsBool(PREF_AUTO_SERVER, true)

    fun setAutoMode(enabled: Boolean) {
        MmkvManager.encodeSettings(PREF_AUTO_SERVER, enabled)
    }

    fun selectManual(guid: String) {
        applyPersisted(persistAfterTap(tapGuid = guid, previousGuid = MmkvManager.getSelectServer(), firstUsableGuid = firstUsableGuid()))
    }

    fun selectAuto() {
        applyPersisted(persistAfterTap(tapGuid = AUTO_GUID, previousGuid = MmkvManager.getSelectServer(), firstUsableGuid = firstUsableGuid()))
    }

    /**
     * After import/refresh/delete: keep a valid GUID for display.
     * AUTO stays AUTO; a missing/invalid manual selection falls back to the first usable server.
     */
    fun ensureValidSelection() {
        val persisted = persistAfterEnsureValid(
            auto = isAutoMode(),
            selectedGuid = MmkvManager.getSelectServer(),
            inventory = usableGuids(),
        )
        persisted.selectedGuid?.let { MmkvManager.setSelectServer(it) }
    }

    fun firstUsableGuid(): String? = usableGuids().firstOrNull()

    fun alreadySelected(tapGuid: String, selectedGuid: String?, auto: Boolean): Boolean {
        if (tapGuid.isBlank() || tapGuid == AUTO_GUID) return auto
        return !auto && tapGuid == selectedGuid
    }

    fun persistAfterTap(
        tapGuid: String,
        previousGuid: String?,
        firstUsableGuid: String?,
    ): PersistedSelection {
        if (tapGuid.isBlank() || tapGuid == AUTO_GUID) {
            val keep = previousGuid?.takeIf { it.isNotBlank() && it != AUTO_GUID } ?: firstUsableGuid
            return PersistedSelection(auto = true, selectedGuid = keep)
        }
        return PersistedSelection(auto = false, selectedGuid = tapGuid)
    }

    fun persistAfterEnsureValid(
        auto: Boolean,
        selectedGuid: String?,
        inventory: List<String>,
    ): PersistedSelection {
        val valid = !selectedGuid.isNullOrBlank() &&
            selectedGuid != AUTO_GUID &&
            inventory.contains(selectedGuid)
        if (valid) return PersistedSelection(auto = auto, selectedGuid = selectedGuid)
        return PersistedSelection(auto = auto, selectedGuid = inventory.firstOrNull())
    }

    fun resolveForConnect(): ResolveResult {
        return persistResolution(
            pick(
                servers = candidates(),
                auto = isAutoMode(),
                selectedGuid = MmkvManager.getSelectServer(),
            )
        )
    }

    /**
     * Network handover: keep the current AUTO target when it is still healthy
     * (`delay > 0`). Only re-pick when the current target is missing, untested,
     * or marked unreachable. Manual selection is never replaced by a faster peer.
     */
    fun resolveForHandover(): ResolveResult {
        return persistResolution(
            resolveForHandover(
                servers = candidates(),
                auto = isAutoMode(),
                selectedGuid = MmkvManager.getSelectServer(),
            )
        )
    }

    fun resolveForHandover(
        servers: List<Candidate>,
        auto: Boolean,
        selectedGuid: String?,
    ): ResolveResult {
        if (auto) {
            val current = servers.firstOrNull { it.guid == selectedGuid }
            if (current != null && current.delay > 0L) {
                return ResolveResult.Success(current.guid, resolvedFromAuto = true)
            }
        }
        return pick(servers, auto, selectedGuid)
    }

    fun pick(
        servers: List<Candidate>,
        auto: Boolean,
        selectedGuid: String?,
    ): ResolveResult {
        if (servers.isEmpty()) {
            return ResolveResult.Failure("HF-VPN-010 Нет серверов")
        }
        if (!auto) {
            val match = servers.firstOrNull { it.guid == selectedGuid }
            if (match == null) {
                val fallback = servers.first()
                return ResolveResult.Success(fallback.guid, resolvedFromAuto = false)
            }
            if (match.delay < 0L) {
                return ResolveResult.Failure("HF-VPN-011 Сервер недоступен")
            }
            return ResolveResult.Success(match.guid, resolvedFromAuto = false)
        }
        val healthy = servers.filter { it.delay > 0L }.minByOrNull { it.delay }
        if (healthy != null) {
            return ResolveResult.Success(healthy.guid, resolvedFromAuto = true)
        }
        val untested = servers.filter { it.delay == 0L }
        if (untested.isNotEmpty()) {
            return ResolveResult.Success(untested.first().guid, resolvedFromAuto = true)
        }
        return ResolveResult.Failure("HF-VPN-011 Нет доступных серверов")
    }

    fun matchImportedKey(
        keys: Collection<String>,
        previousSelectedStillPresent: String?,
    ): String? {
        if (!previousSelectedStillPresent.isNullOrBlank() && keys.contains(previousSelectedStillPresent)) {
            return previousSelectedStillPresent
        }
        return keys.firstOrNull()
    }

    private fun applyPersisted(persisted: PersistedSelection) {
        setAutoMode(persisted.auto)
        persisted.selectedGuid?.let { MmkvManager.setSelectServer(it) }
    }

    private fun persistResolution(result: ResolveResult): ResolveResult {
        if (result is ResolveResult.Success) {
            MmkvManager.setSelectServer(result.guid)
        }
        return result
    }

    private fun usableGuids(): List<String> =
        MmkvManager.decodeAllServerList().filter { guid ->
            guid != AUTO_GUID && MmkvManager.decodeServerConfig(guid) != null
        }

    private fun candidates(): List<Candidate> =
        MmkvManager.decodeAllServerList().mapNotNull { guid ->
            if (guid == AUTO_GUID) return@mapNotNull null
            val profile = MmkvManager.decodeServerConfig(guid) ?: return@mapNotNull null
            val delay = MmkvManager.decodeServerAffiliationInfo(guid)?.testDelayMillis ?: 0L
            Candidate(guid, profile.remarks, delay)
        }

    data class Candidate(val guid: String, val remarks: String, val delay: Long)

    data class PersistedSelection(val auto: Boolean, val selectedGuid: String?)

    sealed class ResolveResult {
        data class Success(val guid: String, val resolvedFromAuto: Boolean) : ResolveResult()
        data class Failure(val message: String) : ResolveResult()
    }
}

/** First list row is always AUTO; real servers follow; footer is last. */
object HotfoxServerListContract {
    const val AUTO_ROW_INDEX = 0
    const val VIEW_TYPE_AUTO = 0
    const val VIEW_TYPE_ITEM = 1
    const val VIEW_TYPE_FOOTER = 2

    fun itemCount(serverCount: Int): Int = serverCount + 2

    fun viewType(position: Int, serverCount: Int): Int {
        return when {
            position == AUTO_ROW_INDEX -> VIEW_TYPE_AUTO
            position == serverCount + 1 -> VIEW_TYPE_FOOTER
            else -> VIEW_TYPE_ITEM
        }
    }

    /**
     * Maps a `serversCache` index to the RecyclerView adapter position.
     * AUTO occupies row 0; real servers are offset by +1.
     * @return adapter position, or -1 when a manual selection is not in this list.
     */
    fun adapterPositionForSelection(auto: Boolean, dataIndex: Int): Int {
        if (auto) return AUTO_ROW_INDEX
        if (dataIndex < 0) return -1
        return AUTO_ROW_INDEX + 1 + dataIndex
    }
}
