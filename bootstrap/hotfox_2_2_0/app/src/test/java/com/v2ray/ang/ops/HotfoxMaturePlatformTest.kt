package com.v2ray.ang.ops

import com.v2ray.ang.commerce.EntitlementStatus
import com.v2ray.ang.vpn.AutoCandidateFilter
import com.v2ray.ang.vpn.AutoFilterReason
import com.v2ray.ang.vpn.AutoSelectionPolicy
import com.v2ray.ang.vpn.HotfoxEngineFacade
import com.v2ray.ang.vpn.HotfoxServerSelection
import com.v2ray.ang.vpn.VpnSessionCoordinator
import com.v2ray.ang.vpn.VpnSessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec

class HotfoxMaturePlatformTest {
    @Before
    fun reset() {
        HotfoxControlPlane.resetForTests()
        HotfoxNodeDrain.resetForTests()
        HotfoxRemoteFlags.resetForTests()
        HotfoxDeviceRegistry.resetForTests()
        HotfoxPrivacyTelemetry.resetForTests()
        VpnSessionCoordinator.resetForTests()
    }

    @Test
    fun engineFacadeReobservesProcessScopedStateAfterUiDeath() {
        val attempt = VpnSessionCoordinator.beginAttempt()
        assertTrue(VpnSessionCoordinator.markConnected(attempt, pathVerified = true))
        val first = HotfoxEngineFacade.snapshot()
        assertEquals(VpnSessionState.CONNECTED, first.state)
        assertTrue(first.isProtected())
        val afterRecreation = HotfoxEngineFacade.snapshot()
        assertEquals(first.state, afterRecreation.state)
        assertEquals(first.attempt, afterRecreation.attempt)
        assertFalse(HotfoxEngineFacade::class.java.methods.any { it.name.contains("markConnected") })
        assertFalse(HotfoxEngineFacade::class.java.methods.any { it.name.contains("beginAttempt") })
    }

    @Test
    fun signedControlPlaneKeepsLastGoodOnInvalidSignatureAndOutage() {
        val keys = ecdsa()
        val now = 1_000L
        val first = controlJson(
            nodes = listOf(node("ams", load = HotfoxLoadBucket.LOW, maintenance = true)),
            now = now,
            keys = keys,
        )
        val applied = HotfoxControlPlane.apply(first, now, keys.publicHex)
        requireNotNull(applied)
        assertEquals(setOf("ams"), applied.maintenanceGuids())
        val unsigned = first.replace(Regex("\"signature\":\"[^\"]+\""), "\"signature\":\"00\"")
        assertEquals(setOf("ams"), HotfoxControlPlane.apply(unsigned, now, keys.publicHex)?.maintenanceGuids())
        assertEquals(setOf("ams"), HotfoxControlPlane.apply(null, now, keys.publicHex)?.maintenanceGuids())
        assertTrue(HotfoxOfflinePolicy.preserveServersOnControlPlaneOutage())
        assertFalse(HotfoxOfflinePolicy.wipeServersOnUnsignedControlPlane())
    }

    @Test
    fun capacityAwareAutoDoesNotSendEveryoneToOverloadedLowLatencyNode() {
        val keys = ecdsa()
        val now = 2_000L
        HotfoxControlPlane.apply(
            controlJson(
                nodes = listOf(
                    node("fast-busy", load = HotfoxLoadBucket.HIGH, weight = 1),
                    node("slower-idle", load = HotfoxLoadBucket.LOW, weight = 1),
                ),
                now = now,
                keys = keys,
            ),
            now,
            keys.publicHex,
        )
        val servers = listOf(
            HotfoxServerSelection.Candidate("fast-busy", "Busy", 20L),
            HotfoxServerSelection.Candidate("slower-idle", "Idle", 80L),
        )
        val picked = AutoSelectionPolicy.pick(servers, auto = true, selectedGuid = null, nowEpochMs = now)
        assertEquals(
            HotfoxServerSelection.ResolveResult.Success("slower-idle", true),
            picked,
        )
    }

    @Test
    fun maintenanceIsExcludedFromAutoButManualStaysSticky() {
        val keys = ecdsa()
        val now = 3_000L
        HotfoxControlPlane.apply(
            controlJson(
                nodes = listOf(
                    node("keep-out", maintenance = true),
                    node("ok", load = HotfoxLoadBucket.MEDIUM),
                ),
                now = now,
                keys = keys,
            ),
            now,
            keys.publicHex,
        )
        val servers = listOf(
            HotfoxServerSelection.Candidate("keep-out", "A", 10L),
            HotfoxServerSelection.Candidate("ok", "B", 40L),
        )
        assertEquals(AutoFilterReason.MAINTENANCE, AutoCandidateFilter.reasonOf(servers[0]))
        val auto = AutoSelectionPolicy.pick(servers, auto = true, selectedGuid = "keep-out", nowEpochMs = now)
        assertEquals(HotfoxServerSelection.ResolveResult.Success("ok", true), auto)
        val manual = AutoSelectionPolicy.pick(servers, auto = false, selectedGuid = "keep-out", nowEpochMs = now)
        assertEquals(HotfoxServerSelection.ResolveResult.Success("keep-out", false), manual)
        val failover = AutoSelectionPolicy.failover(
            servers,
            healthByGuid = emptyMap(),
            auto = false,
            selectedGuid = "keep-out",
            attempt = 0,
            nowEpochMs = now,
        )
        assertEquals("manual_sticky", failover.reason)
    }

    @Test
    fun fleetRestoreRemovesMaintenanceWhenSignedMetadataSaysSo() {
        val keys = ecdsa()
        val now = 4_000L
        HotfoxControlPlane.apply(
            controlJson(listOf(node("n1", maintenance = true)), now, keys, policyVersion = 1),
            now,
            keys.publicHex,
        )
        assertEquals(setOf("n1"), HotfoxControlPlane.maintenanceGuids())
        HotfoxControlPlane.apply(
            controlJson(listOf(node("n1", maintenance = false)), now, keys, policyVersion = 2),
            now,
            keys.publicHex,
        )
        assertTrue(HotfoxControlPlane.maintenanceGuids().isEmpty())
        assertEquals(2, HotfoxControlPlane.policyVersion())
    }

    @Test
    fun deviceRegistryUsesGeneratedIdsAndEnforcesLimitRevokeRename() {
        val now = 10_000L
        val id = HotfoxDeviceRegistry.generateDeviceId()
        assertFalse(HotfoxDeviceRegistry.looksLikeHardwareId(id))
        assertEquals(
            HotfoxDeviceAction.REGISTERED,
            HotfoxDeviceRegistry.register(id, "Pixel", now, limit = 1).action,
        )
        val second = HotfoxDeviceRegistry.register(
            HotfoxDeviceRegistry.generateDeviceId(),
            "Tablet",
            now,
            limit = 1,
        )
        assertEquals(HotfoxDeviceAction.LIMIT_REACHED, second.action)
        assertEquals(HotfoxDeviceAction.REJECTED, HotfoxDeviceRegistry.register("123456789012345", "Phone", now).action)
        assertEquals(HotfoxDeviceAction.RENAMED, HotfoxDeviceRegistry.rename(id, "Kitchen").action)
        assertEquals("Kitchen", HotfoxDeviceRegistry.list().single().displayName)
        assertEquals(HotfoxDeviceAction.REVOKED, HotfoxDeviceRegistry.revoke(id).action)
        assertEquals(0, HotfoxDeviceRegistry.activeCount())
        assertEquals(
            HotfoxDeviceAction.REGISTERED,
            HotfoxDeviceRegistry.register(HotfoxDeviceRegistry.generateDeviceId(), "New", now, limit = 1).action,
        )
    }

    @Test
    fun apiVersioningFailsGracefullyWithoutWipingLastGood() {
        val keys = ecdsa()
        val now = 5_000L
        HotfoxControlPlane.apply(
            controlJson(listOf(node("ok")), now, keys, minClientProtocol = 1),
            now,
            keys.publicHex,
        )
        assertEquals("ok", HotfoxControlPlane.current()?.nodes?.single()?.guid)
        HotfoxControlPlane.apply(
            controlJson(listOf(node("newer")), now, keys, minClientProtocol = 9),
            now,
            keys.publicHex,
            clientProtocol = 1,
        )
        assertEquals("ok", HotfoxControlPlane.current()?.nodes?.single()?.guid)
        assertTrue(HotfoxApiCompatibility.failGracefully(HotfoxApiCompatibility.evaluate(clientProtocol = 1, minClientProtocol = 9)))
        assertEquals(HotfoxApiCompat.OK, HotfoxApiCompatibility.evaluate(clientProtocol = 1, minClientProtocol = 1))
    }

    @Test
    fun offlinePolicyDoesNotFabricateEntitlementAndHonorsTtl() {
        assertFalse(HotfoxOfflinePolicy.fabricateEntitlementOnBillingOutage())
        assertFalse(
            HotfoxOfflinePolicy.entitlementContinues(
                status = EntitlementStatus.NONE,
                expiresAtEpochSeconds = 100L,
                nowEpochSeconds = 10L,
                billingDown = true,
            ),
        )
        assertTrue(
            HotfoxOfflinePolicy.entitlementContinues(
                status = EntitlementStatus.ACTIVE,
                expiresAtEpochSeconds = 100L,
                nowEpochSeconds = 10L,
                billingDown = true,
            ),
        )
        assertFalse(
            HotfoxOfflinePolicy.entitlementContinues(
                status = EntitlementStatus.ACTIVE,
                expiresAtEpochSeconds = 5L,
                nowEpochSeconds = 10L,
                billingDown = false,
            ),
        )
        assertTrue(HotfoxOfflinePolicy.metadataUsable(expiresAtEpochMs = 10L, nowEpochMs = 10L, graceMs = 5L))
        assertFalse(HotfoxOfflinePolicy.metadataUsable(expiresAtEpochMs = 10L, nowEpochMs = 20L, graceMs = 5L))
    }

    @Test
    fun privacyTelemetryRejectsSecretsAndRecordsSafeCounters() {
        assertTrue(
            HotfoxPrivacyTelemetry.record(
                HotfoxPrivacySafeDatum(HotfoxPrivacySafeEvent.SHADOW_FALLBACK, region = "eu-west", transport = "tcp"),
            ),
        )
        assertFalse(
            HotfoxPrivacyTelemetry.record(
                HotfoxPrivacySafeDatum(HotfoxPrivacySafeEvent.TRANSPORT_FAIL, region = "vless://secret"),
            ),
        )
        assertEquals(1, HotfoxPrivacyTelemetry.current()[HotfoxPrivacySafeEvent.SHADOW_FALLBACK])
        assertNull(HotfoxPrivacyTelemetry.current()[HotfoxPrivacySafeEvent.TRANSPORT_FAIL])
    }

    @Test
    fun migrationPreservesEntitlementAutoManualAndRouting() {
        val migrated = HotfoxStateMigration.migrate(
            HotfoxV2PersistedState(
                autoMode = true,
                selectedGuid = "ams-1",
                routingMode = "exclude_apps",
                entitlementStatus = "ACTIVE",
                entitlementExpiresAtEpochSeconds = 99L,
                hasManualSubscription = true,
                shadowAuto = true,
                existingDeviceId = "keep-device",
            ),
        )
        assertTrue(migrated.autoMode)
        assertEquals("ams-1", migrated.selectedGuid)
        assertEquals("exclude_apps", migrated.routingMode)
        assertEquals("ACTIVE", migrated.entitlementStatus)
        assertEquals(99L, migrated.entitlementExpiresAtEpochSeconds)
        assertTrue(migrated.hasManualSubscription)
        assertTrue(migrated.shadowAuto)
        assertEquals("keep-device", migrated.deviceId)
        assertEquals(0, migrated.controlPlanePolicyVersion)
    }

    @Test
    fun remoteFlagsStillDenyFakeConnectedAndDnsIPv6Weakening() {
        val keys = ecdsa()
        val now = 7L
        val ok = flagsJson(mapOf("ops.update_check" to true), now, keys)
        assertEquals(true, HotfoxRemoteFlags.apply(ok, now, keys.publicHex)["ops.update_check"])
        val poisoned = ok.replace(
            "\"ops.update_check\":true",
            "\"vpn.fake_connected\":true,\"ops.update_check\":false",
        )
        assertEquals(true, HotfoxRemoteFlags.apply(poisoned, now, keys.publicHex)["ops.update_check"])
        assertNull(HotfoxRemoteFlags.current()["vpn.fake_connected"])
        assertTrue(HotfoxPlatformInventory.remoteConfigForbidden.contains("vpn.fake_connected"))
        assertTrue(HotfoxPlatformInventory.neverCollect.any { it.contains("IMEI") })
        assertTrue(HotfoxPlatformInventory.securityBoundaries.any { it.contains("HotfoxEngineFacade") })
    }

    @Test
    fun clientTooOldDoesNotPublishProtectedState() {
        val before = VpnSessionCoordinator.currentState()
        HotfoxControlPlane.apply("not-json", nowEpochMs = 1L, publicKeySpkiHex = "ab")
        assertEquals(before, VpnSessionCoordinator.currentState())
        assertFalse(HotfoxEngineFacade.currentState().isProtected())
    }

    private data class TestKeys(val publicHex: String, val private: java.security.PrivateKey)

    private fun ecdsa(): TestKeys {
        val g = KeyPairGenerator.getInstance("EC")
        g.initialize(ECGenParameterSpec("secp256r1"))
        val pair = g.generateKeyPair()
        return TestKeys(pair.public.encoded.joinToString("") { "%02x".format(it) }, pair.private)
    }

    private fun sign(payload: ByteArray, key: java.security.PrivateKey): String {
        val s = Signature.getInstance("SHA256withECDSA")
        s.initSign(key)
        s.update(payload)
        return s.sign().joinToString("") { "%02x".format(it) }
    }

    private fun node(
        guid: String,
        region: String = "eu-west",
        weight: Int = 1,
        load: HotfoxLoadBucket = HotfoxLoadBucket.LOW,
        maintenance: Boolean = false,
        drained: Boolean = false,
        role: HotfoxNodeRole = HotfoxNodeRole.EXIT,
    ): HotfoxControlNode = HotfoxControlNode(guid, region, weight, load, maintenance, drained, role)

    private fun controlJson(
        nodes: List<HotfoxControlNode>,
        now: Long,
        keys: TestKeys,
        issued: Long = now,
        expires: Long = now + 10_000L,
        policyVersion: Int = 1,
        minClientProtocol: Int = 1,
    ): String {
        val payload = HotfoxControlPlane.canonicalPayload(
            nodes = nodes,
            issuedAtEpochMs = issued,
            expiresAtEpochMs = expires,
            policyVersion = policyVersion,
            minClientProtocol = minClientProtocol,
        )
        val sig = sign(payload, keys.private)
        val nodeJson = nodes.joinToString(",") { n ->
            """{"guid":"${n.guid}","region":"${n.region}","weight":${n.weight},"loadBucket":"${n.load.name}","maintenance":${n.maintenance},"drained":${n.drained},"role":"${n.role.name}"}"""
        }
        return """{"schema":1,"issuedAtEpochMs":$issued,"expiresAtEpochMs":$expires,"policyVersion":$policyVersion,"minClientProtocol":$minClientProtocol,"nodes":[$nodeJson],"signature":"$sig"}"""
    }

    private fun flagsJson(
        flags: Map<String, Boolean>,
        now: Long,
        keys: TestKeys,
        issued: Long = now,
        expires: Long = now + 100,
    ): String {
        val sig = sign(HotfoxRemoteFlags.canonicalPayload(flags, issued, expires), keys.private)
        val fields = flags.entries.joinToString(",") { "\"${it.key}\":${it.value}" }
        val extra = if (fields.isBlank()) "" else ",$fields"
        return """{"schema":1,"issuedAtEpochMs":$issued,"expiresAtEpochMs":$expires$extra,"signature":"$sig"}"""
    }
}
