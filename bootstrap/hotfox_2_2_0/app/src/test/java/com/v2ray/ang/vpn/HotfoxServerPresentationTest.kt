package com.v2ray.ang.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HotfoxServerPresentationTest {
    @Test
    fun parsesKnownCityTokensWithoutInventingMissingCountryFromHost() {
        val amsterdam = HotfoxServerPresentation.fromRemark("Amsterdam NL01")
        assertEquals("Амстердам", amsterdam.title)
        assertEquals("Нидерланды", amsterdam.country)
    }

    @Test
    fun unknownRemarkKeepsRawTitle() {
        val custom = HotfoxServerPresentation.fromRemark("edge-node-42")
        assertEquals("edge-node-42", custom.title)
        assertNull(custom.country)
    }

    @Test
    fun affiliationMetadataWinsOverGuessing() {
        val shown = HotfoxServerPresentation.fromRemark("NL-AMS-01", affiliationCountry = "Нидерланды")
        assertEquals("NL-AMS-01", shown.title)
        assertEquals("Нидерланды", shown.country)
    }
}
