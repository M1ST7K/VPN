package com.v2ray.ang.ui

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.OnBackPressedCallback
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
        private const val STATE_STEP = "hotfox_onboarding_step"
        private const val CLICK_GUARD_MS = 700L
    }

    private lateinit var binding: ActivityHotfoxOnboardingBinding
    private var step = HotfoxOnboardingFlow.firstStep()
    private var lastClickElapsed = 0L
    private var awaitingImport = false

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

    private val requestImport = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        awaitingImport = false
        if (facts().hasAccess) {
            go(HotfoxOnboardingFlow.Event.NEXT)
        }
    }

    private val requestServerPick = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            go(HotfoxOnboardingFlow.Event.MANUAL_SERVERS)
        }
    }

    private val requestPurchase = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        // Browser/activity return is not payment proof. Stay on 02 unless access exists.
        if (step == HotfoxOnboardingFlow.Step.WELCOME || step == HotfoxOnboardingFlow.Step.ACCESS) {
            if (facts().hasAccess) {
                go(HotfoxOnboardingFlow.Event.NEXT)
            } else {
                render()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotfoxSystemUi.applyDarkEditorialBars(this)
        val startStep = intent.getStringExtra(EXTRA_START_STEP)
        if (startStep.isNullOrBlank() && savedInstanceState == null && HotfoxOnboardingStore.isComplete()) {
            finishToMain()
            return
        }
        binding = ActivityHotfoxOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        HotfoxSystemUi.hideScrollbars(binding.root)
        HotfoxSystemUi.constrainReadingWidth(binding.onboardingForeground)
        binding.btnOnboardingPrimary.setOnClickListener { onPrimary() }
        binding.btnOnboardingSecondary.setOnClickListener { onSecondary() }
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handleBack()
                }
            },
        )
        step = when {
            savedInstanceState != null ->
                runCatching {
                    HotfoxOnboardingFlow.Step.valueOf(savedInstanceState.getString(STATE_STEP).orEmpty())
                }.getOrDefault(HotfoxOnboardingFlow.firstStep())
            !startStep.isNullOrBlank() ->
                runCatching { HotfoxOnboardingFlow.Step.valueOf(startStep) }
                    .getOrDefault(HotfoxOnboardingFlow.Step.WELCOME)
            else -> HotfoxOnboardingFlow.firstStep()
        }
        render()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_STEP, step.name)
    }

    override fun onResume() {
        super.onResume()
        if (awaitingImport && facts().hasAccess) {
            awaitingImport = false
            go(HotfoxOnboardingFlow.Event.NEXT)
        }
    }

    private fun handleBack() {
        if (!guardedClick()) return
        val previous = HotfoxOnboardingFlow.advance(step, HotfoxOnboardingFlow.Event.BACK, facts())
        if (previous == step) {
            finish()
            return
        }
        step = previous
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
                showFoxPlanet()
                binding.layoutOnboardingNote.isVisible = false
                binding.btnOnboardingPrimary.setText(R.string.hotfox_already_have_subscription)
                binding.btnOnboardingPrimary.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.hf_arrow_right_ink,
                    0,
                )
                binding.btnOnboardingSecondary.setTextColor(getColor(R.color.hf_asset_cream))
                binding.btnOnboardingSecondary.isVisible = true
                binding.btnOnboardingSecondary.setText(R.string.hotfox_onboarding_buy_access)
            }
            HotfoxOnboardingFlow.Step.AUTO -> {
                binding.tvOnboardingTitle.setText(R.string.hotfox_onboarding_auto_title_ui)
                binding.tvOnboardingBody.setText(R.string.hotfox_onboarding_auto_body_ui)
                showAutoArt()
                binding.layoutOnboardingNote.isVisible = false
                binding.btnOnboardingPrimary.setText(R.string.hotfox_onboarding_use_auto)
                binding.btnOnboardingPrimary.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.hf_arrow_right_ink,
                    0,
                )
                binding.btnOnboardingSecondary.setTextColor(getColor(R.color.hf_asset_cream))
                binding.btnOnboardingSecondary.isVisible = true
                binding.btnOnboardingSecondary.setText(R.string.hotfox_onboarding_choose_manual)
            }
            HotfoxOnboardingFlow.Step.VPN_PERMISSION,
            HotfoxOnboardingFlow.Step.FIRST_CONNECTION,
            -> {
                binding.tvOnboardingTitle.setText(R.string.hotfox_onboarding_ready_title_ui)
                binding.tvOnboardingBody.setText(R.string.hotfox_onboarding_ready_body_ui)
                hideHero()
                binding.autoOrbitView.isVisible = false
                binding.imgOnboardingArt.setImageResource(R.drawable.hf_native_ready_complete)
                binding.imgOnboardingArt.alpha = 1f
                binding.imgOnboardingArt.isVisible = true
                binding.imgOnboardingOverlay.isVisible = false
                binding.layoutOnboardingNote.isVisible = true
                binding.btnOnboardingPrimary.setText(R.string.hotfox_onboarding_continue)
                binding.btnOnboardingPrimary.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
                binding.btnOnboardingSecondary.isVisible = false
            }
        }
        binding.btnOnboardingPrimary.contentDescription = binding.btnOnboardingPrimary.text
        binding.btnOnboardingSecondary.contentDescription = binding.btnOnboardingSecondary.text
    }

    private fun showFoxPlanet() {
        binding.onboardingHero.isVisible = true
        binding.onboardingHero.setMode(HotFoxHeroMode.SUBSCRIPTION)
        binding.onboardingHero.setFitParent(true)
        binding.onboardingHero.setHeroLayers(showPlanet = true, showFox = true)
        binding.onboardingHero.setAnimationMode(HotFoxHeroArtwork.AnimationMode.BREATHING)
        binding.autoOrbitView.isVisible = false
        binding.imgOnboardingArt.isVisible = false
        binding.imgOnboardingOverlay.isVisible = false
    }

    private fun showAutoArt() {
        hideHero()
        binding.imgOnboardingArt.isVisible = false
        binding.imgOnboardingOverlay.isVisible = false
        binding.autoOrbitView.isVisible = true
    }

    private fun hideHero() {
        binding.onboardingHero.setAnimationMode(HotFoxHeroArtwork.AnimationMode.NONE)
        binding.onboardingHero.setHeroLayers(showPlanet = false, showFox = false)
        binding.onboardingHero.isVisible = false
    }

    private fun guardedClick(): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (now - lastClickElapsed < CLICK_GUARD_MS) return false
        lastClickElapsed = now
        return true
    }

    private fun onPrimary() {
        if (!guardedClick()) return
        when (step) {
            HotfoxOnboardingFlow.Step.VPN_PERMISSION -> requestVpn()
            HotfoxOnboardingFlow.Step.AUTO -> {
                HotfoxServerSelection.setAutoMode(true)
                go(HotfoxOnboardingFlow.Event.KEEP_AUTO)
            }
            HotfoxOnboardingFlow.Step.WELCOME,
            HotfoxOnboardingFlow.Step.ACCESS,
            -> {
                if (facts().hasAccess) {
                    go(HotfoxOnboardingFlow.Event.NEXT)
                } else {
                    awaitingImport = true
                    requestImport.launch(Intent(this, HotfoxHttpsImportActivity::class.java))
                }
            }
            HotfoxOnboardingFlow.Step.FIRST_CONNECTION -> {
                if (HotfoxOnboardingFlow.completesOnboarding(step, HotfoxOnboardingFlow.Event.FINISH)) {
                    HotfoxOnboardingStore.markComplete()
                    finishToMain()
                }
            }
        }
    }

    private fun onSecondary() {
        if (!guardedClick()) return
        when (step) {
            HotfoxOnboardingFlow.Step.AUTO -> {
                requestServerPick.launch(
                    Intent(this, MainActivity::class.java)
                        .putExtra(MainActivity.EXTRA_SKIP_ONBOARDING, true)
                        .putExtra(MainActivity.EXTRA_OPEN_SECTION, MainActivity.SECTION_SERVERS)
                        .putExtra(MainActivity.EXTRA_ONBOARDING_SERVER_PICK, true),
                )
            }
            HotfoxOnboardingFlow.Step.WELCOME,
            HotfoxOnboardingFlow.Step.ACCESS,
            -> requestPurchase.launch(Intent(this, RenewalActivity::class.java))
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
