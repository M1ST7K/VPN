# CURRENT PHASE — HotFox 3.1 «Mature HotFox Platform / Pre-release Engineering»

Status: **IN PROGRESS**

3.0 is **ENGINEERING COMPLETE — runtime and physical release validation deferred** (checkpoint round 19 `APPROVED`, P0=0, P1=0) on SHA `21f341980121a49f103bd61eaa588c37f1987c8a`.

This is the only product phase agents should actively execute unless the owner explicitly changes the phase.

Previous phase: `docs/phases/3.0-premium-android.md`
Linked phase spec: `docs/phases/3.1-mature-platform.md` and `docs/HOTFOX_ROADMAP.md` (legacy heading «3.0 Mature HotFox Platform», now 3.1)
Owner roadmap override: `.cursor/rules/21-hotfox-roadmap-2.9-vpn-recovery.mdc`
Latest owner validation override: `.cursor/rules/22-hotfox-owner-release-validation-gate.mdc`
Roadmap progression rule: `.cursor/rules/20-hotfox-roadmap-progression.mdc`
Master roadmap: `docs/HOTFOX_MASTER_ROADMAP.md`
Canonical roadmap: `docs/HOTFOX_ROADMAP.md`
Review policy: `docs/AI_REVIEW_POLICY.md`
Phase gate ledger: `docs/PHASE_GATE_STATUS.md`

## Goal

Evolve HotFox from a strong Android VPN app plus backend into a mature, operable platform: engine/UI boundary, signed control plane, capacity-aware AUTO, fleet/device operations, API versioning, offline policy, privacy-safe observability and 2.x migration.

Platform complexity stays behind a controlled interface. Do not split `VpnService` into another process without measured evidence. Do not enable R8 as an aesthetic milestone. Do not fake ping/`CONNECTED`.

## Inherited guarantees (still binding)

- truthful `VpnService`/TUN → HEV → SOCKS `127.0.0.1:10808` → Xray;
- `Защищено` / notification protected / QS ACTIVE only from canonical verified VPN session;
- DNS cannot silently bypass while protection is claimed;
- IPv6 captured fail-closed with `::/0` (LAN bypass is IPv4-only at TUN; NAT64 stays in TUN);
- AUTO remains a persisted mode; manual selection stays manual;
- backend-authoritative entitlement; checkout `success=true` is not payment proof;
- no secrets in APK/logs/notifications;
- 2.5–2.8 routing, Shadow, operations and Autopilot contracts;
- 3.0 truthful headlines / error / notification / QS / onboarding / NotificationManager alias;
- 2.9 SOCKS/TUN/runtime E2E harnesses remain intact for the final release validation gate.

## 3.0 closure evidence

Trusted checkpoint **round 19** returned `APPROVED` with no substantiated P0/P1 on exact SHA `21f341980121a49f103bd61eaa588c37f1987c8a`.

Runtime/emulator/physical VPN E2E remains `NOT EXECUTED / deferred`. `RELEASE READY` is not claimed.

## Round 21 / cap-window

Push CI for routing P1 fix `844918b` was green (`34475599536`). Round 21 did **not** review that head: the old PR-lifetime checkpoint cap on `main` paused automation. That is not a product P0/P1 and is not a stuck 3.1 architecture loop: rounds 1–19 closed 2.4–3.0 APPROVED; round 20 was the first 3.1 review (routing P1, now in tree).

Owner merged PR #5. Trusted `main` includes the per-phase cap at `c1cd9ba7f8ccfddd73e2e654933e2a80e851e37c` (`MAX_REVIEW_ROUNDS=40` after last `VERDICT: APPROVED`; CAP comments are ignored). Owner merged PR #6: orchestrator on `main` now passes exact `expected_sha` (`04445e11d4848b339d8c07ecf85b166781ab23d6`). The old lifetime CAP is no longer a blocker.

Owner merged PR #7. Trusted `main` now includes `docs/AI_REVIEW_PHASE_ID` = `2.4` at `d516623c436c4c4ed1a6b010f451917a76c2f30e`, matching `docs/AI_REVIEW_CURRENT_PHASE.md`. The reviewer load step can read the file from `main`.

This head is a **new** `[hotfox-phase-exit]` after that sync. Do not reuse `868c3e0`, `e57fed5`, `17a1e92`, `5b2bba1`, `844918b`, `1dda013`, or a CAP-marked SHA.

## Work allowed now

- `HotfoxEngineFacade` so UI re-observes process-scoped engine state; Activity is not a second session owner;
- signed control-plane inventory/capacity/maintenance/weight/region/policy version (ECDSA, fail-closed, last-known-good + TTL);
- capacity-aware AUTO using control-plane load plus local health; manual stays sticky;
- fleet drain/maintenance/restore through signed metadata;
- generated scoped device IDs: register / limit / revoke / rename (no hardware fingerprinting);
- API min-client fail-graceful behavior;
- offline/outage: cached metadata TTL, entitlement continuity, billing outage does not fabricate entitlement, control-plane outage does not wipe known-good servers;
- privacy-safe counters (region/transport/billing/update/Shadow) with secret rejection;
- 2.x → 3.1 persisted-state migration;
- privacy/security architecture inventory as reviewable code.

## Not now

- claiming `RELEASE READY`;
- fake CONNECTED, fake ping, mock VPN, disabled TLS/REALITY, or readiness bypass;
- unsigned remote switches that weaken TLS/DNS/IPv6/checkout honesty;
- splitting the VPN engine process without evidence;
- enabling R8/minification as a milestone;
- requiring an account solely to collect identity;
- syncing VPN secrets without an explicit secure design;
- starting `FINAL RELEASE VALIDATION GATE` until 3.1 engineering exit is APPROVED.

## Owner validation timing override

`.cursor/rules/22-hotfox-owner-release-validation-gate.mdc` is authoritative for validation timing.

Emulator/runtime VPN E2E and physical-device E2E remain deferred to the single final release validation gate after 3.1. If they have not executed, report `NOT EXECUTED / deferred`, never PASS.

## Checkpoint protocol — no idle after CI

Intermediate fixes use ordinary commits without review markers.

When all known phase work or all currently substantiated P0/P1 findings are fixed and a commit is intended as the next engineering-exit candidate, that FINAL candidate commit MUST contain `[hotfox-phase-exit]` **before** waiting for push CI.

If review is APPROVED, close 3.1 as `3.1 ENGINEERING COMPLETE — runtime and physical release validation deferred` and stop automatic progression at `FINAL RELEASE VALIDATION GATE`. Do not claim `RELEASE READY`.

## Phase 3.1 exit definition

3.1 may close when:

- repository-side platform scope above is implemented without weakening inherited VPN/truth/security guarantees;
- required CI/build/unit/integration/static checks pass;
- final review has P0=0 / P1=0.

Truthful status wording after approval:

`3.1 ENGINEERING COMPLETE — runtime and physical release validation deferred.`

Then stop at `FINAL RELEASE VALIDATION GATE`. Never claim `RELEASE READY` without real emulator + physical E2E.
