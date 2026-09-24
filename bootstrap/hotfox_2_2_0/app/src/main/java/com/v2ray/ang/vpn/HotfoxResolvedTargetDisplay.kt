package com.v2ray.ang.vpn

/**
 * Truthful AUTO target labels. The selector never emits CONNECTED / Защищено.
 *
 * Sequence: AUTO → Подбираем сервер… → concrete city → Подключение… → Защищено
 * where the last two headlines still come from [ConnectionUiMapper].
 */
object HotfoxResolvedTargetDisplay {
    const val SELECTING_COPY = "Подбираем сервер…"
    const val SHADOW_SELECTING_COPY = HotfoxShadowPolicy.SELECTING_COPY

    enum class Phase {
        AUTO_IDLE,
        SELECTING,
        RESOLVED,
        MANUAL,
    }

    fun phase(
        auto: Boolean,
        connecting: Boolean,
        resolvedRemark: String?,
        stage: VpnConnectionStage = VpnConnectionStage.IDLE,
    ): Phase {
        if (!auto) return Phase.MANUAL
        val hasTarget = !resolvedRemark.isNullOrBlank() && resolvedRemark != "—"
        if (connecting && !hasTarget) {
            return Phase.SELECTING
        }
        return if (hasTarget) Phase.RESOLVED else Phase.AUTO_IDLE
    }

    fun serverLabel(
        auto: Boolean,
        connecting: Boolean,
        city: String?,
        stage: VpnConnectionStage = VpnConnectionStage.IDLE,
        idleFallback: String = "Сервер не выбран",
        autoPrefix: (String) -> String = { "Авто · $it" },
        shadowAuto: Boolean = false,
    ): String {
        val remark = city?.takeIf { it.isNotBlank() && it != "—" }
        return when (phase(auto, connecting, remark, stage)) {
            Phase.SELECTING -> if (shadowAuto) SHADOW_SELECTING_COPY else SELECTING_COPY
            Phase.RESOLVED -> if (auto) autoPrefix(remark!!) else remark!!
            Phase.MANUAL -> remark ?: idleFallback
            Phase.AUTO_IDLE -> remark?.let(autoPrefix) ?: idleFallback
        }
    }

    fun mustNotClaimProtected(phase: Phase): Boolean = phase != Phase.RESOLVED
}
