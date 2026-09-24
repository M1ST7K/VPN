# HOTFOX — VISUAL REFINEMENT FINAL V5

Дата: 2026-09-11  
Ветка: `cursor/hotfox-ui-pixel-lock-rebuild`  
Код V5: `f7b425c` (transparent fox, no-orbit AUTO/Ready, reference fixtures)  
Этот отчёт: matrices / asset QA / emulator blocker  
Merge: **не выполнялся**  
RELEASE READY: **NO**  
Physical device VPN E2E: **NO / NOT EXECUTED**  
Runtime VPN datapath E2E: **NOT EXECUTED / deferred**

## VERDICT

V5 art-director pass **реализован в коде** (P0 matte, запрет orbit на 03/04, debug reference-state fixtures, typography/CTA/chrome).  

**18/18 V5 emulator captures: FAIL / NOT_CAPTURED.**  
**18/18 comparisons: FAIL.**  
**Visual/pixel PASS: 0/18.**  
UI **не принят**. Pixel-perfect **не утверждается**.

Эмулятор AVD `hotfox34` в этой cloud-среде зависает после строки `Activated packet streamer for bluetooth emulation`: `adb devices` пустой, RSS qemu ≈295 MB, CPU ≈0.3%, окна Android Emulator на DISPLAY `:1` нет. Это **объективный hard blocker** для visual evidence, не «production empty catalog».

Постановка production `CONNECTED` / `Защищено` по-прежнему только после real readiness. Экран 07 — **debug presentation fixture**.

## BUILD / TESTS (код, до зависания эмулятора)

| Check | Result |
| --- | --- |
| `:app:assemblePlaystoreDebug` | PASS (APK `HotFox_Proxy_2.2.0_x86_64.apk`, 2026-09-11 17:03) |
| `:app:testPlaystoreDebugUnitTest` | PASS **365/365** на коммите реализации |
| `verification/static_check_2_2_0.py` | PASS (needles planet/orbits/opaque bust) |
| V5 emulator captures | **NOT EXECUTED / BLOCKED** |
| Android lint | NOT EXECUTED as a separate gate |
| Physical reconstruct | NOT EXECUTED |

## FOX ALPHA

Ресурс: `drawable-nodpi/hf_fox_bust_transparent.png`.

| Metric | Value |
| --- | --- |
| alpha=0 | 78.61% |
| alpha=255 | 20.44% |
| partial 1–254 | 0.96% |
| dark FG alpha&lt;220 | 0.92% (порог reject 1.5% — не превышен) |

Диагностика: `verification/ui_rebuild/v5_assets/`. Прямоугольного matte на checkerboard нет. Тёмная шерсть внутри непрозрачная. Не повторяли flood-fill, который проедал мех.

## AUTO / READY — NO ORBIT

- 03: `hf_native_auto_routing` — hub + асимметричные кандидаты, один выделенный path. Не solar-system.
- 04: `hf_native_ready_complete` — серверы + check. Не radar.
- 17: topology только во внешней зоне, shield 112×104dp.

Production refs: `hf_fox_planet` = 0, `hf_shadow_orbits` = 0, `hf_auto_orbits` = 0, `hf_ready_orbits` = 0.

## REFERENCE-STATE FIXTURES

Манифест: `verification/ui_rebuild/REFERENCE_STATE_MANIFEST_V5.json`.

10/11/12/15 — debug-only данные (`HotfoxUiReferenceFixtures`, `src/debug`). Не пишут Mmkv / CommercePreferences / installed-app store. Leak test расширен.

## CAPTURE BLOCKER (команда / ошибка / файл)

Последняя успешная загрузка этого AVD в этой VM: V4, `Boot completed in 41349 ms` (tmux `hotfox-emulator`, ~15:08). После SIGKILL зависших qemu и `-wipe-data` гостевая userdata пересоздана.

Повторяемый симптом:

1. Команда (каноническая V4, DISPLAY=:1, KVM usable):
   `emulator -avd hotfox34 -gpu swiftshader_indirect -no-audio -no-boot-anim -no-snapshot-load -no-snapshot-save -netdelay none -netspeed full`
2. Лог: `/tmp/hotfox34-emu.log` — последняя строка `INFO | Activated packet streamer for bluetooth emulation`
3. `adb devices` — пусто; `127.0.0.1:5555` иногда `offline`
4. RSS qemu ≈ 294–296 MB, CPU падает до 0.3%
5. X11: только `Qt Selection Owner for qemu-system-x86_64` 3×3, окна эмулятора нет

Проверенные варианты, тот же hard-stop:

- `-no-window` / qemu-system-x86_64-headless
- QT window без `-no-window`
- `-wipe-data` + `fastboot.forceColdBoot=yes`, `firstboot.bootFromDownloadableSnapshot=no`
- `XDG_RUNTIME_DIR`, `-feature -Uwb -NetsimWebUi -WiFiPacketStream -VirtioWifi`
- `-qemu -bt hci,null`
- standalone `netsimd --grpc-port 9552` + `-packet-streamer-endpoint 127.0.0.1:9552`  
  netsimd: `AddChip hotfox34` затем `sink_loop` без продолжения boot

Это не обходится debug harness: harness требует `adb` и живой guest.

## ART-DIRECTOR SCORE

`verification/ui_rebuild/HOTFOX_ART_DIRECTOR_SCORE_V5.csv` — все экраны **NOT_CAPTURED**. Категории не накручивались. Pass-rule (нет категории &lt;4, avg≥4.3) **не выполнено**, потому что нет кадров.

## UNRESOLVED

- 18/18 V5 screenshots
- 18/18 side-by-side / overlay / diff
- pixel-perfect
- runtime VPN E2E
- physical device

Повторить capture на среде, где AVD снова доходит до `Boot completed` (как в V4), той же командой `python3 verification/ui_rebuild/capture_harness_v5.py`.

## PRODUCTION TRUTH

VPN datapath / security / entitlement / readiness gate **не менялись**. Fixture 07 ≠ production CONNECTED.
