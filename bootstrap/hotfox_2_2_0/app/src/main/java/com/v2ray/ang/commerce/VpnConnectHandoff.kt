package com.v2ray.ang.commerce

import com.v2ray.ang.vpn.AutoCommercialEligibility
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
        eligibility: AutoCommercialEligibility.Snapshot? = null,
    ): HotfoxServerSelection.ResolveResult {
        val snapshot = eligibility ?: runCatching { AutoCommercialEligibility.live() }.getOrElse {
            AutoCommercialEligibility.Snapshot(
                accessOrigin = CommercePreferences.ORIGIN_NONE,
                managedSubscriptionId = null,
                entitlementUsable = false,
                hasCredential = false,
            )
        }
        val candidates = profiles.map { profile ->
            val delay = delaysByRemarks[profile.remarks] ?: 0L
            val item = profile.profileItem
            if (item != null) {
                HotfoxServerSelection.candidateFrom(
                    guid = profile.guid,
                    profile = item,
                    delayMs = delay,
                    subscriptionEnabled = true,
                    eligibility = snapshot,
                ).copy(delayNetworkScoped = delaysByRemarks.containsKey(profile.remarks))
            } else {
                HotfoxServerSelection.Candidate(
                    guid = profile.guid,
                    remarks = profile.remarks,
                    delay = delay,
                    hasConfig = true,
                    requiresEntitlement = snapshot.requiresEntitlement(profile.subscriptionId),
                    entitlementUsable = snapshot.entitlementUsable,
                    delayNetworkScoped = delaysByRemarks.containsKey(profile.remarks),
                )
            }
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
        eligibility: AutoCommercialEligibility.Snapshot? = null,
    ): HotfoxServerSelection.ResolveResult {
        val snapshot = store.snapshot()
        return resolve(
            auto = snapshot.autoMode,
            profiles = store.profiles(),
            delaysByRemarks = delaysByRemarks,
            selectedGuid = store.profiles().firstOrNull { profile ->
                snapshot.selectedIdentity != null && profile.identity() == snapshot.selectedIdentity
            }?.guid,
            eligibility = eligibility,
        )
    }
}
