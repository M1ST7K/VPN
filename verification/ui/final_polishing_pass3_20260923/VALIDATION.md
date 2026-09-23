# HotFox — Final Polishing Pass 3 (2026-09-23)

Base HEAD: `a930707` (ancestors `48833dc`, `2ca2dbb`, `a930707` verified).
Environment: AVD `HotFox_V51_API34_Clean`, API 34, x86_64 TCG, density 420, locale ru-RU.
Captures come from the real app renderer; `HotfoxUiScreenshotHarnessActivity` (src/debug)
only opens real activities and injects visual state for 06/07. Harness frames are NOT VPN proof.

Viewports (dp → px @420dpi): 360×800 = 945×2100, 393×873 = 1032×2292, 412×915 = 1082×2402.

## P0 — CTA icon + label as one centered group

`HotfoxCenteredIconButton` (AppCompatButton) measures icon width + drawablePadding + label
text width and translates the draw canvas so the whole group sits at the optical center.
Background/ripple keep full width. No per-device offset: the shift is recomputed from the
live width, text and icon on every draw, so it cannot drift with viewport, fontScale or label.

`measure_cta.py` → `cta_centering.json` (pixel scan of rendered group vs button bounds):

| Frame | Label | Group center − button center |
|---|---|---|
| actual/05 393 | Подключить | +0.19dp |
| actual/06 393 | Отменить | +0.57dp |
| actual/07 393 | Отключить | +0.38dp |
| matrix 05/06/07 360 | — | +0.19 / +0.57 / +0.38dp |
| matrix 05/07 412 | — | +0.19 / +0.38dp |
| 05 393 fs1.15 / 360 fs1.15 | Подключить | +0.38 / +0.57dp |

Threshold ≤ 2dp: PASS in all frames (residual is glyph side-bearing, sub-pixel).
CTA bounds are identical across the three states for each viewport.
Error state uses the same `connect_action` view and label path; not captured separately
(no real failing session available) — see gaps.

## P0 — Shadow caption and toggle truth

- Caption `tv_home_shadow_caption`: single line, no ellipsize; `HotfoxHomeV13.fitShadowCaption`
  measures the full phrase «Доп. защита» and falls back to «Защита» only if it does not fit.
  360dp → «Защита»; 393/412 and fs1.15 → «Доп. защита». No «Доп. защ...» anywhere.
- Switch colors: OFF grey thumb `#7C7D8A` on `#2A2B35`; ON white thumb on orange track.
- `shadow_toggle_test.py` on real `MainActivity` (393dp) → `shadow_toggle.json`:
  initial ON (store default `isShadowAuto()=true`) → tap OFF → force-stop+relaunch OFF →
  tap ON → relaunch ON. PASS. Frames: `matrix/shadow_*`.

## P1

- Home card family: server / Shadow / Smart / Subscription share `hf_v13_card` recipe
  (card solid + 1dp `hf_v13_border_subtle`, 16dp radius); subscription outline is neutral,
  orange only as crown accent. Shadow/Smart icons 20dp, 14dp start padding.
- Fixed: at 412dp Disconnected, cards rendered with 16dp gutter while other states had 20dp
  (runtime `constrainReadingWidth` padding dropped inside a layout pass). XML now uses
  `@dimen/hf_page_gutter` and the runtime update is posted. Recaptured: rows start at 53px
  in 05/06/07 for all viewports.
- Hint «Одно касание до защиты» alpha 0.72, smaller icon. Chip/nav unchanged in geometry.
- Fox geometry frozen on Splash/Connect/Home (unit test `approvedFoxGeometryIsFrozenOnSplashConnectAndHome`).
- Atmosphere 01/02: planet alpha only (SPLASH 0.72, SUBSCRIPTION 0.64).
- AUTO 03: native `HotfoxAutoOrbitView`, refined hierarchy (orbits quieter, equator >
  meridians > latitudes, 3dp rim). No raster `hf_auto_orbits` / `hf_globe_orange`.
- HTTPS 04: utility-first — fox width 0.42 (was 0.50, −16%), planet alpha 0.44, glow 0.06.

## Build / tests (local, this candidate)

- `:app:assemblePlaystoreDebug` PASS
- `:app:testPlaystoreDebugUnitTest` PASS — 391 tests, 0 failures (+1 gutter test after, 9/9 in class)
- `:app:lintPlaystoreDebug` PASS (0 errors)
- `:app:assemblePlaystoreRelease` PASS (unsigned compile gate)
- `static_check_2_2_0.py` PASS, `check_no_fixture_leak_v51.py` PASS, `verify_hotfox_2_2_0.sh` PASS

## Interaction (`interaction_test.py` → `interaction.json`, frames in `interaction/`)

13/13 PASS: 02 import → `HotfoxHttpsImportActivity`; 02 buy → `RenewalActivity`;
03 AUTO → «Всё готово»; 03 manual → real Servers (`MainActivity`); 04 Add invalid stays, no crash;
Home Connect (no servers) → «Добавить подключение» sheet; server card → Servers list with AUTO row;
Shadow row → `HotfoxShadowActivity`; Smart → `HotfoxRoutingPrivacyActivity`; Subscription card and
nav Серверы / Подписка / Настройки switch destinations. VPN permission was never granted.

## Files

- `actual/` 01–07 at 393dp fs1.0
- `matrix/` 360/412 Home states, fs1.15 (02, 03, 05, 05@360), compact AUTO/HTTPS, Shadow OFF/ON/recreate
- `comparison/` pass 2 (`final_visual_consistency_20260923`) vs pass 3 per screen + contact sheet
