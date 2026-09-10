# HotFox AI Review — Current Trusted Phase Scope

Current milestone: **FINAL RELEASE VALIDATION GATE**.

3.1 is historical **ENGINEERING COMPLETE — runtime and physical release validation deferred** (round 25 `APPROVED`, P0=0, P1=0) on SHA `7192b04d441d5f2203c0efc47e37bc0f87d9cda4`. Automatic product-phase progression is stopped. Do not claim `RELEASE READY`.

This file is trusted reviewer context from `main`/owner policy. It intentionally stays short. The full canonical product roadmap lives in `docs/HOTFOX_ROADMAP.md`.

Latest owner validation timing override: `.cursor/rules/22-hotfox-owner-release-validation-gate.mdc`.

If older 2.9 docs require emulator/runtime E2E as an intermediate phase blocker, the latest owner override wins for **validation timing only**. It does not weaken production VPN correctness, truthful state, security, DNS/IPv6 safety, secret handling, or final release acceptance.

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

Do not reopen 2.3 solely because runtime/physical release validation is deferred.

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
- DNS through VPN; LAN IPv4 explicit; IPv6 fail-closed `::/0` including NAT64 (LAN bypass does not omit IPv6 Internet prefixes).

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

### 2.9 — VPN Core Recovery / Real Connection Fix

Phase 2.9 VPN Core Recovery is **ENGINEERING COMPLETE — runtime and physical release validation deferred** after checkpoint round 17 (`APPROVED`, no substantiated P0/P1) on SHA `4524207` / IPv6 fix `f8de16e` / isolation `dacfe38`.

Preserve its guarantees:

- SOCKS `10808` is isolated from HTTP `10809`; health probes SOCKS HTTPS;
- `Utils.isXray()` / Xray capability is package-independent;
- generated Reality/network/security drift is fail-closed;
- IPv6 TUN capture is `::/0` including NAT64; LAN bypass is IPv4-only at TUN;
- runtime E2E harnesses remain for the final release validation gate;
- no fake CONNECTED.

Do not reopen 2.9 solely because runtime/physical release validation is deferred.

### 3.0 — Premium Android Experience

Phase 3.0 Premium Android Experience is **ENGINEERING COMPLETE — runtime and physical release validation deferred** after checkpoint round 19 (`APPROVED`, no substantiated P0/P1) on SHA `21f341980121a49f103bd61eaa588c37f1987c8a`.

Preserve its guarantees:

- `Защищено` / notification protected / QS ACTIVE only from canonical `VpnSessionState.CONNECTED`;
- SELECTING / CONNECTING / VERIFYING remain distinct from CONNECTED;
- `ConnectionErrorUiMapper` keeps diagnostic codes out of the headline and strips secrets;
- first-run onboarding around real permission/access/AUTO/connect;
- `NotificationManager` aliases `android.app.NotificationManager` as `AndroidNotificationManager`;
- Autopilot/Routing dedicated surfaces; reduced-motion; decorative route-bars.

Do not reopen 3.0 solely because runtime/physical release validation is deferred.

### 3.1 — Mature HotFox Platform / Pre-release Engineering

Phase 3.1 Mature HotFox Platform is **ENGINEERING COMPLETE — runtime and physical release validation deferred** after checkpoint round 25 (`APPROVED`, no substantiated P0/P1) on SHA `7192b04d441d5f2203c0efc47e37bc0f87d9cda4`.

Preserve its guarantees:

- UI/notification/QS observe `HotfoxEngineFacade`; Activity is not a second session owner;
- signed ECDSA control-plane inventory/capacity/maintenance/weight/region/policy; invalid signature fail-closed; last-known-good + TTL;
- capacity-aware AUTO uses load/weight plus local health; **manual stays sticky**;
- generated scoped device IDs only; no hardware fingerprinting;
- billing outage does not fabricate entitlement; control-plane outage does not wipe servers;
- remote flags cannot fake CONNECTED or weaken TLS/DNS/IPv6/path verification;
- INCLUDE/EXCLUDE captured-app modes do not emit non-block DOMAIN/CIDR Xray rules;
- out-of-range remote expiry epochs fail closed to UNKNOWN/MISSING.

Do not reopen 3.1 solely because runtime/physical release validation is deferred.

## Current goal — FINAL RELEASE VALIDATION GATE

Canonical suite: `docs/HOTFOX_ROADMAP.md` (`FINAL RELEASE DEVICE GATE`) and `docs/phases/final-release-validation-gate.md`.

This is a validation stop, not a new product phase. Do not start another engineering roadmap phase. Do not execute emulator/physical E2E until the owner explicitly starts the gate.

## Highest-priority review targets after 3.1 close

1. **No 2.2–3.1 regression** of VPN path, DNS/IPv6, entitlement, NotificationManager alias, facade/control-plane, or E2E harnesses.
2. **No fabricated RELEASE READY** — missing emulator/runtime/physical execution is `NOT EXECUTED / deferred`, never PASS.
3. **Secrets** — subscription URLs stay runtime-only.
4. **Truthful state** — no false `CONNECTED` / `Защищено`.

## Scope discipline

Do **not** treat unimplemented final-release device-gate execution as a 3.1 P0/P1. That engineering gate is already closed.

Missing emulator/runtime/physical execution by itself is **not** a product P0/P1 under the latest owner validation timing override until the owner starts this gate.

Optional account sync and R8/process-split remain out of scope unless product-justified with evidence.

## Exit gate for 3.1 engineering

Closed. Do not request another `[hotfox-phase-exit]` merely to restate 3.1 completion.

`docs/AI_REVIEW_PHASE_ID` on trusted `main` remains `2.4`. Do not retarget it from a feature-branch head.

## Final release validation policy

Runtime validation is consolidated into this gate after 3.1 engineering completion.

Before `RELEASE READY`, actual emulator and final physical-device acceptance must prove the real VPN path, real HTTPS/browser traffic, public IP behavior where available, DNS safety, IPv6 route/fail-closed behavior, reconnect cycles, teardown, and no false protected state.

If final runtime validation fails, the project returns to the fix/rebuild/retest loop. Never claim `RELEASE READY` until that final gate genuinely passes.
