# HotFox UI rebuild — master report

Base SHA: `46d0cb9c2713033997b56e747b93613d353de587`

Final SHA: (filled after docs commit; see git log on this branch)

## 18/18 status

Implemented as functional Android screens bound to existing canonical state: **18/18 code paths present**.

Exact visual recreation vs SHA-256 originals: **BLOCKED / NOT EXECUTED**. Full-resolution files from `design/hotfox_18_final_style_reference/MANIFEST.txt` are not in the checkout. Preview copies are also absent. Pixel-perfect completion is **not** claimed.

Allowed master-prompt completion wording is **not** used, because the visual engineering gate cannot pass without originals.

## Per-screen visual status

| id | implemented | pixel vs original |
|---|---|---|
| 01 splash | yes — `HotfoxSplashActivity`, fox mark only here, no fake delay | BLOCKED |
| 02 connect HotFox | yes — onboarding WELCOME/ACCESS, existing HTTPS + hosted checkout | BLOCKED |
| 03 AUTO | yes — onboarding AUTO + AUTO wordmark | BLOCKED |
| 04 ready | yes — VPN permission / first-connection, system VPN prepare | BLOCKED |
| 05 not protected | yes — main connection, real CTA | BLOCKED |
| 06 connecting | yes — progress indeterminate, Stop, no fake percent | BLOCKED |
| 07 protected | yes — green only from `ConnectionUiMapper.CONNECTED` | BLOCKED |
| 08 add connection | yes — custom sheet, not default Material dialog | BLOCKED |
| 09 HTTPS subscription | yes — dedicated form, HTTPS-only, URL not logged | BLOCKED |
| 10 servers | yes — filters/AUTO/real latency | BLOCKED |
| 11 server details | yes — real metadata only | BLOCKED |
| 12 subscription | yes — existing commercial/subscription bindings | BLOCKED |
| 13 settings | yes — custom rows to real destinations, including ads | BLOCKED |
| 14 smart routing | yes — existing routing store + DNS/IPv6/LAN rows | BLOCKED |
| 15 apps & rules | yes — chrome + real `PerAppProxyActivity` | BLOCKED |
| 16 Autopilot | yes — existing Autopilot activity restyle | BLOCKED |
| 17 Shadow | yes — `HotfoxShadowStore` | BLOCKED |
| 18 Always-on | yes — opens Android VPN settings, no fake ownership | BLOCKED |

## Residual differences

Cannot be measured against originals. Implementation follows the textual lock (tokens, hierarchy, negative prompts, no planet/globe invention, fox only on splash).

## Tests / build / lint

- Overlay reconstruct (no HEV native libs) + `verification/static_check_2_2_0.py`: **PASS**
- `verification/ui_rebuild/check_locked_originals.py`: **BLOCKED** (18/18 originals missing)
- Secret scan on reconstructed tree: **PASS**
- Local `:app:assemblePlaystoreDebug` / unit tests / lint / release: **NOT EXECUTED** (Android SDK/NDK absent). Push CI is the reconstruct/build/unit/lint evidence path.
- New unit tests: `HotfoxHttpsImportPolicyTest`, `HotfoxUiRebuildContractTest`
- Fake-state scan: connection chrome still from `ConnectionUiMapper` / `HotfoxEngineFacade`; no UI `isConnected`; MainActivity does not call `VpnSessionCoordinator`
- Secret handling: HTTPS field cleared after import; import path does not log the URL

## Core touched

See `TECHNICAL_CORE_TOUCHED.txt`. DNS saver plus existing IPv6 pref toggle from the routing screen.

## Accessibility

Canonical target remains Russian locale, font scale 1.0, dark theme. Touch targets on new rows/CTAs are ≥48dp. Route bars stay decorative (`IMPORTANT_FOR_ACCESSIBILITY_NO`). Locked visual variants for large fonts were not separately produced.

## Untested gaps

See `UNTESTED_UI_GAPS.md`.
