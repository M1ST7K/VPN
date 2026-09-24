# HotFox 01–03 validation — CORRECTIVE PASS 2 / 2026-09-23

Implementation SHA: `9dfff8db6a65dd6469ebd3fcb8b7f3885d37b3fb`  
Owner native AUTO ancestors in history: `901d3db`, `11fd489`, `0411c1c` (`git merge-base --is-ancestor` = 0).

## Defect closed

Screen **03** no longer paints production art with `hf_auto_orbits` + `hf_globe_orange` ImageViews.

Visible AUTO artwork is `HotfoxAutoOrbitView` (Canvas/Paint/Path) in the measured `onboarding_hero_host`. Raster drawables remain in the tree for other uses (server-list globe) and were not upscaled.

## Build (local this run)

| Gate | Result |
| --- | --- |
| `:app:assemblePlaystoreDebug` | PASS |
| `:app:testPlaystoreDebugUnitTest` | PASS **387/387** |
| `python3 verification/static_check_2_2_0.py` | PASS |
| `python3 verification/ui_rebuild/check_no_fixture_leak_v51.py` | PASS |
| `:app:assemblePlaystoreRelease` | NOT EXECUTED this pass |
| GitHub Actions push CI | **NOT CLAIMED** — do not treat skipped/unknown GHA as CI PASS |

Debug APK: `V2rayNG/app/build/outputs/apk/playstore/debug/HotFox_Proxy_2.2.0_x86_64.apk`  
SHA256: `2e2c82e351651fd323e55effc7890b52cf17b7d267381d680f5fe22225f1a9e0`  
applicationId: `com.hotfox.vpn`

## Captures

Emulator: AVD `HotFox_V51_API34_Clean`, API 34, TCG (`-accel off`), GPU swiftshader, ru-RU.

| File | Viewport | fontScale |
| --- | --- | --- |
| `actual/01_standard.png` | 1032×2231 @420dpi | 1.0 |
| `actual/02_standard.png` | 1032×2231 @420dpi | 1.0 |
| `actual/03_standard.png` | 1032×2231 @420dpi | 1.0 |
| `actual/03_compact.png` | 945×2100 @420dpi | 1.0 |
| `actual/03_large.png` | 1082×2402 @420dpi | 1.0 |
| `actual/03_standard_fs115.png` | 1032×2231 @420dpi | 1.15 |
| `actual/02_standard_fs115.png` | 1032×2231 @420dpi | 1.15 |
| `actual/01_compact.png` | 945×2100 @420dpi | 1.0 |
| `actual/02_compact.png` | 945×2100 @420dpi | 1.0 |
| `actual/05_standard.png` | 1032×2231 @420dpi | 1.0 Home V13 smoke |

**Visual/pixel PASS vs iOS triptych: 0/3.** Android system bars, live copy, native AUTO geometry. Not EXACT. Not claimed.

## Visual review (this pass)

- **01 Splash:** fox and brand block intact; planet as rim/environment; brown wash reduced vs prior 1.0 alpha fill; no boxed crop.
- **02 Connect:** title 34sp / body 15sp; fox remains the midpoint; CTA and «Купить доступ» do not overlap the hero on standard/compact/fontScale 1.15.
- **03 AUTO:** crisp vector globe (meridian + longitudes + latitudes), three orbits, four asymmetric nodes. Compact and large hosts keep the motif; not a 268dp blurry PNG.

## Interactions

| ID | Result |
| --- | --- |
| 02 primary → `HotfoxHttpsImportActivity` | PASS — resumed; import UI; no `FATAL EXCEPTION` for `com.hotfox.vpn` |
| 03 AUTO → `setAutoMode(true)` / KEEP_AUTO | PASS — live tap advanced to VPN-ready onboarding («Всё готово») |
| 03 manual → server picker | PASS — `MainActivity` / Servers with AUTO row; no FATAL |
| Home V13 smoke after shared hero alpha/fox band polish for splash/subscription only | PASS — Disconnected still V13 scene + controls |

## Not claimed

RELEASE READY: **NO**  
VPN E2E / physical device: **NOT EXECUTED**  
Merge: not performed  
GitHub Actions CI: **not reported as PASS**
