package com.v2ray.ang.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HotfoxHomeCrashGuardTest {
    @Test
    fun connectActionDoesNotUseMaterialButtonBackgroundOverride() {
        val xml = layout("activity_main.xml")
        val idx = xml.indexOf("""android:id="@+id/connect_action"""")
        assertTrue("connect_action missing from activity_main.xml", idx > 0)
        val open = xml.lastIndexOf('<', idx)
        val window = xml.substring(open.coerceAtLeast(0), (idx + 40).coerceAtMost(xml.length))
        assertTrue(
            "connect_action must stay an AppCompatButton-based centered CTA, window=$window",
            window.contains("com.v2ray.ang.ui.HotfoxCenteredIconButton"),
        )
        assertFalse("MaterialButton + custom android:background crashes on ShapeAppearanceModel", window.contains("MaterialButton"))
        val button = File(
            "src/main/java/com/v2ray/ang/ui/HotfoxCenteredIconButton.kt",
        ).let { if (it.exists()) it else File("app/src/main/java/com/v2ray/ang/ui/HotfoxCenteredIconButton.kt") }
            .readText()
        assertTrue(button.contains(": AppCompatButton("))
        assertTrue("icon and label must be centered as one group", button.contains("canvas.translate(offset, 0f)"))
    }

    @Test
    fun shadowCaptionNeverEllipsizes() {
        val xml = layout("activity_main.xml")
        val idx = xml.indexOf("""android:id="@+id/tv_home_shadow_caption"""")
        assertTrue("tv_home_shadow_caption missing", idx > 0)
        val element = xml.substring(xml.lastIndexOf('<', idx), xml.indexOf("/>", idx))
        assertFalse("Shadow caption must fall back to a short phrase, not an ellipsis", element.contains("ellipsize"))
        assertTrue(element.contains("""android:maxLines="1""""))
    }

    @Test
    fun shadowAndSmartTitlesShrinkInsteadOfEllipsizing() {
        val xml = layout("activity_main.xml")
        listOf("tv_home_shadow_title", "tv_smart_routing_mode").forEach { id ->
            val idx = xml.indexOf("""android:id="@+id/$id"""")
            assertTrue("$id missing", idx > 0)
            val element = xml.substring(xml.lastIndexOf('<', idx), xml.indexOf("/>", idx))
            assertFalse("$id must not ellipsize to «Shad…»", element.contains("ellipsize"))
            assertTrue(element.contains("""android:layout_width="match_parent""""))
        }
        val main = File("src/main/java/com/v2ray/ang/ui/MainActivity.kt")
            .let { if (it.exists()) it else File("app/src/main/java/com/v2ray/ang/ui/MainActivity.kt") }
            .readText()
        assertTrue(main.contains("fitSingleLine(binding.root.findViewById(R.id.tv_home_shadow_title)"))
        assertTrue(main.contains("fitSingleLine(binding.tvSmartRoutingMode"))
    }

    @Test
    fun homeCardRowsStartOnThePageGutter() {
        val xml = layout("activity_main.xml")
        val idx = xml.indexOf("""android:id="@+id/layout_connection_rows"""")
        assertTrue("layout_connection_rows missing", idx > 0)
        val element = xml.substring(idx, xml.indexOf('>', idx))
        assertTrue(
            "card rows must match the runtime reading gutter from the first frame",
            element.contains("""android:paddingStart="@dimen/hf_page_gutter"""") &&
                element.contains("""android:paddingEnd="@dimen/hf_page_gutter""""),
        )
    }

    @Test
    fun onboardingPrimaryDoesNotUseMaterialButtonBackgroundOverride() {
        val xml = layout("activity_hotfox_onboarding.xml")
        val idx = xml.indexOf("""android:id="@+id/btn_onboarding_primary"""")
        assertTrue("btn_onboarding_primary missing", idx > 0)
        val open = xml.lastIndexOf('<', idx)
        val window = xml.substring(open.coerceAtLeast(0), (idx + 50).coerceAtMost(xml.length))
        assertTrue("onboarding primary must stay AppCompatButton, window=$window", window.contains("AppCompatButton"))
        assertFalse("MaterialButton + custom android:background crashes on ShapeAppearanceModel", window.contains("MaterialButton"))
    }

    @Test
    fun httpsImportPrimaryDoesNotUseMaterialButtonBackgroundOverride() {
        val xml = layout("activity_hotfox_https_import.xml")
        val idx = xml.indexOf("""android:id="@+id/btn_https_add"""")
        assertTrue("btn_https_add missing", idx > 0)
        val open = xml.lastIndexOf('<', idx)
        val window = xml.substring(open.coerceAtLeast(0), (idx + 50).coerceAtMost(xml.length))
        assertTrue("https import primary must stay AppCompatButton, window=$window", window.contains("AppCompatButton"))
        assertFalse("MaterialButton + custom android:background crashes on ShapeAppearanceModel", window.contains("MaterialButton"))
    }

    @Test
    fun httpsImportKeepsFormAboveAtmosphericHero() {
        val xml = layout("activity_hotfox_https_import.xml")
        val hero = xml.indexOf("""android:id="@+id/https_hero"""")
        val scrim = xml.indexOf("@drawable/hf_https_form_scrim")
        val foreground = xml.indexOf("""android:id="@+id/https_foreground"""")
        assertTrue("HTTPS hero missing", hero > 0)
        assertTrue("form scrim must sit above hero", scrim > hero)
        assertTrue("form must sit above readability scrim", foreground > scrim)
        assertTrue("security note must be constrained", xml.contains("""android:maxLines="2""""))
        assertTrue("HTTPS confirm must use compact utility surface", xml.contains("@drawable/hf_native_primary_compact"))
    }

    @Test
    fun onboardingHeroIsFullscreenBehindChrome() {
        val xml = layout("activity_hotfox_onboarding.xml")
        val heroIdx = xml.indexOf("""android:id="@+id/onboarding_hero"""")
        val slotIdx = xml.indexOf("""android:id="@+id/onboarding_hero_host"""")
        val foregroundIdx = xml.indexOf("""android:id="@+id/onboarding_foreground"""")
        assertTrue("onboarding_hero missing", heroIdx > 0)
        assertTrue("hero must sit behind the chrome column", heroIdx < foregroundIdx)
        assertTrue("hero must not live inside the weighted art slot", heroIdx < slotIdx)
        val heroWindow = xml.substring(heroIdx, (heroIdx + 900).coerceAtMost(xml.length))
        assertTrue("hero must remain fitParent", heroWindow.contains("hfHeroFitParent"))
        assertFalse("hero must not clip the planet", heroWindow.contains("""hfHeroClip="true""""))
    }

    @Test
    fun autoOrbitIsNativeHostedInArtSlot() {
        val xml = layout("activity_hotfox_onboarding.xml")
        assertTrue("native AUTO orbit view missing", xml.contains("com.v2ray.ang.ui.HotfoxAutoOrbitView"))
        assertTrue(xml.contains("""android:id="@+id/auto_orbit_view""""))
    }

    @Test
    fun hiddenHeroDoesNotBakeLargeBitmapSrc() {
        val xml = layout("view_hotfox_hero.xml")
        assertFalse(xml.contains("@drawable/hf_native_planet_sphere"))
        assertFalse(xml.contains("@drawable/hotfox_fox_master"))
    }

    private fun layout(name: String): String {
        val candidates = listOf(
            File("src/main/res/layout/$name"),
            File("app/src/main/res/layout/$name"),
            File("../../bootstrap/hotfox_2_2_0/app/src/main/res/layout/$name"),
            File("../../../bootstrap/hotfox_2_2_0/app/src/main/res/layout/$name"),
        )
        val file = candidates.firstOrNull { it.isFile }
        assertTrue("missing $name", file != null)
        return file!!.readText()
    }
}
