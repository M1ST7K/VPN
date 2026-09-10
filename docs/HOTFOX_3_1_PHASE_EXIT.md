# HotFox 3.1 — Mature Platform phase-exit checkpoint

This checkpoint requests the canonical phase-exit review after the first coherent 3.1 platform block, now synced with trusted `main` `d516623c436c4c4ed1a6b010f451917a76c2f30e` (PR #7 phase-id `2.4` + PR #6 orchestrator `expected_sha` + PR #5 per-phase cap).

- Implementation commit: `0e39e4b`. Compile fix for `HotfoxV31PersistedState.schema` default (`HotfoxStateMigration.SCHEMA`) on `ed9c996`.
- Round 20 P1 remains in tree from `844918b`: INCLUDE/EXCLUDE no longer emit non-block DOMAIN/CIDR Xray rules, so `BLOCK > APP > DOMAIN > CIDR` matches the enforceable data plane.
- Round 24 P1: out-of-range remote expiry epochs (`Long.MAX_VALUE`) fail closed to UNKNOWN/MISSING; secret-bearing `android-emulator-runner` is pinned to commit `a421e43855164a8197daf9d8d40fe71c6996bb0d` (v2.38.0). This is a **new** SHA; do not reuse `884b6b8` / `e57fed5`.
- 3.0 closure: round 19 `APPROVED` on SHA `21f341980121a49f103bd61eaa588c37f1987c8a`.
- Scope delivered in repository/CI:
  - `HotfoxEngineFacade` — UI/notification/QS re-observe process-scoped engine state; Activity is not a session owner;
  - signed control plane (`HotfoxControlPlane`) — inventory, region, weight, load, maintenance/drain, policy version; ECDSA fail-closed; last-known-good + TTL;
  - capacity-aware AUTO — load/weight plus local health; maintenance excluded from new AUTO picks; **manual stays sticky**;
  - generated device registry — register/limit/revoke/rename; no hardware fingerprinting;
  - API `minClientProtocol` fail-graceful;
  - offline policy — billing outage does not fabricate entitlement; control-plane outage does not wipe servers;
  - privacy-safe counters with secret rejection;
  - 2.x → 3.1 persisted-state migration;
  - remote-flag denylist for `vpn.fake_connected` / DNS / IPv6 / path-verify weakening.
- Inherited 2.2–3.0 VPN/commerce/AUTO/IPv6/SOCKS `10808` / NotificationManager alias guarantees were not rewritten.
- Local reconstruct/debug Android build: `NOT EXECUTED` (no Android SDK in this agent environment).
- Runtime emulator VPN E2E: `NOT EXECUTED / deferred` under the owner validation-timing override.
- Physical Android VPN E2E: `NOT EXECUTED / deferred` until `FINAL RELEASE VALIDATION GATE`.

Trusted checkpoint **round 25** returned `APPROVED` (P0=0, P1=0) on exact SHA `7192b04d441d5f2203c0efc47e37bc0f87d9cda4`.

Comment: https://github.com/M1ST7K/VPN/pull/4#issuecomment-5623226541

This checkpoint closed 3.1 as:

`3.1 ENGINEERING COMPLETE — runtime and physical release validation deferred.`

Automatic engineering progression stopped at `FINAL RELEASE VALIDATION GATE`. Do not claim `RELEASE READY`.

## Owner-requested follow-up phase-exit

The owner explicitly requested `[hotfox-phase-exit]` after the COMPLETE ledger landed. This head is that candidate: 3.1 product plus the COMPLETE / FINAL RELEASE VALIDATION GATE docs. It is **not** a new product phase and does **not** claim `RELEASE READY`.

Do not reuse `7192b04` / `be69893` — those SHAs lack this follow-up marker on the current head.
