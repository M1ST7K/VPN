package com.v2ray.ang.vpn

/**
 * Runtime evidence that [android.net.VpnService.protect] and/or
 * [android.net.ConnectivityManager.bindProcessToNetwork] ran.
 * Presence of those calls in source is not 2.9 proof.
 *
 * Transport labels are WIFI/CELLULAR/ETHERNET/OTHER/none — never SSID/BSSID.
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

    @Volatile
    var bindAttempted: Boolean = false
        private set

    @Volatile
    var bindSuccess: Boolean = false
        private set

    @Volatile
    var lastUnderlyingTransport: String = "none"
        private set

    fun resetForTests() {
        callCount = 0
        successCount = 0
        lastSuccess = null
        bindAttempted = false
        bindSuccess = false
        lastUnderlyingTransport = "none"
    }

    fun record(success: Boolean) {
        callCount += 1
        if (success) successCount += 1
        lastSuccess = success
    }

    fun recordBind(success: Boolean, transport: String?) {
        bindAttempted = true
        bindSuccess = success
        lastUnderlyingTransport = transport?.takeIf { it.isNotBlank() } ?: "none"
    }

    fun summary(): String =
        "protectCalled=$callCount protectOk=$successCount lastProtect=${lastSuccess ?: "none"} " +
            "bindAttempted=$bindAttempted bindOk=$bindSuccess underlyingTransport=$lastUnderlyingTransport"
}
