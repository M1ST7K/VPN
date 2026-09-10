package com.v2ray.ang.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Network
import android.net.ProxyInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import com.v2ray.ang.AppConfig
import com.v2ray.ang.AppConfig.LOOPBACK
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.R
import com.v2ray.ang.contracts.ServiceControl
import com.v2ray.ang.contracts.Tun2SocksControl
import com.v2ray.ang.core.CoreServiceManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.NotificationManager
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.root.RootLanSharing
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.MessageUtil
import com.v2ray.ang.util.MyContextWrapper
import com.v2ray.ang.util.Utils
import com.v2ray.ang.vpn.DuplicateStartDisposition
import com.v2ray.ang.vpn.FailoverAction
import com.v2ray.ang.vpn.HotfoxAutoFailover
import com.v2ray.ang.vpn.HotfoxShadowFailover
import com.v2ray.ang.vpn.HotfoxShadowStore
import com.v2ray.ang.vpn.HotfoxSelfHeal
import com.v2ray.ang.vpn.HotfoxRoutingDataPlane
import com.v2ray.ang.vpn.HotfoxRoutingStore
import com.v2ray.ang.vpn.PerAppVpnPlan
import com.v2ray.ang.vpn.HotfoxServerSelection
import com.v2ray.ang.vpn.VpnConnectionStage
import com.v2ray.ang.vpn.VpnLoopPrevention
import com.v2ray.ang.vpn.VpnReadiness
import com.v2ray.ang.vpn.VpnSessionCoordinator
import com.v2ray.ang.vpn.VpnSessionState
import java.lang.ref.SoftReference
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@SuppressLint("VpnServicePolicy")
class CoreVpnService : VpnService(), ServiceControl {
    private lateinit var mInterface: ParcelFileDescriptor
    private var isRunning = false
    private val health by lazy { HotfoxHealthMonitor(this) }
    private var tun2SocksService: Tun2SocksControl? = null
    private val isStartingLock = AtomicBoolean(false)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var trafficJob: Job? = null
    private var pipelineJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        LogUtil.i(AppConfig.TAG, "StartCore-VPN: Service created")
        CoreServiceManager.serviceControl = SoftReference(this)
    }

    override fun onRevoke() {
        LogUtil.w(AppConfig.TAG, "StartCore-VPN: Permission revoked")
        stopAllService()
    }

//    override fun onLowMemory() {
//        stopV2Ray()
//        super.onLowMemory()
//    }

    override fun onDestroy() {
        LogUtil.i(AppConfig.TAG, "StartCore-VPN: Service destroyed")
        if (isRunning) {
            stopAllService()
        }
        unlockStart()
        pipelineJob?.cancel()
        trafficJob?.cancel()
        serviceScope.cancel()
        health.close()
        NotificationManager.cancelNotification()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        LogUtil.i(AppConfig.TAG, "StartCore-VPN: Service command received")
        NotificationManager.showNotification(null)

        when (VpnSessionCoordinator.duplicateStartDisposition()) {
            DuplicateStartDisposition.REPUBLISH -> {
                if (VpnSessionCoordinator.currentState().isProtected()) {
                    CoreServiceManager.notifyTunnelReady(this)
                }
                return START_STICKY
            }
            DuplicateStartDisposition.IGNORE -> {
                LogUtil.i(AppConfig.TAG, "StartCore-VPN: start already in progress, leaving in-flight attempt")
                return START_STICKY
            }
            DuplicateStartDisposition.START -> Unit
        }

        if (!tryLockStart()) {
            LogUtil.w(AppConfig.TAG, "StartCore-VPN: Start already in progress")
            return START_STICKY
        }

        if (VpnSessionCoordinator.isTeardownActive()) {
            LogUtil.w(AppConfig.TAG, "StartCore-VPN: start refused; teardown active")
            unlockStart()
            return START_STICKY
        }

        val attempt = VpnSessionCoordinator.beginAttempt()
        if (attempt == 0L) {
            LogUtil.w(AppConfig.TAG, "StartCore-VPN: beginAttempt refused")
            unlockStart()
            return START_STICKY
        }
        VpnSessionCoordinator.setState(attempt, VpnSessionState.ESTABLISHING_TUN)
        com.v2ray.ang.vpn.HotfoxSocketProtect.attach(attempt) { fd -> protect(fd) }
        VpnSessionCoordinator.recordStage(attempt, VpnConnectionStage.VPN_PREPARE)
        if (!setupVpnService()) {
            VpnSessionCoordinator.markError(attempt, "HF-VPN-002", "Не удалось создать VPN-интерфейс")
            unlockStart()
            stopSelf()
            return START_NOT_STICKY
        }
        VpnSessionCoordinator.recordStage(attempt, VpnConnectionStage.TUN_ESTABLISH)
        VpnSessionCoordinator.recordStage(attempt, VpnConnectionStage.LOOP_BIND)
        if (!VpnLoopPrevention.requireBindSuccess(VpnLoopPrevention.bindProcessToUnderlying(this))) {
            failTunnelStart(attempt, "HF-VPN-012", "Не удалось привязать процесс к внешней сети")
            return START_NOT_STICKY
        }
        startService()
        return START_STICKY
    }

    override fun getService(): Service {
        return this
    }

    override fun startService() {
        if (!::mInterface.isInitialized) {
            LogUtil.e(AppConfig.TAG, "StartCore-VPN: Interface not initialized")
            return
        }
        SettingsManager.initAssets(this, assets)
        val previous = pipelineJob
        pipelineJob = serviceScope.launch {
            previous?.cancelAndJoin()
            startTunnelPipeline()
        }
    }

    private suspend fun startTunnelPipeline() {
        val attempt = VpnSessionCoordinator.currentAttempt()
        if (!VpnSessionCoordinator.setState(attempt, VpnSessionState.STARTING_CORE)) return
        VpnSessionCoordinator.recordStage(attempt, VpnConnectionStage.XRAY_START)
        LogUtil.i(AppConfig.TAG, "StartCore-VPN: STARTING_CORE attempt=$attempt")
        if (!pipelineStillCurrent(attempt)) return
        // Do not report "connected" before the complete TUN path is usable.
        if (!CoreServiceManager.startCoreLoop(mInterface, notifyUiWhenReady = false)) {
            LogUtil.e(AppConfig.TAG, "StartCore-VPN: Failed to start core loop")
            failTunnelStart(attempt, "HF-VPN-003", "Не удалось запустить ядро")
            return
        }
        if (!pipelineStillCurrent(attempt)) return

        val socksPort = SettingsManager.getSocksPort()
        val socksUser = SettingsManager.getSocksUsername()
        val socksPassword = SettingsManager.getSocksPassword()
        val usingHev = SettingsManager.isUsingHevTun()

        if (!VpnSessionCoordinator.setState(attempt, VpnSessionState.WAITING_SOCKS)) return
        VpnSessionCoordinator.recordStage(attempt, VpnConnectionStage.SOCKS)
        if (!VpnReadiness.waitForLocalSocks(socksPort, username = socksUser, password = socksPassword)) {
            failTunnelStart(attempt, "HF-VPN-004", "Локальный прокси не запустился")
            return
        }
        if (!pipelineStillCurrent(attempt)) return

        VpnSessionCoordinator.recordStage(attempt, VpnConnectionStage.SOCKS_OUTBOUND)
        val isolation = com.v2ray.ang.vpn.HotfoxSocksIsolation.probe(
            socksPort = socksPort,
            socksUser = socksUser,
            socksPassword = socksPassword,
            httpPort = SettingsManager.getHttpPort(),
            tunPresent = true,
            hevPresent = false,
        )
        if (!isolation.socksHttps) {
            failTunnelStart(attempt, "HF-VPN-014", "Xray SOCKS outbound не дал HTTPS без HEV")
            return
        }
        if (!pipelineStillCurrent(attempt)) return

        if (usingHev) {
            if (!VpnSessionCoordinator.setState(attempt, VpnSessionState.STARTING_HEV)) return
            VpnSessionCoordinator.recordStage(attempt, VpnConnectionStage.HEV)
            if (!runTun2socks()) {
                failTunnelStart(attempt, "HF-VPN-005", "Не удалось запустить сетевой туннель")
                return
            }
            if (!pipelineStillCurrent(attempt)) return
            CoreServiceManager.setHevStatsProvider { tun2SocksService?.getStats() }
        }

        if (!VpnSessionCoordinator.setState(attempt, VpnSessionState.VERIFYING_PATH)) return
        VpnSessionCoordinator.recordStage(attempt, VpnConnectionStage.TUN_INJECT)
        val path = VpnReadiness.verifyConfiguredPath(
            socksPort = socksPort,
            socksUser = socksUser,
            socksPassword = socksPassword,
            tunEstablished = isRunning && ::mInterface.isInitialized,
            hevStatsProvider = if (usingHev) ({ tun2SocksService?.getStats() }) else null,
            tunInjector = { VpnReadiness.injectThroughVpn(this@CoreVpnService) },
            xrayTunSnapshot = if (!usingHev) ({ CoreServiceManager.snapshotOutboundCounters() }) else null,
        )
        if (!VpnSessionCoordinator.recordPath(attempt, path)) return
        if (!path.verified) {
            val stage = when (path.reason) {
                "hev-no-progress" -> VpnConnectionStage.HEV_PROGRESS
                "xray-egress-failed" -> VpnConnectionStage.XRAY_EGRESS
                "socks5-handshake-failed" -> VpnConnectionStage.SOCKS
                "hev-not-alive" -> VpnConnectionStage.HEV
                "tun-not-established" -> VpnConnectionStage.TUN_ESTABLISH
                else -> VpnConnectionStage.TUN_INJECT
            }
            VpnSessionCoordinator.recordStage(attempt, stage)
            failTunnelStart(attempt, "HF-VPN-006", "Путь VPN не подтверждён (${path.reason})")
            return
        }

        if (!pipelineStillCurrent(attempt)) return
        VpnSessionCoordinator.recordStage(attempt, VpnConnectionStage.VERIFIED)
        if (!VpnSessionCoordinator.markConnected(attempt, pathVerified = true)) {
            LogUtil.w(AppConfig.TAG, "StartCore-VPN: markConnected rejected attempt=$attempt")
            return
        }
        HotfoxAutoFailover.reset()
        HotfoxShadowFailover.reset()
        HotfoxShadowStore.notePathSuccess()
        MmkvManager.getSelectServer()?.let { HotfoxServerSelection.rememberLastGoodAuto(it) }
        RootLanSharing.startClientSharing(this)
        CoreServiceManager.notifyTunnelReady(this)
        CoreServiceManager.startNetworkMonitorIfNeeded()
        startTrafficReporting()
        health.start(onRepeatedCoreFailure = { handleConnectedAutoFailover() })
        LogUtil.i(AppConfig.TAG, "StartCore-VPN: CONNECTED attempt=$attempt backend=${path.backend}")
    }

    private fun pipelineStillCurrent(attempt: Long): Boolean {
        if (!isRunning || !VpnSessionCoordinator.isCurrent(attempt)) {
            LogUtil.w(AppConfig.TAG, "StartCore-VPN: stale pipeline attempt=$attempt current=${VpnSessionCoordinator.currentAttempt()}")
            return false
        }
        return true
    }

    override fun stopService() {
        stopAllService(true)
    }

    override fun vpnProtect(socket: Int): Boolean {
        val result = com.v2ray.ang.vpn.HotfoxSocketProtect.protect(
            fd = socket,
            protocol = "fd",
            attempt = VpnSessionCoordinator.currentAttempt(),
        )
        LogUtil.i(AppConfig.TAG, "StartCore-VPN: vpnProtect socket=$socket ${result.reason}")
        return result.success
    }

    override fun setUnderlyingNetworks(networks: Array<Network>?): Boolean {
        return super<VpnService>.setUnderlyingNetworks(networks)
    }

    override fun attachBaseContext(newBase: Context?) {
        val context = newBase?.let {
            MyContextWrapper.wrap(newBase, SettingsManager.getLocale())
        }
        super.attachBaseContext(context)
    }

    /**
     * Sets up the VPN service.
     * Prepares the VPN and configures it if preparation is successful.
     */
    private fun setupVpnService(): Boolean {
        val prepare = prepare(this)
        if (prepare != null) {
            LogUtil.e(AppConfig.TAG, "StartCore-VPN: Permission not granted")
            stopSelf()
            return false
        }

        if (configureVpnService() != true) {
            LogUtil.e(AppConfig.TAG, "StartCore-VPN: Configuration failed")
            stopSelf()
            return false
        }
        return true
    }

    /**
     * Configures the VPN service.
     * @return True if the VPN service was configured successfully, false otherwise.
     */
    private fun configureVpnService(): Boolean {
        val builder = Builder()
        val routing = HotfoxRoutingStore.load()
        val ipv6Enabled = MmkvManager.decodeSettingsBool(AppConfig.PREF_IPV6_ENABLED)
        val tun = HotfoxRoutingDataPlane.tunEnforcement(
            routing,
            ipv6Enabled,
            BuildConfig.APPLICATION_ID,
        )

        // Configure network settings (addresses, routing and DNS)
        configureNetworkSettings(builder, tun)

        // Configure app-specific settings (session name and per-app proxy)
        configurePerAppProxy(builder, tun.perApp)

        // Close the old interface since the parameters have been changed
        try {
            if (::mInterface.isInitialized) {
                mInterface.close()
                com.v2ray.ang.vpn.TunFdEvidence.recordClosed()
            }
        } catch (e: Exception) {
            LogUtil.w(AppConfig.TAG, "Failed to close old interface", e)
        }

        // Configure platform-specific features
        configurePlatformFeatures(builder)

        // Create a new interface using the builder and save the parameters
        try {
            mInterface = builder.establish()!!
            com.v2ray.ang.vpn.TunFdEvidence.recordEstablish()
            isRunning = true
            return true
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to establish VPN interface", e)
            stopAllService()
        }
        return false
    }

    /**
     * Configures the basic network settings for the VPN.
     * This includes IP addresses, routing rules, and DNS servers.
     *
     * @param builder The VPN Builder to configure
     */
    private fun configureNetworkSettings(
        builder: Builder,
        tun: HotfoxRoutingDataPlane.TunEnforcement,
    ) {
        val vpnConfig = SettingsManager.getCurrentVpnInterfaceAddressConfig()

        // Configure IPv4 settings
        builder.setMtu(SettingsManager.getVpnMtu())
        builder.addAddress(vpnConfig.ipv4Client, 30)

        // Configure routing rules. GLOBAL never bypasses LAN; other modes honor
        // the explicit LAN preference only — never SettingsManager geosite inference.
        // Custom APP/LAN *rules* are not consulted: they are rejected at parse.
        if (!tun.captureIpv4Default) {
            AppConfig.ROUTED_IP_LIST.forEach {
                val addr = it.split('/')
                builder.addRoute(addr[0], addr[1].toInt())
            }
        } else {
            builder.addRoute("0.0.0.0", 0)
        }

        // Always capture IPv6 with ::/0. Partial GUA routes (2000::/3) omit
        // NAT64 64:ff9b::/96 and operator prefixes, which would leak off TUN
        // while CONNECTED is still reachable. LAN bypass stays IPv4-only.
        // When IPv6 proxying is disabled, CoreConfigManager still fail-closes
        // captured IPv6 to blackhole rather than the underlying network.
        builder.addAddress(vpnConfig.ipv6Client, 126)
        check(tun.captureIpv6Default && tun.ipv6CaptureRoutes.any { it.address == "::" && it.prefix == 0 }) {
            "IPv6 TUN capture must remain fail-closed ::/0"
        }
        builder.addRoute("::", 0)

        // DNS is always installed on the VPN interface. 2.5 does not allow a
        // silent system-DNS bypass while protection is claimed.
        SettingsManager.getVpnDnsServers().forEach {
            if (Utils.isPureIpAddress(it)) {
                builder.addDnsServer(it)
            }
        }

        builder.setSession(getString(R.string.app_name))
    }

    /**
     * Configures platform-specific VPN features for different Android versions.
     *
     * @param builder The VPN Builder to configure
     */
    private fun configurePlatformFeatures(builder: Builder) {
        // Android Q (API 29) and above: Configure metering and HTTP proxy
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false)
            if (MmkvManager.decodeSettingsBool(AppConfig.PREF_APPEND_HTTP_PROXY, false)) {
                // Optional Android HTTP proxy (10809). HEV production path is SOCKS 10808.
                builder.setHttpProxy(ProxyInfo.buildDirectProxy(LOOPBACK, SettingsManager.getHttpPort()))
            }
        }
    }

    /**
     * Configures per-app proxy rules for the VPN builder.
     *
     * - If per-app proxy is not enabled, disallow the VPN service's own package.
     * - If no apps are selected, disallow the VPN service's own package.
     * - If bypass mode is enabled, disallow all selected apps (including self).
     * - If proxy mode is enabled, only allow the selected apps (excluding self).
     *
     * @param builder The VPN Builder to configure.
     */
    private fun configurePerAppProxy(builder: Builder, plan: PerAppVpnPlan) {
        val selfPackageName = BuildConfig.APPLICATION_ID
        val ops = com.v2ray.ang.vpn.HotfoxTunSelfExclusion.forPlan(plan, selfPackageName)
        // Always keep HotFox/Xray off TUN. injectThroughVpn still binds
        // probe sockets to TRANSPORT_VPN so path proof is tunneled, not
        // process-direct HTTPS from the excluded UID.
        fun apply(pkg: String, disallow: Boolean) {
            try {
                if (disallow) builder.addDisallowedApplication(pkg)
                else builder.addAllowedApplication(pkg)
            } catch (e: PackageManager.NameNotFoundException) {
                LogUtil.e(AppConfig.TAG, "StartCore-VPN: Failed to configure app $pkg", e)
            }
        }
        if (ops.useAllowList) {
            ops.allow.forEach { apply(it, disallow = false) }
        } else {
            ops.disallow.forEach { apply(it, disallow = true) }
        }
    }

    /**
     * Runs the tun2socks process.
     * Starts the tun2socks process with the appropriate parameters.
     */
    private fun runTun2socks(): Boolean {
        if (SettingsManager.isUsingHevTun()) {
            tun2SocksService = TProxyService(
                context = applicationContext,
                vpnInterface = mInterface,
            )
        } else {
            tun2SocksService = null
        }

        return tun2SocksService?.startTun2Socks() ?: true
    }

    private fun startTrafficReporting() {
        trafficJob?.cancel()
        trafficJob = serviceScope.launch {
            var baselineTaken = false
            while (isActive && isRunning) {
                val stats = tun2SocksService?.getStats()
                if (stats != null && VpnReadiness.hevStatsAlive(stats)) {
                    // HEV order: txPackets, txBytes, rxPackets, rxBytes.
                    val rawTx = stats[1]
                    val rawRx = stats[3]
                    if (!baselineTaken) {
                        VpnSessionCoordinator.traffic.startSession(rawTx, rawRx)
                        baselineTaken = true
                    }
                    val session = VpnSessionCoordinator.traffic.sample(rawTx, rawRx)
                    MessageUtil.sendMsg2UI(
                        this@CoreVpnService,
                        AppConfig.MSG_HOTFOX_TRAFFIC,
                        "${session.uploadedBytes}:${session.downloadedBytes}",
                    )
                }
                delay(1000L)
            }
        }
    }

    private fun failTunnelStart(attempt: Long, code: String, message: String) {
        LogUtil.e(AppConfig.TAG, "StartCore-VPN: $code $message")
        if (tryAutoFailover(attempt, code)) {
            return
        }
        if (!VpnSessionCoordinator.markError(attempt, code, message)) {
            LogUtil.w(AppConfig.TAG, "StartCore-VPN: stale failTunnelStart ignored attempt=$attempt")
            return
        }
        MessageUtil.sendMsg2UI(this, AppConfig.MSG_STATE_START_FAILURE, message)
        stopAllService()
    }

    private fun tryAutoFailover(attempt: Long, code: String): Boolean {
        if (!VpnSessionCoordinator.isCurrent(attempt)) return false
        val decision = HotfoxShadowFailover.considerLive(code)
        if (decision.action != FailoverAction.SWITCH) return false
        if (!VpnSessionCoordinator.markReconnecting(attempt)) return false
        LogUtil.i(AppConfig.TAG, "StartCore-VPN: AUTO failover ${decision.reason} next=${decision.guid}")
        CoreServiceManager.scheduleAuthorizedRestart(applicationContext)
        stopAllService()
        return true
    }

    private fun handleConnectedAutoFailover() {
        if (VpnSessionCoordinator.currentState() != VpnSessionState.CONNECTED) return
        if (!HotfoxServerSelection.isAutoMode()) return
        val now = System.currentTimeMillis()
        HotfoxShadowStore.notePathDeath(now)
        if (!HotfoxSelfHeal.shouldHeal(
                HotfoxShadowStore.selfHeal,
                pathVerifiedDeath = true,
                nowEpochMs = now,
                connected = true,
                generationCurrent = true,
            )
        ) {
            return
        }
        val decision = HotfoxShadowFailover.considerLive("HF-VPN-006")
        if (decision.action != FailoverAction.SWITCH) return
        HotfoxShadowStore.noteHeal(now)
        LogUtil.i(AppConfig.TAG, "StartCore-VPN: AUTO connected failover ${decision.reason} next=${decision.guid}")
        CoreServiceManager.requestConnectedReload()
    }

    private fun stopAllService(isForced: Boolean = true) {
        unlockStart()
        val state = VpnSessionCoordinator.currentState()
        val keepError = state == VpnSessionState.ERROR
        val keepReconnecting = state == VpnSessionState.RECONNECTING
        val stopAttempt = VpnSessionCoordinator.currentAttempt()
        if (!keepError) {
            if (!keepReconnecting) {
                VpnSessionCoordinator.setState(stopAttempt, VpnSessionState.DISCONNECTING)
            }
            VpnSessionCoordinator.setTeardownActive(true)
        }
        com.v2ray.ang.vpn.HotfoxSocketProtect.detach(VpnSessionCoordinator.currentAttempt())
        isRunning = false
        pipelineJob?.cancel()
        pipelineJob = null
        trafficJob?.cancel()
        trafficJob = null
        health.stop()
        tun2SocksService?.stopTun2Socks()
        tun2SocksService = null

        RootLanSharing.stopClientSharing(this)
        VpnLoopPrevention.unbindProcess(this)

        if (isForced) {
            // stopSelf has to be called ahead of mInterface.close(). otherwise v2ray core cannot be stooped
            stopSelf()
            try {
                if (::mInterface.isInitialized) {
                    mInterface.close()
                    com.v2ray.ang.vpn.TunFdEvidence.recordClosed()
                    LogUtil.i(AppConfig.TAG, "StartCore-VPN: VPN interface closed")
                }
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "StartCore-VPN: Failed to close interface", e)
            }
        }

        CoreServiceManager.stopCoreLoop()
    }

    private fun tryLockStart(): Boolean = isStartingLock.compareAndSet(false, true)

    private fun unlockStart() {
        isStartingLock.set(false)
    }

}
