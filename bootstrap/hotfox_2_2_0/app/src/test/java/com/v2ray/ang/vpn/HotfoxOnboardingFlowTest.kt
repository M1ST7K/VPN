package com.v2ray.ang.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HotfoxOnboardingFlowTest {
    private val fresh = HotfoxOnboardingFlow.Facts(vpnPermissionGranted = false, hasAccess = false)
    private val permitted = HotfoxOnboardingFlow.Facts(vpnPermissionGranted = true, hasAccess = false)
    private val ready = HotfoxOnboardingFlow.Facts(vpnPermissionGranted = true, hasAccess = true)

    @Test
    fun welcomeGoesToVpnWhenPermissionMissing() {
        assertEquals(
            HotfoxOnboardingFlow.Step.VPN_PERMISSION,
            HotfoxOnboardingFlow.afterWelcome(fresh),
        )
        assertEquals(
            HotfoxOnboardingFlow.Step.VPN_PERMISSION,
            HotfoxOnboardingFlow.advance(
                HotfoxOnboardingFlow.Step.WELCOME,
                HotfoxOnboardingFlow.Event.NEXT,
                fresh,
            ),
        )
    }

    @Test
    fun vpnDenialStaysOnPermissionStep() {
        val denied = HotfoxOnboardingFlow.advance(
            HotfoxOnboardingFlow.Step.VPN_PERMISSION,
            HotfoxOnboardingFlow.Event.VPN_DENIED,
            fresh,
        )
        assertEquals(HotfoxOnboardingFlow.Step.VPN_PERMISSION, denied)
    }

    @Test
    fun vpnGrantSkipsAccessWhenUserAlreadyHasServers() {
        assertEquals(
            HotfoxOnboardingFlow.Step.AUTO,
            HotfoxOnboardingFlow.advance(
                HotfoxOnboardingFlow.Step.VPN_PERMISSION,
                HotfoxOnboardingFlow.Event.VPN_GRANTED,
                ready,
            ),
        )
    }

    @Test
    fun accessIsOptionalAndLeadsToAuto() {
        assertEquals(
            HotfoxOnboardingFlow.Step.AUTO,
            HotfoxOnboardingFlow.advance(
                HotfoxOnboardingFlow.Step.ACCESS,
                HotfoxOnboardingFlow.Event.NEXT,
                permitted,
            ),
        )
    }

    @Test
    fun autoKeepAndManualBothReachFirstConnection() {
        assertEquals(
            HotfoxOnboardingFlow.Step.FIRST_CONNECTION,
            HotfoxOnboardingFlow.advance(
                HotfoxOnboardingFlow.Step.AUTO,
                HotfoxOnboardingFlow.Event.KEEP_AUTO,
                ready,
            ),
        )
        assertEquals(
            HotfoxOnboardingFlow.Step.FIRST_CONNECTION,
            HotfoxOnboardingFlow.advance(
                HotfoxOnboardingFlow.Step.AUTO,
                HotfoxOnboardingFlow.Event.MANUAL_SERVERS,
                ready,
            ),
        )
    }

    @Test
    fun finishOnlyCompletesOnLastStep() {
        assertFalse(
            HotfoxOnboardingFlow.completesOnboarding(
                HotfoxOnboardingFlow.Step.WELCOME,
                HotfoxOnboardingFlow.Event.FINISH,
            ),
        )
        assertTrue(
            HotfoxOnboardingFlow.completesOnboarding(
                HotfoxOnboardingFlow.Step.FIRST_CONNECTION,
                HotfoxOnboardingFlow.Event.FINISH,
            ),
        )
        assertEquals("hotfox_onboarding_vpn_retry", HotfoxOnboardingFlow.primaryResName(HotfoxOnboardingFlow.Step.VPN_PERMISSION))
        assertEquals("hotfox_onboarding_connect_title", HotfoxOnboardingFlow.titleResName(HotfoxOnboardingFlow.Step.FIRST_CONNECTION))
    }
}
