package com.v2ray.ang.vpn

/**
 * Race-safe [android.net.VpnService.protect] broker.
 *
 * The pinned AndroidLibXrayLite `CoreCallbackHandler` has no protect(fd)
 * method, so Go/Xray sockets are kept off TUN by [HotfoxTunSelfExclusion].
 * This broker still records every protect() the VpnService can perform
 * (probe sockets, future native hooks) and is fail-closed on stale service,
 * false, or exception.
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

object HotfoxSocketProtect {
    @Volatile
    private var generation: Long = 0

    @Volatile
    private var protector: HotfoxFdProtector? = null

    fun resetForTests() {
        generation = 0
        protector = null
        VpnProtectEvidence.resetForTests()
    }

    fun attach(attempt: Long, protector: HotfoxFdProtector) {
        generation = attempt
        this.protector = protector
    }

    fun detach(attempt: Long) {
        if (generation == attempt) {
            protector = null
        }
    }

    fun protect(fd: Int, protocol: String, attempt: Long = generation): HotfoxProtectResult {
        if (fd < 0) {
            val result = HotfoxProtectResult(false, "invalid-fd", protocol)
            VpnProtectEvidence.record(false)
            return result
        }
        if (attempt != generation) {
            val result = HotfoxProtectResult(false, "stale-generation", protocol)
            VpnProtectEvidence.record(false)
            return result
        }
        val current = protector
        if (current == null) {
            val result = HotfoxProtectResult(false, "stale-service", protocol)
            VpnProtectEvidence.record(false)
            return result
        }
        return try {
            val ok = current.protectFd(fd)
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
