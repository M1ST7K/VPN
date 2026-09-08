# HotFox Phase Gates

This file separates engineering progress from release verification so development can continue without falsely treating deferred physical-device checks as complete.

## Gate model

Each phase may have two independent statuses:

- **ENGINEERING GATE** — code/spec work for the phase is complete enough to continue development: applicable CI/build/tests are green and the trusted checkpoint reviewer reports no substantiated P0/P1 blockers.
- **RELEASE GATE** — all release acceptance evidence for the phase is complete, including physical-device checks when required.

A later phase may begin after the previous phase reaches **ENGINEERING COMPLETE**, provided any deferred release blocker remains explicitly tracked and no production release is declared until all required release gates pass.

## Phase 2.2 — Truthful Core

Candidate implementation SHA before checkpoint round 8: `f6565291af7aff6c5bad543c7b7837de9f0c0fb7`.
Checkpoint request SHA: `62aaa9c5b6763ae7d1fded5d81d0cd2e7e8f7ac1`.

Engineering evidence available for the implementation block:

- debug APK build: PASS;
- unit tests: PASS (108 tests reported by the implementation task);
- Android lint: PASS;
- unsigned release compile: PASS;
- HotFox static/overlay verifiers: PASS;
- GitHub CI build/test/lint/release artifact pipeline: PASS;
- exact-SHA debug APK artifact published.

### Engineering gate status

**BLOCKED — CHECKPOINT ROUND 8: CHANGES_REQUIRED**

Trusted GPT-5.6 Sol checkpoint round 8 found one P0 and three P1 blockers that must be fixed before 2.2 can be marked Engineering Complete:

1. **P0 — loop prevention must fail closed.** Initial startup/reload must not launch Xray when `VpnLoopPrevention.bindProcessToUnderlying()` fails; otherwise Xray outbound can loop back through the TUN.
2. **P1 — repeated-stop ownership race.** Join/new-stop ownership must be decided atomically under authoritative lifecycle/stop ownership so a concurrent caller cannot supersede the live stop epoch and reject late success.
3. **P1 — handover shutdown drain.** A replacement core must not launch while the old expected shutdown callback is unresolved; drain timeout must fail the handover closed.
4. **P1 — detached restart race.** Restart work must be lifecycle/generation-owned and invalidated by a later explicit disconnect/start/server-selection intent.

Phase 2.2 therefore remains the current engineering phase. Do not start 2.3 implementation until a later checkpoint returns `APPROVED` with no substantiated P0/P1 blockers.

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
