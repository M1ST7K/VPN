package com.v2ray.ang.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.v2ray.ang.databinding.ActivityHotfoxSplashBinding
import com.v2ray.ang.vpn.HotfoxOnboardingStore

/**
 * Brand entry only. No fake loading delay and no VPN/session side effects.
 */
class HotfoxSplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityHotfoxSplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val next = if (HotfoxOnboardingStore.shouldPrompt()) {
            Intent(this, HotfoxOnboardingActivity::class.java)
        } else {
            Intent(this, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_SKIP_ONBOARDING, true)
        }
        startActivity(next.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        finish()
    }
}
