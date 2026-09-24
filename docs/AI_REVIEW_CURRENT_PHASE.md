# HotFox AI Review — Current Trusted Phase Scope

Current milestone: **2.4 — Smart Connection**. Phase 2.3 Commercial Foundation is **ENGINEERING COMPLETE**.

This file is trusted reviewer context from `main`. It intentionally stays short. The full product roadmap is not sent to OpenAI on every checkpoint.

## 2.2 status

Phase 2.2 Truthful Core is **ENGINEERING COMPLETE** after checkpoint round 10 (`APPROVED`, no substantiated P0/P1) on SHA `3a835d8` / implementation `75839a5`.

Phase 2.2 **RELEASE GATE is DEFERRED**. Physical-device VPN E2E is NOT EXECUTED. Emulator smoke is not that proof. Do not treat this approval as release acceptance.

## 2.3 status

Phase 2.3 Commercial Foundation is **ENGINEERING COMPLETE** after checkpoint round 16 (`APPROVED`, no substantiated P0/P1) on SHA `afeb63a` / implementation `e6c9e3c`.

Physical validation on a real Android device remains a **separate later gate** and does not block 2.4.

## Goal of 2.4

AUTO becomes a real reliability engine: health repository, bounded probes, deterministic scoring, hysteresis, bounded failover, truthful latency. Do not invent ping. Manual selection must not silently fail over.

## Highest-priority review targets for 2.4

1. **No fake telemetry** — unmeasured servers show `—`, never `0 ms`.
2. **AUTO does not flap** — hysteresis and explicit switch thresholds.
3. **Manual is sticky** — AUTO failover never overwrites a manual selection.
4. **Generation-safe probes** — stale/old-network results cannot overwrite a newer generation.
5. **Do not regress 2.2/2.3** — path-verified `CONNECTED`, AUTO row 0, Keystore secrets, backend-authoritative payment.
6. **Physical-device E2E** — still required before production release; not a 2.4 engineering start blocker.

## Scope discipline

Do **not** turn unimplemented 2.5/2.6/3.0 items into P0/P1. Review regressions in the truthful VPN core and defects in implemented 2.4 AUTO/health code.

## Exit gate for 2.4 engineering

AUTO survives a dead server and ordinary network handover without user intervention, without flapping, without overwriting manual mode, and without displaying invented telemetry.
