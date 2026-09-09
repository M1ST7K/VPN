# CURRENT PHASE — HotFox 2.5 «Privacy Controls / Smart Routing»

Status: **ENGINEERING-EXIT CANDIDATE** (implementation `72471ab`, full CI PASS after Round 6 P0/P1 fixes; this head requests `[hotfox-phase-exit]`)

This is the only product phase agents should actively execute unless the owner explicitly changes the phase.

Linked detailed phase spec: `docs/phases/2.5-privacy-controls.md`
Previous phase: `docs/phases/2.4-smart-connection.md`
Master roadmap: `docs/HOTFOX_MASTER_ROADMAP.md`
Canonical roadmap: `docs/HOTFOX_ROADMAP.md`
Review policy: `docs/AI_REVIEW_POLICY.md`
Phase gate ledger: `docs/PHASE_GATE_STATUS.md`

## Goal

Give the user real control over which traffic uses the VPN while preserving truthful routing, DNS and IPv6 guarantees.

2.5 answers:

> Which traffic should go through HotFox, which traffic may go direct, and which traffic should be blocked?

The UI must represent real Android/Xray routing behavior. Decorative privacy toggles are forbidden.

Do **not** claim HotFox is a production-ready release. Phases 2.2, 2.3 and 2.4 are **engineering-complete**. Physical validation on a real Android device is **NOT EXECUTED** and is consolidated into the single `FINAL RELEASE DEVICE GATE` after 3.0 — it does **not** block 2.5 engineering or the 2.5 → 2.6 transition.

## Inherited guarantees (still binding)

- truthful `VpnService`/TUN → HEV → Xray path;
- `Защищено` only from canonical verified VPN session;
- DNS cannot silently bypass while protection is claimed;
- IPv6 routed or fail-closed;
- AUTO remains a persisted mode; manual selection stays manual;
- backend-authoritative entitlement; checkout `success=true` is not payment proof;
- no secrets in APK/logs.

## Work allowed now

- Routing modes: Smart / Global / include-apps / exclude-apps / Custom.
- Android `VpnService` include/exclude split tunneling; missing packages must not poison the session.
- Domain exact/suffix and CIDR rules with VPN / DIRECT / BLOCK.
- Deterministic precedence for **captured** traffic: `BLOCK > APP-SPECIFIC > DOMAIN-SPECIFIC > CIDR > GLOBAL MODE`. EXCLUDE selected / INCLUDE miss stay outside TUN (DIRECT, including ads).
- Routing reconnect is bound to `VpnRestartGate`; explicit disconnect cancels a pending routing restart.
- Legacy `AppConfig.PREF_SMART_ROUTING_MODE` migrates when the canonical key is empty.
- Explicit LAN policy; GLOBAL never bypasses LAN.
- DNS through VPN by default; no silent ISP fallback while protected.
- Preserve 2.2 IPv6 fail-closed capture.
- Optional geosite ads/tracker BLOCK through the same rule layer.
- Truthful UI labels derived from the active snapshot.
- Persistence/migration of routing prefs; corrupt rules degrade gracefully.
- Generation-scoped reconfiguration while connected (no stale policy overwrite).

## Not now

- HotFox Shadow / multi-transport stealth (phase 2.6);
- operations/release infrastructure (phase 2.7);
- Autopilot (phase 2.8);
- premium Android UX polish as a phase (2.9);
- claiming `RELEASE READY` / production VPN release.

## Checkpoint protocol

Ordinary commits while implementing and while CI is red.

Do **not** put `[hotfox-review]` or `[hotfox-phase-exit]` on intermediate fix commits.

When a coherent 2.5 engineering-exit candidate is ready, make one final commit whose message contains:

`[hotfox-phase-exit]`

After green full CI, the GitHub phase-exit orchestrator dispatches exactly one AI checkpoint review. Do not duplicate that review manually.

## Phase 2.5 exit definition

The user can select a simple routing/privacy mode and the generated Android/Xray behavior matches it deterministically, with no hidden DNS/IPv6 bypass while protection is claimed.
