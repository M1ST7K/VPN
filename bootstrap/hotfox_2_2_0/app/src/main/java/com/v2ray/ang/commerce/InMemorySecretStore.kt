package com.v2ray.ang.commerce

import java.util.concurrent.ConcurrentHashMap

/**
 * JVM test double. Can simulate corruption and keystore invalidation without crashing.
 */
class InMemorySecretStore : SecretStore {
    private enum class SlotKind { VALUE, CORRUPT, INVALIDATED }

    private data class Slot(val kind: SlotKind, val bytes: ByteArray? = null)

    private val slots = ConcurrentHashMap<String, Slot>()
    @Volatile var globallyInvalidated: Boolean = false
    @Volatile var failPuts: Boolean = false

    override fun put(key: String, plaintext: ByteArray): SecretPutResult {
        if (globallyInvalidated) return SecretPutResult.KeystoreInvalidated
        if (failPuts) return SecretPutResult.Failed
        slots[key] = Slot(SlotKind.VALUE, plaintext.copyOf())
        return SecretPutResult.Ok
    }

    override fun get(key: String): SecretGetResult {
        if (globallyInvalidated) return SecretGetResult.KeystoreInvalidated
        val slot = slots[key] ?: return SecretGetResult.Missing
        return when (slot.kind) {
            SlotKind.VALUE -> SecretGetResult.Value(slot.bytes ?: ByteArray(0))
            SlotKind.CORRUPT -> SecretGetResult.Corrupt
            SlotKind.INVALIDATED -> SecretGetResult.KeystoreInvalidated
        }
    }

    override fun delete(key: String) {
        slots.remove(key)
    }

    fun corrupt(key: String) {
        slots[key] = Slot(SlotKind.CORRUPT)
    }

    fun invalidateKey(key: String) {
        slots[key] = Slot(SlotKind.INVALIDATED)
    }

    fun invalidateAll() {
        globallyInvalidated = true
    }
}
