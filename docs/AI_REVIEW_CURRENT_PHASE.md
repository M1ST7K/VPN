# HotFox AI Review — Current Trusted Phase Scope

Current milestone: **2.6 — HotFox Shadow / Stealth & Resilience**.

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

### 2.5 — Privacy Controls / Smart Routing

Phase 2.5 Privacy Controls / Smart Routing is **ENGINEERING COMPLETE — physical release validation deferred** after checkpoint round 7 (`APPROVED`, no substantiated P0/P1) on SHA `18518d2` / implementation `72471ab`.

Preserve its guarantees:

- routing modes map to deterministic Android/Xray policy;
- EXCLUDE/INCLUDE miss stay outside TUN (DIRECT, including ads);
- captured-traffic precedence `BLOCK > APP > DOMAIN > CIDR > GLOBAL`;
- Xray rules bucketed `BLOCK / exact / suffix / CIDR`;
- routing reconnect is bound to `VpnRestartGate`;
- DNS through VPN; LAN explicit; IPv6 fail-closed unless policy says otherwise.

## Current goal — 2.6 HotFox Shadow / Stealth & Resilience

Make HotFox resilient when the default path is unavailable without weakening TLS/REALITY.

2.6 should answer:

> Which server + transport + route is the best usable protected path in the current network environment?

Do not prematurely expand 2.6 into operations/Autopilot/premium UX reserved for 2.7+.

## Highest-priority review targets for 2.6

1. **Path model** — server + transport + security + optional entry/exit, not a second VPN state machine.
2. **Capability filtering** — unsupported transports are rejected, not advertised.
3. **PathScore** — deterministic; never hash-map order.
4. **Bounded fallback** — preferred → alternate transport → alternate server → Shadow route; finite cap; cancel on disconnect/`VpnRestartGate`.
5. **Manual sticky** — Shadow AUTO does not rewrite a manual selection.
6. **Network cache** — TTL + network-context invalidation; no SSID/BSSID tracking.
7. **Shadow AUTO UX** — `Подбираем защищённый маршрут…`, not protocol dumps.
8. **Multihop validity** — no same-node loops; no TLS/REALITY downgrade.
9. **Self-heal** — threshold + cooldown; not a single noisy probe.
10. **Connection Doctor** — categories + real AUTO_FIX, no secrets.
11. **DNS bootstrap** — cached addresses with TTL; not a leak around THROUGH_VPN.
12. **IPv4/IPv6** — dead IPv6 must not hide working IPv4; IPv6 still fail-closes on TUN unless policy routes it.
13. **No 2.2–2.5 regression**.

## Scope discipline

Do **not** turn unimplemented 2.7/2.8/2.9/3.0 roadmap items into P0/P1 during 2.6 review.

## Exit gate for 2.6 engineering

Then run one final `[hotfox-phase-exit]`. If P0=0 and P1=0:

`2.6 ENGINEERING COMPLETE — physical release validation deferred.`

Then immediately move to `2.7 Operations / Release Infrastructure`.

## Physical-device policy

Physical Android validation is **NOT** a blocker for closing 2.6 or later engineering phases. Never claim `RELEASE READY` until the final release device gate is genuinely satisfied.
