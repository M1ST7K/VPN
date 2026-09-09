# HotFox AI Review — Current Trusted Phase Scope

Current milestone: **2.5 — Privacy Controls / Smart Routing**.

This file is trusted reviewer context from `main`. It intentionally stays short. The full canonical product roadmap lives in `docs/HOTFOX_ROADMAP.md` and is not sent in full to every checkpoint.

## Historical status

### 2.2 — Truthful Core

Phase 2.2 Truthful Core is **ENGINEERING COMPLETE** after checkpoint round 10 (`APPROVED`, no substantiated P0/P1) on SHA `3a835d8` / implementation `75839a5`.

Its truthful VPN guarantees remain binding for every later phase.

### 2.3 — Commercial Foundation

Phase 2.3 Commercial Foundation is **ENGINEERING COMPLETE** after checkpoint round 16 (`APPROVED`, no substantiated P0/P1) on SHA `afeb63a` / implementation `e6c9e3c`.

Preserve its guarantees:

- checkout redirect / `success=true` is not payment proof;
- backend verification/webhook state is authoritative;
- entitlement is backend-authoritative;
- sensitive entitlement/subscription credentials use safe Keystore-backed handling where required;
- manual HTTPS subscription path remains available according to product scope;
- commercial access returns to the same truthful VPN path.

Do not reopen 2.3 solely because physical-device validation is deferred.

### 2.4 — Smart Connection

Phase 2.4 Smart Connection is **ENGINEERING COMPLETE — physical release validation deferred** after checkpoint round 4 (`APPROVED`, no substantiated P0/P1) on SHA `f9c9ed2` / AUTO implementation `917a0e9`.

Preserve its guarantees:

- AUTO remains a persisted mode;
- manual selection remains manual;
- real health only (no fake ping);
- eligibility before ranking;
- hysteresis/stickiness and bounded failover;
- stale/old-network health cannot overwrite a newer generation.

## Current goal — 2.5 Privacy Controls / Smart Routing

Give the user real control over which traffic uses the VPN while preserving truthful routing, DNS and IPv6 guarantees.

2.5 should answer:

> Which traffic should go through HotFox, which traffic may go direct, and which traffic should be blocked?

Do not prematurely expand 2.5 into HotFox Shadow / multi-transport work reserved for 2.6.

## Highest-priority review targets for 2.5

1. **Truthful modes** — Smart / Global / include-apps / exclude-apps / Custom map to deterministic Android/Xray policy, not decorative toggles.
2. **Split tunneling** — include/exclude use `VpnService` app policy; missing/uninstalled packages must not break the session or poison remaining rules.
3. **Empty include fails closed** — empty selected-app include does not create an empty allow-list that leaks all apps off-VPN; per-app stays disabled until a usable set exists.
4. **Deterministic precedence** — `BLOCK > APP > DOMAIN > CIDR > GLOBAL MODE`; never hash-map / race order.
5. **No silent DIRECT bypass** — SMART/GLOBAL must not keep preset `geosite:cn` / private DIRECT rules that contradict the UI.
6. **LAN is explicit** — GLOBAL never bypasses LAN; other modes bypass RFC1918 only when the user enabled LAN access.
7. **DNS through VPN** — no silent system-DNS / ISP fallback while protection is claimed.
8. **IPv6** — capture remains fail-closed (`::/0` + Xray blackhole) unless IPv6 proxying is enabled; Smart routing must not create an IPv6 leak.
9. **Malformed CIDR** — never becomes `0.0.0.0/0` or `::/0` DIRECT.
10. **Reconfiguration** — routing changes while connected are generation-scoped; a stale restart must not overwrite a newer policy.
11. **Optional ads BLOCK** — geosite ads failure must not corrupt core VPN routing.
12. **No 2.2/2.3/2.4 regression** — TUN/HEV/Xray path, canonical session truth, entitlement, AUTO mode.

## Scope discipline

Do **not** turn unimplemented 2.6/2.7/2.8/2.9/3.0 roadmap items into P0/P1 during 2.5 review.

Review regressions in completed guarantees and concrete defects in implemented 2.5 routing code.

The detailed roadmap for later phases is in `docs/HOTFOX_ROADMAP.md`.

## Exit gate for 2.5 engineering

Required engineering evidence should include, as applicable:

- app include/exclude;
- missing package handling;
- domain VPN/DIRECT/BLOCK and conflicting rules;
- DNS policy;
- IPv4/IPv6 policy logic;
- LAN allow/deny;
- persistence/migration;
- generation-scoped reconfiguration;
- build/CI checks required by the repository.

Then run one final `[hotfox-phase-exit]` (orchestrator dispatches the AI review after green CI).

If:

- P0 = 0
- P1 = 0

record:

`2.5 ENGINEERING COMPLETE — physical release validation deferred.`

Then immediately move to:

`2.6 HotFox Shadow / Stealth & Resilience`

## Physical-device policy

Physical Android validation is **NOT** a blocker for closing 2.5 or for progressing through 2.6, 2.7, 2.8, 2.9 and 3.0 engineering phases.

Do not request a physical device test merely to advance the roadmap.

Physical-device acceptance is consolidated into the single `FINAL RELEASE DEVICE GATE` defined in `docs/HOTFOX_ROADMAP.md`.

Never claim physical validation as PASS unless it actually ran.
Never claim `RELEASE READY` until the final release device gate is genuinely satisfied.
