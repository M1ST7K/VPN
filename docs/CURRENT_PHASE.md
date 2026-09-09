# CURRENT PHASE — HotFox 2.8 «HotFox Autopilot / Adaptive Protection»

Status: **IN PROGRESS** (2.7 is ENGINEERING COMPLETE — physical release validation deferred)

This is the only product phase agents should actively execute unless the owner explicitly changes the phase.

Linked detailed phase spec: `docs/phases/2.8-autopilot.md`
Previous phase: `docs/phases/2.7-operations.md`
Owner 2.9 override: `docs/HOTFOX_2_9_VPN_RECOVERY.md`
Master roadmap: `docs/HOTFOX_MASTER_ROADMAP.md`
Canonical roadmap: `docs/HOTFOX_ROADMAP.md`
Review policy: `docs/AI_REVIEW_POLICY.md`
Phase gate ledger: `docs/PHASE_GATE_STATUS.md`

## Goal

Make protection zero-touch for ordinary users without a second VPN session controller.

2.8 answers:

> Given the current network, user policy and VPN state, what should HotFox do automatically to keep the intended protection level?

All automatic start/stop goes through `VpnRestartGate` / `CoreServiceManager`.

Do **not** claim `RELEASE READY`. Phases 2.2–2.7 are **engineering-complete**. Physical validation on a real Android device is **NOT EXECUTED** and is consolidated into the single `FINAL RELEASE DEVICE GATE` after 3.1 — it does **not** block 2.8 engineering or the 2.8 → 2.9 transition.

Owner order after 2.8: **2.9 VPN Core Recovery / Real Connection Fix** (not Premium UI). Premium Android Experience is **3.0**. Mature platform is **3.1**.

## Inherited guarantees (still binding)

- truthful `VpnService`/TUN → HEV → Xray path;
- `Защищено` only from canonical verified VPN session;
- DNS cannot silently bypass while protection is claimed;
- IPv6 routed or fail-closed;
- AUTO remains a persisted mode; manual selection stays manual;
- backend-authoritative entitlement; checkout `success=true` is not payment proof;
- no secrets in APK/logs;
- 2.5 routing policy and TUN/Xray honesty;
- 2.6 Shadow fallback never weakens TLS/REALITY;
- 2.7 channels, signed updates, drain, redaction, signing honesty.

## Work allowed now

- `ConnectionIntentEngine` combining network, policy, entitlement, pause and session.
- Trusted home/office vs unknown Wi-Fi vs cellular policies.
- Pause 5/15/60 minutes or until network change.
- Captive portal wait (`Сеть требует авторизации`) without trapping the user.
- Boot/process-start recovery of policy without resurrecting a stale session id.
- Protection profiles `Скорость` / `Баланс` / `Максимальная защита` mapped to real routing.
- Event-driven network callbacks; no Autopilot polling loop.

## Not now

- 2.9 VPN core recovery / real connection E2E (starts only after 2.8 ENGINEERING COMPLETE);
- Premium Android UX polish (now 3.0);
- claiming `RELEASE READY` / production VPN release.

## Checkpoint protocol

Ordinary commits while implementing and while CI is red.

Do **not** put `[hotfox-review]` or `[hotfox-phase-exit]` on intermediate fix commits.

When a coherent 2.8 engineering-exit candidate is ready, make one final commit whose message contains:

`[hotfox-phase-exit]`

After green full CI, the GitHub phase-exit orchestrator dispatches exactly one AI checkpoint review.

## Phase 2.8 exit definition

Autopilot derives a single serialized connection intent from network/policy/entitlement/pause, starts and stops only through the existing session controller, preserves AUTO/manual intent, and does not create reconnect storms — without regressing 2.2–2.7 guarantees.
