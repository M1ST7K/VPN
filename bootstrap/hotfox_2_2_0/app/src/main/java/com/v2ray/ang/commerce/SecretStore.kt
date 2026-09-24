package com.v2ray.ang.commerce

object SecretKeys {
    const val ENTITLEMENT_CREDENTIAL = "entitlement_credential"
    const val SUBSCRIPTION_TOKEN = "hotfox_subscription_token"
    const val RESTORE_SECRET = "restore_secret"
    const val ACCOUNT_REFRESH_TOKEN = "account_refresh_token"
}

sealed class SecretGetResult {
    data class Value(val plaintext: ByteArray) : SecretGetResult() {
        fun utf8(): String = plaintext.decodeToString()
        override fun equals(other: Any?): Boolean =
            other is Value && plaintext.contentEquals(other.plaintext)
        override fun hashCode(): Int = plaintext.contentHashCode()
    }
    data object Missing : SecretGetResult()
    data object Corrupt : SecretGetResult()
    data object KeystoreInvalidated : SecretGetResult()
}

sealed class SecretPutResult {
    data object Ok : SecretPutResult()
    data object KeystoreInvalidated : SecretPutResult()
    data object Failed : SecretPutResult()
}

/**
 * Keystore-backed secret abstraction. Plain MMKV, resources, logs, BuildConfig and URLs
 * must not hold entitlement/subscription/restore credentials.
 */
interface SecretStore {
    fun put(key: String, plaintext: ByteArray): SecretPutResult
    fun get(key: String): SecretGetResult
    fun delete(key: String)
    fun putUtf8(key: String, value: String): SecretPutResult = put(key, value.toByteArray())
}
