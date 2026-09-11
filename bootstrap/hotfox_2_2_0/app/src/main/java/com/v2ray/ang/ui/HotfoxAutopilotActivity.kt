package com.v2ray.ang.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.v2ray.ang.R
import com.v2ray.ang.core.CoreServiceManager
import com.v2ray.ang.databinding.ActivityHotfoxAutopilotBinding
import com.v2ray.ang.extension.toast
import com.v2ray.ang.vpn.HotfoxAutopilotLabels
import com.v2ray.ang.vpn.HotfoxAutopilotRuntime
import com.v2ray.ang.vpn.HotfoxAutopilotSource
import com.v2ray.ang.vpn.HotfoxAutopilotStore
import com.v2ray.ang.vpn.HotfoxConnectionIntent
import com.v2ray.ang.vpn.HotfoxNetworkIdentity
import com.v2ray.ang.vpn.HotfoxNetworkKind
import com.v2ray.ang.vpn.HotfoxPauseKind
import com.v2ray.ang.vpn.HotfoxProtectionLevel
import com.v2ray.ang.vpn.HotfoxProtectionProfiles
import com.v2ray.ang.vpn.HotfoxRoutingStore
import com.v2ray.ang.vpn.HotfoxShadowStore
import com.v2ray.ang.vpn.HotfoxTransportKind

class HotfoxAutopilotActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHotfoxAutopilotBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHotfoxAutopilotBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnAutopilotToggle.setOnClickListener { togglePolicy() }
        binding.switchAutopilot.setOnClickListener { togglePolicy() }
        binding.btnAutopilotPause.setOnClickListener { showPause() }
        binding.btnAutopilotProtection.setOnClickListener { showProtection() }
        binding.btnAutopilotTrusted.setOnClickListener { showTrusted() }
        binding.rowAutopilotWifi.setOnClickListener { toggleWifi() }
        binding.rowAutopilotCellular.setOnClickListener { toggleCellular() }
        binding.rowAutopilotNotifications.setOnClickListener {
            runCatching {
                startActivity(
                    android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName),
                )
            }
        }
        binding.btnAutopilotDone.setOnClickListener { finish() }
        HotfoxChrome.bindBack(this)
        render()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val policy = HotfoxAutopilotStore.policy()
        binding.switchAutopilot.isChecked = policy.enabled
        binding.tvAutopilotEnabledHint.setText(
            if (policy.enabled) R.string.hotfox_autopilot_enabled else R.string.hotfox_autopilot_disabled,
        )
        binding.tvAutopilotWifiValue.setText(
            if (policy.connectUnknownWifi) R.string.hotfox_autopilot_connect else R.string.hotfox_autopilot_skip,
        )
        binding.tvAutopilotCellularValue.setText(
            if (policy.connectCellular) R.string.hotfox_autopilot_connect else R.string.hotfox_autopilot_skip,
        )
        val trustedCount = HotfoxAutopilotStore.trusted().size
        binding.tvAutopilotTrustedValue.text = getString(R.string.hotfox_autopilot_trusted_count, trustedCount)
        val paused = HotfoxAutopilotStore.pause() != null
        binding.tvAutopilotPauseValue.setText(
            if (paused) R.string.hotfox_autopilot_pause_active else R.string.hotfox_autopilot_pause_idle,
        )
        val decision = HotfoxAutopilotStore.lastDecision()
        binding.tvAutopilotDecision.text = decision?.uiLabel()
            ?: HotfoxAutopilotLabels.protectionLabel(HotfoxAutopilotStore.protectionLevel())
        val captive = decision?.intent == HotfoxConnectionIntent.WAIT_FOR_CAPTIVE_PORTAL
        binding.tvAutopilotCaptive.setText(
            if (captive) R.string.hotfox_error_captive_title else R.string.hotfox_autopilot_captive_wait,
        )
        val notificationsOn = androidx.core.app.NotificationManagerCompat.from(this).areNotificationsEnabled()
        binding.tvAutopilotNotifications.setText(
            if (notificationsOn) R.string.hotfox_notifications_on else R.string.hotfox_notifications_off,
        )
    }

    private fun togglePolicy() {
        val policy = HotfoxAutopilotStore.policy()
        HotfoxAutopilotStore.setPolicy(policy.copy(enabled = !policy.enabled))
        HotfoxAutopilotRuntime.apply(this, HotfoxAutopilotSource.NETWORK)
        render()
    }

    private fun toggleWifi() {
        val policy = HotfoxAutopilotStore.policy()
        HotfoxAutopilotStore.setPolicy(policy.copy(connectUnknownWifi = !policy.connectUnknownWifi))
        HotfoxAutopilotRuntime.apply(this, HotfoxAutopilotSource.NETWORK)
        render()
    }

    private fun toggleCellular() {
        val policy = HotfoxAutopilotStore.policy()
        HotfoxAutopilotStore.setPolicy(policy.copy(connectCellular = !policy.connectCellular))
        HotfoxAutopilotRuntime.apply(this, HotfoxAutopilotSource.NETWORK)
        render()
    }

    private fun showPause() {
        val kinds = arrayOf(
            HotfoxPauseKind.MINUTES_5,
            HotfoxPauseKind.MINUTES_15,
            HotfoxPauseKind.HOUR_1,
            HotfoxPauseKind.UNTIL_NETWORK_CHANGE,
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.hotfox_settings_pause)
            .setItems(kinds.map { HotfoxAutopilotLabels.pauseLabel(it) }.toTypedArray()) { _, i ->
                HotfoxAutopilotStore.startPause(kinds[i], System.currentTimeMillis())
                HotfoxAutopilotRuntime.apply(this, HotfoxAutopilotSource.PAUSE)
                toast(HotfoxAutopilotStore.lastDecision()?.uiLabel() ?: HotfoxAutopilotLabels.pauseLabel(kinds[i]))
                render()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showProtection() {
        val levels = arrayOf(
            HotfoxProtectionLevel.SPEED,
            HotfoxProtectionLevel.BALANCE,
            HotfoxProtectionLevel.MAX_PROTECTION,
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.hotfox_settings_protection)
            .setItems(levels.map { HotfoxAutopilotLabels.protectionLabel(it) }.toTypedArray()) { _, i ->
                val level = levels[i]
                HotfoxAutopilotStore.setProtectionLevel(level)
                val defaults = HotfoxProtectionProfiles.defaults(level, HotfoxAutopilotRuntime.classify(this))
                HotfoxRoutingStore.saveMode(defaults.routingMode)
                HotfoxRoutingStore.saveLan(defaults.lanAccess)
                HotfoxRoutingStore.saveAds(defaults.adsBlocked)
                HotfoxShadowStore.setShadowAuto(defaults.shadowAuto)
                if (CoreServiceManager.isRunning()) {
                    CoreServiceManager.restartForRouting(this)
                }
                toast(HotfoxAutopilotLabels.protectionLabel(level))
                render()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showTrusted() {
        val identity = HotfoxNetworkIdentity.current(this)
        val kind = HotfoxAutopilotRuntime.classify(this)
        val labeled = when (kind) {
            HotfoxNetworkKind.TRUSTED_HOME -> getString(R.string.hotfox_network_home)
            HotfoxNetworkKind.TRUSTED_OFFICE -> getString(R.string.hotfox_network_office)
            HotfoxNetworkKind.CELLULAR -> getString(R.string.hotfox_network_cellular)
            HotfoxNetworkKind.CAPTIVE_PORTAL -> getString(R.string.hotfox_network_captive)
            HotfoxNetworkKind.UNKNOWN_WIFI -> getString(R.string.hotfox_network_unknown_wifi)
            HotfoxNetworkKind.NONE -> getString(R.string.hotfox_network_none)
        }
        if (identity.opaqueId.isBlank() || identity.transport != HotfoxTransportKind.WIFI) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.hotfox_settings_trusted)
                .setMessage(getString(R.string.hotfox_trusted_now, labeled) + "\n" + getString(R.string.hotfox_trusted_wifi_only))
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.hotfox_settings_trusted)
            .setMessage(getString(R.string.hotfox_trusted_now, labeled) + "\n" + getString(R.string.hotfox_trusted_mark))
            .setItems(
                arrayOf(
                    getString(R.string.hotfox_trusted_home),
                    getString(R.string.hotfox_trusted_office),
                    getString(R.string.hotfox_trusted_clear),
                ),
            ) { _, i ->
                when (i) {
                    0 -> HotfoxAutopilotStore.markTrusted(identity.opaqueId, HotfoxNetworkKind.TRUSTED_HOME)
                    1 -> HotfoxAutopilotStore.markTrusted(identity.opaqueId, HotfoxNetworkKind.TRUSTED_OFFICE)
                    2 -> HotfoxAutopilotStore.clearTrusted(identity.opaqueId)
                }
                HotfoxAutopilotRuntime.apply(this, HotfoxAutopilotSource.NETWORK)
                render()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
