# HotFox 01–03 validation — ONBOARDING-01-03-20260914

Source SHA of implementation: `112308486236e3dd33685bbf9e42e0a713c7cc2f`  
Evidence SHA (this report): recorded after the capture commit.

## Build

| Gate | Result |
| --- | --- |
| `:app:assemblePlaystoreDebug` | PASS |
| `:app:testPlaystoreDebugUnitTest` | PASS **382/382** |
| `:app:assemblePlaystoreRelease` | PASS (unsigned compile) |
| `:app:lintPlaystoreDebug` | PASS when run after release assemble (combined lint+release once hit a lint engine FileNotFound on generated binding; not an app defect) |
| `python3 verification/static_check_2_2_0.py` | PASS (screen-03 `hf_auto_orbits` allowed only in `HotfoxOnboardingActivity.kt`) |
| `python3 verification/ui_rebuild/check_no_fixture_leak_v51.py` | PASS |
| `verify_reference_lock.py integrity` | PASS |
| `verify_reference_lock.py core` | **CHANGED** — not regenerated. UI flow helper `HotfoxOnboarding.kt` plus historical debug/UI evidence files. VPN/server-selection algorithm unchanged. |
| `bash bootstrap/verify_hotfox_2_2_0.sh` | PASS |

Debug APK: `V2rayNG/app/build/outputs/apk/playstore/debug/HotFox_Proxy_2.2.0_x86_64.apk`  
SHA256: `e73e6e139faec68b7b8de5edd1742997a6a0ee6435fb26125dcc13b90bd3e848`  
applicationId: `com.hotfox.vpn`  
Host CI artifact is the authority for the pushed SHA.

## Layout / visual

Fresh ru-RU captures at **1032×2231 @420dpi**, fontScale 1.0, real SystemUI:

- `actual/01.png` Splash — one fox, planet, bottom wordmark + tagline + loading line, no buttons/nav
- `actual/02.png` Подключите HotFox — wordmark, title/body, one fox+planet in art slot, primary import, secondary buy
- `actual/03.png` AUTO — no fox, `hf_auto_orbits` + `hf_globe_orange`, AUTO / manual CTAs

**Visual/pixel PASS: 0/3.** Not EXACT. iOS-framed crop vs Android SystemUI; existing assets ≠ generated concept (orbit PNG has a filled disc; globe is the stock icon). Residuals documented, comparator not weakened.

## Interactions

| ID | Result |
| --- | --- |
| T03 02 primary → `HotfoxHttpsImportActivity` | PASS (adb tap; import UI shown) |
| T08 03 secondary → server picker | PARTIAL — code + unit wiring PASS; TCG emulator System UI ANR blocked a clean tap. |
| T01–T02, T04–T07, T09–T12 | See UNTESTED_GAPS.md |

## Not claimed

RELEASE READY: **NO**  
VPN E2E / physical device: **NOT EXECUTED**  
Merge: not performed
