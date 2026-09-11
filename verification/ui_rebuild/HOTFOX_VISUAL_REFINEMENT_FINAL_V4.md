# HOTFOX — VISUAL REFINEMENT FINAL V4

Дата: 2026-09-11  
Ветка: `cursor/hotfox-ui-pixel-lock-rebuild`  
База pass: `6dceaad5aa5aaf19daba7a6d04c9823df0e20c66`  
Код capture-fix: `8b83780`  
Merge: **не выполнялся**  
RELEASE READY: **NO**  
Physical device VPN E2E: **NO / NOT EXECUTED**  
Runtime VPN datapath E2E: **NOT EXECUTED / deferred**

## VERDICT

Corrective visual pass по `design/HOTFOX_CURSOR_VISUAL_REFINEMENT_MASTER_V4.txt` доведён до **18/18 реальных emulator captures** через debug-only harness. Сравнение с нормализованными reference boards есть для всех 18 экранов.

**Visual/pixel PASS: 0/18.** Mean abs diff 14–34. Геометрия и editorial hierarchy всё ещё не равны approved boards. UI ближе к owner overrides (без планеты/орбит, светлые system icons, без clip note/privacy под nav), но **не принят как pixel-perfect**.

Постановка `CONNECTED` / `Защищено` в production по-прежнему только после real readiness. Экран 07 — **debug presentation fixture**, не production VPN truth.

## BUILD / TESTS

| Check | Result |
| --- | --- |
| `:app:assemblePlaystoreDebug` | PASS |
| `:app:testPlaystoreDebugUnitTest` | PASS **365/365** |
| `verification/static_check_2_2_0.py` | PASS |
| Emulator | AVD `hotfox34`, 1080×2400, night mode, x86_64 APK |
| Android lint | NOT EXECUTED as a separate gate |

Локальный reconstruct на физическом устройстве: **NOT EXECUTED**.

## CAPTURE / COMPARE

- Normalized references: `verification/ui_rebuild/reference_v4/01.png`…`18.png` (1080×2400)
- Actuals: `verification/ui_rebuild/actual_v4/01.png`…`18.png` — **18/18 CAPTURED**
- Side-by-side / overlay 50% / absdiff: `verification/ui_rebuild/compare_v4/`
- CSV: `verification/ui_rebuild/HOTFOX_PIXEL_DIFF_RESULTS_V4.csv`
- Missing capture: **0**

Harness: `src/debug` `HotfoxUiScreenshotHarnessActivity` + `HotfoxUiVisualOverride` (write только из debug). Production CONNECTED не пишется. Leak test: `HotfoxUiScreenshotFixtureLeakTest`.

## CELESTIAL

Forbidden production refs: **`hf_fox_planet` = 0, `hf_shadow_orbits` = 0**.  
Подробности: `CELESTIAL_ASSET_USAGE_AFTER_V4.txt`.  
02/05/07: fox-only (`hf_fox_bust`), без дуги/горизонта/затмения.  
17: `hf_native_network_topology` + shield.  
03 AUTO globe/orbits сохранены как продуктовая AUTO-иллюстрация (OWNER_OVERRIDE не запрещает этот экран).

## CHROME / INSETS

- Dark HotFox: светлые status/navigation icons (`windowLightStatusBar/NavigationBar = false`) — на всех 18 кадрах иконки светлые.
- Visible production scrollbars на 05/13: **0** (`scrollbars=none`).
- Clipping note 05 под nav: **не воспроизводится** на актуальном кадре.
- Privacy 13 под nav: **не воспроизводится**; row виден над bottom nav.
- 07 nav conflict (Соединение muted + Серверы orange): **исправлен** — активен пункт Соединение.
- 08: native sheet поверх живого disconnected chrome, scrim ~52%, не сплошной чёрный фон.

## PER-SCREEN STATUS

Все: `CAPTURED` + `COMPARED` + **NOT pixel PASS**.

| ID | Экран | Visual notes | Pixel |
| --- | --- | --- | --- |
| 01 | Splash | Fox-only, hold harness | FAIL |
| 02 | Onboard connect | Нет планеты; secondary «Купить доступ» | FAIL |
| 03 | Onboard AUTO | Globe + orbits (разрешено) | FAIL |
| 04 | Onboard ready | Server-ready art | FAIL |
| 05 | Disconnected | Fox-only; note над nav | FAIL |
| 06 | Connecting visual | Stages + CTA «Подключение»; fixture | FAIL |
| 07 | Protected visual | Зелёный «Защищено»; CTA «Отключить»; fixture ≠ production CONNECTED | FAIL |
| 08 | Add sheet | Sheet + dimmed home | FAIL |
| 09 | HTTPS | Fox-only; без секретов | FAIL |
| 10 | Servers | AUTO в 2 строки; ping «Нет соединения» в одну; каталог пуст без импорта | FAIL |
| 11 | Server details | Amsterdam fixture; CTA disabled; load unknown | FAIL |
| 12 | Subscription | Unavailable catalog; disabled Buy читаемый | FAIL |
| 13 | Settings | Без scrollbar; privacy над nav; section eyebrows видны | FAIL |
| 14 | Smart routing | Typography-first rows | FAIL |
| 15 | Apps & rules | FLAG_SECURE снят только при `appsFixture`; пустой список честный | FAIL |
| 16 | Autopilot | Native rows | FAIL |
| 17 | Shadow | Topology, не orbits | FAIL |
| 18 | Always-on | Shield; Android status unknown | FAIL |

Оставшиеся визуальные gaps (не P0 celestial, не missing capture): pill-CTA всё ещё громкие; часть экранов пустее boards из-за отсутствия live subscription/servers; 10/12/15 не заполнены фейковыми серверами/приложениями; mean-abs не является auto-PASS.

## NON-REGRESSION

Не менялись VPN datapath, TLS/REALITY, DNS/IPv6 fail-closed, entitlement, Keystore, production CONNECTED gate.  
Release fixture leakage: harness не в `src/main`; static check PASS.

## МОЖНО ЛИ СЧИТАТЬ UI ПРИНЯТЫМ

Нет. 18/18 evidence есть, 0/18 pixel PASS. Следующий visual pass — по конкретным geometry gaps, не новый generic Material restyle.
