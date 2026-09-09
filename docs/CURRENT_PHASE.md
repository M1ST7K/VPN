# CURRENT PHASE — HotFox 2.6 «HotFox Shadow / Stealth & Resilience»

Status: **ENGINEERING-EXIT CANDIDATE** (implementation `0854e17`, CI isolation `581967c` PASS; this head requests `[hotfox-phase-exit]`)

This is the only product phase agents should actively execute unless the owner explicitly changes the phase.

Linked detailed phase spec: `docs/phases/2.6-shadow.md`
Previous phase: `docs/phases/2.5-privacy-controls.md`
Master roadmap: `docs/HOTFOX_MASTER_ROADMAP.md`
Canonical roadmap: `docs/HOTFOX_ROADMAP.md`
Review policy: `docs/AI_REVIEW_POLICY.md`
Phase gate ledger: `docs/PHASE_GATE_STATUS.md`

## Goal

Make HotFox resilient when the fastest/default VPN path is unavailable, filtered, unstable or degraded.

2.6 answers:

> Which server + transport + route is the best usable protected path in the current network environment?

This is a technical reliability feature. It is not a license to weaken TLS, REALITY or certificate verification.

Do **not** claim HotFox is a production-ready release. Phases 2.2–2.5 are **engineering-complete**. Physical validation on a real Android device is **NOT EXECUTED** and is consolidated into the single `FINAL RELEASE DEVICE GATE` after 3.0 — it does **not** block 2.6 engineering or the 2.6 → 2.7 transition.

## Inherited guarantees (still binding)

- truthful `VpnService`/TUN → HEV → Xray path;
- `Защищено` only from canonical verified VPN session;
- DNS cannot silently bypass while protection is claimed;
- IPv6 routed or fail-closed;
- AUTO remains a persisted mode; manual selection stays manual;
- backend-authoritative entitlement; checkout `success=true` is not payment proof;
- no secrets in APK/logs;
- 2.5 routing policy and TUN/Xray honesty.

## Work allowed now

- Path model: server + transport + security + optional entry/exit.
- Capability filtering: only transports the shipped Xray build can configure (TCP/RAW/WS/gRPC/XHTTP).
- Deterministic `PathScore`.
- Bounded fallback: preferred path → alternate transport → alternate server → Shadow route.
- Network capability cache with TTL and network-context invalidation.
- Shadow AUTO UX: `Shadow: Авто` / `Подбираем защищённый маршрут…`.
- Entry/exit validity and loop prevention; no synthetic TLS-weakening chain.
- Self-heal with threshold, hysteresis and cooldown, cancelled by disconnect/`VpnRestartGate`.
- Connection Doctor categories and real `AUTO_FIX` recovery.
- DNS bootstrap cache with TTL (not a leak path).
- IPv4/IPv6 path intelligence: dead IPv6 must not hide working IPv4.

## Not now

- operations/release infrastructure (phase 2.7);
- Autopilot (phase 2.8);
- premium Android UX polish as a phase (2.9);
- claiming `RELEASE READY` / production VPN release.

## Checkpoint protocol

Ordinary commits while implementing and while CI is red.

Do **not** put `[hotfox-review]` or `[hotfox-phase-exit]` on intermediate fix commits.

When a coherent 2.6 engineering-exit candidate is ready, make one final commit whose message contains:

`[hotfox-phase-exit]`

After green full CI, the GitHub phase-exit orchestrator dispatches exactly one AI checkpoint review.

## Phase 2.6 exit definition

The client can select a usable protected path (server + transport, and Shadow route when infrastructure supports it) with bounded cancelled fallback, without weakening TLS/REALITY or regressing 2.2–2.5 guarantees.
