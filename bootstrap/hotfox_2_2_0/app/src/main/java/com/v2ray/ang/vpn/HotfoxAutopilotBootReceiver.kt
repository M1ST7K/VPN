package com.v2ray.ang.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Recovers Autopilot policy after boot. Does not resurrect a stale VPN session id;
 * intent is derived from current network/permission/entitlement truth.
 */
class HotfoxAutopilotBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }
        val app = context.applicationContext
        HotfoxAutopilotRuntime.ensureStarted(app)
        HotfoxAutopilotRuntime.apply(app, HotfoxAutopilotSource.BOOT)
    }
}
