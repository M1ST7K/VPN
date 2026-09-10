package com.v2ray.ang.vpn

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import androidx.core.content.ContextCompat
import com.v2ray.ang.AppConfig
import com.v2ray.ang.core.CoreServiceManager
import com.v2ray.ang.handler.AngConfigManager
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.service.CoreProxyOnlyService
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.SecretRedactor
import java.io.File
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Debug-only engineering-runtime VPN E2E. The subscription URL is read from a
 * device file, never logged, and deleted immediately. Not physical-device
 * acceptance and never a substitute for [VpnPathVerification.PHYSICAL_E2E_NOT_EXECUTED].
 */
object HotfoxEngineeringRuntimeE2e {
    const val ACTION = "com.hotfox.vpn.action.ENGINEERING_RUNTIME_E2E"
    const val SUB_FILE = "/data/local/tmp/hotfox-e2e-sub.url"
    const val REPORT_NAME = "hotfox-e2e-report.txt"
    const val LOG_PREFIX = "HotfoxEngineeringE2e"

    data class Cycle(
        val index: Int,
        val protected: Boolean,
        val state: String,
        val lastError: String?,
        val socksHttps: Boolean?,
        val tunHttp4: Boolean?,
        val tunHttp6: Boolean?,
        val ipDuringSocks: String?,
        val ipDuringTun: String?,
    )

    fun run(context: Context, cycles: Int): String {
        val url = readAndDeleteSubscriptionUrl()
            ?: return finish("FAIL", "missing subscription file", emptyList(), null, null)
        val imported = runCatching {
            AngConfigManager.importBatchConfig(url, "", true)
        }.getOrElse { error ->
            LogUtil.e(AppConfig.TAG, "$LOG_PREFIX: import failed: ${error.javaClass.simpleName}")
            return finish("FAIL", "import-exception", emptyList(), null, null, socksOnlyHttps = null)
        }
        val (count, countSub) = imported
        if (count <= 0 && countSub <= 0) {
            return finish("FAIL", "import-empty", emptyList(), null, null, socksOnlyHttps = null)
        }
        val guid = HotfoxServerSelection.firstUsableGuid()
            ?: return finish("FAIL", "no-servers", emptyList(), null, null)
        HotfoxServerSelection.selectManual(guid)
        val ipBefore = fetchDirectIp()
        val socksOnly = runSocksOnlyIsolation(context, guid)
        if (!socksOnly) {
            return finish(
                result = "FAIL",
                reason = "socks-only-https",
                cycles = emptyList(),
                ipBefore = ipBefore,
                ipAfter = fetchDirectIp(),
                socksOnlyHttps = false,
            )
        }
        val results = ArrayList<Cycle>()
        val requested = cycles.coerceIn(1, 5)
        repeat(requested) { index ->
            if (index > 0) {
                CoreServiceManager.stopVService(context)
                runCatching { kotlinx.coroutines.runBlocking { VpnSessionCoordinator.awaitIdle(12_000L) } }
            }
            runCatching { CoreServiceManager.startVService(context, guid) }
                .onFailure { error ->
                    LogUtil.e(AppConfig.TAG, "$LOG_PREFIX: start failed: ${error.javaClass.simpleName}")
                }
            val protectedNow = waitState(VpnSessionState.CONNECTED, 45_000L)
            val isolation = HotfoxSocksIsolation.last
            val ipSocks = if (protectedNow || isolation?.socksHttps == true) {
                fetchIpViaSocks(SettingsManager.getSocksPort())
            } else {
                null
            }
            val ipTun = if (protectedNow) fetchIpViaVpn(context) else null
            val cycle = Cycle(
                index = index + 1,
                protected = protectedNow,
                state = VpnSessionCoordinator.currentState().name,
                lastError = VpnSessionCoordinator.lastError(),
                socksHttps = isolation?.socksHttps,
                tunHttp4 = HotfoxTunLayerEvidence.tunHttp4,
                tunHttp6 = HotfoxTunLayerEvidence.tunHttp6,
                ipDuringSocks = ipSocks,
                ipDuringTun = ipTun,
            )
            results += cycle
        }
        CoreServiceManager.stopVService(context)
        runCatching { kotlinx.coroutines.runBlocking { VpnSessionCoordinator.awaitIdle(12_000L) } }
        val ipAfter = fetchDirectIp()
        val (result, reason) = HotfoxEngineeringE2eGate.outcome(
            socksOnlyHttps = true,
            protectedCycles = results.count { it.protected },
            requestedCycles = requested,
        )
        return finish(result, reason, results, ipBefore, ipAfter, socksOnlyHttps = true)
    }

    fun writeReport(context: Context, report: String) {
        val text = SecretRedactor.redact(report)
        runCatching { File(context.filesDir, REPORT_NAME).writeText(text) }
        runCatching { context.getExternalFilesDir(null)?.let { File(it, REPORT_NAME).writeText(text) } }
    }

    private fun finish(
        result: String,
        reason: String,
        cycles: List<Cycle>,
        ipBefore: String?,
        ipAfter: String?,
        socksOnlyHttps: Boolean? = null,
    ): String {
        val during = cycles.firstOrNull { it.protected }?.ipDuringSocks
            ?: cycles.firstOrNull()?.ipDuringSocks
        val report = buildString {
            appendLine("engineeringRuntimeE2e=$result")
            appendLine("physicalDeviceE2e=${VpnPathVerification.PHYSICAL_E2E_NOT_EXECUTED}")
            appendLine("reason=$reason")
            appendLine("socksOnlyHttps=${socksOnlyHttps ?: "none"}")
            appendLine("cycles=${cycles.size}")
            appendLine("protectedCycles=${cycles.count { it.protected }}")
            appendLine("ipBefore=${HotfoxIpEvidence.redact(ipBefore)}")
            appendLine("ipDuring=${HotfoxIpEvidence.redact(during)}")
            appendLine("ipAfter=${HotfoxIpEvidence.redact(ipAfter)}")
            appendLine("ipChanged=${HotfoxIpEvidence.changed(ipBefore, during)}")
            appendLine(VpnProtectEvidence.summary())
            appendLine(TunFdEvidence.summary())
            appendLine(HotfoxTunLayerEvidence.summary())
            appendLine(HotfoxSocksIsolation.last?.summary() ?: "socksHttps=none")
            appendLine(HotfoxOutboundCompare.last?.summary() ?: "generatedPresent=false")
            cycles.forEach { cycle ->
                appendLine(
                    "cycle=${cycle.index} protected=${cycle.protected} state=${cycle.state} " +
                        "socksHttps=${cycle.socksHttps ?: "none"} tunHttp4=${cycle.tunHttp4 ?: "none"} " +
                        "error=${cycle.lastError ?: "none"}",
                )
            }
        }
        LogUtil.i(AppConfig.TAG, "$LOG_PREFIX: RESULT=$result reason=$reason")
        return SecretRedactor.redact(report)
    }

    private fun readAndDeleteSubscriptionUrl(): String? {
        val file = File(SUB_FILE)
        if (!file.isFile) return null
        val url = runCatching { file.readText() }.getOrNull()?.trim().orEmpty()
        runCatching { file.delete() }
        if (url.isEmpty()) return null
        if (!url.startsWith("https://", ignoreCase = true) &&
            !url.startsWith("http://", ignoreCase = true)
        ) {
            return null
        }
        return url
    }

    private fun runSocksOnlyIsolation(context: Context, guid: String): Boolean {
        HotfoxServerSelection.selectManual(guid)
        val started = runCatching {
            ContextCompat.startForegroundService(
                context.applicationContext,
                Intent(context.applicationContext, CoreProxyOnlyService::class.java),
            )
            true
        }.getOrElse { error ->
            LogUtil.e(AppConfig.TAG, "$LOG_PREFIX: proxy-only start failed: ${error.javaClass.simpleName}")
            false
        }
        if (!started) return false
        val proxyOnly = waitState(VpnSessionState.PROXY_ONLY, 45_000L)
        val isolation = HotfoxSocksIsolation.last
        val proven = isolation?.socksOnlyPathProven == true && isolation.socksHttps
        CoreServiceManager.stopVService(context)
        runCatching { kotlinx.coroutines.runBlocking { VpnSessionCoordinator.awaitIdle(12_000L) } }
        LogUtil.i(
            AppConfig.TAG,
            "$LOG_PREFIX: socks-only proxyOnly=$proxyOnly proven=$proven ${isolation?.summary() ?: "none"}",
        )
        return proxyOnly && proven
    }

    private fun waitState(wanted: VpnSessionState, timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val state = VpnSessionCoordinator.currentState()
            if (state == wanted) return true
            if (state == VpnSessionState.ERROR) return false
            Thread.sleep(400L)
        }
        return VpnSessionCoordinator.currentState() == wanted
    }

    private fun fetchDirectIp(): String? = fetchIp(OkHttpClient.Builder().proxy(java.net.Proxy.NO_PROXY))

    private fun fetchIpViaSocks(port: Int): String? {
        if (port <= 0) return null
        return fetchIp(
            OkHttpClient.Builder().proxy(
                java.net.Proxy(java.net.Proxy.Type.SOCKS, InetSocketAddress(AppConfig.LOOPBACK, port)),
            ),
        )
    }

    private fun fetchIpViaVpn(context: Context): String? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return null
        val vpn = VpnReadiness.findVpnNetwork(cm) ?: return null
        return fetchIp(OkHttpClient.Builder().socketFactory(vpn.socketFactory).proxy(java.net.Proxy.NO_PROXY))
    }

    private fun fetchIp(builder: OkHttpClient.Builder): String? {
        val client = builder
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .callTimeout(8, TimeUnit.SECONDS)
            .followRedirects(false)
            .retryOnConnectionFailure(false)
            .build()
        return runCatching {
            client.newCall(
                Request.Builder().url("https://api.ipify.org").header("Cache-Control", "no-cache").build(),
            ).execute().use { response ->
                if (!response.isSuccessful) return@use null
                response.body?.string()?.trim()?.takeIf { it.length in 3..64 }
            }
        }.getOrNull()
    }
}
