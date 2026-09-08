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
        if (guid.isBlank() || guid == AUTO_GUID) {
            selectAuto()
            return
        }
        setAutoMode(false)
        MmkvManager.setSelectServer(guid)
    }

    fun selectAuto() {
        setAutoMode(true)
        firstUsableGuid()?.let { MmkvManager.setSelectServer(it) }
    }

    /**
     * After import/refresh/delete: keep a valid GUID for display.
     * AUTO stays AUTO; a missing/invalid manual selection falls back to the first usable server.
     */
    fun ensureValidSelection() {
        val selected = MmkvManager.getSelectServer()
        val valid = !selected.isNullOrBlank() &&
            selected != AUTO_GUID &&
            MmkvManager.decodeServerConfig(selected) != null
        if (valid) {
            if (isAutoMode()) return
            return
        }
        val fallback = firstUsableGuid() ?: return
        MmkvManager.setSelectServer(fallback)
    }

    fun firstUsableGuid(): String? =
        MmkvManager.decodeAllServerList().firstOrNull { guid ->
            guid != AUTO_GUID && MmkvManager.decodeServerConfig(guid) != null
        }

    fun resolveForConnect(): ResolveResult {
        val servers = MmkvManager.decodeAllServerList().mapNotNull { guid ->
            val profile = MmkvManager.decodeServerConfig(guid) ?: return@mapNotNull null
            val delay = MmkvManager.decodeServerAffiliationInfo(guid)?.testDelayMillis ?: 0L
            Candidate(guid, profile.remarks, delay)
        }
        val result = pick(
            servers = servers,
            auto = isAutoMode(),
            selectedGuid = MmkvManager.getSelectServer(),
        )
        if (result is ResolveResult.Success) {
            MmkvManager.setSelectServer(result.guid)
        }
        return result
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

    data class Candidate(val guid: String, val remarks: String, val delay: Long)

    sealed class ResolveResult {
        data class Success(val guid: String, val resolvedFromAuto: Boolean) : ResolveResult()
        data class Failure(val message: String) : ResolveResult()
    }
}
