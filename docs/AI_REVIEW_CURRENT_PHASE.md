# HotFox AI Review — Current Trusted Phase Scope

Current milestone: **2.3 — Commercial Foundation** is **ENGINEERING COMPLETE**. Phase **2.4 Smart Connection** is **NOT STARTED** because 2.2 physical-device E2E is **NOT EXECUTED**.

This file is trusted reviewer context from `main`. It intentionally stays short. The full product roadmap is not sent to OpenAI on every checkpoint.

## 2.2 status

Phase 2.2 Truthful Core is **ENGINEERING COMPLETE** after checkpoint round 10 (`APPROVED`, no substantiated P0/P1) on SHA `3a835d8` / implementation `75839a5`.

Phase 2.2 **RELEASE GATE is DEFERRED**. Physical-device VPN E2E is NOT EXECUTED. Emulator smoke is not that proof. Do not treat this approval as release acceptance.

## 2.3 status

Phase 2.3 Commercial Foundation is **ENGINEERING COMPLETE** after checkpoint round 16 (`APPROVED`, no substantiated P0/P1) on SHA `afeb63a` / implementation `e6c9e3c`.

2.3 **RELEASE GATE is NOT STARTED**. A sandbox CI fixture is not a live provider sandbox and is not physical-device VPN E2E.

## Goal that closed 2.3 engineering

Allow HotFox-managed access from the Subscription screen without pasting a private subscription URL, while keeping external/manual subscriptions working if the commercial backend is down.

## Highest-priority review targets

1. **No secrets in the APK** — payment-provider private keys, HotFox backend admin secrets, and raw user subscription URLs must not be embedded.
2. **Checkout is not entitlement** — browser `success=true` / return URLs are not payment proof; backend webhook/verification is authoritative.
3. **Keystore-backed secrets** — entitlement/subscription tokens must not live in plain MMKV.
4. **Manual path remains** — if HotFox billing is down, imported HTTPS subscriptions still work.
5. **Do not regress 2.2 VPN truth** — `CONNECTED`/`Защищено` still requires the canonical path-verified session; AUTO stays row 0; DNS/IPv6 remain fail-closed where policy requires it.
6. **Physical-device E2E** — still required before production release and before 2.4 engineering; not satisfied by 2.3.

## Scope discipline

Do **not** turn unimplemented 2.4/2.5/2.6/3.0 items into P0/P1. Review regressions in the truthful VPN core and defects in implemented 2.3 commerce/entitlement code.

## Exit gate for 2.3 engineering (met)

Sandbox/test: plan -> order -> hosted checkout -> backend-verified payment -> entitlement -> safe credential storage -> subscription sync -> AUTO preserved -> VPN still uses the 2.2 path.
