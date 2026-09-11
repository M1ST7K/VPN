# HotFox UI rebuild — master report

Base SHA: `46d0cb9c2713033997b56e747b93613d353de587`

Head at this report: see git log on `cursor/hotfox-ui-pixel-lock-execution`.

## 18/18 status

Implemented as functional Android screens bound to existing canonical state: **18/18 code paths present**.

Full-resolution originals: **PRESENT** (`HOTFOX_18_FINAL_STYLE_REFERENCE.zip` blob `300c4226ef432ebba8b067ddbbcacde6bc1b707b`, 24142063 bytes, MANIFEST SHA-256 match).

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
| 10 servers | yes — AUTO row title `AUTO` + globe + orange selected stroke; `hotfox_auto_server` string kept for static_check; real latency; no invented flags | NOT EXECUTED |
| 11 server details | yes — «Детали сервера» / «Выбрать сервер»; load stays «Нет данных»; Shadow/AUTO from stores | NOT EXECUTED |
| 12 subscription | yes — Premium card + rows bound to commerce/device registry/AUTO; no hardcoded expiry | NOT EXECUTED |
| 13 settings | yes — locked groups + extra Updates/ads rows for real destinations; notification value from `NotificationManagerCompat` | NOT EXECUTED |
| 14 smart routing | yes — DNS/IPv6/LAN + reconnect bound to Autopilot `reconnectOnRestore` | NOT EXECUTED |
| 15 apps & rules | yes — chips include/exclude, search, real installed-app recycler via `AppManagerUtil`/`PerAppProxyViewModel` | NOT EXECUTED |
| 16 Autopilot | yes — orange switch; Wi-Fi/cellular/trusted/pause/captive; notifications from system | NOT EXECUTED |
| 17 Shadow | yes — `HotfoxShadowStore` + truthful extra rows; no fake «Готов» | NOT EXECUTED |
| 18 Always-on | yes — opens Android VPN settings; values stay «Не настроено» | NOT EXECUTED |

## Residual differences

See `UNTESTED_UI_GAPS.md` and `HOTFOX_UI_BUGS.md`. Geometry was calibrated from inner-phone goldens. Without actual screenshots this cannot be scored as pixel-perfect.

## Tests / build / lint

- Overlay reconstruct (no HEV native libs) + `verification/static_check_2_2_0.py`: **PASS**
- `verification/ui_rebuild/check_locked_originals.py`: **ORIGINALS_PRESENT**
- Secret scan on reconstructed tree (static_check pattern): see LOCAL_CHECKS
- Local `:app:assemblePlaystoreDebug` / unit tests / lint / release: **NOT EXECUTED** (Android SDK/NDK absent). Push CI is the reconstruct/build/unit/lint evidence path.
- Contract tests present: `HotfoxHttpsImportPolicyTest`, `HotfoxUiRebuildContractTest`
- Fake-state scan: connection chrome still from `ConnectionUiMapper` / `HotfoxEngineFacade`; no UI `isConnected`; MainActivity does not call `VpnSessionCoordinator`

## Core touched

See `TECHNICAL_CORE_TOUCHED.txt`. DNS saver, IPv6 pref toggle, Autopilot `reconnectOnRestore` toggle from routing screen.

## Accessibility

Canonical target remains Russian locale, font scale 1.0, dark theme. Touch targets on new rows/CTAs are ≥48dp. Locked visual variants for large fonts were not separately produced.

## Untested gaps

See `UNTESTED_UI_GAPS.md`.
