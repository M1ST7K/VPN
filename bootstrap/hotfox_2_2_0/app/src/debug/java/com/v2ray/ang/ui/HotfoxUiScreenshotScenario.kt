package com.v2ray.ang.ui

/**
 * Debug-only screenshot scenarios. Not present in release source sets.
 */
enum class HotfoxUiScreenshotScenario(val id: String) {
    SPLASH("01_SPLASH"),
    ONBOARD_CONNECT("02_ONBOARD_CONNECT"),
    ONBOARD_AUTO("03_ONBOARD_AUTO"),
    ONBOARD_READY("04_ONBOARD_READY"),
    DISCONNECTED("05_DISCONNECTED"),
    CONNECTING("06_CONNECTING_VISUAL"),
    PROTECTED("07_PROTECTED_VISUAL"),
    ADD_CONNECTION_SHEET("08_ADD_CONNECTION_SHEET"),
    HTTPS_SUBSCRIPTION("09_HTTPS_SUBSCRIPTION"),
    SERVERS("10_SERVERS"),
    SERVER_DETAILS("11_SERVER_DETAILS"),
    SUBSCRIPTION("12_SUBSCRIPTION"),
    SETTINGS("13_SETTINGS"),
    SMART_ROUTING("14_SMART_ROUTING"),
    APPS_RULES("15_APPS_RULES"),
    AUTOPILOT("16_AUTOPILOT"),
    SHADOW("17_SHADOW"),
    ALWAYS_ON("18_ALWAYS_ON"),
    ;

    companion object {
        fun fromId(raw: String?): HotfoxUiScreenshotScenario? =
            entries.firstOrNull { it.id == raw || it.name == raw }
    }
}
