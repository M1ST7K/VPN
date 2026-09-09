package com.v2ray.ang.vpn

import com.v2ray.ang.commerce.ManagedConfigParser
import com.v2ray.ang.commerce.CommercePreferences
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.handler.MmkvManager

/**
 * AUTO vs manual server selection. AUTO is a persisted mode, not a fake ProfileItem.
 * Manual taps must not be overwritten by import/refresh.
 */
object HotfoxServerSelection {
    const val AUTO_GUID = "hotfox-auto-server"
    const val PREF_AUTO_SERVER = "pref_hotfox_auto_server"
    const val PREF_LAST_GOOD_AUTO = "pref_hotfox_last_good_auto_guid"

    val health = ServerHealthRepository()

    @Volatile
    var lastAutoReason: String = ""
        private set

    @Volatile
    var lastGoodAutoGuid: String? = null
        private set

    fun recordAutoReason(reason: String) {
        lastAutoReason = reason
    }

    fun rememberLastGoodAuto(guid: String) {
        if (guid.isBlank() || guid == AUTO_GUID) return
        val auto = runCatching { isAutoMode() }.getOrDefault(true)
        if (!auto) return
        lastGoodAutoGuid = guid
        runCatching { MmkvManager.encodeSettings(PREF_LAST_GOOD_AUTO, guid) }
    }

    fun lastGoodAutoGuidOrPersisted(): String? {
        lastGoodAutoGuid?.takeIf { it.isNotBlank() }?.let { return it }
        return runCatching { MmkvManager.decodeSettingsString(PREF_LAST_GOOD_AUTO) }.getOrNull()
            ?.takeIf { it.isNotBlank() && it != AUTO_GUID }
    }

    fun invalidateForNetworkChange(): Long = health.invalidateForNetworkChange()

    fun resetLastGoodForTests() {
        lastGoodAutoGuid = null
        lastAutoReason = ""
    }

    fun persistAutoTarget(guid: String) {
        if (guid.isBlank() || guid == AUTO_GUID) return
        MmkvManager.setSelectServer(guid)
    }

    fun currentCandidates(): List<Candidate> = candidates()

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
        val servers = candidates()
        val now = System.currentTimeMillis()
        val snapshot = healthSnapshot(servers, now)
        return persistResolution(
            pick(
                servers = servers,
                auto = isAutoMode(),
                selectedGuid = MmkvManager.getSelectServer(),
                healthByGuid = snapshot,
                nowEpochMs = now,
                lastGoodGuid = lastGoodAutoGuidOrPersisted(),
                networkContext = health.networkContext,
            ),
            snapshot,
            now,
        )
    }

    /**
     * Network handover: keep the current AUTO target when it remains eligible.
     * Latency from the previous network context is not used to flap to a "faster" peer.
     */
    fun resolveForHandover(): ResolveResult {
        val servers = candidates()
        val now = System.currentTimeMillis()
        val snapshot = healthSnapshot(servers, now)
        return persistResolution(
            resolveForHandover(
                servers = servers,
                auto = isAutoMode(),
                selectedGuid = MmkvManager.getSelectServer(),
                healthByGuid = snapshot,
                nowEpochMs = now,
                lastGoodGuid = lastGoodAutoGuidOrPersisted(),
                networkChanged = true,
                networkContext = health.networkContext,
            ),
            snapshot,
            now,
        )
    }

    fun resolveForHandover(
        servers: List<Candidate>,
        auto: Boolean,
        selectedGuid: String?,
        healthByGuid: Map<String, ServerHealth> = emptyMap(),
        nowEpochMs: Long = 0L,
        lastGoodGuid: String? = null,
        networkChanged: Boolean = false,
        networkContext: Long = 0L,
    ): ResolveResult {
        return AutoSelectionPolicy.handover(
            servers = servers,
            healthByGuid = healthByGuid,
            auto = auto,
            selectedGuid = selectedGuid,
            nowEpochMs = nowEpochMs,
            lastGoodGuid = lastGoodGuid,
            networkChanged = networkChanged,
            networkContext = networkContext,
        )
    }

    fun pick(
        servers: List<Candidate>,
        auto: Boolean,
        selectedGuid: String?,
        healthByGuid: Map<String, ServerHealth> = emptyMap(),
        nowEpochMs: Long = 0L,
        lastGoodGuid: String? = null,
        networkContext: Long = 0L,
    ): ResolveResult {
        return AutoSelectionPolicy.pick(
            servers = servers,
            healthByGuid = healthByGuid,
            auto = auto,
            selectedGuid = selectedGuid,
            nowEpochMs = nowEpochMs,
            lastGoodGuid = lastGoodGuid,
            networkContext = networkContext,
        )
    }

    fun resolveForFailover(attempt: Int): FailoverDecision {
        val servers = candidates()
        val now = System.currentTimeMillis()
        return AutoSelectionPolicy.failover(
            servers = servers,
            healthByGuid = healthSnapshot(servers, now),
            auto = isAutoMode(),
            selectedGuid = MmkvManager.getSelectServer(),
            attempt = attempt,
            nowEpochMs = now,
            lastGoodGuid = lastGoodAutoGuidOrPersisted(),
            networkContext = health.networkContext,
        )
    }

    fun beginProbeCycle(guids: Collection<String>): Long {
        health.retain(guids.toSet())
        val generation = health.bumpGeneration()
        guids.forEach { health.markProbeInFlight(it, generation) }
        return generation
    }

    fun ingestProbeResult(guid: String, delayMs: Long, generation: Long, nowEpochMs: Long = System.currentTimeMillis()) {
        health.record(
            ProbeSample(
                guid = guid,
                success = delayMs > 0L,
                latencyMs = delayMs.takeIf { it > 0L },
                observedAtEpochMs = nowEpochMs,
                generation = generation,
            )
        )
    }

    fun cancelProbeCycle(guids: Collection<String>) {
        guids.forEach { health.clearInFlight(it) }
    }

    fun healthSnapshot(
        servers: List<Candidate>,
        nowEpochMs: Long = 0L,
        stored: Map<String, ServerHealth> = health.all(),
    ): Map<String, ServerHealth> {
        val currentContext = health.networkContext
        return servers.associate { candidate ->
            val live = stored[candidate.guid]
            val merged = when {
                live != null && live.networkContext != currentContext ->
                    live.copy(
                        latestLatencyMs = null,
                        ewmaLatencyMs = null,
                        jitterMs = null,
                        availability = ServerAvailability.UNKNOWN,
                        probeInFlight = false,
                        networkContext = currentContext,
                    )
                live != null -> live
                else -> candidate.healthHint(nowEpochMs, currentContext)
            }
            candidate.guid to merged
        }
    }

    data class AutoDiagnosticSnapshot(
        val candidateCount: Int,
        val eligibleCount: Int,
        val filteredCount: Int,
        val networkContext: Long,
        val lastGoodPresent: Boolean,
        val reason: String,
    )

    fun diagnosticSnapshot(): AutoDiagnosticSnapshot {
        return runCatching {
            val servers = candidates()
            val counts = AutoCandidateFilter.filteredCounts(servers)
            val eligible = counts[AutoFilterReason.ELIGIBLE] ?: 0
            AutoDiagnosticSnapshot(
                candidateCount = servers.size,
                eligibleCount = eligible,
                filteredCount = servers.size - eligible,
                networkContext = health.networkContext,
                lastGoodPresent = !lastGoodAutoGuidOrPersisted().isNullOrBlank(),
                reason = lastAutoReason.ifBlank { "none" },
            )
        }.getOrElse {
            AutoDiagnosticSnapshot(
                candidateCount = 0,
                eligibleCount = 0,
                filteredCount = 0,
                networkContext = health.networkContext,
                lastGoodPresent = lastGoodAutoGuid != null,
                reason = lastAutoReason.ifBlank { "none" },
            )
        }
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

    private fun persistResolution(
        result: ResolveResult,
        healthByGuid: Map<String, ServerHealth> = emptyMap(),
        nowEpochMs: Long = 0L,
    ): ResolveResult {
        lastAutoReason = AutoSelectionPolicy.diagnosticReason(result, healthByGuid, nowEpochMs)
        if (result is ResolveResult.Success) {
            MmkvManager.setSelectServer(result.guid)
        }
        return result
    }

    private fun usableGuids(): List<String> =
        MmkvManager.decodeAllServerList().filter { guid ->
            guid != AUTO_GUID && MmkvManager.decodeServerConfig(guid) != null
        }

    fun candidateFrom(
        guid: String,
        profile: ProfileItem,
        delayMs: Long,
        subscriptionEnabled: Boolean,
        eligibility: AutoCommercialEligibility.Snapshot,
    ): Candidate {
        return Candidate(
            guid = guid,
            remarks = profile.remarks,
            delay = delayMs,
            hasConfig = ManagedConfigParser.isXrayUsable(profile),
            disabled = !subscriptionEnabled,
            requiresEntitlement = eligibility.requiresEntitlement(profile.subscriptionId),
            entitlementUsable = eligibility.entitlementUsable,
            delayNetworkScoped = false,
            network = profile.network,
            security = profile.security,
            ipv6 = false,
        )
    }

    private fun candidates(): List<Candidate> {
        val eligibility = runCatching { AutoCommercialEligibility.live() }.getOrElse {
            AutoCommercialEligibility.Snapshot(
                accessOrigin = CommercePreferences.ORIGIN_NONE,
                managedSubscriptionId = null,
                entitlementUsable = false,
                hasCredential = false,
            )
        }
        return MmkvManager.decodeAllServerList().mapNotNull { guid ->
            if (guid == AUTO_GUID) return@mapNotNull null
            val profile = MmkvManager.decodeServerConfig(guid) ?: return@mapNotNull null
            val delay = MmkvManager.decodeServerAffiliationInfo(guid)?.testDelayMillis ?: 0L
            val subscriptionEnabled = runCatching {
                val subId = profile.subscriptionId
                if (subId.isBlank()) {
                    true
                } else {
                    MmkvManager.decodeSubscription(subId)?.enabled ?: true
                }
            }.getOrDefault(true)
            candidateFrom(guid, profile, delay, subscriptionEnabled, eligibility)
        }
    }

    data class Candidate(
        val guid: String,
        val remarks: String,
        val delay: Long,
        val hasConfig: Boolean = true,
        val disabled: Boolean = false,
        val requiresEntitlement: Boolean = false,
        val entitlementUsable: Boolean = true,
        /**
         * Test/explicit current-network delay may rank. Unscoped MMKV affiliation
         * ping must not be relabeled as health for a new network context.
         */
        val delayNetworkScoped: Boolean = true,
        val network: String? = "tcp",
        val security: String? = "reality",
        val ipv6: Boolean = false,
    ) {
        fun healthHint(nowEpochMs: Long, networkContext: Long): ServerHealth {
            if (!delayNetworkScoped) {
                return ServerHealth(guid = guid, networkContext = networkContext)
            }
            return ServerHealthMath.fromCachedDelay(guid, delay, nowEpochMs, networkContext)
        }
    }

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
