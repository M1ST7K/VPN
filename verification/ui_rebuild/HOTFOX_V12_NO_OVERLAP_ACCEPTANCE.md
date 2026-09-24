# HotFox V12 — no-overlap Home acceptance

Date: 2026-09-13  
Branch: `cursor/hotfox-ui-pixel-lock-rebuild`  
Locale: `ru-RU`  
Viewport: `1032×2292` @ `420dpi` (393×873 dp)  
Emulator: `HotFox_V51_API34_Clean`, software (`-accel off`)

Status: **V12 Home scaffold implemented and captured.**  
Visual/pixel lock vs 18-board set: **not claimed.**  
RELEASE READY: **NO.**  
Runtime/physical VPN E2E: **NOT EXECUTED.**

## Changed files

- `bootstrap/hotfox_2_2_0/app/src/main/res/layout/activity_main.xml` — shared Home column: HeaderStatus, HeroSlot, StatePanel, QuickSettings, FooterInfoSlot; StatePanel is a vertical LinearLayout (content + CTA), not a FrameLayout overlay.
- `bootstrap/hotfox_2_2_0/app/src/main/res/values/hf_hero_dimens.xml` — `hf_home_hero_min_height`, `hf_home_state_panel_height`, `hf_home_cta_height`, `hf_home_footer_height`.
- `bootstrap/hotfox_2_2_0/app/src/main/java/com/v2ray/ang/ui/MainActivity.kt` — Connecting/Connected keep FooterInfoSlot reserved (`View.INVISIBLE`, not `GONE`).
- `bootstrap/hotfox_2_2_0/app/src/main/java/com/v2ray/ang/ui/HotfoxHeroComposition.kt` / `HotFoxHeroArtwork.kt` — fox/planet geometry relative to HeroSlot only (from prior V12 scaffold commit).
- `bootstrap/hotfox_2_2_0/app/src/debug/java/com/v2ray/ang/ui/HotfoxHomeSlotGuard.kt` — debug overlap asserts.
- `bootstrap/hotfox_2_2_0/app/src/debug/java/com/v2ray/ang/ui/HotfoxUiQaPainter.kt` — in-place chrome paint.
- `bootstrap/hotfox_2_2_0/app/src/debug/java/com/v2ray/ang/ui/HotfoxUiScreenshotChromeReceiver.kt` — debug broadcast `UI_SCREENSHOT_CHROME`.
- `bootstrap/hotfox_2_2_0/app/src/debug/AndroidManifest.xml` — exported debug receiver.
- `bootstrap/hotfox_2_2_0/app/src/test/java/com/v2ray/ang/ui/HotfoxUiScreenshotFixtureLeakTest.kt` — receiver name stays out of `src/main`.
- `verification/ui_rebuild/capture_home_v12_no_overlap.py` — warm 05→06→07 capture.
- `verification/ui_rebuild/actual_v12_no_overlap/05.png`, `06.png`, `07.png`, `home_v12_sbs.png`.

VPN engine, Xray/HEV, routing, DNS/IPv6, entitlement, subscription stores, and CONNECTED truth were not modified.

## Architecture

Disconnected / Connecting / Connected share one measured vertical scaffold inside `connection_column`:

1. `header_status_slot` — brand + headline + subtitle  
2. `hero_slot` (`layout_weight=1`, `clipChildren=true`) — only fox/planet may layer  
3. `state_panel_slot` — **fixed** `hf_home_state_panel_height`  
   - `state_panel_content` (`weight=1`) — permission / connecting rail / metrics  
   - `connect_action_host` — 56dp CTA below content, not overlaid on it  
4. `layout_connection_rows` — QuickSettings in normal flow, `maxLines=1` + ellipsize  
5. `footer_info_slot` — **always occupies** `hf_home_footer_height`; copy is `INVISIBLE` in Connecting/Connected  
6. `hotfox_bottom_nav` — opaque sibling under the pager, not an overlay

Hero geometry is identical across the three states (`HomeHeroGeometry`, `setMode(HOME_DISCONNECTED)`).

## Removed hacks

- Full-screen fox overlay behind controls (pre-V12).  
- ScrollView + `layout_weight` that measured later Home children at zero height.  
- `GONE` footer that grew HeroSlot and moved QuickSettings.  
- StatePanel FrameLayout that stacked the CTA on top of Connected route labels.  
- Per-state absolute fox Y / overlay offsets (not restored).

## Build evidence

| Check | Result |
| --- | --- |
| `:app:assemblePlaystoreDebug` | PASS |
| `:app:testPlaystoreDebugUnitTest` | PASS, 370 tests, 0 failures |
| `python3 verification/ui_rebuild/check_no_fixture_leak_v51.py` | PASS |
| `python3 verification/static_check_2_2_0.py` | PASS |
| 360×800 / fontScale 1.15 | NOT EXECUTED |

## Visual evidence (ru-RU, same resolution)

- Disconnected: `verification/ui_rebuild/actual_v12_no_overlap/05.png`
- Connecting: `verification/ui_rebuild/actual_v12_no_overlap/06.png`
- Connected: `verification/ui_rebuild/actual_v12_no_overlap/07.png`
- Side-by-side: `verification/ui_rebuild/actual_v12_no_overlap/home_v12_sbs.png`

Pixel comparison at `1032×2292`:

- Hero orange occupancy identical in 05/06/07 (`0.040260`).  
- QuickSettings top band `y=1350–1450`: **0.00** mean channel delta across all three states.  
- Nav `05` vs `06`: **0.00**; `07` differs only by the protected nav label `Соединение`.  
- Footer band differs 05 vs 06/07 because helper copy is hidden, **slot height kept**.

## Invariant verification

- Functional regions do not overlap.  
- Fox/planet stay inside HeroSlot; no art in bottom nav.  
- Progress rail and traffic stats sit in StatePanel, not over the fox face.  
- Connected route labels (`Вы` / `HotFox` / `Интернет`) sit above `Отключить`, not under the CTA.  
- QuickSettings top is stable (measured 0px delta).  
- Row copy does not wrap into the next row.  
- No `offset` / absolute Y used for Home region placement.  
- Production `CONNECTED` / `Защищено` remains gated by real readiness; 07 is debug visual chrome only.

## Remaining honestly

- Pixel-perfect match to the 18 style boards is not claimed.  
- Compact 360×800 and fontScale 1.15 were not recaptured in this pass.  
- Software emulator ANR dialogs remain an environment issue; Wait-only, no Close app.  
- Runtime VPN path and physical device gate are deferred.
