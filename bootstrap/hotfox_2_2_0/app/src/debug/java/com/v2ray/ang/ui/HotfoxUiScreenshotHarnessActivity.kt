package com.v2ray.ang.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.v2ray.ang.vpn.HotfoxOnboardingFlow

/**
 * Debug-only launcher for deterministic UI screenshots.
 * Reuses production layouts. Does not mark VPN CONNECTED, write entitlement,
 * invent servers in production stores, or enable purchase.
 */
class HotfoxUiScreenshotHarnessActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotfoxSystemUi.applyDarkEditorialBars(this)
        val scenario = HotfoxUiScreenshotScenario.fromId(intent.getStringExtra(EXTRA_SCENARIO))
        if (scenario == null) {
            finish()
            return
        }
        HotfoxUiVisualOverride.clear()
        val next = intentFor(scenario)
        next.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(next)
        finish()
    }

    private fun intentFor(scenario: HotfoxUiScreenshotScenario): Intent {
        return when (scenario) {
            HotfoxUiScreenshotScenario.SPLASH -> {
                HotfoxUiVisualOverride.installDebugPresentation(
                    scenarioId = scenario.id,
                    holdSplash = true,
                )
                Intent(this, HotfoxSplashActivity::class.java)
            }
            HotfoxUiScreenshotScenario.ONBOARD_CONNECT -> onboard(HotfoxOnboardingFlow.Step.WELCOME)
            HotfoxUiScreenshotScenario.ONBOARD_AUTO -> onboard(HotfoxOnboardingFlow.Step.AUTO)
            HotfoxUiScreenshotScenario.ONBOARD_READY -> onboard(HotfoxOnboardingFlow.Step.VPN_PERMISSION)
            HotfoxUiScreenshotScenario.DISCONNECTED -> main(
                scenario.id,
                section = MainActivity.SECTION_CONNECTION,
                chrome = HotfoxUiVisualOverride.CHROME_DISCONNECTED,
            )
            HotfoxUiScreenshotScenario.CONNECTING -> main(
                scenario.id,
                section = MainActivity.SECTION_CONNECTION,
                chrome = HotfoxUiVisualOverride.CHROME_CONNECTING,
            )
            HotfoxUiScreenshotScenario.PROTECTED -> main(
                scenario.id,
                section = MainActivity.SECTION_CONNECTION,
                chrome = HotfoxUiVisualOverride.CHROME_CONNECTED,
            )
            HotfoxUiScreenshotScenario.ADD_CONNECTION_SHEET -> main(
                scenario.id,
                section = MainActivity.SECTION_CONNECTION,
                chrome = HotfoxUiVisualOverride.CHROME_DISCONNECTED,
                showSheet = true,
            )
            HotfoxUiScreenshotScenario.HTTPS_SUBSCRIPTION -> {
                HotfoxUiVisualOverride.installDebugPresentation(scenarioId = scenario.id)
                Intent(this, HotfoxHttpsImportActivity::class.java)
            }
            HotfoxUiScreenshotScenario.SERVERS -> main(
                scenario.id,
                section = MainActivity.SECTION_SERVERS,
                serversFixture = true,
            )
            HotfoxUiScreenshotScenario.SERVER_DETAILS -> {
                HotfoxUiVisualOverride.installDebugPresentation(scenarioId = scenario.id)
                Intent(this, HotfoxUiQaServerDetailsActivity::class.java)
            }
            HotfoxUiScreenshotScenario.SUBSCRIPTION -> main(
                scenario.id,
                section = MainActivity.SECTION_SUBSCRIPTION,
                subscriptionFixture = true,
            )
            HotfoxUiScreenshotScenario.SETTINGS -> {
                HotfoxUiVisualOverride.installDebugPresentation(scenarioId = scenario.id)
                Intent(this, HotfoxSettingsActivity::class.java)
            }
            HotfoxUiScreenshotScenario.SMART_ROUTING -> {
                HotfoxUiVisualOverride.installDebugPresentation(scenarioId = scenario.id)
                Intent(this, HotfoxRoutingPrivacyActivity::class.java)
            }
            HotfoxUiScreenshotScenario.APPS_RULES -> {
                HotfoxUiVisualOverride.installDebugPresentation(
                    scenarioId = scenario.id,
                    appsFixture = true,
                    referenceApps = HotfoxUiReferenceFixtures.apps(),
                )
                Intent(this, PerAppProxyActivity::class.java)
            }
            HotfoxUiScreenshotScenario.AUTOPILOT -> {
                HotfoxUiVisualOverride.installDebugPresentation(scenarioId = scenario.id)
                Intent(this, HotfoxAutopilotActivity::class.java)
            }
            HotfoxUiScreenshotScenario.SHADOW -> {
                HotfoxUiVisualOverride.installDebugPresentation(scenarioId = scenario.id)
                Intent(this, HotfoxShadowActivity::class.java)
            }
            HotfoxUiScreenshotScenario.ALWAYS_ON -> {
                HotfoxUiVisualOverride.installDebugPresentation(scenarioId = scenario.id)
                Intent(this, HotfoxAlwaysOnActivity::class.java)
            }
        }
    }

    private fun onboard(step: HotfoxOnboardingFlow.Step): Intent {
        HotfoxUiVisualOverride.installDebugPresentation(
            scenarioId = HotfoxUiScreenshotScenario.fromId(intent.getStringExtra(EXTRA_SCENARIO))?.id,
        )
        return Intent(this, HotfoxOnboardingActivity::class.java)
            .putExtra(HotfoxOnboardingActivity.EXTRA_START_STEP, step.name)
    }

    private fun main(
        scenarioId: String,
        section: String,
        chrome: String? = null,
        showSheet: Boolean = false,
        serversFixture: Boolean = false,
        subscriptionFixture: Boolean = false,
    ): Intent {
        HotfoxUiVisualOverride.installDebugPresentation(
            scenarioId = scenarioId,
            connectionChrome = chrome,
            showAddSheet = showSheet,
            serversFixture = serversFixture,
            subscriptionFixture = subscriptionFixture,
            referenceServers = if (serversFixture) HotfoxUiReferenceFixtures.servers() else emptyList(),
        )
        return Intent(this, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_SKIP_ONBOARDING, true)
            .putExtra(MainActivity.EXTRA_OPEN_SECTION, section)
    }

    companion object {
        const val EXTRA_SCENARIO = "scenario"
    }
}
