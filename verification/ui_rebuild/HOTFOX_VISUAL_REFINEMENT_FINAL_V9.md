# HotFox V9 visual refinement — final engineering report

Date: 2026-09-12  
Branch: `cursor/hotfox-ui-pixel-lock-rebuild`  
Starting HEAD: `d7b81a3a8c37c21ed5d02e7eccb1f3be7cf7ebc6`  
Capture locale: `ru-RU` (`verification/ui_rebuild/V9_LOCALE_PROOF.txt`)

## Verdict

- Captures: **18/18** in `verification/ui_rebuild/actual_v9/`
- Comparisons: **18/18** in `compare_v9/` (side-by-side, overlay, diff)
- Visual / pixel PASS: **0/18**
- Art-director PASS (0–5, every category ≥4.0 and average ≥4.3): **0/18**
- RELEASE READY: **NO**
- Merge: **no**
- Production CONNECTED: still only after real readiness
- Physical / runtime VPN E2E: **NOT EXECUTED**

V8 evidence is unchanged under `actual_v8/` and `compare_v8/`.

## A. Environment

- Branch: `cursor/hotfox-ui-pixel-lock-rebuild`
- Starting head: `d7b81a3a8c37c21ed5d02e7eccb1f3be7cf7ebc6`
- Implementation: `3ea2c40` (planet/copy/hierarchy), `770b03a` (Autopilot captive locale)
- AVD: `HotFox_V51_API34_Clean`, API 34, 1080×2400, `-accel off`, GPU `swiftshader_indirect`
- Locale: `cmd locale set-app-locales com.hotfox.vpn --locales ru-RU`
- Viewport: 1080×2400 emulator screenshots (not phone mockups)
- Cycles: A 01–04 → B 05–07 → C 08–09 → D 10–12 → E 13–15 → F 16–18 on one debug APK; 16 recaptured after captive-copy fix

## B. Build / test

- `:app:assemblePlaystoreDebug` x86_64: BUILD SUCCESSFUL (`HotFox_Proxy_2.2.0_x86_64.apk`)
- `:app:testPlaystoreDebugUnitTest`: **364/364** PASS (run on `3ea2c40`; `770b03a` is `values-ru` only)
- `static_check_2_2_0.py`: PASS
- `check_no_fixture_leak_v51.py`: PASS
- Lint: **NOT EXECUTED** (no dedicated lint gate run in this pass)

## C. Planet implementation

- Resources: `hf_native_planet_backdrop.png`, `hf_native_planet_support.png`
- Separate transparent fox: `hf_fox_bust_transparent`
- Screens: 01, 02, 05, 06 (static), 07, 09; 08 inherits live 05
- Asset previews: `verification/ui_rebuild/v9_assets/`
- In-app audit: `PLANET_COMPOSITION_AUDIT_V9.md`
- Legacy `hf_fox_planet` / `hotfox_art_fox_planet_*`: **not restored**
- Captures show a filled dark mass and one copper rim; fox occludes the upper limb. Not yet the cinematic sit-on-horizon crop of the boards.

## D. 03 / 04 anchor regression

- Concept unchanged: **YES**
- Planet: **NO**
- Evidence: `actual_v9/03.png`, `actual_v9/04.png`
- Visual regressions vs V8 direction: none intended; still not pixel PASS

## E. Screen table 01–18

| ID | Changes | Remaining deviations | Capture | Compare | MAD | Avg | PASS |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 01 | Brand+track inside planet frame; softer V9 planet | Large empty field / crop vs board | yes | yes | 12.18 | 3.66 | NO |
| 02 | Fox 220dp centered on planet | Hero still a discrete block vs board | yes | yes | 26.84 | 3.65 | NO |
| 03 | Micro only, no planet | Geometry vs board | yes | yes | 25.62 | 3.44 | NO |
| 04 | Micro only, no planet | Geometry vs board | yes | yes | 21.10 | 3.44 | NO |
| 05 | Fox on rim; group stroke | Option group vs board | yes | yes | 27.99 | 3.60 | NO |
| 06 | Static planet + V6 rail | Not board-locked | yes | yes | 18.42 | 3.66 | NO |
| 07 | Planet + no permission contradiction | Debug fixture, not real CONNECTED | yes | yes | 19.65 | 3.66 | NO |
| 08 | Live 05 under sheet, dim 0.22 | Sheet chrome vs board | yes | yes | 22.25 | 3.55 | NO |
| 09 | Support planet α0.55; compact Add | Empty field between form and fox | yes | yes | 29.16 | 3.53 | NO |
| 10 | Filter strings in resources; debug list | City names EN; not board-locked | yes | yes | 19.67 | 3.51 | NO |
| 11 | Localized routing row; compact select | Still light vs board | yes | yes | 26.96 | 3.39 | NO |
| 12 | Compact CTA; hairlines; status string | Lower field open | yes | yes | 26.78 | 3.38 | NO |
| 13 | ScreenTitleSecondary | Not the approved settings board | yes | yes | 19.85 | 3.39 | NO |
| 14 | **Умная маршрутизация** + group eyebrows | LAN/IPv6 captions; not board-locked | yes | yes | 26.95 | 3.61 | NO |
| 15 | Compact Save; ScreenTitleSecondary | List-to-summary gap; MAD ~33 | yes | yes | 33.04 | 3.58 | NO |
| 16 | Captive copy localized | Sparse lower field; «Баланс» store line | yes | yes | 26.75 | 3.54 | NO |
| 17 | Inbound/outbound topology; compact CTA | Still not board-locked | yes | yes | 21.76 | 3.56 | NO |
| 18 | Hairline; compact Android CTA | Still sparse vs board | yes | yes | 28.49 | 3.36 | NO |

## F. Production truth

- Canonical datapath / Dns / IPv6 / readiness **not changed**
- Fixtures remain `src/debug`; leak checker PASS
- Screen 07 debug chrome does not show `Защищено` together with `Нет разрешения VPN`
- Screen 10 populated list is debug overlay only
- Screen 12 debug entitlement values are debug-only; production catalog/payment not faked

## G. Unconfirmed

- Pixel-perfect vs approved boards: **FAIL** (0/18)
- Runtime VPN E2E / physical device: **NOT EXECUTED**
- `RELEASE READY`: **NO**
