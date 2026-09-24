package com.v2ray.ang.ops

import java.util.UUID

/**
 * Scoped generated device identifiers for entitlement device limits.
 * Never uses hardware identifiers (Android ID, IMEI, serial, MAC).
 */
data class HotfoxDevice(
    val deviceId: String,
    val displayName: String,
    val registeredAtEpochMs: Long,
    val lastActiveEpochMs: Long,
    val revoked: Boolean = false,
)

enum class HotfoxDeviceAction {
    REGISTERED,
    REFRESHED,
    RENAMED,
    REVOKED,
    LIMIT_REACHED,
    NOT_FOUND,
    REJECTED,
}

data class HotfoxDeviceDecision(
    val action: HotfoxDeviceAction,
    val device: HotfoxDevice?,
    val activeCount: Int,
)

object HotfoxDeviceRegistry {
    const val DEFAULT_LIMIT = 5
    const val DAY_MS = 86_400_000L

    private val lock = Any()
    private var devices: LinkedHashMap<String, HotfoxDevice> = LinkedHashMap()

    fun resetForTests() {
        synchronized(lock) {
            devices = LinkedHashMap()
        }
    }

    fun generateDeviceId(): String = UUID.randomUUID().toString()

    fun coarseActive(epochMs: Long): Long {
        if (epochMs <= 0L) return 0L
        return (epochMs / DAY_MS) * DAY_MS
    }

    fun list(includeRevoked: Boolean = false): List<HotfoxDevice> =
        synchronized(lock) {
            devices.values.filter { includeRevoked || !it.revoked }.toList()
        }

    fun activeCount(): Int = synchronized(lock) { devices.values.count { !it.revoked } }

    fun register(
        deviceId: String,
        displayName: String,
        nowEpochMs: Long,
        limit: Int = DEFAULT_LIMIT,
    ): HotfoxDeviceDecision {
        synchronized(lock) {
            val id = deviceId.trim()
            val name = sanitizeName(displayName)
            if (id.isBlank() || looksLikeHardwareId(id) || name.isBlank()) {
                return HotfoxDeviceDecision(HotfoxDeviceAction.REJECTED, null, devices.values.count { !it.revoked })
            }
            val existing = devices[id]
            if (existing != null && !existing.revoked) {
                val refreshed = existing.copy(
                    displayName = name.ifBlank { existing.displayName },
                    lastActiveEpochMs = coarseActive(nowEpochMs),
                )
                devices[id] = refreshed
                return HotfoxDeviceDecision(HotfoxDeviceAction.REFRESHED, refreshed, devices.values.count { !it.revoked })
            }
            val active = devices.values.count { !it.revoked }
            if (active >= limit) {
                return HotfoxDeviceDecision(HotfoxDeviceAction.LIMIT_REACHED, existing, active)
            }
            val created = HotfoxDevice(
                deviceId = id,
                displayName = name,
                registeredAtEpochMs = nowEpochMs,
                lastActiveEpochMs = coarseActive(nowEpochMs),
                revoked = false,
            )
            devices[id] = created
            return HotfoxDeviceDecision(HotfoxDeviceAction.REGISTERED, created, devices.values.count { !it.revoked })
        }
    }

    fun revoke(deviceId: String): HotfoxDeviceDecision {
        synchronized(lock) {
            val existing = devices[deviceId] ?: return HotfoxDeviceDecision(HotfoxDeviceAction.NOT_FOUND, null, devices.values.count { !it.revoked })
            val revoked = existing.copy(revoked = true)
            devices[deviceId] = revoked
            return HotfoxDeviceDecision(HotfoxDeviceAction.REVOKED, revoked, devices.values.count { !it.revoked })
        }
    }

    fun rename(deviceId: String, displayName: String): HotfoxDeviceDecision {
        synchronized(lock) {
            val existing = devices[deviceId] ?: return HotfoxDeviceDecision(HotfoxDeviceAction.NOT_FOUND, null, devices.values.count { !it.revoked })
            if (existing.revoked) return HotfoxDeviceDecision(HotfoxDeviceAction.REJECTED, existing, devices.values.count { !it.revoked })
            val name = sanitizeName(displayName)
            if (name.isBlank()) return HotfoxDeviceDecision(HotfoxDeviceAction.REJECTED, existing, devices.values.count { !it.revoked })
            val renamed = existing.copy(displayName = name)
            devices[deviceId] = renamed
            return HotfoxDeviceDecision(HotfoxDeviceAction.RENAMED, renamed, devices.values.count { !it.revoked })
        }
    }

    fun sanitizeName(raw: String): String =
        raw.replace(Regex("[<>\\r\\n]"), " ").replace(Regex("\\s+"), " ").trim().take(40)

    fun looksLikeHardwareId(value: String): Boolean {
        val lower = value.lowercase()
        if (lower.contains("imei") || lower.contains("android_id") || lower.contains("serial")) return true
        if (lower.matches(Regex("^[0-9]{15}$"))) return true
        return false
    }
}
