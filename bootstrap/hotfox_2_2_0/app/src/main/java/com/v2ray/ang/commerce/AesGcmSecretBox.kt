package com.v2ray.ang.commerce

import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-GCM box used by [AndroidKeystoreSecretStore] and JVM tests.
 * Version byte + 12-byte IV + ciphertext+tag.
 */
class AesGcmSecretBox(private val key: SecretKey) {
    fun seal(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv ?: ByteArray(IV_BYTES).also { SecureRandom().nextBytes(it) }
        val encrypted = cipher.doFinal(plain)
        return byteArrayOf(VERSION) + iv + encrypted
    }

    fun open(blob: ByteArray): ByteArray {
        if (blob.size <= 1 + IV_BYTES || blob[0] != VERSION) {
            throw AEADBadTagException("corrupt secret blob")
        }
        val iv = blob.copyOfRange(1, 1 + IV_BYTES)
        val encrypted = blob.copyOfRange(1 + IV_BYTES, blob.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        return cipher.doFinal(encrypted)
    }

    companion object {
        const val VERSION: Byte = 1
        const val IV_BYTES = 12
        const val TAG_BITS = 128
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
