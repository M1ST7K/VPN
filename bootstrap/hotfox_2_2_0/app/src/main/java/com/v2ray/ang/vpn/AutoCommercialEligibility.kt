package com.v2ray.ang.vpn

import com.v2ray.ang.commerce.CommerceCoordinator
import com.v2ray.ang.commerce.CommerceFacts
import com.v2ray.ang.commerce.CommercePreferences
import com.v2ray.ang.commerce.EntitlementMetadata
import com.v2ray.ang.commerce.EntitlementParser
import com.v2ray.ang.commerce.EntitlementStateMachine
import com.v2ray.ang.commerce.MmkvEntitlementMetadataStore

/**
 * Authoritative commercial eligibility for AUTO ranking.
 * Manual HTTPS servers are not entitlement-gated. Managed HotFox profiles are.
 */
object AutoCommercialEligibility {
    data class Snapshot(
        val accessOrigin: String,
        val managedSubscriptionId: String?,
        val entitlementUsable: Boolean,
        val hasCredential: Boolean,
    ) {
        fun requiresEntitlement(subscriptionId: String): Boolean {
            if (accessOrigin != CommercePreferences.ORIGIN_HOTFOX) return false
            val managed = managedSubscriptionId
            return managed.isNullOrBlank() || managed == subscriptionId
        }
    }

    fun from(
        accessOrigin: String,
        managedSubscriptionId: String?,
        hasCredential: Boolean,
        metadata: EntitlementMetadata?,
        nowEpochSeconds: Long,
    ): Snapshot {
        val originHotfox = accessOrigin == CommercePreferences.ORIGIN_HOTFOX
        val hasHotfox = hasCredential && metadata != null && originHotfox
        val usable = if (!hasHotfox || metadata == null) {
            false
        } else {
            EntitlementStateMachine.isUsable(EntitlementParser.effectiveStatus(metadata, nowEpochSeconds))
        }
        return Snapshot(
            accessOrigin = accessOrigin,
            managedSubscriptionId = managedSubscriptionId,
            entitlementUsable = usable,
            hasCredential = hasCredential,
        )
    }

    fun fromFacts(
        facts: CommerceFacts,
        accessOrigin: String,
        managedSubscriptionId: String?,
    ): Snapshot {
        val usable = facts.hasHotfoxEntitlement &&
            facts.entitlementStatus != null &&
            EntitlementStateMachine.isUsable(facts.entitlementStatus)
        return Snapshot(
            accessOrigin = accessOrigin,
            managedSubscriptionId = managedSubscriptionId,
            entitlementUsable = usable,
            hasCredential = facts.hasHotfoxEntitlement,
        )
    }

    fun live(nowEpochSeconds: Long = System.currentTimeMillis() / 1000L): Snapshot {
        val origin = runCatching { CommercePreferences.accessOrigin() }
            .getOrDefault(CommercePreferences.ORIGIN_NONE)
        val managed = runCatching { CommercePreferences.hotfoxSubscriptionId() }.getOrNull()
        val fromCoordinator = runCatching {
            CommerceCoordinator.peek()?.collectFacts()
        }.getOrNull()
        if (fromCoordinator != null) {
            val usable = fromCoordinator.hasHotfoxEntitlement &&
                fromCoordinator.entitlementStatus != null &&
                EntitlementStateMachine.isUsable(fromCoordinator.entitlementStatus)
            return Snapshot(
                accessOrigin = origin,
                managedSubscriptionId = managed,
                entitlementUsable = usable,
                hasCredential = fromCoordinator.hasHotfoxEntitlement,
            )
        }
        val metadata = runCatching { MmkvEntitlementMetadataStore.read() }.getOrNull()
        val hasCredentialHint = metadata != null && origin == CommercePreferences.ORIGIN_HOTFOX
        return from(
            accessOrigin = origin,
            managedSubscriptionId = managed,
            hasCredential = hasCredentialHint,
            metadata = metadata,
            nowEpochSeconds = nowEpochSeconds,
        )
    }
}
