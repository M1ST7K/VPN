package com.v2ray.ang.vpn

/**
 * Xray-core capability must not depend on branding [applicationId].
 * HotFox ships libv2ray/Xray as `com.hotfox.vpn`; upstream v2rayNG uses
 * `com.v2ray.ang`. Core config/inbound branches must treat both as Xray.
 */
object HotfoxXrayCapability {
    const val HOTFOX_APPLICATION_ID = "com.hotfox.vpn"
    const val V2RAYNG_APPLICATION_ID_PREFIX = "com.v2ray.ang"

    fun isXrayCore(applicationId: String): Boolean {
        val id = applicationId.trim()
        if (id.isEmpty()) return true
        if (id == HOTFOX_APPLICATION_ID || id.startsWith("com.hotfox.")) return true
        if (id.startsWith(V2RAYNG_APPLICATION_ID_PREFIX)) return true
        // This product always embeds Xray (pinned libv2ray.aar). Unknown branding
        // must not silently fall back to a non-Xray config path.
        return true
    }
}
