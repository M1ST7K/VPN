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

    @Test
    fun planetOverflowsFullScreenHostOnEveryMode() {
        phones.forEach { (w, h) ->
            HotFoxHeroMode.values().forEach { mode ->
                assertTrue(
                    "planet must overflow $mode $w x $h",
                    HotfoxHeroComposition.planetOverflowsHost(w, h, sphere, sphere, mode),
                )
                val planet = HotfoxHeroComposition.planet(w, h, sphere, sphere, mode)
                assertTrue(
                    "planet too small $mode $w: ${planet.diameter}",
                    planet.diameter >= w * 1.25f,
                )
                assertTrue(
                    "planet too huge $mode $w: ${planet.diameter}",
                    planet.diameter <= w * 1.60f + 0.5f,
                )
            }
        }
    }

    @Test
    fun foxDominatesWidthAndKeepsEarsOnScreen() {
        phones.forEach { (w, h) ->
            listOf(
                HotFoxHeroMode.SPLASH,
                HotFoxHeroMode.HOME_DISCONNECTED,
                HotFoxHeroMode.HOME_CONNECTING,
                HotFoxHeroMode.HOME_CONNECTED,
                HotFoxHeroMode.SUBSCRIPTION,
                HotFoxHeroMode.SUBSCRIPTION_INPUT,
            ).forEach { mode ->
                assertTrue(
                    "fox clipped on ${mode.name} $w x $h",
                    HotfoxHeroComposition.foxFitsInHost(w, h, mode),
                )
                val fox = HotfoxHeroComposition.fox(w, h, mode)
                assertTrue(
                    "fox too narrow on ${mode.name} $w: frac=${fox.widthFrac}",
                    fox.widthPx >= w * 0.62f,
                )
                assertTrue(
                    "fox too wide on ${mode.name} $w: frac=${fox.widthFrac}",
                    fox.widthPx <= w * 0.82f + 1f,
                )
                assertTrue("ears cropped on ${mode.name}", fox.top >= -1)
                assertTrue("muzzle cropped on ${mode.name}", fox.left + fox.widthPx <= w + 1f)
            }
        }
    }

    @Test
    fun foxSitsInTheMidScreenNotTheHeader() {
        val fox = HotfoxHeroComposition.fox(393f, 873f, HotFoxHeroMode.HOME_DISCONNECTED)
        val headY = fox.top + fox.heightPx * 0.36f
        val frac = headY / 873f
        assertTrue("disconnected fox head too high: $frac", frac >= 0.34f)
        assertTrue("disconnected fox head too low: $frac", frac <= 0.52f)
        val splash = HotfoxHeroComposition.fox(393f, 873f, HotFoxHeroMode.SPLASH)
        val splashHead = (splash.top + splash.heightPx * 0.36f) / 873f
        assertTrue("splash fox too high: $splashHead", splashHead >= 0.44f)
        assertTrue("splash fox too low: $splashHead", splashHead <= 0.62f)
        val sub = HotfoxHeroComposition.fox(393f, 873f, HotFoxHeroMode.SUBSCRIPTION)
        val subHead = sub.top + sub.heightPx * 0.36f
        assertTrue("subscription fox should sit lower", subHead > headY)
    }

    @Test
    fun vpnStateChangesDoNotJumpTheFox() {
        val hostH = 873f
        val disconnected = HotfoxHeroComposition.fox(393f, hostH, HotFoxHeroMode.HOME_DISCONNECTED)
        val connecting = HotfoxHeroComposition.fox(393f, hostH, HotFoxHeroMode.HOME_CONNECTING)
        val connected = HotfoxHeroComposition.fox(393f, hostH, HotFoxHeroMode.HOME_CONNECTED)
        assertTrue(abs(disconnected.centerY - connecting.centerY) <= 72f)
        assertTrue(abs(disconnected.centerY - connected.centerY) <= 72f)
    }

    @Test
    fun foxHasASlightRightBias() {
        val fox = HotfoxHeroComposition.fox(393f, 873f, HotFoxHeroMode.HOME_DISCONNECTED)
        val frac = fox.centerX / 393f
        assertTrue("fox too left: $frac", frac >= 0.50f)
        assertTrue("fox too right: $frac", frac <= 0.58f)
    }

    @Test
    fun legacyVariantsMapToFullscreenModes() {
        assertEquals(HotFoxHeroMode.SPLASH, HotfoxHeroComposition.Variant.SPLASH.toMode())
        assertEquals(HotFoxHeroMode.HOME_DISCONNECTED, HotfoxHeroComposition.Variant.PAGE.toMode())
        assertEquals(HotFoxHeroMode.SUBSCRIPTION_INPUT, HotfoxHeroComposition.Variant.SUPPORT.toMode())
        val page = HotfoxHeroComposition.planet(
            393f, 873f, sphere, sphere, HotfoxHeroComposition.Variant.PAGE,
        )
        assertEquals(393f * 0.46f, page.centerX, 0.1f)
        assertTrue(page.diameter > 393f)
    }
}
