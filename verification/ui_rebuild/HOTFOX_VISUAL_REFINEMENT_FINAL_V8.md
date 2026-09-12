# HotFox V8 visual refinement — final engineering report

Date: 2026-09-12  
Branch: `cursor/hotfox-ui-pixel-lock-rebuild`  
Starting HEAD: `e56d0331bcf525ea3356ae26bdac494b1ffb0266`  
Capture locale: `ru-RU` (`verification/ui_rebuild/V8_LOCALE_PROOF.txt`)

## Verdict

- Captures: **18/18** in `verification/ui_rebuild/actual_v8/`
- Comparisons: **18/18** in `compare_v8/` (side-by-side, overlay, diff)
- Visual / pixel PASS: **0/18**
- Art-director PASS (0–5, every category ≥4.0 and average ≥4.3): **0/18**
- RELEASE READY: **NO**
- Merge: **no**
- Production CONNECTED: still only after real readiness
- Physical / runtime VPN E2E: **NOT EXECUTED**

V7 evidence is unchanged under `actual_v7/` and `compare_v7/`.

## A. Environment

- Branch: `cursor/hotfox-ui-pixel-lock-rebuild`
- Starting head: `e56d0331bcf525ea3356ae26bdac494b1ffb0266`
- Implementation commits: `ffdd58e` (filled planet PNG), `ffa60b9` (home/import/apps polish), plus the V8 utility-page fill commit on this evidence head
- AVD: `HotFox_V51_API34_Clean`, API 34, 1080×2400, `-accel off`, GPU `swiftshader_indirect`
- Locale: `cmd locale set-app-locales com.hotfox.vpn --locales ru-RU`
- Viewport: 1080×2400 emulator screenshots (not phone mockups)

## B. Build / test

- `:app:assemblePlaystoreDebug` x86_64: BUILD SUCCESSFUL (`HotFox_Proxy_2.2.0_x86_64.apk`)
- `:app:testPlaystoreDebugUnitTest`: **364/364** PASS
- `static_check_2_2_0.py`: PASS
- `check_no_fixture_leak_v51.py`: PASS
- Lint: **NOT EXECUTED** (no dedicated lint gate run in this pass)

## C. Planet implementation

- Resource: `app/src/main/res/drawable-nodpi/hf_native_planet_backdrop.png`
- Separate transparent fox: `hf_fox_bust_transparent`
- Screens: 01, 02, 05, 06 (static), 07, 09; 08 inherits live 05
- Asset previews: `verification/ui_rebuild/v8_assets/`
- In-app audit: `PLANET_COMPOSITION_AUDIT_V8.md`
- Legacy `hf_fox_planet` / `hotfox_art_fox_planet_*`: **not restored**
- V7 double-arc XML vector removed. Captures show a filled dark mass and one copper rim, fox occluding the upper limb. Not yet the cinematic sit-on-horizon crop of the boards.

## D. 03 / 04 anchor regression

- Concept unchanged: **YES**
- Planet: **NO**
- Evidence: `actual_v8/03.png`, `actual_v8/04.png`
- Visual regressions vs V6/V7 direction: none intended; still not pixel PASS

## E. Screen table 01–18

| ID | Changes | Remaining deviations | Capture | Compare | Avg | PASS |
| --- | --- | --- | --- | --- | --- | --- |
| 01 | Filled planet PNG, fox on rim, smaller wordmark | Brand block still large vs board | yes | yes | 3.56 | NO |
| 02 | Planet behind fox on Connect | Spacing/CTA vs board | yes | yes | 3.55 | NO |
| 03 | Micro only, no planet | Geometry vs board | yes | yes | 3.44 | NO |
| 04 | Micro only, no planet | Geometry vs board | yes | yes | 3.44 | NO |
| 05 | Filled planet, quieter permission | Option group and fox crop vs board | yes | yes | 3.50 | NO |
| 06 | Static planet + V6 rail | Not board-locked | yes | yes | 3.58 | NO |
| 07 | Planet + no permission contradiction | Debug fixture, not real CONNECTED | yes | yes | 3.58 | NO |
| 08 | Live 05 under sheet, dim 0.28 | Sheet chrome vs board | yes | yes | 3.45 | NO |
| 09 | Supporting planet | Fox/planet scale vs board | yes | yes | 3.41 | NO |
| 10 | Debug server overlay | Empty production catalog elsewhere | yes | yes | 3.41 | NO |
| 11 | Debug details | Still light vs board | yes | yes | 3.26 | NO |
| 12 | Debug subscription labels | Lower field open | yes | yes | 3.24 | NO |
| 13 | Editorial rows | Not the approved settings board | yes | yes | 3.30 | NO |
| 14 | Real DNS/LAN/IPv6/custom/reconnect rows | Copy/wrapping vs board; LAN off is truthful | yes | yes | 3.50 | NO |
| 15 | 5 debug apps + packages, no clip, no scrollbar | List-to-summary gap; MAD ~35 | yes | yes | 3.49 | NO |
| 16 | Filled policy rows, truthful 0 trusted | Captive caption wrap; not board-locked | yes | yes | 3.50 | NO |
| 17 | Corridor topology + five rows | CTA still low; not board-locked | yes | yes | 3.48 | NO |
| 18 | Body + shield + truthful Unknown | Still sparse vs board | yes | yes | 3.29 | NO |

## F. Production truth

- VPN datapath / DNS / IPv6 / routing / payment / subscription semantics: **not modified**
- Fixture helpers remain under `src/debug`
- `check_no_fixture_leak_v51.py`: PASS
- Production CONNECTED still gated by real readiness
- Screen 07 `Защищено` is debug presentation chrome only
- Screen 10/11/12/15 populated lists are debug-only
- Trusted network count on 16 is the real store size (0), not a fake “2 сети”

## G. Release status

RELEASE READY = **NO**. Runtime and physical release validation were not executed and remain a separate gate.
