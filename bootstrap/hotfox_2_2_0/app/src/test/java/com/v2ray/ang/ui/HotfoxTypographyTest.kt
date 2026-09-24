package com.v2ray.ang.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HotfoxTypographyTest {
    private val staticWeights = mapOf(
        "onest_regular" to 400,
        "onest_medium" to 500,
        "onest_semibold" to 600,
        "onest_bold" to 700,
    )

    @Test
    fun onestStaticWeightsAreBundledLocally() {
        val fonts = res("font")
        staticWeights.keys.forEach { name ->
            val ttf = File(fonts, "$name.ttf")
            assertTrue("$name.ttf missing", ttf.isFile && ttf.length() > 50_000)
        }
        val extra = fonts.listFiles().orEmpty().map { it.name }
            .filter { it.startsWith("onest") && it.removeSuffix(".ttf") !in staticWeights }
        assertTrue("only 400/500/600/700 may be bundled, found $extra", extra.isEmpty())
    }

    @Test
    fun familyMapsEachWeightToItsStaticFileForApi24() {
        val family = File(res("font"), "hotfox_onest.xml").readText()
        assertFalse("no downloadable font provider", family.contains("fontProviderAuthority"))
        staticWeights.forEach { (name, weight) ->
            assertTrue(family.contains("""android:font="@font/$name""""))
            assertTrue("AppCompat attrs needed below API 26", family.contains("""app:font="@font/$name""""))
            assertTrue(family.contains("""app:fontWeight="$weight""""))
        }
    }

    @Test
    fun appThemeUsesOnestAndRolesNeverSynthesizeWeights() {
        val themes = File(res("values"), "themes.xml").readText()
        val editorial = themes.substringAfter("""<style name="HotFoxEditorialTheme"""").substringBefore("</style>")
        assertTrue(editorial.contains("""<item name="android:fontFamily">@font/hotfox_onest</item>"""))
        assertTrue(editorial.contains("""<item name="fontFamily">@font/hotfox_onest</item>"""))

        val typography = File(res("values"), "hf_typography.xml").readText()
        Regex("""<style name="(HotFox\.Type\.[A-Za-z]+)"[^>/]*>(.*?)</style>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(typography)
            .filter { it.groupValues[2].contains("fontFamily") }
            .forEach { match ->
                assertTrue("${match.groupValues[1]} must pin textStyle normal", match.groupValues[2].contains("""android:textStyle">normal"""))
            }
    }

    @Test
    fun weightedRolesInOnboardingAndHomeAreNotAlsoBold() {
        listOf("activity_main.xml", "activity_hotfox_onboarding.xml", "activity_hotfox_https_import.xml", "activity_hotfox_splash.xml")
            .forEach { name ->
                val xml = File(res("layout"), name).readText()
                Regex("""<[\w.]+\s[^>]*?style="@style/HotFox\.Type\.[^"]+"[^>]*>""").findAll(xml).forEach { element ->
                    assertFalse("$name: role style plus textStyle=bold fakes a weight: ${element.value.take(120)}", element.value.contains("""textStyle="bold""""))
                }
            }
        val home = File(res("layout"), "activity_main.xml").readText()
        val connect = home.substring(home.lastIndexOf('<', home.indexOf("""android:id="@+id/connect_action"""")))
            .substringBefore("/>")
        assertTrue(connect.contains("""style="@style/HotFox.Type.Button""""))
    }

    @Test
    fun oflLicenseShipsWithTheApp() {
        val license = File(res("font").absoluteFile.parentFile.parentFile, "assets/licenses/ONEST_OFL.txt")
        assertTrue("assets/licenses/ONEST_OFL.txt missing", license.isFile)
        assertTrue(license.readText().contains("SIL OPEN FONT LICENSE Version 1.1"))
        assertEquals(1, license.readText().lines().count { it.contains("The Onest Project Authors") })
    }

    private fun res(dir: String): File {
        val candidates = listOf(
            File("src/main/res/$dir"),
            File("app/src/main/res/$dir"),
            File("../../bootstrap/hotfox_2_2_0/app/src/main/res/$dir"),
        )
        return candidates.firstOrNull { it.exists() } ?: error("res/$dir not found from ${File(".").absolutePath}")
    }
}
