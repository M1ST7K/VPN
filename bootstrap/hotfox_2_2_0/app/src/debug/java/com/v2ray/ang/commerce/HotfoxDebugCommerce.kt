package com.v2ray.ang.commerce

import com.v2ray.ang.BuildConfig

/**
 * Debug-only sandbox backend selector. Release compiles a stub that never instantiates
 * [SandboxCommerceBackend].
 */
object HotfoxDebugCommerce {
    fun maybeSandbox(): HotfoxCommerceBackend? {
        if (!BuildConfig.HOTFOX_SANDBOX_COMMERCE) return null
        return SandboxCommerceBackend()
    }
}
