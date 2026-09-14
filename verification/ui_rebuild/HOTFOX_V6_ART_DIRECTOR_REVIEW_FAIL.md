# HOTFOX — ART DIRECTOR REVIEW V6
## Status: FAIL / 18 OF 18 CAPTURED, 0 OF 18 VISUALLY ACCEPTED

Date: 2026-09-11
Reviewed branch: `cursor/hotfox-ui-pixel-lock-rebuild`
Reviewed head: `8bdd3d09ca528ff656f9e275e33152bebc5f89c6`

Evidence reviewed:
- `verification/ui_rebuild/actual_v5_1/01.png` … `18.png`
- owner-supplied V5.1 contact sheet and enlarged captures of 05, 08, 10 and 17
- `HOTFOX_PIXEL_DIFF_RESULTS_V5_1.csv`
- `HOTFOX_ART_DIRECTOR_SCORE_V5_1.csv`
- `HOTFOX_VISUAL_REFINEMENT_FINAL_V5_1.md`

## Executive verdict

V5.1 fixed an important engineering problem: fixture data is now contained in `src/debug`, a clean emulator path exists, and 18/18 captures were finally produced. That is good QA hygiene.

Visually, however, V5.1 is still not an acceptable final HotFox UI. The implementation remains too close to a generic dark Android utility with orange brand paint. It does not yet reproduce the approved premium editorial HotFox language.

The repository itself confirms this: visual/pixel PASS is 0/18 and art-director PASS is 0/18. Mean absolute diff is still large on every screen; the worst are 15 (35.04), 09 (32.74), 16 (31.88), 14 (30.32), 18 (29.72), 12 (29.69), 05 (27.85), 17 (27.52). These numbers are not the sole design metric, but they correctly indicate that the current geometry/content differs materially from the approved boards.

## Process defects discovered in V5.1 evidence

### 1. The screenshot locale is wrong for the approved Russian reference set

The V5.1 contact sheet mixes Russian and English:
- 02: `Connect HotFox`, `I already have a subscription`, `Buy access`
- 03: `Best server — automatically`, `Use AUTO`, `Choose manually`
- 04: `All set`, `Continue`
- 09: `HTTPS subscription`, `Paste`, `Add`
- 11: `Server details`, `Status`, `Load`, `Auto-select`
- 13: `Settings`, English helper/section labels
- 14: `Smart Routing`, English helper copy
- 16: `Autopilot`, English rows
- 17: English helper/rows/button
- 18: English rows/button

Meanwhile other screens and fixtures remain Russian. A mixed-language contact sheet cannot be used as valid visual evidence against Russian approved boards.

V6 visual QA MUST run in `ru-RU`. Do not hardcode Russian into production merely to pass screenshots; set the test environment/debug capture locale correctly and use existing localized resources. Production localization behavior remains intact.

### 2. The self-score scale drifted

V5 asked for a 0–5 art-director scale; V5.1 produced scores around 5.8–6.6 on an apparent 0–10 scale, while still marking every screen `NO`. This is not a usable acceptance gate.

V6 uses a fixed 0–5 rubric only. No screen passes if any category <4.0 or average <4.3. Scores must include concrete evidence notes.

### 3. Debug QA must inject state/data, not become a second UI renderer

`HotfoxUiQaPainter` is correctly debug-only now, but it repeatedly repaints views on a timer. V6 should keep debug data/state isolation while avoiding an alternate visual implementation. Debug code may inject deterministic state/data and open screens. Typography, spacing, layout hierarchy and component geometry must be production UI, not debug-only cosmetic overrides.

---

# GLOBAL DESIGN CRITIQUE

## A. The product still reads as “dark Android app”, not premium HotFox

The visual language is still dominated by:
- default-looking Android typography;
- conventional full-width rounded orange buttons;
- standard list rows with left icon / text / right chevron;
- ordinary bottom navigation;
- heavy bordered cards;
- weak editorial rhythm.

The approved direction is content-over-containers, typography-first, warm dark editorial minimalism with restrained orange. V5.1 remains too utilitarian.

## B. Typography lacks authority and coherence

Current problems:
- several main titles are too small/ordinary while CTAs are oversized;
- helper/body copy is often tiny and low-contrast;
- section labels are microscopic on 10/13/14/15;
- row titles and row values do not share a consistent baseline/weight system;
- line breaks look incidental rather than composed;
- English capture makes the hierarchy even less comparable to the Russian reference.

Required:
- one measured editorial type matrix with display, root-title, subpage-title, body-lead, body-support, eyebrow, row-title, row-caption, value, nav-label and legal-caption roles;
- per-screen line-break lock;
- warm cream rather than stark white;
- readable muted text;
- optical baseline alignment.

## C. Orange is still overused as a large area color

Large orange pills appear on 02, 03, 04, 05, 09, 14, 15, 16, 17 and 18. This destroys accent hierarchy.

Orange should identify brand/action/state precisely, not paint every primary action identically.

V6 must establish contextual CTA hierarchy:
- onboarding primary;
- home connect/disconnect;
- subpage compact primary;
- secondary/quiet action;
- disabled;
- progress.

## D. Negative space is often dead space

03, 04, 12, 14, 16, 17 and 18 have large empty lower zones that do not create deliberate editorial tension; they look unfinished.

Negative space must have a reason: balance an artwork, anchor a CTA, create an intentional visual axis, or match the exact board. Empty space must not simply be leftover LinearLayout weight.

## E. Icon system is optically inconsistent

Gear, crown, server stack, shield, route graph, back, chevrons, nav icons and app controls do not share the same perceived stroke weight. Some are too large, others too small.

V6 needs optical size tuning, not just nominal `24dp` equality.

## F. Bottom navigation is still visually generic

The four-item nav reads like standard utility chrome:
- inactive contrast is weak;
- label size is too small;
- icon weights vary;
- active orange competes with giant orange CTAs;
- the nav sometimes dominates sparse subpages.

Exact per-screen reference decides whether it appears. Do not normalize it globally.

## G. Green usage is too broad

Screen 10 uses bright green pings. Owner direction was green only for real success/protected semantics. Latency/availability should follow the exact reference; do not make every good-looking numeric value green by default. In debug reference state, color still follows the board, not generic networking convention.

---

# SCREEN-BY-SCREEN CRITIQUE

## 01 — Splash

Good:
- fox matte is finally gone.

Problems:
- fox is too small and sits high, leaving an enormous unstructured middle void;
- wordmark and loading track form a separate bottom composition rather than one intentional brand entrance;
- visual axis is weak;
- tagline is too tiny;
- loading track looks generic.

Fix:
- measure fox bounds/position directly from approved board;
- reconnect hero and wordmark composition;
- make the progress detail thinner/subtler and board-accurate;
- keep no planet/orbit/glow.

## 02 — Onboarding Connect

Problems:
- invalid EN locale for RU board;
- title/body are too small versus the fox and CTA;
- fox floats in a large empty field;
- CTA is an overwide generic orange pill;
- secondary action is too weak and too close to the system bottom area;
- hierarchy feels template-generated.

Fix:
- capture in ru-RU;
- exact title/body sizes and line breaks from board;
- artwork placement must relate to copy and CTA;
- refine CTA width, height, radius and arrow;
- secondary action gets deliberate spacing and contrast.

## 03 — AUTO

Good:
- old globe/orbit asset is gone.

Problems:
- current topology is too skeletal and looks like a placeholder diagram;
- central icon and thin crossing lines do not feel premium or intelligent;
- huge dead area;
- invalid EN locale;
- CTA still same template pill.

Fix:
- create a refined asymmetric routing topology with a small number of precise nodes, one selected route, clear source/destination logic;
- no circles/orbits around a center;
- no “solar system” reading;
- stronger relationship among headline, explanation, visual and CTA.

## 04 — Ready

Good:
- no concentric radar rings.

Problems:
- server/check illustration is tiny compared with empty screen;
- it looks like a generic setup icon rather than a premium completion moment;
- invalid EN locale;
- permission note and CTA feel detached;
- lower half is mostly empty.

Fix:
- increase visual authority without adding orbit/radar language;
- align copy → completion art → permission note → CTA on one strong axis;
- use exact reference proportions.

## 05 — Disconnected Home

This is improved but still not final.

Problems:
- title and subtitle are still too generic/Roboto-like;
- fox is now clean but slightly too small and visually isolated;
- `Нет разрешения VPN` creates a large dead gap before CTA;
- full-width orange CTA is visually too heavy;
- main control group has a thick border and feels like a conventional settings card;
- row icons/values/chevrons create too much equal-weight noise;
- the Smart subtitle is dense and wraps heavily;
- lower informational notice is weakly structured;
- gear is optically too small relative to brand/title;
- bottom nav remains generic.

Fix:
- retune title/body/fox as a single hero cluster;
- reduce CTA mass;
- lighten the group border and separators;
- reduce row visual noise; values remain clear but quieter;
- refine the note into an editorial message, not another pseudo-card;
- tune gear/nav icon sizes optically.

## 06 — Connecting

Critical regression:
- the circular/arc progress graphic around the fox again reads like an orbit around a central object. Even if semantically “progress”, it visually reintroduces the banned celestial language.

Other problems:
- too much information simultaneously: title, fox/ring, stage list, progress control, settings group, nav;
- stage labels are tiny;
- hierarchy is noisy;
- controls beneath the active connection process distract from the state.

Fix:
- NO circular orbit around fox with satellites/dots;
- use non-celestial progress: segmented route line, asymmetric progress path, short status rail, or restrained partial progress element not enclosing the fox;
- connection process becomes the dominant story;
- mute or hide nonessential home controls during connecting if reference permits.

## 07 — Protected

Critical state contradiction:
- screenshot says `Защищено`, but also shows `Нет разрешения VPN` under the fox. A protected visual fixture must not present contradictory permission copy.

Other problems:
- fox is too small;
- metric labels and route detail are tiny;
- lower home controls remain dense;
- success state does not feel calmer than disconnected state.

Fix:
- debug-only presentation must hide/replace the permission warning while NOT changing real Android VPN permission;
- production truth remains untouched;
- enlarge/clarify only the important success metrics;
- simplify lower hierarchy;
- green remains limited to actual/debug success presentation.

## 08 — Add Connection Bottom Sheet

Improved but still too generic.

Problems:
- title is oversized relative to rows;
- row icons are too large;
- rows still feel tall;
- close button is too visually loud;
- sheet surface and border radius are standard Material-like;
- scrim suppresses the underlying context too strongly;
- no fine separators/typographic rhythm.

Fix:
- more compact title and row geometry;
- smaller visible icons inside 48dp touch targets;
- thinner separators or deliberate spacing;
- quieter close icon and handle;
- lighter, more elegant scrim;
- exact reference height and radius.

## 09 — HTTPS Subscription

One of the worst current screens (MAD 32.74).

Problems:
- invalid EN locale;
- workflow is visually tiny at top while lower screen is mostly empty;
- fox sits as a small decorative object with no compositional connection to form;
- input is generic and cramped;
- `Paste` and action hierarchy look utilitarian;
- CTA is again an identical orange pill.

Fix:
- ru-RU capture;
- task-first composition;
- stronger form field hierarchy and error/focus states;
- if fox exists in exact board, integrate it intentionally; otherwise remove it from this screen;
- do not expose real URLs/tokens.

## 10 — Servers

This is one of the better V5.1 screens, but not final.

Problems:
- title/filter/tab stack consumes too much vertical space;
- `Нет соединения` is too loud next to filters;
- recommended row icon/dot and underline create several competing orange accents;
- ping green may violate restricted-success color semantics and must follow exact board;
- server rows still look like ordinary preference/list rows;
- row density and typography need board matching;
- flags/pings/chevrons need baseline refinement.

Fix:
- exact board tabs/filter geometry;
- one dominant orange selection indicator, not several unrelated accents;
- pings use board-approved color semantics;
- refine row rhythm and flag/value alignment;
- fixture remains debug-only.

## 11 — Server Details

Problems:
- invalid EN locale and mixed-language content;
- title/body/values are too small;
- the page is a sparse list with weak hierarchy;
- values on the right are nearly invisible;
- disabled CTA is muddy and unclear;
- too much empty lower space;
- bottom nav presence must match exact reference, not global policy.

Fix:
- ru-RU capture;
- stronger title/value hierarchy;
- consistent right-value column;
- deliberate row grouping;
- clear disabled/selected server action state;
- debug data only in debug source.

## 12 — Subscription

Problems:
- large unused lower half;
- card is serviceable but visually generic;
- tiny metadata and masked URL are too low-contrast;
- main orange CTA still oversized;
- secondary action looks like another generic button;
- hierarchy between plan, expiry, device count, URL, main action and secondary action is weak.

Fix:
- match exact board state and proportions;
- plan/entitlement becomes clear focal block;
- reduce CTA visual mass;
- improve information hierarchy while preserving truth in production and debug-only fixture in QA.

## 13 — Settings

Problems:
- invalid EN locale for RU board;
- typography is tiny and compressed;
- rows are visually raw, like a stock Android settings list;
- section labels are too small;
- icons have uneven optical weight;
- chevrons float far right without enough structural relation;
- huge empty lower area after the list;
- nav is tiny relative to rest of UI.

Fix:
- ru-RU capture;
- slightly denser than V4 but not microscopic;
- subtle grouping via hairlines/spacing, no giant cards;
- consistent icon box and row baseline;
- exact section heading scale.

## 14 — Smart Routing

One of the weakest screens (MAD 30.32).

Problems:
- invalid EN locale and mixed-language row copy;
- content cluster is tiny and top-heavy;
- switch dominates one row;
- full-width orange CTA is disproportionate to the small amount of content;
- enormous empty lower zone;
- reads as stock settings rather than a routing control center.

Fix:
- ru-RU capture;
- compose mode selector, apps/rules, LAN as a purposeful routing hierarchy;
- tighter but more confident typography;
- contextual compact action;
- exact reference selected state.

## 15 — Apps & Rules

Worst pixel diff in V5.1 (35.04).

Problems:
- visible vertical scrollbar in capture is unacceptable unless exact board shows it;
- fixture app icons are effectively blank placeholder squares, making the list look unfinished;
- rows are too small/dense;
- search and segmented control look generic;
- custom rules block and app list do not share one grid;
- selected checkboxes/squares look crude;
- bottom save CTA crowds the list;
- visual language is far from premium.

Fix:
- hide scrollbars for visual/product design unless reference explicitly shows them;
- debug fixture must use deterministic, safe, visually complete icon glyphs/assets, not blank ColorDrawables;
- production continues to use real app icons;
- refine search/segment/row/check states;
- ensure list has correct padding above sticky/bottom action;
- no clipping.

## 16 — Autopilot

Weakest art-director score in V5.1.

Problems:
- invalid EN locale;
- sparse generic list;
- switch is oversized and visually heavy;
- helper/status line is tiny;
- CTA is too wide and bright;
- large dead lower area.

Fix:
- ru-RU capture;
- compact rule hierarchy;
- smaller visible switch while preserving touch target;
- contextual CTA;
- use space to clarify trusted/unknown network behavior if reference includes it, otherwise tighten composition.

## 17 — Shadow

Critical: visually, the forbidden orbit aesthetic is still back.

The new curves no longer literally form closed ellipses, but the composition is still a central shield encircled by curved bands and dots. It reads as orbital/celestial around a central body.

Other problems:
- invalid EN locale;
- shield is thick/heavy;
- curved lines dominate more than the feature information;
- two rows float with little structure;
- CTA is oversized;
- huge dead lower half.

Fix:
- remove all encircling curves around shield;
- use angular/asymmetric network routing paths, branching lines, nodes, directional segments or side-fed topology;
- no path may visually orbit the shield;
- shield is calmer/thinner;
- more deliberate row grouping;
- compact CTA.

## 18 — Always-on VPN

Problems:
- invalid EN locale;
- `Unknown` values make the board visually weak; exact reference state must decide QA labels;
- shield/rows/button are isolated islands;
- full-width orange CTA dominates;
- huge empty lower field;
- nav is visually detached.

Fix:
- ru-RU capture;
- debug visual state may paint safe deterministic labels ONLY if exact board requires them; never modify Android secure setting;
- rebalance shield/rows/action vertically;
- quieter CTA;
- exact nav presence and state.

---

# V6 ACCEPTANCE

V6 is accepted only if all are true:

1. Capture environment is locked to Russian (`ru-RU`) for the approved Russian reference set.
2. 18/18 actual screenshots exist from the current candidate APK.
3. 18/18 side-by-side, overlay and diff evidence exists.
4. 0 screens show mixed-language QA evidence.
5. 0 screens contain planet/orbit/celestial visual language, including 06 and 17.
6. 07 has no contradictory `Защищено` + `Нет разрешения VPN` presentation.
7. 15 has no blank placeholder app icons and no unintended scrollbar.
8. CTA hierarchy is contextual; not every page uses the same giant orange pill.
9. Typography and spacing are measured against each board.
10. Debug QA injects data/state only; production geometry is what is being captured.
11. Art-director score uses the fixed 0–5 rubric; every category >=4.0, average >=4.3.
12. Build/unit/static/fixture-leak gates pass.
13. No VPN/security/runtime semantics are changed.
14. Pixel-perfect and RELEASE READY are not claimed unless their separate gates genuinely pass.
15. Do not merge automatically.
