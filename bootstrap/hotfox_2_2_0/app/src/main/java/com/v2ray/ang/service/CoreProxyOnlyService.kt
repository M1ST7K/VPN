package com.v2ray.ang.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.v2ray.ang.AppConfig
import com.v2ray.ang.contracts.ServiceControl
import com.v2ray.ang.core.CoreServiceManager
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.MyContextWrapper
import com.v2ray.ang.vpn.DuplicateStartDisposition
import com.v2ray.ang.vpn.VpnReadiness
import com.v2ray.ang.vpn.VpnSessionCoordinator
import com.v2ray.ang.vpn.VpnSessionState
import java.lang.ref.SoftReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class CoreProxyOnlyService : Service(), ServiceControl {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        LogUtil.i(AppConfig.TAG, "StartCore-Proxy: Service created")
        CoreServiceManager.serviceControl = SoftReference(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        LogUtil.i(AppConfig.TAG, "StartCore-Proxy: Service command received")
        when (VpnSessionCoordinator.duplicateStartDisposition()) {
            DuplicateStartDisposition.REPUBLISH -> {
                if (VpnSessionCoordinator.currentState() == VpnSessionState.PROXY_ONLY) {
                    CoreServiceManager.notifyTunnelReady(this)
                }
                return START_STICKY
            }
            DuplicateStartDisposition.IGNORE -> return START_STICKY
            DuplicateStartDisposition.START -> Unit
        }

        val attempt = VpnSessionCoordinator.beginAttempt()
        if (attempt == 0L) {
            LogUtil.w(AppConfig.TAG, "StartCore-Proxy: beginAttempt refused")
            return START_STICKY
        }
        VpnSessionCoordinator.setState(attempt, VpnSessionState.STARTING_CORE)
        if (!CoreServiceManager.startCoreLoop(null, notifyUiWhenReady = false)) {
            LogUtil.e(AppConfig.TAG, "StartCore-Proxy: Failed to start core loop")
            VpnSessionCoordinator.markError(attempt, "HF-VPN-003", "Не удалось запустить ядро")
            stopSelf()
            return START_NOT_STICKY
        }
        serviceScope.launch {
            if (!VpnSessionCoordinator.setState(attempt, VpnSessionState.WAITING_SOCKS)) return@launch
            val socksPort = SettingsManager.getSocksPort()
            if (!VpnReadiness.waitForLocalSocks(
                    socksPort,
                    username = SettingsManager.getSocksUsername(),
                    password = SettingsManager.getSocksPassword(),
                )
            ) {
                if (!VpnSessionCoordinator.markError(attempt, "HF-VPN-004", "Локальный прокси не запустился")) {
                    return@launch
                }
                stopSelf()
                return@launch
            }
            if (!VpnSessionCoordinator.isCurrent(attempt)) return@launch
            val isolation = com.v2ray.ang.vpn.HotfoxSocksIsolation.probe(
                socksPort = socksPort,
                socksUser = SettingsManager.getSocksUsername(),
                socksPassword = SettingsManager.getSocksPassword(),
                tunPresent = false,
                hevPresent = false,
            )
            if (!isolation.socksHttps) {
                if (!VpnSessionCoordinator.markError(
                        attempt,
                        "HF-VPN-014",
                        "Xray SOCKS outbound не дал HTTPS без TUN/HEV",
                    )
                ) {
                    return@launch
                }
                stopSelf()
                return@launch
            }
            if (!VpnSessionCoordinator.markProxyOnly(attempt)) return@launch
            CoreServiceManager.notifyTunnelReady(this@CoreProxyOnlyService)
            LogUtil.i(AppConfig.TAG, "StartCore-Proxy: PROXY_ONLY attempt=$attempt")
        }
        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        CoreServiceManager.stopCoreLoop()
        super.onDestroy()
    }

    override fun getService(): Service {
        return this
    }

    override fun startService() {
        // do nothing
    }

    override fun stopService() {
        stopSelf()
    }

    override fun vpnProtect(socket: Int): Boolean {
        return true
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun attachBaseContext(newBase: Context?) {
        val context = newBase?.let {
            MyContextWrapper.wrap(newBase, SettingsManager.getLocale())
        }
        super.attachBaseContext(context)
    }
}
