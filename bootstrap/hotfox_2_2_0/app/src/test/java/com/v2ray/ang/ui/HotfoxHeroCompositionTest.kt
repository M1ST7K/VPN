package com.v2ray.ang.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class HotfoxHeroCompositionTest {
    private val sphere = 2048f
    /** HeroSlot-sized hosts, not full-screen posters. */
    private val slots = listOf(
        360f to 216f,
        375f to 228f,
        393f to 240f,
        393f to 262f,
        412f to 252f,
        430f to 264f,
    )
    private val homeModes = listOf(
        HotFoxHeroMode.HOME_DISCONNECTED,
        HotFoxHeroMode.HOME_CONNECTING,
        HotFoxHeroMode.HOME_CONNECTED,
    )

    @Test
    fun homeStatesShareLockedGeometry() {
        slots.forEach { (w, h) ->
            assertTrue("home geometry drifted on $w x $h", HotfoxHeroComposition.homeStatesShareGeometry(w, h))
            val fox = HotfoxHeroComposition.fox(w, h, HotFoxHeroMode.HOME_DISCONNECTED)
            assertEquals(w * HomeHeroGeometry.FOX_CENTER_X, fox.centerX, 0.5f)
            assertEquals(h * HomeHeroGeometry.FOX_CENTER_Y, fox.centerY, 0.5f)
            assertTrue("fox escaped slot bottom $w x $h", fox.bottom <= h + 1f)
            assertTrue("fox escaped slot top $w x $h", fox.top >= -1)
            val planet = HotfoxHeroComposition.planet(w, h, sphere, sphere, HotFoxHeroMode.HOME_DISCONNECTED)
            assertEquals(w * HomeHeroGeometry.PLANET_WIDTH, planet.diameter, 0.5f)
            assertEquals(w * HomeHeroGeometry.PLANET_CENTER_X, planet.centerX, 0.5f)
            assertEquals(h * HomeHeroGeometry.PLANET_CENTER_Y, planet.centerY, 0.5f)
        }
    }

    @Test
    fun homeFoxFitsInsideHeroSlot() {
        val fox = HotfoxHeroComposition.fox(393f, 262f, HotFoxHeroMode.HOME_DISCONNECTED)
        assertTrue("fox top ${fox.top}", fox.top >= -1)
        assertTrue("fox bottom ${fox.bottom} slot=262", fox.bottom <= 263)
        assertTrue("fox not large enough ${fox.widthFrac}", fox.widthFrac >= 0.50f)
        assertTrue("fox wider than slot ${fox.widthFrac}", fox.widthFrac <= HomeHeroGeometry.FOX_WIDTH + 0.001f)
        homeModes.forEach { mode ->
            val other = HotfoxHeroComposition.fox(393f, 262f, mode)
            assertEquals(fox.left, other.left)
            assertEquals(fox.top, other.top)
            assertEquals(fox.widthPx, other.widthPx)
            assertEquals(fox.heightPx, other.heightPx)
        }
    }

    @Test
    fun planetMayOverflowHeroSlotOnly() {
        slots.forEach { (w, h) ->
            homeModes.forEach { mode ->
                val planet = HotfoxHeroComposition.planet(w, h, sphere, sphere, mode)
                assertTrue(planet.diameter >= w * 1.20f)
                assertTrue(planet.diameter <= w * 1.35f + 0.5f)
            }
            val splash = HotfoxHeroComposition.planet(w, 800f, sphere, sphere, HotFoxHeroMode.SPLASH)
            assertTrue(splash.diameter > w)
            assertTrue(splash.diameter <= w * 1.30f)
        }
    }

    @Test
    fun foxKeepsAspectAndIsNeverCropped() {
        slots.forEach { (w, h) ->
            HotFoxHeroMode.values().forEach { mode ->
                val hostH = if (HotfoxHeroComposition.isHome(mode)) h else 800f
                val fox = HotfoxHeroComposition.fox(w, hostH, mode)
                val aspect = fox.widthPx.toFloat() / fox.heightPx.toFloat()
                assertEquals(HotfoxHeroComposition.FOX_ASPECT, aspect, 0.01f)
                assertTrue("ears cropped ${mode.name}", fox.top >= -1)
                assertTrue("muzzle cropped ${mode.name}", fox.left + fox.widthPx <= w + 1f)
            }
        }
    }

    @Test
    fun homeFoxWidthIsStableAcrossStates() {
        val fox = HotfoxHeroComposition.fox(412f, 252f, HotFoxHeroMode.HOME_CONNECTING)
        assertTrue(fox.widthFrac >= 0.50f)
        assertTrue(fox.widthFrac <= HomeHeroGeometry.FOX_WIDTH + 0.001f)
        assertEquals(
            HotfoxHeroComposition.fox(412f, 252f, HotFoxHeroMode.HOME_DISCONNECTED).widthFrac,
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
            393f, 262f, sphere, sphere, HotfoxHeroComposition.Variant.PAGE,
        )
        assertEquals(393f * HomeHeroGeometry.PLANET_CENTER_X, page.centerX, 0.1f)
        val disconnected = HotfoxHeroComposition.fox(393f, 262f, HotFoxHeroMode.HOME_DISCONNECTED)
        val connected = HotfoxHeroComposition.fox(393f, 262f, HotFoxHeroMode.HOME_CONNECTED)
        assertEquals(0f, abs(disconnected.centerY - connected.centerY), 0.01f)
    }
}
