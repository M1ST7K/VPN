package com.v2ray.ang.ui

import com.v2ray.ang.R

/**
 * Debug-only visual reference data. Never written to production stores.
 */
object HotfoxUiReferenceFixtures {
    fun servers(): List<HotfoxUiVisualOverride.ReferenceServerRow> = listOf(
        HotfoxUiVisualOverride.ReferenceServerRow("Amsterdam", "Нидерланды", "18 ms", R.drawable.hf_flag_nl),
        HotfoxUiVisualOverride.ReferenceServerRow("Frankfurt", "Германия", "24 ms", R.drawable.hf_flag_de),
        HotfoxUiVisualOverride.ReferenceServerRow("Paris", "Франция", "31 ms", R.drawable.hf_flag_fr),
        HotfoxUiVisualOverride.ReferenceServerRow("London", "Великобритания", "28 ms", R.drawable.hf_flag_gb),
        HotfoxUiVisualOverride.ReferenceServerRow("New York", "США", "86 ms", R.drawable.hf_flag_us),
        HotfoxUiVisualOverride.ReferenceServerRow("Toronto", "Канада", "92 ms", R.drawable.hf_flag_ca),
    )

    fun apps(): List<HotfoxUiVisualOverride.ReferenceAppRow> = listOf(
        HotfoxUiVisualOverride.ReferenceAppRow("Telegram", "Мессенджер", true),
        HotfoxUiVisualOverride.ReferenceAppRow("Chrome", "Браузер", true),
        HotfoxUiVisualOverride.ReferenceAppRow("Банк", "Финансы", false),
        HotfoxUiVisualOverride.ReferenceAppRow("YouTube", "Видео", true),
        HotfoxUiVisualOverride.ReferenceAppRow("Maps", "Карты и навигация", false),
        HotfoxUiVisualOverride.ReferenceAppRow("Photos", "Галерея", false),
        HotfoxUiVisualOverride.ReferenceAppRow("Mail", "Почта", true),
    )
}
