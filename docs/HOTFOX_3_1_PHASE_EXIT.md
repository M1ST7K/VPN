# HotFox 3.1 — Mature Platform phase-exit checkpoint

This checkpoint requests the canonical phase-exit review after the first coherent 3.1 platform block, now synced with trusted `main` `04445e11d4848b339d8c07ecf85b166781ab23d6` (PR #6 orchestrator `expected_sha` + PR #5 per-phase cap).

- Implementation commit: `0e39e4b`. Compile fix for `HotfoxV31PersistedState.schema` default (`HotfoxStateMigration.SCHEMA`) on `ed9c996`.
- Round 20 P1 remains in tree from `844918b`: INCLUDE/EXCLUDE no longer emit non-block DOMAIN/CIDR Xray rules, so `BLOCK > APP > DOMAIN > CIDR` matches the enforceable data plane.
- Round 21 did not review `844918b` (old lifetime CAP on `main`). Candidate `5b2bba1` had green CI but no review (orchestrator omitted `expected_sha`). After PR #6, this is a **new** SHA; do not treat `5b2bba1` / `844918b` / `1dda013` as this candidate.
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

If approved, this checkpoint closes 3.1 as:

`3.1 ENGINEERING COMPLETE — runtime and physical release validation deferred.`

Then stop at `FINAL RELEASE VALIDATION GATE`. Do not claim `RELEASE READY`.
