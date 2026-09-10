# HotFox 3.0 — Premium Android Experience phase-exit checkpoint

This checkpoint requests the canonical phase-exit review after the first coherent 3.0 Premium UX block and green push CI.

- Implementation commit: `535f97f44dcbdc29046b0516a61529170debc87b`.
- Phase-start docs: `dd2af7e` (2.9 ENGINEERING COMPLETE recorded; 3.0 opened).
- Scope delivered in repository/CI:
  - truthful connection sequence `SELECTING` / `CONNECTING` / `VERIFYING` / `CONNECTED` from `VpnSessionState` only;
  - `Защищено` / notification protected / QS `ACTIVE` only when the canonical session is `CONNECTED`;
  - `PROXY_ONLY` and `ROOT_RUNNING` stay non-protected;
  - `ConnectionErrorUiMapper` keeps diagnostic codes out of the headline and strips secrets;
  - first-run onboarding: welcome → VPN permission → access → AUTO → first connection;
  - dedicated Autopilot and Routing/privacy surfaces;
  - reduced-motion gating, restrained connect haptics, decorative route-bars a11y;
  - deterministic `HotfoxPremiumViewState` fixtures instead of brittle pixel goldens.
- Inherited 2.2–2.9 VPN/commerce/AUTO/IPv6/SOCKS `10808` guarantees were not rewritten.
- Exact implementation push CI: `34459935364` — SUCCESS (payload integrity, reconstruct/build, unit/lint/release gates as configured, Publish HotFox Dev Latest).
- Runtime emulator VPN E2E: `NOT EXECUTED / deferred` under the owner validation-timing override.
- Physical Android VPN E2E: `NOT EXECUTED / deferred` until `FINAL RELEASE VALIDATION GATE`.

If approved, this checkpoint closes 3.0 as:

`3.0 ENGINEERING COMPLETE — runtime and physical release validation deferred.`

Then immediately start `3.1 Mature HotFox Platform / Pre-release Engineering`. Do not claim `RELEASE READY`.
