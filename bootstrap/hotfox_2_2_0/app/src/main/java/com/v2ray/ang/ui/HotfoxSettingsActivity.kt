package com.v2ray.ang.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.v2ray.ang.R
import com.v2ray.ang.core.CoreServiceManager
import com.v2ray.ang.databinding.ActivityHotfoxSettingsBinding
import com.v2ray.ang.vpn.HotfoxRoutingStore

class HotfoxSettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHotfoxSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHotfoxSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val rows = listOf(
            Row(R.string.hotfox_settings_connection_dns) {
                startActivity(Intent(this, SettingsActivity::class.java))
            },
            Row(R.string.hotfox_settings_apps) {
                startActivity(Intent(this, HotfoxAppsRulesActivity::class.java))
            },
            Row(R.string.hotfox_smart_routing) {
                startActivity(Intent(this, HotfoxRoutingPrivacyActivity::class.java))
            },
            Row(R.string.hotfox_settings_subscriptions) {
                startActivity(Intent(this, SubSettingActivity::class.java))
            },
            Row(R.string.hotfox_settings_ads) { showAdsToggle() },
            Row(R.string.hotfox_settings_shadow) {
                startActivity(Intent(this, HotfoxShadowActivity::class.java))
            },
            Row(R.string.hotfox_settings_autopilot) {
                startActivity(Intent(this, HotfoxAutopilotActivity::class.java))
            },
            Row(R.string.hotfox_always_on_title) {
                startActivity(Intent(this, HotfoxAlwaysOnActivity::class.java))
            },
            Row(R.string.hotfox_settings_diagnostics) {
                startActivity(
                    Intent(this, MainActivity::class.java)
                        .putExtra(MainActivity.EXTRA_COPY_DIAGNOSTICS, true)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                )
                finish()
            },
            Row(R.string.hotfox_settings_updates) {
                startActivity(Intent(this, CheckUpdateActivity::class.java))
            },
            Row(R.string.hotfox_settings_about) {
                startActivity(Intent(this, AboutActivity::class.java))
            },
        )
        val inflater = LayoutInflater.from(this)
        rows.forEach { row ->
            val view = inflater.inflate(R.layout.item_hotfox_settings_row, binding.settingsRows, false)
            view.findViewById<TextView>(R.id.tv_row_title).setText(row.title)
            view.setOnClickListener { row.open() }
            binding.settingsRows.addView(view)
            val divider = android.view.View(this)
            divider.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(R.dimen.hotfox_hairline).coerceAtLeast(1),
            )
            divider.setBackgroundResource(R.color.hotfox_hairline)
            binding.settingsRows.addView(divider)
        }
    }

    private fun showAdsToggle() {
        val enabled = HotfoxRoutingStore.load().adsBlocked
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.hotfox_settings_ads)
            .setMessage(
                getString(
                    R.string.hotfox_ads_message,
                    getString(if (enabled) R.string.hotfox_ads_on else R.string.hotfox_ads_off),
                ),
            )
            .setPositiveButton(if (enabled) R.string.hotfox_ads_disable else R.string.hotfox_ads_enable) { _, _ ->
                HotfoxRoutingStore.saveAds(!enabled)
                if (CoreServiceManager.isRunning()) {
                    CoreServiceManager.restartForRouting(this)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private data class Row(val title: Int, val open: () -> Unit)
}
