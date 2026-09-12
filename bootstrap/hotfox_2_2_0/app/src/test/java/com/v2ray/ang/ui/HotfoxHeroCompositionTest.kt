package com.v2ray.ang.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HotfoxHeroCompositionTest {
    private val sphere = 2048f
    private val phones = listOf(
        360f to 800f,
        375f to 812f,
        393f to 852f,
        412f to 915f,
        430f to 932f,
    )
    private val homeHosts = listOf(
        360f to 300f,
        375f to 320f,
        393f to 360f,
        412f to 380f,
        430f to 400f,
        1080f to 900f,
    )

    @Test
    fun pagePlanetOverflowsHostSoItIsNotARoundCard() {
        homeHosts.forEach { (w, h) ->
            assertTrue(
                "PAGE planet must overflow $w x $h",
                HotfoxHeroComposition.planetOverflowsHost(
                    w, h, sphere, sphere, HotfoxHeroComposition.Variant.PAGE,
                ),
            )
        }
    }

    @Test
    fun foxNeverExceedsHostOnRequiredPhones() {
        phones.forEach { (w, h) ->
            listOf(
                    HotfoxHeroComposition.Variant.SPLASH,
                    HotfoxHeroComposition.Variant.PAGE,
                    HotfoxHeroComposition.Variant.SUPPORT,
                ).forEach { variant ->
                val hostH = when (variant) {
                    HotfoxHeroComposition.Variant.SPLASH -> h
                    HotfoxHeroComposition.Variant.PAGE -> h * 0.42f
                    HotfoxHeroComposition.Variant.SUPPORT -> h * 0.38f
                }
                assertTrue(
                    "fox clipped on ${variant.name} $w x $hostH",
                    HotfoxHeroComposition.foxFitsInHost(w, hostH, variant),
                )
                val fox = HotfoxHeroComposition.fox(w, hostH, variant)
                assertTrue("fox too small on ${variant.name} $w", fox.sizePx >= w * 0.45f)
            }
        }
    }

    @Test
    fun pageAndSupportShareTheSameHorizontalFocalPoint() {
        val page = HotfoxHeroComposition.planet(
            393f, 360f, sphere, sphere, HotfoxHeroComposition.Variant.PAGE,
        )
        val support = HotfoxHeroComposition.planet(
            393f, 360f, sphere, sphere, HotfoxHeroComposition.Variant.SUPPORT,
        )
        assertEquals(393f * 0.50f, page.centerX, 0.1f)
        assertEquals(page.centerX, support.centerX, 0.1f)
    }

    @Test
    fun splashAndPageKeepFoxInsideASquareFitBox() {
        val splash = HotfoxHeroComposition.fox(393f, 780f, HotfoxHeroComposition.Variant.SPLASH)
        val page = HotfoxHeroComposition.fox(393f, 360f, HotfoxHeroComposition.Variant.PAGE)
        assertTrue(splash.centerVertically)
        assertTrue(!page.centerVertically)
        assertEquals(splash.sizePx, splash.sizePx)
        assertTrue(page.sizePx <= 360)
        assertTrue(splash.sizePx + splash.bottomMarginPx <= 780)
    }
}
