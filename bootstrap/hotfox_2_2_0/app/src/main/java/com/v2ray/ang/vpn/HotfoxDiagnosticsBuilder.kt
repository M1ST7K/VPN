package com.v2ray.ang.vpn

import com.v2ray.ang.BuildConfig
import com.v2ray.ang.util.SecretRedactor

object HotfoxDiagnosticsBuilder {
    fun build(
        androidRelease: String,
        api: Int,
        abi: String,
        socksPort: Int,
        socksReady: Boolean?,
        hevRunning: Boolean?,
        ipv4Captured: Boolean?,
        ipv6Captured: Boolean?,
        ipv6Policy: String,
        routingMode: String,
        serverRemark: String?,
        uploaded: Long?,
        downloaded: Long?,
        lastError: String?,
        serverCount: Int,
        path: VpnPathVerification? = VpnSessionCoordinator.lastPath(),
    ): String {
        val state = VpnSessionCoordinator.currentState()
        val raw = buildString {
            appendLine("HotFox Proxy ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("distribution=${BuildConfig.DISTRIBUTION}")
            appendLine("android=$androidRelease api=$api abi=$abi")
            appendLine("state=${state.name}")
            appendLine("uiPhase=${state.uiPhase()}")
            appendLine("protected=${state.isProtected()}")
            appendLine("routing=$routingMode")
            appendLine("server=${serverRemark ?: "—"}")
            appendLine("socks=127.0.0.1:$socksPort ready=${formatTriState(socksReady)}")
            appendLine("socks5=${formatTriState(path?.socks5Ready ?: socksReady)}")
            appendLine("hev=${formatTriState(hevRunning)}")
            appendLine("hevProgressed=${formatTriState(path?.hevProgressed)}")
            appendLine("tunForwarded=${formatTriState(path?.tunForwarded)}")
            appendLine("xrayEgressMs=${path?.xrayEgressMs ?: "unknown"}")
            appendLine("pathVerified=${path?.verified ?: "unknown"}")
            appendLine("pathBackend=${path?.backend ?: "unknown"}")
            appendLine("pathReason=${path?.reason ?: "none"}")
            appendLine("ipv4Captured=${formatTriState(ipv4Captured)}")
            appendLine("ipv6Captured=${formatTriState(ipv6Captured)}")
            appendLine("ipv6Policy $ipv6Policy")
            appendLine("autoReason=${HotfoxServerSelection.lastAutoReason.ifBlank { "none" }}")
            val auto = HotfoxServerSelection.diagnosticSnapshot()
            appendLine("autoCandidates=${auto.candidateCount}")
            appendLine("autoEligible=${auto.eligibleCount}")
            appendLine("autoFiltered=${auto.filteredCount}")
            appendLine("autoNetworkContext=${auto.networkContext}")
            appendLine("autoLastGood=${if (auto.lastGoodPresent) "present" else "none"}")
            appendLine("shadowAuto=${HotfoxShadowStore.isShadowAuto()}")
            appendLine("shadowPath=${HotfoxShadowStore.lastPathId.ifBlank { "none" }}")
            appendLine("shadowReason=${HotfoxShadowStore.lastDecisionReason.ifBlank { "none" }}")
            appendLine("e2e=${VpnPathVerification.PHYSICAL_E2E_NOT_EXECUTED}")
            appendLine("uploaded=${uploaded ?: "—"} downloaded=${downloaded ?: "—"}")
            appendLine("servers=$serverCount")
            appendLine("lastError=${lastError ?: "none"}")
            appendLine("stage=${VpnSessionCoordinator.lastStage().code}")
            appendLine("attempt=${VpnSessionCoordinator.currentAttempt()}")
        }
        return SecretRedactor.redact(raw)
    }

    private fun formatTriState(value: Boolean?): String = when (value) {
        true -> "true"
        false -> "false"
        null -> "unknown"
    }
}
