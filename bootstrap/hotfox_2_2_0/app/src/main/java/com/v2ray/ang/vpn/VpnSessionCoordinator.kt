package com.v2ray.ang.vpn

import android.os.SystemClock
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Process-scoped session coordinator. Survives Activity recreation while the
 * VPN process remains alive. CONNECTED is never inferred from a Boolean flag.
 */
object VpnSessionCoordinator {
    private val attemptId = AtomicLong(0L)
    private val state = AtomicReference(VpnSessionState.DISCONNECTED)
    private val sessionStartElapsed = AtomicLong(0L)
    private val lastError = AtomicReference<String?>(null)
    val traffic = HotfoxTrafficAccumulator()

    fun beginAttempt(): Long {
        val id = attemptId.incrementAndGet()
        state.set(VpnSessionState.PREPARING)
        lastError.set(null)
        return id
    }

    fun isCurrent(id: Long): Boolean = id != 0L && id == attemptId.get()

    fun currentAttempt(): Long = attemptId.get()

    fun currentState(): VpnSessionState = state.get()

    fun setState(next: VpnSessionState) {
        if (next == VpnSessionState.CONNECTED) {
            if (sessionStartElapsed.get() == 0L) {
                sessionStartElapsed.set(SystemClock.elapsedRealtime())
            }
        }
        if (next == VpnSessionState.DISCONNECTED || next == VpnSessionState.ERROR) {
            if (next == VpnSessionState.DISCONNECTED) {
                sessionStartElapsed.set(0L)
                traffic.reset()
            }
        }
        state.set(next)
    }

    fun markConnected() {
        if (sessionStartElapsed.get() == 0L) {
            sessionStartElapsed.set(SystemClock.elapsedRealtime())
        }
        state.set(VpnSessionState.CONNECTED)
    }

    fun markReconnecting() {
        state.set(VpnSessionState.RECONNECTING)
    }

    fun markDisconnected() {
        attemptId.incrementAndGet()
        state.set(VpnSessionState.DISCONNECTED)
        sessionStartElapsed.set(0L)
        traffic.reset()
        lastError.set(null)
    }

    fun markError(code: String, message: String) {
        attemptId.incrementAndGet()
        lastError.set("$code $message".trim())
        sessionStartElapsed.set(0L)
        traffic.reset()
        state.set(VpnSessionState.ERROR)
    }

    fun sessionStartedAtElapsed(): Long? {
        val started = sessionStartElapsed.get()
        return started.takeIf { it > 0L }
    }

    fun lastError(): String? = lastError.get()
}
