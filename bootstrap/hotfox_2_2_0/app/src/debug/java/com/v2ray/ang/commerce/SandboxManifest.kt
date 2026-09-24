package com.v2ray.ang.commerce

/**
 * Sandbox/CI inventory helpers. Debug/test only.
 */
object SandboxManifest {
    const val FORMAT = ManagedManifestParser.FORMAT
    const val SHARE_FORMAT = "hotfox-managed-share-v1"

    fun encode(servers: List<ManifestRefreshPolicy.ServerIdentity>): CommerceManifest =
        ManagedManifestParser.encode(servers)

    fun parse(manifest: CommerceManifest) = ManagedManifestParser.parse(manifest)

    fun shareLinkBody(): String = listOf(
        "socks://ams.sandbox.hotfox.invalid:1080#Amsterdam",
        "socks://fra.sandbox.hotfox.invalid:1080#Frankfurt",
    ).joinToString("\n")

    fun encodeShareLinks(): CommerceManifest = CommerceManifest(
        format = SHARE_FORMAT,
        payload = shareLinkBody(),
    )

    fun sandboxInventory(): List<ManifestRefreshPolicy.ServerIdentity> = listOf(
        ManifestRefreshPolicy.ServerIdentity(
            remarks = "Amsterdam",
            server = "ams.sandbox.hotfox.invalid",
            port = "443",
            protocol = "VLESS",
            network = "xhttp",
            security = "reality",
            fingerprint = "chrome",
        ),
        ManifestRefreshPolicy.ServerIdentity(
            remarks = "Frankfurt",
            server = "fra.sandbox.hotfox.invalid",
            port = "443",
            protocol = "VLESS",
            network = "xhttp",
            security = "reality",
            fingerprint = "chrome",
        ),
    )
}
