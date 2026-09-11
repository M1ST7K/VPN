# UNTESTED UI GAPS

## Originals

Full-resolution locked files from `design/hotfox_18_final_style_reference/MANIFEST.txt` are **PRESENT** (zip + extracted copies). `verification/ui_rebuild/check_locked_originals.py` reports `PIXEL_DIFF_STATUS=ORIGINALS_PRESENT` (missing=0, mismatch=0).

Extracted `*.jpeg`/`*.png` boards are gitignored; the zip + MANIFEST remain the committed source of truth. Inner-phone goldens live in `verification/ui_rebuild/reference_analysis/*_golden_ui.png`.

## Owner-environment blocker: emulator screenshot / pixel-diff

Exact pixel-diff / overlay / heatmap against SHA-256 originals is **NOT EXECUTED**.

Missing:

- Android emulator (or device) screenshot capture of the 18 implemented screens
- aligned actual-vs-golden overlays, heatmaps, changed-pixel counts

Do **not** treat inner-phone goldens or preview-like crops as an exact-pixel PASS. The master-prompt completion wording is **not** used.

## Other gaps

- Local `:app:assemblePlaystoreDebug` / unit tests / lint / release compile: **NOT EXECUTED** (Android SDK/NDK absent). Push CI is the reconstruct/build/unit/lint evidence path.
- `verify_hotfox_2_2_0.sh`: **NOT EXECUTED** here (HEV `.so` not built in this environment).
- Runtime VPN E2E / physical device: **NOT EXECUTED / deferred**. UI work must not claim `RELEASE READY`.
- Screen 01 splash is not held with a fake delay; the user may barely see it.
- Screens 10/11 have no committed country-flag bitmaps; emoji flags are not invented.
- Screen 12 commercial layout remains the existing entitlement-bound subscription surface, not a pixel clone of the presentation-board marketing expiry example. Dynamic values stay backend/store-derived (no hardcoded `12 марта 2026`).
- Screen 13 keeps extra Прочее rows for Updates and ads so those real destinations stay reachable.
- Screen 15 chrome opens real `PerAppProxyActivity` instead of inlining the app recycler.
- Screen 07 bottom-nav label in the board says «Соединение»; chrome follows screen 05 («Главная»).
- Presentation-board callout lines may remain on goldens; they are not product UI.
