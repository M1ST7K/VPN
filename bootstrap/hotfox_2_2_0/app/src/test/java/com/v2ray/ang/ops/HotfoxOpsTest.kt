package com.v2ray.ang.ops

import com.v2ray.ang.vpn.AutoCandidateFilter
import com.v2ray.ang.vpn.AutoFilterReason
import com.v2ray.ang.vpn.AutoSelectionPolicy
import com.v2ray.ang.vpn.HotfoxServerSelection
import com.v2ray.ang.vpn.VpnSessionCoordinator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec

class HotfoxOpsTest {
    private val running = HotfoxBuildIdentity(
        versionName = "2.2.0",
        versionCode = 22000,
        gitSha = "abc123",
        channel = HotfoxReleaseChannel.DEV,
        distribution = "Local",
        debug = true,
    )

    @Before
    fun reset() {
        HotfoxNodeDrain.resetForTests()
        HotfoxRemoteFlags.resetForTests()
        VpnSessionCoordinator.resetForTests()
    }

    @Test
    fun channelFromStorageAndSandboxIsolation() {
        assertEquals(HotfoxReleaseChannel.DEV, HotfoxReleaseChannel.fromStorage(null))
        assertEquals(HotfoxReleaseChannel.BETA, HotfoxReleaseChannel.fromStorage("beta"))
        assertEquals(HotfoxReleaseChannel.STABLE, HotfoxReleaseChannel.fromStorage("STABLE"))
        assertFalse(HotfoxReleaseChannel.DEV.isolatesSandbox())
        assertTrue(HotfoxReleaseChannel.STABLE.isolatesSandbox())
        assertTrue(HotfoxReleaseChannel.STABLE.requiresUpdateSignature())
        assertFalse(HotfoxReleaseChannel.DEV.requiresUpdateSignature())
        assertEquals(
            "hotfox-2.2.0-22000-dev-abc123",
            running.artifactLabel(),
        )
    }

    @Test
    fun updateRejectsHashMismatchChannelDowngradeExpiryAndKnownBad() {
        val now = 1_000L
        val base = unsignedManifest(versionCode = 22001, channel = HotfoxReleaseChannel.DEV, now = now)
        val bytes = "apk-bytes".toByteArray()
        val hashed = base.copy(sha256 = HotfoxOpsCrypto.sha256Hex(bytes))
        assertEquals(
            HotfoxUpdateState.AVAILABLE,
            HotfoxUpdatePolicy.evaluate(running, hashed, now, publicKeySpkiHex = "", apkBytes = bytes).state,
        )
        assertEquals(
            HotfoxUpdateState.REJECTED_HASH,
            HotfoxUpdatePolicy.evaluate(running, hashed, now, "", apkBytes = "other".toByteArray()).state,
        )
        assertEquals(
            HotfoxUpdateState.REJECTED_CHANNEL,
            HotfoxUpdatePolicy.evaluate(
                running.copy(channel = HotfoxReleaseChannel.STABLE),
                hashed,
                now,
                "",
            ).state,
        )
        assertEquals(
            HotfoxUpdateState.REJECTED_DOWNGRADE,
            HotfoxUpdatePolicy.evaluate(
                running.copy(versionCode = 23000),
                hashed,
                now,
                "",
            ).state,
        )
        assertEquals(
            HotfoxUpdateState.REJECTED_EXPIRED,
            HotfoxUpdatePolicy.evaluate(running, hashed, now + 10_000, "").state,
        )
        val bad = hashed.copy(knownBadVersionCodes = setOf(22000))
        assertEquals(
            HotfoxUpdateState.BLOCKED_KNOWN_BAD,
            HotfoxUpdatePolicy.evaluate(running, bad, now, "").state,
        )
        assertEquals(HotfoxUpdateState.MISSING, HotfoxUpdatePolicy.evaluate(running, null, now, "").state)
        assertEquals(HotfoxUpdateState.REJECTED_SCHEMA, HotfoxUpdatePolicy.evaluate(running, hashed.copy(schema = 9), now, "").state)
        assertEquals("Доступно обновление", HotfoxUpdatePolicy.uiLabel(HotfoxUpdateDecision(HotfoxUpdateState.AVAILABLE, "newer")))
        assertFalse(HotfoxUpdatePolicy.maySilentlyInstall())
        val available = HotfoxUpdatePolicy.evaluate(running, hashed, now, "", apkBytes = bytes)
        assertTrue(available.offersInstall)
        assertTrue(HotfoxUpdatePolicy.canOfferUserInstall(available, bytes))
        assertFalse(HotfoxUpdatePolicy.canOfferUserInstall(available, "other".toByteArray()))
        assertEquals(
            HotfoxUpdateState.REJECTED_IDENTITY,
            HotfoxUpdatePolicy.evaluate(running, hashed.copy(packageName = "com.evil.app"), now, "").state,
        )
        assertEquals(
            HotfoxUpdateState.REJECTED_IDENTITY,
            HotfoxUpdatePolicy.evaluate(
                running.copy(signingCertSha256 = "aa".repeat(32)),
                hashed.copy(signingCertSha256 = "bb".repeat(32)),
                now,
                "",
            ).state,
        )
    }

    @Test
    fun signedManifestRequiredForStableAndInvalidSignatureRejected() {
        val keys = ecdsa()
        val now = 50L
        val unsigned = unsignedManifest(22001, HotfoxReleaseChannel.STABLE, now)
        val signed = unsigned.copy(signatureHex = sign(unsigned.canonicalPayload(), keys.private))
        val runningStable = running.copy(channel = HotfoxReleaseChannel.STABLE)
        assertEquals(
            HotfoxUpdateState.REJECTED_SIGNATURE,
            HotfoxUpdatePolicy.evaluate(runningStable, unsigned, now, keys.publicHex).state,
        )
        assertEquals(
            HotfoxUpdateState.AVAILABLE,
            HotfoxUpdatePolicy.evaluate(runningStable, signed, now, keys.publicHex).state,
        )
        val other = ecdsa()
        assertEquals(
            HotfoxUpdateState.REJECTED_SIGNATURE,
            HotfoxUpdatePolicy.evaluate(runningStable, signed, now, other.publicHex).state,
        )
    }

    @Test
    fun parseHttpsOnlyAndRoundTripKnownBad() {
        val json = """
            {"schema":1,"channel":"dev","versionName":"2.2.1","versionCode":22001,
             "gitSha":"deadbeef","apkUrl":"https://example.com/hotfox.apk",
             "sha256":"${"a".repeat(64)}","minUpdaterVersionCode":1,
             "issuedAtEpochMs":1,"expiresAtEpochMs":9,
             "knownBadVersionCodes":[21999,22000],"signature":""}
        """.trimIndent()
        val parsed = HotfoxUpdatePolicy.parse(json)
        requireNotNull(parsed)
        assertEquals(setOf(21999, 22000), parsed.knownBadVersionCodes)
        assertEquals(null, HotfoxUpdatePolicy.parse(json.replace("https://", "http://")))
    }

    @Test
    fun drainRequiresSignatureAndExpiresKeepLastGood() {
        val keys = ecdsa()
        val now = 100L
        val first = drainJson(setOf("guid-a"), now, keys)
        assertEquals(setOf("guid-a"), HotfoxNodeDrain.apply(first, now, keys.publicHex))
        val unsigned = first.replace(Regex("\"signature\":\"[^\"]+\""), "\"signature\":\"00\"")
        assertEquals(setOf("guid-a"), HotfoxNodeDrain.apply(unsigned, now, keys.publicHex))
        val expired = drainJson(setOf("guid-b"), now, keys, issued = now, expires = now + 1)
        assertEquals(setOf("guid-a"), HotfoxNodeDrain.apply(expired, now + 50, keys.publicHex))
        val emptyKey = drainJson(setOf("guid-c"), now, keys)
        assertEquals(setOf("guid-a"), HotfoxNodeDrain.apply(emptyKey, now, publicKeySpkiHex = ""))
    }

    @Test
    fun drainedNodeIsExcludedFromNewAutoPicksButManualCanStay() {
        val keys = ecdsa()
        val now = 5L
        HotfoxNodeDrain.apply(drainJson(setOf("keep-out"), now, keys), now, keys.publicHex)
        val servers = listOf(
            HotfoxServerSelection.Candidate("keep-out", "A", delay = 20),
            HotfoxServerSelection.Candidate("ok", "B", delay = 30),
        )
        assertEquals(AutoFilterReason.DRAINED, AutoCandidateFilter.reasonOf(servers[0]))
        val picked = AutoSelectionPolicy.pick(
            servers,
            auto = true,
            selectedGuid = null,
            nowEpochMs = now,
        )
        assertTrue(picked is HotfoxServerSelection.ResolveResult.Success)
        assertEquals("ok", (picked as HotfoxServerSelection.ResolveResult.Success).guid)
        val manual = AutoSelectionPolicy.pick(
            servers,
            auto = false,
            selectedGuid = "keep-out",
            nowEpochMs = now,
        )
        assertEquals(
            HotfoxServerSelection.ResolveResult.Success("keep-out", false),
            manual,
        )
    }

    @Test
    fun serviceHealthExpiresAndIncidentCannotBeEmptyHtml() {
        val now = 10L
        val json = """
            {"issuedAtEpochMs":1,"expiresAtEpochMs":20,"api":"ok","billing":"degraded",
             "entitlement":"ok","manifest":"ok","nodes":"down","shadowEntries":"unknown",
             "incidentTitle":"Плановые работы","incidentExpiresAtEpochMs":15}
        """.trimIndent()
        val status = HotfoxServiceHealth.parse(json, now)
        requireNotNull(status)
        assertEquals(HotfoxComponentHealth.DEGRADED, status.billing)
        assertEquals(HotfoxComponentHealth.DOWN, status.nodes)
        assertTrue(status.incident!!.visible(now))
        assertFalse(status.incident!!.visible(16))
        assertEquals(null, HotfoxServiceHealth.parse(json, nowEpochMs = 21))
        val html = json.replace("Плановые работы", "<script>alert(1)</script>Работы")
        val sanitized = HotfoxServiceHealth.parse(html, now)
        requireNotNull(sanitized)
        assertFalse(sanitized.incident!!.title.contains("<"))
        assertFalse(sanitized.incident!!.title.contains(">"))
        val before = VpnSessionCoordinator.currentState()
        HotfoxServiceHealth.parse(json, now)
        assertEquals(before, VpnSessionCoordinator.currentState())
        assertFalse(VpnSessionCoordinator.currentState().isProtected())
    }

    @Test
    fun remoteFlagsAllowlistAndDenySecurityWeakening() {
        val keys = ecdsa()
        val now = 3L
        val ok = flagsJson(mapOf("ops.update_check" to true), now, keys)
        assertEquals(true, HotfoxRemoteFlags.apply(ok, now, keys.publicHex)["ops.update_check"])
        val bad = flagsJson(mapOf("ops.update_check" to false), now, keys)
            .replace("\"ops.update_check\":false", "\"vpn.skip_tls\":true,\"ops.update_check\":false")
        assertEquals(true, HotfoxRemoteFlags.apply(bad, now, keys.publicHex)["ops.update_check"])
        assertEquals(null, HotfoxRemoteFlags.current()["vpn.skip_tls"])
        val unsigned = ok.replace(Regex("\"signature\":\"[^\"]+\""), "\"signature\":\"00\"")
        assertEquals(true, HotfoxRemoteFlags.apply(unsigned, now, keys.publicHex)["ops.update_check"])
        val expired = flagsJson(mapOf("ops.update_check" to false), issued = 1, expires = 2, now = now, keys = keys)
        assertEquals(true, HotfoxRemoteFlags.apply(expired, now, keys.publicHex)["ops.update_check"])
        assertEquals(true, HotfoxRemoteFlags.apply(ok, now, publicKeySpkiHex = "")["ops.update_check"])
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

    private fun unsignedManifest(
        versionCode: Int,
        channel: HotfoxReleaseChannel,
        now: Long,
    ): HotfoxUpdateManifest {
        val sha = "b".repeat(64)
        return HotfoxUpdateManifest(
            schema = 1,
            channel = channel,
            versionName = "2.2.1",
            versionCode = versionCode,
            gitSha = "ffff",
            apkUrl = "https://example.com/hotfox.apk",
            sha256 = sha,
            minUpdaterVersionCode = 1,
            issuedAtEpochMs = now,
            expiresAtEpochMs = now + 100,
            knownBadVersionCodes = emptySet(),
            signatureHex = "",
        )
    }

    private fun drainJson(guids: Set<String>, now: Long, keys: TestKeys, issued: Long = now, expires: Long = now + 100): String {
        val unsigned = HotfoxDrainManifest(
            schema = 1,
            issuedAtEpochMs = issued,
            expiresAtEpochMs = expires,
            drainedGuids = guids,
            signatureHex = "",
        )
        val sig = sign(unsigned.canonicalPayload(), keys.private)
        val list = guids.joinToString(",") { "\"$it\"" }
        return """{"schema":1,"issuedAtEpochMs":$issued,"expiresAtEpochMs":$expires,"drainedGuids":[$list],"signature":"$sig"}"""
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
