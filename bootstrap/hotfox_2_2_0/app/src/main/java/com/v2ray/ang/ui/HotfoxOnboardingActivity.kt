package com.v2ray.ang.ui

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.v2ray.ang.R
import com.v2ray.ang.commerce.CommercePreferences
import com.v2ray.ang.databinding.ActivityHotfoxOnboardingBinding
import com.v2ray.ang.vpn.HotfoxOnboardingFlow
import com.v2ray.ang.vpn.HotfoxOnboardingStore
import com.v2ray.ang.vpn.HotfoxServerSelection

class HotfoxOnboardingActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_START_STEP = "hotfox_onboarding_start_step"
    }

    private lateinit var binding: ActivityHotfoxOnboardingBinding
    private var step = HotfoxOnboardingFlow.firstStep()

    private val requestVpnPermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            step = HotfoxOnboardingFlow.advance(
                step,
                HotfoxOnboardingFlow.Event.VPN_GRANTED,
                facts(),
            )
        } else {
            step = HotfoxOnboardingFlow.advance(
                step,
                HotfoxOnboardingFlow.Event.VPN_DENIED,
                facts(),
            )
        }
        render()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotfoxSystemUi.applyDarkEditorialBars(this)
        val startStep = intent.getStringExtra(EXTRA_START_STEP)
        if (startStep.isNullOrBlank() && HotfoxOnboardingStore.isComplete()) {
            finishToMain()
            return
        }
        binding = ActivityHotfoxOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        HotfoxSystemUi.hideScrollbars(binding.root)
        binding.btnOnboardingPrimary.setOnClickListener { onPrimary() }
        binding.btnOnboardingSecondary.setOnClickListener { onSecondary() }
        if (!startStep.isNullOrBlank()) {
            step = runCatching { HotfoxOnboardingFlow.Step.valueOf(startStep) }
                .getOrDefault(HotfoxOnboardingFlow.Step.WELCOME)
        }
        render()
    }

    private fun facts(): HotfoxOnboardingFlow.Facts {
        val vpnGranted = VpnService.prepare(this) == null
        val hasAccess = CommercePreferences.accessOrigin() != CommercePreferences.ORIGIN_NONE ||
            HotfoxServerSelection.firstUsableGuid() != null
        return HotfoxOnboardingFlow.Facts(
            vpnPermissionGranted = vpnGranted,
            hasAccess = hasAccess,
        )
    }

    private fun render() {
        when (step) {
            HotfoxOnboardingFlow.Step.WELCOME,
            HotfoxOnboardingFlow.Step.ACCESS,
            -> {
                binding.tvOnboardingTitle.setText(R.string.hotfox_onboarding_connect_title_ui)
                binding.tvOnboardingBody.setText(R.string.hotfox_onboarding_connect_body_ui)
                binding.imgOnboardingPlanet.isVisible = true
                binding.imgOnboardingArt.setImageResource(R.drawable.hf_fox_bust_transparent)
                binding.imgOnboardingOverlay.isVisible = false
                binding.imgOnboardingOverlay2.isVisible = false
                binding.layoutOnboardingNote.isVisible = false
                binding.btnOnboardingPrimary.setText(R.string.hotfox_already_have_subscription)
                binding.btnOnboardingPrimary.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.hf_arrow_right_ink, 0)
                binding.btnOnboardingSecondary.isVisible = true
                binding.btnOnboardingSecondary.setText(R.string.hotfox_onboarding_buy_access)
            }
            HotfoxOnboardingFlow.Step.AUTO -> {
                binding.tvOnboardingTitle.setText(R.string.hotfox_onboarding_auto_title_ui)
                binding.tvOnboardingBody.setText(R.string.hotfox_onboarding_auto_body_ui)
                binding.imgOnboardingPlanet.isVisible = false
                binding.imgOnboardingArt.setImageResource(R.drawable.hf_native_auto_routing)
                binding.imgOnboardingOverlay.isVisible = false
                binding.imgOnboardingOverlay2.isVisible = false
                binding.layoutOnboardingNote.isVisible = false
                binding.btnOnboardingPrimary.setText(R.string.hotfox_onboarding_use_auto)
                binding.btnOnboardingSecondary.isVisible = true
                binding.btnOnboardingSecondary.setText(R.string.hotfox_onboarding_choose_manual)
            }
            HotfoxOnboardingFlow.Step.VPN_PERMISSION,
            HotfoxOnboardingFlow.Step.FIRST_CONNECTION,
            -> {
                binding.tvOnboardingTitle.setText(R.string.hotfox_onboarding_ready_title_ui)
                binding.tvOnboardingBody.setText(R.string.hotfox_onboarding_ready_body_ui)
                binding.imgOnboardingPlanet.isVisible = false
                binding.imgOnboardingArt.setImageResource(R.drawable.hf_native_ready_complete)
                binding.imgOnboardingOverlay.isVisible = false
                binding.imgOnboardingOverlay2.isVisible = false
                binding.layoutOnboardingNote.isVisible = true
                binding.btnOnboardingPrimary.setText(R.string.hotfox_onboarding_continue)
                binding.btnOnboardingSecondary.isVisible = false
            }
        }
        binding.btnOnboardingPrimary.contentDescription = binding.btnOnboardingPrimary.text
    }

    private fun onPrimary() {
        when (step) {
            HotfoxOnboardingFlow.Step.VPN_PERMISSION -> requestVpn()
            HotfoxOnboardingFlow.Step.AUTO -> {
                HotfoxServerSelection.setAutoMode(true)
                go(HotfoxOnboardingFlow.Event.KEEP_AUTO)
            }
            HotfoxOnboardingFlow.Step.ACCESS -> {
                startActivity(Intent(this, HotfoxHttpsImportActivity::class.java))
                go(HotfoxOnboardingFlow.Event.NEXT)
            }
            HotfoxOnboardingFlow.Step.WELCOME -> go(HotfoxOnboardingFlow.Event.NEXT)
            HotfoxOnboardingFlow.Step.FIRST_CONNECTION -> {
                if (HotfoxOnboardingFlow.completesOnboarding(step, HotfoxOnboardingFlow.Event.FINISH)) {
                    HotfoxOnboardingStore.markComplete()
                    finishToMain()
                }
            }
        }
    }

    private fun onSecondary() {
        when (step) {
            HotfoxOnboardingFlow.Step.AUTO -> {
                HotfoxServerSelection.setAutoMode(false)
                go(HotfoxOnboardingFlow.Event.MANUAL_SERVERS)
            }
            HotfoxOnboardingFlow.Step.WELCOME,
            HotfoxOnboardingFlow.Step.ACCESS,
            -> startActivity(Intent(this, RenewalActivity::class.java))
            else -> Unit
        }
    }

    private fun go(event: HotfoxOnboardingFlow.Event) {
        if (HotfoxOnboardingFlow.completesOnboarding(step, event)) {
            HotfoxOnboardingStore.markComplete()
            finishToMain()
            return
        }
        step = HotfoxOnboardingFlow.advance(step, event, facts())
        render()
    }

    private fun requestVpn() {
        val intent = VpnService.prepare(this)
        if (intent == null) {
            go(HotfoxOnboardingFlow.Event.VPN_GRANTED)
        } else {
            requestVpnPermission.launch(intent)
        }
    }

    private fun finishToMain() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_SKIP_ONBOARDING, true)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        finish()
    }
}
