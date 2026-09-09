# CURRENT PHASE — HotFox 2.9 «VPN Core Recovery / Real Connection Fix»

Status: **IN PROGRESS** (2.8 is ENGINEERING COMPLETE — physical release validation deferred)

This is the only product phase agents should actively execute unless the owner explicitly changes the phase.

Linked detailed phase spec: `docs/HOTFOX_2_9_VPN_RECOVERY.md`
Previous phase: `docs/phases/2.8-autopilot.md`
Owner override: `.cursor/rules/21-hotfox-roadmap-2.9-vpn-recovery.mdc`
Master roadmap: `docs/HOTFOX_MASTER_ROADMAP.md`
Canonical roadmap: `docs/HOTFOX_ROADMAP.md`
Review policy: `docs/AI_REVIEW_POLICY.md`
Phase gate ledger: `docs/PHASE_GATE_STATUS.md`

## Goal

Prove the complete working pipeline:

Android application traffic → VpnService → TUN → HEV/tun2socks → local SOCKS `127.0.0.1:10808` → Xray → remote VPN server → Internet → response back to the Android application.

2.9 answers:

> Can a normal user install HotFox, import a known-working subscription, select a server, press Connect, and use Android Internet through that VPN?

Build success, TUN creation, Xray/HEV start, local SOCKS listen, or the Android VPN icon are **not** acceptance.

## Inherited guarantees (still binding)

- truthful `VpnService`/TUN → HEV → Xray path;
- `Защищено` only from canonical verified VPN session (fail-closed);
- DNS cannot silently bypass while protection is claimed;
- IPv6 routed or fail-closed;
- AUTO remains a persisted mode; manual selection stays manual;
- backend-authoritative entitlement; checkout `success=true` is not payment proof;
- no secrets in APK/logs;
- 2.5 routing policy and TUN/Xray honesty;
- 2.6 Shadow fallback never weakens TLS/REALITY;
- 2.7 channels, signed updates, drain, redaction, signing honesty;
- 2.8 Autopilot serialized intent through `VpnRestartGate`.

## Work allowed now

- Root-cause isolation from subscription → generated Xray config → SOCKS-only Xray → protect/underlying network → TUN/HEV → DNS → device traffic.
- Mandatory SOCKS-only Internet test on `127.0.0.1:10808` without VpnService/TUN/HEV.
- Independent HTTP inbound `127.0.0.1:10809` checks (do not confuse with SOCKS).
- `Utils.isXray()` / package-name capability detection must not depend on `com.v2ray.ang`.
- Runtime evidence that outbound sockets use the underlying network (no TUN feedback loop).
- Sanitized field-by-field config compare; credentials `[REDACTED]`.
- Engineering-runtime E2E in the available emulator/device environment.

## Not now

- Premium Android UX polish (now 3.0);
- Mature platform / pre-release (now 3.1);
- claiming `RELEASE READY` / production VPN release;
- fake CONNECTED, mock VPN, disabled TLS/REALITY, direct-routing test traffic, or readiness bypass.

## Checkpoint protocol

Ordinary commits while implementing and while CI is red.

Do **not** put `[hotfox-review]` or `[hotfox-phase-exit]` on intermediate fix commits.

When a coherent 2.9 engineering-exit candidate is ready (including engineering-runtime VPN E2E evidence), make one final commit whose message contains:

`[hotfox-phase-exit]`

## Phase 2.9 exit definition

Real Internet through the HotFox client path is proven in the available engineering runtime; fail-closed UI is preserved; no secrets committed; P0=0 / P1=0 on final review.

Truthful status wording after that review:

`2.9 ENGINEERING COMPLETE — real engineering-runtime VPN E2E passed; final physical release validation deferred.`
