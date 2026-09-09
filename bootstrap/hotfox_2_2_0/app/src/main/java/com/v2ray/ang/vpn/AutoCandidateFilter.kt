package com.v2ray.ang.vpn

import com.v2ray.ang.ops.HotfoxNodeDrain

/**
 * Eligibility before AUTO ranking. One invalid/blocked entry must not poison
 * the remaining candidates. Manual HTTPS servers are not entitlement-gated.
 * Drained nodes are excluded from new AUTO picks only.
 */
enum class AutoFilterReason {
    ELIGIBLE,
    AUTO_SENTINEL,
    MISSING_GUID,
    MISSING_CONFIG,
    DISABLED,
    DRAINED,
    ENTITLEMENT_BLOCKED,
}

data class AutoEligibilityDecision(
    val candidate: HotfoxServerSelection.Candidate,
    val reason: AutoFilterReason,
) {
    val eligible: Boolean get() = reason == AutoFilterReason.ELIGIBLE
}

object AutoCandidateFilter {
    fun reasonOf(
        candidate: HotfoxServerSelection.Candidate,
        excludeDrained: Boolean = true,
    ): AutoFilterReason {
        if (candidate.guid.isBlank()) return AutoFilterReason.MISSING_GUID
        if (candidate.guid == HotfoxServerSelection.AUTO_GUID) return AutoFilterReason.AUTO_SENTINEL
        if (!candidate.hasConfig) return AutoFilterReason.MISSING_CONFIG
        if (candidate.disabled) return AutoFilterReason.DISABLED
        if (excludeDrained && candidate.guid in HotfoxNodeDrain.activeGuids()) {
            return AutoFilterReason.DRAINED
        }
        if (candidate.requiresEntitlement && !candidate.entitlementUsable) {
            return AutoFilterReason.ENTITLEMENT_BLOCKED
        }
        return AutoFilterReason.ELIGIBLE
    }

    fun evaluateAll(
        servers: List<HotfoxServerSelection.Candidate>,
        excludeDrained: Boolean = true,
    ): List<AutoEligibilityDecision> =
        servers.map { AutoEligibilityDecision(it, reasonOf(it, excludeDrained)) }

    fun eligible(
        servers: List<HotfoxServerSelection.Candidate>,
        excludeDrained: Boolean = true,
    ): List<HotfoxServerSelection.Candidate> =
        evaluateAll(servers, excludeDrained).filter { it.eligible }.map { it.candidate }

    fun filteredCounts(servers: List<HotfoxServerSelection.Candidate>): Map<AutoFilterReason, Int> =
        evaluateAll(servers, excludeDrained = true).groupingBy { it.reason }.eachCount()
}
