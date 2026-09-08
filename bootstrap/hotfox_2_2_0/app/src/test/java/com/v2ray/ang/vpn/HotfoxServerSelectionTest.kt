package com.v2ray.ang.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HotfoxServerSelectionTest {
    @Test
    fun firstImportSelectsFirstKeyWhenNothingWasSelected() {
        val keys = listOf("amsterdam", "frankfurt")
        assertEquals("amsterdam", HotfoxServerSelection.matchImportedKey(keys, null))
    }

    @Test
    fun refreshPreservesPreviousSelectionWhenStillPresent() {
        val keys = listOf("frankfurt", "amsterdam")
        assertEquals("amsterdam", HotfoxServerSelection.matchImportedKey(keys, "amsterdam"))
    }

    @Test
    fun deletedSelectionFallsBackToFirstImported() {
        val keys = listOf("paris")
        assertEquals("paris", HotfoxServerSelection.matchImportedKey(keys, "gone"))
    }

    @Test
    fun autoPicksLowestPositiveDelay() {
        val servers = listOf(
            HotfoxServerSelection.Candidate("a", "A", 90L),
            HotfoxServerSelection.Candidate("b", "B", 28L),
            HotfoxServerSelection.Candidate("c", "C", -1L),
        )
        val result = HotfoxServerSelection.pick(servers, auto = true, selectedGuid = "a")
        assertEquals(HotfoxServerSelection.ResolveResult.Success("b", true), result)
    }

    @Test
    fun autoSkipsUnhealthyAndUsesUntestedIfNeeded() {
        val servers = listOf(
            HotfoxServerSelection.Candidate("dead", "Dead", -1L),
            HotfoxServerSelection.Candidate("fresh", "Fresh", 0L),
        )
        val result = HotfoxServerSelection.pick(servers, auto = true, selectedGuid = null)
        assertEquals(HotfoxServerSelection.ResolveResult.Success("fresh", true), result)
    }

    @Test
    fun autoFailsWhenEveryServerIsDead() {
        val servers = listOf(
            HotfoxServerSelection.Candidate("a", "A", -1L),
            HotfoxServerSelection.Candidate("b", "B", -1L),
        )
        val result = HotfoxServerSelection.pick(servers, auto = true, selectedGuid = "a")
        assertTrue(result is HotfoxServerSelection.ResolveResult.Failure)
        assertTrue((result as HotfoxServerSelection.ResolveResult.Failure).message.contains("HF-VPN-011"))
    }

    @Test
    fun manualSelectionIsNotReplacedByHealthierServer() {
        val servers = listOf(
            HotfoxServerSelection.Candidate("slow", "Slow", 120L),
            HotfoxServerSelection.Candidate("fast", "Fast", 12L),
        )
        val result = HotfoxServerSelection.pick(servers, auto = false, selectedGuid = "slow")
        assertEquals(HotfoxServerSelection.ResolveResult.Success("slow", false), result)
    }

    @Test
    fun emptyInventoryFailsClosed() {
        val result = HotfoxServerSelection.pick(emptyList(), auto = true, selectedGuid = null)
        assertTrue(result is HotfoxServerSelection.ResolveResult.Failure)
        assertTrue((result as HotfoxServerSelection.ResolveResult.Failure).message.contains("HF-VPN-010"))
    }
}
