# HotFox Phase Gates

This file separates engineering progress from release verification so development can continue without falsely treating deferred physical-device checks as complete.

## Gate model

Each phase may have two independent statuses:

- **ENGINEERING GATE** — code/spec work for the phase is complete enough to continue development: applicable CI/build/tests are green and the trusted checkpoint reviewer reports no substantiated P0/P1 blockers.
- **RELEASE GATE** — all release acceptance evidence for the phase is complete, including physical-device checks when required.

A later phase may begin after the previous phase reaches **ENGINEERING COMPLETE**, provided any deferred release blocker remains explicitly tracked and no production release is declared until all required release gates pass.

## Phase 2.2 — Truthful Core

Round-8 implementation SHA: `2bd2b9eacbfbb513207d4c165dbc529ede62e9ab`.
Round-9 restart-race fix SHA: `75839a5689cc034e7e568b5cbe4f9c5fced96381`.

Round-8 P0/P1 were addressed in `2bd2b9e`:

1. Loop-prevention bind failure is fail-closed on startup and handover (`HF-VPN-012`).
2. Repeated-stop join vs mint is decided under `lifecycleLock` (no pre-lock worker snapshot).
3. Handover does not launch a replacement until the old-core shutdown is drained (`HF-VPN-013` on timeout).
4. `MSG_STATE_RESTART` became generation-owned and invalidated by later stop/start intent.

Round 9 found no P0 and one remaining P1: restart authorization was still a check-then-start TOCTOU race. `75839a5` fixes that by making authorization and dispatch atomic with stop invalidation via `VpnRestartGate.tryDispatchStart()`.

Local engineering evidence for `75839a5`:

- debug APK build: PASS;
- unit tests: PASS (120);
- Android lint: PASS;
- unsigned release compile: PASS;
- HotFox static/overlay verifiers: PASS.

GitHub CI for exact SHA `75839a5689cc034e7e568b5cbe4f9c5fced96381`:

- Payload integrity: PASS;
- Reconstruct and build Android app: PASS;
- Unit tests: PASS;
- Android lint: PASS;
- unsigned release compile: PASS;
- debug APK artifact upload: PASS;
- Publish HotFox Dev Latest: PASS;
- Emulator UI smoke: manual-only and not part of the engineering gate.

### Engineering gate status

**ENGINEERING COMPLETE** — trusted checkpoint **round 10** returned `APPROVED` with no substantiated P0/P1 (head `3a835d8`, implementation `75839a5`).

P2 only: the production-visible `VpnRestartGate.testProbe` seam may later move behind a test-only abstraction. It is unset in production and is not a blocker.

Phase 2.3 Commercial Foundation is **ENGINEERING COMPLETE** (round 16 `APPROVED`). Phase 2.4 Smart Connection is **ENGINEERING COMPLETE** (round 4 `APPROVED`). Phase 2.5 Privacy Controls / Smart Routing is **ENGINEERING COMPLETE** (round 7 `APPROVED`). Phase 2.6 HotFox Shadow / Stealth & Resilience is the active engineering phase. Physical-device VPN E2E remains **NOT EXECUTED** and does not block 2.6. Do not claim a production VPN release.

The trusted reviewer scope on `main` is 2.3 and the checkpoint cap has been raised so a 2.3 review can run.

### Release gate status

**DEFERRED / NOT VERIFIED**

Physical-device evidence is intentionally deferred so it does not block engineering progress after the engineering gate is eventually approved.

Still required before production release:

- external IPv4 before/after VPN;
- real browser/app traffic through the tunnel;
- DNS leak behavior;
- IPv6 behavior/fail-closed result;
- rapid reconnect;
- Wi-Fi/cellular handover;
- permission/service lifecycle sanity on a physical Android device.

## Transition rule

When the trusted checkpoint for 2.2 returns `APPROVED` with no P0/P1 blockers:

1. mark 2.2 **ENGINEERING COMPLETE**;
2. keep 2.2 **RELEASE GATE DEFERRED** until physical-device evidence exists;
3. move `docs/CURRENT_PHASE.md` to **2.3 Commercial Foundation**;
4. continue roadmap development without claiming HotFox is release-verified.

If a checkpoint reports P0/P1 blockers, 2.2 remains engineering-incomplete until those findings are fixed and a later checkpoint is approved.

## Phase 2.3 — Commercial Foundation

First coherent block implementation SHA: `6a0eef8f4b9007d097539078f7840d95c75f855e`.

Included in this block:

1. Explicit commercial presentation states (`NO_ACCESS` … `RESTORE_REQUIRED`).
2. HotFox Premium no-access onboarding from backend/cached plan data; manual HTTPS import remains usable when the commercial backend is down.
3. Provider-agnostic `/v1` client (`plans`, orders, entitlement, restore, manifest, optional promo) plus a deterministic sandbox fixture. Browser `success=true` is never payment truth.
4. Keystore-backed `SecretStore` for entitlement/subscription/restore credentials; corrupt/invalidated ciphertext surfaces as restore-required rather than a crash.
5. Transactional subscription manifest refresh that preserves last-known-good inventory, favorites, manual selection, and AUTO.
6. Failure-class unit tests for idempotent orders, fake checkout returns, backend-down + manual import, keystore corruption, empty/malformed manifest, and AUTO preservation.

Local engineering evidence for `6a0eef8`:

- debug APK build: PASS;
- unit tests: PASS (128);
- Android lint: PASS;
- unsigned release compile: PASS;
- HotFox static/overlay verifiers: PASS;
- overlay secret scan: PASS.

GitHub CI for exact SHA `6a0eef8f4b9007d097539078f7840d95c75f855e`:

- Payload integrity: PASS;
- Reconstruct and build Android app: PASS;
- remaining bootstrap/Android jobs: PASS (run `34319308050`).

Round-11 trusted checkpoint on `856bfce` returned **CHANGES_REQUIRED** (P1 only). Implementation fix SHA: `664bd27a51b7bfdbfdaa5a00bcae90023abbbc6a`.

Round-11 P1s addressed in `664bd27`:

1. Paid checkout claims a backend-authoritative entitlement and persists the Keystore credential before HotFox-managed access is enabled. Browser `success=true` remains non-authoritative.
2. Keystore write failure does not set origin or clear the pending order. Presentation uses trusted entitlement status/expiry; missing backend status is not defaulted to `ACTIVE`.
3. Checkout idempotency keys are persisted before network dispatch, reused after lost responses, and serialized against concurrent taps.
4. Manifest refresh identity includes protocol/transport/security plus a SHA-256 fingerprint of connection-defining fields.
5. Dashboard reads a cached commercial snapshot; Keystore/file secret I/O runs off the UI thread.

Local engineering evidence for `664bd27`:

- debug APK build: PASS;
- unit tests: PASS (133);
- Android lint: PASS;
- unsigned release compile: PASS;
- HotFox static/overlay verifiers: PASS;
- overlay secret scan: PASS.

GitHub CI for exact SHA `664bd27a51b7bfdbfdaa5a00bcae90023abbbc6a`:

- Payload integrity: PASS;
- Reconstruct and build Android app: PASS;
- remaining bootstrap/Android jobs: PASS (run `34321635941`).

Round-12 trusted checkpoint on `a343520` returned **CHANGES_REQUIRED** (P1 only). Implementation fix SHA: `75d71a4b164283e3dafae64b9c3f9a70aa35e918`.

Round-12 P1s addressed in `75d71a4`:

1. Checkout return extracts/falls back to the order id and polls `getOrder` even when the browser URI claims `success=true`. URI markers are never payment truth and no longer short-circuit polling.
2. Authoritative `getEntitlement` `Ok(null)` invalidates the Keystore credential, trusted metadata, HotFox origin, and fulfilled local order. Transient backend errors do not.
3. Entitlement parse rejects inverted intervals and grace-before-expiry. Time before `startsAt` is non-usable (`PROVISIONING`).
4. Manifest identity hashes VLESS UUID (`password`) and TLS/Reality client fingerprint plus other config-generation fields.

Local engineering evidence for `75d71a4`:

- debug APK build: PASS;
- unit tests: PASS (137);
- Android lint: PASS;
- unsigned release compile: PASS;
- HotFox static/overlay verifiers: PASS;
- overlay secret scan: PASS.

GitHub CI for exact SHA `75d71a4b164283e3dafae64b9c3f9a70aa35e918`:

- Payload integrity: PASS;
- Reconstruct and build Android app: PASS;
- remaining bootstrap/Android jobs: PASS (run `34323252775`).

Round-13 trusted checkpoint on `bdae55b` returned **APPROVED** with no substantiated P0/P1.

Sandbox/test payment E2E implementation SHA: `5be5f346243f3f5acaeb232ecf81190c4611db10`.

Included in this block:

1. Deterministic CI fixture (`SandboxPaymentE2e`, `WebhookReconciliation`, `HostedCheckoutFixture`) proves catalog → idempotent order → hosted-checkout handoff → HMAC webhook reconciliation → entitlement claim → Keystore credential → transactional manifest sync → AUTO + 2.2 `HotfoxServerSelection.pick` handoff → restore.
2. Browser `success=true` is never payment truth and still polls `getOrder`.
3. No payment-provider private key or paid subscription credential is embedded. `HOTFOX_SANDBOX_COMMERCE` / `HOTFOX_PAYMENT_BACKEND_URL` are environment-injected.
4. Checkout return / restore on the Subscription screen refresh entitlement and preserve AUTO without publishing `CONNECTED`.
5. Exact remaining manual step: a live provider sandbox hosted checkout against a server that holds provider keys (`docs/phases/2.3-sandbox-payment-e2e.md`).

Local engineering evidence for `5be5f34`:

- debug APK build: PASS;
- unit tests: PASS (152);
- Android lint: PASS;
- unsigned release compile: PASS;
- HotFox static/overlay verifiers: PASS;
- overlay secret scan: PASS.

GitHub CI for exact SHA `5be5f346243f3f5acaeb232ecf81190c4611db10`:

- Payload integrity: PASS;
- Reconstruct and build Android app: PASS;
- remaining bootstrap/Android jobs: PASS (run `34332785857`).

Round-14 trusted checkpoint on `cd29573` returned **CHANGES_REQUIRED** (one P0, three P1). Implementation fix SHA: `2fd6dbe913d5894f695a12ef7646e0b51be0c570`.

Round-14 P0/P1s addressed in `2fd6dbe`:

1. Sandbox commerce is debug/test-only. Release Gradle fails if `HOTFOX_SANDBOX_COMMERCE=true`. Release cannot instantiate `SandboxCommerceBackend`.
2. Authenticated manifests apply through `ManagedManifestApplicator` + `ManagedServerStore` (MMKV/`ProfileItem` in production). AUTO is restored after the swap. URL manifests fetch from a Keystore token and do not write the URL to MMKV. AUTO connect uses persisted GUIDs.
3. `completeFulfillment` issues an entitlement only after a successful transition to `FULFILLED`. Unpaid/cancelled orders cannot create or claim an entitlement.
4. A second PAID webhook with a different provider payment ID is `payment_id_conflict`. `paymentOwners` is mutated only after paid-state checks.

Local engineering evidence for `2fd6dbe`:

- debug APK build: PASS;
- unit tests: PASS (155);
- Android lint: PASS;
- unsigned release compile: PASS;
- HotFox static/overlay verifiers: PASS;
- overlay secret scan: PASS.

GitHub CI for exact SHA `2fd6dbe913d5894f695a12ef7646e0b51be0c570`:

- Payload integrity: PASS;
- Reconstruct and build Android app: PASS;
- remaining bootstrap/Android jobs: PASS (run `34337272139`).

Round-15 trusted checkpoint on `dc358f6` returned **CHANGES_REQUIRED** (three P1). Implementation fix SHA: `e6c9e3c78e04abc75c76e24492e6dbf505838a68`.

Round-15 P1s addressed in `e6c9e3c`:

1. Identity-only managed JSON is rejected (`incomplete_manifest`). Live inventory is imported from complete share-link payloads; persisted profiles must produce an Xray outbound.
2. Managed replacement is staged: new profiles are written before the live subscription list is swapped. Mid-write failure restores last-known-good inventory and AUTO.
3. `HOTFOX_SANDBOX_COMMERCE=true` can configure debug. Release `BuildConfig` stays `false`. Release tasks are rejected from `gradle.taskGraph.whenReady`.

Local engineering evidence for `e6c9e3c`:

- debug APK build: PASS;
- unit tests: PASS (158);
- Android lint: PASS;
- unsigned release compile: PASS;
- HotFox static/overlay verifiers: PASS;
- overlay secret scan: PASS;
- sandbox-enabled debug BuildConfig: PASS;
- sandbox-enabled release GenerateBuildConfig: FAIL as required.

GitHub CI for exact SHA `e6c9e3c78e04abc75c76e24492e6dbf505838a68`:

- Payload integrity: PASS;
- Reconstruct and build Android app: PASS;
- remaining bootstrap/Android jobs: PASS (run `34340006767`).

Round-16 trusted checkpoint on `afeb63a` returned **APPROVED** with no substantiated P0/P1.

See `docs/phases/2.3-sandbox-payment-e2e.md` for proven vs manual.

### Engineering gate status

**ENGINEERING COMPLETE** — trusted checkpoint **round 16** returned `APPROVED` with no substantiated P0/P1 (head `afeb63a`, implementation `e6c9e3c`).

A green CI sandbox fixture is not a live provider sandbox and is not physical-device VPN E2E.

Do not claim a production VPN release. 2.2 physical-device E2E remains **NOT EXECUTED**.

### Release gate status

**NOT STARTED** — 2.3 does not satisfy the 2.2 physical-device release gate.

## Phase 2.4 — Smart Connection

**ENGINEERING COMPLETE — physical release validation deferred.** 2.3 engineering gate is closed (round 16 `APPROVED`, P0 = 0, P1 = 0). Physical-device VPN E2E remains the single later `FINAL RELEASE DEVICE GATE` and does **not** block 2.5.

Implementation SHA for the 2.4 completion block: `206990ea0cd3e7f291e2c568ee6c3669ffd1df74`.

Included in this block:

1. `ServerHealthRepository.invalidateForNetworkChange()` drops previous-network latency; last-success timestamps remain for last-good bias.
2. Cold-start AUTO prefers last-good eligible GUID when no fresh health exists.
3. `AutoCandidateFilter` excludes invalid/entitlement-blocked entries without poisoning remaining candidates. Manual HTTPS is not entitlement-gated.
4. Truthful resolved-target labels (`Подбираем сервер…` / `Авто · city`). `Защищено` still comes only from `VpnSessionCoordinator`.
5. Safe AUTO diagnostics: candidate/eligible/filtered counts, network context, last-good present/none. No subscription URLs or credentials.

GitHub CI for exact SHA `206990ea0cd3e7f291e2c568ee6c3669ffd1df74` (run `34355382189`):

- Payload integrity: PASS
- Reconstruct and overlay verification: PASS
- Static check: PASS
- debug APK build: PASS
- `:app:testPlaystoreDebugUnitTest`: PASS
- Android lint: PASS
- unsigned release compile: PASS
- sandbox debug BuildConfig: PASS
- Publish HotFox Dev Latest: PASS
- Emulator UI smoke: skipped (not VPN E2E)

This is not physical-device VPN E2E and does not yet mark 2.4 engineering-complete.

Round-1 trusted checkpoint on `1359592` returned **CHANGES_REQUIRED** (two P1). Implementation fix SHA: `917a0e98d6b61a4d673bdb4b1b36cf6c77e0c286`.

Round-1 P1s addressed in `917a0e9`:

1. `HotfoxServerSelection.candidates()` / `candidateFrom()` populate eligibility from `ManagedConfigParser.isXrayUsable`, subscription enabled state, and authoritative `AutoCommercialEligibility` (origin / managed subscription / entitlement). Invalid, disabled, and entitlement-blocked profiles are excluded before AUTO ranking. Manual HTTPS remains ungated.
2. Unscoped MMKV affiliation delays (`delayNetworkScoped = false`) are not relabeled as current-network health after `invalidateForNetworkChange()`. Post-handover ranking uses live/fresh probes; stickiness remains GUID-based (`last-good` / current eligible target).

GitHub CI for exact SHA `917a0e98d6b61a4d673bdb4b1b36cf6c77e0c286` (run `34361778626`):

- Payload integrity: PASS
- Reconstruct and overlay verification: PASS
- Static check: PASS
- debug APK build: PASS
- unit tests: PASS
- Android lint: PASS
- unsigned release compile: PASS (reconstruct job)
- Publish HotFox Dev Latest: PASS
- Emulator UI smoke: skipped (not VPN E2E)

This head is a 2.4 `[hotfox-phase-exit]` candidate. Do not record `2.4 ENGINEERING COMPLETE` until the phase-exit reviewer returns `APPROVED` with P0 = 0 and P1 = 0. Physical-device VPN E2E remains **NOT EXECUTED**.

Round-2 trusted checkpoint on `4c6f2a7` returned **CHANGES_REQUIRED** (three P1, automation/gate integrity). Fixes land in ordinary commits:

1. `workflow_run` orchestrator already lives on default `main`; dispatch now re-checks the open PR head SHA, passes `expected_sha`, and does not fail closed on a comment 403 after a successful dispatch.
2. Trusted reviewer requires `expected_sha` in `phase_exit` mode and refuses SHA mismatch / missing `[hotfox-phase-exit]` marker.
3. Roadmap continuation job runs only when `approved == true`, `verdict == APPROVED`, and `reviewed_sha == expected_sha`. `CHANGES_REQUIRED` does not post an engineering-complete handoff.

GitHub CI for exact SHA `47fbaad0d829f141f08f8d4b8c309f57a87a011e` (run `34366000496`):

- Payload integrity: PASS
- Reconstruct and overlay verification: PASS
- debug APK / unit tests / lint / unsigned release / Publish Dev Latest: PASS
- Emulator UI smoke: skipped (not VPN E2E)

This head is the next 2.4 `[hotfox-phase-exit]` candidate. Do not record `2.4 ENGINEERING COMPLETE` until APPROVED (P0=0, P1=0).

Round-3 trusted checkpoint on `47fbaad` returned **CHANGES_REQUIRED** (one P1). Fix SHA: `f9c9ed21fb4f3c7f35caa1d38053b4a7ab8d3d92`.

Phase-exit validation now uses the distinct marker `[hotfox-phase-exit]`. Ordinary `[hotfox-review]` is not required in `phase_exit` mode, so a correctly marked candidate is not refused as `NOT_REQUESTED`.

GitHub CI for exact SHA `f9c9ed21fb4f3c7f35caa1d38053b4a7ab8d3d92` (run `34367604694`): reconstruct/unit/lint/release/Publish PASS. Emulator skipped.

Round-4 trusted checkpoint on `f9c9ed21fb4f3c7f35caa1d38053b4a7ab8d3d92` returned **APPROVED** with no substantiated P0/P1. Comment: https://github.com/M1ST7K/VPN/pull/4#issuecomment-5604059835

AUTO implementation SHA remains `917a0e98d6b61a4d673bdb4b1b36cf6c77e0c286`. Phase-exit SHA-binding / `[hotfox-phase-exit]` marker validation SHA is `f9c9ed2`. Docs-only follow-up `e8a27dd` does not change the reviewed implementation.

### Engineering gate status

**ENGINEERING COMPLETE — physical release validation deferred.**

Trusted checkpoint **round 4** returned `APPROVED` (P0 = 0, P1 = 0) on head `f9c9ed2`. Physical-device VPN E2E remains **NOT EXECUTED** and is not a 2.4→2.5 blocker.

### Release gate status

**DEFERRED / NOT VERIFIED** — consolidated into `FINAL RELEASE DEVICE GATE` after 3.0.

## Phase 2.5 — Privacy Controls / Smart Routing

**IN PROGRESS / engineering-exit candidate.** 2.4 engineering gate is closed (round 4 `APPROVED`, P0 = 0, P1 = 0). Physical-device VPN E2E remains the single later `FINAL RELEASE DEVICE GATE` and does **not** block 2.5 engineering or progression to 2.6.

Implementation SHA for the 2.5 completion block: `72471ab9cc2baea5a72be2e628812e4ddd806099`.

Included in this block:

1. Routing modes Smart / Global / include-apps / exclude-apps / Custom. Captured-traffic precedence `BLOCK > APP > DOMAIN > CIDR > GLOBAL`. EXCLUDE selected / INCLUDE miss are outside TUN (DIRECT, including ads).
2. `VpnService` per-app plan and LAN TUN routes come from `HotfoxRoutingStore` snapshot. Empty include does not enable an empty allow-list. GLOBAL never bypasses LAN.
3. DNS is installed on the VPN interface; 2.5 does not offer a silent system-DNS bypass while protection is claimed.
4. IPv6 capture remains `::/0` fail-closed unless IPv6 proxying and explicit LAN bypass are both on.
5. Xray injector prepends HotFox rules in buckets `BLOCK / exact-domain / suffix-domain / CIDR` and drops preset `direct` rules except in Custom.
6. IDN hosts normalize to punycode. Malformed CIDR cannot become `0.0.0.0/0` or `::/0` DIRECT.
7. Routing reconnect is bound to `VpnRestartGate`; explicit disconnect cancels a pending routing restart. Stale routing generation cannot overwrite a newer policy.
8. Legacy `AppConfig.PREF_SMART_ROUTING_MODE` migrates when the canonical key is empty.
9. Truthful UI labels from the active snapshot; Always-on / kill switch is Android system guidance, not a silent enable.

GitHub CI for exact SHA `72471ab9cc2baea5a72be2e628812e4ddd806099` (run `34377430185`):

- Payload integrity: PASS
- Reconstruct and overlay verification: PASS
- Static check: PASS
- debug APK build: PASS
- unit tests: PASS
- Android lint: PASS
- unsigned release compile: PASS
- Publish HotFox Dev Latest: PASS
- Emulator UI smoke: skipped (not VPN E2E)

Round 6 `CHANGES_REQUIRED` on `d830dec` is addressed in `72471ab`.

### Engineering gate status

**ENGINEERING COMPLETE — physical release validation deferred.**

Trusted checkpoint **round 7** returned `APPROVED` (P0 = 0, P1 = 0) on head `18518d2` / implementation `72471ab`. Physical-device VPN E2E remains **NOT EXECUTED** and is not a 2.5→2.6 blocker.

### Release gate status

**DEFERRED / NOT VERIFIED** — consolidated into `FINAL RELEASE DEVICE GATE` after 3.0.

## Phase 2.6 — HotFox Shadow / Stealth & Resilience

**IN PROGRESS.** 2.5 engineering gate is closed (round 7 `APPROVED`, P0 = 0, P1 = 0). Physical-device VPN E2E remains the single later `FINAL RELEASE DEVICE GATE` and does **not** block 2.6 engineering or progression to 2.7.

Do not record `2.6 ENGINEERING COMPLETE` until implementation, green full CI, and phase-exit `APPROVED` (P0=0, P1=0). Physical-device VPN E2E remains **NOT EXECUTED**.


