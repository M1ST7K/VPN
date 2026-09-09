package com.v2ray.ang.ops

enum class HotfoxUpdateState {
    UP_TO_DATE,
    AVAILABLE,
    BLOCKED_KNOWN_BAD,
    REJECTED_DOWNGRADE,
    REJECTED_CHANNEL,
    REJECTED_HASH,
    REJECTED_SIGNATURE,
    REJECTED_IDENTITY,
    REJECTED_EXPIRED,
    REJECTED_SCHEMA,
    MISSING,
}

data class HotfoxUpdateManifest(
    val schema: Int,
    val channel: HotfoxReleaseChannel,
    val versionName: String,
    val versionCode: Int,
    val gitSha: String,
    val apkUrl: String,
    val sha256: String,
    val minUpdaterVersionCode: Int,
    val issuedAtEpochMs: Long,
    val expiresAtEpochMs: Long,
    val knownBadVersionCodes: Set<Int>,
    val signatureHex: String,
    val packageName: String = "com.hotfox.vpn",
    val signingCertSha256: String = "",
) {
    fun canonicalPayload(): ByteArray {
        val body = buildString {
            append("apkUrl=").append(apkUrl).append('\n')
            append("channel=").append(channel.storageValue).append('\n')
            append("expiresAtEpochMs=").append(expiresAtEpochMs).append('\n')
            append("gitSha=").append(gitSha).append('\n')
            append("issuedAtEpochMs=").append(issuedAtEpochMs).append('\n')
            append("knownBad=").append(knownBadVersionCodes.sorted().joinToString(",")).append('\n')
            append("minUpdaterVersionCode=").append(minUpdaterVersionCode).append('\n')
            append("packageName=").append(packageName).append('\n')
            append("schema=").append(schema).append('\n')
            append("sha256=").append(sha256.lowercase()).append('\n')
            append("signingCertSha256=").append(signingCertSha256.lowercase()).append('\n')
            append("versionCode=").append(versionCode).append('\n')
            append("versionName=").append(versionName).append('\n')
        }
        return body.toByteArray(Charsets.UTF_8)
    }
}

data class HotfoxUpdateDecision(
    val state: HotfoxUpdateState,
    val reason: String,
    val manifest: HotfoxUpdateManifest? = null,
) {
    val offersInstall: Boolean get() = state == HotfoxUpdateState.AVAILABLE
}

object HotfoxUpdatePolicy {
    const val SCHEMA = 1

    fun parse(raw: String?): HotfoxUpdateManifest? {
        if (raw.isNullOrBlank()) return null
        val obj = raw.trim()
        if (!obj.startsWith("{") || !obj.endsWith("}")) return null
        val schema = HotfoxJsonFields.intValue(obj, "schema") ?: return null
        val channel = HotfoxReleaseChannel.fromStorage(HotfoxJsonFields.string(obj, "channel"))
        val versionName = HotfoxJsonFields.string(obj, "versionName") ?: return null
        val versionCode = HotfoxJsonFields.intValue(obj, "versionCode") ?: return null
        val gitSha = HotfoxJsonFields.string(obj, "gitSha") ?: return null
        val apkUrl = HotfoxJsonFields.string(obj, "apkUrl") ?: return null
        val sha256 = HotfoxJsonFields.string(obj, "sha256") ?: return null
        val minUpdater = HotfoxJsonFields.intValue(obj, "minUpdaterVersionCode") ?: 0
        val issued = HotfoxJsonFields.longValue(obj, "issuedAtEpochMs") ?: return null
        val expires = HotfoxJsonFields.longValue(obj, "expiresAtEpochMs") ?: return null
        val signature = HotfoxJsonFields.string(obj, "signature").orEmpty()
        val knownBad = HotfoxJsonFields.intList(obj, "knownBadVersionCodes")
        val packageName = HotfoxJsonFields.string(obj, "packageName") ?: "com.hotfox.vpn"
        val signingCert = HotfoxJsonFields.string(obj, "signingCertSha256").orEmpty()
        if (versionName.isBlank() || versionCode <= 0) return null
        if (!apkUrl.startsWith("https://")) return null
        if (packageName.isBlank()) return null
        return HotfoxUpdateManifest(
            schema = schema,
            channel = channel,
            versionName = versionName,
            versionCode = versionCode,
            gitSha = gitSha,
            apkUrl = apkUrl,
            sha256 = sha256.lowercase(),
            minUpdaterVersionCode = minUpdater,
            issuedAtEpochMs = issued,
            expiresAtEpochMs = expires,
            knownBadVersionCodes = knownBad,
            signatureHex = signature,
            packageName = packageName,
            signingCertSha256 = signingCert.lowercase(),
        )
    }

    fun evaluate(
        running: HotfoxBuildIdentity,
        manifest: HotfoxUpdateManifest?,
        nowEpochMs: Long,
        publicKeySpkiHex: String,
        apkBytes: ByteArray? = null,
        allowDevDowngrade: Boolean = false,
    ): HotfoxUpdateDecision {
        if (manifest == null) {
            return HotfoxUpdateDecision(HotfoxUpdateState.MISSING, "missing_manifest")
        }
        if (manifest.schema != SCHEMA) {
            return HotfoxUpdateDecision(HotfoxUpdateState.REJECTED_SCHEMA, "schema", manifest)
        }
        if (!HotfoxOpsCrypto.isSha256Hex(manifest.sha256)) {
            return HotfoxUpdateDecision(HotfoxUpdateState.REJECTED_HASH, "sha256_format", manifest)
        }
        if (nowEpochMs > manifest.expiresAtEpochMs || nowEpochMs < manifest.issuedAtEpochMs) {
            return HotfoxUpdateDecision(HotfoxUpdateState.REJECTED_EXPIRED, "expired", manifest)
        }
        if (manifest.channel != running.channel) {
            return HotfoxUpdateDecision(HotfoxUpdateState.REJECTED_CHANNEL, "channel", manifest)
        }
        if (manifest.packageName != running.packageName) {
            return HotfoxUpdateDecision(HotfoxUpdateState.REJECTED_IDENTITY, "package", manifest)
        }
        if (manifest.signingCertSha256.isNotBlank() && running.signingCertSha256.isNotBlank() &&
            !manifest.signingCertSha256.equals(running.signingCertSha256, ignoreCase = true)
        ) {
            return HotfoxUpdateDecision(HotfoxUpdateState.REJECTED_IDENTITY, "signing_cert", manifest)
        }
        val signatureRequired = running.channel.requiresUpdateSignature() || publicKeySpkiHex.isNotBlank()
        if (signatureRequired) {
            if (publicKeySpkiHex.isBlank() ||
                !HotfoxOpsCrypto.verifyEcdsaP256(
                    manifest.canonicalPayload(),
                    manifest.signatureHex,
                    publicKeySpkiHex,
                )
            ) {
                return HotfoxUpdateDecision(HotfoxUpdateState.REJECTED_SIGNATURE, "signature", manifest)
            }
        }
        if (running.versionCode in manifest.knownBadVersionCodes ||
            manifest.versionCode in manifest.knownBadVersionCodes
        ) {
            return HotfoxUpdateDecision(HotfoxUpdateState.BLOCKED_KNOWN_BAD, "known_bad", manifest)
        }
        if (HotfoxVersion.isDowngrade(running.versionCode, manifest.versionCode)) {
            val allow = running.channel == HotfoxReleaseChannel.DEV && allowDevDowngrade
            if (!allow) {
                return HotfoxUpdateDecision(HotfoxUpdateState.REJECTED_DOWNGRADE, "downgrade", manifest)
            }
        }
        if (running.versionCode >= manifest.versionCode) {
            return HotfoxUpdateDecision(HotfoxUpdateState.UP_TO_DATE, "current", manifest)
        }
        if (running.versionCode < manifest.minUpdaterVersionCode) {
            return HotfoxUpdateDecision(HotfoxUpdateState.REJECTED_SCHEMA, "min_updater", manifest)
        }
        if (apkBytes != null) {
            val actual = HotfoxOpsCrypto.sha256Hex(apkBytes)
            if (!actual.equals(manifest.sha256, ignoreCase = true)) {
                return HotfoxUpdateDecision(HotfoxUpdateState.REJECTED_HASH, "apk_hash", manifest)
            }
        }
        return HotfoxUpdateDecision(HotfoxUpdateState.AVAILABLE, "newer", manifest)
    }

    /** Sideload never installs itself. AVAILABLE is a user-visible prompt only. */
    fun maySilentlyInstall(): Boolean = false

    fun canOfferUserInstall(decision: HotfoxUpdateDecision, apkBytes: ByteArray): Boolean {
        if (maySilentlyInstall() || !decision.offersInstall) return false
        val manifest = decision.manifest ?: return false
        val actual = HotfoxOpsCrypto.sha256Hex(apkBytes)
        return actual.equals(manifest.sha256, ignoreCase = true)
    }

    fun uiLabel(decision: HotfoxUpdateDecision): String = when (decision.state) {
        HotfoxUpdateState.UP_TO_DATE -> "Обновлений нет"
        HotfoxUpdateState.AVAILABLE -> "Доступно обновление"
        HotfoxUpdateState.BLOCKED_KNOWN_BAD -> "Версия отозвана"
        HotfoxUpdateState.REJECTED_DOWNGRADE -> "Понижение версии отклонено"
        HotfoxUpdateState.REJECTED_CHANNEL -> "Канал обновления не совпадает"
        HotfoxUpdateState.REJECTED_HASH -> "Контрольная сумма не совпала"
        HotfoxUpdateState.REJECTED_SIGNATURE -> "Подпись манифеста недействительна"
        HotfoxUpdateState.REJECTED_IDENTITY -> "Пакет или сертификат подписи не совпал"
        HotfoxUpdateState.REJECTED_EXPIRED -> "Манифест обновления просрочен"
        HotfoxUpdateState.REJECTED_SCHEMA -> "Манифест обновления не поддерживается"
        HotfoxUpdateState.MISSING -> "Метаданные обновления недоступны"
    }
}
