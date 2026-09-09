package com.v2ray.ang.vpn

/**
 * TUN inject layers must be judged independently (spec 18).
 * UDP DNS FAIL with TCP HTTPS PASS is not an Xray-outbound failure.
 */
object HotfoxTunLayerEvidence {
    @Volatile
    var tunHttp: Boolean? = null
        private set

    @Volatile
    var tunDns: Boolean? = null
        private set

    fun resetForTests() {
        tunHttp = null
        tunDns = null
    }

    fun record(http: Boolean?, dns: Boolean?) {
        tunHttp = http
        tunDns = dns
    }

    fun injectSucceeded(http: Boolean?, dns: Boolean?): Boolean = http == true || dns == true

    fun summary(): String =
        "tunHttp=${tunHttp ?: "none"} tunDns=${tunDns ?: "none"}"
}
