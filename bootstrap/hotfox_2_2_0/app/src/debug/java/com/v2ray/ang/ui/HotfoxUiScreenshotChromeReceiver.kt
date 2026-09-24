package com.v2ray.ang.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Debug-only chrome switcher. Paints Disconnected/Connecting/Connected on the
 * already-resumed MainActivity without CLEAR_TASK, which ANRs the software emulator.
 * Does not write VPN, entitlement, or subscription stores.
 */
class HotfoxUiScreenshotChromeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val chrome = intent.getStringExtra(EXTRA_CHROME) ?: return
        HotfoxUiQaPainter.applyConnectionChromeNow(chrome)
    }

    companion object {
        const val ACTION = "com.hotfox.vpn.action.UI_SCREENSHOT_CHROME"
        const val EXTRA_CHROME = "chrome"
    }
}
