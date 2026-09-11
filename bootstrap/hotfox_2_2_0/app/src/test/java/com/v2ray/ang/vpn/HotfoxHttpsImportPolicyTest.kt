package com.v2ray.ang.vpn

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HotfoxHttpsImportPolicyTest {
    @Test
    fun onlyHttpsUrlsAreAccepted() {
        assertTrue(HotfoxHttpsImportPolicy.isHttpsSubscriptionUrl("https://provider.example/sub"))
        assertFalse(HotfoxHttpsImportPolicy.isHttpsSubscriptionUrl("http://provider.example/sub"))
        assertFalse(HotfoxHttpsImportPolicy.isHttpsSubscriptionUrl("https://"))
        assertFalse(HotfoxHttpsImportPolicy.isHttpsSubscriptionUrl("  "))
        assertFalse(HotfoxHttpsImportPolicy.isHttpsSubscriptionUrl(null))
    }
}
