# HotFox V7 visual refinement — final engineering report

Date: 2026-09-12  
Branch: `cursor/hotfox-ui-pixel-lock-rebuild`  
Starting V6/V7-docs HEAD: `f04ec7dda9c1aa04ec79b31f762862f2e4f4a33d`  
Implementation HEAD before evidence: `967a74c93a2c78f6ae998bfc8f114df817992e61`  
Capture locale: `ru-RU` (`verification/ui_rebuild/V7_LOCALE_PROOF.txt`)

## Verdict

- Captures: **18/18** in `verification/ui_rebuild/actual_v7/`
- Comparisons: **18/18** in `compare_v7/` (side-by-side, overlay, diff)
- Visual / pixel PASS: **0/18**
- Art-director PASS (0–5, every category ≥4.0 and average ≥4.3): **0/18**
- RELEASE READY: **NO**
- Merge: **no**
- Production CONNECTED: still only after real readiness
- Physical / runtime VPN E2E: **NOT EXECUTED**

V6 evidence is unchanged under `actual_v6/` and `compare_v6/`.

## Planet contract

Separate layer: `drawable/hf_native_planet_backdrop.xml` behind `hf_fox_bust_transparent`.  
Forbidden legacy composites were **not** restored: `hf_fox_planet` = 0, `hotfox_art_fox_planet_*` = 0.

| Screen | Planet |
| --- | --- |
| 01 Splash | yes, behind fox |
| 02 Onboard Connect | yes, behind fox |
| 03 AUTO | **no** (anchor) |
| 04 Ready | **no** (anchor) |
| 05 Disconnected | yes, behind fox |
| 06 Connecting | yes, static backdrop only; rail/stages unchanged |
| 07 Protected | yes, behind fox |
| 08 Add sheet | only as live 05 background under scrim |
| 09 HTTPS | yes, behind fox |
| 10–18 | **no** |
| 17 Shadow | shield + branching topology only |

## 03 / 04 regression

Concept frozen. Captures show routing topology (03) and completion servers+check (04) with **no planet**. Direction matches V6; not claimed as pixel PASS.

## Screen 10 fixture binding

Debug-only `content_list_overlay` RecyclerView in `activity_main.xml` (GONE in production).  
`HotfoxUiQaPainter` in `src/debug` binds `HotfoxUiQaServerAdapter` and hides the ViewPager for the screenshot scenario.  
Capture 10 shows AUTO + Amsterdam/Frankfurt/Paris/London/New York/Toronto. Ping values remain muted. Production catalog is not written.

## Remaining gaps

- Geometry still differs from boards (MAD ≈ 13–35). Not pixel-perfect.
- 14/16 still sparse in the lower half.
- 13 still reads closer to editorial settings than the approved board.
- 17 topology still radiates around the shield; it is not an orbit band and has no planet.
- Art-director scores are all below the 4.0 / 4.3 PASS gate.

## Checks run

- `:app:assemblePlaystoreDebug` x86_64: BUILD SUCCESSFUL
- `:app:testPlaystoreDebugUnitTest`: **364/364** PASS
- `static_check_2_2_0.py`: PASS
- `check_no_fixture_leak_v51.py`: PASS
- Emulator captures: AVD `HotFox_V51_API34_Clean`, API 34, ru-RU, 1080×2400
