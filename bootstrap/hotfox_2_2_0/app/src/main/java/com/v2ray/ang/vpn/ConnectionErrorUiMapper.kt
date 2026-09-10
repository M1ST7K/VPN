package com.v2ray.ang.vpn

/**
 * User-facing error copy. Diagnostic codes stay in [Presentation.diagnosticCode],
 * never as the headline. Secrets and subscription URLs are not copied through.
 */
object ConnectionErrorUiMapper {
    enum class PrimaryAction {
        RETRY,
        GRANT_VPN,
        OPEN_SUBSCRIPTION,
        NONE,
    }

    data class Presentation(
        val titleResName: String,
        val detailResName: String,
        val primaryAction: PrimaryAction,
        val diagnosticCode: String,
    )

    fun fromLastError(error: String?): Presentation {
        val raw = error?.trim().orEmpty()
        val code = sanitizedCode(raw)
        val lower = raw.lowercase()
        return when {
            looksLikeSecret(raw) -> generic(code = "")
            raw.contains("HF-VPN-012") || raw.contains("process-bind") ->
                Presentation(
                    titleResName = "hotfox_error_loop_title",
                    detailResName = "hotfox_error_loop_detail",
                    primaryAction = PrimaryAction.RETRY,
                    diagnosticCode = "HF-VPN-012",
                )
            raw.contains("HF-VPN-014") || raw.contains("socks-outbound") ->
                Presentation(
                    titleResName = "hotfox_error_socks_title",
                    detailResName = "hotfox_error_socks_detail",
                    primaryAction = PrimaryAction.RETRY,
                    diagnosticCode = "HF-VPN-014",
                )
            raw.contains("permission") || raw.contains("VPN_PERMISSION") ->
                Presentation(
                    titleResName = "hotfox_error_permission_title",
                    detailResName = "hotfox_error_permission_detail",
                    primaryAction = PrimaryAction.GRANT_VPN,
                    diagnosticCode = code.ifBlank { "HF-DOC-002" },
                )
            lower.contains("expired") ->
                Presentation(
                    titleResName = "hotfox_error_expired_title",
                    detailResName = "hotfox_error_expired_detail",
                    primaryAction = PrimaryAction.OPEN_SUBSCRIPTION,
                    diagnosticCode = code.ifBlank { "EXPIRED" },
                )
            lower.contains("entitlement") ->
                Presentation(
                    titleResName = "hotfox_error_entitlement_title",
                    detailResName = "hotfox_error_entitlement_detail",
                    primaryAction = PrimaryAction.OPEN_SUBSCRIPTION,
                    diagnosticCode = code.ifBlank { "HF-DOC-003" },
                )
            lower.contains("backend_unavailable") || lower.contains("backend unavailable") ->
                Presentation(
                    titleResName = "hotfox_error_backend_title",
                    detailResName = "hotfox_error_backend_detail",
                    primaryAction = PrimaryAction.OPEN_SUBSCRIPTION,
                    diagnosticCode = code.ifBlank { "BACKEND_UNAVAILABLE" },
                )
            lower.contains("wait_for_network") || lower.contains("no network") || raw.contains("Нет сети") ->
                Presentation(
                    titleResName = "hotfox_error_network_title",
                    detailResName = "hotfox_error_network_detail",
                    primaryAction = PrimaryAction.RETRY,
                    diagnosticCode = code.ifBlank { "WAIT_FOR_NETWORK" },
                )
            lower.contains("no_target") || lower.contains("no eligible") || raw.contains("Нет доступного сервера") ->
                Presentation(
                    titleResName = "hotfox_error_no_target_title",
                    detailResName = "hotfox_error_no_target_detail",
                    primaryAction = PrimaryAction.OPEN_SUBSCRIPTION,
                    diagnosticCode = code.ifBlank { "NO_TARGET" },
                )
            lower.contains("exhausted") ->
                Presentation(
                    titleResName = "hotfox_error_exhausted_title",
                    detailResName = "hotfox_error_exhausted_detail",
                    primaryAction = PrimaryAction.RETRY,
                    diagnosticCode = code.ifBlank { "budget_exhausted" },
                )
            raw.contains("tun-not-forwarded") || raw.contains("tun-datapath") ->
                Presentation(
                    titleResName = "hotfox_error_tun_title",
                    detailResName = "hotfox_error_tun_detail",
                    primaryAction = PrimaryAction.RETRY,
                    diagnosticCode = code.ifBlank { "tun-datapath" },
                )
            lower.contains("dns") || lower.contains("bootstrap") ->
                Presentation(
                    titleResName = "hotfox_error_dns_title",
                    detailResName = "hotfox_error_dns_detail",
                    primaryAction = PrimaryAction.RETRY,
                    diagnosticCode = code.ifBlank { "dns-bootstrap" },
                )
            lower.contains("captive") || raw.contains("авторизации") ->
                Presentation(
                    titleResName = "hotfox_error_captive_title",
                    detailResName = "hotfox_error_captive_detail",
                    primaryAction = PrimaryAction.NONE,
                    diagnosticCode = code.ifBlank { "WAIT_FOR_CAPTIVE_PORTAL" },
                )
            lower.contains("shadow") ->
                Presentation(
                    titleResName = "hotfox_error_shadow_title",
                    detailResName = "hotfox_error_shadow_detail",
                    primaryAction = PrimaryAction.NONE,
                    diagnosticCode = code.ifBlank { "shadow-recovery" },
                )
            lower.contains("update required") || raw.contains("REJECTED_") ->
                Presentation(
                    titleResName = "hotfox_error_update_title",
                    detailResName = "hotfox_error_update_detail",
                    primaryAction = PrimaryAction.NONE,
                    diagnosticCode = code.ifBlank { "update-required" },
                )
            raw.isBlank() -> generic(code = "")
            else -> generic(code = code)
        }
    }

    private fun generic(code: String) = Presentation(
        titleResName = "hotfox_headline_error",
        detailResName = "hotfox_error_generic_detail",
        primaryAction = PrimaryAction.RETRY,
        diagnosticCode = code,
    )

    internal fun sanitizedCode(raw: String): String {
        if (looksLikeSecret(raw)) return ""
        val token = raw.substringBefore(' ').substringBefore('\n').take(48)
        return token
    }

    internal fun looksLikeSecret(raw: String): Boolean {
        val lower = raw.lowercase()
        return lower.contains("vless://") ||
            lower.contains("vmess://") ||
            lower.contains("http://") ||
            lower.contains("https://") ||
            lower.contains("sk_live") ||
            lower.contains("sk_test")
    }
}
