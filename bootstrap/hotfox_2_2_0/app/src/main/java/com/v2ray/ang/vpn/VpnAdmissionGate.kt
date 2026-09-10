package com.v2ray.ang.vpn

import java.util.concurrent.atomic.AtomicLong

/**
 * Admission epoch for async VPN start. A rejected stale teardown must not
 * invalidate a live admission. Only a successful teardown claim may bump
 * the epoch.
 */
object VpnAdmissionGate {
    private val epoch = AtomicLong(0L)

    fun snapshot(): Long = epoch.get()

    fun isCurrent(admission: Long): Boolean = admission == epoch.get()

    fun invalidateAfterClaim(claimed: Boolean): Boolean {
        if (!claimed) return false
        epoch.incrementAndGet()
        return true
    }

    fun shouldAbandonEstablished(
        admission: Long,
        currentEpoch: Long,
        pipelineCurrent: Boolean,
        cancelled: Boolean,
    ): Boolean {
        return cancelled || admission != currentEpoch || !pipelineCurrent
    }

    fun resetForTests() {
        epoch.set(0L)
    }
}
