package com.v2ray.ang.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HotfoxOnboardingActionWiringTest {
    @Test
    fun subscriptionCtaOpensImportNotVpn() {
        val src = source("HotfoxOnboardingActivity.kt")
        assertTrue(src.contains("HotfoxHttpsImportActivity"))
        assertTrue(src.contains("awaitingImport"))
        assertTrue(src.contains("requestImport.launch"))
        val primary = src.substring(src.indexOf("private fun onPrimary()"))
        val accessArm = primary.substringAfter("HotfoxOnboardingFlow.Step.ACCESS")
            .substringBefore("HotfoxOnboardingFlow.Step.FIRST_CONNECTION")
        assertTrue(accessArm.contains("facts().hasAccess"))
        assertTrue(accessArm.contains("requestImport.launch"))
        assertFalse(
            "WELCOME/ACCESS must not call requestVpn()",
            accessArm.contains("requestVpn()"),
        )
    }

    @Test
    fun manualOpensServerPickerWithoutClearingAutoFirst() {
        val src = source("HotfoxOnboardingActivity.kt")
        assertTrue(src.contains("EXTRA_ONBOARDING_SERVER_PICK"))
        assertTrue(src.contains("SECTION_SERVERS"))
        val secondary = src.substring(src.indexOf("private fun onSecondary()"))
        val autoArm = secondary.substringAfter("Step.AUTO").substringBefore("Step.WELCOME")
        assertFalse(
            "manual must not setAutoMode(false) before a picker commit",
            autoArm.contains("setAutoMode(false)"),
        )
        assertTrue(autoArm.contains("requestServerPick.launch"))
    }

    @Test
    fun autoCtaKeepsNativeOrbitAndSetAutoMode() {
        val src = source("HotfoxOnboardingActivity.kt")
        assertTrue(src.contains("showAutoArt()"))
        assertTrue(src.contains("autoOrbitView"))
        assertFalse("AUTO must not paint hf_auto_orbits", src.contains("hf_auto_orbits"))
        assertFalse("AUTO must not paint hf_globe_orange", src.contains("hf_globe_orange"))
        val primary = src.substring(src.indexOf("private fun onPrimary()"))
        val autoArm = primary.substringAfter("HotfoxOnboardingFlow.Step.AUTO")
            .substringBefore("HotfoxOnboardingFlow.Step.WELCOME")
        assertTrue(autoArm.contains("setAutoMode(true)"))
        assertTrue(autoArm.contains("KEEP_AUTO"))
    }

    @Test
    fun purchaseOpensExistingRenewal() {
        val src = source("HotfoxOnboardingActivity.kt")
        assertTrue(src.contains("RenewalActivity"))
        assertTrue(src.contains("requestPurchase.launch"))
    }

    private fun source(name: String): String {
        val candidates = listOf(
            File("src/main/java/com/v2ray/ang/ui/$name"),
            File("app/src/main/java/com/v2ray/ang/ui/$name"),
            File("../../bootstrap/hotfox_2_2_0/app/src/main/java/com/v2ray/ang/ui/$name"),
            File("../../../bootstrap/hotfox_2_2_0/app/src/main/java/com/v2ray/ang/ui/$name"),
        )
        val file = candidates.firstOrNull { it.isFile }
        assertTrue("missing $name", file != null)
        return file!!.readText()
    }
}
