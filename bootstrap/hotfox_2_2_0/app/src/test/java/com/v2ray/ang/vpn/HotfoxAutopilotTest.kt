package com.v2ray.ang.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HotfoxAutopilotTest {
    @Before
    fun reset() {
        HotfoxAutopilotStore.resetForTests()
        HotfoxAutopilotRuntime.resetForTests()
        VpnSessionCoordinator.resetForTests()
        VpnRestartGate.resetForTests()
    }

    @Test
    fun trustedHomeStaysOffWhileUnknownWifiConnects() {
        HotfoxAutopilotStore.markTrusted("cafe", HotfoxNetworkKind.TRUSTED_HOME)
        assertEquals(
            HotfoxNetworkKind.TRUSTED_HOME,
            HotfoxTrustedNetworks.classify(
                HotfoxTransportKind.WIFI,
                captive = false,
                opaqueNetworkId = "cafe",
                trusted = HotfoxAutopilotStore.trusted(),
            ),
        )
        val home = decide(network = HotfoxNetworkKind.TRUSTED_HOME)
        assertEquals(HotfoxConnectionIntent.KEEP_CURRENT, home.intent)
        assertFalse(HotfoxAutopilotApply.shouldDispatchStart(home, sessionProtected = false))
        val publicWifi = decide(network = HotfoxNetworkKind.UNKNOWN_WIFI)
        assertEquals(HotfoxConnectionIntent.CONNECT, publicWifi.intent)
        assertTrue(HotfoxAutopilotApply.shouldDispatchStart(publicWifi, sessionProtected = false))
    }

    @Test
    fun cellularPolicyConnectsAndTrustedOfficeDisconnectsActiveSession() {
        val cell = decide(network = HotfoxNetworkKind.CELLULAR)
        assertEquals(HotfoxConnectionIntent.CONNECT, cell.intent)
        HotfoxAutopilotStore.setPolicy(HotfoxAutopilotPolicy(connectCellular = false))
        val idle = decide(network = HotfoxNetworkKind.CELLULAR)
        assertEquals(HotfoxConnectionIntent.KEEP_CURRENT, idle.intent)
        val off = decide(network = HotfoxNetworkKind.TRUSTED_OFFICE, sessionProtected = true)
        assertEquals(HotfoxConnectionIntent.DISCONNECT_BY_POLICY, off.intent)
        assertTrue(HotfoxAutopilotApply.shouldDispatchStop(off, sessionProtected = true))
    }

    @Test
    fun pauseExpiryAndUntilNetworkChange() {
        HotfoxAutopilotStore.startPause(HotfoxPauseKind.MINUTES_5, nowEpochMs = 1_000L)
        val paused = decide(now = 2_000L, network = HotfoxNetworkKind.UNKNOWN_WIFI)
        assertEquals(HotfoxConnectionIntent.PAUSED, paused.intent)
        assertEquals("Сеть требует авторизации", HotfoxAutopilotLabels.label(HotfoxConnectionIntent.WAIT_FOR_CAPTIVE_PORTAL))
        val timed = HotfoxAutopilotStore.pause()
        assertEquals(1_000L + 5 * 60_000L, HotfoxAutopilotAlarms.expiryEpochMs(timed, 2_000L))
        assertNull(HotfoxAutopilotAlarms.expiryEpochMs(timed, 1_000L + 5 * 60_000L))
        val expired = decide(
            now = 1_000L + 5 * 60_000L + 1,
            network = HotfoxNetworkKind.UNKNOWN_WIFI,
            source = HotfoxAutopilotSource.PAUSE,
        )
        assertEquals(HotfoxConnectionIntent.CONNECT, expired.intent)
        HotfoxAutopilotStore.startPause(HotfoxPauseKind.UNTIL_NETWORK_CHANGE, nowEpochMs = 10L)
        val until = decide(now = 99_000L, network = HotfoxNetworkKind.UNKNOWN_WIFI)
        assertEquals(HotfoxConnectionIntent.PAUSED, until.intent)
        assertNull(HotfoxAutopilotAlarms.expiryEpochMs(HotfoxAutopilotStore.pause(), 99_000L))
        HotfoxAutopilotStore.noteNetworkChange()
        val afterNow = 1_000L + 5 * 60_000L + 1 + ConnectionIntentEngine.MIN_CONNECT_GAP_MS + 1
        val after = decide(
            now = afterNow,
            network = HotfoxNetworkKind.UNKNOWN_WIFI,
            source = HotfoxAutopilotSource.NETWORK,
        )
        assertEquals(HotfoxConnectionIntent.RECONNECT, after.intent)
    }

    @Test
    fun pauseStopsProtectedSessionAndIsNotPermanentDisable() {
        HotfoxAutopilotStore.startPause(HotfoxPauseKind.MINUTES_15, nowEpochMs = 5L)
        val stop = decide(now = 6L, network = HotfoxNetworkKind.CELLULAR, sessionProtected = true)
        assertEquals(HotfoxConnectionIntent.DISCONNECT_BY_POLICY, stop.intent)
        assertTrue(HotfoxAutopilotStore.policy().enabled)
    }

    @Test
    fun explicitUserDisconnectSuppressesUntilNetworkChange() {
        HotfoxAutopilotStore.noteUserDisconnect()
        val suppressed = decide(
            network = HotfoxNetworkKind.UNKNOWN_WIFI,
            source = HotfoxAutopilotSource.NETWORK,
        )
        assertEquals(HotfoxConnectionIntent.KEEP_CURRENT, suppressed.intent)
        HotfoxAutopilotStore.noteNetworkChange()
        val restored = decide(
            network = HotfoxNetworkKind.UNKNOWN_WIFI,
            source = HotfoxAutopilotSource.NETWORK,
        )
        assertEquals(HotfoxConnectionIntent.RECONNECT, restored.intent)
    }

    @Test
    fun networkLossWaitsAndRestoreReconnectsWithoutDuplicateWhileBusy() {
        val loss = decide(network = HotfoxNetworkKind.NONE)
        assertEquals(HotfoxConnectionIntent.WAIT_FOR_NETWORK, loss.intent)
        val busy = decide(network = HotfoxNetworkKind.CELLULAR, sessionBusy = true)
        assertEquals(HotfoxConnectionIntent.KEEP_CURRENT, busy.intent)
        val restore = decide(
            network = HotfoxNetworkKind.CELLULAR,
            source = HotfoxAutopilotSource.NETWORK,
        )
        assertEquals(HotfoxConnectionIntent.RECONNECT, restore.intent)
        val already = decide(
            network = HotfoxNetworkKind.CELLULAR,
            sessionProtected = true,
            source = HotfoxAutopilotSource.NETWORK,
        )
        assertEquals(HotfoxConnectionIntent.KEEP_CURRENT, already.intent)
        assertFalse(HotfoxAutopilotApply.shouldDispatchStart(already, sessionProtected = true))
    }

    @Test
    fun wifiCellularHandoffKeepsSerializedProtectedSession() {
        val handoff = decide(
            network = HotfoxNetworkKind.CELLULAR,
            sessionProtected = true,
            source = HotfoxAutopilotSource.NETWORK,
        )
        assertEquals(HotfoxConnectionIntent.KEEP_CURRENT, handoff.intent)
        assertEquals("handover_keep_serialized", handoff.reason)
    }

    @Test
    fun captivePortalWaitsAndReleasesVpnInsteadOfGenericFailure() {
        val wait = decide(network = HotfoxNetworkKind.CAPTIVE_PORTAL)
        assertEquals(HotfoxConnectionIntent.WAIT_FOR_CAPTIVE_PORTAL, wait.intent)
        assertEquals("Сеть требует авторизации", wait.uiLabel())
        val release = decide(network = HotfoxNetworkKind.CAPTIVE_PORTAL, sessionProtected = true)
        assertEquals(HotfoxConnectionIntent.DISCONNECT_BY_POLICY, release.intent)
        val after = decide(network = HotfoxNetworkKind.UNKNOWN_WIFI)
        assertEquals(HotfoxConnectionIntent.CONNECT, after.intent)
    }

    @Test
    fun processStartDoesNotResurrectStaleSessionAndHonorsBootPolicy() {
        val boot = decide(
            network = HotfoxNetworkKind.UNKNOWN_WIFI,
            source = HotfoxAutopilotSource.BOOT,
        )
        assertEquals(HotfoxConnectionIntent.CONNECT, boot.intent)
        HotfoxAutopilotStore.setPolicy(HotfoxAutopilotPolicy(connectAfterBoot = false))
        val skipped = decide(
            network = HotfoxNetworkKind.UNKNOWN_WIFI,
            source = HotfoxAutopilotSource.PROCESS_START,
        )
        assertEquals(HotfoxConnectionIntent.KEEP_CURRENT, skipped.intent)
        assertFalse(VpnSessionCoordinator.currentState().isProtected())
    }

    @Test
    fun permissionAndEntitlementBlockConnect() {
        val perm = decide(network = HotfoxNetworkKind.UNKNOWN_WIFI, vpnPermissionGranted = false)
        assertEquals(HotfoxConnectionIntent.BLOCKED_PERMISSION, perm.intent)
        val entitlement = decide(
            network = HotfoxNetworkKind.UNKNOWN_WIFI,
            entitlementUsable = false,
            hasUsableTarget = false,
            autoMode = true,
        )
        assertEquals(HotfoxConnectionIntent.BLOCKED_ENTITLEMENT, entitlement.intent)
    }

    @Test
    fun manualSelectionIsPreservedAndAutoStillConnects() {
        val manual = decide(network = HotfoxNetworkKind.UNKNOWN_WIFI, autoMode = false)
        assertTrue(manual.preserveManualSelection)
        assertEquals(HotfoxConnectionIntent.CONNECT, manual.intent)
        val auto = decide(
            now = 1_000L + ConnectionIntentEngine.MIN_CONNECT_GAP_MS + 1,
            network = HotfoxNetworkKind.UNKNOWN_WIFI,
            autoMode = true,
        )
        assertFalse(auto.preserveManualSelection)
        assertEquals(HotfoxConnectionIntent.CONNECT, auto.intent)
    }

    @Test
    fun staleEventCannotOverrideNewerIntentAndReconnectGapPreventsStorm() {
        val firstGen = HotfoxAutopilotStore.nextEvent()
        val newerGen = HotfoxAutopilotStore.nextEvent()
        val staleSnap = HotfoxAutopilotStore.snapshot(
            network = HotfoxNetworkKind.CELLULAR,
            sessionProtected = false,
            sessionBusy = false,
            vpnPermissionGranted = true,
            entitlementUsable = true,
            hasUsableTarget = true,
            autoMode = true,
            nowEpochMs = 50L,
            source = HotfoxAutopilotSource.NETWORK,
            eventGeneration = firstGen,
        )
        assertNull(HotfoxAutopilotStore.consider(staleSnap))
        val live = decide(now = 50L, network = HotfoxNetworkKind.UNKNOWN_WIFI)
        assertEquals(HotfoxConnectionIntent.CONNECT, live.intent)
        assertEquals(newerGen + 1, live.eventGeneration)
        val storm = decide(now = 50L + 1_000L, network = HotfoxNetworkKind.UNKNOWN_WIFI)
        assertEquals(HotfoxConnectionIntent.KEEP_CURRENT, storm.intent)
        assertEquals("reconnect_gap", storm.reason)
        val later = decide(now = 50L + ConnectionIntentEngine.MIN_CONNECT_GAP_MS + 1, network = HotfoxNetworkKind.UNKNOWN_WIFI)
        assertEquals(HotfoxConnectionIntent.CONNECT, later.intent)
    }

    @Test
    fun protectionProfilesMapToRealRoutingBehavior() {
        val publicMax = HotfoxProtectionProfiles.defaults(
            HotfoxProtectionLevel.MAX_PROTECTION,
            HotfoxNetworkKind.UNKNOWN_WIFI,
        )
        assertEquals(HotfoxRoutingMode.GLOBAL, publicMax.routingMode)
        assertFalse(publicMax.lanAccess)
        assertTrue(publicMax.adsBlocked)
        assertTrue(publicMax.shadowAuto)
        val homeSpeed = HotfoxProtectionProfiles.defaults(
            HotfoxProtectionLevel.SPEED,
            HotfoxNetworkKind.TRUSTED_HOME,
        )
        assertTrue(homeSpeed.lanAccess)
        assertFalse(homeSpeed.adsBlocked)
        val cell = HotfoxProtectionProfiles.defaults(
            HotfoxProtectionLevel.BALANCE,
            HotfoxNetworkKind.CELLULAR,
        )
        assertTrue(cell.preferEfficientPath)
        assertEquals("Баланс", HotfoxAutopilotLabels.protectionLabel(HotfoxProtectionLevel.BALANCE))
        assertNotEquals(HotfoxTrustedNetworks.opaqueId("HomeWifi"), "HomeWifi")
        assertTrue(HotfoxNetworkIdentity.usableSsid("HomeWifi"))
        assertFalse(HotfoxNetworkIdentity.usableSsid("<unknown ssid>"))
        assertFalse(HotfoxNetworkIdentity.usableSsid("0x"))
    }

    @Test
    fun processRecreationRestoresPolicyPauseAndTrustedWithoutStaleGeneration() {
        HotfoxAutopilotStore.setPolicy(HotfoxAutopilotPolicy(connectCellular = false))
        HotfoxAutopilotStore.setProtectionLevel(HotfoxProtectionLevel.MAX_PROTECTION)
        HotfoxAutopilotStore.markTrusted("cafe", HotfoxNetworkKind.TRUSTED_HOME)
        HotfoxAutopilotStore.startPause(HotfoxPauseKind.MINUTES_15, nowEpochMs = 10L)
        HotfoxAutopilotStore.noteUserDisconnect()
        val blob = HotfoxAutopilotCodec.encode(HotfoxAutopilotStore.export())
        HotfoxAutopilotStore.resetForTests()
        HotfoxAutopilotStore.restore(HotfoxAutopilotCodec.decode(blob), preserveGenerations = false)
        assertFalse(HotfoxAutopilotStore.policy().connectCellular)
        assertEquals(HotfoxProtectionLevel.MAX_PROTECTION, HotfoxAutopilotStore.protectionLevel())
        assertEquals(HotfoxNetworkKind.TRUSTED_HOME, HotfoxAutopilotStore.trusted()["cafe"])
        assertTrue(HotfoxAutopilotStore.suppressAuto())
        assertEquals(0L, HotfoxAutopilotStore.liveGeneration())
        val paused = decide(now = 20L, network = HotfoxNetworkKind.UNKNOWN_WIFI)
        assertEquals(HotfoxConnectionIntent.PAUSED, paused.intent)
    }

    @Test
    fun codecRejectsUnknownTrustedKindsAndKeepsUntilNetworkPauseAcrossRestore() {
        val trusted = HotfoxAutopilotCodec.decodeTrusted("ok=TRUSTED_HOME;bad=UNKNOWN_WIFI;x=TRUSTED_OFFICE")
        assertEquals(HotfoxNetworkKind.TRUSTED_HOME, trusted["ok"])
        assertEquals(HotfoxNetworkKind.TRUSTED_OFFICE, trusted["x"])
        assertFalse(trusted.containsKey("bad"))
        val pause = HotfoxAutopilotCodec.decodePause("UNTIL_NETWORK_CHANGE|1|9")
        assertEquals(0L, pause?.networkGenerationAtStart)
        assertTrue(pause!!.active(99_000L, networkGeneration = 0L))
        assertFalse(pause.active(99_000L, networkGeneration = 1L))
        assertFalse(HotfoxAutopilotApply.shouldApplyProfile(HotfoxRoutingMode.CUSTOM))
        assertFalse(HotfoxAutopilotApply.shouldApplyProfile(HotfoxRoutingMode.INCLUDE_APPS))
        assertTrue(HotfoxAutopilotApply.shouldApplyProfile(HotfoxRoutingMode.SMART))
        HotfoxAutopilotStore.setProtectionLevel(HotfoxProtectionLevel.MAX_PROTECTION)
        val max = decide(network = HotfoxNetworkKind.UNKNOWN_WIFI)
        assertEquals(HotfoxRoutingMode.GLOBAL, max.profile.routingMode)
    }

    @Test
    fun userConnectClearsPauseAndDoesNotRewriteSessionCoordinator() {
        HotfoxAutopilotStore.startPause(HotfoxPauseKind.HOUR_1, nowEpochMs = 1L)
        HotfoxAutopilotStore.noteUserConnect()
        val before = VpnSessionCoordinator.currentState()
        val connect = decide(
            network = HotfoxNetworkKind.UNKNOWN_WIFI,
            source = HotfoxAutopilotSource.USER_CONNECT,
        )
        assertEquals(HotfoxConnectionIntent.CONNECT, connect.intent)
        assertEquals(before, VpnSessionCoordinator.currentState())
    }

    private fun decide(
        network: HotfoxNetworkKind,
        sessionProtected: Boolean = false,
        sessionBusy: Boolean = false,
        vpnPermissionGranted: Boolean = true,
        entitlementUsable: Boolean = true,
        hasUsableTarget: Boolean = true,
        autoMode: Boolean = true,
        now: Long = 1_000L,
        // PROCESS_START is the first apply after policy/state change.
        // NETWORK is reconnect-after-restore (Runtime always uses it after noteNetworkChange).
        source: HotfoxAutopilotSource = HotfoxAutopilotSource.PROCESS_START,
    ): HotfoxIntentDecision {
        val snap = HotfoxAutopilotStore.snapshot(
            network = network,
            sessionProtected = sessionProtected,
            sessionBusy = sessionBusy,
            vpnPermissionGranted = vpnPermissionGranted,
            entitlementUsable = entitlementUsable,
            hasUsableTarget = hasUsableTarget,
            autoMode = autoMode,
            nowEpochMs = now,
            source = source,
        )
        return requireNotNull(HotfoxAutopilotStore.consider(snap))
    }
}
