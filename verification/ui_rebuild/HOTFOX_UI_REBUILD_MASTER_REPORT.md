# HotFox UI rebuild — master report

Base SHA: `46d0cb9c2713033997b56e747b93613d353de587`

Final SHA: (filled after docs commit; see git log on this branch)

## 18/18 status

Implemented as functional Android screens bound to existing canonical state: **18/18 code paths present**.

Full-resolution originals: **PRESENT** (`HOTFOX_18_FINAL_STYLE_REFERENCE.zip` + MANIFEST SHA-256 match).

Exact visual recreation vs SHA-256 originals: **NOT EXECUTED**. Inner-phone goldens were extracted. Emulator/device screenshots, overlays and pixel-diff statistics were not captured in this environment.

Allowed master-prompt completion wording is **not** used.

## Per-screen visual status

| id | implemented | pixel vs original |
|---|---|---|
| 01 splash | yes — fox+planet fullscreen, orange HotFox, tagline, no fake delay | NOT EXECUTED |
| 02 connect HotFox | yes — onboarding WELCOME/ACCESS, existing HTTPS + hosted checkout | NOT EXECUTED |
| 03 AUTO | yes — globe artwork only here, persisted AUTO | NOT EXECUTED |
| 04 ready | yes — server-ready artwork, system VPN prepare | NOT EXECUTED |
| 05 not protected | yes — 4-tab chrome, fox artwork, real CTA | NOT EXECUTED |
| 06 connecting | yes — stages + ring, Stop, no fake percent | NOT EXECUTED |
| 07 protected | yes — green only from `ConnectionUiMapper.CONNECTED` | NOT EXECUTED |
| 08 add connection | yes — custom sheet, not default Material dialog | NOT EXECUTED |
| 09 HTTPS subscription | yes — dedicated form, HTTPS-only, URL not logged | NOT EXECUTED |
| 10 servers | yes — AUTO row 0, real latency, filters hidden but IDs live | NOT EXECUTED |
| 11 server details | yes — real metadata only; load stays «Нет данных» | NOT EXECUTED |
| 12 subscription | yes — existing commercial/subscription bindings | NOT EXECUTED |
| 13 settings | yes — locked groups + extra Updates/ads rows for real destinations | NOT EXECUTED |
| 14 smart routing | yes — existing routing store + DNS/IPv6/LAN rows | NOT EXECUTED |
| 15 apps & rules | yes — chrome + real `PerAppProxyActivity` | NOT EXECUTED |
| 16 Autopilot | yes — enabled / Wi-Fi / cellular / trusted / pause bound to `HotfoxAutopilotStore` | NOT EXECUTED |
| 17 Shadow | yes — `HotfoxShadowStore` | NOT EXECUTED |
| 18 Always-on | yes — opens Android VPN settings, values stay «Не настроено» | NOT EXECUTED |

## Residual differences

See `UNTESTED_UI_GAPS.md` and `HOTFOX_UI_BUGS.md`. Geometry was calibrated from inner-phone goldens (tokens, 4-tab chrome, CTA 56/28, row 72dp). Without actual screenshots this cannot be scored as pixel-perfect.

## Tests / build / lint

- Overlay reconstruct (no HEV native libs) + `verification/static_check_2_2_0.py`: **PASS**
- `verification/ui_rebuild/check_locked_originals.py`: **ORIGINALS_PRESENT**
- Secret scan on reconstructed tree (static_check pattern): **PASS**
- Local `:app:assemblePlaystoreDebug` / unit tests / lint / release: **NOT EXECUTED** (Android SDK/NDK absent). Push CI is the reconstruct/build/unit/lint evidence path.
- Contract tests present: `HotfoxHttpsImportPolicyTest`, `HotfoxUiRebuildContractTest`
- Fake-state scan: connection chrome still from `ConnectionUiMapper` / `HotfoxEngineFacade`; no UI `isConnected`; MainActivity does not call `VpnSessionCoordinator`

## Core touched

See `TECHNICAL_CORE_TOUCHED.txt`. DNS saver plus existing IPv6 pref toggle from the routing screen.

## Accessibility

Canonical target remains Russian locale, font scale 1.0, dark theme. Touch targets on new rows/CTAs are ≥48dp. Route bars stay decorative (`IMPORTANT_FOR_ACCESSIBILITY_NO`). Locked visual variants for large fonts were not separately produced.

## Untested gaps

See `UNTESTED_UI_GAPS.md`.
