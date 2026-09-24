# HOTFOX ONBOARDING 01–03 — CORRECTIVE PASS 2 / 2026-09-23

## OWNER DIRECTIVE

The latest implementation is functional, but visual acceptance is NOT complete.

This pass is mandatory and supersedes conflicting visual implementation details in the previous 01–03 onboarding prompt wherever this document is more specific.

Do not redesign the whole app.
Do not touch VPN/core/security/entitlement semantics.
Do not roll back Home V13.
Do not regenerate the fox.
Do not use screenshot backgrounds.

The purpose of this pass is to finish screens:

01 — Splash / Brand Entry
02 — Connect HotFox onboarding
03 — AUTO onboarding

using the existing project and live Views/ViewBinding UI, with a real corrective visual pass and fresh evidence.

---

# 1. CURRENT EVIDENCE REVIEW

Fresh actual frames currently exist at:

- verification/ui/onboarding_01_03_20260914/actual/01.png
- verification/ui/onboarding_01_03_20260914/actual/02.png
- verification/ui/onboarding_01_03_20260914/actual/03.png

Current validation honestly reports Visual/pixel PASS: 0/3.

This is the correct status. Do not relabel them as PASS without new evidence.

The current implementation is materially improved, but the remaining visual problems are:

## Screen 01

- overall direction is good;
- fox asset and brand atmosphere are good;
- planet creates too much broad brown wash in the upper area and does not always read cleanly as a celestial edge;
- lower brand block can be optically tightened;
- screen still needs cross-device confirmation rather than one 1032×2231 capture.

## Screen 02

- correct direction, good fox asset, correct live actions;
- title/body block is visually heavy;
- rhythm between title/body, hero, and CTA is not yet premium;
- too much dead space can appear depending on viewport;
- planet/fox composition must feel intentional instead of merely filling the background;
- CTA/footer area must remain clean and never collide on short displays.

## Screen 03 — PRIMARY VISUAL DEFECT

This screen is currently the weakest of the three.

The reason is not merely spacing.

Production currently uses:

- hf_auto_orbits
- hf_globe_orange

as raster ImageViews, with fixed dimensions and low alpha.

Current code includes fixed values such as:

- orbit image around 268dp;
- globe around 96dp;
- orbit alpha around 0.42;
- globe alpha around 0.95.

This causes the AUTO artwork to feel:

- blurry;
- generic;
- visually cheaper than screens 01/02;
- too weak in contrast;
- too small / too static in the composition;
- disconnected from the premium editorial quality of the fox screens.

DO NOT solve this by generating another bitmap.

DO NOT upscale the blurry PNG.

DO NOT apply sharpening filters.

DO NOT hide the issue by increasing alpha.

Instead, reconstruct the AUTO orbit/globe motif as native scalable UI geometry.

---

# 2. HARD TECHNICAL BOUNDARY

Do not change:

- VpnService;
- Xray;
- HEV;
- routing;
- DNS;
- IPv6;
- server-selection semantics;
- entitlement truth;
- subscription parsing;
- purchase verification;
- onboarding state-machine meaning;
- connection truthfulness;
- Home V13 functional behavior.

Allowed scope:

- onboarding/splash layout;
- onboarding artwork presentation;
- native decorative View for screen 03;
- visual resources;
- dimensions;
- spacing;
- typography tuning;
- responsive rules;
- accessibility labels;
- screenshot/capture harness updates;
- UI-only tests.

---

# 3. SCREEN 03 MUST STOP USING THE BLURRY RASTER COMPOSITION

For production screen 03, replace the current visible composition made from:

- hf_auto_orbits
- hf_globe_orange

with a native, scalable rendering.

Preferred implementation:

`HotfoxAutoOrbitView.kt`

using Android Canvas / Paint / Path.

A VectorDrawable-based implementation is acceptable if it is easier to keep responsive, but a custom View is preferred because it allows exact scaling, multiple orbit radii, node placement, and subtle reduced-motion-safe animation.

The old PNG assets may remain in the repository for provenance/history/other uses, but screen 03 should no longer visually depend on their raster pixels.

Do not delete historical assets unless verified unused elsewhere.

---

# 4. HOTFOX AUTO ORBIT VIEW — REQUIRED ART DIRECTION

Create one reusable decorative View that draws:

1. three concentric elliptical/circular orbit tracks;
2. four satellite/node points distributed asymmetrically;
3. one crisp central globe symbol;
4. one subtle ambient orange aura;
5. optional ultra-subtle phase motion when reduced motion is OFF.

The visual result must remain restrained and premium.

NO:
- neon;
- glowing gamer UI;
- cyberpunk;
- glass;
- bright sci-fi HUD;
- thick circles;
- giant bloom;
- spinning arcade animation.

YES:
- near-black canvas;
- warm orange/copper accents;
- hairline orbit strokes;
- precise editorial geometry;
- soft low-alpha halos;
- crisp globe linework.

---

# 5. AUTO ORBIT VIEW — GEOMETRY

Use the actual measured size of the art host.

Do not use raw display pixels.

Do not hardcode a single 268dp square and call it responsive.

Let:

`S = min(width, height)`

Target composition:

- outer orbit diameter: approx 0.82 × S;
- middle orbit diameter: approx 0.64 × S;
- inner orbit diameter: approx 0.46 × S;
- central globe outer diameter: approx 0.26–0.31 × S;
- satellite core radius: approx 0.018–0.024 × S;
- satellite halo radius: approx 0.040–0.052 × S.

Clamp to sensible dp min/max values so very large tablets do not create giant art.

Suggested visual bounds:

- orbit group min: ~220dp;
- orbit group ideal phone range: ~250–310dp;
- orbit group max for this screen: ~340dp;
- globe ideal: ~82–108dp depending on measured host.

The globe must not be fixed to 96dp independent of the host.

---

# 6. AUTO ORBIT VIEW — STROKES AND COLORS

Use existing HotFox palette where possible.

Recommended semantic colors:

- main orange: existing hf_v13_orange / equivalent;
- warm secondary orange/copper: existing orange start/end tokens;
- orbit hairline: orange with alpha around 0.10–0.18;
- secondary orbit: alpha around 0.08–0.14;
- node outline: alpha around 0.18–0.28;
- node core: alpha around 0.55–0.80;
- globe primary stroke: orange alpha around 0.88–1.0;
- globe secondary glow: orange alpha around 0.12–0.22.

Use anti-aliased Paint.

Target approximate stroke widths at phone density:

- orbit: 0.75–1.25dp;
- node ring: 1.0–1.5dp;
- globe outer stroke: 2.5–4dp;
- globe internal latitude/longitude strokes: 2–3dp.

Do not make the orbit tracks visually compete with the title.

---

# 7. CENTRAL GLOBE — MUST BE CRISP

The current blurred orange globe is not acceptable.

Draw a proper globe symbol natively.

It should contain:

- outer circle;
- central vertical meridian;
- two curved side meridians OR one pair of symmetric longitude curves;
- two horizontal latitude bands;
- optional equator emphasis.

Do not simply draw a circle with a plus sign.

Do not use a Unicode globe character.

Do not use a low-resolution bitmap.

The linework should look intentionally designed at 1×, 2×, and high-density displays.

---

# 8. AUTO ART MOTION

Motion is optional and must be subtle.

If implemented:

- outer nodes may drift/rotate by only a few degrees over a long duration;
- duration >= 12 seconds per cycle;
- no bounce;
- no pulsating zoom;
- no fast orbit;
- central globe remains stable;
- honor reduced-motion settings;
- animation must not trigger layout changes.

Static is acceptable if it looks better.

Visual quality is more important than adding motion.

---

# 9. SCREEN 03 COMPOSITION

The AUTO screen must feel like part of the same product family as 01/02.

Required hierarchy:

HotFox wordmark

large title:
"Лучший сервер —
автоматически"

supporting body

clean negative space

premium native orbit/globe composition

primary CTA:
"Использовать AUTO"

secondary:
"Выбрать вручную"

The art should occupy the middle of the screen with visual authority.

Do not let it look like a small icon floating in empty space.

Do not let it touch the title/body or CTA.

Use measured layout flow.

No absolute screen-Y hacks.

---

# 10. SCREEN 03 RESPONSIVE ART SLOT

Keep screen 03 art in the existing measured art host or a revised measured art host.

The art host must:

- take remaining vertical space between body and CTA zone;
- have a practical minimum height;
- keep the orbit art centered optically, not merely mathematically if the title is tall;
- never force CTA below navigation/system bars;
- never overlap title/body;
- never overlap CTA.

If height is constrained:

compression order:

1. reduce vertical decorative gaps;
2. reduce orbit-group diameter within min bound;
3. reduce body/title spacing slightly;
4. reduce secondary-button vertical padding slightly;

Do not:
- shrink title into tiny type;
- shrink primary CTA below usable touch size;
- overlap art and CTA.

---

# 11. SCREEN 02 — TYPOGRAPHIC RHYTHM FIX

Screen 02 is structurally correct but still visually heavy.

Do a measured premium polish.

Current title size is around 36sp.

Test a narrower responsive range, not blind hardcoding.

Target visual title size:
- approximately 34–36sp at standard phone width;
- preserve two-line editorial break;
- no ugly orphan word;
- max 2 lines;
- line spacing/tightness intentional.

Body:
- approx 15–16sp;
- reduce excessive line spacing if present;
- muted cream/gray remains readable;
- max 2–3 lines depending width;
- no collision with hero.

Top stack:
- wordmark;
- title;
- body;
- hero breathing space.

Do not create a giant dead gap between body and fox.

Do not push fox into CTA.

The fox should be the midpoint visual anchor.

---

# 12. SCREEN 02 — HERO ART DIRECTION

Reuse the approved existing fox.

Do not regenerate it.

Do not use hotfox_hero_scene if the current task specifically uses separate onboarding planet + fox.

Maintain one fox.

Maintain one planet layer.

Improve only placement/opacity/scale if required.

The planet should read as a planet rim/environment, not as a flat brown overlay.

Target:
- keep the brightest rim away from body copy;
- reduce muddy brown dominance;
- preserve black negative space;
- keep fox face readable and premium;
- avoid clipping ears/muzzle;
- no raster stretching.

Any geometry change must be verified on at least two viewport classes.

---

# 13. SCREEN 01 — SPLASH POLISH

Do not radically redesign screen 01.

The current actual is close.

Polish goals:

- preserve the cinematic fox;
- keep brand block centered and clean;
- keep loader restrained;
- reduce any sense of a giant flat brown wash;
- make the planet edge/rim feel intentional;
- preserve dark negative space around the fox;
- ensure wordmark/tagline do not sit too low on gesture navigation;
- avoid top-right planet texture looking like an accidental cropped rectangle.

The splash should feel premium and calm.

Do not add extra controls or copy.

---

# 14. REMOVE FALSE VISUAL DEPENDENCY ON ONE EMULATOR SIZE

The previous proof used 1032×2231 @420dpi.

That is not enough.

This pass requires at minimum fresh actual captures for:

A. standard/tall:
- approx 393×873 logical viewport or equivalent emulator profile;

B. compact:
- approx 360×800;

C. one larger:
- approx 412×915 or 430×932.

If the capture harness works in physical pixels, document the logical size/density mapping.

At minimum, screens 01/02/03 must be checked at A and B.

Screen 03 specifically must be checked at A/B/C because its art is being made responsive.

---

# 15. FONT SCALE CHECK

At minimum verify:

- fontScale 1.0;
- fontScale 1.15.

Preferred:
- add 1.30 check for screen 03 and screen 02.

At larger font scale:

- CTA remains reachable;
- title remains <= intended line count where possible;
- no text overlaps art;
- secondary action remains visible;
- art compresses before controls collide.

Do not claim 1.3 if it was not actually rendered.

---

# 16. INTERACTION SAFETY

Preserve and re-test:

Screen 02:
- primary existing-subscription action -> HTTPS import;
- secondary buy-access action -> RenewalActivity / existing purchase flow;

Screen 03:
- AUTO -> setAutoMode(true) then continue according to existing flow;
- manual -> real server picker;

Do not fake completion.

Do not auto-grant access.

Do not replace activity routing with screenshot-only debug actions.

---

# 17. CURRENT 02 CTA CRASH REGRESSION MUST NOT RETURN

The earlier Material3/custom-background crash was fixed by using AppCompatButton.

Do not undo that.

Fresh test:
- launch 02;
- tap primary;
- verify HotfoxHttpsImportActivity resumes;
- verify no FATAL EXCEPTION for com.hotfox.vpn.

Do not call this proven from code review alone.

---

# 18. HOME V13 REGRESSION GUARD

This pass is onboarding-focused.

Do not redesign Home.

But after touching shared Hero code, capture at least one fresh Home V13 smoke screenshot (Disconnected is sufficient) OR run the existing relevant Home visual/unit guard proving Home geometry did not regress.

If shared HotFoxHeroArtwork changes:
- run Home hero composition tests;
- run Home crash guard tests;
- confirm Home fox/scene remains unchanged.

If a visual regression is observed, fix it before finishing.

---

# 19. TESTS TO ADD / UPDATE

Add or update tests for:

- AUTO art View can measure/layout at compact and tall sizes;
- no zero/negative dimensions;
- no dependence on exact raw screen pixel size;
- reduced-motion disables animation if animation exists;
- onboarding action wiring remains intact;
- screenshot fixture leak remains protected;
- no debug-only capture state leaks to production.

Do not write meaningless tests that only assert constants.

---

# 20. BUILD GATES

Run the repository-appropriate gates.

At minimum:

- assemblePlaystoreDebug;
- testPlaystoreDebugUnitTest;
- static_check_2_2_0.py;
- fixture leak check.

If release assemble and lint are feasible in this run, run them.

If CI is skipped due workflow conditions, document that honestly.
Do not describe a skipped GitHub Actions run as CI PASS.

Local build PASS != GitHub CI PASS.

---

# 21. VISUAL EVIDENCE PACKAGE

Create a fresh corrective evidence directory, e.g.:

verification/ui/onboarding_01_03_corrective_20260923/

Include:

- actual/01_standard.png
- actual/02_standard.png
- actual/03_standard.png
- actual/03_compact.png
- actual/03_large.png
- comparison/sbs_01.png
- comparison/sbs_02.png
- comparison/sbs_03.png
- VALIDATION.md
- UNTESTED_GAPS.md
- capture_manifest.json

If extra matrix captures are produced, include them.

Do not overwrite old evidence as though it never existed.
Keep history honest.

---

# 22. VISUAL ACCEPTANCE — SCREEN 01

FAIL if:

- planet looks like a rectangular brown panel;
- fox is clipped;
- fox becomes tiny;
- wordmark/tagline/loader collide with nav inset;
- bright planet rim hits brand text;
- art changes drastically from the accepted direction.

PASS only after fresh visual inspection.

---

# 23. VISUAL ACCEPTANCE — SCREEN 02

FAIL if:

- title/body collide with art;
- giant dead gap exists between body and fox;
- fox touches CTA;
- CTA touches system navigation;
- planet dominates copy;
- fox/planet looks like a boxed image;
- CTA text truncates or wraps badly;
- primary and secondary actions overlap;
- title becomes 3+ ugly lines at normal width.

PASS only after fresh visual inspection.

---

# 24. VISUAL ACCEPTANCE — SCREEN 03

This is the strictest gate.

FAIL if:

- central globe is blurry;
- raster orbit PNG is still the visible production art;
- orbit tracks are barely visible;
- art feels like a tiny icon;
- central globe is oversized gamer neon;
- nodes are perfectly symmetric / generic;
- art collides with title/body/CTA;
- fixed 96dp globe remains the only sizing rule;
- screen contains huge unexplained dead space;
- art looks visually cheaper than 01/02.

PASS target:

- crisp native globe;
- hairline premium orbits;
- restrained copper/orange palette;
- clear visual center;
- balanced negative space;
- responsive geometry;
- same brand quality tier as fox screens.

---

# 25. NO SCREENSHOT CHEATING

Do not:

- use reference_triptych.png as a background;
- flatten text/buttons into a bitmap;
- replace interactive UI with a screenshot;
- ship debug fixture data;
- draw fake server/payment/VPN state only for capture.

All controls remain live.

---

# 26. CODE QUALITY

Prefer one small dedicated component for AUTO art rather than adding more conditionals into HotFoxHeroArtwork.

Ideal separation:

- HotFoxHeroArtwork => fox/planet art;
- HotfoxAutoOrbitView => AUTO orbit/globe art.

Do not contaminate HotFoxHeroArtwork with AUTO-specific orbit code.

This prevents another shared-Hero regression.

---

# 27. FINAL REPORT REQUIRED

When complete, report:

1. exact HEAD SHA;
2. commits created;
3. exact source files changed;
4. whether production 03 still depends visually on hf_auto_orbits/hf_globe_orange;
5. build/test commands + results;
6. actual screenshots and their viewport/fontScale;
7. interaction checks;
8. Home regression result;
9. honest remaining gaps;
10. explicit statement that VPN/core/security behavior was not rewritten.

Do not report pixel exact unless measurement actually proves it.

Do not report RELEASE READY.

---

# 28. EXECUTION BEHAVIOR

Do not stop after writing a plan.

Do not ask the owner to manually tune coordinates.

Do not make one pass and stop if fresh screenshots still look weak.

Use this loop:

INSPECT
→ IMPLEMENT
→ BUILD
→ CAPTURE
→ VISUALLY REVIEW
→ FIX
→ RECAPTURE
→ VERIFY ACTIONS
→ REPORT

Continue autonomously until the screen 03 quality issue is genuinely resolved and 01/02 no longer have obvious rhythm problems.

The owner explicitly asked to finish the work, not to produce another theoretical recommendation.
