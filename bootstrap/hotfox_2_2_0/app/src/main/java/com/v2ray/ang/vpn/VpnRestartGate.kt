package com.v2ray.ang.vpn

import java.util.concurrent.atomic.AtomicLong

/**
 * Owns MSG_STATE_RESTART work. A later user stop/start must invalidate an
 * in-flight restart so a detached wait-for-idle cannot reconnect against a
 * newer disconnect intent.
 */
object VpnRestartGate {
    private val generation = AtomicLong(0L)

    fun current(): Long = generation.get()

    fun nextRequest(): Long = generation.incrementAndGet()

    fun invalidate() {
        generation.incrementAndGet()
    }

    fun isCurrent(request: Long): Boolean = request != 0L && request == generation.get()

    fun resetForTests() {
        generation.set(0L)
    }
}
