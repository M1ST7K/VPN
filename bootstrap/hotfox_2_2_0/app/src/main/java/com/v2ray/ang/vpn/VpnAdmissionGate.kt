package com.v2ray.ang.vpn

import java.util.concurrent.atomic.AtomicLong

/**
 * Admission epoch and commit lock for async VPN start.
 *
 * A rejected stale teardown must not invalidate a live admission. Claim,
 * epoch invalidation, and pipeline cancel must run under [withCommitLock]
 * together with [tryCommitEstablished]. A claimed teardown rejects commit
 * even if the epoch has not been bumped yet, so admission cannot start an
 * obsolete pipeline after disconnect/revoke ownership is taken.
 */
object VpnAdmissionGate {
    private val epoch = AtomicLong(0L)
    private val commitLock = Any()

    fun snapshot(): Long = epoch.get()

    fun isCurrent(admission: Long): Boolean = admission == epoch.get()

    fun <T> withCommitLock(block: () -> T): T = synchronized(commitLock, block)

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

    fun tryCommitEstablished(
        admission: Long,
        pipelineCurrent: Boolean,
        cancelled: Boolean,
        teardownActive: Boolean,
    ): Boolean {
        if (teardownActive) return false
        return !shouldAbandonEstablished(admission, epoch.get(), pipelineCurrent, cancelled)
    }

    fun resetForTests() {
        epoch.set(0L)
    }
}
