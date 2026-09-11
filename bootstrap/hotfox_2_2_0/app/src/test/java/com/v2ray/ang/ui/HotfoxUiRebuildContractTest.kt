package com.v2ray.ang.ui

import com.v2ray.ang.vpn.HotfoxOnboardingFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HotfoxUiRebuildContractTest {
    @Test
    fun eighteenLockedScreensAreCatalogued() {
        assertEquals(18, HotfoxLockedScreens.IDS.size)
        assertEquals("01_splash_brand_entry", HotfoxLockedScreens.IDS.first())
        assertEquals("18_always_on_kill_switch", HotfoxLockedScreens.IDS.last())
        assertEquals(18, HotfoxLockedScreens.IDS.toSet().size)
    }

    @Test
    fun onboardingCopyResourceNamesStayStable() {
        assertEquals("hotfox_onboarding_welcome_title", HotfoxOnboardingFlow.titleResName(HotfoxOnboardingFlow.Step.WELCOME))
        assertEquals("hotfox_onboarding_auto_title", HotfoxOnboardingFlow.titleResName(HotfoxOnboardingFlow.Step.AUTO))
        assertEquals("hotfox_onboarding_vpn_title", HotfoxOnboardingFlow.titleResName(HotfoxOnboardingFlow.Step.VPN_PERMISSION))
        assertEquals("hotfox_onboarding_connect_title", HotfoxOnboardingFlow.titleResName(HotfoxOnboardingFlow.Step.FIRST_CONNECTION))
        assertEquals("hotfox_onboarding_skip_access", HotfoxOnboardingFlow.primaryResName(HotfoxOnboardingFlow.Step.ACCESS))
    }

    @Test
    fun httpsImportRejectsNonHttps() {
        assertTrue(com.v2ray.ang.vpn.HotfoxHttpsImportPolicy.isHttpsSubscriptionUrl("https://provider.example/sub"))
        assertFalse(com.v2ray.ang.vpn.HotfoxHttpsImportPolicy.isHttpsSubscriptionUrl("http://provider.example/sub"))
    }

    @Test
    fun addConnectionActionsCoverRequiredImportPaths() {
        val actions = HotfoxAddConnectionSheet.Action.entries.map { it.name }.toSet()
        assertTrue(actions.containsAll(setOf("HTTPS", "CLIPBOARD", "QR", "FILE", "ALWAYS_ON")))
        assertFalse(actions.contains("MOCK"))
    }
}
