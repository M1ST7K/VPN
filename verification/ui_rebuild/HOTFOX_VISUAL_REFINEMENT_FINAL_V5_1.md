# HotFox V5.1 — final visual recovery report

Status: **CONTAINMENT + RENDERER + 18/18 CAPTURES**.  
Visual/pixel PASS: **0/18**. Art-director PASS: **0/18**.  
RELEASE READY: **NO**. Merge: **NO**.

Head: see git `cursor/hotfox-ui-pixel-lock-rebuild`.

## P0 containment

BEFORE: `verification/ui_rebuild/V5_1_FIXTURE_LEAK_BEFORE.txt` — FAIL (HotfoxUiVisualOverride and fixture literals in `src/main`).

AFTER: `verification/ui_rebuild/V5_1_FIXTURE_LEAK_AFTER.txt` — PASS.

Production no longer contains fixture symbols. Debug presentation lives under `src/debug` (override, painter, server/app adapters, QA server-details activity, ContentProvider installer).

Generic seams only: `skipAutoAdvance`, `EXTRA_START_STEP`, `applyConnectionChrome` / `updateStatusText` / `presentAddConnectionSheet`.

Production CONNECTED remains gated by real readiness. Screen 07 is debug chrome (`Защищено` + `00:00:00`) while VPN permission is still absent.

## Renderer

New AVD `HotFox_V51_API34_Clean`. KVM hang reproduced; `-accel off` recovered adb. See `V5_1_CAPTURE_ENVIRONMENT.md`.

## Evidence

- Smoke: `actual_v5_1_smoke/` (01/03/04/05/10/12/15/17)
- Full: `actual_v5_1/` **18/18**
- Compare: `compare_v5_1/` side-by-side / overlay / diff
- CSV: `HOTFOX_PIXEL_DIFF_RESULTS_V5_1.csv` (all `CAPTURED_COMPARED_NOT_PIXEL_PASS`)
- Score: `HOTFOX_ART_DIRECTOR_SCORE_V5_1.csv` (all `pass=NO`)

## In-app asset notes (not pixel acceptance)

- Transparent fox on 01/02/05/07/09: no rectangular matte in these captures; ear/neck look intact at app scale.
- AUTO (03) and Ready (04): topology/completion, not concentric orbits.
- Shadow (17): paths support the shield.

## Tests (repository)

- `:app:assemblePlaystoreDebug` PASS
- unit tests 364/364 (leak test rewritten; previous empty-override test removed)
- `static_check_2_2_0.py` PASS
- `check_no_fixture_leak_v51.py` PASS

## Not done / not claimed

- Pixel-perfect vs boards
- Physical device
- Runtime VPN E2E
- RELEASE READY
- Merge
