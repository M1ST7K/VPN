package com.v2ray.ang.vpn

import java.util.concurrent.atomic.AtomicLong

/**
 * Generation-scoped routing reconfiguration.
 *
 * Rapid mode/rule changes bump the generation; only the latest generation may
 * apply. A stale in-flight restart must not overwrite a newer policy.
 */
object HotfoxRoutingApply {
    private val generation = AtomicLong(0L)
    private val applied = AtomicLong(-1L)

    fun bump(): Long = generation.incrementAndGet()

    fun current(): Long = generation.get()

    fun appliedGeneration(): Long = applied.get()

    /**
     * Returns true when [candidate] is still the latest generation and has not
     * been applied yet. Concurrent callers for the same generation serialize:
     * only one applies.
     */
    fun tryApply(candidate: Long): Boolean {
        if (candidate <= 0L) return false
        while (true) {
            val latest = generation.get()
            if (candidate != latest) return false
            val currentApplied = applied.get()
            if (currentApplied >= candidate) return false
            if (applied.compareAndSet(currentApplied, candidate)) return true
        }
    }

    fun resetForTests() {
        generation.set(0L)
        applied.set(-1L)
    }
}
