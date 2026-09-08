# HotFox Phase Gates

This file separates engineering progress from release verification so development can continue without falsely treating deferred physical-device checks as complete.

## Gate model

Each phase may have two independent statuses:

- **ENGINEERING GATE** — code/spec work for the phase is complete enough to continue development: applicable CI/build/tests are green and the trusted checkpoint reviewer reports no substantiated P0/P1 blockers.
- **RELEASE GATE** — all release acceptance evidence for the phase is complete, including physical-device checks when required.

A later phase may begin after the previous phase reaches **ENGINEERING COMPLETE**, provided any deferred release blocker remains explicitly tracked and no production release is declared until all required release gates pass.

## Phase 2.2 — Truthful Core

Candidate implementation SHA before this checkpoint: `f6565291af7aff6c5bad543c7b7837de9f0c0fb7`.

Engineering evidence already available for that implementation block:

- debug APK build: PASS;
- unit tests: PASS (108 tests reported by the implementation task);
- Android lint: PASS;
- unsigned release compile: PASS;
- HotFox static/overlay verifiers: PASS;
- GitHub CI build/test/lint/release artifact pipeline: PASS;
- exact-SHA debug APK artifact published.

Current engineering status: **CHECKPOINT REVIEW REQUESTED**.

Current release status: **NOT VERIFIED**.

Deferred physical-device release evidence:

- external IPv4 before/after VPN;
- real browser/app traffic through the tunnel;
- DNS leak behavior;
- IPv6 behavior/fail-closed result;
- rapid reconnect;
- Wi-Fi/cellular handover;
- permission/service lifecycle sanity on a physical Android device.

These checks remain mandatory before release even if phase 2.3 development begins.

## Transition rule

If the trusted GPT-5.6 Sol checkpoint for the current 2.2 implementation reports `APPROVED` with no substantiated P0/P1 blockers, mark 2.2 **ENGINEERING COMPLETE / RELEASE GATE DEFERRED** and move `docs/CURRENT_PHASE.md` to 2.3 Commercial Foundation.

If the checkpoint reports P0/P1 blockers, 2.2 remains engineering-incomplete until those findings are fixed and a later checkpoint is approved.
