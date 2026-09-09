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

Phase 2.3 Commercial Foundation is now the active engineering phase in `docs/CURRENT_PHASE.md`. Do not claim a production VPN release.

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

Sandbox/test payment E2E (this PR, pending checkpoint):

- Deterministic CI fixture (`SandboxPaymentE2e`, `WebhookReconciliation`, `HostedCheckoutFixture`) proves catalog → idempotent order → hosted-checkout handoff → HMAC webhook reconciliation → entitlement claim → Keystore credential → transactional manifest sync → AUTO + 2.2 `HotfoxServerSelection.pick` handoff → restore.
- Browser `success=true` is never payment truth.
- No payment-provider private key or paid subscription credential is embedded. `HOTFOX_SANDBOX_COMMERCE` / `HOTFOX_PAYMENT_BACKEND_URL` are environment-injected.
- Exact remaining manual step: a real provider sandbox hosted checkout against a server that holds provider keys (`docs/phases/2.3-sandbox-payment-e2e.md`).

See `docs/phases/2.3-sandbox-payment-e2e.md` for proven vs manual.

### Engineering gate status

**IN PROGRESS** — sandbox/test payment E2E is implemented as a CI fixture. Phase 2.3 is **not** engineering-complete until this block’s `[hotfox-review]` checkpoint is **APPROVED**. A green CI fixture is not a live provider sandbox and is not physical-device VPN E2E.

Do not claim a production VPN release. 2.2 physical-device E2E remains **NOT EXECUTED**.

### Release gate status

**NOT STARTED** — 2.3 does not satisfy the 2.2 physical-device release gate.
