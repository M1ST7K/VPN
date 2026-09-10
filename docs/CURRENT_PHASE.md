# CURRENT PHASE — HotFox 2.9 «VPN Core Recovery / Real Connection Fix»

Status: **ENGINEERING-EXIT CANDIDATE** (implementation `dacfe386260b204a562b231ba6f31d81ed5d01e1`; round-15 P1 `--channel=3` fix `37115cd`; this head requests `[hotfox-phase-exit]`)

This is the only product phase agents should actively execute unless the owner explicitly changes the phase.

Linked detailed phase spec: `docs/HOTFOX_2_9_VPN_RECOVERY.md`
Previous phase: `docs/phases/2.8-autopilot.md`
Owner roadmap override: `.cursor/rules/21-hotfox-roadmap-2.9-vpn-recovery.mdc`
Latest owner validation override: `.cursor/rules/22-hotfox-owner-release-validation-gate.mdc`
Master roadmap: `docs/HOTFOX_MASTER_ROADMAP.md`
Canonical roadmap: `docs/HOTFOX_ROADMAP.md`
Review policy: `docs/AI_REVIEW_POLICY.md`
Phase gate ledger: `docs/PHASE_GATE_STATUS.md`

## Goal

Implement and harden the complete intended production pipeline:

Android application traffic → VpnService → TUN → HEV/tun2socks → local SOCKS `127.0.0.1:10808` → Xray → remote VPN server → Internet → response back to the Android application.

2.9 exists to correct the known HotFox-side datapath defects and to leave the codebase ready for final runtime validation.

Build success, TUN creation, Xray/HEV start, local SOCKS listen, or the Android VPN icon still do **not** prove runtime VPN success. However, under the latest owner decision, missing emulator/runtime E2E is no longer an intermediate blocker for engineering-roadmap progression.

## Inherited guarantees (still binding)

- truthful `VpnService`/TUN → HEV → Xray architecture;
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
- SOCKS-only diagnostic path on `127.0.0.1:10808` without VpnService/TUN/HEV.
- Independent HTTP inbound `127.0.0.1:10809` checks (do not confuse with SOCKS).
- `Utils.isXray()` / package-name capability detection must not depend on `com.v2ray.ang`.
- Source/runtime instrumentation required to make later final validation observable and diagnosable.
- Sanitized field-by-field config compare; credentials `[REDACTED]`.
- Unit/integration/static/CI coverage for production-path correctness.
- Keep engineering-runtime E2E harnesses intact for the final release validation gate.

## Not now

- claiming `RELEASE READY` / production VPN release;
- fake CONNECTED, mock VPN, disabled TLS/REALITY, direct-routing test traffic, or readiness bypass;
- committing or logging the private subscription URL/credentials.

Premium Android UX remains the next roadmap phase (3.0), followed by 3.1 Mature Platform / pre-release engineering.

## Owner validation timing override

`.cursor/rules/22-hotfox-owner-release-validation-gate.mdc` is authoritative for validation timing.

For 2.9, emulator/runtime VPN E2E and physical-device E2E are deferred to the single final release validation gate after 3.1 engineering completion.

If runtime E2E has not executed, it must be reported as `NOT EXECUTED / deferred`, never as PASS.

## Checkpoint protocol

Ordinary commits while implementing and while CI is red.

Do **not** put `[hotfox-review]` or `[hotfox-phase-exit]` on intermediate fix commits.

When a coherent repository-side 2.9 engineering-exit candidate is ready and required build/unit/lint/static/integration CI is green, make one final commit whose message contains:

`[hotfox-phase-exit]`

Then run the final AI review.

## Phase 2.9 exit definition

2.9 may close when:

- the intended production VPN architecture and 2.9 fixes are implemented;
- diagnostic/final-E2E harnesses remain available;
- required CI/build/unit/integration/static checks pass;
- no secrets are committed/logged;
- fail-closed truth/security guarantees are preserved;
- final review has P0=0 / P1=0.

Missing emulator/runtime/physical validation by itself is not a P0/P1 and does not block 2.9 under the latest owner override.

Truthful status wording after approval:

`2.9 ENGINEERING COMPLETE — runtime and physical release validation deferred.`

Then immediately start `3.0 Premium Android Experience`.
