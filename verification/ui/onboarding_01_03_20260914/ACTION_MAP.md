# HotFox 01–03 action map — ONBOARDING-01-03-20260914

Production layouts: `activity_hotfox_splash.xml`, `activity_hotfox_onboarding.xml`.
Controller: `HotfoxOnboardingActivity` + `HotfoxOnboardingFlow`.
Home V13 (`activity_main.xml`) is unchanged in appearance.

| Screen | Visible control | Destination | Notes |
| --- | --- | --- | --- |
| 01 Splash | none | `HotfoxOnboardingActivity` if `HotfoxOnboardingStore.shouldPrompt()`, else `MainActivity` with `EXTRA_SKIP_ONBOARDING` | One `startActivity` via `handedOff`. No 2–3s sleep. Debug harness `holdSplash` only. Does not mark onboarding complete. |
| 02 Primary «У меня уже есть подписка» | `HotfoxHttpsImportActivity` via `ActivityResult` | Does **not** request VPN. Stays on 02 until `hasAccess`. Success → AUTO. Cancel/invalid → remain on 02, not complete. If access already exists → `NEXT` → AUTO. |
| 02 Secondary «Купить доступ» | `RenewalActivity` via `ActivityResult` | Existing commerce/renewal screen. Return is **not** payment proof. No silent no-op. |
| 03 Primary «Использовать AUTO» | `HotfoxServerSelection.setAutoMode(true)` then `KEEP_AUTO` | First-class AUTO. Next is VPN permission (04) if missing, else FIRST_CONNECTION. No fake VPN start. |
| 03 Secondary «Выбрать вручную» | `MainActivity` `SECTION_SERVERS` + `EXTRA_ONBOARDING_SERVER_PICK` | Real server list. Does **not** `setAutoMode(false)` before a commit. `RESULT_OK` → `MANUAL_SERVERS`. Cancel (`RESULT_CANCELED` / back) keeps previous mode. |
| 04 VPN | `VpnService.prepare` | Only after AUTO/manual. Denial stays on permission step. |

`hasAccess` = `CommercePreferences.accessOrigin() != ORIGIN_NONE || HotfoxServerSelection.firstUsableGuid() != null`.
WELCOME and ACCESS share the 02 layout; subscription CTA no longer diverges into VPN vs import.
