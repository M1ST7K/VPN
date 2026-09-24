# HOTFOX — DESIGN REVIEW V4 — FAIL / REQUIRES MAJOR VISUAL REFINEMENT

Дата: 2026-09-11
Основание: реальные emulator screenshots, присланные после asset-first rebuild, плюс текущие `verification/ui_rebuild/actual/*`, `HOTFOX_PIXEL_DIFF_RESULTS.csv` и исходные approved reference boards.

## VERDICT

Текущая реализация технически стала заметно честнее, но визуально пока НЕ соответствует заявленному HotFox premium/editorial уровню и НЕ может считаться финальной.

Это не “мелкая полировка”. Нужен отдельный visual refinement pass.

Главные проблемы:
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

## GLOBAL DESIGN CRITIQUE

### 1. Это пока “asset skin”, а не собранная дизайн-система
Новый kit подключён, но компоновка слишком часто остаётся универсальной:
header → 32sp title → body → large art/card → 56dp pill button → 64dp card rows.
Из-за этого разные экраны выглядят как один Android template, а не как специально выстроенные editorial compositions.

### 2. Typography oversized and mechanical
`HotFox.ScreenTitle = 32sp` используется слишком глобально.
На 1080×2400 это даёт тяжёлые заголовки, которые съедают композицию и спорят с брендом.
Нельзя использовать один размер просто потому, что token так сказал. Tokens — baseline, reference — truth.

Нужно:
- измерить каждый approved screen;
- разделить display/title/subtitle/section/row/caption/nav;
- контролировать weight, tracking, line height, baseline;
- убрать ощущение стандартного Roboto/Material hierarchy;
- сохранить Android native text, но подобрать точные параметры.

### 3. Cream palette потеряна
Визуально основная типографика часто выглядит почти чисто белой.
Это делает UI резче, дешевле и больше похожим на стандартный Material dark theme.
HotFox должен быть мягче: warm cream + warm muted grey + restrained orange.

### 4. Orange слишком “app-store CTA”
CTA сейчас яркие, широкие, одинаковые и часто становятся самым громким элементом экрана.
Оранжевый должен быть точным акцентом, а не универсальным способом сделать любой экран “брендовым”.

### 5. Cards слишком много
Approved direction — content over containers, minimal cards.
Текущая реализация часто собирает весь смысл в большие rounded rectangles.
Это утяжеляет UI и делает его более типичным Android settings/dashboard.

### 6. Grid/vertical rhythm не собраны
Универсальные 20dp и 64dp создают формальную консистентность, но не композиционную.
В ряде экранов:
- сверху всё сжато;
- середина пустая;
- снизу снова тяжёлый блок;
- hero artwork механически центрирован;
- CTA не связан с контентом ритмом.

### 7. System bars inconsistent
На части captures status-bar icons/time выглядят чёрными на почти чёрном фоне.
На других — белыми.
Для dark HotFox это недопустимо.
Системные иконки должны быть consistently light на всех dark screens.

### 8. Bottom navigation is too heavy
Четырёхпунктовая навигация:
- визуально высокая;
- иконки крупные;
- активный orange слишком громкий;
- labels мелкие и слабые;
- sometimes она перекрывает scroll content;
- не должна механически присутствовать на каждом screen, если exact reference её не показывает.

### 9. Scrollbars и clipping
Visible vertical scrollbars на 05/13 — production-polish failure.
Также контент оказывается под bottom navigation.
Scroll функционально разрешён, но scrollbar не должен попадать в visual reference, если его нет в reference.
Нужен correct bottom inset/padding, а не overlay поверх контента.

### 10. Planet / celestial motifs всё ещё живы
Это P0 design violation относительно текущей owner directive.
На экранах 02 и 05 виден glowing planetary arc.
В коде реально используется `@drawable/hf_fox_planet`.
На 17 используются `hf_shadow_orbits`.
Owner directive выше старого reference:
НЕ использовать planet/globe/orbit/celestial horizon/eclipse как декоративный motif.
`hf_fox_planet` запрещён для production.
`hf_shadow_orbits` в текущем виде запрещён.
Если в старом board это было — это deliberate owner override, а не повод вернуть motif.

## SCREEN-BY-SCREEN CRITIQUE

### Screen 02 — Onboarding Connect
- planet arc dominates and is explicitly forbidden;
- artwork слишком тяжёлое и съедает editorial negative space;
- headline/body выглядят обычным onboarding template;
- CTA слишком generic;
- secondary action слишком слабый и выглядит случайной ссылкой;
- visual balance слишком bottom-heavy;
- fox должна быть чистым hero artwork без планетарного объекта;
- button/text/art spacing нужно измерить относительно reference и затем адаптировать под owner override.

### Screen 05 — Disconnected
- запрещённый planet arc;
- hero слишком мал относительно title/card и механически центрирован;
- огромный разрыв между header/hero/CTA не выглядит intentional;
- `Нет разрешения VPN` висит отдельной технической строкой без правильной hierarchy;
- primary CTA слишком generic и dominates;
- rows card тяжёлый, выглядит settings-dashboard;
- long subtitles обрезаются;
- следующий block снизу partially hidden behind nav;
- visible scrollbar;
- settings icon oversized/heavy;
- status bar contrast incorrect on capture;
- lower navigation too tall and too heavy.

### Screen 10 — Servers
- огромная пустота под единственной строкой выглядит как незавершённый экран;
- filters слишком мелкие относительно 32sp title;
- `Нет соединения` переносится на две строки — ломает ритм и выглядит случайно;
- `Мои серверы` как centered web-tab + underline ощущается как другой visual language;
- server title обрезан уже на единственной строке;
- generic globe icon не даёт premium feel;
- empty/truthful runtime state можно сохранить, но empty composition должна выглядеть намеренно;
- bottom nav снова визуально тяжелее самого content.

### Screen 12 — Subscription
- большая часть экрана визуально пустая и не используется композиционно;
- disabled buy CTA имеет грязно-коричневый цвет и плохую читаемость;
- hierarchy между Premium title, unavailable state, explanation, actions слабая;
- много tiny grey text;
- экран выглядит как “backend unavailable debug state”, а не premium product state;
- runtime truth нужно сохранить, но presentation unavailable-state должна быть polished и intentional.

### Screen 13 — Settings
- самый очевидный Material/settings screen;
- слишком тяжёлые rounded cards;
- title oversized;
- back + HotFox header + giant title дают тройную конкуренцию;
- section labels слишком мелкие;
- row density и card radius generic;
- right scrollbar visible;
- нижний content режется bottom navigation;
- bottom nav visually dominates;
- icon stroke weights inconsistently feel;
- privacy row partially hidden — hard layout defect.

### Screen 14 — Smart Routing
- один тяжёлый card container вместо лёгкой editorial структуры;
- subtitle слишком широкий и ломается неаккуратно;
- current switch выглядит как отдельный generic control language;
- CTA слишком мощный по сравнению с content;
- lower half dead empty;
- визуально screen чувствуется как форма настроек, а не premium routing control.

### Screen 16 — Autopilot
- switch слишком большой и похож на generic iOS/Material hybrid;
- card опять задаёт всю композицию;
- title/subtitle/row hierarchy слишком стандартная;
- `Нет разрешения VPN`/service note выглядит технически, а не product-level;
- CTA again generic;
- нужно больше typography-first hierarchy и меньше container-first thinking.

### Screen 17 — Shadow
- `hf_shadow_orbits` visually resembles orbital/celestial decoration and violates owner direction;
- shield + orbit graphic слишком “cyber/security illustration”;
- product specifically forbids cyberpunk/gamer/celestial drift;
- card и CTA generic;
- большая нижняя пустота;
- Shield itself can remain, but supporting lines must read as abstract network/routing paths, not planetary orbits.

### Screen 18 — Always-on VPN
- shield слишком generic;
- hierarchy card → CTA → tiny note стандартная;
- huge unused lower area;
- subpage chrome/bottom-navigation behavior must be checked against exact reference, not normalized globally;
- card should feel lighter and more editorial;
- status `Неизвестно` is truthful but its visual treatment needs to be deliberate.

## MISSING EVIDENCE

Current final report explicitly says:
- 9/18 real emulator captures;
- 0/18 pixel PASS;
- no overlay/diff golden pass.

Missing captures:
01 Splash
03 Onboarding AUTO
04 Onboarding Ready
06 Connecting
07 Protected
08 Add Connection Sheet
09 HTTPS Subscription
11 Server Details
15 Apps & Rules

This is not acceptable as final UI QA.

## REQUIRED NEXT PASS

The next agent run must:
1. Treat the asset kit as a parts bin, NOT as higher visual authority than approved references or owner overrides.
2. Remove forbidden celestial artwork from production.
3. Rebuild typography and geometry based on measured screenshots, not generic tokens.
4. Reduce card-heavy Material look.
5. Fix system bars, bottom insets, clipping, scrollbars and nav weight.
6. Create a DEBUG-ONLY screenshot harness to render all 18 presentation states without changing production VPN truth.
7. Capture 18/18.
8. Produce side-by-side + 50% overlay + diff for every screen.
9. Iterate until each screen receives explicit manual/automated visual status.
10. Keep RELEASE READY = NO until separate runtime release validation is executed.
