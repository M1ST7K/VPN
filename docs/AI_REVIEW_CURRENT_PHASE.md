# HotFox AI Review — Current Trusted Phase Scope

Current milestone: **2.2 — Truthful Core**.

This file is trusted reviewer context from `main`. It intentionally stays short. The full product roadmap is not sent to OpenAI on every checkpoint.

## Goal

Before adding commercial/billing features, prove that HotFox is a truthful, stable Android VPN on the exact build under review.

The production path remains conceptually:

`Android apps -> VpnService/TUN -> HEV/tun2socks -> local SOCKS -> Xray -> supported VPN outbound -> server -> Internet`

## Current release blockers

Treat these as the highest-priority review targets until resolved by evidence:

1. **Stop ownership/finalization** — repeated stop, timeout and late completion must not invalidate the live stop generation, clear teardown too early, or permanently block reconnect.
2. **Generation-safe Xray shutdown handling during handover** — delayed shutdown callbacks from an old core must not kill a replacement session; replacement-core failures must not be globally suppressed.
3. **Unexpected service/process lifecycle** — revoke/destroy/restart must not leave stale protected state, HEV/Xray/TUN resources or process network binding.
4. **Real datapath truthfulness** — `CONNECTED`/`Защищено` requires the strongest bounded in-process path verification available; component liveness alone is insufficient.
5. **DNS/IPv6 fail-closed behavior** — no silent bypass while protection is claimed.
6. **AUTO integrity** — AUTO remains row 0 and a persisted mode distinct from a manual server; resolved target must match the config actually used.
7. **Build/artifact provenance** — a mutable Dev Latest artifact must not be overwritten by an older cross-branch run or point at a mismatched tag/SHA.
8. **Physical-device acceptance** — automated/emulator evidence is not a substitute for external-IP, DNS and IPv6 checks on a real Android device.

## Scope discipline

For milestone 2.2, do **not** turn future roadmap items into P0/P1 merely because they are not implemented yet. Billing, promo codes, account systems, advanced privacy features, extra Android integrations and 3.0 architecture are future milestones.

Review only regressions/defects in current implemented or phase-required functionality.

## Exit gate for 2.2

The milestone is ready to close only when:

- applicable build/tests/lint/static checks are green;
- trusted checkpoint review has no substantiated P0/P1 blockers;
- an installable APK is tied to the exact SHA;
- physical-device E2E proves real traffic through the VPN, external IP change, and acceptable DNS/IPv6 behavior;
- rapid reconnect, permission revocation and Wi-Fi/cellular handover are stable enough for release acceptance.
