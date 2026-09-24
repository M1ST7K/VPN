package com.v2ray.ang.vpn

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.v2ray.ang.AppConfig
import com.v2ray.ang.util.LogUtil

/**
 * Loop prevention is defense-in-depth:
 * 1. [HotfoxTunSelfExclusion] keeps the HotFox UID off TUN so Go/Xray sockets
 *    cannot recurse (AndroidLibXrayLite has no CoreCallback.protect()).
 * 2. This binder pins libc sockets to the physical uplink.
 * Probe sockets must still use [android.net.Network.bindSocket] on
 * TRANSPORT_VPN so they traverse TUN → HEV → SOCKS.
 *
 * [android.net.ConnectivityManager.bindProcessToNetwork] returning false is
 * a failure. Exceptions and a missing/stale Network are failures.
 */
object VpnLoopPrevention {
    fun interpretBindAttempt(apiReturned: Boolean?, thrown: Throwable?): Boolean {
        if (thrown != null) return false
        return apiReturned == true
    }

    fun bindProcessToUnderlying(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: run {
                VpnProtectEvidence.recordBind(false, "none")
                return false
            }
        val underlying = findUnderlying(cm) ?: run {
            LogUtil.w(AppConfig.TAG, "VpnLoopPrevention: no underlying internet network")
            VpnProtectEvidence.recordBind(false, "none")
            return false
        }
        if (isStaleNetwork(cm, underlying)) {
            LogUtil.w(AppConfig.TAG, "VpnLoopPrevention: underlying network is stale")
            VpnProtectEvidence.recordBind(false, "none")
            return false
        }
        val transport = transportLabel(cm, underlying)
        return runCatching {
            val apiReturned = cm.bindProcessToNetwork(underlying)
            val ok = interpretBindAttempt(apiReturned, null)
            if (ok) {
                LogUtil.i(AppConfig.TAG, "VpnLoopPrevention: process bound to underlying transport=$transport")
            } else {
                LogUtil.w(AppConfig.TAG, "VpnLoopPrevention: bindProcessToNetwork returned false transport=$transport")
            }
            VpnProtectEvidence.recordBind(ok, transport)
            ok
        }.getOrElse { error ->
            LogUtil.w(AppConfig.TAG, "VpnLoopPrevention: bindProcessToNetwork failed: ${error.javaClass.simpleName}")
            VpnProtectEvidence.recordBind(false, transport)
            false
        }
    }

    fun isStaleNetwork(cm: ConnectivityManager, network: Network): Boolean {
        val caps = cm.getNetworkCapabilities(network) ?: return true
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return true
        return !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun transportLabel(cm: ConnectivityManager, network: Network): String {
        val caps = cm.getNetworkCapabilities(network) ?: return "OTHER"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
            else -> "OTHER"
        }
    }

    /**
     * Binding failure is fail-closed. False from the platform API is failure.
     * Self-UID exclusion is the primary Go/Xray escape; this bind is
     * defense-in-depth for libc sockets.
     */
    fun requireBindSuccess(bound: Boolean): Boolean {
        if (bound) return true
        LogUtil.e(AppConfig.TAG, "VpnLoopPrevention: bind required; refusing Xray start")
        return false
    }

    fun unbindProcess(context: Context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return
        runCatching { cm.bindProcessToNetwork(null) }
    }

    fun findUnderlying(cm: ConnectivityManager): Network? {
        val networks = runCatching { cm.allNetworks }.getOrDefault(emptyArray())
        for (network in networks) {
            val caps = cm.getNetworkCapabilities(network) ?: continue
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) continue
            if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return network
        }
        return null
    }
}
