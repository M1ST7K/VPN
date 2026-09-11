package com.v2ray.ang.vpn

import com.v2ray.ang.commerce.CommercePreferences
import com.v2ray.ang.commerce.EntitlementMetadata
import com.v2ray.ang.commerce.EntitlementStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoCommercialEligibilityTest {
    private val now = 1_700_000_000L

    @Test
    fun expiredManagedEntitlementIsNotUsable() {
        val snapshot = AutoCommercialEligibility.from(
            accessOrigin = CommercePreferences.ORIGIN_HOTFOX,
            managedSubscriptionId = "hotfox-sub",
            hasCredential = true,
            metadata = EntitlementMetadata(
                status = EntitlementStatus.EXPIRED,
                startsAtEpochSeconds = now - 200_000,
                expiresAtEpochSeconds = now - 100,
                planId = "plan_1m",
                orderId = "ord-exp",
            ),
            nowEpochSeconds = now,
        )
        assertTrue(snapshot.requiresEntitlement("hotfox-sub"))
        assertFalse(snapshot.entitlementUsable)
        assertFalse(snapshot.requiresEntitlement("https-manual"))
    }

    @Test
    fun externalOriginDoesNotGateManualHttps() {
        val snapshot = AutoCommercialEligibility.from(
            accessOrigin = CommercePreferences.ORIGIN_EXTERNAL,
            managedSubscriptionId = null,
            hasCredential = false,
            metadata = null,
            nowEpochSeconds = now,
        )
        assertFalse(snapshot.requiresEntitlement("any-sub"))
        assertFalse(snapshot.entitlementUsable)
    }

    @Test
    fun activeHotfoxEntitlementIsUsable() {
        val snapshot = AutoCommercialEligibility.from(
            accessOrigin = CommercePreferences.ORIGIN_HOTFOX,
            managedSubscriptionId = "hotfox-sub",
            hasCredential = true,
            metadata = EntitlementMetadata(
                status = EntitlementStatus.ACTIVE,
                startsAtEpochSeconds = now - 10,
                expiresAtEpochSeconds = now + 86_400L,
                planId = "plan_1m",
                orderId = "ord-ok",
            ),
            nowEpochSeconds = now,
        )
        assertTrue(snapshot.entitlementUsable)
        assertTrue(snapshot.requiresEntitlement("hotfox-sub"))
    }
}
