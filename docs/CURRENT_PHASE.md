# CURRENT PHASE — FINAL RELEASE VALIDATION GATE

Status: **WAITING — automatic engineering progression stopped**

3.1 Mature HotFox Platform / Pre-release Engineering is **ENGINEERING COMPLETE — runtime and physical release validation deferred** (checkpoint round 25 `APPROVED`, P0=0, P1=0) on SHA `7192b04d441d5f2203c0efc47e37bc0f87d9cda4`.

This is the single remaining canonical milestone. Agents must **stop automatic product-phase progression**. Do not start a new engineering phase. Do not claim `RELEASE READY`.

Previous phase: `docs/phases/3.1-mature-platform.md`
Linked gate spec: `docs/phases/final-release-validation-gate.md`
Owner validation override: `.cursor/rules/22-hotfox-owner-release-validation-gate.mdc`
Roadmap progression rule: `.cursor/rules/20-hotfox-roadmap-progression.mdc`
Engineering gates: `.cursor/rules/10-hotfox-engineering-gates.mdc`
Master roadmap: `docs/HOTFOX_MASTER_ROADMAP.md`
Canonical roadmap: `docs/HOTFOX_ROADMAP.md` (`FINAL RELEASE DEVICE GATE`)
Review policy: `docs/AI_REVIEW_POLICY.md`
Phase gate ledger: `docs/PHASE_GATE_STATUS.md`

`docs/AI_REVIEW_PHASE_ID` on this branch remains `2.4` to match trusted `main`. Do not retarget it to `3.1` from a feature-branch head.

## Goal

Hold the engineering-complete product until the owner explicitly starts the final release validation gate. That gate requires **actual** emulator and physical Android evidence of the truthful VPN path. Missing runtime/device execution is expected until the owner starts the gate; it is not a new product phase.

## Inherited guarantees (still binding)

- truthful `VpnService`/TUN → HEV → SOCKS `127.0.0.1:10808` → Xray;
- `Защищено` / notification protected / QS ACTIVE only from canonical verified VPN session;
- DNS cannot silently bypass while protection is claimed;
- IPv6 captured fail-closed with `::/0` (LAN bypass is IPv4-only at TUN; NAT64 stays in TUN);
- AUTO remains a persisted mode; manual selection stays manual;
- backend-authoritative entitlement; checkout `success=true` is not payment proof;
- no secrets in APK/logs/notifications;
- 2.5–2.8 routing, Shadow, operations and Autopilot contracts;
- 3.0 truthful headlines / error / notification / QS / onboarding / NotificationManager alias;
- 3.1 `HotfoxEngineFacade`, signed control plane, capacity-aware AUTO, device registry, offline policy, remote-flag denylist;
- 2.9 SOCKS/TUN/runtime E2E harnesses remain intact for this gate.

## 3.1 closure evidence

Trusted checkpoint **round 25** returned `APPROVED` with no substantiated P0/P1 on exact SHA `7192b04d441d5f2203c0efc47e37bc0f87d9cda4`.

Comment: https://github.com/M1ST7K/VPN/pull/4#issuecomment-5623226541

Runtime/emulator/physical VPN E2E remains `NOT EXECUTED / deferred`. `RELEASE READY` is not claimed.

## Work allowed now

- keep the engineering-complete ledger truthful;
- fix a newly discovered real P0/P1 regression of an earlier guarantee;
- describe the final gate requirements when asked.

## Not now

- starting a new product/engineering phase;
- executing emulator/runtime VPN E2E unless the owner explicitly starts this gate;
- requesting a physical Android test as a continuation blocker;
- claiming `RELEASE READY`;
- fake CONNECTED, fake ping, mock VPN, disabled TLS/REALITY, or readiness bypass;
- unsigned remote switches that weaken TLS/DNS/IPv6/checkout honesty;
- changing `docs/AI_REVIEW_PHASE_ID` on this feature branch.

## Owner validation timing override

`.cursor/rules/22-hotfox-owner-release-validation-gate.mdc` is authoritative.

After `3.1 ENGINEERING COMPLETE`, automatic roadmap progression **stops** here. Emulator/runtime VPN E2E and physical-device E2E are the content of this gate. If they have not executed, report `NOT EXECUTED / deferred`, never PASS.

Do not idle waiting for a device during 2.9/3.0/3.1 (those phases are closed). Do not run this gate unprompted.

## Final release validation definition

`RELEASE READY` may be claimed only after actual emulator **and** physical Android acceptance prove at least:

- install of the release-candidate artifact;
- known-working subscription supplied only at runtime (never committed);
- SOCKS-only Xray egress where applicable;
- production path: app traffic → `VpnService`/TUN → HEV/tun2socks → local Xray SOCKS `10808` → Xray outbound → remote VPN server → Internet;
- real HTTPS and browser/device-wide traffic;
- public IP before/during/after VPN when the environment permits;
- DNS policy with no silent bypass;
- IPv6 routed or fail-closed with no leak (`::/0`, including NAT64);
- disconnect restores normal networking;
- repeated connect/disconnect/reconnect;
- no false `CONNECTED` / `Защищено`;
- sanitized diagnostics without secrets;
- the physical-device suite in `docs/HOTFOX_ROADMAP.md` (`FINAL RELEASE DEVICE GATE`).

If any of that fails, return to a fix/rebuild/retest loop. Never convert `NOT EXECUTED` into PASS.
