package com.v2ray.ang.commerce

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import android.util.Base64
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import javax.crypto.AEADBadTagException
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Android Keystore AES-GCM secret store. Ciphertext lives in a no-backup file, never in plain MMKV.
 * Invalidation or corrupt blobs surface as restore-required results instead of crashing.
 */
class AndroidKeystoreSecretStore(context: Context) : SecretStore {
    private val file = AtomicFile(File(File(context.noBackupFilesDir, "secure").apply { mkdirs() }, FILE_NAME))

    override fun put(key: String, plaintext: ByteArray): SecretPutResult {
        return try {
            val sealed = AesGcmSecretBox(masterKey()).seal(plain = plaintext)
            val map = readMap().toMutableMap()
            map[key] = Base64.encodeToString(sealed, Base64.NO_WRAP)
            writeMap(map)
            SecretPutResult.Ok
        } catch (_: KeyPermanentlyInvalidatedException) {
            SecretPutResult.KeystoreInvalidated
        } catch (_: UnrecoverableKeyException) {
            SecretPutResult.KeystoreInvalidated
        } catch (_: Exception) {
            SecretPutResult.Failed
        }
    }

    override fun get(key: String): SecretGetResult {
        return try {
            val encoded = readMap()[key] ?: return SecretGetResult.Missing
            val blob = Base64.decode(encoded, Base64.NO_WRAP)
            SecretGetResult.Value(AesGcmSecretBox(masterKey()).open(blob))
        } catch (_: KeyPermanentlyInvalidatedException) {
            SecretGetResult.KeystoreInvalidated
        } catch (_: UnrecoverableKeyException) {
            SecretGetResult.KeystoreInvalidated
        } catch (_: AEADBadTagException) {
            SecretGetResult.Corrupt
        } catch (_: IllegalArgumentException) {
            SecretGetResult.Corrupt
        } catch (_: Exception) {
            SecretGetResult.Corrupt
        }
    }

    override fun delete(key: String) {
        try {
            val map = readMap().toMutableMap()
            map.remove(key)
            writeMap(map)
        } catch (_: Exception) {
            // Deletion failure must not crash; a later get still reports restore if needed.
        }
    }

    private fun readMap(): Map<String, String> {
        if (!file.baseFile.exists()) return emptyMap()
        val json = file.readFully().toString(Charsets.UTF_8)
        if (json.isBlank()) return emptyMap()
        val root = JSONObject(json)
        val out = LinkedHashMap<String, String>()
        root.keys().forEach { name -> out[name] = root.getString(name) }
        return out
    }

    private fun writeMap(map: Map<String, String>) {
        val root = JSONObject()
        map.forEach { (k, v) -> root.put(k, v) }
        var stream: java.io.FileOutputStream? = null
        try {
            stream = file.startWrite()
            stream.write(root.toString().toByteArray())
            stream.fd.sync()
            file.finishWrite(stream)
        } catch (error: Throwable) {
            stream?.let(file::failWrite)
            throw error
        }
    }

    private fun masterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(MASTER_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                MASTER_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val MASTER_ALIAS = "hotfox_secretstore_v1"
        private const val FILE_NAME = "secretstore_v1.json"
    }
}
