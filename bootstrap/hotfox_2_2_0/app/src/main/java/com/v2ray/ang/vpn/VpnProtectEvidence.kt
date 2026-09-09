package com.v2ray.ang.vpn

/**
 * Runtime evidence that [android.net.VpnService.protect] was invoked.
 * Presence of protect() in source is not 2.9 proof.
 */
object VpnProtectEvidence {
    @Volatile
    var callCount: Int = 0
        private set

    @Volatile
    var successCount: Int = 0
        private set

    @Volatile
    var lastSuccess: Boolean? = null
        private set

    fun resetForTests() {
        callCount = 0
        successCount = 0
        lastSuccess = null
    }

    fun record(success: Boolean) {
        callCount += 1
        if (success) successCount += 1
        lastSuccess = success
    }

    fun summary(): String =
        "protectCalled=$callCount protectOk=$successCount lastProtect=${lastSuccess ?: "none"}"
}
