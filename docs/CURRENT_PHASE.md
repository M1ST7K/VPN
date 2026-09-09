# CURRENT PHASE — HotFox 2.7 «Operations / Release Infrastructure»

Status: **ENGINEERING-EXIT CANDIDATE** (implementation `9665716ecfc85faa4203015365a96d9d46b13193`; this head requests `[hotfox-phase-exit]`)

This is the only product phase agents should actively execute unless the owner explicitly changes the phase.

Linked detailed phase spec: `docs/phases/2.7-operations.md`
Previous phase: `docs/phases/2.6-shadow.md`
Master roadmap: `docs/HOTFOX_MASTER_ROADMAP.md`
Canonical roadmap: `docs/HOTFOX_ROADMAP.md`
Review policy: `docs/AI_REVIEW_POLICY.md`
Phase gate ledger: `docs/PHASE_GATE_STATUS.md`

## Goal

Make HotFox operable as a real service: buildable, diagnosable, updateable and safely releasable without depending on manual ad-hoc developer actions.

2.7 answers:

> Can we build, identify, update and operate HotFox without mixing channels, leaking secrets, or trusting unsigned remote config?

This phase builds release infrastructure. It does **not** perform the final physical release device gate.

Do **not** claim `RELEASE READY`. Phases 2.2–2.6 are **engineering-complete**. Physical validation on a real Android device is **NOT EXECUTED** and is consolidated into the single `FINAL RELEASE DEVICE GATE` after 3.0 — it does **not** block 2.7 engineering or the 2.7 → 2.8 transition.

## Inherited guarantees (still binding)

- truthful `VpnService`/TUN → HEV → Xray path;
- `Защищено` only from canonical verified VPN session;
- DNS cannot silently bypass while protection is claimed;
- IPv6 routed or fail-closed;
- AUTO remains a persisted mode; manual selection stays manual;
- backend-authoritative entitlement; checkout `success=true` is not payment proof;
- no secrets in APK/logs;
- 2.5 routing policy and TUN/Xray honesty;
- 2.6 Shadow fallback never weakens TLS/REALITY.

## Work allowed now

- Release channels `dev` / `beta` / `stable` with sandbox isolation.
- Traceable `versionCode` / `versionName` / git SHA / artifact label.
- CI artifact SHA-256 and unsigned-release compile remaining a gate.
- Honest failure when release signing is required but keystore env is missing.
- Signed sideload update manifest: hash, channel, downgrade, known-bad, expiry, ECDSA.
- Node drain for new AUTO picks from signed metadata; manual stays sticky.
- Privacy-safe diagnostics with channel/SHA; redaction of signing secrets.
- Service health / incident banner that cannot change VPN protection state.
- Allowlisted remote flags that cannot weaken TLS/REALITY/checkout honesty.

## Not now

- Autopilot (phase 2.8);
- premium Android UX polish as a phase (2.9);
- claiming `RELEASE READY` / production VPN release.

## Checkpoint protocol

Ordinary commits while implementing and while CI is red.

Do **not** put `[hotfox-review]` or `[hotfox-phase-exit]` on intermediate fix commits.

When a coherent 2.7 engineering-exit candidate is ready, make one final commit whose message contains:

`[hotfox-phase-exit]`

After green full CI, the GitHub phase-exit orchestrator dispatches exactly one AI checkpoint review.

## Phase 2.7 exit definition

HotFox can identify the running artifact, verify a sideload update manifest, isolate channels, redact operational secrets, drain AUTO nodes from signed metadata, and produce CI checksums — without regressing 2.2–2.6 guarantees.
