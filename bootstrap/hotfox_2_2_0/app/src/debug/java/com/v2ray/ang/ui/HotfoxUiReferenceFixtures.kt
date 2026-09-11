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
        HotfoxUiVisualOverride.ReferenceAppRow("Chrome", "com.android.chrome", true),
        HotfoxUiVisualOverride.ReferenceAppRow("Messages", "com.google.android.apps.messaging", true),
        HotfoxUiVisualOverride.ReferenceAppRow("YouTube", "com.google.android.youtube", false),
        HotfoxUiVisualOverride.ReferenceAppRow("Maps", "com.google.android.apps.maps", false),
        HotfoxUiVisualOverride.ReferenceAppRow("Photos", "com.google.android.apps.photos", false),
        HotfoxUiVisualOverride.ReferenceAppRow("Clock", "com.google.android.deskclock", false),
        HotfoxUiVisualOverride.ReferenceAppRow("Settings", "com.android.settings", false),
        HotfoxUiVisualOverride.ReferenceAppRow("Files", "com.android.documentsui", false),
    )
}
