# HotFox — Final visual consistency pass (01–07) — validation

Environment: AVD `HotFox_V51_API34_Clean`, API 34, x86_64 TCG, density 420, locale ru-RU.
Standard viewport 1032x2231; compact 945x2100; fontScale 1.0 and 1.15.
Debug APK SHA256: `ad6bf445248f26ac5a079e10a72cb0669710e5055be1f884b95d11bf36b4d806`.

Captures come from the real app renderer via the debug-only screenshot harness
(`src/debug`). Harness state is presentation-only and is not VPN/payment proof.
The capture script now waits for a per-screen semantic marker in the live UI tree
before capturing; an earlier `05_home_compact` frame taken before Home decoration
finished showed a square CTA without icon, and did not reproduce once the frame
was taken after render.

## Screenshots
- actual/01_splash.png … actual/07_connected.png
- matrix/03_auto_compact.png, 04_https_compact.png, 05_home_compact.png
- matrix/02_connect_fs115.png, 03_auto_fs115.png
- comparison/01–07_side_by_side.png (previous accepted baseline vs current), contact_01_07.png

## Defect found and fixed during review
- Compact Home: Shadow card title broke as `Shado / w`. Fixed by single-line title
  and removing redundant end padding next to SwitchCompat (switch has internal inset).
  Recaptured: single line at compact and standard.

## Gates (local)
| Gate | Result |
|---|---|
| :app:assemblePlaystoreDebug | PASS |
| :app:testPlaystoreDebugUnitTest | PASS 389/389 |
| :app:lintPlaystoreDebug | PASS |
| :app:assemblePlaystoreRelease | PASS (previous commit aca4c38; layout-only change since) |
| static_check_2_2_0.py | PASS |
| check_no_fixture_leak_v51.py | PASS |
| verify_hotfox_2_2_0.sh | PASS |

## Interaction (emulator GUI)
- 02 primary «У меня уже есть подписка» → HotfoxHttpsImportActivity; no FATAL EXCEPTION. PASS
- 03 «Использовать AUTO» → onboarding «Всё готово»; Back → 03; «Выбрать вручную» → real Servers picker (MainActivity). No VPN started (no tun0). PASS
- 04 «Добавить» with non-secret invalid input stays on screen, no crash. PASS
- 04 «Вставить»: NOT EXECUTED — ADB cannot set clipboard on API 34 in this environment.
