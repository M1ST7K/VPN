package com.v2ray.ang.ui

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
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
        if (HotfoxOnboardingStore.isComplete()) {
            finishToMain()
            return
        }
        binding = ActivityHotfoxOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnOnboardingPrimary.setOnClickListener { onPrimary() }
        binding.btnOnboardingSecondary.setOnClickListener { onSecondary() }
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
        val titleId = resources.getIdentifier(HotfoxOnboardingFlow.titleResName(step), "string", packageName)
        val bodyId = resources.getIdentifier(HotfoxOnboardingFlow.bodyResName(step), "string", packageName)
        val primaryId = resources.getIdentifier(HotfoxOnboardingFlow.primaryResName(step), "string", packageName)
        binding.tvOnboardingTitle.setText(if (titleId != 0) titleId else R.string.hotfox_onboarding_welcome_title)
        binding.tvOnboardingBody.setText(if (bodyId != 0) bodyId else R.string.hotfox_onboarding_welcome_body)
        binding.btnOnboardingPrimary.setText(if (primaryId != 0) primaryId else R.string.hotfox_onboarding_continue)
        when (step) {
            HotfoxOnboardingFlow.Step.AUTO -> {
                binding.btnOnboardingSecondary.isVisible = true
                binding.btnOnboardingSecondary.setText(R.string.hotfox_onboarding_auto_manual)
            }
            HotfoxOnboardingFlow.Step.ACCESS -> {
                binding.btnOnboardingSecondary.isVisible = true
                binding.btnOnboardingSecondary.setText(R.string.hotfox_already_have_subscription)
            }
            else -> binding.btnOnboardingSecondary.isVisible = false
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
            HotfoxOnboardingFlow.Step.FIRST_CONNECTION -> {
                if (HotfoxOnboardingFlow.completesOnboarding(step, HotfoxOnboardingFlow.Event.FINISH)) {
                    HotfoxOnboardingStore.markComplete()
                    finishToMain()
                }
            }
            else -> go(HotfoxOnboardingFlow.Event.NEXT)
        }
    }

    private fun onSecondary() {
        when (step) {
            HotfoxOnboardingFlow.Step.AUTO -> {
                HotfoxServerSelection.setAutoMode(false)
                go(HotfoxOnboardingFlow.Event.MANUAL_SERVERS)
            }
            HotfoxOnboardingFlow.Step.ACCESS -> go(HotfoxOnboardingFlow.Event.NEXT)
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
