package com.v2ray.ang.vpn

/**
 * In-process operational gate for the configured production path.
 *
 * This is not physical-device E2E. A passing result never claims that a third-party
 * app's external IP changed; that proof stays [PHYSICAL_E2E_NOT_EXECUTED] until a
 * real Android device measures it.
 *
 * [tunForwarded] is the TUN-backend progress gate. SOCKS5 and Xray HTTP-204 are
 * component checks and are not sufficient on their own.
 */
data class VpnPathVerification(
    val socks5Ready: Boolean,
    val hevAlive: Boolean?,
    val hevProgressed: Boolean? = null,
    val tunForwarded: Boolean? = null,
    val xrayEgressMs: Long?,
    val tunEstablished: Boolean?,
    val backend: String,
    val verified: Boolean,
    val reason: String? = null,
) {
    val physicalDeviceE2e: String = PHYSICAL_E2E_NOT_EXECUTED

    companion object {
        const val PHYSICAL_E2E_NOT_EXECUTED = "NOT_EXECUTED"
        const val BACKEND_HEV = "hev"
        const val BACKEND_XRAY_TUN = "xray-tun"
        const val BACKEND_PROXY = "proxy-only"
        const val BACKEND_ROOT = "root"
    }
}
