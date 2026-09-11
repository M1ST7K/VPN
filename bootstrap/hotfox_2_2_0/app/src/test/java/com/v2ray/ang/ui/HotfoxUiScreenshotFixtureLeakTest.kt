package com.v2ray.ang.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HotfoxUiScreenshotFixtureLeakTest {
    @Test
    fun visualOverrideStartsEmpty() {
        HotfoxUiVisualOverride.clear()
        assertNull(HotfoxUiVisualOverride.scenarioId)
        assertFalse(HotfoxUiVisualOverride.holdSplash)
        assertNull(HotfoxUiVisualOverride.onboardingStep)
        assertNull(HotfoxUiVisualOverride.connectionChrome)
        assertFalse(HotfoxUiVisualOverride.showAddSheet)
        assertFalse(HotfoxUiVisualOverride.serverDetailsFixture)
        assertFalse(HotfoxUiVisualOverride.appsFixture)
        assertFalse(HotfoxUiVisualOverride.serversFixture)
        assertFalse(HotfoxUiVisualOverride.subscriptionFixture)
        assertTrue(HotfoxUiVisualOverride.referenceServers.isEmpty())
        assertTrue(HotfoxUiVisualOverride.referenceApps.isEmpty())
    }

    @Test
    fun releaseMainSourceDoesNotReferenceScreenshotHarness() {
        val overlay = File("src/main")
        val roots = listOf(
            overlay,
            File("../src/main"),
            File("app/src/main"),
        )
        val main = roots.firstOrNull { it.isDirectory } ?: overlay
        if (!main.isDirectory) return
        val hits = main.walkTopDown()
            .filter { it.isFile && (it.extension == "kt" || it.extension == "java" || it.extension == "xml") }
            .flatMap { file ->
                file.readText().lineSequence().mapIndexedNotNull { index, line ->
                    if (line.contains("HotfoxUiScreenshotHarness") || line.contains("HotfoxUiScreenshotScenario")) {
                        "${file.path}:${index + 1}:$line"
                    } else {
                        null
                    }
                }
            }
            .toList()
        assertFalse(hits.joinToString("\n"), hits.isNotEmpty())
    }
}
