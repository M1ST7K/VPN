package com.v2ray.ang.vpn

import kotlin.math.max

/**
 * Session-relative HEV counters. Native values may reset when tun2socks restarts
 * inside the same VPN session; those resets are accumulated instead of showing a drop.
 *
 * HEV txBytes are bytes leaving the TUN toward the proxy (upload / Отправлено).
 * HEV rxBytes are bytes written back to the TUN for apps (download / Загружено).
 */
class HotfoxTrafficAccumulator {
    private var baselineTx = 0L
    private var baselineRx = 0L
    private var lastRawTx = 0L
    private var lastRawRx = 0L
    private var extraTx = 0L
    private var extraRx = 0L
    private var hasBaseline = false

    @Synchronized
    fun startSession(rawTx: Long = 0L, rawRx: Long = 0L) {
        baselineTx = sanitize(rawTx)
        baselineRx = sanitize(rawRx)
        lastRawTx = baselineTx
        lastRawRx = baselineRx
        extraTx = 0L
        extraRx = 0L
        hasBaseline = true
    }

    @Synchronized
    fun reset() {
        hasBaseline = false
        baselineTx = 0L
        baselineRx = 0L
        lastRawTx = 0L
        lastRawRx = 0L
        extraTx = 0L
        extraRx = 0L
    }

    @Synchronized
    fun sample(rawTx: Long, rawRx: Long): SessionTraffic {
        val tx = sanitize(rawTx)
        val rx = sanitize(rawRx)
        if (!hasBaseline) {
            startSession(tx, rx)
            return SessionTraffic(0L, 0L)
        }
        if (tx < lastRawTx || rx < lastRawRx) {
            extraTx += max(0L, lastRawTx - baselineTx)
            extraRx += max(0L, lastRawRx - baselineRx)
            baselineTx = tx
            baselineRx = rx
        }
        lastRawTx = tx
        lastRawRx = rx
        return SessionTraffic(
            uploadedBytes = extraTx + max(0L, tx - baselineTx),
            downloadedBytes = extraRx + max(0L, rx - baselineRx),
        )
    }

    private fun sanitize(value: Long): Long = if (value < 0L) 0L else value
}

data class SessionTraffic(
    val uploadedBytes: Long,
    val downloadedBytes: Long,
)
