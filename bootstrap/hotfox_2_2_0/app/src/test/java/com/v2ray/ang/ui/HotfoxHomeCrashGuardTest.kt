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
        assertTrue("connect_action must stay AppCompatButton, window=$window", window.contains("AppCompatButton"))
        assertFalse("MaterialButton + custom android:background crashes on ShapeAppearanceModel", window.contains("MaterialButton"))
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
