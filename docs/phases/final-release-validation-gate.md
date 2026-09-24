# FINAL RELEASE VALIDATION GATE

Canonical device-acceptance suite: `docs/HOTFOX_ROADMAP.md` heading `FINAL RELEASE DEVICE GATE`.
Owner timing override: `.cursor/rules/22-hotfox-owner-release-validation-gate.mdc`.

## Status

**WAITING — automatic engineering progression stopped.**

3.1 is **ENGINEERING COMPLETE — runtime and physical release validation deferred** (round 25 `APPROVED`, P0=0, P1=0) on SHA `7192b04d441d5f2203c0efc47e37bc0f87d9cda4`.

Runtime/emulator/physical VPN E2E: **NOT EXECUTED / deferred**.
`RELEASE READY` is **not** claimed.

Do not start a new product phase. Do not execute this gate until the owner explicitly starts it.

## Objective

Prove the shipping VPN product on an Android emulator and on real hardware before any `RELEASE READY` claim.

This is a validation gate, not a new feature phase.

## Binding constraints

- TUN → HEV → SOCKS `127.0.0.1:10808` → Xray remains the truthful path;
- `CONNECTED` / `Защищено` only from `VpnSessionCoordinator` after path verification;
- subscription URLs and secrets are runtime-only — never committed, logged, or printed;
- keep existing 2.9 SOCKS/TUN E2E harnesses; do not weaken them;
- a VPN icon alone is not proof.

## Minimum acceptance (must actually run)

- build/install the release-candidate artifact;
- SOCKS-only Xray egress where applicable;
- full production path through TUN/HEV/Xray;
- real HTTPS and browser/device-wide traffic;
- public IP before/during/after VPN when the environment permits;
- DNS policy and no silent DNS bypass;
- IPv6 routed or fail-closed (`::/0`, including NAT64; LAN bypass IPv4-only at TUN);
- disconnect restores normal networking;
- repeated connect/disconnect/reconnect;
- no false `CONNECTED` / `Защищено`;
- sanitized diagnostics without secrets;
- physical-device suite R1–R8 in `docs/HOTFOX_ROADMAP.md`.

## Explicitly not this gate’s job

- inventing a new engineering roadmap phase;
- treating missing runtime evidence as a 3.1 P0/P1 (3.1 is already closed);
- converting `NOT EXECUTED` into PASS;
- claiming `RELEASE READY` before emulator **and** physical acceptance pass.

## If the gate fails

Return to a fix / rebuild / retest loop on the same truthful architecture. Do not disable path verification, DNS/IPv6 fail-closed, or entitlement authority to obtain a green report.
