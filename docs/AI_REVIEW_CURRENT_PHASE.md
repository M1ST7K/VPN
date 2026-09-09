# HotFox AI Review — Current Trusted Phase Scope

Current milestone: **2.7 — Operations / Release Infrastructure**.

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

## Current goal — 2.7 Operations / Release Infrastructure

Make HotFox buildable, diagnosable, updateable and safely releasable.

2.7 should answer:

> Can we identify, update and operate HotFox without mixing channels, leaking secrets, or trusting unsigned remote config?

Do not prematurely expand 2.7 into Autopilot/premium UX reserved for 2.8+.

## Highest-priority review targets for 2.7

1. **Channels** — `dev`/`beta`/`stable`; stable cannot mix sandbox commerce.
2. **Provenance** — versionCode/versionName/git SHA/channel on the artifact and in diagnostics.
3. **Signing honesty** — missing required keystore fails the build; no silent unsigned-as-signed.
4. **Update manifest** — HTTPS URL, SHA-256, channel match, expiry, ECDSA when required, reject downgrade/known-bad/hash mismatch.
5. **No silent install** — AVAILABLE is a user-visible state, not an auto-install.
6. **Node drain** — signed metadata only; AUTO excludes drained nodes; manual stays sticky.
7. **Redaction** — diagnostics/logs never emit subscription URLs, tokens, UUIDs, signing passwords.
8. **Incident banner** — cannot change canonical VPN protection state.
9. **Remote flags** — allowlisted keys; cannot disable TLS/REALITY or trust checkout `success=true`.
10. **No 2.2–2.6 regression**.

## Scope discipline

Do **not** turn unimplemented 2.8/2.9/3.0 roadmap items into P0/P1 during 2.7 review.

## Exit gate for 2.7 engineering

Then run one final `[hotfox-phase-exit]`. If P0=0 and P1=0:

`2.7 ENGINEERING COMPLETE — physical release validation deferred.`

Then immediately move to `2.8 HotFox Autopilot / Adaptive Protection`.

## Physical-device policy

Physical Android validation is **NOT** a blocker for closing 2.7 or later engineering phases. Never claim `RELEASE READY` until the final release device gate is genuinely satisfied.
