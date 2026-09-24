# UI regression smoke

| Item | Status | Evidence |
|---|---|---|
| App launch on emulator API 34 (candidate `08b35ff`) | PASS (launch only) | `evidence/emulator_api34_onboarding_launch_08b35ff.png` — `HotfoxOnboardingActivity` "Connect HotFox", fox hero, primary CTA, "Buy access"; no clipping visible |
| Splash | NOT EXECUTED | — |
| Connect HotFox screen | PASS (static render only, `08b35ff`) | screenshot above |
| AUTO / HTTPS import via UI | NOT EXECUTED | — |
| Home Disconnected / Connecting / Connected | NOT EXECUTED | — |
| Screens 08–18 navigation | NOT EXECUTED | — |
| Onest loads | NOT EXECUTED as a dedicated check (typeface visually consistent in screenshot) | — |

Remaining items are blocked by the emulator infrastructure failure (`EMULATOR_E2E.md`).
The candidate fix touches no UI code. CI `emulator-ui-smoke` is manual-only and was
skipped on push runs; it is **not** counted as PASS.
