package com.v2ray.ang.vpn

/**
 * 2.8 Autopilot. Pure intent decisions; start/stop still go through
 * [VpnRestartGate] / [CoreServiceManager], never a second session controller.
 */
enum class HotfoxNetworkKind {
    NONE,
    CELLULAR,
    UNKNOWN_WIFI,
    TRUSTED_HOME,
    TRUSTED_OFFICE,
    CAPTIVE_PORTAL,
}

enum class HotfoxTransportKind {
    NONE,
    WIFI,
    CELLULAR,
}

enum class HotfoxPauseKind {
    MINUTES_5,
    MINUTES_15,
    HOUR_1,
    UNTIL_NETWORK_CHANGE,
    ;

    fun durationMs(): Long = when (this) {
        MINUTES_5 -> 5 * 60_000L
        MINUTES_15 -> 15 * 60_000L
        HOUR_1 -> 60 * 60_000L
        UNTIL_NETWORK_CHANGE -> Long.MAX_VALUE
    }
}

enum class HotfoxProtectionLevel {
    SPEED,
    BALANCE,
    MAX_PROTECTION,
}

enum class HotfoxConnectionIntent {
    KEEP_CURRENT,
    CONNECT,
    RECONNECT,
    WAIT_FOR_NETWORK,
    WAIT_FOR_CAPTIVE_PORTAL,
    PAUSED,
    DISCONNECT_BY_POLICY,
    BLOCKED_PERMISSION,
    BLOCKED_ENTITLEMENT,
    NO_TARGET,
}

data class HotfoxPauseState(
    val kind: HotfoxPauseKind,
    val untilEpochMs: Long,
    val networkGenerationAtStart: Long,
) {
    fun active(nowEpochMs: Long, networkGeneration: Long): Boolean = when (kind) {
        HotfoxPauseKind.UNTIL_NETWORK_CHANGE -> networkGeneration == networkGenerationAtStart
        else -> nowEpochMs < untilEpochMs
    }

    fun durationMs(): Long = kind.durationMs()
}

data class HotfoxAutopilotPolicy(
    val enabled: Boolean = true,
    val connectUnknownWifi: Boolean = true,
    val connectCellular: Boolean = true,
    val remainOffOnTrusted: Boolean = true,
    val reconnectOnRestore: Boolean = true,
    val connectAfterBoot: Boolean = true,
)

data class HotfoxProfileDefaults(
    val routingMode: HotfoxRoutingMode,
    val lanAccess: Boolean,
    val adsBlocked: Boolean,
    val shadowAuto: Boolean,
    val preferEfficientPath: Boolean,
)

data class HotfoxAutopilotSnapshot(
    val network: HotfoxNetworkKind,
    val networkGeneration: Long,
    val sessionProtected: Boolean,
    val sessionBusy: Boolean,
    val vpnPermissionGranted: Boolean,
    val entitlementUsable: Boolean,
    val hasUsableTarget: Boolean,
    val autoMode: Boolean,
    val nowEpochMs: Long,
    val eventGeneration: Long,
    val source: HotfoxAutopilotSource,
    val policy: HotfoxAutopilotPolicy,
    val pause: HotfoxPauseState?,
    val suppressAutoUntilNetworkChange: Boolean,
    val lastConnectIntentAtEpochMs: Long,
    val protectionLevel: HotfoxProtectionLevel = HotfoxProtectionLevel.BALANCE,
)

enum class HotfoxAutopilotSource {
    NETWORK,
    USER_CONNECT,
    USER_DISCONNECT,
    PAUSE,
    BOOT,
    PROCESS_START,
    PERMISSION,
    ENTITLEMENT,
}

data class HotfoxIntentDecision(
    val intent: HotfoxConnectionIntent,
    val reason: String,
    val eventGeneration: Long,
    val preserveManualSelection: Boolean,
    val profile: HotfoxProfileDefaults,
) {
    val wantsStart: Boolean
        get() = intent == HotfoxConnectionIntent.CONNECT || intent == HotfoxConnectionIntent.RECONNECT

    val wantsStop: Boolean
        get() = intent == HotfoxConnectionIntent.DISCONNECT_BY_POLICY

    fun uiLabel(): String = HotfoxAutopilotLabels.label(intent)
}

object HotfoxAutopilotLabels {
    fun label(intent: HotfoxConnectionIntent): String = when (intent) {
        HotfoxConnectionIntent.KEEP_CURRENT -> "Без изменений"
        HotfoxConnectionIntent.CONNECT -> "Подключить по политике"
        HotfoxConnectionIntent.RECONNECT -> "Переподключить"
        HotfoxConnectionIntent.WAIT_FOR_NETWORK -> "Нет сети"
        HotfoxConnectionIntent.WAIT_FOR_CAPTIVE_PORTAL -> "Сеть требует авторизации"
        HotfoxConnectionIntent.PAUSED -> "Защита на паузе"
        HotfoxConnectionIntent.DISCONNECT_BY_POLICY -> "Отключить по политике"
        HotfoxConnectionIntent.BLOCKED_PERMISSION -> "Нет разрешения VPN"
        HotfoxConnectionIntent.BLOCKED_ENTITLEMENT -> "Нет действующего доступа"
        HotfoxConnectionIntent.NO_TARGET -> "Нет доступного сервера"
    }

    fun pauseLabel(kind: HotfoxPauseKind): String = when (kind) {
        HotfoxPauseKind.MINUTES_5 -> "Пауза 5 минут"
        HotfoxPauseKind.MINUTES_15 -> "Пауза 15 минут"
        HotfoxPauseKind.HOUR_1 -> "Пауза 1 час"
        HotfoxPauseKind.UNTIL_NETWORK_CHANGE -> "Пауза до смены сети"
    }

    fun protectionLabel(level: HotfoxProtectionLevel): String = when (level) {
        HotfoxProtectionLevel.SPEED -> "Скорость"
        HotfoxProtectionLevel.BALANCE -> "Баланс"
        HotfoxProtectionLevel.MAX_PROTECTION -> "Максимальная защита"
    }
}

object HotfoxProtectionProfiles {
    fun defaults(level: HotfoxProtectionLevel, network: HotfoxNetworkKind): HotfoxProfileDefaults {
        val publicWifi = network == HotfoxNetworkKind.UNKNOWN_WIFI || network == HotfoxNetworkKind.CAPTIVE_PORTAL
        val home = network == HotfoxNetworkKind.TRUSTED_HOME || network == HotfoxNetworkKind.TRUSTED_OFFICE
        val cellular = network == HotfoxNetworkKind.CELLULAR
        return when (level) {
            HotfoxProtectionLevel.SPEED -> HotfoxProfileDefaults(
                routingMode = HotfoxRoutingMode.SMART,
                lanAccess = home || cellular,
                adsBlocked = false,
                shadowAuto = publicWifi,
                preferEfficientPath = cellular,
            )
            HotfoxProtectionLevel.BALANCE -> HotfoxProfileDefaults(
                routingMode = HotfoxRoutingMode.SMART,
                lanAccess = home,
                adsBlocked = publicWifi,
                shadowAuto = publicWifi,
                preferEfficientPath = cellular,
            )
            HotfoxProtectionLevel.MAX_PROTECTION -> HotfoxProfileDefaults(
                routingMode = HotfoxRoutingMode.GLOBAL,
                lanAccess = false,
                adsBlocked = true,
                shadowAuto = true,
                preferEfficientPath = false,
            )
        }
    }
}

/**
 * Timed pause resume is alarm-driven (not a polling loop).
 * [HotfoxPauseKind.UNTIL_NETWORK_CHANGE] is cleared by [HotfoxAutopilotStore.noteNetworkChange].
 */
object HotfoxAutopilotAlarms {
    fun expiryEpochMs(pause: HotfoxPauseState?, nowEpochMs: Long): Long? {
        if (pause == null) return null
        if (pause.kind == HotfoxPauseKind.UNTIL_NETWORK_CHANGE) return null
        if (nowEpochMs >= pause.untilEpochMs) return null
        return pause.untilEpochMs
    }
}

object HotfoxTrustedNetworks {
    fun classify(
        transport: HotfoxTransportKind,
        captive: Boolean,
        opaqueNetworkId: String,
        trusted: Map<String, HotfoxNetworkKind>,
    ): HotfoxNetworkKind {
        if (transport == HotfoxTransportKind.NONE) return HotfoxNetworkKind.NONE
        if (captive) return HotfoxNetworkKind.CAPTIVE_PORTAL
        if (transport == HotfoxTransportKind.CELLULAR) return HotfoxNetworkKind.CELLULAR
        val marked = trusted[opaqueNetworkId]
        return when (marked) {
            HotfoxNetworkKind.TRUSTED_HOME, HotfoxNetworkKind.TRUSTED_OFFICE -> marked
            else -> HotfoxNetworkKind.UNKNOWN_WIFI
        }
    }

    fun opaqueId(ssidHint: String): String {
        val raw = ssidHint.trim().lowercase()
        if (raw.isBlank()) return ""
        return HotfoxAutopilotCrypto.sha256Prefix(raw)
    }
}

internal object HotfoxAutopilotCrypto {
    fun sha256Prefix(value: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.take(8).joinToString("") { b -> "%02x".format(b.toInt() and 0xFF) }
    }
}

object ConnectionIntentEngine {
    const val MIN_CONNECT_GAP_MS = 5_000L

    fun decide(snap: HotfoxAutopilotSnapshot): HotfoxIntentDecision {
        val profile = HotfoxProtectionProfiles.defaults(
            snap.protectionLevel,
            snap.network,
        )
        val preserveManual = !snap.autoMode
        fun out(intent: HotfoxConnectionIntent, reason: String) = HotfoxIntentDecision(
            intent = intent,
            reason = reason,
            eventGeneration = snap.eventGeneration,
            preserveManualSelection = preserveManual,
            profile = profile,
        )

        if (snap.sessionBusy) {
            return out(HotfoxConnectionIntent.KEEP_CURRENT, "session_busy")
        }
        if (snap.source == HotfoxAutopilotSource.USER_CONNECT) {
            return when {
                !snap.vpnPermissionGranted -> out(HotfoxConnectionIntent.BLOCKED_PERMISSION, "user_connect_permission")
                !snap.hasUsableTarget -> out(HotfoxConnectionIntent.NO_TARGET, "user_connect_no_target")
                snap.network == HotfoxNetworkKind.NONE -> out(HotfoxConnectionIntent.WAIT_FOR_NETWORK, "user_connect_no_network")
                snap.network == HotfoxNetworkKind.CAPTIVE_PORTAL ->
                    out(HotfoxConnectionIntent.WAIT_FOR_CAPTIVE_PORTAL, "user_connect_captive")
                else -> out(HotfoxConnectionIntent.CONNECT, "user_connect")
            }
        }
        if (snap.source == HotfoxAutopilotSource.USER_DISCONNECT) {
            return out(HotfoxConnectionIntent.KEEP_CURRENT, "user_disconnect")
        }

        if (snap.network == HotfoxNetworkKind.NONE) {
            return if (snap.sessionProtected) {
                out(HotfoxConnectionIntent.KEEP_CURRENT, "no_network_keep_session")
            } else {
                out(HotfoxConnectionIntent.WAIT_FOR_NETWORK, "no_network")
            }
        }
        if (snap.network == HotfoxNetworkKind.CAPTIVE_PORTAL) {
            return if (snap.sessionProtected) {
                out(HotfoxConnectionIntent.DISCONNECT_BY_POLICY, "captive_release_portal")
            } else {
                out(HotfoxConnectionIntent.WAIT_FOR_CAPTIVE_PORTAL, "captive")
            }
        }
        if (!snap.vpnPermissionGranted) {
            return out(HotfoxConnectionIntent.BLOCKED_PERMISSION, "permission")
        }

        val pause = snap.pause
        if (pause != null && pause.active(snap.nowEpochMs, snap.networkGeneration)) {
            return if (snap.sessionProtected) {
                out(HotfoxConnectionIntent.DISCONNECT_BY_POLICY, "pause_stop")
            } else {
                out(HotfoxConnectionIntent.PAUSED, "pause")
            }
        }

        if (snap.suppressAutoUntilNetworkChange) {
            return out(HotfoxConnectionIntent.KEEP_CURRENT, "user_disconnect_suppress")
        }

        if (!snap.policy.enabled) {
            return out(HotfoxConnectionIntent.KEEP_CURRENT, "autopilot_disabled")
        }

        if (!snap.hasUsableTarget) {
            return if (!snap.entitlementUsable && !preserveManual) {
                out(HotfoxConnectionIntent.BLOCKED_ENTITLEMENT, "entitlement")
            } else {
                out(HotfoxConnectionIntent.NO_TARGET, "no_target")
            }
        }

        val trustedStayOff = snap.policy.remainOffOnTrusted &&
            (snap.network == HotfoxNetworkKind.TRUSTED_HOME || snap.network == HotfoxNetworkKind.TRUSTED_OFFICE)
        if (trustedStayOff) {
            return if (snap.sessionProtected) {
                out(HotfoxConnectionIntent.DISCONNECT_BY_POLICY, "trusted_off")
            } else {
                out(HotfoxConnectionIntent.KEEP_CURRENT, "trusted_remain_off")
            }
        }

        val shouldProtect = when (snap.network) {
            HotfoxNetworkKind.UNKNOWN_WIFI -> snap.policy.connectUnknownWifi
            HotfoxNetworkKind.CELLULAR -> snap.policy.connectCellular
            HotfoxNetworkKind.TRUSTED_HOME, HotfoxNetworkKind.TRUSTED_OFFICE -> !snap.policy.remainOffOnTrusted
            else -> false
        }
        if (!shouldProtect) {
            return out(HotfoxConnectionIntent.KEEP_CURRENT, "policy_idle")
        }

        if (snap.source == HotfoxAutopilotSource.BOOT || snap.source == HotfoxAutopilotSource.PROCESS_START) {
            if (!snap.policy.connectAfterBoot) {
                return out(HotfoxConnectionIntent.KEEP_CURRENT, "boot_disabled")
            }
        }

        if (snap.sessionProtected) {
            val handover = snap.source == HotfoxAutopilotSource.NETWORK
            return if (handover && snap.policy.reconnectOnRestore) {
                out(HotfoxConnectionIntent.KEEP_CURRENT, "handover_keep_serialized")
            } else {
                out(HotfoxConnectionIntent.KEEP_CURRENT, "already_protected")
            }
        }

        if (snap.lastConnectIntentAtEpochMs > 0L &&
            snap.nowEpochMs - snap.lastConnectIntentAtEpochMs < ConnectionIntentEngine.MIN_CONNECT_GAP_MS
        ) {
            return out(HotfoxConnectionIntent.KEEP_CURRENT, "reconnect_gap")
        }

        val reconnect = snap.source == HotfoxAutopilotSource.NETWORK && snap.policy.reconnectOnRestore
        return if (reconnect) {
            out(HotfoxConnectionIntent.RECONNECT, "network_restore")
        } else {
            out(HotfoxConnectionIntent.CONNECT, "policy_connect")
        }
    }
}

object HotfoxAutopilotStore {
    private const val PREF_STATE = "hotfox_autopilot_state"

    @Volatile
    private var hydrated: Boolean = false

    @Volatile
    private var policy: HotfoxAutopilotPolicy = HotfoxAutopilotPolicy()

    @Volatile
    private var pause: HotfoxPauseState? = null

    @Volatile
    private var trusted: Map<String, HotfoxNetworkKind> = emptyMap()

    @Volatile
    private var suppressAutoUntilNetworkChange: Boolean = false

    @Volatile
    private var eventGeneration: Long = 0L

    @Volatile
    private var networkGeneration: Long = 0L

    @Volatile
    private var lastConnectIntentAtEpochMs: Long = 0L

    @Volatile
    private var lastDecision: HotfoxIntentDecision? = null

    @Volatile
    private var level: HotfoxProtectionLevel = HotfoxProtectionLevel.BALANCE

    private fun hydrate() {
        if (hydrated) return
        synchronized(this) {
            if (hydrated) return
            runCatching {
                val raw = com.v2ray.ang.handler.MmkvManager.decodeSettingsString(PREF_STATE)
                if (!raw.isNullOrBlank()) {
                    restore(HotfoxAutopilotCodec.decode(raw), preserveGenerations = false)
                }
            }
            hydrated = true
        }
    }

    private fun persist() {
        runCatching {
            com.v2ray.ang.handler.MmkvManager.encodeSettings(PREF_STATE, HotfoxAutopilotCodec.encode(export()))
        }
    }

    fun export(): HotfoxAutopilotPersisted = HotfoxAutopilotPersisted(
        policy = policy,
        pause = pause,
        trusted = trusted,
        suppressAutoUntilNetworkChange = suppressAutoUntilNetworkChange,
        level = level,
    )

    fun restore(state: HotfoxAutopilotPersisted, preserveGenerations: Boolean = false) {
        policy = state.policy
        trusted = state.trusted
        suppressAutoUntilNetworkChange = state.suppressAutoUntilNetworkChange
        level = state.level
        pause = state.pause
        if (!preserveGenerations) {
            eventGeneration = 0L
            networkGeneration = 0L
            lastConnectIntentAtEpochMs = 0L
            lastDecision = null
        }
    }

    fun resetForTests() {
        policy = HotfoxAutopilotPolicy()
        pause = null
        trusted = emptyMap()
        suppressAutoUntilNetworkChange = false
        eventGeneration = 0L
        networkGeneration = 0L
        lastConnectIntentAtEpochMs = 0L
        lastDecision = null
        level = HotfoxProtectionLevel.BALANCE
        hydrated = true
    }

    fun policy(): HotfoxAutopilotPolicy {
        hydrate()
        return policy
    }

    fun setPolicy(value: HotfoxAutopilotPolicy) {
        hydrate()
        policy = value
        persist()
    }

    fun pause(): HotfoxPauseState? {
        hydrate()
        return pause
    }

    fun protectionLevel(): HotfoxProtectionLevel {
        hydrate()
        return level
    }

    fun setProtectionLevel(value: HotfoxProtectionLevel) {
        hydrate()
        level = value
        persist()
    }

    fun trusted(): Map<String, HotfoxNetworkKind> {
        hydrate()
        return trusted
    }

    fun markTrusted(opaqueId: String, kind: HotfoxNetworkKind) {
        hydrate()
        if (opaqueId.isBlank()) return
        if (kind != HotfoxNetworkKind.TRUSTED_HOME && kind != HotfoxNetworkKind.TRUSTED_OFFICE) return
        trusted = trusted + (opaqueId to kind)
        persist()
    }

    fun clearTrusted(opaqueId: String) {
        hydrate()
        trusted = trusted - opaqueId
        persist()
    }

    fun currentPause(nowEpochMs: Long): HotfoxPauseState? {
        hydrate()
        val p = pause ?: return null
        return if (p.active(nowEpochMs, networkGeneration)) p else {
            pause = null
            persist()
            null
        }
    }

    fun startPause(kind: HotfoxPauseKind, nowEpochMs: Long): HotfoxPauseState {
        hydrate()
        val state = HotfoxPauseState(
            kind = kind,
            untilEpochMs = if (kind == HotfoxPauseKind.UNTIL_NETWORK_CHANGE) Long.MAX_VALUE
            else nowEpochMs + kind.durationMs(),
            networkGenerationAtStart = networkGeneration,
        )
        pause = state
        persist()
        return state
    }

    fun clearPause() {
        hydrate()
        pause = null
        persist()
    }

    fun noteUserDisconnect() {
        hydrate()
        suppressAutoUntilNetworkChange = true
        persist()
    }

    fun noteUserConnect() {
        hydrate()
        suppressAutoUntilNetworkChange = false
        pause = null
        persist()
    }

    fun noteNetworkChange() {
        hydrate()
        networkGeneration += 1
        var changed = false
        if (suppressAutoUntilNetworkChange) {
            suppressAutoUntilNetworkChange = false
            changed = true
        }
        val p = pause
        if (p != null && p.kind == HotfoxPauseKind.UNTIL_NETWORK_CHANGE) {
            pause = null
            changed = true
        }
        if (changed) persist()
    }

    fun nextEvent(): Long {
        hydrate()
        eventGeneration += 1
        return eventGeneration
    }

    fun liveGeneration(): Long {
        hydrate()
        return eventGeneration
    }

    fun networkGeneration(): Long {
        hydrate()
        return networkGeneration
    }

    fun suppressAuto(): Boolean {
        hydrate()
        return suppressAutoUntilNetworkChange
    }

    fun lastDecision(): HotfoxIntentDecision? {
        hydrate()
        return lastDecision
    }

    fun lastConnectAt(): Long {
        hydrate()
        return lastConnectIntentAtEpochMs
    }

    fun consider(snap: HotfoxAutopilotSnapshot): HotfoxIntentDecision? {
        hydrate()
        if (snap.eventGeneration != eventGeneration) return null
        val decision = ConnectionIntentEngine.decide(snap)
        lastDecision = decision
        if (decision.wantsStart) {
            lastConnectIntentAtEpochMs = snap.nowEpochMs
        }
        return decision
    }

    fun snapshot(
        network: HotfoxNetworkKind,
        sessionProtected: Boolean,
        sessionBusy: Boolean,
        vpnPermissionGranted: Boolean,
        entitlementUsable: Boolean,
        hasUsableTarget: Boolean,
        autoMode: Boolean,
        nowEpochMs: Long,
        source: HotfoxAutopilotSource,
        eventGeneration: Long = nextEvent(),
    ): HotfoxAutopilotSnapshot {
        hydrate()
        return HotfoxAutopilotSnapshot(
            network = network,
            networkGeneration = networkGeneration,
            sessionProtected = sessionProtected,
            sessionBusy = sessionBusy,
            vpnPermissionGranted = vpnPermissionGranted,
            entitlementUsable = entitlementUsable,
            hasUsableTarget = hasUsableTarget,
            autoMode = autoMode,
            nowEpochMs = nowEpochMs,
            eventGeneration = eventGeneration,
            source = source,
            policy = policy,
            pause = currentPause(nowEpochMs),
            suppressAutoUntilNetworkChange = suppressAutoUntilNetworkChange,
            lastConnectIntentAtEpochMs = lastConnectIntentAtEpochMs,
            protectionLevel = level,
        )
    }
}

object HotfoxAutopilotApply {
    fun shouldDispatchStart(decision: HotfoxIntentDecision, sessionProtected: Boolean): Boolean =
        !sessionProtected && decision.wantsStart

    fun shouldDispatchStop(decision: HotfoxIntentDecision, sessionProtected: Boolean): Boolean =
        sessionProtected && decision.wantsStop

    fun shouldApplyProfile(currentMode: HotfoxRoutingMode): Boolean =
        currentMode == HotfoxRoutingMode.SMART || currentMode == HotfoxRoutingMode.GLOBAL

    fun applyProfileIfCompatible(profile: HotfoxProfileDefaults): Boolean {
        val current = runCatching { HotfoxRoutingStore.load() }.getOrElse { return false }
        if (!shouldApplyProfile(current.mode)) return false
        return runCatching {
            if (current.mode != profile.routingMode) {
                HotfoxRoutingStore.saveMode(profile.routingMode)
            }
            if (current.lanAccess != profile.lanAccess) {
                HotfoxRoutingStore.saveLan(profile.lanAccess)
            }
            if (current.adsBlocked != profile.adsBlocked) {
                HotfoxRoutingStore.saveAds(profile.adsBlocked)
            }
            HotfoxShadowStore.setShadowAuto(profile.shadowAuto)
            true
        }.getOrDefault(false)
    }
}

data class HotfoxAutopilotPersisted(
    val policy: HotfoxAutopilotPolicy,
    val pause: HotfoxPauseState?,
    val trusted: Map<String, HotfoxNetworkKind>,
    val suppressAutoUntilNetworkChange: Boolean,
    val level: HotfoxProtectionLevel,
)

object HotfoxAutopilotCodec {
    fun encodePolicy(policy: HotfoxAutopilotPolicy): String = listOf(
        policy.enabled,
        policy.connectUnknownWifi,
        policy.connectCellular,
        policy.remainOffOnTrusted,
        policy.reconnectOnRestore,
        policy.connectAfterBoot,
    ).joinToString(",") { if (it) "1" else "0" }

    fun decodePolicy(raw: String): HotfoxAutopilotPolicy {
        val parts = raw.split(',')
        fun at(i: Int, default: Boolean) = parts.getOrNull(i)?.let { it == "1" } ?: default
        return HotfoxAutopilotPolicy(
            enabled = at(0, true),
            connectUnknownWifi = at(1, true),
            connectCellular = at(2, true),
            remainOffOnTrusted = at(3, true),
            reconnectOnRestore = at(4, true),
            connectAfterBoot = at(5, true),
        )
    }

    fun encodeTrusted(trusted: Map<String, HotfoxNetworkKind>): String =
        trusted.entries.joinToString(";") { "${it.key}=${it.value.name}" }

    fun decodeTrusted(raw: String): Map<String, HotfoxNetworkKind> {
        if (raw.isBlank()) return emptyMap()
        return raw.split(';').mapNotNull { item ->
            val idx = item.indexOf('=')
            if (idx <= 0) return@mapNotNull null
            val id = item.substring(0, idx)
            val kind = runCatching { HotfoxNetworkKind.valueOf(item.substring(idx + 1)) }.getOrNull()
            if (kind == HotfoxNetworkKind.TRUSTED_HOME || kind == HotfoxNetworkKind.TRUSTED_OFFICE) {
                id to kind
            } else {
                null
            }
        }.toMap()
    }

    fun encodePause(pause: HotfoxPauseState?): String {
        if (pause == null) return ""
        return "${pause.kind.name}|${pause.untilEpochMs}|${pause.networkGenerationAtStart}"
    }

    fun decodePause(raw: String): HotfoxPauseState? {
        if (raw.isBlank()) return null
        val parts = raw.split('|')
        if (parts.size < 3) return null
        val kind = runCatching { HotfoxPauseKind.valueOf(parts[0]) }.getOrNull() ?: return null
        val until = parts[1].toLongOrNull() ?: return null
        val gen = parts[2].toLongOrNull() ?: 0L
        return HotfoxPauseState(
            kind = kind,
            untilEpochMs = until,
            networkGenerationAtStart = if (kind == HotfoxPauseKind.UNTIL_NETWORK_CHANGE) 0L else gen,
        )
    }

    fun encode(state: HotfoxAutopilotPersisted): String = listOf(
        encodePolicy(state.policy),
        state.level.name,
        if (state.suppressAutoUntilNetworkChange) "1" else "0",
        encodeTrusted(state.trusted),
        encodePause(state.pause),
    ).joinToString("\n")

    fun decode(raw: String): HotfoxAutopilotPersisted {
        val lines = raw.split('\n')
        return HotfoxAutopilotPersisted(
            policy = decodePolicy(lines.getOrElse(0) { "" }),
            level = runCatching { HotfoxProtectionLevel.valueOf(lines.getOrElse(1) { "BALANCE" }) }
                .getOrDefault(HotfoxProtectionLevel.BALANCE),
            suppressAutoUntilNetworkChange = lines.getOrElse(2) { "0" } == "1",
            trusted = decodeTrusted(lines.getOrElse(3) { "" }),
            pause = decodePause(lines.getOrElse(4) { "" }),
        )
    }
}
