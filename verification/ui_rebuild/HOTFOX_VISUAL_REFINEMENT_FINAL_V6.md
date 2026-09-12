# HotFox V6 visual rebuild — final engineering report

Date: 2026-09-11  
Branch: `cursor/hotfox-ui-pixel-lock-rebuild`  
Capture locale: `ru-RU` (`verification/ui_rebuild/V6_LOCALE_PROOF.txt`)

## Verdict

- Captures: **18/18** in `verification/ui_rebuild/actual_v6/`
- Comparisons: **18/18** in `compare_v6/` (side-by-side, overlay, diff)
- Visual / pixel PASS: **0/18**
- Art-director PASS (0–5, every category ≥4.0 and average ≥4.3): **0/18**
- RELEASE READY: **NO**
- Merge: **no**
- Production CONNECTED: still only after real readiness
- Physical / runtime VPN E2E: **NOT EXECUTED**

V5.1 evidence is unchanged under `actual_v5_1/` and `compare_v5_1/`.

## What landed

1. QA locale lock via `adb shell cmd locale set-app-locales com.hotfox.vpn --locales ru-RU`. Production strings were not rewritten to Russian-only.
2. V5.1 containment kept. `check_no_fixture_leak_v51.py` **PASS**.
3. Screen 06: connection ring around the fox removed; segmented route rail under the fox; connecting CTA is `Остановить`.
4. Screen 07: debug protected chrome hides `tv_connection_stage` so `Защищено` is not shown with `Нет разрешения VPN`. VPN permission is not granted; CONNECTED is not production truth.
5. Screen 15: debug-only app glyphs; vertical scrollbar hidden.
6. Screen 17: asymmetric branching topology, no encircling curves; compact subpage CTA.
7. Contextual CTA hierarchy: full orange remaining on onboarding/home; compact/quiet on subpages.
8. Ping values in debug server rows are muted, not auto-green.

## Remaining gaps

- Geometry still differs from boards (MAD 13–35). Not pixel-perfect.
- Screen 10 debug server fixture did not reliably bind the ViewPager fragment recycler in this emulator pass; the stored 10.png is the truthful empty production list in ru-RU.
- Software-emulated compositor can show Android splash/launcher frames; later 10 recapture attempts hit that. The accepted 10 frame is the earlier HotFox UI capture from the full run/smoke, not the launcher.
- Debug painter may still re-apply fixture labels because production `applyRunningState` overwrites chrome. It does not restyle geometry.

## Checks run

- `:app:assemblePlaystoreDebug` x86_64: BUILD SUCCESSFUL
- `:app:testPlaystoreDebugUnitTest`: BUILD SUCCESSFUL
- `static_check_2_2_0.py`: PASS
- `check_no_fixture_leak_v51.py`: PASS
