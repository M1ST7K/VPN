package com.v2ray.ang.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HotfoxAutoFailoverTest {
    @Before
    fun reset() {
        HotfoxAutoFailover.reset()
        HotfoxServerSelection.health.resetForTests()
    }

    @Test
    fun tunBindFailuresDoNotSwitchServer() {
        val servers = listOf(
            HotfoxServerSelection.Candidate("a", "A", -1L),
            HotfoxServerSelection.Candidate("b", "B", 20L),
        )
        val tun = HotfoxAutoFailover.consider("HF-VPN-002", auto = true, failedGuid = "a", servers = servers)
        assertEquals(FailoverAction.STOP, tun.action)
        assertEquals("not_server_failure", tun.reason)
        val bind = HotfoxAutoFailover.consider("HF-VPN-012", auto = true, failedGuid = "a", servers = servers)
        assertEquals(FailoverAction.STOP, bind.action)
        assertEquals(0, HotfoxAutoFailover.attempts)
    }

    @Test
    fun autoSwitchesAwayFromDeadTarget() {
        val servers = listOf(
            HotfoxServerSelection.Candidate("a", "A", 40L),
            HotfoxServerSelection.Candidate("b", "B", 28L),
        )
        val decision = HotfoxAutoFailover.consider(
            code = "HF-VPN-006",
            auto = true,
            failedGuid = "a",
            servers = servers,
        )
        assertEquals(FailoverAction.SWITCH, decision.action)
        assertEquals("b", decision.guid)
        assertEquals(1, decision.attempt)
        assertTrue(decision.reason.contains("failover_previous_unreachable"))
        assertEquals(1, HotfoxAutoFailover.attempts)
    }

    @Test
    fun manualSelectionNeverFailsOver() {
        val servers = listOf(
            HotfoxServerSelection.Candidate("slow", "Slow", 120L),
            HotfoxServerSelection.Candidate("fast", "Fast", 12L),
        )
        val decision = HotfoxAutoFailover.consider(
            code = "HF-VPN-006",
            auto = false,
            failedGuid = "slow",
            servers = servers,
        )
        assertEquals(FailoverAction.STOP, decision.action)
        assertEquals("manual_sticky", decision.reason)
        assertEquals("slow", decision.guid)
        assertEquals(0, HotfoxAutoFailover.attempts)
    }

    @Test
    fun attemptCapStopsAutoFailover() {
        val servers = listOf(
            HotfoxServerSelection.Candidate("a", "A", 40L),
            HotfoxServerSelection.Candidate("b", "B", 20L),
        )
        repeat(AutoSelectionPolicy.FAILOVER_ATTEMPT_CAP) {
            val step = HotfoxAutoFailover.consider("HF-VPN-004", auto = true, failedGuid = "a", servers = servers)
            assertEquals(FailoverAction.SWITCH, step.action)
        }
        val capped = HotfoxAutoFailover.consider("HF-VPN-006", auto = true, failedGuid = "b", servers = servers)
        assertEquals(FailoverAction.STOP, capped.action)
        assertEquals("attempt_cap", capped.reason)
    }

    @Test
    fun allUnhealthyStopsWithActionableReason() {
        val servers = listOf(
            HotfoxServerSelection.Candidate("a", "A", -1L),
            HotfoxServerSelection.Candidate("b", "B", -1L),
        )
        val decision = HotfoxAutoFailover.consider("HF-VPN-006", auto = true, failedGuid = "a", servers = servers)
        assertEquals(FailoverAction.STOP, decision.action)
        assertEquals("all_unhealthy", decision.reason)
    }

    @Test
    fun stableConnectResetsAttemptWindow() {
        val servers = listOf(
            HotfoxServerSelection.Candidate("a", "A", -1L),
            HotfoxServerSelection.Candidate("b", "B", 20L),
        )
        HotfoxAutoFailover.consider("HF-VPN-003", auto = true, failedGuid = "a", servers = servers)
        assertEquals(1, HotfoxAutoFailover.attempts)
        HotfoxAutoFailover.reset()
        assertEquals(0, HotfoxAutoFailover.attempts)
        assertFalse(HotfoxAutoFailover.isServerTargetFailure("HF-VPN-013"))
        assertTrue(HotfoxAutoFailover.isServerTargetFailure("HF-VPN-003"))
    }
}
