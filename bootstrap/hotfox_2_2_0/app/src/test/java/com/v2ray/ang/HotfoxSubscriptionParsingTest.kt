package com.v2ray.ang

import com.v2ray.ang.fmt.CustomFmt
import com.v2ray.ang.fmt.VlessFmt
import com.v2ray.ang.util.SubscriptionFormatDetector
import com.v2ray.ang.util.SubscriptionUserInfoParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.Base64

class HotfoxSubscriptionParsingTest {
    private val vless =
        "vless://11111111-2222-3333-4444-555555555555@vpn.example:443" +
            "?encryption=none&security=reality&pbk=publicKeyForTest&sid=1a2b3c4d" +
            "&sni=cdn.example&fp=chrome&type=grpc&serviceName=hotfox" +
            "&flow=xtls-rprx-vision#Reality%20GRPC"

    @Test
    fun detectsAndDecodesBase64VlessList() {
        val encoded = Base64.getEncoder().encodeToString("$vless\n$vless".toByteArray())
        assertEquals(SubscriptionFormatDetector.Format.BASE64_LIST, SubscriptionFormatDetector.detect(encoded))
    }

    @Test
    fun parsesJsonXrayProfile() {
        val json = """
            {
              "remarks":"JSON node",
              "outbounds":[{
                "protocol":"vless",
                "settings":{"vnext":[{"address":"json.example","port":443,"users":[]}]}
              }]
            }
        """.trimIndent()

        assertEquals(SubscriptionFormatDetector.Format.JSON, SubscriptionFormatDetector.detect(json))
        val parsed = CustomFmt.parse(json)
        assertEquals("JSON node", parsed.remarks)
        assertEquals("json.example", parsed.server)
        assertEquals("443", parsed.serverPort)
    }

    @Test
    fun parsesDirectVlessRealityVisionGrpcLink() {
        assertEquals(SubscriptionFormatDetector.Format.VLESS_LINK, SubscriptionFormatDetector.detect(vless))
        val parsed = VlessFmt.parse(vless)
        assertNotNull(parsed)
        assertEquals("vpn.example", parsed?.server)
        assertEquals("443", parsed?.serverPort)
        assertEquals("reality", parsed?.security)
        assertEquals("publicKeyForTest", parsed?.publicKey)
        assertEquals("1a2b3c4d", parsed?.shortId)
        assertEquals("grpc", parsed?.network)
        assertEquals("hotfox", parsed?.serviceName)
        assertEquals("xtls-rprx-vision", parsed?.flow)
    }

    @Test
    fun parsesVlessXhttpAndIpv6Endpoint() {
        val link = "vless://11111111-2222-3333-4444-555555555555@[2001:db8::8]:443" +
            "?encryption=none&security=reality&pbk=testPublicKey&sid=abcd" +
            "&type=xhttp&path=%2Fhotfox&mode=auto&flow=xtls-rprx-vision#IPv6-XHTTP"
        val parsed = VlessFmt.parse(link)
        assertNotNull(parsed)
        assertEquals("2001:db8::8", parsed?.server)
        assertEquals("xhttp", parsed?.network)
        assertEquals("/hotfox", parsed?.path)
        assertEquals("auto", parsed?.xhttpMode)
    }

    @Test
    fun normalizesLegacySplitHttpAndRawTransportNames() {
        val splitHttp = VlessFmt.parse(
            "vless://00000000-0000-4000-8000-000000000001@example.com:443" +
                "?encryption=none&security=reality&type=splithttp&path=%2Fapi&mode=auto" +
                "&pbk=public-key&sid=01#SplitHTTP"
        )
        val raw = VlessFmt.parse(
            "vless://00000000-0000-4000-8000-000000000002@example.com:443" +
                "?encryption=none&security=reality&type=raw&flow=xtls-rprx-vision" +
                "&pbk=public-key&sid=02#Raw"
        )

        assertEquals("xhttp", splitHttp?.network)
        assertEquals("tcp", raw?.network)
    }

    @Test
    fun parsesMinimalDirectVlessLinkWithDefaults() {
        val parsed = VlessFmt.parse(
            "vless://00000000-0000-4000-8000-000000000003@example.com:443#Minimal"
        )

        assertNotNull(parsed)
        assertEquals("example.com", parsed?.server)
        assertEquals("tcp", parsed?.network)
        assertEquals("none", parsed?.method)
    }

    @Test
    fun parsesSubscriptionUserInfoHeader() {
        val parsed = SubscriptionUserInfoParser.parse(
            "upload=1024; download=2048; total=10737418240; expire=1893456000",
        )
        assertNotNull(parsed)
        assertEquals(1024L, parsed?.uploadBytes)
        assertEquals(2048L, parsed?.downloadBytes)
        assertEquals(10737418240L, parsed?.totalBytes)
        assertEquals(1893456000L, parsed?.expireAtEpochSeconds)
    }

    @Test
    fun ignoresMalformedAndNegativeUserInfoValues() {
        val parsed = SubscriptionUserInfoParser.parse("UPLOAD=9; download=-1; total=nope")
        assertNotNull(parsed)
        assertEquals(9L, parsed?.uploadBytes)
        assertEquals(0L, parsed?.downloadBytes)
        assertEquals(0L, parsed?.totalBytes)
    }

    @Test
    fun parsesExpireMillisecondsAndDateAndBodyComment() {
        val millis = SubscriptionUserInfoParser.parse("upload=1; expire=1893456000000")
        assertEquals(1893456000L, millis?.expireAtEpochSeconds)

        val dated = SubscriptionUserInfoParser.parse("expire=2026-10-05")
        val expected = java.time.LocalDate.of(2026, 10, 5).atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC)
        assertEquals(expected, dated?.expireAtEpochSeconds)

        val profileExpire = SubscriptionUserInfoParser.parse("1893456000")
        assertEquals(1893456000L, profileExpire?.expireAtEpochSeconds)

        val body = SubscriptionUserInfoParser.parse(
            header = null,
            body = "# subscription-userinfo: upload=2; download=3; total=4; expire=1893456000",
        )
        assertEquals(2L, body?.uploadBytes)
        assertEquals(1893456000L, body?.expireAtEpochSeconds)
    }

    @Test
    fun missingExpireStaysUnknown() {
        val parsed = SubscriptionUserInfoParser.parse("upload=8; download=9; total=10")
        assertEquals(0L, parsed?.expireAtEpochSeconds)
    }

    @Test
    fun collectHeadersPrefersSubscriptionUserinfo() {
        val collected = SubscriptionUserInfoParser.collectHeaders { name ->
            when (name.lowercase()) {
                "profile-expire" -> "1893456000"
                "subscription-userinfo" -> "upload=1; download=2; total=3; expire=1893456000"
                else -> null
            }
        }
        val parsed = SubscriptionUserInfoParser.parse(collected)
        assertEquals(1893456000L, parsed?.expireAtEpochSeconds)
        assertEquals(1L, parsed?.uploadBytes)
    }
}
