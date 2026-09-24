# HotFox Pass 4 — Onest typography validation (2026-09-23)

Base HEAD: `7c50b30b9f8fe94222912521480b0572d4256623`
Implementation commits: `08c2f39` (static Onest 2.001 + OFL + provenance), `15edd36` (semantic roles, fitted titles).

Environment: API 34 AVD `HotFox_V51_API34_Clean`, ru-RU, density 420, software rendering.
Viewports: 360dp = 945x2100, 393dp = 1032x2292, 412dp = 1082x2402. API 24 AVD `HotFox_P4_API24` for smoke.

## Font provenance
See `docs/HOTFOX_TYPOGRAPHY_ONEST.md` (source, OFL, SHA256, mapping). License shipped as `assets/licenses/ONEST_OFL.txt`.

## Captures
- `actual/` — 01..07 at 393x873dp, fontScale 1.0.
- `matrix/` — Home 05/06/07 at 360 and 412; Home 05/06/07 at 360/393/412 with fontScale 1.15; 02/03/04 at 360 and fontScale 1.15; Shadow toggle frames.
- `comparison/` — A (pass 3, pre-Onest) vs B (pass 4, Onest) for 01..07 plus zoom crops for Home, Connect, HTTPS; contact sheet.
- `api24/` — API 24 smoke renders.

## CTA optical centering (`cta_centering.json`, `measure_cta.py`)
18/18 frames (Подключить / Отменить / Отключить x 360/393/412 x fontScale 1.0/1.15):
horizontal delta of icon+label group vs button center = 0.19–0.38dp. Limit 2dp. PASS.

## Shadow card
| frame | title | caption | ellipsis |
|---|---|---|---|
| 393 fs1.0 | Shadow | Доп. защита | none |
| 360 fs1.0 | Shadow | Защита | none |
| 360 fs1.15 | Shadow | Защита | none |
| 393 fs1.15 | Shadow | Защита | none |
| 412 fs1.0 | Shadow | Доп. защита | none |
| 412 fs1.15 | Shadow | Доп. защита | none |

"Доп. защ…" never appears. Shadow/Smart titles shrink (15sp → min 12sp) instead of ellipsizing.
Toggle persistence on real `MainActivity` (`shadow_toggle.json`): ON → OFF → recreate OFF → ON → recreate ON. PASS.

## Fox geometry
Fox region on 02/04/05 pixel-identical to pass 3 (diff 0). Fox not moved.

## Interaction wiring
`interaction_test.py` → `interaction.json`: 13/13 PASS (02 import/buy, 03 AUTO/manual, 04 add-invalid, Home connect/server/Shadow/Smart/subscription, nav Серверы/Подписка/Настройки). Frame for `02_buy_access` not saved (screencap returned empty while RenewalActivity opened); activity check PASS.

## API 24 smoke (`api24_smoke.json`)
Splash, Home (harness and real `MainActivity`), HTTPS render with Onest; no crash. uiautomator
marker check unavailable on 2 animated screens (`could not get idle state`); render verified from PNG.
Smoke only — not VPN E2E.

## Gates
- `:app:assemblePlaystoreDebug` PASS
- `:app:testPlaystoreDebugUnitTest` PASS (local 398 tests before final fit tweak; push CI run 35931538952 on `15edd36`: Unit tests, Android lint, unsigned release — success; emulator smoke job SKIPPED)
- `:app:lintPlaystoreDebug` 0 errors
- `:app:assemblePlaystoreRelease` PASS; release APK contains 4 Onest TTF (SHA256 match) and OFL asset
- `python3 verification/static_check_2_2_0.py` PASS
- `python3 verification/ui_rebuild/check_no_fixture_leak_v51.py` PASS
- `bash bootstrap/verify_hotfox_2_2_0.sh` PASS

RELEASE READY: NO.
