package com.v2ray.ang.ops

import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

object HotfoxOpsCrypto {
    fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { b -> "%02x".format(b.toInt() and 0xFF) }
    }

    fun isSha256Hex(value: String): Boolean =
        value.length == 64 && value.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }

    fun decodeHex(hex: String): ByteArray? {
        val trimmed = hex.trim()
        if (trimmed.length % 2 != 0) return null
        if (trimmed.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) return null
        return ByteArray(trimmed.length / 2) { i ->
            trimmed.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    fun verifyEcdsaP256(payload: ByteArray, signatureHex: String, publicKeySpkiHex: String): Boolean {
        val signature = decodeHex(signatureHex) ?: return false
        val spki = decodeHex(publicKeySpkiHex) ?: return false
        if (payload.isEmpty() || signature.isEmpty() || spki.isEmpty()) return false
        return runCatching {
            val key = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(spki))
            val verifier = Signature.getInstance("SHA256withECDSA")
            verifier.initVerify(key)
            verifier.update(payload)
            verifier.verify(signature)
        }.getOrDefault(false)
    }
}
