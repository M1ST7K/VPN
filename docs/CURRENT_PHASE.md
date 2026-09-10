# CURRENT PHASE — HotFox 3.0 «Premium Android Experience»

Status: **ENGINEERING-EXIT CANDIDATE** (round-18 P1 fixed by implementation `a002e012d585e63d1e7498f5eb5a34dcc8a55af0`; push CI `34464117864` passed; this head requests `[hotfox-phase-exit]`).

2.9 is **ENGINEERING COMPLETE — runtime and physical release validation deferred**.

This is the only product phase agents should actively execute unless the owner explicitly changes the phase.

Previous phase: `docs/phases/2.9-vpn-recovery.md`
Linked phase spec: `docs/phases/3.0-premium-android.md` and `docs/HOTFOX_ROADMAP.md` (legacy heading «2.9 Premium Android Experience», now 3.0)
Owner roadmap override: `.cursor/rules/21-hotfox-roadmap-2.9-vpn-recovery.mdc`
Latest owner validation override: `.cursor/rules/22-hotfox-owner-release-validation-gate.mdc`
Roadmap progression rule: `.cursor/rules/20-hotfox-roadmap-progression.mdc`
Master roadmap: `docs/HOTFOX_MASTER_ROADMAP.md`
Canonical roadmap: `docs/HOTFOX_ROADMAP.md`
Review policy: `docs/AI_REVIEW_POLICY.md`
Phase gate ledger: `docs/PHASE_GATE_STATUS.md`

## Goal

Make truthful HotFox behavior feel simple, deliberate and premium without hiding technical failure.

Canonical visual language remains: charcoal / purple-black canvas, warm cream typography, restrained orange accent, green only for real protected success.

Primary phone navigation remains exactly:

`Соединение` / `Серверы` / `Подписка`

## Inherited guarantees (still binding)

- truthful `VpnService`/TUN → HEV → SOCKS `127.0.0.1:10808` → Xray;
- `Защищено` / notification protected / QS ACTIVE only from canonical verified VPN session;
- DNS cannot silently bypass while protection is claimed;
- IPv6 captured fail-closed with `::/0` (LAN bypass is IPv4-only at TUN; NAT64 stays in TUN);
- AUTO remains a persisted mode; manual selection stays manual;
- backend-authoritative entitlement; checkout `success=true` is not payment proof;
- no secrets in APK/logs/notifications;
- 2.5–2.8 routing, Shadow, operations and Autopilot contracts;
- 2.9 SOCKS/TUN/runtime E2E harnesses remain intact for the final release validation gate.

## Round 18 closure evidence

The round-18 P1 was the `NotificationManager` Android framework type-name collision. Implementation `a002e01` aliases `android.app.NotificationManager` as `AndroidNotificationManager` and adds regression/static coverage.

Push CI for `a002e01` completed successfully (`HotFox bootstrap and Android CI`, run `34464117864`).

The cloud/background agent did not run a local reconstruct/debug build because its environment lacked Android SDK. That local result is truthfully `NOT EXECUTED`; it is not an intermediate stop condition because repository CI provides the required engineering build/unit/lint/static/release-compilation gate.

Runtime/emulator/physical VPN E2E remains `NOT EXECUTED / deferred` under the owner validation override. `RELEASE READY` is not claimed.

## Work allowed now

- Connection-state presentation sequence derived from `VpnSessionState` / `VpnConnectionStage`;
- truthful AUTO / Shadow labels;
- error/recovery presentation mapper (never raw `HF-VPN-*` as the headline);
- subscription, server-list, routing and Autopilot UX polish;
- first-run onboarding around real permission/access/AUTO/connect requirements;
- truthful foreground notification and Quick Settings Tile adapters;
- accessibility, reduced-motion, restrained haptics;
- screenshot/golden or deterministic ViewState fixtures where maintainable.

## Not now

- claiming `RELEASE READY`;
- fake CONNECTED, fake ping, mock VPN, disabled TLS/REALITY, or readiness bypass;
- deleting or weakening 2.9 diagnostic/runtime E2E harnesses;
- starting 3.1 Mature Platform until 3.0 engineering exit is APPROVED.

## Owner validation timing override

`.cursor/rules/22-hotfox-owner-release-validation-gate.mdc` is authoritative for validation timing.

Emulator/runtime VPN E2E and physical-device E2E remain deferred to the single final release validation gate after 3.1. If they have not executed, report `NOT EXECUTED / deferred`, never PASS.

## Checkpoint protocol — no idle after CI

Intermediate fixes use ordinary commits without review markers.

When all known phase work or all currently substantiated P0/P1 findings are fixed and a commit is intended as the next engineering-exit candidate, that FINAL candidate commit MUST contain `[hotfox-phase-exit]` **before** waiting for push CI. The marker is review intent, not proof of green CI.

The GitHub orchestrator itself waits for successful push CI before it dispatches the expensive AI review. Therefore the agent must never stop with “жду push CI; `[hotfox-phase-exit]` не ставил” on a final known-fix candidate.

If CI is red, repair with ordinary commits and put `[hotfox-phase-exit]` on the next coherent final candidate. If review is CHANGES_REQUIRED, fix every substantiated P0/P1 and repeat automatically. If review is APPROVED, close 3.0 and immediately start 3.1 without waiting for another user message.

## Phase 3.0 exit definition

3.0 may close when:

- Premium UX scope above is implemented without weakening inherited VPN/truth/security guarantees;
- required CI/build/unit/integration/static checks pass;
- final review has P0=0 / P1=0.

Truthful status wording after approval:

`3.0 ENGINEERING COMPLETE — runtime and physical release validation deferred.`

Then immediately start `3.1 Mature HotFox Platform / Pre-release Engineering`.
