package com.v2ray.ang.vpn

import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.MmkvManager

object HotfoxRoutingStore {
    fun load(): RoutingPolicySnapshot {
        val canonical = runCatching {
            MmkvManager.decodeSettingsString(HotfoxRoutingPolicy.PREF_MODE)
        }.getOrNull()
        val legacy = runCatching {
            MmkvManager.decodeSettingsString(AppConfig.PREF_SMART_ROUTING_MODE)
        }.getOrNull()
        val stored = HotfoxRoutingPolicy.resolveStoredMode(canonical, legacy)
        if (stored.persistCanonical) {
            persistMode(stored.mode)
        }
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
            mode = stored.mode,
            selectedApps = apps.filter { it.isNotBlank() }.toSet(),
            rules = rules,
            lanAccess = lan,
            adsBlocked = ads,
            dnsThroughVpn = dnsVpn,
        )
    }

    fun saveMode(mode: HotfoxRoutingMode) {
        persistMode(mode)
        HotfoxRoutingApply.bump()
    }

    private fun persistMode(mode: HotfoxRoutingMode) {
        MmkvManager.encodeSettings(HotfoxRoutingPolicy.PREF_MODE, mode.storageValue)
        val perApp = mode == HotfoxRoutingMode.INCLUDE_APPS || mode == HotfoxRoutingMode.EXCLUDE_APPS
        MmkvManager.encodeSettings(AppConfig.PREF_PER_APP_PROXY, perApp)
        MmkvManager.encodeSettings(
            AppConfig.PREF_BYPASS_APPS,
            mode == HotfoxRoutingMode.EXCLUDE_APPS,
        )
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
