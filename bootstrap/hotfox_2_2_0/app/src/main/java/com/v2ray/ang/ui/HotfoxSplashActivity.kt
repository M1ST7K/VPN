package com.v2ray.ang.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.v2ray.ang.databinding.ActivityHotfoxSplashBinding
import com.v2ray.ang.vpn.HotfoxOnboardingStore

class HotfoxSplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityHotfoxSplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // No artificial delay: proceed after the first real layout of the cold/warm start.
        binding.root.post { continueToApp() }
    }

    private fun continueToApp() {
        if (isFinishing) return
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
