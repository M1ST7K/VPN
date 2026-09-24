package com.v2ray.ang.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * One-shot resume after a timed Autopilot pause. Not used for
 * [HotfoxPauseKind.UNTIL_NETWORK_CHANGE], which follows network generation.
 */
class HotfoxAutopilotPauseReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        HotfoxAutopilotRuntime.ensureStarted(app)
        HotfoxAutopilotRuntime.apply(app, HotfoxAutopilotSource.PAUSE)
    }
}
