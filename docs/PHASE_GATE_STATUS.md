# HotFox Phase Gates

This file separates engineering progress from release verification so development can continue without falsely treating deferred physical-device checks as complete.

## Gate model

Each phase may have two independent statuses:

- **ENGINEERING GATE** — code/spec work for the phase is complete enough to continue development: applicable CI/build/tests are green and the trusted checkpoint reviewer reports no substantiated P0/P1 blockers.
- **RELEASE GATE** — all release acceptance evidence for the phase is complete, including physical-device checks when required.

A later phase may begin after the previous phase reaches **ENGINEERING COMPLETE**, provided any deferred release blocker remains explicitly tracked and no production release is declared until all required release gates pass.

## Phase 2.2 — Truthful Core

Round-8 implementation SHA: `2bd2b9eacbfbb513207d4c165dbc529ede62e9ab`.
Green GitHub reconstruct SHA including later CI-only follow-ups: `b32622683a6908b5faf48221ac8181c86eedef60`.

Round-8 P0/P1 were addressed in `2bd2b9e`:

1. Loop-prevention bind failure is fail-closed on startup and handover (`HF-VPN-012`).
2. Repeated-stop join vs mint is decided under `lifecycleLock` (no pre-lock worker snapshot).
3. Handover does not launch a replacement until the old-core shutdown is drained (`HF-VPN-013` on timeout).
4. `MSG_STATE_RESTART` is generation-owned (`VpnRestartGate`) and invalidated by a later stop/start.

Local engineering evidence for `2bd2b9e`:

- debug APK build: PASS;
- unit tests: PASS (117);
- Android lint: PASS;
- unsigned release compile: PASS;
- HotFox static/overlay verifiers: PASS.

GitHub on `b326226`:

- Payload integrity: PASS;
- Reconstruct and build Android app: PASS;
- Publish HotFox Dev Latest: PASS;
- Emulator UI smoke: skipped on push (manual `workflow_dispatch` only; not VPN E2E).

### Engineering gate status

**CHECKPOINT REVIEW REQUESTED** after the round-8 fix block.

Do not mark 2.2 Engineering Complete until this checkpoint returns `APPROVED` with no substantiated P0/P1. Do not start 2.3 until that happens. Physical-device E2E remains a separate release gate.

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
