package com.v2ray.ang.commerce

/**
 * Release builds cannot instantiate the in-process sandbox commerce fixture.
 */
object HotfoxDebugCommerce {
    fun maybeSandbox(): HotfoxCommerceBackend? = null
}
