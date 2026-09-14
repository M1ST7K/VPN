package com.v2ray.ang.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class HotfoxHeroCompositionTest {
    private val sphere = 2048f
    /** Phone canvases. Home fox is composed against the screen, not HeroSlot leftover. */
    private val phones = listOf(
        360f to 800f,
        375f to 812f,
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
            assertEquals(w * HomeHeroGeometry.FOX_CENTER_X, fox.centerX, 0.5f)
            assertTrue("fox below min top $w x $h top=${fox.top}", fox.top + 1f >= h * HomeHeroGeometry.FOX_MIN_TOP_FRAC)
            assertTrue(
                "fox covers CTA band $w x $h bottom=${fox.bottom}",
                fox.bottom <= h * HomeHeroGeometry.FOX_SAFE_BOTTOM_FRAC + 1f,
            )
            val planet = HotfoxHeroComposition.planet(w, h, sphere, sphere, HotFoxHeroMode.HOME_DISCONNECTED)
            assertEquals(w * HomeHeroGeometry.PLANET_WIDTH, planet.diameter, 0.5f)
            assertEquals(w * HomeHeroGeometry.PLANET_CENTER_X, planet.centerX, 0.5f)
            assertEquals(h * HomeHeroGeometry.PLANET_CENTER_Y, planet.centerY, 0.5f)
        }
    }

    @Test
    fun homeFoxFillsHeadlineToCtaBand() {
        val w = 393f
        val h = 873f
        val fox = HotfoxHeroComposition.fox(w, h, HotFoxHeroMode.HOME_DISCONNECTED)
        assertTrue("fox too narrow ${fox.widthFrac}", fox.widthFrac >= 0.62f)
        assertTrue("fox wider than spec ${fox.widthFrac}", fox.widthFrac <= HomeHeroGeometry.FOX_WIDTH + 0.001f)
        assertTrue("fox too short ${fox.heightPx}", fox.heightPx >= h * 0.30f)
        assertTrue("muzzle not low enough ${fox.bottom}", fox.bottom >= h * 0.46f)
        assertTrue("fox covers CTA ${fox.bottom}", fox.bottom <= h * HomeHeroGeometry.FOX_SAFE_BOTTOM_FRAC + 1f)
        homeModes.forEach { mode ->
            val other = HotfoxHeroComposition.fox(w, h, mode)
            assertEquals(fox.left, other.left)
            assertEquals(fox.top, other.top)
            assertEquals(fox.widthPx, other.widthPx)
            assertEquals(fox.heightPx, other.heightPx)
        }
    }

    @Test
    fun planetMayOverflowHeroSlotOnly() {
        phones.forEach { (w, h) ->
            homeModes.forEach { mode ->
                val planet = HotfoxHeroComposition.planet(w, h, sphere, sphere, mode)
                assertTrue(planet.diameter >= w * 1.20f)
                assertTrue(planet.diameter <= w * 1.35f + 0.5f)
            }
            val splash = HotfoxHeroComposition.planet(w, h, 1080f, 1400f, HotFoxHeroMode.SPLASH)
            assertTrue("splash planet must cover height ${splash.layoutHeight} vs $h", splash.layoutHeight >= h)
            assertTrue("splash planet left crop ${splash.left}", splash.left <= 0)
            assertTrue("splash planet top crop ${splash.top}", splash.top <= 0)
            assertTrue(
                "splash planet right crop",
                splash.left + splash.layoutWidth >= w,
            )
            assertTrue(
                "splash planet bottom crop",
                splash.top + splash.layoutHeight >= h,
            )
        }
    }

    @Test
    fun foxKeepsAspectAndIsNeverCropped() {
        phones.forEach { (w, h) ->
            HotFoxHeroMode.values().forEach { mode ->
                val hostH = if (HotfoxHeroComposition.isHome(mode)) h else 800f
                val fox = HotfoxHeroComposition.fox(w, hostH, mode)
                val aspect = fox.widthPx.toFloat() / fox.heightPx.toFloat()
                assertEquals(HotfoxHeroComposition.FOX_ASPECT, aspect, 0.01f)
                if (HotfoxHeroComposition.isHome(mode)) {
                    assertTrue("ears into status ${mode.name}", fox.top + 1f >= hostH * HomeHeroGeometry.FOX_MIN_TOP_FRAC)
                    assertTrue("muzzle over CTA ${mode.name}", fox.bottom <= hostH * HomeHeroGeometry.FOX_SAFE_BOTTOM_FRAC + 1f)
                } else {
                    val spec = HotfoxHeroComposition.spec(mode)
                    assertTrue("ears cropped ${mode.name}", fox.top + 1f >= hostH * spec.foxTopMinFrac)
                    assertTrue(
                        "muzzle over CTA ${mode.name}",
                        fox.bottom <= hostH * spec.foxBottomMaxFrac + 1f,
                    )
                }
                assertTrue("muzzle cropped ${mode.name}", fox.left + fox.widthPx <= w + 1f)
            }
        }
    }

    @Test
    fun homeBackdropPlanetFillsTheScreen() {
        val w = 393f
        val h = 873f
        val planet = HotfoxHeroComposition.fullscreenPlanet(w, h, sphere, sphere)
        assertTrue("planet shorter than viewport ${planet.diameter}", planet.diameter >= h)
        assertTrue("planet does not cover width", planet.left <= 0 && planet.left + planet.diameter >= w)
        assertTrue("planet does not cover height", planet.top + planet.diameter >= h * 0.9f)
        assertEquals(w * HomePlanetBackdropGeometry.CENTER_X, planet.centerX, 0.5f)
        assertEquals(h * HomePlanetBackdropGeometry.CENTER_Y, planet.centerY, 0.5f)
        val other = HotfoxHeroComposition.fullscreenPlanet(w, h, sphere, sphere)
        assertEquals(planet.diameter, other.diameter, 0f)
        assertEquals(planet.left, other.left)
        assertEquals(planet.top, other.top)
    }

    @Test
    fun homeFoxWidthIsStableAcrossStates() {
        val fox = HotfoxHeroComposition.fox(393f, 873f, HotFoxHeroMode.HOME_CONNECTING)
        assertTrue(fox.widthFrac >= 0.62f)
        assertTrue(fox.widthFrac <= HomeHeroGeometry.FOX_WIDTH + 0.001f)
        assertEquals(
            HotfoxHeroComposition.fox(393f, 873f, HotFoxHeroMode.HOME_DISCONNECTED).widthFrac,
            fox.widthFrac,
            0f,
        )
    }

    @Test
    fun homeFoxDoesNotUseShortHeroSlot() {
        val slot = HotfoxHeroComposition.fox(393f, 262f, HotFoxHeroMode.HOME_DISCONNECTED)
        val screen = HotfoxHeroComposition.fox(393f, 873f, HotFoxHeroMode.HOME_DISCONNECTED)
        assertTrue("slot leftover still shrinks the bust ${slot.heightPx} vs ${screen.heightPx}", screen.heightPx > slot.heightPx)
        assertTrue(HotfoxHeroComposition.foxFitsInHost(393f, 873f, HotFoxHeroMode.HOME_DISCONNECTED))
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
        val disconnected = HotfoxHeroComposition.fox(393f, 873f, HotFoxHeroMode.HOME_DISCONNECTED)
        val connected = HotfoxHeroComposition.fox(393f, 873f, HotFoxHeroMode.HOME_CONNECTED)
        assertEquals(0f, abs(disconnected.centerY - connected.centerY), 0.01f)
    }

    @Test
    fun splashFoxIsLargerThanConnectFox() {
        val splash = HotfoxHeroComposition.fox(360f, 800f, HotFoxHeroMode.SPLASH)
        val connect = HotfoxHeroComposition.fox(360f, 800f, HotFoxHeroMode.SUBSCRIPTION)
        assertTrue("splash fox should be taller than connect fox", splash.heightPx > connect.heightPx)
        assertTrue("connect fox stays below title", connect.top + 1f >= 800f * 0.28f)
        assertTrue("connect fox stays above CTA", connect.bottom <= 800f * 0.73f + 1f)
        assertTrue("splash fox stays on canvas", splash.top >= -1 && splash.bottom <= 801)
    }

    @Test
    fun onboardingPlanetCoversThePhoneNotASlot() {
        val dw = 1080f
        val dh = 1400f
        phones.forEach { (w, h) ->
            listOf(HotFoxHeroMode.SPLASH, HotFoxHeroMode.SUBSCRIPTION).forEach { mode ->
                val planet = HotfoxHeroComposition.planet(w, h, dw, dh, mode)
                assertTrue("${mode.name} left crop ${planet.left}", planet.left <= 0)
                assertTrue("${mode.name} top crop ${planet.top}", planet.top <= 0)
                assertTrue(
                    "${mode.name} right crop",
                    planet.left + planet.layoutWidth >= w,
                )
                assertTrue(
                    "${mode.name} bottom crop ${planet.top + planet.layoutHeight} vs $h",
                    planet.top + planet.layoutHeight >= h,
                )
                assertTrue(
                    "${mode.name} must not use a width-only square shorter than the phone",
                    planet.layoutHeight > w * 1.38f,
                )
            }
        }
    }
}
