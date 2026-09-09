package com.v2ray.ang.commerce

import com.v2ray.ang.vpn.HotfoxServerSelection

/**
 * Hands persisted HotFox-managed profiles to the existing 2.2 AUTO/connect resolver.
 * Uses repository GUIDs. Does not start VpnService, publish CONNECTED, or mark pathVerified.
 */
object VpnConnectHandoff {
    fun resolve(
        auto: Boolean,
        profiles: List<ManagedProfile>,
        delaysByRemarks: Map<String, Long> = emptyMap(),
        selectedGuid: String? = null,
    ): HotfoxServerSelection.ResolveResult {
        val candidates = profiles.map { profile ->
            HotfoxServerSelection.Candidate(
                guid = profile.guid,
                remarks = profile.remarks,
                delay = delaysByRemarks[profile.remarks] ?: 0L,
            )
        }
        return HotfoxServerSelection.pick(
            servers = candidates,
            auto = auto,
            selectedGuid = selectedGuid ?: profiles.firstOrNull()?.guid,
        )
    }

    fun resolve(
        store: ManagedServerStore,
        delaysByRemarks: Map<String, Long> = emptyMap(),
    ): HotfoxServerSelection.ResolveResult {
        val snapshot = store.snapshot()
        return resolve(
            auto = snapshot.autoMode,
            profiles = store.profiles(),
            delaysByRemarks = delaysByRemarks,
            selectedGuid = store.profiles().firstOrNull { profile ->
                snapshot.selectedIdentity != null && profile.identity() == snapshot.selectedIdentity
            }?.guid,
        )
    }
}
