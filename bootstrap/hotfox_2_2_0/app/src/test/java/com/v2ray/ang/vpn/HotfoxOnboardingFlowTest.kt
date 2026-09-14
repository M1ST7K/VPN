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
    fun welcomeDoesNotRequestVpnForSubscriptionCta() {
        assertEquals(
            HotfoxOnboardingFlow.Step.ACCESS,
            HotfoxOnboardingFlow.afterWelcome(fresh),
        )
        assertEquals(
            HotfoxOnboardingFlow.Step.ACCESS,
            HotfoxOnboardingFlow.advance(
                HotfoxOnboardingFlow.Step.WELCOME,
                HotfoxOnboardingFlow.Event.NEXT,
                fresh,
            ),
        )
        assertEquals(
            HotfoxOnboardingFlow.Step.AUTO,
            HotfoxOnboardingFlow.advance(
                HotfoxOnboardingFlow.Step.WELCOME,
                HotfoxOnboardingFlow.Event.NEXT,
                ready,
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
    fun vpnGrantContinuesToFirstConnection() {
        assertEquals(
            HotfoxOnboardingFlow.Step.FIRST_CONNECTION,
            HotfoxOnboardingFlow.advance(
                HotfoxOnboardingFlow.Step.VPN_PERMISSION,
                HotfoxOnboardingFlow.Event.VPN_GRANTED,
                ready,
            ),
        )
    }

    @Test
    fun accessLeadsToAutoOnlyAfterNext() {
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
    fun autoKeepRequestsVpnWhenMissing() {
        assertEquals(
            HotfoxOnboardingFlow.Step.VPN_PERMISSION,
            HotfoxOnboardingFlow.advance(
                HotfoxOnboardingFlow.Step.AUTO,
                HotfoxOnboardingFlow.Event.KEEP_AUTO,
                fresh,
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
    fun autoBackReturnsToAccessOrWelcome() {
        assertEquals(
            HotfoxOnboardingFlow.Step.ACCESS,
            HotfoxOnboardingFlow.advance(
                HotfoxOnboardingFlow.Step.AUTO,
                HotfoxOnboardingFlow.Event.BACK,
                fresh,
            ),
        )
        assertEquals(
            HotfoxOnboardingFlow.Step.WELCOME,
            HotfoxOnboardingFlow.advance(
                HotfoxOnboardingFlow.Step.AUTO,
                HotfoxOnboardingFlow.Event.BACK,
                ready,
            ),
        )
    }

    @Test
    fun vpnBackReturnsToAutoNotWelcome() {
        assertEquals(
            HotfoxOnboardingFlow.Step.AUTO,
            HotfoxOnboardingFlow.advance(
                HotfoxOnboardingFlow.Step.VPN_PERMISSION,
                HotfoxOnboardingFlow.Event.BACK,
                fresh,
            ),
        )
    }

    @Test
    fun welcomeBackDoesNotCompleteOnboarding() {
        assertEquals(
            HotfoxOnboardingFlow.Step.WELCOME,
            HotfoxOnboardingFlow.advance(
                HotfoxOnboardingFlow.Step.WELCOME,
                HotfoxOnboardingFlow.Event.BACK,
                fresh,
            ),
        )
        assertFalse(
            HotfoxOnboardingFlow.completesOnboarding(
                HotfoxOnboardingFlow.Step.WELCOME,
                HotfoxOnboardingFlow.Event.NEXT,
            ),
        )
        assertFalse(
            HotfoxOnboardingFlow.completesOnboarding(
                HotfoxOnboardingFlow.Step.ACCESS,
                HotfoxOnboardingFlow.Event.NEXT,
            ),
        )
        assertFalse(
            HotfoxOnboardingFlow.completesOnboarding(
                HotfoxOnboardingFlow.Step.AUTO,
                HotfoxOnboardingFlow.Event.KEEP_AUTO,
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
