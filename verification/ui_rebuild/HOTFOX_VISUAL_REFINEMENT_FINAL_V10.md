# HotFox V10 visual refinement — final engineering report

Date: 2026-09-12  
Branch: `cursor/hotfox-ui-pixel-lock-rebuild`  
Starting HEAD: `4b7c1665e5bf4d10acefa4b192a1812e22e18a64`  
Capture locale: `ru-RU` (`verification/ui_rebuild/V10_LOCALE_PROOF.txt`)

## Verdict

- Captures: **18/18** in `verification/ui_rebuild/actual_v10/`
- Comparisons: **18/18** in `compare_v10/` (side-by-side, overlay, diff)
- Visual / pixel PASS: **0/18**
- Art-director PASS (0–5, every category ≥4.0 and average ≥4.3): **0/18**
- RELEASE READY: **NO**
- Merge: **no**
- Production CONNECTED: still only after real readiness
- Physical / runtime VPN E2E: **NOT EXECUTED**

V9 evidence is unchanged under `actual_v9/` and `compare_v9/`.

## A. Environment

- Branch: `cursor/hotfox-ui-pixel-lock-rebuild`
- Starting head: `4b7c1665e5bf4d10acefa4b192a1812e22e18a64`
- Implementation: `24798dc` (planet/hierarchy/utility polish)
- Debug fixture follow-up: `224183b` (Амстердам; six unclipped apps)
- AVD: `HotFox_V51_API34_Clean`, API 34, 1080×2400
- Locale: `cmd locale set-app-locales com.hotfox.vpn --locales ru-RU`
- Viewport: 1080×2400 emulator screenshots (not phone mockups)
- Cycles: A 01–04 → B 05–07 → C 08–09 → D 10–12 → E 13–15 → F 16–18; 11 and 15 recaptured after fixture polish

## B. Build / test

- `:app:assemblePlaystoreDebug` x86_64: BUILD SUCCESSFUL (`HotFox_Proxy_2.2.0_x86_64.apk`)
- `:app:testPlaystoreDebugUnitTest`: **364/364** PASS
- `static_check_2_2_0.py`: PASS
- `check_no_fixture_leak_v51.py`: PASS
- Lint: **NOT EXECUTED**

## C. Planet implementation

- Resources: `hf_native_planet_backdrop.png`, `hf_native_planet_support.png`
- Separate transparent fox: `hf_fox_bust_transparent`
- Screens: 01, 02, 05, 06 (static), 07, 09; 08 inherits live 05
- 03 / 04 / 10–18: no planet
- Asset previews: `verification/ui_rebuild/v10_assets/`
- In-app audit: `PLANET_COMPOSITION_AUDIT_V10.md`
- Legacy `hf_fox_planet` / `hotfox_art_fox_planet_*`: **not restored**
- Captures show a filled dark plum mass and a short copper crescent. The fox occludes the upper limb. Not pixel-locked to boards.

## D. 03 / 04 anchor regression

- Concept unchanged: **YES**
- Planet: **NO**
- Evidence: `actual_v10/03.png`, `actual_v10/04.png`
- Micro only: hub recentered (03), ready illustration staged (04)

## E. Screen-by-screen changelog

| ID | Changes | Remaining deviations | Capture | Compare | MAD | Avg | PASS |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 01 | Filled planet volume; fox+wordmark+loader | Crop/empty field vs board | yes | yes | 14.52 | 3.74 | NO |
| 02 | Fox sits on planet; secondary CTA | Hero block vs board | yes | yes | 27.32 | 3.71 | NO |
| 03 | Micro: 280dp centered hub; cream secondary | Geometry vs board | yes | yes | 25.98 | 3.50 | NO |
| 04 | Micro: 236dp centered device | Geometry vs board | yes | yes | 21.34 | 3.46 | NO |
| 05 | Planet volume; cream permission; 56dp rows | Option group vs board | yes | yes | 32.32 | 3.69 | NO |
| 06 | Static planet; thicker rail | Not board-locked | yes | yes | 18.69 | 3.70 | NO |
| 07 | Planet volume; no permission contradiction | Debug fixture, not real CONNECTED | yes | yes | 20.14 | 3.73 | NO |
| 08 | Dim 0.18; Always-on VPN label; 56dp rows | Sheet chrome vs board | yes | yes | 21.20 | 3.68 | NO |
| 09 | Support planet α0.72; fox 208dp | Empty field remains | yes | yes | 29.35 | 3.60 | NO |
| 10 | RU city names; green ping only healthy | Debug overlay; not board-locked | yes | yes | 20.16 | 3.60 | NO |
| 11 | Амстердам; 56dp rows; 13sp note | Still light vs board | yes | yes | 27.19 | 3.49 | NO |
| 12 | 56dp rows; 13sp legal | Lower field open | yes | yes | 26.84 | 3.45 | NO |
| 13 | 52dp rows | Not the approved settings board | yes | yes | 19.63 | 3.46 | NO |
| 14 | Calmer eyebrows; 13sp note | LAN/IPv6 captions; not board-locked | yes | yes | 27.70 | 3.63 | NO |
| 15 | 6 debug apps, no clip | List-to-meta gap; MAD ~33 | yes | yes | 33.60 | 3.63 | NO |
| 16 | Footer 13sp | Sparse; «Баланс» store line | yes | yes | 27.05 | 3.54 | NO |
| 17 | Larger topology + shield | Still not board-locked | yes | yes | 22.33 | 3.68 | NO |
| 18 | `hf_native_always_on_hero`; Body copy | Still sparse vs board | yes | yes | 29.42 | 3.56 | NO |

QA checklist: `HOTFOX_V10_DESIGN_QA_SELF_CHECK.md`  
Scores: `HOTFOX_ART_DIRECTOR_SCORE_V10.csv`

## F. Production truth

- Canonical datapath / DNS / IPv6 / readiness **not changed**
- Fixtures remain `src/debug`; leak checker PASS
- Screen 07 debug chrome does not show `Защищено` together with `Нет разрешения VPN`
- Screen 10 populated list is debug overlay only
- Screen 12 debug entitlement values are debug-only; production catalog/payment not faked
- Screen 15 extra apps are debug glyphs only; production uses installed icons

## G. Unconfirmed

- Pixel-perfect vs approved boards: **FAIL** (0/18)
- Runtime VPN E2E / physical device: **NOT EXECUTED**
- `RELEASE READY`: **NO**
