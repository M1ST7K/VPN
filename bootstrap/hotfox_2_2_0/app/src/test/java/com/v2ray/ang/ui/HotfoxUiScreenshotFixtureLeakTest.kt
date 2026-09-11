package com.v2ray.ang.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HotfoxUiScreenshotFixtureLeakTest {
    private val forbidden = listOf(
        "HotfoxUiVisualOverride",
        "ReferenceServerRow",
        "ReferenceAppRow",
        "serverDetailsFixture",
        "serversFixture",
        "subscriptionFixture",
        "appsFixture",
        "referenceServers",
        "referenceApps",
        "renderFixture(",
        "applySubscriptionVisualFixture",
        "debug-ui-fixture-server",
        "HotfoxUiScreenshotHarness",
        "HotfoxUiScreenshotScenario",
        "HotfoxUiQaPainter",
        "HotfoxUiQaInstaller",
        "HotfoxUiQaServerDetails",
        "HotfoxUiReferenceFixtures",
        "bindFixtureRow",
        "applyDebugConnectionChromeIfPresent",
        "applyDebugPresentationIfPresent",
        "\"18 ms\"",
        "\"12%\"",
        "\"31.12.2026\"",
    )

    @Test
    fun releaseMainSourceDoesNotContainFixtureSpecificLogic() {
        val main = mainSourceRoot()
        assertTrue("production src/main was not found for leak scan", main.isDirectory)
        val hits = mutableListOf<String>()
        main.walkTopDown()
            .filter { it.isFile && it.extension in setOf("kt", "java", "xml") }
            .forEach { file ->
                file.readLines().forEachIndexed { index, line ->
                    forbidden.forEach { needle ->
                        if (line.contains(needle)) {
                            hits += "${file.path}:${index + 1}: $needle: ${line.trim()}"
                        }
                    }
                }
            }
        assertFalse(
            "fixture-specific logic leaked into src/main:\n${hits.joinToString("\n")}",
            hits.isNotEmpty(),
        )
    }

    private fun mainSourceRoot(): File {
        val candidates = listOf(
            File("src/main"),
            File("app/src/main"),
            File("../src/main"),
            File("../../bootstrap/hotfox_2_2_0/app/src/main"),
            File("../../../bootstrap/hotfox_2_2_0/app/src/main"),
        )
        return candidates.firstOrNull { it.isDirectory } ?: File("src/main")
    }
}
