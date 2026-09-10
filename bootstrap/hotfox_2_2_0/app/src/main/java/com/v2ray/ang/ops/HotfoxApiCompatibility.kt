package com.v2ray.ang.ops

/**
 * Explicit API compatibility for commerce, control plane, devices and updates.
 * Old clients fail gracefully instead of applying a newer incompatible schema.
 * Unknown fields are ignored; the same field must not silently change meaning.
 */
enum class HotfoxApiCompat {
    OK,
    UNKNOWN_FIELDS_OK,
    CLIENT_TOO_OLD,
    SERVER_TOO_OLD,
}

object HotfoxApiCompatibility {
    const val CLIENT_PROTOCOL = 1
    const val MIN_SERVER_PROTOCOL = 1

    fun evaluate(
        clientProtocol: Int = CLIENT_PROTOCOL,
        minClientProtocol: Int = MIN_SERVER_PROTOCOL,
        serverProtocol: Int = minClientProtocol,
        minServerProtocol: Int = MIN_SERVER_PROTOCOL,
    ): HotfoxApiCompat {
        if (clientProtocol < minClientProtocol) return HotfoxApiCompat.CLIENT_TOO_OLD
        if (serverProtocol < minServerProtocol) return HotfoxApiCompat.SERVER_TOO_OLD
        return HotfoxApiCompat.OK
    }

    fun failGracefully(result: HotfoxApiCompat): Boolean =
        result == HotfoxApiCompat.CLIENT_TOO_OLD || result == HotfoxApiCompat.SERVER_TOO_OLD
}
