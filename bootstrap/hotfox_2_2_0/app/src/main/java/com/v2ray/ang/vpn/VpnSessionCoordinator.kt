package com.v2ray.ang.vpn

import com.v2ray.ang.AppConfig
import com.v2ray.ang.util.LogUtil
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlinx.coroutines.delay

data class StopTicket(
    val epoch: Long,
    val owned: Boolean,
)

/**
 * Process-scoped session coordinator. Survives Activity recreation while the
 * VPN process remains alive. CONNECTED is never inferred from a Boolean flag and
 * cannot be published without a generation-scoped, path-verified completion.
 *
 * Generation checks and mutations share [generationLock] so a newer [beginAttempt]
 * cannot be overwritten by a stale path/error write.
 */
object VpnSessionCoordinator {
    private val generationLock = Any()
    private val attemptId = AtomicLong(0L)
    private val state = AtomicReference(VpnSessionState.DISCONNECTED)
    private val sessionStartElapsed = AtomicLong(0L)
    private val lastError = AtomicReference<String?>(null)
    private val lastPath = AtomicReference<VpnPathVerification?>(null)
    private val lastStage = AtomicReference(VpnConnectionStage.IDLE)
    private val teardownActive = AtomicBoolean(false)
    private val lastStopSucceeded = AtomicBoolean(true)
    private val stopEpoch = AtomicLong(0L)
    private val lifecycleLock = ReentrantLock(true)
    val traffic = HotfoxTrafficAccumulator()

    fun beginAttempt(): Long {
        synchronized(generationLock) {
            if (teardownActive.get()) {
                LogUtil.w(AppConfig.TAG, "VpnSession: beginAttempt refused; teardown active")
                return 0L
            }
            val id = attemptId.incrementAndGet()
            lastPath.set(null)
            lastError.set(null)
            lastStage.set(VpnConnectionStage.RESOLVE_SERVER)
            state.set(VpnSessionState.PREPARING)
            return id
        }
    }

    fun recordStage(attempt: Long, stage: VpnConnectionStage): Boolean {
        synchronized(generationLock) {
            if (!matches(attempt)) return false
            lastStage.set(stage)
            return true
        }
    }

    fun lastStage(): VpnConnectionStage = lastStage.get()

    fun isCurrent(id: Long): Boolean = id != 0L && id == attemptId.get()

    fun currentAttempt(): Long = attemptId.get()

    fun currentState(): VpnSessionState = state.get()

    fun lastPath(): VpnPathVerification? = lastPath.get()

    fun isTeardownActive(): Boolean = teardownActive.get()

    fun lastStopSucceeded(): Boolean = lastStopSucceeded.get()

    fun setTeardownActive(active: Boolean) {
        synchronized(generationLock) {
            teardownActive.set(active)
        }
    }

    /**
     * Atomically claim teardown for [ownedAttempt] **before** start admission
     * is released. A stale attempt cannot mark teardown or move a newer
     * session to DISCONNECTING.
     */
    fun claimTeardown(ownedAttempt: Long): Boolean {
        synchronized(generationLock) {
            if (ownedAttempt == 0L || !matches(ownedAttempt)) {
                return false
            }
            teardownActive.set(true)
            val current = state.get()
            if (current != VpnSessionState.ERROR && current != VpnSessionState.RECONNECTING) {
                lastStage.set(VpnConnectionStage.STOPPING)
                state.set(VpnSessionState.DISCONNECTING)
            }
            return true
        }
    }

    /**
     * Core-stop result. Barrier clears only after confirmed successful termination
     * **and** the caller has finished resource cleanup. Failed/exception stops stay
     * fail-closed. A stale epoch/attempt cannot disconnect a newer session.
     */
    fun currentStopEpoch(): Long = stopEpoch.get()

    fun completeStopOutcome(stopLoopSucceeded: Boolean): Boolean =
        completeStopOutcome(stopEpoch.get(), currentAttempt(), stopLoopSucceeded)

    fun completeStopOutcome(epoch: Long, attempt: Long, stopLoopSucceeded: Boolean): Boolean {
        synchronized(generationLock) {
            if (stopEpoch.get() != epoch) return false
            lastStopSucceeded.set(stopLoopSucceeded)
            if (stopLoopSucceeded) {
                if (!matches(attempt)) return false
                teardownActive.set(false)
                if (state.get() != VpnSessionState.ERROR) {
                    attemptId.incrementAndGet()
                    lastPath.set(null)
                    lastError.set(null)
                    lastStage.set(VpnConnectionStage.IDLE)
                    sessionStartElapsed.set(0L)
                    traffic.reset()
                    state.set(VpnSessionState.DISCONNECTED)
                }
                return true
            }
            teardownActive.set(true)
            lastStage.set(VpnConnectionStage.STOPPING)
            if (state.get() != VpnSessionState.ERROR) {
                lastError.set("HF-VPN-008 Ядро не остановилось")
                sessionStartElapsed.set(0L)
                traffic.reset()
                lastPath.set(null)
                state.set(VpnSessionState.ERROR)
                attemptId.incrementAndGet()
            }
            return true
        }
    }

    /**
     * Late worker success after the owner's timeout. Clears the barrier only for
     * the still-current stop epoch, after the caller has finished leftover cleanup.
     * Does not disconnect a newer start attempt (epoch mismatch).
     */
    fun completeLateStopSuccess(epoch: Long): Boolean {
        synchronized(generationLock) {
            if (stopEpoch.get() != epoch) return false
            lastStopSucceeded.set(true)
            teardownActive.set(false)
            val current = state.get()
            if (current == VpnSessionState.ERROR ||
                current == VpnSessionState.DISCONNECTING
            ) {
                lastPath.set(null)
                lastStage.set(VpnConnectionStage.IDLE)
                sessionStartElapsed.set(0L)
                traffic.reset()
                state.set(VpnSessionState.DISCONNECTED)
            }
            return true
        }
    }

    fun recordPath(verification: VpnPathVerification): Boolean =
        recordPath(currentAttempt(), verification)

    fun recordPath(attempt: Long, verification: VpnPathVerification): Boolean {
        synchronized(generationLock) {
            if (!matches(attempt)) return false
            lastPath.set(verification)
            return true
        }
    }

    fun setState(next: VpnSessionState): Boolean = setState(currentAttempt(), next)

    fun setState(attempt: Long, next: VpnSessionState): Boolean {
        synchronized(generationLock) {
            if (next == VpnSessionState.CONNECTED) {
                LogUtil.w(AppConfig.TAG, "VpnSession: setState(CONNECTED) ignored; use markConnected")
                return false
            }
            if (!matches(attempt)) return false
            if (next == VpnSessionState.DISCONNECTED) {
                sessionStartElapsed.set(0L)
                traffic.reset()
            }
            state.set(next)
            return true
        }
    }

    fun markConnected(attempt: Long, pathVerified: Boolean): Boolean {
        synchronized(generationLock) {
            if (!matches(attempt)) {
                LogUtil.w(AppConfig.TAG, "VpnSession: stale markConnected attempt=$attempt current=${attemptId.get()}")
                return false
            }
            if (teardownActive.get()) {
                LogUtil.w(AppConfig.TAG, "VpnSession: markConnected refused; teardown active")
                return false
            }
            if (!pathVerified) {
                LogUtil.w(AppConfig.TAG, "VpnSession: markConnected refused without path verification")
                return false
            }
            if (sessionStartElapsed.get() == 0L) {
                sessionStartElapsed.set(elapsedRealtimeMs())
            }
            lastStage.set(VpnConnectionStage.VERIFIED)
            state.set(VpnSessionState.CONNECTED)
            return true
        }
    }

    fun markProxyOnly(attempt: Long): Boolean {
        synchronized(generationLock) {
            if (!matches(attempt) || teardownActive.get()) return false
            state.set(VpnSessionState.PROXY_ONLY)
            return true
        }
    }

    fun markRootRunning(attempt: Long): Boolean {
        synchronized(generationLock) {
            if (!matches(attempt) || teardownActive.get()) return false
            state.set(VpnSessionState.ROOT_RUNNING)
            return true
        }
    }

    fun markReconnecting(attempt: Long): Boolean {
        synchronized(generationLock) {
            if (!matches(attempt) || teardownActive.get()) return false
            state.set(VpnSessionState.RECONNECTING)
            return true
        }
    }

    fun markDisconnected() {
        synchronized(generationLock) {
            attemptId.incrementAndGet()
            lastPath.set(null)
            lastStage.set(VpnConnectionStage.IDLE)
            state.set(VpnSessionState.DISCONNECTED)
            sessionStartElapsed.set(0L)
            traffic.reset()
            lastError.set(null)
            teardownActive.set(false)
            lastStopSucceeded.set(true)
        }
    }

    fun markError(code: String, message: String): Boolean =
        markError(currentAttempt(), code, message)

    fun markError(attempt: Long, code: String, message: String): Boolean {
        synchronized(generationLock) {
            if (!matches(attempt)) {
                LogUtil.w(
                    AppConfig.TAG,
                    "VpnSession: stale markError $code attempt=$attempt current=${attemptId.get()}",
                )
                return false
            }
            lastPath.set(null)
            lastError.set("$code $message".trim())
            sessionStartElapsed.set(0L)
            traffic.reset()
            state.set(VpnSessionState.ERROR)
            attemptId.incrementAndGet()
            return true
        }
    }

    fun markStopIncomplete(message: String) {
        completeStopOutcome(false)
        synchronized(generationLock) {
            lastError.set("HF-VPN-008 $message".trim())
        }
    }

    fun duplicateStartDisposition(): DuplicateStartDisposition {
        if (teardownActive.get()) return DuplicateStartDisposition.IGNORE
        return when (val current = state.get()) {
            VpnSessionState.CONNECTED -> DuplicateStartDisposition.REPUBLISH
            VpnSessionState.PROXY_ONLY, VpnSessionState.ROOT_RUNNING -> DuplicateStartDisposition.REPUBLISH
            else -> if (current.isBusy()) DuplicateStartDisposition.IGNORE else DuplicateStartDisposition.START
        }
    }

    fun beginStart() {
        lifecycleLock.lock()
    }

    fun endStart() {
        unlockIfHeld()
    }

    fun tryBeginReload(): Boolean {
        if (teardownActive.get()) return false
        if (state.get() != VpnSessionState.CONNECTED) return false
        if (!lifecycleLock.tryLock()) return false
        if (teardownActive.get() || state.get() != VpnSessionState.CONNECTED) {
            lifecycleLock.unlock()
            return false
        }
        return true
    }

    fun endReload() {
        unlockIfHeld()
    }

    /**
     * Owns a stop generation. Join vs mint is decided under [lifecycleLock] so a
     * concurrent caller cannot snapshot "no worker" and then supersede the live
     * epoch after the first stop times out.
     */
    fun beginStop(): StopTicket {
        lifecycleLock.lock()
        synchronized(generationLock) {
            val current = stopEpoch.get()
            if (teardownActive.get() && current != 0L) {
                return StopTicket(epoch = current, owned = false)
            }
            teardownActive.set(true)
            lastStage.set(VpnConnectionStage.STOPPING)
            return StopTicket(epoch = stopEpoch.incrementAndGet(), owned = true)
        }
    }

    fun endStop() {
        unlockIfHeld()
    }

    fun tryLockForTest(timeoutMs: Long): Boolean =
        lifecycleLock.tryLock(timeoutMs, TimeUnit.MILLISECONDS)

    fun unlockForTest() {
        unlockIfHeld()
    }

    private fun unlockIfHeld() {
        if (lifecycleLock.isHeldByCurrentThread) {
            lifecycleLock.unlock()
        }
    }

    private fun matches(attempt: Long): Boolean = attempt != 0L && attempt == attemptId.get()

    fun sessionStartedAtElapsed(): Long? {
        val started = sessionStartElapsed.get()
        return started.takeIf { it > 0L }
    }

    fun lastError(): String? = lastError.get()

    suspend fun awaitIdle(timeoutMillis: Long = 8_000L): Boolean {
        val deadline = System.nanoTime() / 1_000_000L + timeoutMillis
        while (System.nanoTime() / 1_000_000L < deadline) {
            if (isIdle()) return true
            delay(40L)
        }
        return isIdle()
    }

    private fun isIdle(): Boolean {
        if (teardownActive.get()) return false
        val current = state.get()
        return current == VpnSessionState.DISCONNECTED ||
            (current == VpnSessionState.ERROR && lastStopSucceeded.get())
    }

    fun resetForTests() {
        while (lifecycleLock.isHeldByCurrentThread) {
            lifecycleLock.unlock()
        }
        synchronized(generationLock) {
            attemptId.set(0L)
            state.set(VpnSessionState.DISCONNECTED)
            sessionStartElapsed.set(0L)
            lastError.set(null)
            lastPath.set(null)
            lastStage.set(VpnConnectionStage.IDLE)
            teardownActive.set(false)
            lastStopSucceeded.set(true)
            stopEpoch.set(0L)
            traffic.reset()
        }
    }

    internal fun elapsedRealtimeMs(): Long =
        runCatching { android.os.SystemClock.elapsedRealtime() }.getOrElse { System.nanoTime() / 1_000_000L }
}
