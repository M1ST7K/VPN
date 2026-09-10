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
    var tunHttp4: Boolean? = null
        private set

    @Volatile
    var tunHttp6: Boolean? = null
        private set

    @Volatile
    var tunDns: Boolean? = null
        private set

    fun resetForTests() {
        tunHttp = null
        tunHttp4 = null
        tunHttp6 = null
        tunDns = null
    }

    fun record(http: Boolean?, dns: Boolean?, http4: Boolean? = null, http6: Boolean? = null) {
        tunHttp = http
        tunDns = dns
        tunHttp4 = http4
        tunHttp6 = http6
    }

    fun injectSucceeded(http: Boolean?, dns: Boolean?): Boolean = http == true || dns == true

    fun summary(): String =
        "tunHttp=${tunHttp ?: "none"} tunHttp4=${tunHttp4 ?: "none"} tunHttp6=${tunHttp6 ?: "none"} tunDns=${tunDns ?: "none"}"
}
