# HotFox AI Review — Current Trusted Phase Scope

Current milestone: **2.4 — Smart Connection**.

This file is trusted reviewer context from `main`. It intentionally stays short. The full canonical product roadmap lives in `docs/HOTFOX_ROADMAP.md` and is not sent in full to every checkpoint.

## Historical status

### 2.2 — Truthful Core

Phase 2.2 Truthful Core is **ENGINEERING COMPLETE** after checkpoint round 10 (`APPROVED`, no substantiated P0/P1) on SHA `3a835d8` / implementation `75839a5`.

Its truthful VPN guarantees remain binding for every later phase.

### 2.3 — Commercial Foundation

Phase 2.3 Commercial Foundation is treated as the completed predecessor to the current Smart Connection work once its engineering exit is satisfied by the repository evidence/final review.

Preserve its guarantees:

- checkout redirect / `success=true` is not payment proof;
- backend verification/webhook state is authoritative;
- entitlement is backend-authoritative;
- sensitive entitlement/subscription credentials use safe Keystore-backed handling where required;
- manual HTTPS subscription path remains available according to product scope;
- commercial access returns to the same truthful VPN path.

Do not reopen 2.3 solely because physical-device validation is deferred.

## Current goal — 2.4 Smart Connection

Turn `Авто-выбор сервера` into a real first-class intelligent server-selection mode while preserving manual server intent and the truthful 2.2 VPN path.

2.4 should answer:

> Which eligible server is the best server to use right now?

Do not prematurely expand 2.4 into the full transport/path/stealth work reserved for 2.6 HotFox Shadow.

## Highest-priority review targets for 2.4

1. **AUTO remains a persisted mode** — the concrete server resolved for one session must not overwrite the user's persisted AUTO choice.
2. **Manual selection remains manual** — AUTO must not silently replace an explicitly selected server.
3. **Real health only** — ping/health values must be measured or explicitly unavailable; no fake production pings/health.
4. **Eligibility before ranking** — entitlement, manifest/config validity and supported server data must filter candidates before AUTO selection.
5. **Deterministic scoring** — health/latency/failure/freshness policy should be explicit and testable, not UI magic.
6. **Hysteresis/stickiness** — a healthy active server must not flap because another server is only trivially faster.
7. **Bounded probing/failover** — concurrency, retries and failover must be bounded, serialized and cancellable.
8. **Stale async protection** — obsolete probes/selection work must not overwrite a newer network/session decision.
9. **Network-change handling** — Wi-Fi/cellular/loss/restore must not blindly reuse invalid stale health state or create duplicate sessions.
10. **Truthful resolved target** — UI should be able to show the concrete AUTO target, but only the canonical VPN session may emit `CONNECTED` / `Защищено`.
11. **No 2.2 regression** — DNS, IPv6, startup ordering, TUN/HEV/Xray cleanup and canonical session truth remain release-critical.
12. **No 2.3 regression** — Smart Connection must not bypass entitlement, expose secrets or break the manual subscription path.

## Scope discipline

Do **not** turn unimplemented 2.5/2.6/2.7/2.8/2.9/3.0 roadmap items into P0/P1 during 2.4 review.

Review regressions in completed guarantees and concrete defects in implemented 2.4 Smart Connection code.

The detailed roadmap for later phases is in `docs/HOTFOX_ROADMAP.md`.

## Exit gate for 2.4 engineering

Required engineering evidence should include, as applicable:

- candidate filtering;
- real bounded health/probe logic;
- deterministic server scoring;
- cold-start fallback;
- hysteresis/stickiness;
- manual/AUTO separation;
- bounded server failover;
- network-context handling;
- safe diagnostics;
- automated/unit/integration coverage for important pure/race-prone logic;
- build/CI checks required by the repository.

Then run one final `[hotfox-review]`.

If:

- P0 = 0
- P1 = 0

record:

`2.4 ENGINEERING COMPLETE — physical release validation deferred.`

Then immediately move to:

`2.5 Privacy Controls / Smart Routing`

## Physical-device policy

Physical Android validation is **NOT** a blocker for closing 2.4 or for progressing through 2.5, 2.6, 2.7, 2.8, 2.9 and 3.0 engineering phases.

Do not request a physical device test merely to advance the roadmap.

Physical-device acceptance is consolidated into the single `FINAL RELEASE DEVICE GATE` defined in `docs/HOTFOX_ROADMAP.md`.

Never claim physical validation as PASS unless it actually ran.
Never claim `RELEASE READY` until the final release device gate is genuinely satisfied.
