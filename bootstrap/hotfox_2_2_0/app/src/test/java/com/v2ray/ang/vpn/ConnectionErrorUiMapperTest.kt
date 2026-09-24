package com.v2ray.ang.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionErrorUiMapperTest {
    @Test
    fun diagnosticCodeIsNotTheTitle() {
        val socks = ConnectionErrorUiMapper.fromLastError("HF-VPN-014 socks-outbound-https")
        assertEquals("hotfox_error_socks_title", socks.titleResName)
        assertEquals("HF-VPN-014", socks.diagnosticCode)
        assertTrue(socks.titleResName != socks.diagnosticCode)

        val loop = ConnectionErrorUiMapper.fromLastError("HF-VPN-012")
        assertEquals("hotfox_error_loop_title", loop.titleResName)
        assertEquals(ConnectionErrorUiMapper.PrimaryAction.RETRY, loop.primaryAction)
    }

    @Test
    fun secretsAreNotCopiedIntoPresentation() {
        val leaked = ConnectionErrorUiMapper.fromLastError("vless://uuid@example.com:443")
        assertEquals("hotfox_headline_error", leaked.titleResName)
        assertEquals("", leaked.diagnosticCode)
        assertTrue(ConnectionErrorUiMapper.looksLikeSecret("https://secret.example/sub"))
        assertEquals("", ConnectionErrorUiMapper.sanitizedCode("https://secret.example/sub"))
    }

    @Test
    fun permissionAndEntitlementHaveRecoveryActions() {
        val permission = ConnectionErrorUiMapper.fromLastError("vpn permission missing")
        assertEquals(ConnectionErrorUiMapper.PrimaryAction.GRANT_VPN, permission.primaryAction)
        val entitlement = ConnectionErrorUiMapper.fromLastError("ENTITLEMENT_BLOCKED")
        assertEquals(ConnectionErrorUiMapper.PrimaryAction.OPEN_SUBSCRIPTION, entitlement.primaryAction)
        val expired = ConnectionErrorUiMapper.fromLastError("EXPIRED")
        assertEquals("hotfox_error_expired_title", expired.titleResName)
        val captive = ConnectionErrorUiMapper.fromLastError("WAIT_FOR_CAPTIVE_PORTAL")
        assertEquals("hotfox_error_captive_title", captive.titleResName)
        assertEquals(ConnectionErrorUiMapper.PrimaryAction.NONE, captive.primaryAction)
        val noTarget = ConnectionErrorUiMapper.fromLastError("NO_TARGET")
        assertEquals("hotfox_error_no_target_title", noTarget.titleResName)
    }
}
