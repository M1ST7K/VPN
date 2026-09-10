package com.v2ray.ang.vpn

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicLong

/**
 * Owns MSG_STATE_RESTART work. A later user stop/start must invalidate an
 * in-flight restart so wait-for-idle cannot reconnect against a newer disconnect.
 *
 * Authorization and start dispatch share [intentLock] with [invalidate] so a
 * stop cannot sneak in between a stale `isCurrent` check and startVService.
 */
object VpnRestartGate {
    data class DispatchProbe(
        val authorized: CountDownLatch,
        val resume: CountDownLatch,
    )

    private val generation = AtomicLong(0L)
    private val intentLock = Any()

    @Volatile
    var testProbe: DispatchProbe? = null

    fun current(): Long = synchronized(intentLock) { generation.get() }

    fun nextRequest(): Long = synchronized(intentLock) { generation.incrementAndGet() }

    fun invalidate() {
        synchronized(intentLock) {
            generation.incrementAndGet()
        }
    }

    fun isCurrent(request: Long): Boolean = synchronized(intentLock) {
        request != 0L && request == generation.get()
    }

    /**
     * If [request] is still the live restart intent, run [dispatch] before
     * releasing the intent lock. A concurrent [invalidate] either waits until
     * dispatch returns, or wins and this method returns false without starting.
     *
     * [testProbe] is test-only: it opens a window after the first authorization
     * so a stop can invalidate before the serialized dispatch re-check.
     */
    fun tryDispatchStart(request: Long, dispatch: () -> Unit): Boolean {
        val probe = testProbe
        if (probe != null) {
            synchronized(intentLock) {
                if (!canDispatchLocked(request)) return false
            }
            probe.authorized.countDown()
            try {
                probe.resume.await()
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return false
            }
        }
        synchronized(intentLock) {
            if (!canDispatchLocked(request)) return false
            dispatch()
            return true
        }
    }

    private fun canDispatchLocked(request: Long): Boolean {
        if (request == 0L || request != generation.get()) return false
        if (VpnSessionCoordinator.isTeardownActive()) return false
        return true
    }

    fun resetForTests() {
        synchronized(intentLock) {
            generation.set(0L)
            testProbe = null
        }
    }
}
