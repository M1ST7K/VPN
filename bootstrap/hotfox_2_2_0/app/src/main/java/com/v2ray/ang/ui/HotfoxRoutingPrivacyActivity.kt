package com.v2ray.ang.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.v2ray.ang.R
import com.v2ray.ang.core.CoreServiceManager
import com.v2ray.ang.databinding.ActivityHotfoxRoutingBinding
import com.v2ray.ang.extension.toast
import com.v2ray.ang.vpn.HotfoxRoutingMode
import com.v2ray.ang.vpn.HotfoxRoutingStore

class HotfoxRoutingPrivacyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHotfoxRoutingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHotfoxRoutingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        HotfoxChrome.bindBack(this)
        binding.btnRoutingMode.setOnClickListener { finish() }
        binding.rowRoutingMode.setOnClickListener { showModePicker() }
        binding.btnRoutingAdvanced.setOnClickListener {
            startActivity(Intent(this, RoutingSettingActivity::class.java))
        }
        binding.rowRoutingApps.setOnClickListener {
            startActivity(Intent(this, HotfoxAppsRulesActivity::class.java))
        }
        binding.rowRoutingLan.setOnClickListener {
            val snapshot = HotfoxRoutingStore.load()
            HotfoxRoutingStore.saveLan(!snapshot.lanAccess)
            toast(R.string.hotfox_route_changed)
            if (CoreServiceManager.isRunning()) {
                CoreServiceManager.restartForRouting(this)
            }
            render()
        }
        binding.rowRoutingDns.setOnClickListener {
            val snapshot = HotfoxRoutingStore.load()
            HotfoxRoutingStore.saveDns(!snapshot.dnsThroughVpn)
            toast(R.string.hotfox_route_changed)
            if (CoreServiceManager.isRunning()) {
                CoreServiceManager.restartForRouting(this)
            }
            render()
        }
        binding.rowRoutingIpv6.setOnClickListener {
            val enabled = com.v2ray.ang.handler.MmkvManager.decodeSettingsBool(
                com.v2ray.ang.AppConfig.PREF_IPV6_ENABLED,
            )
            com.v2ray.ang.handler.MmkvManager.encodeSettings(
                com.v2ray.ang.AppConfig.PREF_IPV6_ENABLED,
                !enabled,
            )
            toast(R.string.hotfox_route_changed)
            if (CoreServiceManager.isRunning()) {
                CoreServiceManager.restartForRouting(this)
            }
            render()
        }
        render()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val snapshot = HotfoxRoutingStore.load()
        binding.tvRoutingMode.text = snapshot.uiLabel()
        binding.tvRoutingModeDetail.setText(
            when (snapshot.mode) {
                HotfoxRoutingMode.SMART -> R.string.hotfox_route_smart_summary
                HotfoxRoutingMode.GLOBAL -> R.string.hotfox_route_global_summary
                HotfoxRoutingMode.INCLUDE_APPS -> R.string.hotfox_route_include_summary
                HotfoxRoutingMode.EXCLUDE_APPS -> R.string.hotfox_route_exclude_summary
                HotfoxRoutingMode.CUSTOM -> R.string.hotfox_route_custom_summary
            },
        )
        binding.tvRoutingLanValue.setText(
            if (snapshot.lanAccess) R.string.hotfox_route_lan_on else R.string.hotfox_route_lan_off,
        )
        binding.tvRoutingDnsValue.setText(
            if (snapshot.dnsThroughVpn) R.string.hotfox_routing_dns_vpn else R.string.hotfox_routing_dns_direct,
        )
        val ipv6 = com.v2ray.ang.handler.MmkvManager.decodeSettingsBool(
            com.v2ray.ang.AppConfig.PREF_IPV6_ENABLED,
        )
        binding.tvRoutingIpv6Value.setText(
            if (ipv6) R.string.hotfox_routing_ipv6_on else R.string.hotfox_routing_ipv6_off,
        )
    }

    private fun showModePicker() {
        val snapshot = HotfoxRoutingStore.load()
        val modes = arrayOf(
            HotfoxRoutingMode.SMART,
            HotfoxRoutingMode.GLOBAL,
            HotfoxRoutingMode.INCLUDE_APPS,
            HotfoxRoutingMode.EXCLUDE_APPS,
            HotfoxRoutingMode.CUSTOM,
        )
        val labels = arrayOf(
            getString(R.string.hotfox_route_smart),
            getString(R.string.hotfox_route_global),
            getString(R.string.hotfox_route_include),
            getString(R.string.hotfox_route_exclude),
            getString(R.string.hotfox_route_custom),
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.hotfox_smart_routing_title)
            .setSingleChoiceItems(labels, modes.indexOf(snapshot.mode)) { dialog, which ->
                val selected = modes[which]
                if (selected != snapshot.mode) {
                    HotfoxRoutingStore.saveMode(selected)
                    toast(R.string.hotfox_route_changed)
                    if (CoreServiceManager.isRunning()) {
                        CoreServiceManager.restartForRouting(this)
                    }
                    if (selected == HotfoxRoutingMode.INCLUDE_APPS || selected == HotfoxRoutingMode.EXCLUDE_APPS) {
                        startActivity(Intent(this, PerAppProxyActivity::class.java))
                    }
                    if (selected == HotfoxRoutingMode.CUSTOM) {
                        startActivity(Intent(this, RoutingSettingActivity::class.java))
                    }
                    render()
                }
                dialog.dismiss()
            }
            .setPositiveButton(
                if (snapshot.lanAccess) R.string.hotfox_route_lan_on else R.string.hotfox_route_lan_off,
            ) { _, _ ->
                HotfoxRoutingStore.saveLan(!snapshot.lanAccess)
                toast(R.string.hotfox_route_changed)
                if (CoreServiceManager.isRunning()) {
                    CoreServiceManager.restartForRouting(this)
                }
                render()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
