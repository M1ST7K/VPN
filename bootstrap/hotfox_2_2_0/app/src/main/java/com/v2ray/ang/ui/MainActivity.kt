package com.v2ray.ang.ui

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.text.format.Formatter
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.core.CoreServiceManager
import com.v2ray.ang.databinding.ActivityMainBinding
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.enums.PermissionType
import com.v2ray.ang.enums.SmartRoutingMode
import com.v2ray.ang.extension.toast
import com.v2ray.ang.extension.toastError
import com.v2ray.ang.handler.AngConfigManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.SettingsChangeManager
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.handler.SubscriptionUpdater
import com.v2ray.ang.root.RootManager
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.Utils
import com.v2ray.ang.viewmodel.MainViewModel
import com.v2ray.ang.vpn.ConnectionUiMapper
import com.v2ray.ang.vpn.HotfoxServerPresentation
import com.v2ray.ang.vpn.HotfoxSubscriptionPresentation
import com.v2ray.ang.vpn.HotfoxTrafficFormatter
import com.v2ray.ang.vpn.SubscriptionPresentation
import com.v2ray.ang.vpn.VpnSessionCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : HelperBaseActivity(), NavigationView.OnNavigationItemSelectedListener {
    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    val mainViewModel: MainViewModel by viewModels()
    private lateinit var groupPagerAdapter: GroupPagerAdapter
    private var tabMediator: TabLayoutMediator? = null
    private var haloAnimator: ObjectAnimator? = null
    private var logoFloatAnimator: ObjectAnimator? = null
    private var lastVisualState: ConnectionVisualState? = null
    private var lastServerLabel: String? = null
    private var connectionClockJob: Job? = null
    private var lastFabTapElapsed = 0L
    private var subscriptionUrlForCopy: String? = null
    private var currentSection = UiSection.CONNECTION

    private enum class UiSection {
        CONNECTION,
        SERVERS,
        SUBSCRIPTION,
    }

    private enum class ConnectionVisualState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
    }

    private val requestVpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) startV2Ray()
        else applyRunningState(false, false)
    }
    private val requestActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (SettingsChangeManager.consumeRestartService() && mainViewModel.isRunning.value == true) {
            restartV2Ray()
        }
        if (SettingsChangeManager.consumeSetupGroupTab()) {
            setupGroupTab()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupToolbar(binding.toolbar, false, "")

        // setup viewpager and tablayout
        groupPagerAdapter = GroupPagerAdapter(this, emptyList())
        binding.viewPager.adapter = groupPagerAdapter
        binding.viewPager.isUserInputEnabled = true

        // setup navigation drawer
        setupNavigationDrawer()
        binding.toolbar.setNavigationIcon(R.drawable.ic_settings_24dp)
        binding.toolbar.navigationContentDescription = getString(R.string.hotfox_settings_content_description)
        binding.toolbar.setNavigationOnClickListener {
            MaterialAlertDialogBuilder(this).setTitle("Настройки HotFox")
                .setItems(arrayOf("Подключение и DNS", "Приложения через VPN", "Правила маршрутизации", "Подписки", "Блокировка рекламы", "Диагностика", "О приложении")) { _, i ->
                    when (i) {
                        0 -> requestActivityLauncher.launch(Intent(this, SettingsActivity::class.java))
                        1 -> requestActivityLauncher.launch(Intent(this, PerAppProxyActivity::class.java))
                        2 -> requestActivityLauncher.launch(Intent(this, RoutingSettingActivity::class.java))
                        3 -> requestActivityLauncher.launch(Intent(this, SubSettingActivity::class.java))
                        4 -> {
                            val enabled = MmkvManager.decodeSettingsBool("hotfox_block_ads", false)
                            MaterialAlertDialogBuilder(this).setTitle("Фильтр рекламы")
                                .setMessage("Блокировка рекламных доменов по geosite. Сейчас: " + if (enabled) "включена" else "выключена")
                                .setPositiveButton(if (enabled) "Выключить" else "Включить") { _, _ ->
                                    MmkvManager.encodeSettings("hotfox_block_ads", !enabled)
                                    if (mainViewModel.isRunning.value == true) restartV2Ray()
                                }.setNegativeButton(android.R.string.cancel, null).show()
                        }
                        5 -> copyDiagnostics()
                        6 -> startActivity(Intent(this, AboutActivity::class.java))
                    }
                }.show()
        }
        setupBottomNavigation()
        setupEditorialNavigation()

        binding.fab.setOnClickListener { handleFabAction() }
        binding.connectAction.setOnClickListener { handleFabAction() }
        binding.layoutTest.setOnClickListener { handleLayoutTestClick() }
        binding.selectedServerCard.setOnClickListener {
            showSection(UiSection.SERVERS)
            binding.viewPager.postDelayed({ locateSelectedServer() }, 180L)
        }
        binding.smartRoutingCard.setOnClickListener { showSmartRoutingDialog() }
        binding.supportButton.setOnClickListener { showAddDialog() }

        setupGroupTab()
        setupViewModel()
        SubscriptionUpdater.sync()
        mainViewModel.reloadServerList()
        refreshDashboard()
        refreshSmartRouting()
        runEntranceAnimations()

        checkAndRequestPermission(PermissionType.POST_NOTIFICATIONS) {
        }
    }

    private fun setupEditorialNavigation() {
        binding.railConnection.setOnClickListener { showSection(UiSection.CONNECTION) }
        binding.railServers.setOnClickListener { showSection(UiSection.SERVERS) }
        binding.railSubscription.setOnClickListener { showSection(UiSection.SUBSCRIPTION) }

        binding.filterAll.setOnClickListener {
            mainViewModel.setFavoriteOnly(false)
            mainViewModel.filterConfig("")
            binding.filterAll.setTextColor(ContextCompat.getColor(this, R.color.hotfox_editorial_text))
            binding.filterFavorites.setTextColor(ContextCompat.getColor(this, R.color.hotfox_editorial_text_dim))
        }
        binding.filterFavorites.setOnClickListener {
            mainViewModel.setFavoriteOnly(true)
            binding.filterFavorites.setTextColor(ContextCompat.getColor(this, R.color.hotfox_editorial_text))
            binding.filterAll.setTextColor(ContextCompat.getColor(this, R.color.hotfox_editorial_text_dim))
            binding.viewPager.post {
                if (mainViewModel.serversCache.isEmpty()) {
                    toast(getString(R.string.hotfox_favorites_empty))
                }
            }
        }
        binding.filterCountries.setOnClickListener { showCountryFilterDialog() }

        binding.actionSubscriptionUpdate.setOnClickListener { updateCurrentSubscription() }
        binding.actionSubscriptionImport.setOnClickListener { showAddDialog() }
        binding.actionSubscriptionAdd.setOnClickListener { showAddDialog() }
        binding.tvSubscriptionUrl.setOnClickListener { copySubscriptionUrl() }

        showSection(UiSection.CONNECTION)
    }

    private fun showSection(section: UiSection) {
        currentSection = section
        binding.screenConnection.isVisible = section == UiSection.CONNECTION
        binding.screenServers.isVisible = section == UiSection.SERVERS
        binding.screenSubscription.isVisible = section == UiSection.SUBSCRIPTION
        binding.dotConnection.setBackgroundResource(if (section == UiSection.CONNECTION) R.drawable.hotfox_dot_orange else R.drawable.hotfox_dot_muted)
        binding.dotServers.setBackgroundResource(if (section == UiSection.SERVERS) R.drawable.hotfox_dot_orange else R.drawable.hotfox_dot_muted)
        binding.dotSubscription.setBackgroundResource(if (section == UiSection.SUBSCRIPTION) R.drawable.hotfox_dot_orange else R.drawable.hotfox_dot_muted)
        if (section == UiSection.SUBSCRIPTION) refreshDashboard()
    }

    private fun showCountryFilterDialog() {
        val countries = linkedSetOf<String>()
        val serverIds = MmkvManager.decodeServerList(mainViewModel.subscriptionId)
        serverIds.forEach { guid ->
            val remark = MmkvManager.decodeServerConfig(guid)?.remarks
            HotfoxServerPresentation.fromRemark(remark).country?.let(countries::add)
        }
        val sorted = countries.sorted()
        if (sorted.isEmpty()) {
            toast(getString(R.string.hotfox_countries_unknown))
            return
        }
        val labels = arrayOf(getString(R.string.hotfox_all_countries)) + sorted.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle("Страны")
            .setItems(labels) { _, which ->
                mainViewModel.setFavoriteOnly(false)
                if (which == 0) {
                    mainViewModel.filterConfig("")
                } else {
                    mainViewModel.filterConfig(sorted[which - 1])
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun updateCurrentSubscription() {
        binding.actionSubscriptionUpdate.isEnabled = false
        binding.actionSubscriptionUpdate.text = "Обновление…"
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { mainViewModel.updateConfigViaSubAll() }
            binding.actionSubscriptionUpdate.isEnabled = true
            binding.actionSubscriptionUpdate.text = "Обновить"
            mainViewModel.reloadServerList()
            setupGroupTab()
            refreshDashboard()
            val message = if (result.failureCount == 0) {
                "Обновлено серверов: ${result.configCount}"
            } else {
                "Обновлено: ${result.successCount}, ошибок: ${result.failureCount}"
            }
            toast(message)
        }
    }

    private fun copySubscriptionUrl() {
        val url = subscriptionUrlForCopy?.takeIf { it.isNotBlank() } ?: return
        val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("HotFox subscription", url))
        toast("URL подписки скопирован")
    }

    private fun copyDiagnostics() {
        lifecycleScope.launch {
            val socksPort = SettingsManager.getSocksPort()
            val socksReady = withContext(Dispatchers.IO) { com.v2ray.ang.vpn.VpnReadiness.probe(socksPort) }
            val selected = MmkvManager.getSelectServer()?.let(MmkvManager::decodeServerConfig)
            val ipv6 = MmkvManager.decodeSettingsBool(AppConfig.PREF_IPV6_ENABLED)
            val routing = MmkvManager.decodeSettingsString(AppConfig.PREF_SMART_ROUTING_MODE).orEmpty()
            val traffic = mainViewModel.tunnelTraffic.value
            val path = VpnSessionCoordinator.lastPath()
            val report = com.v2ray.ang.vpn.HotfoxDiagnosticsBuilder.build(
                androidRelease = Build.VERSION.RELEASE,
                api = Build.VERSION.SDK_INT,
                abi = Build.SUPPORTED_ABIS.firstOrNull().orEmpty(),
                socksPort = socksPort,
                socksReady = socksReady,
                hevRunning = path?.hevAlive,
                ipv4Captured = null,
                ipv6Captured = null,
                ipv6Policy = if (ipv6) "proxy" else "fail-closed-blackhole",
                routingMode = routing.ifBlank { "smart" },
                serverRemark = selected?.remarks,
                uploaded = traffic?.first,
                downloaded = traffic?.second,
                lastError = VpnSessionCoordinator.lastError(),
                serverCount = MmkvManager.decodeServerList(selected?.subscriptionId.orEmpty()).size,
                path = path,
            )
            val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("HotFox diagnostics", report))
            toast(getString(R.string.hotfox_copy_diagnostics))
        }
    }

    private fun showAddDialog() {
        MaterialAlertDialogBuilder(this).setTitle("Добавить подключение")
            .setItems(arrayOf("HTTPS-подписка", "Из буфера обмена", "QR-код", "Из файла", "Always-on / Kill Switch")) { _, i ->
                when (i) {
                    0 -> {
                        val field = android.widget.EditText(this).apply {
                            hint = "https://…"; setSingleLine(true)
                            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
                            setPadding(36, 24, 36, 24)
                        }
                        MaterialAlertDialogBuilder(this).setTitle("Ссылка подписки").setView(field)
                            .setPositiveButton("Добавить") { _, _ ->
                                val value = field.text.toString().trim()
                                if (value.startsWith("https://", true)) importBatchConfig(value)
                                else toast("Нужна HTTPS-ссылка")
                            }.setNegativeButton(android.R.string.cancel, null).show()
                    }
                    1 -> importClipboard()
                    2 -> importQRcode()
                    3 -> importConfigLocal()
                    4 -> MaterialAlertDialogBuilder(this).setTitle("Защита при обрыве")
                        .setMessage("В настройках Android откройте HotFox Proxy и включите «Постоянная VPN» и «Блокировать соединения без VPN». Блокировку обеспечивает Android. Прямые маршруты и исключённые приложения могут стать недоступны.")
                        .setPositiveButton("Настройки Android") { _, _ -> startActivity(Intent(android.provider.Settings.ACTION_VPN_SETTINGS)) }
                        .setNegativeButton(android.R.string.cancel, null).show()
                }
            }.show()
    }

    private fun setupBottomNavigation() {
        binding.bottomNav.selectedItemId = R.id.nav_home
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_subscriptions -> {
                    requestActivityLauncher.launch(Intent(this, SubSettingActivity::class.java))
                    true
                }
                R.id.nav_renewal -> {
                    startActivity(Intent(this, RenewalActivity::class.java))
                    true
                }
                R.id.nav_settings -> {
                    requestActivityLauncher.launch(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun setupNavigationDrawer() {
        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        binding.navView.setNavigationItemSelectedListener(this)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
    }

    private fun setupViewModel() {
        mainViewModel.updateTestResultAction.observe(this) { setTestState(it) }
        mainViewModel.isRunning.observe(this) { isRunning ->
            applyRunningState(false, isRunning)
        }
        mainViewModel.tunnelTraffic.observe(this) { (txBytes, rxBytes) ->
            binding.tvDownloaded.text = "↓ ${HotfoxTrafficFormatter.formatBytes(rxBytes)}"
            binding.tvUploaded.text = "↑ ${HotfoxTrafficFormatter.formatBytes(txBytes)}"
        }
        mainViewModel.tunnelHealth.observe(this) { status ->
            if (mainViewModel.isRunning.value == true) {
                when {
                    status.startsWith("core-ok:") -> {
                        // Core health only proves that Xray can reach the selected server through
                        // its local proxy. It must never overwrite the VpnService/TUN status.
                        val latency = status.substringAfter(":").toIntOrNull() ?: 0
                        binding.tvTestState.text = getString(R.string.hotfox_proxy_ok, latency)
                    }
                    status == "core-failed" -> {
                        binding.tvTestState.setText(R.string.hotfox_proxy_failed)
                    }
                }
            }
        }
        mainViewModel.startListenBroadcast()
        mainViewModel.initAssets(assets)
    }

    private fun setupGroupTab() {
        val groups = mainViewModel.getSubscriptions(this)
        groupPagerAdapter.update(groups)

        tabMediator?.detach()
        tabMediator = TabLayoutMediator(binding.tabGroup, binding.viewPager) { tab, position ->
            groupPagerAdapter.groups.getOrNull(position)?.let { group ->
                tab.text = group.remarks
                tab.tag = group.id
            }
        }.also { it.attach() }

        if (groups.isNotEmpty()) {
            val targetIndex = groups.indexOfFirst { it.id == mainViewModel.subscriptionId }
                .takeIf { it >= 0 } ?: 0
            binding.viewPager.setCurrentItem(targetIndex, false)
        }

        // A single real subscription is rendered as one full-width header/tab with its actual
        // name. No synthetic Default column next to it.
        binding.tabGroup.tabMode = if (groups.size <= 1) TabLayout.MODE_FIXED else TabLayout.MODE_SCROLLABLE
        binding.tabGroup.isVisible = groups.isNotEmpty()
        refreshGroupTabTitles(true)
    }

    fun refreshGroupTabTitles(refreshAll: Boolean = false) {
        val groupsToRefresh = if (refreshAll || mainViewModel.subscriptionId.isEmpty()) {
            groupPagerAdapter.groups
        } else {
            groupPagerAdapter.groups.filter { it.id == mainViewModel.subscriptionId }
        }

        groupsToRefresh.forEach { group ->
            if (group.id.isEmpty()) {
                return@forEach
            }
            val tabIndex = groupPagerAdapter.groups.indexOfFirst { it.id == group.id }
            if (tabIndex >= 0) {
                binding.tabGroup.getTabAt(tabIndex)?.text = group.remarks
            }
        }
    }

    private fun handleFabAction() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastFabTapElapsed < 700L) return
        lastFabTapElapsed = now
        if (mainViewModel.isRunning.value != true && MmkvManager.getSelectServer().isNullOrEmpty()) {
            showAddDialog(); return
        }
        applyRunningState(isLoading = true, isRunning = false)
        binding.fab.postDelayed({ if (!isFinishing && !binding.fab.isEnabled) applyRunningState(false, mainViewModel.isRunning.value == true) }, 15_000)

        if (mainViewModel.isRunning.value == true) {
            CoreServiceManager.stopVService(this)
        } else if (SettingsManager.isVpnMode()) {
            val intent = VpnService.prepare(this)
            if (intent == null) {
                startV2Ray()
            } else {
                requestVpnPermission.launch(intent)
            }
        } else {
            startV2Ray()
        }
    }

    private fun handleLayoutTestClick() {
        if (currentSection == UiSection.SERVERS) {
            setTestState("Проверка…")
            mainViewModel.testAllRealPing()
            binding.layoutTest.postDelayed({
                mainViewModel.sortByTestResults()
                mainViewModel.reloadServerList()
                setTestState("Пинг")
            }, 2600L)
            return
        }
        if (mainViewModel.isRunning.value == true) {
            setTestState(getString(R.string.connection_test_testing))
            mainViewModel.testCurrentServerRealPing()
        }
    }

    private fun startV2Ray() {
        if (MmkvManager.getSelectServer().isNullOrEmpty()) {
            toast(R.string.title_file_chooser)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN && MmkvManager.decodeSettingsBool(AppConfig.PREF_PROXY_SHARING)) {
            checkAndRequestPermission(PermissionType.ACCESS_LOCAL_NETWORK) {}
        }

        if (SettingsManager.isRootMode()) {
            lifecycleScope.launch {
                val hasRoot = RootManager.refresh()
                if (!hasRoot) {
                    toast(R.string.toast_root_required)
                    applyRunningState(false, false)
                    return@launch
                }
                CoreServiceManager.startVService(this@MainActivity)
            }
            return
        }

        CoreServiceManager.startVService(this)
    }

    fun restartV2Ray() {
        lifecycleScope.launch {
            val session = VpnSessionCoordinator.currentState()
            if (mainViewModel.isRunning.value == true || session.isServiceActive() || session.isBusy()) {
                CoreServiceManager.stopVService(this@MainActivity)
                VpnSessionCoordinator.awaitIdle()
            }
            startV2Ray()
        }
    }

    private fun setTestState(content: String?) {
        binding.tvTestState.text = content
    }

    @Suppress("UNUSED_PARAMETER")
    private fun applyRunningState(isLoading: Boolean, isRunning: Boolean) {
        val session = VpnSessionCoordinator.currentState()
        val headline = ConnectionUiMapper.resolveHeadline(session, isLoading)
        val headlineRes = when (headline) {
            ConnectionUiMapper.Headline.CONNECTED -> R.string.hotfox_headline_connected
            ConnectionUiMapper.Headline.CONNECTING -> R.string.hotfox_headline_connecting
            ConnectionUiMapper.Headline.RECONNECTING -> R.string.hotfox_headline_reconnecting
            ConnectionUiMapper.Headline.ERROR -> R.string.hotfox_headline_error
            ConnectionUiMapper.Headline.DISCONNECTING -> R.string.hotfox_headline_disconnecting
            ConnectionUiMapper.Headline.PROXY_ONLY -> R.string.hotfox_headline_proxy_only
            ConnectionUiMapper.Headline.ROOT_RUNNING -> R.string.hotfox_headline_root
            ConnectionUiMapper.Headline.DISCONNECTED -> R.string.hotfox_headline_disconnected
        }
        val visual = when (headline) {
            ConnectionUiMapper.Headline.CONNECTED -> ConnectionVisualState.CONNECTED
            ConnectionUiMapper.Headline.DISCONNECTED, ConnectionUiMapper.Headline.ERROR -> ConnectionVisualState.DISCONNECTED
            else -> ConnectionVisualState.CONNECTING
        }

        binding.fab.isEnabled = !isLoading && headline != ConnectionUiMapper.Headline.DISCONNECTING
        binding.connectAction.isEnabled = binding.fab.isEnabled
        binding.fab.setImageResource(R.drawable.ic_hotfox_power_36)
        binding.fab.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(
                this,
                if (headline == ConnectionUiMapper.Headline.CONNECTED) R.color.color_fab_active else R.color.hotfox_orange,
            )
        )
        binding.connectAction.text = when (headline) {
            ConnectionUiMapper.Headline.CONNECTED,
            ConnectionUiMapper.Headline.RECONNECTING,
            ConnectionUiMapper.Headline.PROXY_ONLY,
            ConnectionUiMapper.Headline.ROOT_RUNNING,
            -> getString(R.string.hotfox_disconnect)
            ConnectionUiMapper.Headline.DISCONNECTING -> getString(R.string.hotfox_headline_disconnecting)
            ConnectionUiMapper.Headline.ERROR -> getString(R.string.hotfox_connect)
            ConnectionUiMapper.Headline.CONNECTING -> getString(R.string.hotfox_headline_connecting)
            ConnectionUiMapper.Headline.DISCONNECTED -> getString(R.string.hotfox_connect)
        }
        binding.fab.contentDescription = binding.connectAction.text
        updateStatusText(headlineRes, R.color.hotfox_editorial_text)
        if (ConnectionUiMapper.timerShouldRun(session) && headline == ConnectionUiMapper.Headline.CONNECTED) {
            startConnectionClock()
            setTestState(getString(R.string.connection_connected))
            binding.layoutTest.isFocusable = true
        } else {
            stopConnectionClock(reset = headline == ConnectionUiMapper.Headline.DISCONNECTED || headline == ConnectionUiMapper.Headline.ERROR)
            if (headline == ConnectionUiMapper.Headline.ERROR) {
                setTestState(VpnSessionCoordinator.lastError() ?: getString(R.string.hotfox_headline_error))
            } else if (headline == ConnectionUiMapper.Headline.PROXY_ONLY) {
                setTestState(getString(R.string.hotfox_headline_proxy_only))
            } else if (headline == ConnectionUiMapper.Headline.ROOT_RUNNING) {
                setTestState(getString(R.string.hotfox_headline_root))
            } else if (headline != ConnectionUiMapper.Headline.CONNECTING && headline != ConnectionUiMapper.Headline.DISCONNECTING) {
                setTestState(getString(R.string.connection_not_connected))
            }
            binding.layoutTest.isFocusable = false
        }
        startConnectionAnimation(visual)
    }

    private fun updateStatusText(textRes: Int, colorRes: Int) {
        binding.tvConnectLabel.animate().cancel()
        binding.tvConnectLabel.setText(textRes)
        binding.tvConnectLabel.setTextColor(ContextCompat.getColor(this, colorRes))
        binding.tvConnectLabel.alpha = 0.35f
        binding.tvConnectLabel.translationY = 5f
        binding.tvConnectLabel.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(220L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    private fun startConnectionClock() {
        val started = VpnSessionCoordinator.sessionStartedAtElapsed() ?: return
        connectionClockJob?.cancel()
        connectionClockJob = lifecycleScope.launch {
            while (isActive && ConnectionUiMapper.timerShouldRun(VpnSessionCoordinator.currentState())) {
                val origin = VpnSessionCoordinator.sessionStartedAtElapsed() ?: break
                val elapsed = SystemClock.elapsedRealtime() - origin
                val totalSeconds = elapsed / 1000L
                val hours = totalSeconds / 3600L
                val minutes = (totalSeconds % 3600L) / 60L
                val seconds = totalSeconds % 60L
                binding.tvVpnStatus.text = String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
                delay(1000L)
            }
        }
    }

    private fun stopConnectionClock(reset: Boolean) {
        connectionClockJob?.cancel()
        connectionClockJob = null
        if (reset) {
            binding.tvVpnStatus.text = "00:00:00"
        }
    }

    private fun startConnectionAnimation(state: ConnectionVisualState) {
        if (lastVisualState == state) return
        lastVisualState = state
        haloAnimator?.cancel()
        binding.connectAction.scaleX = 1f
        binding.connectAction.scaleY = 1f
        binding.connectAction.alpha = 1f

        binding.connectAction.scaleX = 0.97f
        binding.connectAction.scaleY = 0.97f
        binding.connectAction.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(320L)
            .setInterpolator(OvershootInterpolator(1.15f))
            .start()

        if (state == ConnectionVisualState.DISCONNECTED) return

        // The approved UI is deliberately calm: only the thin action underline breathes.
        val endScale = if (state == ConnectionVisualState.CONNECTING) 1.025f else 1.012f
        val endAlpha = if (state == ConnectionVisualState.CONNECTING) 0.55f else 0.82f
        haloAnimator = ObjectAnimator.ofPropertyValuesHolder(
            binding.connectAction,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, endScale),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, endScale),
            PropertyValuesHolder.ofFloat(View.ALPHA, 1f, endAlpha),
        ).apply {
            duration = if (state == ConnectionVisualState.CONNECTING) 720L else 1700L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

    }

    private fun runEntranceAnimations() {
        val views = listOf(
            binding.toolbar,
            binding.heroSurface,
            binding.statusBlock,
            binding.selectedServerCard,
            binding.smartRoutingCard,
            binding.tabGroup,
        ).filter { it.isVisible }
        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = if (index < 2) -14f else 18f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(index * 55L)
                .setDuration(360L)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }
    }

    private fun startLogoAnimation() {
        if (!binding.brandLogo.isVisible || logoFloatAnimator?.isRunning == true) return
        logoFloatAnimator = ObjectAnimator.ofFloat(binding.brandLogo, View.TRANSLATION_Y, 0f, -7f, 0f).apply {
            duration = 3200L
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun showSmartRoutingDialog() {
        val modes = SmartRoutingMode.entries
        val labels = arrayOf(
            getString(R.string.hotfox_route_smart),
            getString(R.string.hotfox_route_global),
            getString(R.string.hotfox_route_custom),
        )
        val current = SmartRoutingMode.fromStorage(
            MmkvManager.decodeSettingsString(AppConfig.PREF_SMART_ROUTING_MODE)
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.hotfox_smart_routing_title)
            .setSingleChoiceItems(labels, modes.indexOf(current)) { dialog, which ->
                val selected = modes[which]
                if (selected != current) {
                    MmkvManager.encodeSettings(AppConfig.PREF_SMART_ROUTING_MODE, selected.storageValue)
                    refreshSmartRouting()
                    animateRoutingCard()
                    toast(R.string.hotfox_route_changed)
                    if (mainViewModel.isRunning.value == true) restartV2Ray()
                }
                dialog.dismiss()
            }
            .setNeutralButton("Правила") { _, _ -> requestActivityLauncher.launch(Intent(this, RoutingSettingActivity::class.java)) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun refreshSmartRouting() {
        when (SmartRoutingMode.fromStorage(
            MmkvManager.decodeSettingsString(AppConfig.PREF_SMART_ROUTING_MODE)
        )) {
            SmartRoutingMode.SMART -> {
                binding.tvSmartRoutingMode.setText(R.string.hotfox_route_smart)
                binding.tvSmartRoutingSummary.setText(R.string.hotfox_route_smart_summary)
                binding.tvRoutingInline.text = "Умная маршрутизация · Auto"
            }
            SmartRoutingMode.GLOBAL -> {
                binding.tvSmartRoutingMode.setText(R.string.hotfox_route_global)
                binding.tvSmartRoutingSummary.setText(R.string.hotfox_route_global_summary)
                binding.tvRoutingInline.text = "Маршрутизация · Global"
            }
            SmartRoutingMode.CUSTOM -> {
                binding.tvSmartRoutingMode.setText(R.string.hotfox_route_custom)
                binding.tvSmartRoutingSummary.setText(R.string.hotfox_route_custom_summary)
                binding.tvRoutingInline.text = "Маршрутизация · Custom"
            }
        }
    }

    private fun animateRoutingCard() {
        binding.smartRoutingCard.animate().cancel()
        binding.smartRoutingCard.scaleX = 0.97f
        binding.smartRoutingCard.scaleY = 0.97f
        binding.smartRoutingCard.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(280L)
            .setInterpolator(OvershootInterpolator(1.25f))
            .start()
        ObjectAnimator.ofFloat(binding.smartRoutingIcon, View.ROTATION, 0f, 180f, 360f).apply {
            duration = 520L
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNav.selectedItemId = R.id.nav_home
        refreshDashboard()
        refreshSmartRouting()
        startLogoAnimation()
        applyRunningState(false, mainViewModel.isRunning.value == true)
    }

    fun refreshDashboard() {
        val selected = MmkvManager.getSelectServer()
            ?.let(MmkvManager::decodeServerConfig)
        val presentation = HotfoxServerPresentation.fromRemark(selected?.remarks)
        val selectedLabel = presentation.title
            .takeIf { it.isNotBlank() && it != "—" }
            ?: getString(R.string.hotfox_server_not_selected)
        binding.tvSelectedServer.text = selectedLabel
        val affiliation = MmkvManager.getSelectServer()?.let(MmkvManager::decodeServerAffiliationInfo)
        val delayMs = affiliation?.testDelayMillis
        val delayLabel = when {
            delayMs == null || delayMs <= 0L -> getString(R.string.hotfox_ms_unknown)
            else -> affiliation.getTestDelayString()
        }
        val countryOrHint = presentation.country
            ?: selected?.description?.takeIf { it.isNotBlank() }
        binding.tvSelectedServerHint.text = listOfNotNull(countryOrHint, delayLabel).joinToString(" · ")
            .ifBlank { getString(R.string.hotfox_choose_location) }
        if (lastServerLabel != null && lastServerLabel != selectedLabel) {
            binding.selectedServerCard.alpha = 0.55f
            binding.selectedServerCard.animate().alpha(1f).setDuration(280L).start()
        }
        lastServerLabel = selectedLabel

        val subscription = selected?.subscriptionId
            ?.takeIf { it.isNotBlank() }
            ?.let(MmkvManager::decodeSubscription)
        if (subscription == null) {
            subscriptionUrlForCopy = null
            binding.tvSubscriptionTraffic.setText(R.string.hotfox_traffic_unknown)
            binding.tvSubscriptionExpire.text = "—"
            binding.tvSubscriptionRemaining.text = getString(R.string.hotfox_expiry_unknown)
            binding.tvSubscriptionUrl.text = "—"
            binding.tvSubscriptionServers.text = "0"
            binding.tvSubscriptionState.text = "○ Не синхронизирована"
            binding.tvSubscriptionState.setTextColor(ContextCompat.getColor(this, R.color.hotfox_editorial_text_dim))
            return
        }

        val used = (subscription.uploadBytes + subscription.downloadBytes).coerceAtLeast(0L)
        subscriptionUrlForCopy = subscription.url
        binding.tvSubscriptionUrl.text = HotfoxTrafficFormatter.maskSubscriptionUrl(subscription.url)
        binding.tvSubscriptionServers.text = MmkvManager.decodeServerList(selected?.subscriptionId.orEmpty()).size.toString()
        val shown = HotfoxSubscriptionPresentation.fromExpiryEpochSeconds(
            expireAtEpochSeconds = subscription.expireAtEpochSeconds,
            serverCount = MmkvManager.decodeServerList(selected?.subscriptionId.orEmpty()).size,
        )
        binding.tvSubscriptionState.text = when (shown.status) {
            SubscriptionPresentation.Status.ACTIVE -> "● Активна"
            SubscriptionPresentation.Status.EXPIRED -> "● Истекла"
            SubscriptionPresentation.Status.UNKNOWN -> "○ Статус неизвестен"
            SubscriptionPresentation.Status.MISSING -> "○ Не синхронизирована"
        }
        binding.tvSubscriptionState.setTextColor(
            ContextCompat.getColor(
                this,
                when (shown.status) {
                    SubscriptionPresentation.Status.ACTIVE -> R.color.hotfox_success_bright
                    SubscriptionPresentation.Status.EXPIRED -> R.color.hotfox_orange
                    else -> R.color.hotfox_editorial_text_dim
                },
            )
        )
        binding.tvSubscriptionTraffic.text = if (subscription.totalBytes > 0L) {
            getString(
                R.string.hotfox_traffic_value,
                Formatter.formatFileSize(this, used),
                Formatter.formatFileSize(this, subscription.totalBytes),
            )
        } else {
            getString(R.string.hotfox_traffic_unknown)
        }
        binding.tvSubscriptionExpire.text = shown.expiryLabel ?: "—"
        binding.tvSubscriptionRemaining.text = when {
            shown.status == SubscriptionPresentation.Status.UNKNOWN || shown.remainingDays == null ->
                getString(R.string.hotfox_expiry_unknown)
            shown.remainingDays == 0 -> getString(R.string.hotfox_days_left, 0)
            else -> getString(R.string.hotfox_days_left, shown.remainingDays)
        }
    }

    override fun onPause() {
        super.onPause()
        logoFloatAnimator?.cancel()
        logoFloatAnimator = null
        binding.brandLogo.translationY = 0f
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        val searchItem = menu.findItem(R.id.search_view)
        if (searchItem != null) {
            val searchView = searchItem.actionView as SearchView
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean = false

                override fun onQueryTextChange(newText: String?): Boolean {
                    mainViewModel.filterConfig(newText.orEmpty())
                    return false
                }
            })

            searchView.setOnCloseListener {
                mainViewModel.filterConfig("")
                false
            }
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.import_qrcode -> {
            importQRcode()
            true
        }

        R.id.import_clipboard -> {
            importClipboard()
            true
        }

        R.id.import_local -> {
            importConfigLocal()
            true
        }

        R.id.import_manually_policy_group -> {
            importManually(EConfigType.POLICYGROUP.value)
            true
        }

        R.id.import_manually_proxy_chain -> {
            importManually(EConfigType.PROXYCHAIN.value)
            true
        }

        R.id.import_manually_vmess -> {
            importManually(EConfigType.VMESS.value)
            true
        }

        R.id.import_manually_vless -> {
            importManually(EConfigType.VLESS.value)
            true
        }

        R.id.import_manually_ss -> {
            importManually(EConfigType.SHADOWSOCKS.value)
            true
        }

        R.id.import_manually_socks -> {
            importManually(EConfigType.SOCKS.value)
            true
        }

        R.id.import_manually_http -> {
            importManually(EConfigType.HTTP.value)
            true
        }

        R.id.import_manually_trojan -> {
            importManually(EConfigType.TROJAN.value)
            true
        }

        R.id.import_manually_wireguard -> {
            importManually(EConfigType.WIREGUARD.value)
            true
        }

        R.id.import_manually_hysteria2 -> {
            importManually(EConfigType.HYSTERIA2.value)
            true
        }

        R.id.export_all -> {
            exportAll()
            true
        }

        R.id.real_ping_all -> {
            toast(getString(R.string.connection_test_testing_count, mainViewModel.serversCache.count()))
            mainViewModel.testAllRealPing()
            true
        }

        R.id.service_restart -> {
            restartV2Ray()
            true
        }

        R.id.del_all_config -> {
            delAllConfig()
            true
        }

        R.id.del_duplicate_config -> {
            delDuplicateConfig()
            true
        }

        R.id.del_invalid_config -> {
            delInvalidConfig()
            true
        }

        R.id.sort_by_test_results -> {
            sortByTestResults()
            true
        }

        R.id.sub_update -> {
            importConfigViaSub()
            true
        }

        R.id.locate_selected_config -> {
            locateSelectedServer()
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    private fun importManually(createConfigType: Int) {
        if (createConfigType == EConfigType.POLICYGROUP.value) {
            startActivity(
                Intent()
                    .putExtra("subscriptionId", mainViewModel.subscriptionId)
                    .setClass(this, ServerGroupActivity::class.java)
            )
        } else if (createConfigType == EConfigType.PROXYCHAIN.value) {
            startActivity(
                Intent()
                    .putExtra("subscriptionId", mainViewModel.subscriptionId)
                    .setClass(this, ServerProxyChainActivity::class.java)
            )
        } else {
            startActivity(
                Intent()
                    .putExtra("createConfigType", createConfigType)
                    .putExtra("subscriptionId", mainViewModel.subscriptionId)
                    .setClass(this, ServerActivity::class.java)
            )
        }
    }

    /**
     * import config from qrcode
     */
    private fun importQRcode(): Boolean {
        launchQRCodeScanner { scanResult ->
            if (scanResult != null) {
                importBatchConfig(scanResult)
            }
        }
        return true
    }

    /**
     * import config from clipboard
     */
    private fun importClipboard()
            : Boolean {
        try {
            val clipboard = Utils.getClipboard(this)
            importBatchConfig(clipboard)
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to import config from clipboard", e)
            return false
        }
        return true
    }

    private fun importBatchConfig(server: String?) {
        showLoading()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val (count, countSub) = AngConfigManager.importBatchConfig(server, mainViewModel.subscriptionId, true)
                delay(500L)
                withContext(Dispatchers.Main) {
                    when {
                        count > 0 -> {
                            toast(getString(R.string.title_import_config_count, count))
                            mainViewModel.reloadServerList()
                            refreshGroupTabTitles()
                        }

                        countSub > 0 -> { setupGroupTab(); importConfigViaSub() }
                        else -> toastError(R.string.toast_failure)
                    }
                    hideLoading()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    toastError(R.string.toast_failure)
                    hideLoading()
                }
                LogUtil.e(AppConfig.TAG, "Failed to import batch config", e)
            }
        }
    }

    /**
     * import config from local config file
     */
    private fun importConfigLocal(): Boolean {
        try {
            showFileChooser()
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to import config from local file", e)
            return false
        }
        return true
    }


    /**
     * import config from sub
     */
    fun importConfigViaSub(): Boolean {
        showLoading()

        lifecycleScope.launch(Dispatchers.IO) {
            val result = mainViewModel.updateConfigViaSubAll()
            delay(500L)
            launch(Dispatchers.Main) {
                if (result.successCount + result.failureCount + result.skipCount == 0) {
                    toast(R.string.title_update_subscription_no_subscription)
                } else if (result.successCount > 0 && result.failureCount + result.skipCount == 0) {
                    toast(getString(R.string.title_update_config_count, result.configCount))
                } else {
                    toast(
                        getString(
                            R.string.title_update_subscription_result,
                            result.configCount, result.successCount, result.failureCount, result.skipCount
                        )
                    )
                }
                if (result.configCount > 0) {
                    mainViewModel.reloadServerList()
                    refreshGroupTabTitles()
                    refreshDashboard()
                }
                hideLoading()
            }
        }
        return true
    }

    private fun exportAll() {
        showLoading()
        lifecycleScope.launch(Dispatchers.IO) {
            val ret = mainViewModel.exportAllServer()
            launch(Dispatchers.Main) {
                if (ret > 0)
                    toast(getString(R.string.title_export_config_count, ret))
                else
                    toastError(R.string.toast_failure)
                hideLoading()
            }
        }
    }

    private fun delAllConfig() {
        AlertDialog.Builder(this).setMessage(R.string.del_config_comfirm)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                showLoading()
                lifecycleScope.launch(Dispatchers.IO) {
                    val ret = mainViewModel.removeAllServer()
                    launch(Dispatchers.Main) {
                        mainViewModel.reloadServerList()
                        refreshGroupTabTitles()
                        toast(getString(R.string.title_del_config_count, ret))
                        hideLoading()
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                //do noting
            }
            .show()
    }

    private fun delDuplicateConfig() {
        AlertDialog.Builder(this).setMessage(R.string.del_config_comfirm)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                showLoading()
                lifecycleScope.launch(Dispatchers.IO) {
                    val ret = mainViewModel.removeDuplicateServer()
                    launch(Dispatchers.Main) {
                        mainViewModel.reloadServerList()
                        refreshGroupTabTitles()
                        toast(getString(R.string.title_del_duplicate_config_count, ret))
                        hideLoading()
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                //do noting
            }
            .show()
    }

    private fun delInvalidConfig() {
        AlertDialog.Builder(this).setMessage(R.string.del_invalid_config_comfirm)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                showLoading()
                lifecycleScope.launch(Dispatchers.IO) {
                    val ret = mainViewModel.removeInvalidServer()
                    launch(Dispatchers.Main) {
                        mainViewModel.reloadServerList()
                        refreshGroupTabTitles()
                        toast(getString(R.string.title_del_config_count, ret))
                        hideLoading()
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                //do noting
            }
            .show()
    }

    private fun sortByTestResults() {
        showLoading()
        lifecycleScope.launch(Dispatchers.IO) {
            mainViewModel.sortByTestResults()
            launch(Dispatchers.Main) {
                mainViewModel.reloadServerList()
                hideLoading()
            }
        }
    }

    /**
     * show file chooser
     */
    private fun showFileChooser() {
        launchFileChooser { uri ->
            if (uri == null) {
                return@launchFileChooser
            }

            readContentFromUri(uri)
        }
    }

    /**
     * read content from uri
     */
    private fun readContentFromUri(uri: Uri) {
        try {
            contentResolver.openInputStream(uri).use { input ->
                importBatchConfig(input?.bufferedReader()?.readText())
            }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to read content from URI", e)
        }
    }

    /**
     * Locates and scrolls to the currently selected server.
     * If the selected server is in a different group, automatically switches to that group first.
     */
    private fun locateSelectedServer() {
        val targetSubscriptionId = mainViewModel.findSubscriptionIdBySelect()
        if (targetSubscriptionId.isNullOrEmpty()) {
            toast(R.string.title_file_chooser)
            return
        }

        val targetGroupIndex = groupPagerAdapter.groups.indexOfFirst { it.id == targetSubscriptionId }
        if (targetGroupIndex < 0) {
            toast(R.string.toast_server_not_found_in_group)
            return
        }

        // Switch to target group if needed, then scroll to the server
        if (binding.viewPager.currentItem != targetGroupIndex) {
            binding.viewPager.setCurrentItem(targetGroupIndex, true)
            binding.viewPager.postDelayed({ scrollToSelectedServer(targetGroupIndex) }, 1000)
        } else {
            scrollToSelectedServer(targetGroupIndex)
        }
    }

    /**
     * Scrolls to the selected server in the specified fragment.
     * @param groupIndex The index of the group/fragment to scroll in
     */
    private fun scrollToSelectedServer(groupIndex: Int) {
        val itemId = groupPagerAdapter.getItemId(groupIndex)
        val fragment = supportFragmentManager.findFragmentByTag("f$itemId") as? GroupServerFragment

        if (fragment?.isAdded == true && fragment.view != null) {
            fragment.scrollToSelectedServer()
        } else {
            toast(R.string.toast_fragment_not_available)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_BUTTON_B) {
            moveTaskToBack(false)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }


    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.sub_setting -> requestActivityLauncher.launch(Intent(this, SubSettingActivity::class.java))
            R.id.renewal -> startActivity(Intent(this, RenewalActivity::class.java))
            R.id.per_app_proxy_settings -> requestActivityLauncher.launch(Intent(this, PerAppProxyActivity::class.java))
            R.id.routing_setting -> requestActivityLauncher.launch(Intent(this, RoutingSettingActivity::class.java))
            R.id.user_asset_setting -> requestActivityLauncher.launch(Intent(this, UserAssetActivity::class.java))
            R.id.settings -> requestActivityLauncher.launch(Intent(this, SettingsActivity::class.java))
            R.id.about -> startActivity(Intent(this, AboutActivity::class.java))
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onDestroy() {
        tabMediator?.detach()
        haloAnimator?.cancel()
        logoFloatAnimator?.cancel()
        connectionClockJob?.cancel()
        super.onDestroy()
    }
}
