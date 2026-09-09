# CURRENT PHASE — HotFox 2.4 «Smart Connection»

Status: **IN PROGRESS** (round-2 P1s: bind phase-exit review to green SHA and gate continuation on `APPROVED`; 2.4 AUTO P1s remain in `917a0e9`)

This is the only product phase agents should actively execute unless the owner explicitly changes the phase.

Linked detailed phase spec: `docs/phases/2.4-smart-connection.md`
Previous phase: `docs/phases/2.3-commercial-foundation.md`
Master roadmap: `docs/HOTFOX_MASTER_ROADMAP.md`
Review policy: `docs/AI_REVIEW_POLICY.md`
Phase gate ledger: `docs/PHASE_GATE_STATUS.md`

## Goal

Turn `Авто-выбор сервера` from a lowest-cached-ping selector into a real connection reliability engine: health repository, bounded probes, deterministic scoring, hysteresis, bounded AUTO failover, truthful latency UI.

Do **not** claim HotFox is a production-ready release. Phase 2.2 is **engineering-complete** and **release-deferred**. Phase 2.3 is **engineering-complete**. Physical validation on a real Android device is **NOT EXECUTED** and is consolidated into the single `FINAL RELEASE DEVICE GATE` after 3.0 — it does **not** block 2.4 engineering or the 2.4 → 2.5 transition.

## Inherited 2.2 release gate (still blocking `RELEASE READY` only)

Physical-device VPN E2E is **NOT EXECUTED**. It is **not** a 2.4 engineering-phase blocker. Before any production `RELEASE READY` claim, an exact SHA must still prove:

- external IPv4 before VPN != after successful connect;
- real browser/app traffic through the tunnel;
- DNS/IPv6 leak or explicit fail-closed behavior;
- disconnect restores normal network;
- rapid reconnect, permission revoke, Wi-Fi/cellular handover.

Emulator UI smoke is not that proof.

## Work allowed now

- `ServerHealthRepository` and freshness/TTL rules.
- Bounded parallel probes with cancel, timeouts, backoff; no fake ping (`—` when unmeasured).
- Deterministic AUTO score (latency/jitter/failure/staleness). Not “AI”.
- Hysteresis so AUTO does not flap on a few milliseconds.
- Bounded AUTO failover; manual selection never silently switches.
- Generation-safe handover: stale probes cannot overwrite a newer network generation.
- Network-context invalidation so Wi-Fi latency is not reused on cellular.
- Last-good AUTO memory for cold-start fallback.
- Eligibility filtering before ranking (invalid/entitlement-blocked entries must not poison the set).
- Truthful resolved AUTO target (`Подбираем сервер…` then concrete city). `Защищено` still comes only from the canonical VPN session.
- Editorial server-list rows; AUTO remains row 0.
- Unit tests listed in `docs/phases/2.4-smart-connection.md`.

## Not now

- extra ad/tracker product work (phase 2.5);
- extra Android widgets/automation (phase 2.6);
- 3.0 architecture cleanup;
- claiming `автопродление` unless the backend actually owns recurring billing;
- claiming `RELEASE READY` / production VPN release.

## Checkpoint protocol

Ordinary commits while implementing and while CI is red.

Do **not** put `[hotfox-review]` or `[hotfox-phase-exit]` on intermediate fix commits.

When a coherent 2.4 engineering-exit candidate is ready, make one final commit whose message contains:

`[hotfox-phase-exit]`

After green full CI, the GitHub phase-exit orchestrator dispatches exactly one AI checkpoint review. Do not duplicate that review manually.

## Phase 2.4 exit definition

AUTO should survive a dead server and ordinary network handover without user intervention, without flapping, without overwriting manual mode, and without displaying invented telemetry.
