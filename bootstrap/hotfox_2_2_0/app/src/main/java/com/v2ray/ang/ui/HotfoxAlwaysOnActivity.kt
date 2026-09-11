package com.v2ray.ang.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ActivityHotfoxAlwaysOnBinding
import com.v2ray.ang.extension.toast

class HotfoxAlwaysOnActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHotfoxAlwaysOnBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHotfoxAlwaysOnBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnAlwaysOnOpen.setOnClickListener { openAndroidVpnSettings() }
        render()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        binding.tvAlwaysOnStatus.setText(R.string.hotfox_always_on_unknown)
        binding.tvAlwaysOnKill.setText(R.string.hotfox_always_on_unknown)
    }

    private fun openAndroidVpnSettings() {
        val opened = runCatching {
            startActivity(Intent(Settings.ACTION_VPN_SETTINGS))
            true
        }.getOrDefault(false)
        if (!opened) toast(R.string.hotfox_always_on_unavailable)
    }
}
