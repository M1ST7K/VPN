package com.v2ray.ang.commerce

import com.v2ray.ang.vpn.HotfoxServerSelection

/**
 * Hands a HotFox-managed inventory to the existing 2.2 AUTO/connect resolver.
 * Does not start VpnService, publish CONNECTED, or mark pathVerified.
 */
object VpnConnectHandoff {
    fun resolve(
        auto: Boolean,
        inventory: List<ManifestRefreshPolicy.ServerIdentity>,
        delaysByRemarks: Map<String, Long> = emptyMap(),
        selectedRemarks: String? = null,
    ): HotfoxServerSelection.ResolveResult {
        val candidates = inventory.mapIndexed { index, identity ->
            HotfoxServerSelection.Candidate(
                guid = "hotfox-managed-$index",
                remarks = identity.remarks,
                delay = delaysByRemarks[identity.remarks] ?: 0L,
            )
        }
        val selectedGuid = selectedRemarks?.let { remarks ->
            candidates.firstOrNull { it.remarks == remarks }?.guid
        } ?: candidates.firstOrNull()?.guid
        return HotfoxServerSelection.pick(
            servers = candidates,
            auto = auto,
            selectedGuid = selectedGuid,
        )
    }
}
