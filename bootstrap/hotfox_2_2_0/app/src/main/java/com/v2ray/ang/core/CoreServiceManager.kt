package com.v2ray.ang.core

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.system.OsConstants
import androidx.core.content.ContextCompat
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.contracts.ServiceControl
import com.v2ray.ang.dto.OutboundTrafficStat
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.extension.isComplexType
import com.v2ray.ang.extension.toast
import com.v2ray.ang.extension.toastError
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.NotificationManager
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.handler.SpeedtestManager
import com.v2ray.ang.root.RootManager
import com.v2ray.ang.service.CoreProxyOnlyService
import com.v2ray.ang.service.CoreRootService
import com.v2ray.ang.service.CoreVpnService
import com.v2ray.ang.service.DialerNativeService
import com.v2ray.ang.service.DialerWebviewService
import com.v2ray.ang.service.IDialerService
import com.v2ray.ang.service.NetworkMonitor
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.ErrorMessageMapper
import com.v2ray.ang.util.MessageUtil
import com.v2ray.ang.util.Utils
import com.v2ray.ang.vpn.HotfoxAutoFailover
import com.v2ray.ang.vpn.HotfoxRoutingStore
import com.v2ray.ang.vpn.HotfoxServerSelection
import com.v2ray.ang.vpn.HotfoxXrayConfigInjector
import com.v2ray.ang.vpn.FailoverAction
import com.v2ray.ang.vpn.VpnRestartGate
import com.v2ray.ang.vpn.VpnLoopPrevention
import com.v2ray.ang.vpn.VpnReadiness
import com.v2ray.ang.vpn.VpnSessionCoordinator
import com.v2ray.ang.vpn.VpnSessionState
import com.v2ray.ang.vpn.XrayShutdownGate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.ProcessFinder
import java.lang.ref.SoftReference
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.jvm.Volatile

object CoreServiceManager {

    private val coreController: CoreController = CoreNativeManager.newCoreController(CoreCallback())
    private val mMsgReceive = ReceiveMessageHandler()
    private var currentConfig: ProfileItem? = null
    private var processFinder: XrayProcessFinder? = null
    private var browserDialer: IDialerService? = null
    private var networkMonitor: NetworkMonitor? = null

    @Volatile
    private var isReloading = false

    internal val xrayShutdownGate = XrayShutdownGate()
    private val restartSupervisor = SupervisorJob()
    private val restartScope = CoroutineScope(restartSupervisor + Dispatchers.IO)

    private val stopInProgress = AtomicBoolean(false)
    private val stopWorker = AtomicReference<Thread?>(null)
    private val lateStopWork = AtomicReference<LateStopWork?>(null)
    private val stopExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "hotfox-core-stop-exec").apply { isDaemon = false }
    }

    @Volatile
    private var hevStatsProvider: (() -> LongArray?)? = null

    /** TUN descriptor used to start Xray; retained so a network handover can reload the core in-place. */
    private var currentVpnInterface: ParcelFileDescriptor? = null

    var serviceControl: SoftReference<ServiceControl>? = null
        set(value) {
            field = value
            val service = value?.get()?.getService()
            CoreNativeManager.initCoreEnv(service)
            if (service != null && processFinder == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                processFinder = XrayProcessFinder(service)
                coreController.registerProcessFinder(processFinder)
            }
        }

    /**
     * Starts the V2Ray service from a toggle action.
     * @param context The context from which the service is started.
     * @return True if the service was started successfully, false otherwise.
     */
    fun startVServiceFromToggle(context: Context): Boolean {
        when (val resolved = HotfoxServerSelection.resolveForConnect()) {
            is HotfoxServerSelection.ResolveResult.Failure -> {
                context.toast(resolved.message)
                return false
            }
            is HotfoxServerSelection.ResolveResult.Success -> Unit
        }
        try {
            startContextService(context)
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "StartCore-Manager: ${e.message}", e)
            context.toast(e.message ?: e.javaClass.simpleName)
            return false
        }
        return true
    }

    /**
     * Starts the V2Ray service.
     * @param context The context from which the service is started.
     * @param guid The GUID of the server configuration to use (optional).
     */
    fun startVService(context: Context, guid: String? = null) {
        VpnRestartGate.invalidate()
        startVServiceBody(context, guid)
    }

    /**
     * Restart path only. Must be invoked from [VpnRestartGate.tryDispatchStart]
     * so a newer stop cannot race a start after a stale isCurrent check.
     */
    private fun startVServiceAfterAuthorizedRestart(context: Context) {
        startVServiceBody(context, guid = null)
    }

    private fun startVServiceBody(context: Context, guid: String? = null) {
        LogUtil.i(AppConfig.TAG, "StartCore-Manager: startVService from ${context::class.java.simpleName}")

        if (guid != null) {
            HotfoxServerSelection.selectManual(guid)
        } else {
            when (val resolved = HotfoxServerSelection.resolveForConnect()) {
                is HotfoxServerSelection.ResolveResult.Failure -> {
                    LogUtil.e(AppConfig.TAG, "StartCore-Manager: ${resolved.message}")
                    context.toast(resolved.message)
                    return
                }
                is HotfoxServerSelection.ResolveResult.Success -> Unit
            }
        }

        try {
            startContextService(context)
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "StartCore-Manager: ${e.message}", e)
            context.toast(e.message ?: e.javaClass.simpleName)
        }
    }

    /**
     * Stops the V2Ray service.
     * @param context The context from which the service is stopped.
     */
    fun stopVService(context: Context) {
        VpnRestartGate.invalidate()
        restartSupervisor.cancelChildren()
        //context.toast(R.string.toast_services_stop)
        MessageUtil.sendMsg2Service(context, AppConfig.MSG_STATE_STOP, "")
    }

    /**
     * Checks if the V2Ray service is running.
     * @return True if the service is running, false otherwise.
     */
    fun isRunning() = coreController.isRunning

    /**
     * Gets the name of the currently running server.
     * @return The name of the running server.
     */
    fun getRunningServerName() = currentConfig?.remarks.orEmpty()

    /**
     * Starts the context service for V2Ray.
     * Chooses between VPN service or Proxy-only service based on user settings.
     * @param context The context from which the service is started.
     * @throws IllegalStateException if the core is already running, no server is selected,
     *   server config cannot be decoded, or server configuration is invalid.
     * @throws Exception if the foreground service fails to start.
     */
    @Throws(Exception::class)
    private fun startContextService(context: Context) {
        if (coreController.isRunning) {
            LogUtil.w(AppConfig.TAG, "StartCore-Manager: Core already running")
            return
        }

        val guid = MmkvManager.getSelectServer()
            ?: run {
                LogUtil.e(AppConfig.TAG, "StartCore-Manager: No server selected")
                error(context.getString(R.string.app_tile_first_use))
            }

        val config = MmkvManager.decodeServerConfig(guid)
            ?: run {
                LogUtil.e(AppConfig.TAG, "StartCore-Manager: Failed to decode server config")
                error(context.getString(R.string.toast_config_file_invalid))
            }

        if (!config.configType.isComplexType()
            && !Utils.isValidUrl(config.server)
            && !Utils.isPureIpAddress(config.server.orEmpty())
        ) {
            LogUtil.e(AppConfig.TAG, "StartCore-Manager: Invalid server configuration")
            error(context.getString(R.string.toast_config_file_invalid))
        }

        // refresh socks port when enabled dynamic socks port
        SettingsManager.refreshRuntimeSocksPort()

//        val result = V2rayConfigUtil.getV2rayConfig(context, guid)
//        if (!result.status) error(result.errorMessage.ifBlank { "Failed to get V2Ray config" })

        if (config.insecure == true) {
            context.toastError(R.string.toast_allow_insecure_deprecated)
            context.toastError(R.string.toast_allow_insecure_deprecated)
        }

        if (MmkvManager.decodeSettingsBool(AppConfig.PREF_PROXY_SHARING)) {
            context.toast(R.string.toast_warning_pref_proxysharing_short)
        } else {
            context.toast(R.string.toast_services_start)
        }

        val isRootMode = SettingsManager.isRootMode()
        if (isRootMode && !RootManager.cachedRoot()) {
            LogUtil.e(AppConfig.TAG, "StartCore-Manager: root mode requires a prior root probe")
            error(context.getString(R.string.toast_root_required))
        }

        val intent = if (isRootMode) {
            LogUtil.i(AppConfig.TAG, "StartCore-Manager: Starting Root service")
            Intent(context.applicationContext, CoreRootService::class.java)
        } else if (SettingsManager.isVpnMode()) {
            LogUtil.i(AppConfig.TAG, "StartCore-Manager: Starting VPN service")
            Intent(context.applicationContext, CoreVpnService::class.java)
        } else {
            LogUtil.i(AppConfig.TAG, "StartCore-Manager: Starting Proxy service")
            Intent(context.applicationContext, CoreProxyOnlyService::class.java)
        }

        try {
            ContextCompat.startForegroundService(context, intent)
        } catch (e: SecurityException) {
            LogUtil.e(AppConfig.TAG, "StartCore-Manager: Missing permission to start foreground service", e)
            throw IllegalStateException(e.message ?: e.javaClass.simpleName, e)
        } catch (e: RuntimeException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                e.javaClass.name == "android.app.ForegroundServiceStartNotAllowedException"
            ) {
                LogUtil.e(AppConfig.TAG, "StartCore-Manager: Foreground service start not allowed", e)
                throw IllegalStateException(e.message ?: e.javaClass.simpleName, e)
            }
            throw e
        }
    }

    /**
     * Refer to the official documentation for [registerReceiver](https://developer.android.com/reference/androidx/core/content/ContextCompat#registerReceiver(android.content.Context,android.content.BroadcastReceiver,android.content.IntentFilter,int):
     * `registerReceiver(Context, BroadcastReceiver, IntentFilter, int)`.
     * Starts the V2Ray core service.
     */
    fun startCoreLoop(vpnInterface: ParcelFileDescriptor?, notifyUiWhenReady: Boolean = true): Boolean {
        reconcileTeardownBarrier()
        if (VpnSessionCoordinator.isTeardownActive() || isCoreStopActive()) {
            LogUtil.e(AppConfig.TAG, "StartCore-Manager: start refused; previous stop still active")
            return false
        }
        VpnSessionCoordinator.beginStart()
        try {
            if (coreController.isRunning) {
                LogUtil.w(AppConfig.TAG, "StartCore-Manager: Core already running")
                return false
            }

            val service = getService()
            if (service == null) {
                LogUtil.e(AppConfig.TAG, "StartCore-Manager: Service is null")
                return false
            }

            try {
                if (vpnInterface != null &&
                    !VpnLoopPrevention.requireBindSuccess(
                        VpnLoopPrevention.bindProcessToUnderlying(service),
                    )
                ) {
                    LogUtil.e(AppConfig.TAG, "StartCore-Manager: refusing Xray start; loop bind failed")
                    return false
                }
                doStartCoreLoop(service, vpnInterface, notifyUiWhenReady)
                return true
            } catch (e: Exception) {
                val message = e.message?.takeUnless { it.isBlank() } ?: e.javaClass.simpleName
                LogUtil.e(AppConfig.TAG, "StartCore-Manager: $message", e)
                MessageUtil.sendMsg2UI(service, AppConfig.MSG_STATE_START_FAILURE, ErrorMessageMapper.toRussian(message))
                NotificationManager.cancelNotification()
                return false
            }
        } finally {
            VpnSessionCoordinator.endStart()
        }
    }

    @Throws(Exception::class)
    private fun doStartCoreLoop(
        service: Service,
        vpnInterface: ParcelFileDescriptor?,
        notifyUiWhenReady: Boolean,
    ) {
        val mFilter = IntentFilter(AppConfig.BROADCAST_ACTION_SERVICE)
        mFilter.addAction(Intent.ACTION_SCREEN_ON)
        mFilter.addAction(Intent.ACTION_SCREEN_OFF)
        mFilter.addAction(Intent.ACTION_USER_PRESENT)
        ContextCompat.registerReceiver(service, mMsgReceive, mFilter, Utils.receiverFlags())

        currentVpnInterface = vpnInterface
        launchCore(service, vpnInterface, notifyUiWhenReady = notifyUiWhenReady)
    }

    fun setHevStatsProvider(provider: (() -> LongArray?)?) {
        hevStatsProvider = provider
    }

    /** Starts watching the physical uplink only after a verified VPN CONNECTED. */
    fun startNetworkMonitorIfNeeded() {
        val service = getService() ?: return
        startNetworkMonitor(service)
    }

    /** Starts Xray against the supplied TUN descriptor without recreating Android's VPN interface. */
    @Throws(Exception::class)
    private fun launchCore(
        service: Service,
        vpnInterface: ParcelFileDescriptor?,
        notifyUiWhenReady: Boolean,
        isReload: Boolean = false,
    ) {
        val guid = MmkvManager.getSelectServer() ?: error("No server selected")
        val config = MmkvManager.decodeServerConfig(guid) ?: error("Failed to decode server config")

        LogUtil.i(AppConfig.TAG, "StartCore-Manager: Starting core loop for ${config.remarks}")
        val result = CoreConfigManager.getV2rayConfig(service, guid)
        LogUtil.d(AppConfig.TAG, "Core configuration prepared")
        if (!result.status) {
            error(result.errorMessage.ifBlank { "Failed to get V2Ray config" })
        }
        val routingSnapshot = HotfoxRoutingStore.load()
        val coreConfigJson = HotfoxXrayConfigInjector.apply(result.content, routingSnapshot)

        currentConfig = config
        var tunFd = vpnInterface?.fd ?: 0
        val dialerAddr = if (currentConfig?.browserDialerMode.isNullOrEmpty()) {
            ""
        } else {
            "127.0.0.1:${Utils.findRandomFreePort()}"
        }
        if (SettingsManager.isUsingHevTun()) {
            tunFd = 0
        }

        NotificationManager.showNotification(currentConfig)
        CoreNativeManager.reconcileBrowserDialer(dialerAddr)
        coreController.startLoop(coreConfigJson, tunFd)

        if (!coreController.isRunning) {
            error("Core failed to start")
        }
        xrayShutdownGate.onCoreLaunched()

        if (browserDialer != null) {
            browserDialer!!.stop()
            browserDialer = null
        }
        if (config.browserDialerMode == "OkHttp") {
            browserDialer = DialerNativeService()
            browserDialer!!.start(service, dialerAddr)
        } else if (config.browserDialerMode == "WebView") {
            browserDialer = DialerWebviewService()
            browserDialer!!.start(service, dialerAddr)
        }

        if (notifyUiWhenReady && !isReload) {
            // Proxy-only / root callers may announce their own non-VPN running state.
            // The Android VPN path must never use this until markConnected(pathVerified=true).
            notifyTunnelReady(service)
        } else {
            NotificationManager.startSpeedNotification()
        }
        LogUtil.i(
            AppConfig.TAG,
            if (isReload) "StartCore-Manager: Core reloaded successfully" else "StartCore-Manager: Core started successfully",
        )
    }

    /** Announces a usable connection only after the Android TUN path is ready. */
    fun notifyTunnelReady(service: Service? = getService()) {
        service ?: return
        MessageUtil.sendMsg2UI(service, AppConfig.MSG_STATE_START_SUCCESS, "")
        NotificationManager.startSpeedNotification()
    }

    /**
     * Stops the V2Ray core service.
     * Unregisters broadcast receivers, stops notifications, and shuts down plugins.
     * @return True if the core was stopped successfully, false otherwise.
     */
    fun stopCoreLoop(preStop: (() -> Unit)? = null): Boolean {
        if (isMainThread()) {
            VpnSessionCoordinator.setTeardownActive(true)
            val attempt = VpnSessionCoordinator.currentAttempt()
            if (VpnSessionCoordinator.currentState() != VpnSessionState.ERROR) {
                VpnSessionCoordinator.setState(attempt, VpnSessionState.DISCONNECTING)
            }
            stopExecutor.execute {
                stopCoreLoopBlocking(preStop)
            }
            return false
        }
        return stopCoreLoopBlocking(preStop)
    }

    private fun stopCoreLoopBlocking(preStop: (() -> Unit)? = null): Boolean {
        val ticket = VpnSessionCoordinator.beginStop()
        val epoch = ticket.epoch
        val attempt = VpnSessionCoordinator.currentAttempt()
        try {
            if (!ticket.owned) {
                LogUtil.i(AppConfig.TAG, "StartCore-Manager: joining in-flight stop epoch=$epoch")
                stopWorker.get()?.join(8_000)
                return VpnSessionCoordinator.lastStopSucceeded() && !VpnSessionCoordinator.isTeardownActive()
            }
            if (!stopInProgress.compareAndSet(false, true) && isCoreStopActive()) {
                LogUtil.i(AppConfig.TAG, "StartCore-Manager: stop already in progress epoch=$epoch")
                return false
            }
            val service = getService()
            val runningGen = xrayShutdownGate.currentGeneration()
            if (runningGen != 0L) {
                xrayShutdownGate.expectShutdownOf(runningGen)
            }
            try {
                runCatching { preStop?.invoke() }
                networkMonitor?.unregister()
                networkMonitor = null
                currentVpnInterface = null
                hevStatsProvider = null

                val stopped = if (coreController.isRunning) awaitCoreStop(epoch, attempt) else true
                if (!stopped) {
                    LogUtil.e(AppConfig.TAG, "StartCore-Manager: core stop did not complete")
                    VpnSessionCoordinator.completeStopOutcome(epoch, attempt, false)
                    if (service != null) {
                        MessageUtil.sendMsg2UI(
                            service,
                            AppConfig.MSG_STATE_START_FAILURE,
                            "Ядро не остановилось",
                        )
                    }
                    return false
                }

                finishCoreStopResources(service)
                if (!VpnSessionCoordinator.completeStopOutcome(epoch, attempt, true)) {
                    LogUtil.w(AppConfig.TAG, "StartCore-Manager: stop finalizer ignored; newer attempt owns the session")
                }
                return true
            } finally {
                if (!isCoreStopActive() && VpnSessionCoordinator.lastStopSucceeded()) {
                    stopInProgress.set(false)
                    xrayShutdownGate.clearExpected()
                }
            }
        } finally {
            VpnSessionCoordinator.endStop()
        }
    }

    private fun finishCoreStopResources(service: Service?) {
        CoreNativeManager.reconcileBrowserDialer("")
        if (browserDialer != null) {
            browserDialer!!.stop()
            browserDialer = null
        }
        if (service != null) {
            MessageUtil.sendMsg2UI(service, AppConfig.MSG_STATE_STOP_SUCCESS, "")
            NotificationManager.cancelNotification()
            try {
                service.unregisterReceiver(mMsgReceive)
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "StartCore-Manager: Failed to unregister receiver", e)
            }
        }
    }

    fun isCoreStopActive(): Boolean {
        val worker = stopWorker.get() ?: return false
        if (worker.isAlive) return true
        stopWorker.compareAndSet(worker, null)
        return false
    }

    private fun reconcileTeardownBarrier() {
        if (isCoreStopActive()) return
        if (stopInProgress.get() && stopWorker.get() == null && VpnSessionCoordinator.lastStopSucceeded()) {
            stopInProgress.set(false)
        }
    }

    private fun isMainThread(): Boolean =
        runCatching { Looper.getMainLooper() == Looper.myLooper() }.getOrDefault(false)

    /**
     * Awaits [CoreController.stopLoop] on a dedicated non-daemon thread.
     * Returns false on timeout or exception; the worker remains a start barrier until it dies.
     */
    private fun awaitCoreStop(epoch: Long, attempt: Long): Boolean {
        val done = CountDownLatch(1)
        val succeeded = AtomicBoolean(false)
        val timedOut = AtomicBoolean(false)
        val worker = Thread({
            try {
                coreController.stopLoop()
                succeeded.set(true)
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "StartCore-Manager: Failed to stop V2Ray loop", e)
                succeeded.set(false)
            } finally {
                done.countDown()
                val ok = succeeded.get()
                stopWorker.compareAndSet(Thread.currentThread(), null)
                if (ok && timedOut.get()) {
                    val late = lateStopWork.get()
                    if (late != null && late.epoch == epoch && lateStopWork.compareAndSet(late, null)) {
                        runCatching { finishCoreStopResources(getService()) }
                        VpnSessionCoordinator.completeLateStopSuccess(epoch)
                        stopInProgress.set(false)
                        xrayShutdownGate.clearExpected()
                    }
                }
            }
        }, "hotfox-core-stop")
        worker.isDaemon = false
        stopWorker.set(worker)
        worker.start()
        val finished = try {
            done.await(8, TimeUnit.SECONDS)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }
        if (finished || succeeded.get()) {
            worker.join(1_000)
            return succeeded.get()
        }
        timedOut.set(true)
        lateStopWork.set(LateStopWork(epoch, attempt))
        if (succeeded.get()) {
            lateStopWork.set(null)
            worker.join(1_000)
            return true
        }
        LogUtil.e(AppConfig.TAG, "StartCore-Manager: core stop timed out")
        return false
    }

    /**
     * Generation-owned restart used by user restart and AUTO failover.
     * Does not start VpnService until [VpnRestartGate.tryDispatchStart] wins.
     */
    fun scheduleAuthorizedRestart(context: Context) {
        val request = VpnRestartGate.nextRequest()
        val app = context.applicationContext
        restartScope.launch {
            VpnSessionCoordinator.awaitIdle()
            val started = VpnRestartGate.tryDispatchStart(request) {
                startVServiceAfterAuthorizedRestart(app)
            }
            if (!started) {
                LogUtil.i(AppConfig.TAG, "StartCore-Manager: restart cancelled request=$request")
            }
        }
    }

    fun requestConnectedReload(): Boolean = reloadCore()

    private fun startNetworkMonitor(service: Service) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        if (networkMonitor != null) return
        val connectivity = service.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        networkMonitor = NetworkMonitor(
            connectivity = connectivity,
            onUnderlyingNetworksChanged = { networks ->
                serviceControl?.get()?.setUnderlyingNetworks(networks)
            },
            onHandover = { reloadCore() },
        ).also { it.register() }
    }

    /** Restarts Xray with the same TUN FD after Wi-Fi/cellular handover. */
    private fun reloadCore(): Boolean {
        if (!VpnSessionCoordinator.tryBeginReload()) {
            LogUtil.i(AppConfig.TAG, "StartCore-Manager: skip reload; startup/stop in progress or not CONNECTED")
            return false
        }
        val service = getService()
        if (service == null || !isRunning()) {
            VpnSessionCoordinator.endReload()
            return false
        }

        val attempt = VpnSessionCoordinator.currentAttempt()
        return try {
            val tunInterface = currentVpnInterface
            isReloading = true
            if (!VpnSessionCoordinator.markReconnecting(attempt)) return false
            HotfoxServerSelection.invalidateForNetworkChange()
            when (val resolved = HotfoxServerSelection.resolveForHandover()) {
                is HotfoxServerSelection.ResolveResult.Failure -> {
                    LogUtil.e(AppConfig.TAG, "StartCore-Manager: handover server resolve failed: ${resolved.message}")
                    if (!VpnSessionCoordinator.markError(attempt, "HF-VPN-011", resolved.message)) {
                        return false
                    }
                    MessageUtil.sendMsg2UI(service, AppConfig.MSG_STATE_START_FAILURE, resolved.message)
                    runCatching { serviceControl?.get()?.stopService() }
                    return false
                }
                is HotfoxServerSelection.ResolveResult.Success -> {
                    LogUtil.i(
                        AppConfig.TAG,
                        "StartCore-Manager: handover resolved auto=${resolved.resolvedFromAuto} attempt=$attempt",
                    )
                }
            }
            if (!VpnSessionCoordinator.isCurrent(attempt)) return false
            LogUtil.i(AppConfig.TAG, "StartCore-Manager: Core reload start attempt=$attempt")
            val ctx = service.applicationContext
            if (!VpnLoopPrevention.requireBindSuccess(VpnLoopPrevention.bindProcessToUnderlying(ctx))) {
                return failHandover(service, attempt, "HF-VPN-012", "Не удалось привязать процесс к внешней сети")
            }
            val oldGen = xrayShutdownGate.currentGeneration()
            if (oldGen != 0L) {
                xrayShutdownGate.expectShutdownOf(oldGen)
                coreController.stopLoop()
                if (!xrayShutdownGate.drain(3_000)) {
                    LogUtil.e(
                        AppConfig.TAG,
                        "StartCore-Manager: old-core shutdown did not drain; refusing replacement gen=$oldGen",
                    )
                    return failHandover(
                        service,
                        attempt,
                        "HF-VPN-013",
                        "Не удалось безопасно остановить предыдущее ядро",
                    )
                }
            } else {
                coreController.stopLoop()
            }
            if (!xrayShutdownGate.mayLaunchReplacement()) {
                return failHandover(
                    service,
                    attempt,
                    "HF-VPN-013",
                    "Не удалось безопасно остановить предыдущее ядро",
                )
            }
            launchCore(
                service = service,
                vpnInterface = tunInterface,
                notifyUiWhenReady = false,
                isReload = true,
            )
            val socksPort = SettingsManager.getSocksPort()
            val socksUser = SettingsManager.getSocksUsername()
            val socksPassword = SettingsManager.getSocksPassword()
            if (!VpnReadiness.waitForLocalSocksBlocking(socksPort, username = socksUser, password = socksPassword)) {
                if (!VpnSessionCoordinator.markError(attempt, "HF-VPN-004", "Локальный прокси не восстановился")) {
                    return false
                }
                MessageUtil.sendMsg2UI(service, AppConfig.MSG_STATE_START_FAILURE, "Локальный прокси не восстановился")
                runCatching { serviceControl?.get()?.stopService() }
                return false
            }
            if (!VpnSessionCoordinator.isCurrent(attempt)) return false
            val usingHev = SettingsManager.isUsingHevTun()
            val svc = getService()
            val path = VpnReadiness.verifyConfiguredPathBlocking(
                socksPort = socksPort,
                socksUser = socksUser,
                socksPassword = socksPassword,
                tunEstablished = tunInterface != null,
                hevStatsProvider = if (usingHev) hevStatsProvider else null,
                tunInjector = {
                    svc?.let { VpnReadiness.injectThroughVpn(it) } ?: false
                },
                xrayTunSnapshot = if (!usingHev) ({ snapshotOutboundCounters() }) else null,
            )
            if (!VpnSessionCoordinator.recordPath(attempt, path)) return false
            if (!path.verified) {
                if (!VpnSessionCoordinator.markError(attempt, "HF-VPN-006", "Путь VPN не подтверждён (${path.reason})")) {
                    return false
                }
                MessageUtil.sendMsg2UI(service, AppConfig.MSG_STATE_START_FAILURE, "Путь VPN не подтверждён")
                runCatching { serviceControl?.get()?.stopService() }
                return false
            }
            if (!VpnSessionCoordinator.markConnected(attempt, pathVerified = true)) {
                LogUtil.w(AppConfig.TAG, "StartCore-Manager: stale reload completion attempt=$attempt")
                return false
            }
            HotfoxAutoFailover.reset()
            MmkvManager.getSelectServer()?.let { HotfoxServerSelection.rememberLastGoodAuto(it) }
            true
        } catch (e: Exception) {
            val message = e.message?.takeUnless { it.isBlank() } ?: e.javaClass.simpleName
            LogUtil.e(AppConfig.TAG, "StartCore-Manager: Failed to reload core: $message", e)
            VpnSessionCoordinator.markError(attempt, "HF-VPN-003", "Не удалось переподключить ядро")
            MessageUtil.sendMsg2UI(service, AppConfig.MSG_STATE_START_FAILURE, ErrorMessageMapper.toRussian(message))
            runCatching { serviceControl?.get()?.stopService() }
            false
        } finally {
            isReloading = false
            VpnSessionCoordinator.endReload()
        }
    }

    private fun failHandover(service: Service, attempt: Long, code: String, message: String): Boolean {
        val decision = HotfoxAutoFailover.considerLive(code)
        if (decision.action == FailoverAction.SWITCH) {
            LogUtil.i(AppConfig.TAG, "StartCore-Manager: AUTO handover failover ${decision.reason} next=${decision.guid}")
            if (VpnSessionCoordinator.markReconnecting(attempt)) {
                scheduleAuthorizedRestart(service.applicationContext)
            }
            runCatching { serviceControl?.get()?.stopService() }
            return false
        }
        if (!VpnSessionCoordinator.markError(attempt, code, message)) {
            return false
        }
        MessageUtil.sendMsg2UI(service, AppConfig.MSG_STATE_START_FAILURE, message)
        runCatching { serviceControl?.get()?.stopService() }
        return false
    }

    fun snapshotOutboundCounters(): LongArray {
        var up = 0L
        var down = 0L
        queryAllOutboundTrafficStats().forEach { stat ->
            when (stat.direction.lowercase()) {
                "uplink", "up", "tx" -> up += stat.value
                "downlink", "down", "rx" -> down += stat.value
                else -> {
                    up += stat.value
                }
            }
        }
        return longArrayOf(0L, up, 0L, down)
    }

    /**
     * Queries and resets all outbound traffic counters in one core call.
     * Go side format: tag,direction,value;tag,direction,value;
     */
    fun queryAllOutboundTrafficStats(): List<OutboundTrafficStat> {
        val payload = coreController.queryAllOutboundTrafficStats()

        val result = ArrayList<OutboundTrafficStat>()

        payload.split(';').forEach { entry ->
            if (entry.isBlank()) return@forEach

            val parts = entry.split(',', limit = 3)
            if (parts.size != 3) return@forEach

            val value = parts[2].toLongOrNull() ?: return@forEach

            result.add(
                OutboundTrafficStat(
                    tag = parts[0],
                    direction = parts[1],
                    value = value,
                )
            )
        }
//        LogUtil.d(AppConfig.TAG, "Queried outbound traffic stats: $result")
        return result
    }

    /**
     * Measures the connection delay for the current V2Ray configuration.
     * Tests with primary URL first, then falls back to alternative URL if needed.
     * Also fetches remote IP information if the delay test was successful.
     */
    private fun measureV2rayDelay() {
        if (coreController.isRunning == false) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val service = getService() ?: return@launch
            var time = -1L
            var errorStr = ""

            try {
                time = coreController.measureDelay(SettingsManager.getDelayTestUrl())
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "StartCore-Manager: Failed to measure delay", e)
                errorStr = e.message?.substringAfter("\":") ?: "empty message"
            }
            if (time == -1L) {
                try {
                    time = coreController.measureDelay(SettingsManager.getDelayTestUrl(true))
                } catch (e: Exception) {
                    LogUtil.e(AppConfig.TAG, "StartCore-Manager: Failed to measure delay", e)
                    errorStr = e.message?.substringAfter("\":") ?: "empty message"
                }
            }

            val result = if (time >= 0) {
                service.getString(R.string.connection_test_available, time)
            } else {
                service.getString(R.string.connection_test_error, errorStr)
            }
            MessageUtil.sendMsg2UI(service, AppConfig.MSG_MEASURE_DELAY_SUCCESS, result)

            // Only fetch IP info if the delay test was successful
            if (time >= 0) {
                SpeedtestManager.getRemoteIPInfo()?.let { ip ->
                    MessageUtil.sendMsg2UI(service, AppConfig.MSG_MEASURE_DELAY_SUCCESS, "$result\n$ip")
                }
            }
        }
    }

    /**
     * Gets the current service instance.
     * @return The current service instance, or null if not available.
     */
    private fun getService(): Service? {
        return serviceControl?.get()?.getService()
    }

    /**
     * Core callback handler implementation for handling V2Ray core events.
     * Handles startup, shutdown, socket protection, and status emission.
     */
    private class CoreCallback : CoreCallbackHandler {
        /**
         * Called when V2Ray core starts up.
         * @return 0 for success, any other value for failure.
         */
        override fun startup(): Long {
            return 0
        }

        /**
         * Called when V2Ray core shuts down.
         * @return 0 for success, any other value for failure.
         */
        override fun shutdown(): Long {
            val generation = xrayShutdownGate.currentGeneration()
            if (xrayShutdownGate.onCoreShutdown(generation) == XrayShutdownGate.Disposition.EXPECTED) {
                return 0
            }
            if (stopInProgress.get()) {
                return 0
            }
            val serviceControl = serviceControl?.get() ?: return -1
            return try {
                serviceControl.stopService()
                0
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "StartCore-Manager: Failed to stop service", e)
                -1
            }
        }

        /**
         * Called when V2Ray core emits status information.
         * @param l Status code.
         * @param s Status message.
         * @return Always returns 0.
         */
        override fun onEmitStatus(l: Long, s: String?): Long {
            return 0
        }
    }

    /**
     * Process finder implementation for Xray core.
     * Uses ConnectivityManager to find the owning UID of a connection based on network parameters.
     */
    private class XrayProcessFinder(context: Context) : ProcessFinder {
        private val cm: ConnectivityManager? = context.getSystemService(ConnectivityManager::class.java)

        override fun findProcessByConnection(network: String, srcIP: String, srcPort: Long, destIP: String, destPort: Long): Long {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return -1L
            if (cm == null) return -1L
            val proto = when (network) {
                "tcp" -> OsConstants.IPPROTO_TCP
                "udp" -> OsConstants.IPPROTO_UDP
                else -> return -1L
            }

            if (destIP.isBlank() || destPort == 0L) {
                LogUtil.d(AppConfig.TAG, "ProcessFinder: Find $network connection from $srcIP:$srcPort to :$destPort, (no dest)")
                return -1L
            }

            return try {
                val uid = cm.getConnectionOwnerUid(
                    proto,
                    InetSocketAddress(srcIP, srcPort.toInt()),
                    InetSocketAddress(destIP, destPort.toInt())
                ).toLong()
                LogUtil.d(AppConfig.TAG, "ProcessFinder: Find $network connection from $srcIP:$srcPort to $destIP:$destPort, uid=$uid")
                //LogUtil.d(AppConfig.TAG, "ProcessFinder: Find $network connection from $srcIP:$srcPort to $destIP:$destPort, uid=$uid,${PackageUidResolver.uidToPackageName(uid.toString())}")

                uid
            } catch (_: Exception) {
                -1L
            }
        }
    }

    /**
     * Broadcast receiver for handling messages sent to the service.
     * Handles registration, service control, and screen events.
     */
    private class ReceiveMessageHandler : BroadcastReceiver() {
        /**
         * Handles received broadcast messages.
         * Processes service control messages and screen state changes.
         * @param ctx The context in which the receiver is running.
         * @param intent The intent being received.
         */
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val serviceControl = serviceControl?.get() ?: return
            when (intent?.getIntExtra("key", 0)) {
                AppConfig.MSG_REGISTER_CLIENT -> {
                    val session = VpnSessionCoordinator.currentState()
                    if (session.isProtected() || session.isNonVpnRunning()) {
                        MessageUtil.sendMsg2UI(serviceControl.getService(), AppConfig.MSG_STATE_RUNNING, "")
                    } else if (!session.isBusy()) {
                        MessageUtil.sendMsg2UI(serviceControl.getService(), AppConfig.MSG_STATE_NOT_RUNNING, "")
                    }
                }

                AppConfig.MSG_UNREGISTER_CLIENT -> {
                    // nothing to do
                }

                AppConfig.MSG_STATE_START -> {
                    // nothing to do
                }

                AppConfig.MSG_STATE_STOP -> {
                    LogUtil.i(AppConfig.TAG, "StartCore-Manager: Stop service")
                    VpnRestartGate.invalidate()
                    restartSupervisor.cancelChildren()
                    serviceControl.stopService()
                }

                AppConfig.MSG_STATE_RESTART -> {
                    LogUtil.i(AppConfig.TAG, "StartCore-Manager: Restart service")
                    val app = serviceControl.getService().applicationContext
                    serviceControl.stopService()
                    scheduleAuthorizedRestart(app)
                }

                AppConfig.MSG_MEASURE_DELAY -> {
                    measureV2rayDelay()
                }
            }

            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    LogUtil.i(AppConfig.TAG, "StartCore-Manager: Screen off")
                    NotificationManager.stopSpeedNotification()
                }

                Intent.ACTION_SCREEN_ON -> {
                    LogUtil.i(AppConfig.TAG, "StartCore-Manager: Screen on")
                    NotificationManager.startSpeedNotification()
                }
            }
        }
    }
}

private data class LateStopWork(val epoch: Long, val attempt: Long)
