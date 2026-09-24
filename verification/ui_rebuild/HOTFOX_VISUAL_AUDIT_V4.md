# HOTFOX — VISUAL AUDIT V4

Дата: 2026-09-11
HEAD на старте pass: `6dceaad5aa5aaf19daba7a6d04c9823df0e20c66`
Baseline captures: `verification/ui_rebuild/baseline_v4/actual/` (02, 05, 10, 12, 13, 14, 16, 17, 18)

Этот аудит **не смягчает** findings из `HOTFOX_DESIGN_REVIEW_FAIL_V4.md`. Текущий UI на старте pass:

- 9/18 real emulator captures
- 0/18 visual/pixel PASS
- UI НЕ принят

## VERDICT НА СТАРТЕ PASS

Текущая реализация технически стала заметно честнее, но визуально пока НЕ соответствует заявленному HotFox premium/editorial уровню и НЕ может считаться финальной.

Это не “мелкая полировка”. Нужен отдельный visual refinement pass.

Главные проблемы (сохранены полностью):
- UI слишком похож на обычный Material/utility Android app, на который наложили брендовые цвета и ассеты;
- typography/spacing/grid не доведены до референса;
- cards и pill-CTA используются слишком шаблонно;
- визуальная иерархия часто рыхлая: много случайного пустого пространства, затем слишком плотные блоки;
- нижняя навигация чрезмерно тяжёлая и не всегда логична по экрану;
- системные бары оформлены непоследовательно;
- присутствуют запрещённые owner-ом planet/orbit/celestial мотивы;
- часть экранов визуально не помещается, content режется bottom navigation;
- на экранах видны scrollbars, которых в финальной композиции быть не должно;
- реальные скриншоты есть только для 9/18;
- 0/18 screens имеют подтверждённый pixel/geometry pass.

## GLOBAL DESIGN CRITIQUE (из FAIL V4, без даунгрейда)

### 1. Это пока “asset skin”, а не собранная дизайн-система
Новый kit подключён, но компоновка слишком часто остаётся универсальной:
header → 32sp title → body → large art/card → 56dp pill button → 64dp card rows.
Из-за этого разные экраны выглядят как один Android template, а не как специально выстроенные editorial compositions.

### 2. Typography oversized and mechanical
`HotFox.ScreenTitle = 32sp` используется слишком глобально.
На 1080×2400 это даёт тяжёлые заголовки, которые съедают композицию и спорят с брендом.

### 3. Cream palette потеряна
Визуально основная типографика часто выглядит почти чисто белой.

### 4. Orange слишком “app-store CTA”
CTA сейчас яркие, широкие, одинаковые и часто становятся самым громким элементом экрана.

### 5. Cards слишком много
Approved direction — content over containers, minimal cards.

### 6. Grid/vertical rhythm не собраны
Универсальные 20dp и 64dp создают формальную консистентность, но не композиционную.

### 7. System bars inconsistent
На части captures status-bar icons/time выглядят чёрными на почти чёрном фоне.

### 8. Bottom navigation is too heavy
Четырёхпунктовая навигация визуально высокая; sometimes она перекрывает scroll content.

### 9. Scrollbars и clipping
Visible vertical scrollbars на 05/13 — production-polish failure.
Также контент оказывается под bottom navigation.

### 10. Planet / celestial motifs всё ещё живы
Это P0 design violation относительно текущей owner directive.
На экранах 02 и 05 виден glowing planetary arc.
В коде реально используется `@drawable/hf_fox_planet`.
На 17 используются `hf_shadow_orbits`.

## SCREEN-BY-SCREEN (из FAIL V4)

Screen 02 — planet arc dominates; fox должна быть чистым hero artwork без планетарного объекта.
Screen 05 — запрещённый planet arc; note/rows clip; scrollbar; VPN permission hierarchy.
Screen 10 — фильтры и `Нет соединения` ломают ритм; пустота.
Screen 12 — unavailable state выглядит как debug, disabled CTA грязно-коричневый.
Screen 13 — Material/settings; scrollbar; privacy row hidden behind nav.
Screen 14 — тяжёлый card; CTA dominates.
Screen 16 — generic switch/card.
Screen 17 — `hf_shadow_orbits` celestial.
Screen 18 — generic shield/card hierarchy.

Missing captures: 01, 03, 04, 06, 07, 08, 09, 11, 15.

## P0 ACTIONS THIS PASS

1. Удалить production refs на `hf_fox_planet` и `hf_shadow_orbits`.
2. Light system-bar icons на всех dark screens.
3. scrollbars=none + bottom padding, чтобы 05/13 не резали контент.
4. Typography scale: DisplayTitle 32 / ScreenTitle 28 / RowTitle 16 / Body 15 / Nav 11.
5. Cards → transparent/hairline grouping на 13/14/16/17/18.
6. Debug-only screenshot harness для 18 presentation states.
7. Deterministic crop + 18 actual + compare artifacts.

OWNER_OVERRIDE_CELESTIAL_REMOVAL: если approved board всё ещё содержит планетарную дугу, её нельзя воспроизводить.
