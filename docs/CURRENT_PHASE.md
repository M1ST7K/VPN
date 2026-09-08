# CURRENT PHASE — HotFox 2.2 «Truthful Core»

Status: **IN PROGRESS**

This is the only product phase agents should actively execute unless the owner explicitly changes the phase.

Linked detailed phase spec: `docs/phases/2.2-truthful-core.md`
Master roadmap: `docs/HOTFOX_MASTER_ROADMAP.md`
Review policy: `docs/AI_REVIEW_POLICY.md`

## Goal

Produce an exact-sha Android APK that is not merely buildable but can truthfully prove real VPN protection on a physical Android device.

Required user journey:

`Select AUTO/manual server -> Connect -> canonical CONNECTED -> real app/browser traffic exits through VPN server -> Disconnect restores normal network`

## Current release blockers

The latest trusted checkpoint before this phase split still identified these remaining lifecycle/publication issues. Do not move to billing or feature expansion until a newer checkpoint proves them resolved.

1. **Stop ownership / stop epoch**
   - A repeated stop must not supersede the active stop worker's generation.
   - An on-time worker completion must not clear teardown before the owner finishes cleanup.
   - A late successful stop after timeout must finish the same teardown and permit reconnect safely.

2. **Generation-safe Xray shutdown during handover**
   - A delayed shutdown callback from the old core must not stop a replacement session.
   - A replacement-core failure must not be accidentally suppressed by a global temporal flag.

3. **Dev Latest publication authority**
   - One mutable `HotFox Dev Latest` artifact cannot be safely published by multiple independent branches without a repository-wide freshness rule.
   - Prefer one authoritative publisher or immutable branch/SHA artifacts.

4. **Physical-device VPN proof**
   - Still required for the exact retest SHA.
   - Emulator smoke does not satisfy this.

## Work allowed now

- VPN lifecycle/state/race fixes.
- Xray/HEV/TUN datapath verification.
- DNS/IPv6 fail-closed correctness required for 2.2 acceptance.
- loop-prevention correctness.
- AUTO correctness and server-to-config binding.
- connection/subscription UI bug fixes already required by the owner.
- CI/APK provenance required to produce a trustworthy retest artifact.
- deterministic unit/integration tests for current blockers.
- emulator UI smoke maintenance where it directly protects the current UI.

## Not now

Do not start these until the 2.2 exit gate is met:

- payment checkout;
- HotFox billing backend;
- promo codes;
- account/device-plan system;
- new ad/tracker blocking product;
- extra Android widgets/automation unrelated to 2.2;
- broad architecture rewrites;
- major visual redesign;
- 3.0 cleanup.

The future work is documented, not forgotten.

## Checkpoint protocol

Use ordinary commits while fixing code and CI.

Only when a coherent fix block is complete and applicable automated gates are green, make one final commit whose message contains:

`[hotfox-review]`

That commit requests the expensive GPT-5.6 Sol checkpoint review.

Do not put `[hotfox-review]` on every intermediate commit.

## Automated gate

Before a checkpoint, run/verify as applicable:

- debug APK build;
- unit tests;
- Android lint;
- unsigned release compile;
- HotFox static/overlay verifier;
- secret scan;
- GitHub CI;
- emulator install/navigation smoke for relevant UI changes.

## Physical-device exit gate

For the exact candidate SHA record:

- physical device model + Android version;
- external IPv4 before VPN;
- connected resolved server;
- external IPv4 after VPN;
- assert before != after;
- real HTTPS browser traffic;
- real traffic from another app where practical;
- DNS leak observation;
- IPv6 behavior (VPN egress or explicit fail-closed according to policy);
- disconnect restores normal network;
- rapid reconnect;
- Wi-Fi/cellular handover;
- permission revoke / service destruction sanity;
- sanitized diagnostic/log evidence.

## Phase 2.2 exit definition

2.2 may be marked complete only when all are true:

- CI/build/test gates green on the exact candidate SHA;
- trusted checkpoint review has no substantiated P0/P1 blockers;
- APK artifact is tied to that SHA;
- physical-device E2E is PASS;
- UI does not falsely claim protection;
- AUTO is first and actually resolves the server used by the tunnel;
- no release-blocking DNS/IPv6 bypass is observed;
- repeated connect/disconnect/reconnect and handover are stable enough for release acceptance.

When this exit gate is met, change `CURRENT_PHASE.md` to phase 2.3 and update the trusted reviewer current-phase scope on `main` before beginning commercial work.
