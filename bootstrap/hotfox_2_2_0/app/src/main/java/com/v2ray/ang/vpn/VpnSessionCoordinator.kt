package com.v2ray.ang.vpn

import com.v2ray.ang.AppConfig
import com.v2ray.ang.util.LogUtil
import java.util.concurrent.TimeUnit
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
    private val lifecycleLock = ReentrantLock(true)
    val traffic = HotfoxTrafficAccumulator()

    fun beginAttempt(): Long {
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

    fun recordPath(verification: VpnPathVerification) {
        lastPath.set(verification)
    }

    /**
     * Intermediate states only. [VpnSessionState.CONNECTED] must go through
     * [markConnected] with a current attempt and a verified path.
     */
    fun setState(next: VpnSessionState) {
        if (next == VpnSessionState.CONNECTED) {
            LogUtil.w(AppConfig.TAG, "VpnSession: setState(CONNECTED) ignored; use markConnected")
            return
        }
        if (next == VpnSessionState.DISCONNECTED || next == VpnSessionState.ERROR) {
            if (next == VpnSessionState.DISCONNECTED) {
                sessionStartElapsed.set(0L)
                traffic.reset()
            }
        }
        state.set(next)
    }

    fun markConnected(attempt: Long, pathVerified: Boolean): Boolean {
        if (!isCurrent(attempt)) {
            LogUtil.w(AppConfig.TAG, "VpnSession: stale markConnected attempt=$attempt current=${attemptId.get()}")
            return false
        }
        if (!pathVerified) {
            LogUtil.w(AppConfig.TAG, "VpnSession: markConnected refused without path verification")
            return false
        }
        if (sessionStartElapsed.get() == 0L) {
        if (sessionStartElapsed.get() == 0L) {
            sessionStartElapsed.set(elapsedRealtimeMs())
        }
        }
        state.set(VpnSessionState.CONNECTED)
        return true
    }

    fun markProxyOnly(attempt: Long): Boolean {
        if (!isCurrent(attempt)) return false
        state.set(VpnSessionState.PROXY_ONLY)
        return true
    }

    fun markRootRunning(attempt: Long): Boolean {
        if (!isCurrent(attempt)) return false
        state.set(VpnSessionState.ROOT_RUNNING)
        return true
    }

    fun markReconnecting(attempt: Long): Boolean {
        if (!isCurrent(attempt)) return false
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
    }

    fun markError(code: String, message: String) {
        attemptId.incrementAndGet()
        lastPath.set(null)
        lastError.set("$code $message".trim())
        sessionStartElapsed.set(0L)
        traffic.reset()
        state.set(VpnSessionState.ERROR)
    }

    fun duplicateStartDisposition(): DuplicateStartDisposition {
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
        if (state.get() != VpnSessionState.CONNECTED) return false
        if (!lifecycleLock.tryLock()) return false
        if (state.get() != VpnSessionState.CONNECTED) {
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
            val current = state.get()
            if (current == VpnSessionState.DISCONNECTED || current == VpnSessionState.ERROR) {
                return true
            }
            delay(40L)
        }
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
        traffic.reset()
    }

    internal fun elapsedRealtimeMs(): Long =
        runCatching { android.os.SystemClock.elapsedRealtime() }.getOrElse { System.nanoTime() / 1_000_000L }
}
