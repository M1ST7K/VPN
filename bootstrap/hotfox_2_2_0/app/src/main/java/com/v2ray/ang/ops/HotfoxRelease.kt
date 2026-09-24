package com.v2ray.ang.ops

/**
 * 2.7 release channels. Channel must not silently mix production and
 * sandbox backends; that check lives in Gradle plus [isolatesSandbox].
 */
enum class HotfoxReleaseChannel {
    DEV,
    BETA,
    STABLE,
    ;

    val storageValue: String
        get() = name.lowercase()

    /** Stable/beta never accept sandbox commerce BuildConfig. */
    fun isolatesSandbox(): Boolean = this != DEV

    /** Beta/stable require a signed update manifest. Dev may be unsigned. */
    fun requiresUpdateSignature(): Boolean = this != DEV

    companion object {
        fun fromStorage(raw: String?): HotfoxReleaseChannel = when (raw?.trim()?.lowercase()) {
            "beta" -> BETA
            "stable" -> STABLE
            else -> DEV
        }
    }
}

data class HotfoxBuildIdentity(
    val versionName: String,
    val versionCode: Int,
    val gitSha: String,
    val channel: HotfoxReleaseChannel,
    val distribution: String,
    val debug: Boolean,
    val packageName: String = "com.hotfox.vpn",
    val signingCertSha256: String = "",
) {
    fun artifactLabel(): String {
        val sha = gitSha.takeIf { it.isNotBlank() }?.take(12) ?: "unknown"
        return "hotfox-${versionName}-${versionCode}-${channel.storageValue}-$sha"
    }

    companion object {
        fun runningFromBuildConfig(): HotfoxBuildIdentity = HotfoxBuildIdentity(
            versionName = com.v2ray.ang.BuildConfig.VERSION_NAME,
            versionCode = com.v2ray.ang.BuildConfig.VERSION_CODE,
            gitSha = com.v2ray.ang.BuildConfig.HOTFOX_GIT_SHA,
            channel = HotfoxReleaseChannel.fromStorage(com.v2ray.ang.BuildConfig.HOTFOX_CHANNEL),
            distribution = com.v2ray.ang.BuildConfig.DISTRIBUTION,
            debug = com.v2ray.ang.BuildConfig.DEBUG,
            packageName = com.v2ray.ang.BuildConfig.APPLICATION_ID,
        )
    }
}

object HotfoxVersion {
    fun compare(a: Int, b: Int): Int = a.compareTo(b)

    fun isDowngrade(runningCode: Int, offeredCode: Int): Boolean = offeredCode < runningCode
}
