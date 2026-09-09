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

### Engineering gate status

**IN PROGRESS** — first coherent app/foundation block is implemented and automated gates are green. Phase 2.3 is **not** engineering-complete until a sandbox/test payment proves the full `plan → checkout → backend-verified entitlement → sync → AUTO → 2.2 VPN path` exit.

Do not claim a production VPN release. 2.2 physical-device E2E remains **NOT EXECUTED**.

### Release gate status

**NOT STARTED** — 2.3 does not satisfy the 2.2 physical-device release gate.
