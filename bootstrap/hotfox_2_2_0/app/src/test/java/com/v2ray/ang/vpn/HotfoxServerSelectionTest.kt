package com.v2ray.ang.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun autoAppearsAsFirstRowAboveRealServers() {
        assertEquals(0, HotfoxServerListContract.AUTO_ROW_INDEX)
        assertEquals(HotfoxServerListContract.VIEW_TYPE_AUTO, HotfoxServerListContract.viewType(0, 4))
        assertEquals(HotfoxServerListContract.VIEW_TYPE_ITEM, HotfoxServerListContract.viewType(1, 4))
        assertEquals(HotfoxServerListContract.VIEW_TYPE_ITEM, HotfoxServerListContract.viewType(4, 4))
        assertEquals(HotfoxServerListContract.VIEW_TYPE_FOOTER, HotfoxServerListContract.viewType(5, 4))
        assertEquals(6, HotfoxServerListContract.itemCount(4))
    }

    @Test
    fun autoModeScrollsToFirstRowEvenWhenAServerIsResolved() {
        assertEquals(
            HotfoxServerListContract.AUTO_ROW_INDEX,
            HotfoxServerListContract.adapterPositionForSelection(auto = true, dataIndex = 4),
        )
        assertEquals(
            HotfoxServerListContract.AUTO_ROW_INDEX,
            HotfoxServerListContract.adapterPositionForSelection(auto = true, dataIndex = -1),
        )
    }

    @Test
    fun manualServerScrollAccountsForAutoRowOffset() {
        assertEquals(1, HotfoxServerListContract.adapterPositionForSelection(auto = false, dataIndex = 0))
        assertEquals(5, HotfoxServerListContract.adapterPositionForSelection(auto = false, dataIndex = 4))
        assertEquals(-1, HotfoxServerListContract.adapterPositionForSelection(auto = false, dataIndex = -1))
    }

    @Test
    fun autoPersistenceIsDistinctFromManualGuid() {
        val auto = HotfoxServerSelection.persistAfterTap(
            tapGuid = HotfoxServerSelection.AUTO_GUID,
            previousGuid = "amsterdam",
            firstUsableGuid = "frankfurt",
        )
        assertTrue(auto.auto)
        assertEquals("amsterdam", auto.selectedGuid)

        val manual = HotfoxServerSelection.persistAfterTap(
            tapGuid = "frankfurt",
            previousGuid = "amsterdam",
            firstUsableGuid = "amsterdam",
        )
        assertFalse(manual.auto)
        assertEquals("frankfurt", manual.selectedGuid)
    }

    @Test
    fun ensureValidDoesNotClearAutoModeOrReplaceAValidManualServer() {
        val autoKept = HotfoxServerSelection.persistAfterEnsureValid(
            auto = true,
            selectedGuid = "amsterdam",
            inventory = listOf("amsterdam", "frankfurt"),
        )
        assertTrue(autoKept.auto)
        assertEquals("amsterdam", autoKept.selectedGuid)

        val manualKept = HotfoxServerSelection.persistAfterEnsureValid(
            auto = false,
            selectedGuid = "slow",
            inventory = listOf("fast", "slow"),
        )
        assertFalse(manualKept.auto)
        assertEquals("slow", manualKept.selectedGuid)
    }

    @Test
    fun tappingResolvedServerLeavesAutoMode() {
        assertFalse(
            HotfoxServerSelection.alreadySelected(
                tapGuid = "amsterdam",
                selectedGuid = "amsterdam",
                auto = true,
            )
        )
        assertTrue(
            HotfoxServerSelection.alreadySelected(
                tapGuid = HotfoxServerSelection.AUTO_GUID,
                selectedGuid = "amsterdam",
                auto = true,
            )
        )
        assertTrue(
            HotfoxServerSelection.alreadySelected(
                tapGuid = "amsterdam",
                selectedGuid = "amsterdam",
                auto = false,
            )
        )
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
    fun handoverKeepsHealthyAutoTarget() {
        val servers = listOf(
            HotfoxServerSelection.Candidate("amsterdam", "Amsterdam", 80L),
            HotfoxServerSelection.Candidate("frankfurt", "Frankfurt", 70L),
        )
        val result = HotfoxServerSelection.resolveForHandover(servers, auto = true, selectedGuid = "amsterdam")
        assertEquals(HotfoxServerSelection.ResolveResult.Success("amsterdam", true), result)
    }

    @Test
    fun handoverSwitchesWhenChallengerIsSignificantlyBetter() {
        val servers = listOf(
            HotfoxServerSelection.Candidate("amsterdam", "Amsterdam", 80L),
            HotfoxServerSelection.Candidate("frankfurt", "Frankfurt", 20L),
        )
        val result = HotfoxServerSelection.resolveForHandover(servers, auto = true, selectedGuid = "amsterdam")
        assertEquals(HotfoxServerSelection.ResolveResult.Success("frankfurt", true), result)
    }

    @Test
    fun handoverReplacesUnhealthyAutoTarget() {
        val servers = listOf(
            HotfoxServerSelection.Candidate("amsterdam", "Amsterdam", -1L),
            HotfoxServerSelection.Candidate("frankfurt", "Frankfurt", 20L),
        )
        val result = HotfoxServerSelection.resolveForHandover(servers, auto = true, selectedGuid = "amsterdam")
        assertEquals(HotfoxServerSelection.ResolveResult.Success("frankfurt", true), result)
    }

    @Test
    fun handoverDoesNotOverrideManualSelection() {
        val servers = listOf(
            HotfoxServerSelection.Candidate("slow", "Slow", 120L),
            HotfoxServerSelection.Candidate("fast", "Fast", 12L),
        )
        val result = HotfoxServerSelection.resolveForHandover(servers, auto = false, selectedGuid = "slow")
        assertEquals(HotfoxServerSelection.ResolveResult.Success("slow", false), result)
    }

    @Test
    fun reconnectCanResolveANewServer() {
        val servers = listOf(
            HotfoxServerSelection.Candidate("old", "Old", -1L),
            HotfoxServerSelection.Candidate("amsterdam", "Amsterdam", 40L),
        )
        val result = HotfoxServerSelection.pick(servers, auto = true, selectedGuid = "old")
        assertEquals(HotfoxServerSelection.ResolveResult.Success("amsterdam", true), result)
    }

    @Test
    fun emptyInventoryFailsClosed() {
        val result = HotfoxServerSelection.pick(emptyList(), auto = true, selectedGuid = null)
        assertTrue(result is HotfoxServerSelection.ResolveResult.Failure)
        assertTrue((result as HotfoxServerSelection.ResolveResult.Failure).message.contains("HF-VPN-010"))
    }

    @Test
    fun lateAutoResultCannotOverwriteManualSelection() {
        val request = HotfoxServerSelection.captureAutoRequest(auto = true, generation = 3L)
        assertFalse(
            HotfoxServerSelection.shouldCommitAutoResult(
                request = request,
                currentAuto = false,
                currentGeneration = 4L,
            ),
        )
        val late = HotfoxServerSelection.resolveLateAutoAgainstManual(
            request = request,
            currentAuto = false,
            currentGeneration = 4L,
            currentSelectedGuid = "manual-y",
            autoResultGuid = "auto-x",
        )
        assertTrue(late is HotfoxServerSelection.ResolveResult.Success)
        val success = late as HotfoxServerSelection.ResolveResult.Success
        assertEquals("manual-y", success.guid)
        assertFalse(success.resolvedFromAuto)
    }

    @Test
    fun matchingAutoGenerationStillCommits() {
        val request = HotfoxServerSelection.captureAutoRequest(auto = true, generation = 7L)
        assertTrue(
            HotfoxServerSelection.shouldCommitAutoResult(
                request = request,
                currentAuto = true,
                currentGeneration = 7L,
            ),
        )
        val ok = HotfoxServerSelection.resolveLateAutoAgainstManual(
            request = request,
            currentAuto = true,
            currentGeneration = 7L,
            currentSelectedGuid = "sticky",
            autoResultGuid = "auto-x",
        )
        assertEquals(HotfoxServerSelection.ResolveResult.Success("auto-x", true), ok)
    }
}
