package com.v2ray.ang.vpn

/**
 * Broad failure categories and a real recovery action when one exists.
 * Never dumps secrets or full Xray config.
 */
enum class DoctorCategory {
    NO_INTERNET,
    VPN_PERMISSION,
    ENTITLEMENT,
    SERVER_UNAVAILABLE,
    PRIMARY_TRANSPORT,
    ALTERNATE_AVAILABLE,
    DNS_BOOTSTRAP,
    CONFIG_UNSUPPORTED,
    LOCAL_PIPELINE,
}

enum class DoctorAction {
    NONE,
    AUTO_FIX,
}

data class DoctorFinding(
    val category: DoctorCategory,
    val title: String,
    val detail: String,
    val action: DoctorAction,
    val code: String,
)

object ConnectionDoctor {
    fun diagnose(
        hasUnderlyingInternet: Boolean,
        vpnPermissionGranted: Boolean,
        entitlementUsable: Boolean,
        primaryPathAvailable: Boolean,
        alternatePathAvailable: Boolean,
        dnsBootstrapReady: Boolean,
        configSupported: Boolean,
        localPipelineOk: Boolean,
        primaryTransportAvailable: Boolean = true,
    ): DoctorFinding {
        if (!hasUnderlyingInternet) {
            return finding(DoctorCategory.NO_INTERNET, "HF-DOC-001", "Нет доступа в Интернет", "Проверьте Wi‑Fi или мобильные данные.", DoctorAction.NONE)
        }
        if (!vpnPermissionGranted) {
            return finding(DoctorCategory.VPN_PERMISSION, "HF-DOC-002", "Нет разрешения VPN", "Разрешите HotFox создать VPN-подключение.", DoctorAction.NONE)
        }
        if (!entitlementUsable) {
            return finding(DoctorCategory.ENTITLEMENT, "HF-DOC-003", "Нет доступа HotFox", "Подписка не подтверждена backend.", DoctorAction.NONE)
        }
        if (!configSupported) {
            return finding(DoctorCategory.CONFIG_UNSUPPORTED, "HF-DOC-004", "Конфигурация не поддерживается", "Этот транспорт или security mode клиент не настраивает.", DoctorAction.NONE)
        }
        if (!dnsBootstrapReady) {
            return finding(DoctorCategory.DNS_BOOTSTRAP, "HF-DOC-005", "DNS bootstrap недоступен", "Будет использован кэш адресов с TTL, без обхода VPN.", DoctorAction.AUTO_FIX)
        }
        if (!localPipelineOk) {
            return finding(DoctorCategory.LOCAL_PIPELINE, "HF-DOC-006", "Сбой локального VPN-пути", "TUN/HEV/Xray не подтвердили путь.", DoctorAction.AUTO_FIX)
        }
        if (!primaryTransportAvailable && alternatePathAvailable) {
            return finding(DoctorCategory.ALTERNATE_AVAILABLE, "HF-DOC-007", "Основной маршрут недоступен", "Есть запасной защищённый маршрут.", DoctorAction.AUTO_FIX)
        }
        if (!primaryTransportAvailable) {
            return finding(DoctorCategory.PRIMARY_TRANSPORT, "HF-DOC-009", "Основной транспорт недоступен", "Попробуем другой поддерживаемый транспорт.", DoctorAction.AUTO_FIX)
        }
        if (!primaryPathAvailable && alternatePathAvailable) {
            return finding(DoctorCategory.ALTERNATE_AVAILABLE, "HF-DOC-007", "Основной маршрут недоступен", "Есть запасной защищённый маршрут.", DoctorAction.AUTO_FIX)
        }
        if (!primaryPathAvailable) {
            return finding(DoctorCategory.SERVER_UNAVAILABLE, "HF-DOC-008", "Сервер недоступен", "Нет живого кандидата AUTO.", DoctorAction.NONE)
        }
        return finding(DoctorCategory.PRIMARY_TRANSPORT, "HF-DOC-000", "Соединение в порядке", "Канонический путь подтверждён.", DoctorAction.NONE)
    }

    /**
     * Invokes real bounded recovery: Shadow fallback when AUTO, otherwise stop.
     * Generation must still be current; user disconnect cancels via [VpnRestartGate].
     */
    fun autoFix(
        finding: DoctorFinding,
        paths: List<ConnectionPath>,
        failedPath: ConnectionPath?,
        cache: NetworkCapabilityCache,
        serverScores: Map<String, Double>,
        auto: Boolean,
        shadowAuto: Boolean,
        attempt: Int,
        nowEpochMs: Long,
        networkContext: Long,
        generation: Long,
        currentGeneration: Long,
    ): HotfoxShadowPolicy.Decision {
        if (finding.action != DoctorAction.AUTO_FIX) {
            return HotfoxShadowPolicy.Decision(FailoverAction.STOP, failedPath, "doctor_no_autofix", attempt)
        }
        if (generation != 0L && !VpnRestartGate.isCurrent(generation)) {
            return HotfoxShadowPolicy.Decision(FailoverAction.STOP, failedPath, "doctor_stale_restart", attempt)
        }
        return HotfoxShadowPolicy.fallback(
            paths = paths,
            failedPath = failedPath,
            cache = cache,
            serverScores = serverScores,
            auto = auto,
            shadowAuto = shadowAuto,
            attempt = attempt,
            nowEpochMs = nowEpochMs,
            networkContext = networkContext,
            generation = generation,
            currentGeneration = currentGeneration,
        )
    }

    private fun finding(
        category: DoctorCategory,
        code: String,
        title: String,
        detail: String,
        action: DoctorAction,
    ) = DoctorFinding(category, title, detail, action, code)
}
