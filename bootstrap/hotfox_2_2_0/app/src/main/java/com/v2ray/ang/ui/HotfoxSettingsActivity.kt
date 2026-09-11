package com.v2ray.ang.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ActivityHotfoxSettingsBinding
import com.v2ray.ang.extension.toast

class HotfoxSettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHotfoxSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHotfoxSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.findViewById<android.view.View>(R.id.btn_header_back)?.apply {
            visibility = android.view.View.VISIBLE
            setOnClickListener { finish() }
        }
        HotfoxNavBinder.bind(
            binding.root,
            HotfoxNavBinder.Destination.SETTINGS,
            onHome = { HotfoxNavBinder.openMain(this, MainActivity.SECTION_CONNECTION) },
            onServers = { HotfoxNavBinder.openMain(this, MainActivity.SECTION_SERVERS) },
            onSubscription = { HotfoxNavBinder.openMain(this, MainActivity.SECTION_SUBSCRIPTION) },
            onSettings = {},
        )
        binding.rowSettingsAutopilot.setOnClickListener {
            startActivity(Intent(this, HotfoxAutopilotActivity::class.java))
        }
        binding.rowSettingsAlwaysOn.setOnClickListener {
            startActivity(Intent(this, HotfoxAlwaysOnActivity::class.java))
        }
        binding.rowSettingsNotifications.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.rowSettingsDiagnostics.setOnClickListener {
            startActivity(Intent(this, LogcatActivity::class.java))
        }
        binding.rowSettingsLan.setOnClickListener {
            startActivity(Intent(this, HotfoxRoutingPrivacyActivity::class.java))
        }
        binding.rowSettingsDns.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.rowSettingsIpv6.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.rowSettingsAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
        binding.rowSettingsPrivacy.setOnClickListener {
            toast(R.string.hotfox_privacy_policy)
        }
    }
}
