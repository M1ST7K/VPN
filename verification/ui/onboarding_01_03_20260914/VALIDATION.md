# HotFox 01–03 validation — ONBOARDING-01-03-20260914

Implementation SHA (planet/import fix): `e0a6bd3af71851dcf661c8977d935d5039ce3605`  
Evidence SHA: recorded after this capture commit.

## Owner follow-up 2026-09-14

Планета на 01/02 была обрезана прямоугольным клипом: `hfHeroFitParent` ставил квадрат `width × overflow` внутрь слота с `clipToOutline`. Исправление: cover по длинной стороне экрана с аспектом 1080×1400, fullscreen слой за chrome на 02, без clip.

Последний прошлый кадр с «Close app / Wait» — это **System UI / Process system ANR** на TCG-эмуляторе, не FATAL `com.hotfox.vpn`. Кнопка импорта `<Button>` в Material3-теме заменена на `AppCompatButton`. Повторный tap 02 → `HotfoxHttpsImportActivity` без диалога, без `FATAL EXCEPTION`.

## Build

| Gate | Result |
| --- | --- |
| `:app:assemblePlaystoreDebug` | PASS |
| `:app:testPlaystoreDebugUnitTest` | PASS **385/385** |
| `:app:assemblePlaystoreRelease` | PASS (unsigned compile) |
| `python3 verification/static_check_2_2_0.py` | PASS |
| `python3 verification/ui_rebuild/check_no_fixture_leak_v51.py` | PASS |
| `verify_reference_lock.py integrity` | PASS |
| `verify_reference_lock.py core` | **CHANGED** — not regenerated |
| `:app:lintPlaystoreDebug` | NOT EXECUTED this pass (release `lintVital` ran as part of assembleRelease) |

Debug APK: `V2rayNG/app/build/outputs/apk/playstore/debug/HotFox_Proxy_2.2.0_x86_64.apk`  
SHA256: `edb0cb0fc9b368f0f6546031989e5536f43a256e6d2f5e8556b0d1449ec20002`  
applicationId: `com.hotfox.vpn`

## Layout / visual

Fresh ru-RU captures at **1032×2231 @420dpi**, fontScale 1.0, real SystemUI:

- `actual/01.png` Splash — planet covers the phone, copper rim off the right/top, no boxed crop
- `actual/02.png` Подключите HotFox — fullscreen planet behind title/fox/CTA, not an art-slot postage stamp
- `actual/03.png` AUTO — no fox/planet, orbits + globe, AUTO / manual CTAs

**Visual/pixel PASS: 0/3.** Not EXACT versus the iOS triptych crop.

## Interactions

| ID | Result |
| --- | --- |
| T03 02 primary → `HotfoxHttpsImportActivity` | PASS — activity resumed; import UI without ANR overlay; logcat has no `FATAL EXCEPTION` for `com.hotfox.vpn` |
| T08 03 secondary → server picker | NOT EXECUTED this pass (code + unit wiring still PASS) |

## Not claimed

RELEASE READY: **NO**  
VPN E2E / physical device: **NOT EXECUTED**  
Merge: not performed
