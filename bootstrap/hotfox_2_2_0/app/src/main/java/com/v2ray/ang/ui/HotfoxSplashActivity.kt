package com.v2ray.ang.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.v2ray.ang.databinding.ActivityHotfoxSplashBinding
import com.v2ray.ang.vpn.HotfoxOnboardingStore

class HotfoxSplashActivity : AppCompatActivity() {
    companion object {
        private const val STATE_HANDED_OFF = "hotfox_splash_handed_off"
    }

    internal var skipAutoAdvance = false
    private var handedOff = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotfoxSystemUi.applyDarkEditorialBars(this)
        val binding = ActivityHotfoxSplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        handedOff = savedInstanceState?.getBoolean(STATE_HANDED_OFF) == true
        // No artificial delay: proceed after the first real layout of the cold/warm start.
        if (!handedOff) {
            binding.root.post {
                if (!skipAutoAdvance) continueToApp()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_HANDED_OFF, handedOff)
    }

    private fun continueToApp() {
        if (handedOff || isFinishing) return
        handedOff = true
        val next = if (HotfoxOnboardingStore.shouldPrompt()) {
            Intent(this, HotfoxOnboardingActivity::class.java)
        } else {
            Intent(this, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_SKIP_ONBOARDING, true)
        }
        startActivity(next)
        finish()
    }
}
