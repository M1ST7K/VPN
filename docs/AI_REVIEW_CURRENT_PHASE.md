# HotFox AI Review — Current Trusted Phase Scope

Current milestone: **2.9 — VPN Core Recovery / Real Connection Fix**.

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

### 2.6 — HotFox Shadow / Stealth & Resilience

Phase 2.6 HotFox Shadow / Stealth & Resilience is **ENGINEERING COMPLETE — physical release validation deferred** after checkpoint round 10 (`APPROVED`, no substantiated P0/P1) on SHA `c47a7d0` / implementation `d6fa8b6`.

Preserve its guarantees:

- path model is server + transport + security + optional entry/exit;
- unsupported transports are rejected;
- PathScore is deterministic;
- bounded fallback; Shadow AUTO does not rewrite a manual selection;
- fallback never downgrades TLS/REALITY;
- self-heal uses threshold + cooldown and is cancelled by disconnect/`VpnRestartGate`.

### 2.7 — Operations / Release Infrastructure

Phase 2.7 Operations / Release Infrastructure is **ENGINEERING COMPLETE — physical release validation deferred** after checkpoint round 11 (`APPROVED`, no substantiated P0/P1) on SHA `2d37690` / implementation `9665716`.

Preserve its guarantees:

- channels `dev`/`beta`/`stable` do not mix sandbox into stable;
- sideload updates require hash/channel/expiry and ECDSA when required; no silent install;
- AUTO drain is signed; manual selection stays sticky;
- diagnostics redaction; health/incident cannot mutate VPN protection state;
- remote flags cannot weaken TLS/REALITY or checkout honesty.

### 2.8 — HotFox Autopilot / Adaptive Protection

Phase 2.8 Autopilot is **ENGINEERING COMPLETE — physical release validation deferred** after checkpoint round 13 (`APPROVED`, no substantiated P0/P1) on SHA `33beed7` / implementation `ed23ee2`.

Preserve its guarantees:

- Autopilot start/stop goes through `VpnRestartGate` / `CoreServiceManager`;
- stale `eventGeneration` cannot override a newer decision;
- trusted vs unknown vs cellular policy is explicit; manual server is not rewritten to AUTO;
- pause is temporary and distinct from disable;
- captive portal wait/release with `Сеть требует авторизации`;
- reconnect gap is bounded; no second session controller.

## Current goal — 2.9 VPN Core Recovery / Real Connection Fix

Prove real Internet through the HotFox client path. Canonical spec: `docs/HOTFOX_2_9_VPN_RECOVERY.md`.

2.9 should answer:

> Can a normal user import a known-working subscription, connect, and use Android Internet through that VPN?

Do not expand 2.9 into 3.0 Premium UI.

## Highest-priority review targets for 2.9

1. **Isolation** — SOCKS-only `127.0.0.1:10808` without TUN/HEV is tested separately from HTTP `10809`.
2. **Package-independent Xray** — core capability must not depend on `applicationId` `com.v2ray.ang`.
3. **Generated config** — sanitized field-by-field compare; credentials `[REDACTED]`.
4. **protect / underlying network** — runtime evidence, not source presence alone.
5. **No routing loop** — Xray must not re-enter TUN → HEV → SOCKS → Xray.
6. **Fail-closed UI** — no fake CONNECTED / `Защищено`.
7. **No security weakening** — no trust-all TLS, no mock VPN, no readiness bypass.
8. **No 2.2–2.8 regression**.

## Scope discipline

Do **not** turn unimplemented 3.0 Premium UI or 3.1 Mature Platform items into P0/P1 during 2.9 review.

## Exit gate for 2.9 engineering

Then run one final `[hotfox-phase-exit]`. If P0=0 and P1=0 and engineering-runtime VPN E2E passed:

`2.9 ENGINEERING COMPLETE — real engineering-runtime VPN E2E passed; final physical release validation deferred.`

Then immediately move to `3.0 Premium Android Experience`.

## Physical-device policy

A physical handset is **NOT** required to close 2.9 if emulator/runtime E2E in the available engineering environment passed. Never claim `RELEASE READY` until the final release device gate is genuinely satisfied.
