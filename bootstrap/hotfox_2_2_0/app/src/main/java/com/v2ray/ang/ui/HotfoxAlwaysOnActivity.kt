package com.v2ray.ang.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ActivityHotfoxAlwaysOnBinding

class HotfoxAlwaysOnActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHotfoxAlwaysOnBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotfoxSystemUi.applyDarkEditorialBars(this)
        binding = ActivityHotfoxAlwaysOnBinding.inflate(layoutInflater)
        setContentView(binding.root)
        HotfoxSystemUi.hideScrollbars(binding.root)
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
            onSettings = { startActivity(Intent(this, HotfoxSettingsActivity::class.java)) },
        )
        binding.rowAlwaysOn.setOnClickListener { openAndroidVpn() }
        binding.rowLockdown.setOnClickListener { openAndroidVpn() }
        binding.btnOpenAndroidVpn.setOnClickListener { openAndroidVpn() }
        binding.tvAlwaysOnValue.setText(R.string.hotfox_unknown)
        binding.tvLockdownValue.setText(R.string.hotfox_unknown)
    }

    private fun openAndroidVpn() {
        startActivity(Intent(Settings.ACTION_VPN_SETTINGS))
    }
}
