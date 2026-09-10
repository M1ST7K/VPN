package com.v2ray.ang.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VpnAdmissionGateTest {
    @Before
    fun reset() {
        VpnAdmissionGate.resetForTests()
    }

    @Test
    fun rejectedStaleTeardownDoesNotInvalidateLiveAdmission() {
        val live = VpnAdmissionGate.snapshot()
        assertTrue(VpnAdmissionGate.isCurrent(live))
        assertFalse(VpnAdmissionGate.invalidateAfterClaim(claimed = false))
        assertEquals(live, VpnAdmissionGate.snapshot())
        assertTrue(VpnAdmissionGate.isCurrent(live))
    }

    @Test
    fun successfulTeardownClaimInvalidatesPriorAdmission() {
        val live = VpnAdmissionGate.snapshot()
        assertTrue(VpnAdmissionGate.invalidateAfterClaim(claimed = true))
        assertFalse(VpnAdmissionGate.isCurrent(live))
        assertTrue(VpnAdmissionGate.isCurrent(VpnAdmissionGate.snapshot()))
    }

    @Test
    fun establishedTunIsAbandonedWhenAdmissionBecomesStaleOrCancelled() {
        assertFalse(
            VpnAdmissionGate.shouldAbandonEstablished(
                admission = 3L,
                currentEpoch = 3L,
                pipelineCurrent = true,
                cancelled = false,
            ),
        )
        assertTrue(
            VpnAdmissionGate.shouldAbandonEstablished(
                admission = 3L,
                currentEpoch = 4L,
                pipelineCurrent = true,
                cancelled = false,
            ),
        )
        assertTrue(
            VpnAdmissionGate.shouldAbandonEstablished(
                admission = 3L,
                currentEpoch = 3L,
                pipelineCurrent = false,
                cancelled = false,
            ),
        )
        assertTrue(
            VpnAdmissionGate.shouldAbandonEstablished(
                admission = 3L,
                currentEpoch = 3L,
                pipelineCurrent = true,
                cancelled = true,
            ),
        )
    }
}
