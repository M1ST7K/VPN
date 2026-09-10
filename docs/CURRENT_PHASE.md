# CURRENT PHASE — RUNTIME REPAIR GATE (owner opened)

Status: **ROUND 31 P0/P1 FIXES — phase-exit candidate; host CI/APK evidence must be this HEAD; emulator/physical still NOT EXECUTED**

The owner explicitly opened `docs/HOTFOX_RUNTIME_REPAIR_EXECUTION_PROMPT.md` from HEAD `d723f65`.
That supersedes the previous no-op / deferred-runtime instruction **for this engineering/runtime repair only**.

3.1 remains **ENGINEERING COMPLETE — runtime and physical release validation deferred** as a product-phase ledger fact (round 25 `APPROVED` on `7192b04`; ledger confirmed by round 26 on `23e547a`). This gate does **not** start a new roadmap product phase and does **not** claim `RELEASE READY`.

Linked prompt: `docs/HOTFOX_RUNTIME_REPAIR_EXECUTION_PROMPT.md`
Status ledger: `docs/HOTFOX_RUNTIME_REPAIR_STATUS.md`
Previous phase spec: `docs/phases/3.1-mature-platform.md`
Final device suite (still required before RELEASE READY): `docs/phases/final-release-validation-gate.md`
Owner validation override: `.cursor/rules/22-hotfox-owner-release-validation-gate.mdc` (timing of *release* READY; this repair is an explicit owner exception for implementation/runtime evidence)

`docs/AI_REVIEW_PHASE_ID` on this branch remains `2.4` to match trusted `main`.

## Goal

Fix the real product defects that make VPN appear connected while traffic/public IP do not change:

1. P0 — native Xray sockets must not re-enter TUN;
2. P1 — `bindProcessToNetwork` false is failure;
3. P1 — CUSTOM/POLICYGROUP/PROXYCHAIN are not compared as network protocols;
4. P1 — one canonical routing snapshot and real outbound/balancer tags;
5. SHA-tied debug APK + host CI gates;
6. feasible emulator evidence when infrastructure exists;
7. iterate review until P0=0/P1=0 **and** do not stop on compile-only green if runtime evidence is still missing.

Never print or commit subscription URLs, credentials, API keys, UUIDs, or private keys.

## Inherited guarantees (still binding)

- truthful `VpnService`/TUN → HEV → SOCKS `127.0.0.1:10808` → Xray;
- `Защищено` only after path verification;
- DNS / IPv6 `::/0` fail-closed;
- AUTO persisted; manual sticky;
- backend-authoritative entitlement;
- no secrets in APK/logs;
- 2.9 SOCKS/TUN harnesses kept.

## Work allowed now

All phases in the runtime-repair prompt, including reconstruction, P0/P1 implementation, tests, CI, SHA-tied APK, and review/fix loop.

## Not now

- claiming `RELEASE READY` without the physical-device suite actually running;
- fake CONNECTED / fake ping / TLS bypass;
- exposing secrets;
- changing `docs/AI_REVIEW_PHASE_ID` on this feature branch.
