package com.v2ray.ang.vpn

/**
 * Eligibility before AUTO ranking. One invalid/blocked entry must not poison
 * the remaining candidates. Manual HTTPS servers are not entitlement-gated.
 */
enum class AutoFilterReason {
    ELIGIBLE,
    AUTO_SENTINEL,
    MISSING_GUID,
    MISSING_CONFIG,
    DISABLED,
    ENTITLEMENT_BLOCKED,
}

data class AutoEligibilityDecision(
    val candidate: HotfoxServerSelection.Candidate,
    val reason: AutoFilterReason,
) {
    val eligible: Boolean get() = reason == AutoFilterReason.ELIGIBLE
}

object AutoCandidateFilter {
    fun reasonOf(candidate: HotfoxServerSelection.Candidate): AutoFilterReason {
        if (candidate.guid.isBlank()) return AutoFilterReason.MISSING_GUID
        if (candidate.guid == HotfoxServerSelection.AUTO_GUID) return AutoFilterReason.AUTO_SENTINEL
        if (!candidate.hasConfig) return AutoFilterReason.MISSING_CONFIG
        if (candidate.disabled) return AutoFilterReason.DISABLED
        if (candidate.requiresEntitlement && !candidate.entitlementUsable) {
            return AutoFilterReason.ENTITLEMENT_BLOCKED
        }
        return AutoFilterReason.ELIGIBLE
    }

    fun evaluateAll(servers: List<HotfoxServerSelection.Candidate>): List<AutoEligibilityDecision> =
        servers.map { AutoEligibilityDecision(it, reasonOf(it)) }

    fun eligible(servers: List<HotfoxServerSelection.Candidate>): List<HotfoxServerSelection.Candidate> =
        evaluateAll(servers).filter { it.eligible }.map { it.candidate }

    fun filteredCounts(servers: List<HotfoxServerSelection.Candidate>): Map<AutoFilterReason, Int> =
        evaluateAll(servers).groupingBy { it.reason }.eachCount()
}
