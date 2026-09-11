# HOTFOX UI CORRECTIVE REVIEW — HARD FAIL

Status: **FAIL / DO NOT MERGE / DO NOT CALL PIXEL-LOCK COMPLETE**

Target branch: `cursor/hotfox-ui-pixel-lock-execution`

This review is intentionally strict. The approved 18 visual references are the immutable visual source of truth. The current implementation is not accepted as a faithful reconstruction.

## Executive verdict

The current pass is functionally non-trivial, but visually it does **not** satisfy the HotFox 1:1 reconstruction contract.

The implementation used a hybrid shortcut:

- real Android views for text, rows, controls and state;
- large raster crops/extractions from the approved screenshots for artwork/background regions;
- manually approximated XML geometry;
- no emulator screenshot capture;
- no actual overlay/diff loop;
- no per-screen pixel PASS.

That is not enough.

Current pixel-lock score: **0 / 18 screens passed**.

The current implementation MUST be treated as an intermediate reconstruction only.

---

# P0 — RELEASE-BLOCKING VISUAL FAILURES

## P0.1 — Pixel verification was never executed

`HOTFOX_PIXEL_DIFF_RESULTS.csv` says `NOT_CAPTURED` / `NOT_EXECUTED` for all 18 screens.

Therefore:

- no screen has proven geometry fidelity;
- no screen has proven typography fidelity;
- no screen has proven artwork fidelity;
- no screen has proven spacing/radius/icon fidelity;
- no screen may be called pixel-perfect;
- no screen may be marked PASS.

A visual reconstruction without a captured implementation screenshot compared against the approved source is unfinished.

Required correction:

1. launch the actual Android app;
2. reach the exact target state;
3. capture the screen at the canonical viewport;
4. normalize only unavoidable system/dynamic regions;
5. compare to the approved reference;
6. produce overlay + heatmap + statistics;
7. correct the implementation;
8. repeat until accepted.

No visual PASS without this loop.

---

## P0.2 — Production UI contains screenshot-derived composite raster crops

The following production artwork names are evidence of the current shortcut:

- `hotfox_art_fox_planet_splash.png`
- `hotfox_art_fox_planet_onboarding.png`
- `hotfox_art_fox_planet_home.png`
- `hotfox_art_fox_planet_connecting.png`
- `hotfox_art_fox_planet_https.png`
- `hotfox_art_globe_orbits.png`
- `hotfox_art_server_ready.png`
- `hotfox_art_shadow_shield.png`
- `hotfox_art_always_on_shield.png`

A screenshot crop is NOT automatically a legitimate production asset.

The only raster assets allowed in the final implementation are:

1. standalone visual elements explicitly provided as approved production PNG assets by the owner; or
2. isolated decorative artwork recreated/exported cleanly as a standalone asset without baked-in UI, text, phone chrome, rows, buttons, labels, navigation, or screenshot contamination.

Forbidden:

- cropping a large rectangular region from the reference and dropping it behind live controls;
- keeping remnants of neighboring UI inside an artwork crop;
- using a screenshot crop to fake layout precision;
- baking text, controls, rows or navigation into a bitmap;
- using the original phone screenshot itself as the app screen;
- extracting arbitrary decorative motifs and propagating them to other screens.

The owner is preparing a ZIP containing individually cut approved PNG elements. When that ZIP is added, those assets become the preferred visual asset source.

Until then, do NOT create additional screenshot-derived production crops.

---

## P0.3 — Decorative motif propagation is forbidden

The existing code contains multiple `fox_planet_*` resources.

That naming pattern itself shows that a motif was generalized across screens instead of treating each approved screen independently.

Hard rule:

**NEVER INVENT A PLANET. NEVER PROPAGATE A PLANET. NEVER PROPAGATE A FOX.**

If a specific approved reference contains a particular decorative element, reproduce only the exact element that exists on that exact screen.

If a specific approved reference does not contain it, it must not appear there.

Do not infer a global “space theme”.
Do not infer a global “fox + planet theme”.
Do not create visual continuity by copying motifs between screens.

Each reference owns its own artwork.

---

## P0.4 — The implementation intentionally deviated from the locked references

The current bug report explicitly records deliberate differences, including:

- extra rows on Settings;
- navigation wording normalized across screens instead of following the exact reference;
- themed Material dialogs retained where the locked visual treatment differs;
- missing country flag treatment;
- other geometry/content substitutions.

Under the HotFox visual contract, an intentional visual deviation is still a defect.

The correct logic is:

> If a functional destination must remain reachable but the approved screen does not show it, preserve the functionality through the approved information architecture without visually adding unapproved rows to the locked screen.

Do NOT solve product preservation by visually modifying the approved screen.

---

# P1 — MAJOR VISUAL QUALITY FAILURES

## P1.1 — Approximate XML is not sufficient

Hardcoded values such as `22dp`, `168dp`, `16dp`, `32sp`, etc. are not proof of reference matching.

Every important geometry value must be derived from the reference and verified against an emulator screenshot.

Required measurements per screen:

- content bounds;
- top inset;
- baseline positions;
- title box;
- body box;
- artwork box;
- CTA frame;
- row height;
- divider Y positions;
- icon centers;
- icon visual bounds;
- corner radii;
- bottom navigation geometry;
- text baselines and line wraps.

No “looks close enough”.

---

## P1.2 — Typography must be verified visually

Using `sans-serif`, `sans-serif-medium`, or approximate `sp` values does not establish fidelity.

For every screen verify:

- font family;
- weight;
- optical size;
- letter spacing;
- line height;
- baseline;
- line wrapping;
- paragraph width;
- capitalization;
- numeric alignment.

Do not rewrite Russian copy just to fit the layout.

---

## P1.3 — Default Material remnants are not accepted where the reference is custom

The current implementation still contains Material components/dialog paths that do not visually match the approved design.

Material is an implementation library, not the design source of truth.

Allowed:

- using Material internals when fully restyled to the approved visual result.

Not allowed:

- visible stock Material dialog/chip/button/switch/card appearance when it differs from the reference.

---

## P1.4 — Dynamic truth must remain real without changing locked composition

The visual references may contain example values.
Production must not fake:

- ping;
- server availability;
- load;
- entitlement;
- expiry;
- devices;
- traffic;
- connection state;
- Android Always-on state;
- VPN protection state.

Correct approach:

- preserve reference geometry and typography;
- bind real values into those slots;
- use deterministic fixtures only in visual tests.

Do not solve truthfulness by redesigning the screen.

---

# P1 — TECHNICAL NON-REGRESSION

The VPN/session/Xray/HEV/payment/control-plane architecture must remain intact.

The UI correction phase MUST NOT rewrite or simplify:

- Android `VpnService` lifecycle;
- TUN ownership;
- HEV/tun2socks path;
- local SOCKS handoff;
- Xray lifecycle;
- connection truth mapping;
- DNS handling;
- IPv6 fail-closed behavior;
- AUTO/manual selection semantics;
- routing policy semantics;
- Shadow security semantics;
- Autopilot state machine;
- subscription entitlement authority;
- device registry;
- notification / quick settings truth;
- stale-session/race protections.

UI code may bind to existing state/actions.
It may NOT create parallel state machines to make the screenshots easier to reproduce.

Any technical file touched for UI convenience requires explicit justification and regression tests.

---

# ASSET-FIRST RECONSTRUCTION RULE

The owner will provide a ZIP of individually cut PNG assets from the approved design.

When present:

1. inventory every PNG;
2. record dimensions, alpha bounds and SHA-256;
3. map each asset to exact screen(s);
4. do not rename ambiguously;
5. do not crop it again unless the owner asset itself contains intentional transparent padding;
6. do not recolor it unless the reference proves a state tint;
7. do not stretch non-scalable artwork;
8. preserve aspect ratio;
9. use density-appropriate Android resource placement;
10. avoid rasterizing elements that should remain native interactive UI.

Asset ownership must be explicit, e.g.:

`screen_05/fox_home.png`

not:

`fox_planet_generic.png`

Shared usage is allowed only if the exact same visual element is demonstrably reused in the approved references.

---

# REQUIRED CORRECTION WORKFLOW

Do NOT rebuild all 18 screens blindly in one pass.

Use this controlled sequence:

### Phase A — asset intake

- wait for / import owner-supplied element ZIP;
- inventory assets;
- validate transparency and dimensions;
- create asset-to-screen manifest;
- identify which elements are native UI vs decorative PNG.

### Phase B — canonical screens first

Reconstruct and validate one screen at a time.

Start with the strongest already-approved master application screen, then its adjacent states.

For each screen:

1. implement;
2. build;
3. launch;
4. navigate to deterministic test state;
5. screenshot;
6. diff;
7. fix;
8. repeat;
9. mark PASS only with evidence.

### Phase C — propagate only structural tokens

Allowed to reuse:

- background colors;
- typography tokens;
- spacing constants where proven;
- CTA geometry where identical;
- icon treatment where identical;
- bottom navigation component where literally identical.

Forbidden to propagate:

- decorative artwork;
- fox imagery;
- planets/orbits;
- state-specific illustration;
- screen-specific row composition;
- screen-specific text;
- screen-specific accent placement.

### Phase D — 18/18 evidence gate

Before merge, repository must contain for every screen:

- approved reference identifier;
- implementation screenshot;
- overlay or diff image;
- measured changed-pixel result;
- notes for any intentionally masked dynamic region;
- PASS/FAIL.

`18/18 PASS` is required for the visual gate.

---

# ABSOLUTE NEGATIVE PROMPT

DO NOT:

- invent design;
- improve design;
- simplify design;
- modernize design;
- reinterpret design;
- generalize motifs;
- add a planet;
- add a globe;
- add an orbit;
- add a fox where it is absent;
- remove a fox where it is present;
- use generic Material styling;
- add convenience rows;
- rename navigation because it seems more consistent;
- add cards not in the reference;
- add shadows not in the reference;
- add gradients not in the reference;
- add metrics not in the reference;
- bake UI into screenshots;
- ship screenshot crops as fake layout;
- claim visual completion without captured evidence;
- touch VPN core for cosmetic convenience.

---

# CURRENT VERDICT BY CATEGORY

Functional reconstruction: **substantial work exists**.

Visual reconstruction: **unverified and not accepted**.

Pixel-lock verification: **0/18**.

Production merge readiness: **FAIL**.

Technical core confidence: **must remain protected during correction**.

---

# EXIT CRITERIA

This review can be superseded only when ALL conditions are true:

- owner-supplied visual assets are integrated correctly;
- no unauthorized screenshot-derived composite crops remain;
- no decorative motif is propagated without exact reference evidence;
- no unapproved visual additions remain;
- real functional state remains bound;
- VPN technical core has no regression;
- actual Android screenshots are captured;
- pixel comparison has been run;
- all 18 screens have evidence;
- all 18 screens are accepted.

Until then:

**HOTFOX UI REBUILD = FAIL / INCOMPLETE / DO NOT MERGE.**
