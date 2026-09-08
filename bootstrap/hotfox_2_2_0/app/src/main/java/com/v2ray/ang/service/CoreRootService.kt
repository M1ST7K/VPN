package com.v2ray.ang.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.v2ray.ang.AppConfig
import com.v2ray.ang.contracts.ServiceControl
import com.v2ray.ang.core.CoreServiceManager
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.root.RootProxyManager
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.MyContextWrapper
import com.v2ray.ang.vpn.DuplicateStartDisposition
import com.v2ray.ang.vpn.VpnSessionCoordinator
import com.v2ray.ang.vpn.VpnSessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.ref.SoftReference

/**
 * Foreground service for the root (system-wide) run modes. Unlike [CoreVpnService] it
 * does not use Android VpnService — traffic is routed by iptables instead
 * (see [RootProxyManager]).
 *
 * The in-process core is started first (so its listener is up and the foreground
 * notification is posted promptly), then the root routing rules are installed off the
 * main thread. On teardown the rules are removed before the core stops, on the
 * dedicated stop worker rather than the Android lifecycle thread.
 */
class CoreRootService : Service(), ServiceControl {

    private var setupJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        LogUtil.i(AppConfig.TAG, "StartCore-Root: Service created")
        CoreServiceManager.serviceControl = SoftReference(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        LogUtil.i(AppConfig.TAG, "StartCore-Root: command received")

        when (VpnSessionCoordinator.duplicateStartDisposition()) {
            DuplicateStartDisposition.REPUBLISH -> {
                if (VpnSessionCoordinator.currentState() == VpnSessionState.ROOT_RUNNING) {
                    CoreServiceManager.notifyTunnelReady(this)
                }
                return START_STICKY
            }
            DuplicateStartDisposition.IGNORE -> return START_STICKY
            DuplicateStartDisposition.START -> Unit
        }

        val attempt = VpnSessionCoordinator.beginAttempt()
        if (attempt == 0L) {
            LogUtil.w(AppConfig.TAG, "StartCore-Root: beginAttempt refused")
            return START_STICKY
        }
        VpnSessionCoordinator.setState(attempt, VpnSessionState.STARTING_CORE)
        if (!CoreServiceManager.startCoreLoop(null, notifyUiWhenReady = false)) {
            LogUtil.e(AppConfig.TAG, "StartCore-Root: core failed to start")
            VpnSessionCoordinator.markError(attempt, "HF-VPN-003", "Не удалось запустить ядро")
            stopService()
            return START_NOT_STICKY
        }

        setupJob = CoroutineScope(Dispatchers.IO).launch {
            if (!RootProxyManager.start(this@CoreRootService)) {
                LogUtil.e(AppConfig.TAG, "StartCore-Root: failed to start root mode, stopping")
                if (!VpnSessionCoordinator.markError(attempt, "HF-VPN-007", "Не удалось установить root-маршрутизацию")) {
                    return@launch
                }
                stopService()
                return@launch
            }
            if (!VpnSessionCoordinator.isCurrent(attempt)) return@launch
            if (!VpnSessionCoordinator.markRootRunning(attempt)) return@launch
            CoreServiceManager.notifyTunnelReady(this@CoreRootService)
            LogUtil.i(AppConfig.TAG, "StartCore-Root: ROOT_RUNNING attempt=$attempt")
        }

        return START_STICKY
    }

    override fun onDestroy() {
        val app = applicationContext
        val inFlight = setupJob
        CoreServiceManager.stopCoreLoop(
            preStop = {
                runBlocking { inFlight?.cancelAndJoin() }
                RootProxyManager.stop(app)
            },
        )
        super.onDestroy()
    }

    override fun getService(): Service = this

    override fun startService() {
        // do nothing
    }

    override fun stopService() {
        stopSelf()
    }

    override fun vpnProtect(socket: Int): Boolean = true

    override fun onBind(intent: Intent?): IBinder? = null

    override fun attachBaseContext(newBase: Context?) {
        val context = newBase?.let {
            MyContextWrapper.wrap(newBase, SettingsManager.getLocale())
        }
        super.attachBaseContext(context)
    }
}
