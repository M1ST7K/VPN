package com.v2ray.ang.vpn

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.v2ray.ang.AppConfig
import com.v2ray.ang.util.LogUtil

/**
 * This libv2ray build has no CoreCallback.protect(). Loop prevention therefore
 * binds the HotFox process to the physical uplink so Xray outbounds never
 * re-enter the TUN. Probe sockets must use [android.net.Network.socketFactory]
 * / bindSocket on TRANSPORT_VPN so they still traverse TUN → HEV → SOCKS.
 */
object VpnLoopPrevention {
    fun bindProcessToUnderlying(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val underlying = findUnderlying(cm) ?: run {
            LogUtil.w(AppConfig.TAG, "VpnLoopPrevention: no underlying internet network")
            return false
        }
        return runCatching {
            cm.bindProcessToNetwork(underlying)
            LogUtil.i(AppConfig.TAG, "VpnLoopPrevention: process bound to underlying network")
            true
        }.getOrElse { error ->
            LogUtil.w(AppConfig.TAG, "VpnLoopPrevention: bindProcessToNetwork failed: ${error.javaClass.simpleName}")
            false
        }
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
