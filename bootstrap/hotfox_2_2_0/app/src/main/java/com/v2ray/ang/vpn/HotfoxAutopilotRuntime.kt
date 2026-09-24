package com.v2ray.ang.vpn

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.VpnService
import android.net.wifi.WifiManager
import android.os.Build
import com.v2ray.ang.commerce.CommercePreferences
import com.v2ray.ang.core.CoreServiceManager

/**
 * Event-driven Autopilot runtime. Registers one default-network callback and
 * never polls. Start/stop still go through [CoreServiceManager] / [VpnRestartGate].
 */
object HotfoxAutopilotRuntime {
    @Volatile
    private var callback: ConnectivityManager.NetworkCallback? = null

    @Volatile
    private var lastKind: HotfoxNetworkKind? = null

    @Volatile
    private var lastEventAtEpochMs: Long = 0L

    fun resetForTests() {
        lastKind = null
        lastEventAtEpochMs = 0L
    }

    fun ensureStarted(context: Context) {
        if (callback != null) return
        synchronized(this) {
            if (callback != null) return
            val app = context.applicationContext
            val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            val cb = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    onNetworkEvent(app)
                }

                override fun onLost(network: Network) {
                    onNetworkEvent(app)
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    onNetworkEvent(app)
                }
            }
            runCatching { cm.registerDefaultNetworkCallback(cb) }
            callback = cb
        }
    }

    fun classify(context: Context): HotfoxNetworkKind {
        val identity = HotfoxNetworkIdentity.current(context)
        return HotfoxTrustedNetworks.classify(
            transport = identity.transport,
            captive = identity.captive,
            opaqueNetworkId = identity.opaqueId,
            trusted = HotfoxAutopilotStore.trusted(),
        )
    }

    fun entitlementUsable(): Boolean {
        val origin = runCatching { CommercePreferences.accessOrigin() }
            .getOrDefault(CommercePreferences.ORIGIN_NONE)
        if (origin != CommercePreferences.ORIGIN_HOTFOX) return true
        return runCatching { AutoCommercialEligibility.live().entitlementUsable }.getOrDefault(false)
    }

    fun apply(context: Context, source: HotfoxAutopilotSource) {
        val app = context.applicationContext
        val granted = runCatching { VpnService.prepare(app) == null }.getOrDefault(false)
        val session = VpnSessionCoordinator.currentState()
        val decision = HotfoxAutopilotStore.consider(
            HotfoxAutopilotStore.snapshot(
                network = classify(app),
                sessionProtected = session.isProtected(),
                sessionBusy = session.isBusy(),
                vpnPermissionGranted = granted,
                entitlementUsable = entitlementUsable(),
                hasUsableTarget = runCatching { HotfoxServerSelection.firstUsableGuid() != null }.getOrDefault(false),
                autoMode = runCatching { HotfoxServerSelection.isAutoMode() }.getOrDefault(true),
                nowEpochMs = System.currentTimeMillis(),
                source = source,
            ),
        ) ?: run {
            syncPauseAlarm(app)
            return
        }
        if (decision.wantsStart) {
            HotfoxAutopilotApply.applyProfileIfCompatible(decision.profile)
        }
        CoreServiceManager.applyAutopilot(app, decision)
        syncPauseAlarm(app)
    }

    fun syncPauseAlarm(context: Context) {
        val app = context.applicationContext
        val now = System.currentTimeMillis()
        val until = HotfoxAutopilotAlarms.expiryEpochMs(HotfoxAutopilotStore.currentPause(now), now)
        val am = app.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = pausePendingIntent(app)
        if (until == null) {
            am.cancel(pi)
            pi.cancel()
            return
        }
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, until, pi)
    }

    private fun pausePendingIntent(app: Context): PendingIntent {
        val intent = Intent(app, HotfoxAutopilotPauseReceiver::class.java)
        return PendingIntent.getBroadcast(
            app,
            2808,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun onNetworkEvent(app: Context) {
        val kind = classify(app)
        val now = System.currentTimeMillis()
        if (kind == lastKind && now - lastEventAtEpochMs < 1_000L) return
        lastKind = kind
        lastEventAtEpochMs = now
        HotfoxAutopilotStore.noteNetworkChange()
        val session = VpnSessionCoordinator.currentState()
        if (session.isProtected() || session.isBusy()) return
        apply(app, HotfoxAutopilotSource.NETWORK)
    }
}

data class HotfoxNetworkIdentity(
    val transport: HotfoxTransportKind,
    val captive: Boolean,
    val opaqueId: String,
) {
    companion object {
        fun current(context: Context): HotfoxNetworkIdentity {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return HotfoxNetworkIdentity(HotfoxTransportKind.NONE, captive = false, opaqueId = "")
            val network = cm.activeNetwork
                ?: return HotfoxNetworkIdentity(HotfoxTransportKind.NONE, captive = false, opaqueId = "")
            val caps = cm.getNetworkCapabilities(network)
                ?: return HotfoxNetworkIdentity(HotfoxTransportKind.NONE, captive = false, opaqueId = "")
            val captive = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)
            val transport = when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> HotfoxTransportKind.WIFI
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> HotfoxTransportKind.CELLULAR
                else -> HotfoxTransportKind.NONE
            }
            val ssid = ssidHint(context, caps)
            val opaque = if (ssid.isNotBlank()) HotfoxTrustedNetworks.opaqueId(ssid) else ""
            return HotfoxNetworkIdentity(transport, captive, opaque)
        }

        fun usableSsid(raw: String): Boolean {
            val ssid = raw.trim().trim('"')
            return ssid.isNotBlank() && ssid != "<unknown ssid>" && ssid != "0x" && !ssid.equals("unknown", ignoreCase = true)
        }

        private fun ssidHint(context: Context, caps: NetworkCapabilities): String {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val info = caps.transportInfo as? android.net.wifi.WifiInfo
                val ssid = info?.ssid.orEmpty()
                if (usableSsid(ssid)) return ssid.trim().trim('"')
            }
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return ""
            val ssid = runCatching { wm.connectionInfo?.ssid.orEmpty() }.getOrDefault("")
            return if (usableSsid(ssid)) ssid.trim().trim('"') else ""
        }
    }
}
