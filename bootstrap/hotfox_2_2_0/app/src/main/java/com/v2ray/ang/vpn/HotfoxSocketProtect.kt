package com.v2ray.ang.vpn

import java.util.concurrent.atomic.AtomicReference

/**
 * Race-safe [android.net.VpnService.protect] broker.
 *
 * The pinned AndroidLibXrayLite `CoreCallbackHandler` has no protect(fd)
 * method, so Go/Xray sockets are kept off TUN by [HotfoxTunSelfExclusion].
 * This broker still records every protect() the VpnService can perform
 * (probe sockets, future native hooks) and is fail-closed on stale service,
 * false, or exception.
 *
 * Ownership is an immutable `(attempt, protector)` snapshot. Attach/detach
 * publish or CAS-clear that pair atomically so an older teardown cannot
 * drop a newer service callback, and [protect] never mixes generations.
 */
data class HotfoxProtectResult(
    val success: Boolean,
    val reason: String,
    val protocol: String,
) {
    fun diagnostic(): String = "HF-VPN-015 protect $protocol $reason"
}

fun interface HotfoxFdProtector {
    fun protectFd(fd: Int): Boolean
}

internal data class HotfoxProtectBinding(
    val attempt: Long,
    val protector: HotfoxFdProtector,
)

object HotfoxSocketProtect {
    private val binding = AtomicReference<HotfoxProtectBinding?>(null)

    fun resetForTests() {
        binding.set(null)
        VpnProtectEvidence.resetForTests()
    }

    fun attach(attempt: Long, protector: HotfoxFdProtector) {
        binding.set(HotfoxProtectBinding(attempt, protector))
    }

    fun detach(attempt: Long) {
        while (true) {
            val current = binding.get() ?: return
            if (current.attempt != attempt) return
            if (binding.compareAndSet(current, null)) return
        }
    }

    fun boundAttemptForTests(): Long = binding.get()?.attempt ?: 0L

    fun protect(fd: Int, protocol: String, attempt: Long? = null): HotfoxProtectResult {
        if (fd < 0) {
            val result = HotfoxProtectResult(false, "invalid-fd", protocol)
            VpnProtectEvidence.record(false)
            return result
        }
        val snapshot = binding.get()
        if (snapshot == null) {
            val result = HotfoxProtectResult(false, "stale-service", protocol)
            VpnProtectEvidence.record(false)
            return result
        }
        val expected = attempt ?: snapshot.attempt
        if (expected != snapshot.attempt) {
            val result = HotfoxProtectResult(false, "stale-generation", protocol)
            VpnProtectEvidence.record(false)
            return result
        }
        return try {
            val ok = snapshot.protector.protectFd(fd)
            val result = HotfoxProtectResult(ok, if (ok) "ok" else "protect-false", protocol)
            VpnProtectEvidence.record(ok)
            result
        } catch (error: RuntimeException) {
            val result = HotfoxProtectResult(false, "protect-exception:${error.javaClass.simpleName}", protocol)
            VpnProtectEvidence.record(false)
            result
        }
    }

    fun requireSuccess(result: HotfoxProtectResult): Boolean {
        if (result.success) return true
        return false
    }
}
