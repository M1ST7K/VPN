package com.v2ray.ang.vpn

import com.v2ray.ang.handler.MmkvManager

/**
 * First-run flow around real requirements only:
 * welcome/access (subscription) → AUTO/manual picker → VPN permission → first connection.
 * The subscription CTA must not request VPN permission. Unrelated permissions
 * are never requested for visual completion.
 */
object HotfoxOnboardingFlow {
    enum class Step {
        WELCOME,
        VPN_PERMISSION,
        ACCESS,
        AUTO,
        FIRST_CONNECTION,
    }

    enum class Event {
        NEXT,
        BACK,
        VPN_GRANTED,
        VPN_DENIED,
        KEEP_AUTO,
        MANUAL_SERVERS,
        FINISH,
    }

    data class Facts(
        val vpnPermissionGranted: Boolean,
        val hasAccess: Boolean,
    )

    fun firstStep(): Step = Step.WELCOME

    fun afterWelcome(facts: Facts): Step = when {
        facts.hasAccess -> Step.AUTO
        else -> Step.ACCESS
    }

    fun afterAuto(facts: Facts): Step =
        if (facts.vpnPermissionGranted) Step.FIRST_CONNECTION else Step.VPN_PERMISSION

    fun afterVpnResolved(facts: Facts): Step = Step.FIRST_CONNECTION

    fun advance(current: Step, event: Event, facts: Facts): Step = when (current) {
        Step.WELCOME -> if (event == Event.NEXT) {
            afterWelcome(facts)
        } else {
            current
        }
        Step.VPN_PERMISSION -> when (event) {
            Event.VPN_GRANTED, Event.NEXT -> afterVpnResolved(facts.copy(vpnPermissionGranted = true))
            Event.VPN_DENIED -> Step.VPN_PERMISSION
            Event.BACK -> Step.AUTO
            else -> current
        }
        Step.ACCESS -> when (event) {
            Event.NEXT -> Step.AUTO
            Event.BACK -> Step.WELCOME
            else -> current
        }
        Step.AUTO -> when (event) {
            Event.KEEP_AUTO, Event.MANUAL_SERVERS, Event.NEXT -> afterAuto(facts)
            Event.BACK -> if (facts.hasAccess) Step.WELCOME else Step.ACCESS
            else -> current
        }
        Step.FIRST_CONNECTION -> when (event) {
            Event.BACK -> Step.AUTO
            Event.FINISH, Event.NEXT -> Step.FIRST_CONNECTION
            else -> current
        }
    }

    fun completesOnboarding(current: Step, event: Event): Boolean =
        current == Step.FIRST_CONNECTION && (event == Event.FINISH || event == Event.NEXT)

    fun primaryResName(step: Step): String = when (step) {
        Step.WELCOME -> "hotfox_onboarding_continue"
        Step.VPN_PERMISSION -> "hotfox_onboarding_vpn_retry"
        Step.ACCESS -> "hotfox_onboarding_skip_access"
        Step.AUTO -> "hotfox_onboarding_auto_keep"
        Step.FIRST_CONNECTION -> "hotfox_onboarding_done"
    }

    fun titleResName(step: Step): String = when (step) {
        Step.WELCOME -> "hotfox_onboarding_welcome_title"
        Step.VPN_PERMISSION -> "hotfox_onboarding_vpn_title"
        Step.ACCESS -> "hotfox_onboarding_access_title"
        Step.AUTO -> "hotfox_onboarding_auto_title"
        Step.FIRST_CONNECTION -> "hotfox_onboarding_connect_title"
    }

    fun bodyResName(step: Step): String = when (step) {
        Step.WELCOME -> "hotfox_onboarding_welcome_body"
        Step.VPN_PERMISSION -> "hotfox_onboarding_vpn_body"
        Step.ACCESS -> "hotfox_onboarding_access_body"
        Step.AUTO -> "hotfox_onboarding_auto_body"
        Step.FIRST_CONNECTION -> "hotfox_onboarding_connect_body"
    }
}

object HotfoxOnboardingStore {
    const val PREF_COMPLETE = "pref_hotfox_onboarding_complete"

    fun isComplete(): Boolean = runCatching {
        MmkvManager.decodeSettingsBool(PREF_COMPLETE, false)
    }.getOrDefault(false)

    fun shouldPrompt(): Boolean = !isComplete()

    fun markComplete() {
        runCatching { MmkvManager.encodeSettings(PREF_COMPLETE, true) }
    }

    fun resetForTests() {
        runCatching { MmkvManager.encodeSettings(PREF_COMPLETE, false) }
    }
}
