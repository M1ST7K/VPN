# HOTFOX V12 — NO-OVERLAP HOME ARCHITECTURE REBUILD

## OWNER DIRECTIVE — THIS OVERRIDES PRIOR HOME/HERO GEOMETRY INSTRUCTIONS

This is the active owner directive for the HotFox Home screen.

Where this document conflicts with older HotFox UI prompts, hero-positioning notes, per-state offsets, V8/V9/V10/V11 geometry experiments, screenshot-tuning values, or Cursor rules, **THIS DOCUMENT WINS**.

Do not continue tweaking the current broken composition by another 10–40dp. The current defect is architectural, not cosmetic.

The current Home implementation repeatedly allows independent UI layers to occupy the same pixels. The fox, planet, status UI, progress UI, CTA, traffic statistics, quick-settings card, helper copy, and bottom navigation have been positioned as partially independent overlay systems. That is why each new state fixes one collision and creates another.

V12 replaces that strategy with a single shared non-overlapping layout scaffold.

---

# 0. HARD SCOPE BOUNDARY

This task is **UI architecture only**.

DO NOT change:

- VPN engine;
- Xray / HEV / tun2socks integration;
- `VpnService` behavior;
- routing semantics;
- DNS behavior;
- IPv6 behavior;
- kill-switch / fail-closed behavior;
- server selection business logic;
- AUTO selection logic;
- subscription parsing;
- entitlement logic;
- backend contracts;
- repository/network/data layers;
- secret handling;
- connection state truthfulness;
- public-IP verification;
- security guarantees;
- release-gate semantics.

Preserve the current runtime-repair baseline and every technical guarantee already documented in the repository.

Do not fake CONNECTED state for screenshots.

Do not add secrets, URLs, UUIDs, private keys, tokens, or credentials to source, logs, screenshots, fixtures, or prompt files.

---

# 1. CURRENT FAILURE — UNDERSTAND IT BEFORE EDITING

The present Home screens are visually unacceptable because functional blocks overlap one another and/or overlap the hero artwork in uncontrolled ways.

Observed failure pattern:

- Disconnected: hero art, permission message, Connect CTA, quick-settings card, helper copy, and bottom navigation compete for the same lower-middle area.
- Connecting: progress graph and textual steps are drawn over the fox/planet; Stop CTA is squeezed between progress and the settings card; the card begins too high.
- Connected: download/upload statistics and the route diagram are placed on top of the hero; Disconnect CTA and settings card compete for vertical room.
- Long quick-setting subtitles wrap into adjacent rows.
- State changes cause different content heights, which move subsequent elements and create new collisions.
- Bottom navigation is visually too close to or invaded by content/art.
- Previous fixes used `offset`, absolute fractions, z-index ordering, or state-specific hero transforms. Those are the source of instability.

Do not solve this with another hero `offset` tweak.

Do not solve this by making the fox tiny.

Do not solve this by hiding controls behind opaque rectangles.

Do not solve this by letting elements overlap and then adjusting z-order.

Rebuild the Home layout architecture.

---

# 2. NON-NEGOTIABLE INVARIANT: FUNCTIONAL UI MUST NEVER OVERLAP

For the Home screen, functional UI regions must occupy mutually exclusive vertical layout regions.

Conceptually:

`HeaderStatus ∩ HeroSlot = empty`

`HeroSlot ∩ StatePanelSlot = empty`

`StatePanelSlot ∩ QuickSettingsSlot = empty`

`QuickSettingsSlot ∩ FooterInfoSlot = empty`

`FooterInfoSlot ∩ BottomNavSlot = empty`

If FooterInfo collapses on a short display, then:

`QuickSettingsSlot ∩ BottomNavSlot = empty`

The only intentionally layered content is decorative artwork **inside `HeroSlot` itself**:

- background;
- planet;
- fox;
- local fade/glow.

Functional controls are not allowed to float across slot boundaries.

---

# 3. FORBIDDEN IMPLEMENTATION PATTERNS

For functional Home UI, do not use any of the following as a positioning strategy:

- `Modifier.offset(...)`;
- `absoluteOffset(...)`;
- negative offsets;
- negative padding tricks;
- `graphicsLayer { translationX = ... }`;
- `graphicsLayer { translationY = ... }`;
- z-index hacks to hide collisions;
- hard-coded Y positions for buttons/cards/progress/stats;
- screen-state-specific absolute coordinates;
- fixed screenshot pixel coordinates;
- placing all Home controls in one `Box(fillMaxSize())` and manually positioning each control;
- placing progress/statistics directly on top of the fox;
- placing cards on top of the fox because the card is translucent;
- positioning bottom navigation as a floating overlay above the rest of the screen;
- state-dependent hero position or scale changes used to make room for controls.

`offset` may be used only for tiny local visual corrections inside a bounded component where it cannot cross the component's measured bounds. It must never determine Home region placement.

If current code contains these techniques for Home region placement, remove/refactor them instead of stacking another workaround on top.

---

# 4. BUILD ONE SHARED HOME SCAFFOLD

Disconnected, Connecting, and Connected are not three separately-positioned screens.

They are three states of **one shared scaffold**.

Implement or refactor toward an architecture conceptually equivalent to:

```kotlin
@Composable
fun HotFoxHomeScaffold(
    headerStatus: @Composable () -> Unit,
    hero: @Composable () -> Unit,
    statePanel: @Composable () -> Unit,
    quickSettings: @Composable () -> Unit,
    footerInfo: @Composable () -> Unit,
    bottomNavigation: @Composable () -> Unit,
)
```

The outer layout must use real measured constraints, e.g. `BoxWithConstraints`, a `Column`, or a small custom `Layout` with deterministic slot allocation.

The important point is not the function name. The important point is that every major Home region gets measured and placed once, in normal layout flow, without overlaps.

Recommended hierarchy:

```text
Root / safe viewport
├── HeaderStatusSlot
├── HeroSlot
├── StatePanelSlot
├── QuickSettingsSlot
├── FooterInfoSlot (optional/collapsible)
└── BottomNavSlot
```

All three Home states use the same root hierarchy.

---

# 5. SAFE VIEWPORT AND SYSTEM INSETS

Calculate the usable Home viewport after applying relevant system insets.

Respect:

- status bars;
- display cutout;
- navigation bars;
- gesture navigation inset.

Use Compose `WindowInsets` APIs instead of guessing physical pixels.

Do not double-apply insets.

Do not let bottom navigation collide with Android gesture/navigation UI.

Do not use raw screenshot height as if it were content height.

---

# 6. VERTICAL REGION BUDGET — DESIGN INTENT, NOT ABSOLUTE OFFSETS

For a normal portrait device, use this only as the visual budget:

- approximately 0–24%: header + connection status;
- approximately 24–54%: hero artwork;
- approximately 54–66%: state-specific functional panel;
- approximately 66–90%: quick settings + optional compact helper;
- approximately 90–100%: bottom navigation.

These are **not** instructions to use `offset(y = screenHeight * 0.xx)`.

Use them to decide measured slot sizes inside the shared scaffold.

The final layout must be constraint-driven and responsive.

---

# 7. HEADER / STATUS SLOT

The top slot contains:

- HotFox brand/header;
- Settings action if the current design requires it;
- state headline: `Не защищено`, `Подключаем...`, or `Защищено`;
- state subtitle/timer.

Requirements:

- same horizontal margins across all states;
- same header baseline and settings-icon position across all states;
- headline location stable across states;
- subtitle/timer does not push the hero unpredictably;
- no bright planet edge directly behind small subtitle text;
- no hero/fox intrusion into this slot;
- use a consistent 8/12/16dp spacing rhythm rather than arbitrary spacers.

The headline may naturally have slightly different glyph width, but its bounding region remains structurally stable.

---

# 8. HERO SLOT — ARTWORK IS ALLOWED TO LAYER ONLY HERE

The canonical fox/planet artwork remains an important brand asset, but it must live inside a bounded hero region.

Hero requirements:

1. `HeroSlot` is a real measured region in the shared scaffold.
2. Planet and fox are layered **inside this region**.
3. The hero may use a local `Box` and internal alignment.
4. The fox uses the canonical approved asset already present in the branch/project.
5. Do not regenerate, redraw, recolor, crop, or substitute the fox.
6. Fox uses `ContentScale.Fit`.
7. Fox aspect ratio is preserved.
8. Full ears and muzzle stay intact.
9. Planet remains a separate layer.
10. Planet may be oversized/cropped **inside the HeroSlot only**.
11. A dark fade at the bottom of HeroSlot transitions artwork cleanly into the state panel.
12. Artwork must not remain visibly bright underneath buttons, progress, cards, stats, or navigation.
13. Same hero geometry for Disconnected, Connecting, and Connected.
14. No state-specific fox X/Y/scale values.
15. State may alter only subtle glow/alpha/animation if it cannot affect geometry.

It is acceptable for the hero container to clip its own decorative children at its own region boundary **after** the fox geometry has been designed so the important silhouette is intact. This is exactly what prevents decorative pixels from spilling into state controls.

Do not crop the fox itself with `ContentScale.Crop`.

Do not put the fox inside a circle, card, rounded mask, or banner.

The hero should feel cinematic inside its allocated region, not like an image row and not like a full-screen object invading controls.

---

# 9. HERO GEOMETRY STABILITY

For all three Home states:

- HeroSlot top Y is identical.
- HeroSlot bottom Y is identical.
- Fox scale is identical.
- Fox center is identical.
- Planet scale is identical.
- Planet center is identical.
- Fade bounds are identical.

Target geometry delta between states: `0dp`.

Do not move the fox down in Connecting.

Do not shrink the fox in Connected.

Do not move the planet to make room for stats.

The functional UI changes below it; the hero does not.

---

# 10. STATE PANEL SLOT — SAME BOUNDS IN ALL STATES

This is the most important structural change.

Create one `StatePanelSlot` whose measured height is the same for:

- Disconnected;
- Connecting;
- Connected.

It must be tall enough for the tallest intended state content at the current width/device class.

The next region (`QuickSettingsSlot`) must therefore begin at the same Y position in every Home state.

State changes must not push the settings card up or down.

If using `AnimatedContent`, `Crossfade`, or another transition:

- parent StatePanel bounds remain fixed;
- animation does not animate parent size;
- content is constrained to the slot;
- no child may escape the slot;
- do not use state-specific parent offsets.

Target top-position delta of QuickSettings across state screenshots: <= 2dp, ideally 0dp.

---

# 11. DISCONNECTED STATE PANEL

The Disconnected panel should be simple and visually dominant only where appropriate.

Contains:

- optional compact VPN permission/status message;
- primary orange `Подключить` CTA.

Rules:

- permission text must live inside the StatePanel, not float over the fox;
- do not position `Нет разрешения VPN` on the hero boundary;
- if permission is unavailable, present it as compact secondary status/chip/caption;
- Connect CTA must be the clear primary action;
- CTA width aligns with page/content grid;
- CTA min touch height approximately 56dp;
- no fox face/fur visible directly behind CTA;
- no CTA overlap with QuickSettings card.

If there is insufficient height, reduce decorative/secondary copy before reducing the CTA touch target.

---

# 12. CONNECTING STATE PANEL — REMOVE THE DEBUG-DASHBOARD FEEL

Current Connecting UI is too tall and collides because it tries to display a progress polyline + four textual steps + Stop button while sharing the hero area.

Rebuild it **inside StatePanelSlot**.

Preferred priority:

1. compact progress indicator;
2. current active step;
3. minimal secondary progress context;
4. Stop action.

If four textual rows cannot fit cleanly, do **not** force all four rows into the slot.

Acceptable compact alternatives:

- one horizontal progress line with four small milestones + only the current step label;
- current step + `2 из 4` indicator;
- current step + one subdued next-step line;
- a compact four-row list only on displays where it actually fits without reducing touch targets or colliding.

Do not draw the progress line across the fox.

Do not draw step labels over the planet.

Do not move the fox to make room.

Stop CTA belongs inside the same StatePanelSlot and must not collide with QuickSettings.

The Stop action is secondary relative to the orange Connect action; preserve current visual language but it may be less visually dominant.

---

# 13. CONNECTED STATE PANEL

Connected state must also fit entirely inside the exact same StatePanel bounds.

Contains the useful connection summary:

- downloaded traffic;
- uploaded traffic;
- user -> HotFox -> Internet path if retained;
- `Отключить` action.

Rules:

- these statistics do not float over the hero;
- the route diagram does not sit on the fox/planet;
- no translucent black rectangle should appear as an accidental overlay on the artwork;
- arrange metrics compactly inside the state panel;
- Disconnect button stays inside the state panel;
- do not enlarge state-panel height in Connected state;
- quick-settings card begins at the same Y as other states.

If necessary, simplify visual ornamentation while preserving information.

For example, two compact metrics on one row and a lightweight route row are better than an oversized dashboard covering the hero.

---

# 14. QUICK SETTINGS CARD — NORMAL FLOW ONLY

The quick-settings card must be a normal measured child below StatePanelSlot.

It must never be positioned with a free-floating Y offset.

Card rules:

- stable top position across all Home states;
- stable width and horizontal margins;
- stable corner radius;
- near-opaque dark surface, around `#111016` with approximately 0.94–0.97 effective opacity, or the closest existing design token;
- hero detail must not fight with card text;
- no bright orange fur visible through text-heavy rows;
- each row gets a reliable minimum touch target, approximately 52–56dp;
- dividers stay within card bounds;
- icons use consistent optical size/stroke;
- trailing status/chevron gets reserved width;
- text gets the remaining width using `weight(1f)` or equivalent;
- primary row label should be one line;
- secondary text should normally be max one line on Home;
- use ellipsis instead of wrapping into the next row;
- never let one row's text overlap the next row.

The card may contain:

- server selection;
- subscription;
- Shadow;
- routing/traffic mode.

But the Home screen is not a settings documentation page. Long explanations belong in detail screens.

---

# 15. FIX `Весь трафик · Smart` ROW

The current long subtitle is one of the clearest symptoms of bad density.

Home-row target:

Primary:

`Весь трафик · Smart`

Secondary may be shortened to something equivalent to:

`Интернет через VPN · LAN по настройке`

or existing concise product wording.

Maximum one line on standard widths; ellipsize if needed.

Do not allow:

`Интернет через VPN; LAN только если ...`

to wrap into multiple lines and collide with the card bottom or next region.

Do not change the underlying routing behavior. This is display copy only.

---

# 16. FOOTER / HELPER COPY

Disconnected currently carries extra subscription/helper text under the card.

That helper is secondary information.

Rules:

- it gets its own measured `FooterInfoSlot` if retained;
- it may not overlap card or bottom navigation;
- on short screens, compact/shorten it before compressing critical controls;
- if product semantics allow, show one concise line and move detail elsewhere;
- never use negative margin to squeeze it above the nav;
- no helper text should be partially clipped by navigation.

If the footer cannot fit on the shortest supported screen after reasonable spacing compression, the secondary helper is the first candidate to collapse—not the CTA, not the nav, and not row touch targets.

---

# 17. BOTTOM NAVIGATION — SOLID, SEPARATE, FIXED REGION

Bottom navigation must be its own final region in normal layout flow.

Requirements:

- completely opaque app background behind navigation;
- no fox visible behind it;
- no planet visible behind it;
- no settings card behind it;
- no helper copy underneath it;
- `navigationBarsPadding()` / equivalent applied correctly;
- active item styling preserved;
- icon/text alignment consistent;
- touch targets remain accessible;
- position is identical across Disconnected, Connecting, Connected.

Do not implement the nav as a floating overlay over Home content.

---

# 18. RESPONSIVE CONTRACT

Validate at least:

- 360 x 800;
- 375 x 812;
- 393 x 852;
- 393 x 873;
- 412 x 915;
- 430 x 932.

Also verify font scale:

- 1.0;
- 1.15.

The implementation must not be tuned for one screenshot resolution.

Use DP constraints and measured content.

Do not derive production geometry from one emulator's raw pixel dimensions.

---

# 19. SHORT-SCREEN COMPRESSION ORDER

When vertical space is limited, compress in this exact priority order:

1. decorative empty gaps;
2. optional top/bottom hero breathing room within allowed hero minimum;
3. secondary helper/footer copy;
4. secondary card subtitles;
5. internal non-touch spacing;
6. hero height within a defined minimum range.

Do **not** solve short screens by:

- overlapping slots;
- making nav float;
- shrinking button touch targets below acceptable size;
- making settings rows unreadably short;
- pushing content behind system navigation;
- cropping critical text;
- moving the fox over controls.

---

# 20. TYPOGRAPHY / TEXT SAFETY

Every text element must have explicit behavior under constrained width.

Rules:

- primary labels: max one line unless explicitly intended otherwise;
- secondary Home-row labels: max one line where practical;
- use `overflow = TextOverflow.Ellipsis` for constrained secondary text;
- do not allow `softWrap` to make row height unpredictable unless row is explicitly multi-line;
- reserve width for trailing values and chevrons;
- no text under icons;
- no text under another text layer;
- no clipping at card edge;
- no label touching rounded card corners.

Russian strings are the baseline for sizing because they are often wider than English equivalents.

---

# 21. SPACING SYSTEM

Use a coherent spacing rhythm.

Prefer existing design tokens. If absent, align to a small set such as:

- 4dp micro-gap;
- 8dp compact gap;
- 12dp row/internal gap;
- 16dp standard section gap;
- 24dp page margin / major gap where current design supports it.

Avoid arbitrary sequences such as 7dp, 13dp, 19dp added solely to make one screenshot fit.

Do not accumulate magic numbers across states.

---

# 22. STATE TRANSITION STABILITY

Switching state must not restructure the whole page.

The following bounds should remain invariant:

- HeaderStatusSlot bounds;
- HeroSlot bounds;
- QuickSettingsSlot top Y;
- BottomNavSlot bounds.

Only the contents of StatePanelSlot change.

Target deltas across side-by-side screenshots:

- hero top/bottom: 0dp;
- hero fox/planet geometry: 0dp;
- quick-settings card top: <= 2dp, target 0dp;
- bottom nav top: 0dp;
- page horizontal margins: 0dp.

Do not use animated parent-size changes that violate these constraints.

---

# 23. OPTIONAL DEBUG BOUNDS — USE THEM TO PROVE THE LAYOUT

During development, instrument region bounds temporarily.

Use `onGloballyPositioned`, layout coordinates, or an equivalent debug helper to record:

- header rect;
- hero rect;
- state panel rect;
- quick-settings rect;
- footer rect when present;
- bottom-nav rect.

In debug builds, add checks/logging equivalent to:

```kotlin
require(header.bottom <= hero.top + epsilon)
require(hero.bottom <= statePanel.top + epsilon)
require(statePanel.bottom <= quickSettings.top + epsilon)
require(quickSettings.bottom <= footerOrNav.top + epsilon)
require(footer.bottom <= bottomNav.top + epsilon) // when footer exists
```

Use a tiny epsilon only for rounding.

The point is to prove slot separation rather than eyeballing it.

Do not ship visible debug rectangles.

Debug logging may remain only if it is quiet, non-sensitive, and guarded by debug configuration.

---

# 24. INSPECT AND DELETE LEGACY COLLISION HACKS

Before finalizing, search Home-related code for:

- `offset(`;
- `absoluteOffset(`;
- `translationY`;
- `translationX`;
- `zIndex(`;
- screen-height percentage offsets;
- state-specific hero geometry;
- duplicated Home layouts;
- fixed absolute card Y positions;
- progress overlay boxes;
- stats overlay boxes;
- bottom-nav overlay placement.

Not every occurrence in the entire app is automatically wrong, but every Home occurrence must be justified.

Remove obsolete workaround code instead of leaving dead branches that a later edit can reactivate.

---

# 25. IMPLEMENTATION ORDER — DO NOT SKIP AHEAD

## Phase A — inspect

Locate:

- current Home root composable;
- current hero component;
- Disconnected/Connecting/Connected branches;
- quick-settings card;
- bottom navigation;
- all `offset` / translation / zIndex usage involved in Home.

Document the actual collision cause briefly in the commit/implementation notes.

## Phase B — shared scaffold

Create/refactor the shared non-overlap scaffold first.

Use placeholder colored/debug slots if useful locally.

Do not tune fox artwork yet.

## Phase C — lock major bounds

Make Disconnected layout fit with no overlap using real slot measurement.

Verify bottom nav and settings card.

## Phase D — hero inside bounded slot

Place planet + fox inside `HeroSlot`.

Tune only within that slot.

Do not let art escape into functional regions.

## Phase E — state panel variants

Implement Disconnected/Connecting/Connected content inside the same StatePanelSlot.

Do not change its external bounds by state.

## Phase F — quick settings and footer

Make rows constraint-safe.

Fix `Весь трафик · Smart` subtitle density.

## Phase G — smallest supported screen

Test 360x800 first.

If it fails, follow the compression priority instead of introducing overlap.

## Phase H — target emulator visual verification

Capture all three Home states at exactly the same emulator resolution.

## Phase I — cross-resolution validation

Check the full supported matrix.

## Phase J — cleanup

Remove debug visuals and obsolete collision hacks.

---

# 26. VISUAL ACCEPTANCE GATE — SIDE-BY-SIDE SCREENSHOTS REQUIRED

Build and run the app.

Capture fresh screenshots for:

- Disconnected;
- Connecting;
- Connected.

Use the same device/resolution for the three-state comparison.

Place them side-by-side or provide a deterministic comparison artifact.

The work is **FAILED** if any of these occur:

1. progress line or step text overlaps the fox;
2. traffic stats overlap the fox/planet;
3. CTA overlaps hero content;
4. CTA overlaps quick-settings card;
5. settings card overlaps hero or state panel;
6. helper text overlaps settings card or bottom nav;
7. bottom navigation overlaps any page content;
8. fox/planet appears behind bottom navigation;
9. card text wraps into another row;
10. subtitle is clipped at card bottom;
11. card moves materially between Home states;
12. hero moves materially between Home states;
13. bottom nav moves between Home states;
14. huge unexplained dead black gap appears because content was merely pushed away;
15. user-facing controls are made unreasonably small just to pass screenshot fit;
16. a state-specific offset is introduced to make only one state look correct;
17. functional UI is layered over another functional region using z-order;
18. content goes behind Android system navigation;
19. typography is clipped at fontScale 1.15;
20. fox is turned into a tiny logo/banner as an avoidance tactic.

If ANY acceptance condition fails, continue fixing. Do not report the task complete.

---

# 27. DESIGN QUALITY GATE

Passing collision checks is necessary but not sufficient.

The final result must also preserve the intended HotFox character:

- dark premium background;
- cream/off-white typography;
- precise orange accent;
- green only for protected/healthy status where appropriate;
- cinematic fox/planet identity;
- typography-first hierarchy;
- minimal heavy container use;
- clear primary action;
- calm visual rhythm;
- strong negative space without giant accidental holes;
- no gamer neon/glass/cyberpunk clutter;
- no random gradients behind every element;
- no giant dashboard cards.

The UI should feel intentionally composed, not simply prevented from overlapping.

---

# 28. ACCESSIBILITY / INTERACTION GATE

Do not fix layout by sacrificing usability.

Keep:

- reasonable minimum touch targets;
- readable text contrast;
- meaningful content descriptions where required;
- focus/talkback order following visual order;
- no invisible overlapping clickable regions;
- no clickable card underneath another clickable overlay;
- buttons fully reachable above system navigation.

If old overlay code leaves overlapping hit targets even after the pixels look correct, the task is still failed.

---

# 29. BUILD AND TEST REQUIREMENTS

After implementation:

1. run the relevant Gradle compile/build task available in the repository;
2. run unit/UI tests that cover touched components if present;
3. install/run on the available emulator if infrastructure exists;
4. capture fresh Home screenshots;
5. inspect them, do not merely trust successful compilation;
6. iterate until the acceptance gate passes.

A green compile is not visual acceptance.

Do not claim emulator evidence if no emulator actually ran.

Do not claim release readiness; this is a UI task and the repository's runtime/device release gates remain separate.

---

# 30. REQUIRED FINAL REPORT FROM CURSOR

When the V12 task is genuinely complete, report:

### Changed files

Exact list of source/resources changed.

### Architecture

Briefly state:

- which shared scaffold now owns Home vertical placement;
- how StatePanel uses stable bounds;
- how HeroSlot prevents artwork bleed;
- how bottom nav is separated.

### Removed hacks

List relevant old `offset` / translation / overlay hacks removed.

### Build evidence

Provide command(s) and result(s).

### Visual evidence

Provide paths/names for fresh screenshots of:

- Disconnected;
- Connecting;
- Connected.

### Invariant verification

Confirm:

- no functional overlaps;
- same hero bounds across states;
- same settings-card top across states;
- same bottom-nav bounds across states;
- no row text collision;
- 360x800 checked;
- fontScale 1.15 checked, if the test environment permits.

### Runtime boundary

Explicitly confirm that VPN/network/business logic was not rewritten for this UI task.

---

# 31. DEFINITION OF DONE

V12 is done only when all of the following are true:

- Home is one shared scaffold, not three independently positioned compositions;
- all functional regions are measured in normal layout flow;
- only hero artwork layers inside its bounded hero slot;
- Disconnected/Connecting/Connected use identical major slot bounds;
- state content changes do not move the settings card;
- bottom navigation is an opaque, separate region;
- no functional control overlaps another functional control;
- no functional control is drawn over the fox/planet;
- no fox/planet pixels bleed behind bottom navigation;
- long Russian row text cannot collide with adjacent rows;
- shortest supported device remains usable;
- build succeeds;
- fresh screenshots are visually inspected;
- any failed screenshot is fixed before completion is reported.

---

# 32. FINAL OWNER INTENT

Stop treating each screenshot as a separate poster that needs manual coordinates.

This is an application.

The Home screen must have a real layout system.

The fox and planet are the brand artwork.

They do not own the whole screen.

The controls are functional UI.

They must never fight the artwork for the same pixels.

Build one stable vertical composition, give every functional region its own measured space, keep artwork inside its hero slot, and make all three connection states inherit the exact same skeleton.

**NO MORE OVERLAP PATCHES. REBUILD THE HOME LAYOUT ARCHITECTURE AND PROVE IT WITH FRESH SCREENSHOTS.**
