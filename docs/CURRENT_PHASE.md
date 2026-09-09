# CURRENT PHASE — HotFox 2.3 «Commercial Foundation»

Status: **IN PROGRESS** (round-13 **APPROVED** on `bdae55b`; sandbox/test payment E2E implementation `5be5f34`, GitHub reconstruct **PASS** `34332785857`, requesting round-14 `[hotfox-review]`)

This is the only product phase agents should actively execute unless the owner explicitly changes the phase.

Linked detailed phase spec: `docs/phases/2.3-commercial-foundation.md`
Sandbox/test payment E2E: `docs/phases/2.3-sandbox-payment-e2e.md`
Master roadmap: `docs/HOTFOX_MASTER_ROADMAP.md`
Review policy: `docs/AI_REVIEW_POLICY.md`
Phase gate ledger: `docs/PHASE_GATE_STATUS.md`

## Goal

Let a new user obtain HotFox-managed VPN access from the Subscription screen without pasting a private subscription URL, while keeping external/manual subscription support.

Do **not** claim HotFox is a production-ready release. Phase 2.2 is **engineering-complete** and **release-deferred**.

## Inherited 2.2 release gate (still blocking production)

Physical-device VPN E2E is **NOT EXECUTED**. Before any production release, an exact SHA must still prove:

- external IPv4 before VPN != after successful connect;
- real browser/app traffic through the tunnel;
- DNS/IPv6 leak or explicit fail-closed behavior;
- disconnect restores normal network;
- rapid reconnect, permission revoke, Wi-Fi/cellular handover.

Emulator UI smoke is not that proof.

## Work allowed now

- Subscription-screen commercial onboarding (NO_ACCESS / plan catalog / buy / restore / promo entry).
- HotFox backend entitlement + hosted checkout integration **without** putting provider secrets in the APK.
- Keystore-backed storage for entitlement/subscription secrets.
- Preserve AUTO, manual import, and the truthful VPN core from 2.2.
- Keep fail-closed VPN/DNS/IPv6 behavior; do not weaken it for commerce.
- Tests for order/entitlement/restore/keystore failure classes as specified in the 2.3 phase doc.

## Not now

Do not start these until 2.3’s own exit gate is met:

- extra ad/tracker product work (phase 2.5);
- extra Android widgets/automation (phase 2.6);
- 3.0 architecture cleanup;
- claiming `автопродление` unless the backend actually owns recurring billing.

## Checkpoint protocol

Use ordinary commits while implementing and while CI is red.

Only when a coherent 2.3 block is complete and applicable automated gates are green, make one final commit whose message contains:

`[hotfox-review]`

Do not put `[hotfox-review]` on every intermediate commit.

The trusted reviewer scope on `main` has been moved to 2.3 and the checkpoint cap has been raised so a 2.3 review can run.

## Phase 2.3 exit definition

2.3 engineering may be marked complete when a sandbox/test payment can prove:

`choose plan -> create order -> hosted checkout -> backend verifies payment -> entitlement issued -> credentials stored safely -> subscription/server sync -> AUTO remains selected -> VPN core still uses the truthful 2.2 path`

without pasting a paid HotFox URL and without embedding payment-provider secrets in the APK.

2.3 does **not** satisfy the 2.2 physical-device release gate.
