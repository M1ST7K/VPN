package com.v2ray.ang.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.core.CoreServiceManager
import com.v2ray.ang.databinding.ActivityHotfoxSettingsBinding
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.vpn.HotfoxAutopilotStore
import com.v2ray.ang.vpn.HotfoxRoutingStore

class HotfoxSettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHotfoxSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHotfoxSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        HotfoxChrome.bindBack(this)
        HotfoxChrome.bindBottomNav(this, HotfoxChrome.SETTINGS)
        renderRows()
    }

    override fun onResume() {
        super.onResume()
        renderRows()
    }

    private fun renderRows() {
        binding.settingsRows.removeAllViews()
        val routing = HotfoxRoutingStore.load()
        val autopilotOn = HotfoxAutopilotStore.policy().enabled
        val ipv6 = MmkvManager.decodeSettingsBool(AppConfig.PREF_IPV6_ENABLED)
        addHeader(R.string.hotfox_settings_group_main)
        addRow(R.drawable.ic_hotfox_line_bolt, R.string.hotfox_settings_autopilot, R.string.hotfox_settings_autopilot_hint, if (autopilotOn) R.string.hotfox_settings_on else R.string.hotfox_settings_off) {
            startActivity(Intent(this, HotfoxAutopilotActivity::class.java))
        }
        addRow(R.drawable.ic_hotfox_line_lock, R.string.hotfox_always_on_title, R.string.hotfox_always_on_status, R.string.hotfox_always_on_not_configured) {
            startActivity(Intent(this, HotfoxAlwaysOnActivity::class.java))
        }
        addRow(R.drawable.ic_hotfox_line_bell, R.string.hotfox_settings_notifications, R.string.hotfox_settings_notifications_hint, if (androidx.core.app.NotificationManagerCompat.from(this).areNotificationsEnabled()) R.string.hotfox_notifications_on else R.string.hotfox_notifications_off) {
            runCatching { startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)) }
        }
        addHeader(R.string.hotfox_settings_group_network)
        addRow(R.drawable.ic_hotfox_line_info, R.string.hotfox_settings_diagnostics, R.string.hotfox_settings_diagnostics_hint, 0) {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_COPY_DIAGNOSTICS, true)
                    .putExtra(MainActivity.EXTRA_SKIP_ONBOARDING, true)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            )
            finish()
        }
        addRow(R.drawable.ic_hotfox_line_route, R.string.hotfox_routing_lan, R.string.hotfox_settings_lan_hint, if (routing.lanAccess) R.string.hotfox_route_lan_on else R.string.hotfox_route_lan_off) {
            startActivity(Intent(this, HotfoxRoutingPrivacyActivity::class.java))
        }
        addRow(R.drawable.ic_hotfox_line_globe, R.string.hotfox_routing_dns, R.string.hotfox_settings_dns_hint, if (routing.dnsThroughVpn) R.string.hotfox_routing_dns_vpn else R.string.hotfox_routing_dns_direct) {
            startActivity(Intent(this, HotfoxRoutingPrivacyActivity::class.java))
        }
        addRow(R.drawable.ic_hotfox_line_shield, R.string.hotfox_routing_ipv6, R.string.hotfox_settings_ipv6_hint, if (ipv6) R.string.hotfox_routing_ipv6_on else R.string.hotfox_routing_ipv6_off) {
            startActivity(Intent(this, HotfoxRoutingPrivacyActivity::class.java))
        }
        addHeader(R.string.hotfox_settings_group_other)
        addRow(R.drawable.ic_hotfox_line_info, R.string.hotfox_settings_about, R.string.hotfox_settings_about_hint, 0) {
            startActivity(Intent(this, AboutActivity::class.java))
        }
        addRow(R.drawable.ic_hotfox_line_lock, R.string.title_privacy_policy, R.string.hotfox_privacy_title, 0) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.hotfox_privacy_title)
                .setMessage(R.string.hotfox_privacy_message)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
        addRow(R.drawable.ic_hotfox_line_star, R.string.hotfox_settings_updates, 0, 0) {
            startActivity(Intent(this, CheckUpdateActivity::class.java))
        }
        addRow(R.drawable.ic_hotfox_line_link, R.string.hotfox_settings_ads, 0, 0) {
            showAdsToggle()
        }
    }

    private fun addHeader(title: Int) {
        val view = TextView(this)
        view.setText(title)
        view.setTextColor(getColor(R.color.hotfox_cream_muted))
        view.textSize = 11f
        view.setPadding(0, (16 * resources.displayMetrics.density).toInt(), 0, (6 * resources.displayMetrics.density).toInt())
        binding.settingsRows.addView(view)
    }

    private fun addRow(icon: Int, title: Int, subtitle: Int, value: Int, open: () -> Unit) {
        val view = LayoutInflater.from(this).inflate(R.layout.item_hotfox_settings_row, binding.settingsRows, false)
        view.findViewById<ImageView>(R.id.iv_row_icon).setImageResource(icon)
        view.findViewById<TextView>(R.id.tv_row_title).setText(title)
        val sub = view.findViewById<TextView>(R.id.tv_row_subtitle)
        if (subtitle != 0) {
            sub.setText(subtitle)
            sub.visibility = android.view.View.VISIBLE
        }
        if (value != 0) view.findViewById<TextView>(R.id.tv_row_value).setText(value)
        view.setOnClickListener { open() }
        binding.settingsRows.addView(view)
        val divider = android.view.View(this)
        divider.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            resources.getDimensionPixelSize(R.dimen.hotfox_hairline).coerceAtLeast(1),
        )
        divider.setBackgroundResource(R.color.hotfox_hairline)
        binding.settingsRows.addView(divider)
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
}
