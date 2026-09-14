# HotFox V11 visual refinement — final engineering report

Date: 2026-09-12  
Branch: `cursor/hotfox-ui-pixel-lock-rebuild`  
Starting HEAD: `6b434faecba14f0cdff8fb656a68458b9529417b`  
Capture locale: `ru-RU` (`verification/ui_rebuild/V11_LOCALE_PROOF.txt`)

## Verdict

- Phone captures: **18/18** in `verification/ui_rebuild/actual_v11/`
- Comparisons: **18/18** in `compare_v11/` (side-by-side, overlay, diff)
- Contact sheet: `verification/ui_rebuild/contact_v11.png`
- Compact phone planet set: `actual_v11_compact/`
- Tablet: `tablet_v11/` (01/02/05/06/07/08/09 + 10/14/15/17/18)
- Large tablet: `tablet_v11_large/` (01/05/07/18)
- Visual / pixel PASS: **0/18**
- Art-director PASS (every category ≥4.0 and average ≥4.3): **0/18**
- RELEASE READY: **NO**
- Merge: **no**
- Production CONNECTED: still only after real readiness
- Physical / runtime VPN E2E: **NOT EXECUTED**

V10 evidence is unchanged under `actual_v10/` and `compare_v10/`.

## A. Environment

- Spec pack HEAD: `6b434fa`
- Planet reconstruct + breakpoints: `b18951b`
- Bleed/clip follow-up: `444854f`
- Evidence pack: `fcd9eb1`
- AVD: `HotFox_V51_API34_Clean`, API 34
- Regular phone viewport: 1080×2400 @420dpi
- Locale: `cmd locale set-app-locales com.hotfox.vpn --locales ru-RU`

## B. Build / test

- `:app:assemblePlaystoreDebug` x86_64: BUILD SUCCESSFUL
- `:app:testPlaystoreDebugUnitTest`: **364/364** PASS
- `static_check_2_2_0.py`: PASS
- `check_no_fixture_leak_v51.py`: PASS
- Lint: **NOT EXECUTED**

## C. Planet implementation

Cues were reconstructed, not upscaled. Production layers: separate planet PNG + `hf_fox_bust_transparent`. Legacy `hf_fox_planet` not restored. See `HOTFOX_V11_PLANET_FIDELITY.md`.

## D. 03 / 04

Concept unchanged. No planet. Evidence: `actual_v11/03.png`, `04.png`.

## E. Screen-by-screen changelog

| ID | Changes | Remaining | Capture | MAD | Avg | PASS |
| --- | --- | --- | --- | --- | --- | --- |
| 01 | Reconstructed cinematic planet; breakpoint fox size | Crop vs board | yes | 16.68 | 3.84 | NO |
| 02 | Onboarding planet crop; centered fox | Geometry vs board | yes | 31.12 | 3.81 | NO |
| 03 | Unchanged concept; gutter dimen only | Geometry vs board | yes | 25.98 | 3.50 | NO |
| 04 | Unchanged concept; gutter dimen only | Geometry vs board | yes | 21.35 | 3.46 | NO |
| 05 | Home planet PNG; horizontal full-bleed | Vertical band crop | yes | 31.07 | 3.79 | NO |
| 06 | Static planet; rail separate | Band crop | yes | 20.09 | 3.79 | NO |
| 07 | Same planet family; debug Защищено | Debug ≠ real CONNECTED | yes | 20.47 | 3.80 | NO |
| 08 | Live 05 behind sheet | Sheet chrome vs board | yes | 21.24 | 3.76 | NO |
| 09 | Support planet; centered fox | Empty-field vs board | yes | 30.49 | 3.73 | NO |
| 10 | Gutter dimen; no planet | Debug overlay | yes | 20.16 | 3.60 | NO |
| 11 | Reading width helper; no planet | Sparse vs board | yes | 27.20 | 3.49 | NO |
| 12 | Reading width helper; no planet | Lower field open | yes | 26.84 | 3.45 | NO |
| 13 | Gutter dimen; no planet | Not the board | yes | 19.62 | 3.46 | NO |
| 14 | Gutter + tablet width; ru-RU kept | Not board-locked | yes | 27.70 | 3.63 | NO |
| 15 | Gutter + tablet sample; 6 debug apps | List-to-meta gap | yes | 33.60 | 3.63 | NO |
| 16 | Gutter dimen; no planet | Sparse | yes | 27.05 | 3.54 | NO |
| 17 | Gutter dimen; no planet | Not board-locked | yes | 22.33 | 3.68 | NO |
| 18 | Always-on hero dimens by breakpoint | Sparse vs board | yes | 29.43 | 3.56 | NO |

## F. Not confirmed

- Pixel-perfect vs boards
- Runtime VPN E2E / public IP
- Physical device
- RELEASE READY

Reports: `HOTFOX_V11_PLANET_FIDELITY.md`, `HOTFOX_V11_RESPONSIVE_PHONE_TABLET.md`, `HOTFOX_ART_DIRECTOR_SCORE_V11.csv`.
