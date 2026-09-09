# CURRENT PHASE — HotFox 2.3 «Commercial Foundation»

Status: **ENGINEERING COMPLETE** (trusted checkpoint **round 16** `APPROVED`, no substantiated P0/P1; implementation `e6c9e3c`, checkpoint head `afeb63a`)

Do **not** start phase 2.4. `docs/phases/2.4-smart-connection.md` requires 2.2 physical-device verification first. Physical-device VPN E2E remains **NOT EXECUTED**.

Linked detailed phase spec: `docs/phases/2.3-commercial-foundation.md`
Sandbox/test payment E2E: `docs/phases/2.3-sandbox-payment-e2e.md`
Next roadmap phase (blocked): `docs/phases/2.4-smart-connection.md`
Master roadmap: `docs/HOTFOX_MASTER_ROADMAP.md`
Review policy: `docs/AI_REVIEW_POLICY.md`
Phase gate ledger: `docs/PHASE_GATE_STATUS.md`

## Goal (closed for engineering)

A new user can obtain HotFox-managed VPN access from the Subscription screen without pasting a private subscription URL, while external/manual subscription support remains.

Do **not** claim HotFox is a production-ready release. Phase 2.2 is **engineering-complete** and **release-deferred**. Phase 2.3 is **engineering-complete** and **release-not-started**.

## Inherited 2.2 release gate (still blocking production and 2.4)

Physical-device VPN E2E is **NOT EXECUTED**. Before any production release, and before 2.4 Smart Connection engineering, an exact SHA must still prove:

- external IPv4 before VPN != after successful connect;
- real browser/app traffic through the tunnel;
- DNS/IPv6 leak or explicit fail-closed behavior;
- disconnect restores normal network;
- rapid reconnect, permission revoke, Wi-Fi/cellular handover.

Emulator UI smoke is not that proof.

## Work allowed now

- Record 2.3 engineering-complete / round-16 APPROVED in the phase ledger.
- Keep fail-closed VPN/DNS/IPv6 behavior; do not weaken it.
- Fix regressions in the truthful 2.2 VPN core or in implemented 2.3 commerce if they appear.

## Not now

Do **not** begin:

- phase 2.4 Smart Connection (blocked on 2.2 physical-device E2E);
- extra ad/tracker product work (phase 2.5);
- extra Android widgets/automation (phase 2.6);
- 3.0 architecture cleanup;
- claiming `автопродление` unless the backend actually owns recurring billing;
- claiming a production VPN release.

## Checkpoint protocol

Ordinary commits while implementing and while CI is red.

Only when a coherent block is complete and applicable automated gates are green, make one final commit whose message contains:

`[hotfox-review]`

Do not put `[hotfox-review]` on every intermediate commit.

## Phase 2.3 exit definition (met for engineering)

Sandbox/test payment proved in automated tests:

`choose plan -> create order -> hosted checkout -> backend verifies payment -> entitlement issued -> credentials stored safely -> subscription/server sync -> AUTO remains selected -> VPN core still uses the truthful 2.2 path`

without pasting a paid HotFox URL and without embedding payment-provider secrets in the APK.

A live provider sandbox hosted checkout with server-side keys remains a **manual** step. 2.3 does **not** satisfy the 2.2 physical-device release gate.
