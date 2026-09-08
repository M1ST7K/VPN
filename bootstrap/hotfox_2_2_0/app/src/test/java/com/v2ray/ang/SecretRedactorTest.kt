package com.v2ray.ang

import com.v2ray.ang.util.SecretRedactor
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecretRedactorTest {
    @Test
    fun redactsVlessUuidAndQuerySecrets() {
        val raw = "vless://11111111-2222-3333-4444-555555555555@vpn.example:443?pbk=publicKeyForTest&sid=abcd"
        val redacted = SecretRedactor.redact(raw)
        assertFalse(redacted.contains("11111111-2222-3333-4444-555555555555"))
        assertFalse(redacted.contains("publicKeyForTest"))
        assertTrue(redacted.contains("<secret-uri>"))
    }

    @Test
    fun redactsSubscriptionUrlAndAuthorization() {
        val raw = "GET https://provider.example/sub/sUq75ktDSV7LmQuB Authorization: Bearer super-secret-token-value-1234"
        val redacted = SecretRedactor.redact(raw)
        assertFalse(redacted.contains("sUq75ktDSV7LmQuB"))
        assertFalse(redacted.contains("super-secret-token-value-1234"))
        assertTrue(redacted.contains("<url>"))
        assertTrue(redacted.contains("<redacted>"))
    }
}
