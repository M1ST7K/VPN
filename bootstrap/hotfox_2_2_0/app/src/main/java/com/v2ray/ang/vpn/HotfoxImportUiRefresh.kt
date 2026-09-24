package com.v2ray.ang.vpn

/**
 * After URL/share import the server list must refresh without process restart.
 * A later subscription refetch that returns 0 configs must not hide a successful first import.
 */
object HotfoxImportUiRefresh {
    fun shouldReloadAfterBatch(count: Int, countSub: Int): Boolean = count > 0 || countSub > 0

    fun shouldReloadAfterSubUpdate(successCount: Int, configCount: Int): Boolean =
        successCount > 0 || configCount > 0
}
