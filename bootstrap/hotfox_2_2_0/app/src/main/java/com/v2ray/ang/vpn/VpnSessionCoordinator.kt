package com.v2ray.ang.vpn

import com.v2ray.ang.AppConfig
import com.v2ray.ang.util.LogUtil
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlinx.coroutines.delay

/**
 * Process-scoped session coordinator. Survives Activity recreation while the
 * VPN process remains alive. CONNECTED is never inferred from a Boolean flag and
 * cannot be published without a generation-scoped, path-verified completion.
 */
object VpnSessionCoordinator {
    private val attemptId = AtomicLong(0L)
    private val state = AtomicReference(VpnSessionState.DISCONNECTED)
    private val sessionStartElapsed = AtomicLong(0L)
    private val lastError = AtomicReference<String?>(null)
    private val lastPath = AtomicReference<VpnPathVerification?>(null)
    private val teardownActive = AtomicBoolean(false)
    private val lifecycleLock = ReentrantLock(true)
    val traffic = HotfoxTrafficAccumulator()

    fun beginAttempt(): Long {
        if (teardownActive.get()) {
            LogUtil.w(AppConfig.TAG, "VpnSession: beginAttempt refused; teardown active")
            return 0L
        }
        val id = attemptId.incrementAndGet()
        lastPath.set(null)
        lastError.set(null)
        state.set(VpnSessionState.PREPARING)
        return id
    }

    fun isCurrent(id: Long): Boolean = id != 0L && id == attemptId.get()

    fun currentAttempt(): Long = attemptId.get()

    fun currentState(): VpnSessionState = state.get()

    fun lastPath(): VpnPathVerification? = lastPath.get()

    fun isTeardownActive(): Boolean = teardownActive.get()

    fun setTeardownActive(active: Boolean) {
        teardownActive.set(active)
    }

    fun recordPath(verification: VpnPathVerification): Boolean =
        recordPath(currentAttempt(), verification)

    fun recordPath(attempt: Long, verification: VpnPathVerification): Boolean {
        if (!isCurrent(attempt)) {
            LogUtil.w(
                AppConfig.TAG,
                "VpnSession: stale recordPath attempt=$attempt current=${attemptId.get()}",
            )
            return false
        }
        lastPath.set(verification)
        return true
    }

    /**
     * Intermediate states only. [VpnSessionState.CONNECTED] must go through
     * [markConnected] with a current attempt and a verified path.
     */
    fun setState(next: VpnSessionState): Boolean = setState(currentAttempt(), next)

    fun setState(attempt: Long, next: VpnSessionState): Boolean {
        if (next == VpnSessionState.CONNECTED) {
            LogUtil.w(AppConfig.TAG, "VpnSession: setState(CONNECTED) ignored; use markConnected")
            return false
        }
        if (!isCurrent(attempt)) {
            LogUtil.w(
                AppConfig.TAG,
                "VpnSession: stale setState ${next.name} attempt=$attempt current=${attemptId.get()}",
            )
            return false
        }
        if (next == VpnSessionState.DISCONNECTED) {
            sessionStartElapsed.set(0L)
            traffic.reset()
        }
        state.set(next)
        return true
    }

    fun markConnected(attempt: Long, pathVerified: Boolean): Boolean {
        if (!isCurrent(attempt)) {
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
        state.set(VpnSessionState.CONNECTED)
        return true
    }

    fun markProxyOnly(attempt: Long): Boolean {
        if (!isCurrent(attempt) || teardownActive.get()) return false
        state.set(VpnSessionState.PROXY_ONLY)
        return true
    }

    fun markRootRunning(attempt: Long): Boolean {
        if (!isCurrent(attempt) || teardownActive.get()) return false
        state.set(VpnSessionState.ROOT_RUNNING)
        return true
    }

    fun markReconnecting(attempt: Long): Boolean {
        if (!isCurrent(attempt) || teardownActive.get()) return false
        state.set(VpnSessionState.RECONNECTING)
        return true
    }

    fun markDisconnected() {
        attemptId.incrementAndGet()
        lastPath.set(null)
        state.set(VpnSessionState.DISCONNECTED)
        sessionStartElapsed.set(0L)
        traffic.reset()
        lastError.set(null)
        teardownActive.set(false)
    }

    fun markError(code: String, message: String): Boolean =
        markError(currentAttempt(), code, message)

    fun markError(attempt: Long, code: String, message: String): Boolean {
        if (!isCurrent(attempt)) {
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

    fun markStopIncomplete(message: String) {
        teardownActive.set(true)
        lastError.set("HF-VPN-008 $message".trim())
        sessionStartElapsed.set(0L)
        traffic.reset()
        lastPath.set(null)
        state.set(VpnSessionState.ERROR)
        attemptId.incrementAndGet()
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

    fun beginStop() {
        lifecycleLock.lock()
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
        return current == VpnSessionState.DISCONNECTED || current == VpnSessionState.ERROR
    }

    fun resetForTests() {
        while (lifecycleLock.isHeldByCurrentThread) {
            lifecycleLock.unlock()
        }
        attemptId.set(0L)
        state.set(VpnSessionState.DISCONNECTED)
        sessionStartElapsed.set(0L)
        lastError.set(null)
        lastPath.set(null)
        teardownActive.set(false)
        traffic.reset()
    }

    internal fun elapsedRealtimeMs(): Long =
        runCatching { android.os.SystemClock.elapsedRealtime() }.getOrElse { System.nanoTime() / 1_000_000L }
}
