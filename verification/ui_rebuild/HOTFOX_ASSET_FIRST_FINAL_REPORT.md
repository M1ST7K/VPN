# HotFox asset-first UI rebuild — final report

Date: 2026-09-11  
Branch: `cursor/hotfox-ui-pixel-lock-rebuild`  
Supervisory prompt: `design/HOTFOX_CURSOR_SUPERVISORY_REPO_PARTS_V3.txt`

## A. Environment

- Branch: `cursor/hotfox-ui-pixel-lock-rebuild`
- Base: `cursor/hotfox-2.4-sync-rules-22f7`
- Android variant: `playstoreDebug` (`:app:assemblePlaystoreDebug`)
- Reconstruct: `bootstrap/bootstrap_source.sh` PASS
- SDK: `ANDROID_HOME=/home/ubuntu/android-sdk`, NDK `29.0.14206865`, compileSdk 37
- Emulator: API 34 `google_apis/x86_64` AVD `hotfox34` (Pixel 6, 1080×2400, night mode). Required `chmod 666 /dev/kvm` because the default user was not in group `kvm`.
- APK installed: `HotFox_Proxy_2.2.0_x86_64.apk` sha256 `cd1e174a151cba8d5a9d1908c046351aa7e0541b4e7af1742721eecd80903990`

## B. Asset intake

**PASS** — see `ASSET_INTAKE_REPORT.md`

- Unique IDs 212; PNG 842; SVG 210; SHA mismatches 0; collisions 0
- `fixture_apps` not in production res

## C. Legacy cleanup

Production refs to named screenshot-derived composites = **0**. Replacements are kit `hf_*` only on the screens that show that artwork.

## D. Build

| Gate | Result |
| --- | --- |
| Reconstruct + overlay | PASS |
| `:app:assemblePlaystoreDebug` | PASS |
| `:app:testPlaystoreDebugUnitTest` | PASS — 363 tests, 0 failures |
| `static_check_2_2_0.py` | PASS |
| originals integrity | PASS |
| lint / release assemble | NOT EXECUTED |

## E. Screen table

Implemented in overlay for 01–18. Visual:

| ID | Capture | Visual |
| --- | --- | --- |
| 01 Splash | NOT_CAPTURED | FAIL / NOT EXECUTED |
| 02 Onboarding connect | CAPTURED | CAPTURED_NOT_PIXEL_PASS |
| 03–04 Onboarding AUTO/ready | NOT_CAPTURED | FAIL / NOT EXECUTED |
| 05 Connection disconnected | CAPTURED | CAPTURED_NOT_PIXEL_PASS |
| 06–09 Connecting/protected/sheet/HTTPS | NOT_CAPTURED | FAIL / NOT EXECUTED |
| 10 Servers | CAPTURED | CAPTURED_NOT_PIXEL_PASS |
| 11 Server details | NOT_CAPTURED | FAIL / NOT EXECUTED |
| 12 Subscription | CAPTURED | CAPTURED_NOT_PIXEL_PASS |
| 13 Settings | CAPTURED | CAPTURED_NOT_PIXEL_PASS |
| 14 Smart Routing | CAPTURED | CAPTURED_NOT_PIXEL_PASS |
| 15 Apps & rules | NOT_CAPTURED | FAIL / NOT EXECUTED |
| 16 Autopilot | CAPTURED | CAPTURED_NOT_PIXEL_PASS |
| 17 Shadow | CAPTURED | CAPTURED_NOT_PIXEL_PASS |
| 18 Always-on | CAPTURED | CAPTURED_NOT_PIXEL_PASS |

9/18 real emulator screenshots. **0/18 pixel PASS.** Overlay/diff golden compare was not claimed.

## F. Remaining gaps

- Pixel/geometry compare vs phone-board references not executed as PASS (boards include device chrome; no declared crop goldens)
- Screens 01, 03, 04, 06, 07, 08, 09, 11, 15 not captured
- 06/07 must not be faked; CONNECTED was not shown
- Non-exported activities cannot be started via `adb shell am start` on API 34
- `onNewIntent` now applies `EXTRA_OPEN_SECTION` (fix committed) — APK used for screenshots was built before that fix; captures used in-app taps
- Physical VPN E2E: NO

## G. Explicit statements

- **RELEASE READY: NO**
- **Emulator screenshots captured: PARTIAL (9/18)**
- **Physical device VPN E2E: NO**
- **18/18 visual evidence: NO**
- **18/18 pixel PASS: NO**
- VPN/security/runtime semantics were not weakened for visuals
