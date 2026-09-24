package com.v2ray.ang.vpn

/**
 * Binds a routing-policy reconnect to [VpnRestartGate] so an explicit user
 * disconnect/teardown cannot be overwritten by a stale await-idle restart.
 */
object HotfoxRoutingRestart {
    data class Plan(
        val restartRequest: Long,
        val routingGeneration: Long,
    )

    fun begin(): Plan = Plan(
        restartRequest = VpnRestartGate.nextRequest(),
        routingGeneration = HotfoxRoutingApply.current(),
    )

    /**
     * Starts only when the restart request is still live and [routingGeneration]
     * is still the latest policy. A later [VpnRestartGate.invalidate] (user
     * stop) or a newer [HotfoxRoutingApply.bump] cancels this plan.
     */
    fun tryDispatch(plan: Plan, dispatch: () -> Unit): Boolean {
        if (plan.routingGeneration > 0L && plan.routingGeneration != HotfoxRoutingApply.current()) {
            return false
        }
        return VpnRestartGate.tryDispatchStart(plan.restartRequest) {
            if (plan.routingGeneration > 0L) {
                HotfoxRoutingApply.tryApply(plan.routingGeneration)
            }
            dispatch()
        }
    }
}
