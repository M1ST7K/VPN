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

    @Test
    fun redactsKeystorePasswords() {
        val raw = "HOTFOX_KEYSTORE_PASSWORD=super-secret-store-pass HOTFOX_KEY_PASSWORD=another-secret-key"
        val redacted = SecretRedactor.redact(raw)
        assertFalse(redacted.contains("super-secret-store-pass"))
        assertFalse(redacted.contains("another-secret-key"))
        assertTrue(redacted.contains("<signing-secret>"))
    }

    @Test
    fun redactsLongTokensAfterSigningSecrets() {
        val raw = "restoreToken=abcdefghijklmnopqrstuvwx"
        val redacted = SecretRedactor.redact(raw)
        assertFalse(redacted.contains("abcdefghijklmnopqrstuvwx"))
        assertTrue(redacted.contains("<secret>") || redacted.contains("<redacted>"))
    }

    @Test
    fun doesNotSwallowGitShaDiagnosticLabel() {
        val raw = "gitSha=0123456789abcdef0123456789abcdef01234567"
        val redacted = SecretRedactor.redact(raw)
        assertTrue(redacted.contains("gitSha="))
    }
}
