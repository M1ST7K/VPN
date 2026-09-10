package com.v2ray.ang.commerce

import com.v2ray.ang.BuildConfig

object HotfoxCommerceFactory {
    fun configuredBackendUrl(): String = BuildConfig.PAYMENT_BACKEND_URL.trim()

    fun sandboxCommerceEnabled(): Boolean = BuildConfig.DEBUG && BuildConfig.HOTFOX_SANDBOX_COMMERCE

    fun create(): HotfoxCommerceBackend {
        if (BuildConfig.DEBUG) {
            HotfoxDebugCommerce.maybeSandbox()?.let { return it }
        }
        val url = configuredBackendUrl()
        if (!url.startsWith("https://", ignoreCase = true)) {
            return UnavailableCommerceBackend
        }
        return HttpHotfoxCommerceBackend(url)
    }
}
