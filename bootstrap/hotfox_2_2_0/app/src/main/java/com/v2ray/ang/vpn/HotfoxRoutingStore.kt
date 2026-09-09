package com.v2ray.ang.vpn

import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.MmkvManager

object HotfoxRoutingStore {
    fun load(): RoutingPolicySnapshot {
        val mode = HotfoxRoutingMode.fromStorage(
            runCatching { MmkvManager.decodeSettingsString(HotfoxRoutingPolicy.PREF_MODE) }.getOrNull()
                ?: runCatching { MmkvManager.decodeSettingsString(AppConfig.PREF_SMART_ROUTING_MODE) }.getOrNull(),
        )
        val lan = runCatching {
            MmkvManager.decodeSettingsBool(HotfoxRoutingPolicy.PREF_LAN, false)
        }.getOrDefault(false)
        val ads = runCatching { MmkvManager.decodeSettingsBool("hotfox_block_ads", false) }.getOrDefault(false)
        val dnsVpn = runCatching {
            MmkvManager.decodeSettingsBool(HotfoxRoutingPolicy.PREF_DNS_VPN, true)
        }.getOrDefault(true)
        val apps = runCatching {
            MmkvManager.decodeSettingsStringSet(AppConfig.PREF_PER_APP_PROXY_SET)
        }.getOrNull().orEmpty()
        val rules = HotfoxRoutingPolicy.parseRules(
            runCatching { MmkvManager.decodeSettingsString(HotfoxRoutingPolicy.PREF_RULES) }.getOrNull(),
        )
        return RoutingPolicySnapshot(
            mode = mode,
            selectedApps = apps.filter { it.isNotBlank() }.toSet(),
            rules = rules,
            lanAccess = lan,
            adsBlocked = ads,
            dnsThroughVpn = dnsVpn,
        )
    }

    fun saveMode(mode: HotfoxRoutingMode) {
        MmkvManager.encodeSettings(HotfoxRoutingPolicy.PREF_MODE, mode.storageValue)
        runCatching { MmkvManager.encodeSettings(AppConfig.PREF_SMART_ROUTING_MODE, mode.storageValue) }
        val perApp = mode == HotfoxRoutingMode.INCLUDE_APPS || mode == HotfoxRoutingMode.EXCLUDE_APPS
        MmkvManager.encodeSettings(AppConfig.PREF_PER_APP_PROXY, perApp)
        MmkvManager.encodeSettings(
            AppConfig.PREF_BYPASS_APPS,
            mode == HotfoxRoutingMode.EXCLUDE_APPS,
        )
        HotfoxRoutingApply.bump()
    }

    fun saveLan(enabled: Boolean) {
        MmkvManager.encodeSettings(HotfoxRoutingPolicy.PREF_LAN, enabled)
        HotfoxRoutingApply.bump()
    }

    fun saveAds(enabled: Boolean) {
        MmkvManager.encodeSettings("hotfox_block_ads", enabled)
        HotfoxRoutingApply.bump()
    }

    fun saveRules(rules: List<RoutingRule>) {
        MmkvManager.encodeSettings(
            HotfoxRoutingPolicy.PREF_RULES,
            HotfoxRoutingPolicy.encodeRules(rules),
        )
        HotfoxRoutingApply.bump()
    }
}
