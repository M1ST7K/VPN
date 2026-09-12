package com.v2ray.ang.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class HotfoxHeroCompositionTest {
    private val sphere = 2048f
    private val phones = listOf(
        360f to 800f,
        375f to 812f,
        393f to 852f,
        393f to 873f,
        412f to 915f,
        430f to 932f,
    )
    private val homeModes = listOf(
        HotFoxHeroMode.HOME_DISCONNECTED,
        HotFoxHeroMode.HOME_CONNECTING,
        HotFoxHeroMode.HOME_CONNECTED,
    )

    @Test
    fun homeStatesShareLockedGeometry() {
        phones.forEach { (w, h) ->
            assertTrue("home geometry drifted on $w x $h", HotfoxHeroComposition.homeStatesShareGeometry(w, h))
            val fox = HotfoxHeroComposition.fox(w, h, HotFoxHeroMode.HOME_DISCONNECTED)
            assertEquals(0.62f, fox.widthFrac, 0.001f)
            assertEquals(w * 0.54f, fox.centerX, 0.5f)
            assertEquals(h * 0.49f, fox.centerY, 0.5f)
            val bottomFrac = fox.bottom / h
            assertTrue("fox too low on $w x $h: $bottomFrac", bottomFrac <= 0.68f)
            assertTrue("fox too high on $w x $h: $bottomFrac", bottomFrac >= 0.60f)
            val planet = HotfoxHeroComposition.planet(w, h, sphere, sphere, HotFoxHeroMode.HOME_DISCONNECTED)
            assertEquals(w * 1.18f, planet.diameter, 0.5f)
            assertEquals(w * 0.43f, planet.centerX, 0.5f)
            assertEquals(h * 0.43f, planet.centerY, 0.5f)
        }
    }

    @Test
    fun homeFoxStaysInArtworkSafeZone() {
        val fox = HotfoxHeroComposition.fox(393f, 873f, HotFoxHeroMode.HOME_DISCONNECTED)
        assertTrue("fox top entered header ${fox.top / 873f}", fox.top / 873f >= 0.22f)
        assertTrue("fox bottom entered nav ${fox.bottom / 873f}", fox.bottom / 873f <= 0.70f)
        homeModes.forEach { mode ->
            val other = HotfoxHeroComposition.fox(393f, 873f, mode)
            assertEquals(fox.left, other.left)
            assertEquals(fox.top, other.top)
            assertEquals(fox.widthPx, other.widthPx)
            assertEquals(fox.heightPx, other.heightPx)
        }
    }

    @Test
    fun planetSizeIsControlled() {
        phones.forEach { (w, h) ->
            homeModes.forEach { mode ->
                val planet = HotfoxHeroComposition.planet(w, h, sphere, sphere, mode)
                assertTrue(planet.diameter >= w * 1.10f)
                assertTrue(planet.diameter <= w * 1.25f + 0.5f)
            }
            val splash = HotfoxHeroComposition.planet(w, h, sphere, sphere, HotFoxHeroMode.SPLASH)
            assertTrue(splash.diameter > w)
            assertTrue(splash.diameter <= w * 1.30f)
        }
    }

    @Test
    fun foxKeepsAspectAndIsNeverCropped() {
        phones.forEach { (w, h) ->
            HotFoxHeroMode.values().forEach { mode ->
                val fox = HotfoxHeroComposition.fox(w, h, mode)
                val aspect = fox.widthPx.toFloat() / fox.heightPx.toFloat()
                assertEquals(HotfoxHeroComposition.FOX_ASPECT, aspect, 0.01f)
                assertTrue("ears cropped ${mode.name}", fox.top >= -1)
                assertTrue("muzzle cropped ${mode.name}", fox.left + fox.widthPx <= w + 1f)
            }
        }
    }

    @Test
    fun homeFoxWidthIsLocked() {
        val fox = HotfoxHeroComposition.fox(412f, 915f, HotFoxHeroMode.HOME_CONNECTING)
        assertTrue(fox.widthFrac >= 0.58f)
        assertTrue(fox.widthFrac <= 0.64f)
        assertEquals(
            HotfoxHeroComposition.fox(412f, 915f, HotFoxHeroMode.HOME_DISCONNECTED).widthFrac,
            fox.widthFrac,
            0f,
        )
    }

    @Test
    fun legacyVariantsMapToFullscreenModes() {
        assertEquals(HotFoxHeroMode.SPLASH, HotfoxHeroComposition.Variant.SPLASH.toMode())
        assertEquals(HotFoxHeroMode.HOME_DISCONNECTED, HotfoxHeroComposition.Variant.PAGE.toMode())
        assertEquals(HotFoxHeroMode.SUBSCRIPTION_INPUT, HotfoxHeroComposition.Variant.SUPPORT.toMode())
        val page = HotfoxHeroComposition.planet(
            393f, 873f, sphere, sphere, HotfoxHeroComposition.Variant.PAGE,
        )
        assertEquals(393f * 0.43f, page.centerX, 0.1f)
        val disconnected = HotfoxHeroComposition.fox(393f, 873f, HotFoxHeroMode.HOME_DISCONNECTED)
        val connected = HotfoxHeroComposition.fox(393f, 873f, HotFoxHeroMode.HOME_CONNECTED)
        assertEquals(0f, abs(disconnected.centerY - connected.centerY), 0.01f)
    }
}
