# HOTFOX UI V10 — ART DIRECTION REVIEW / DESIGN CRITIQUE

## Verdict
Current pass is **better than the previous one**, but it is **still not production-finish premium UI**. The direction is correct, yet the execution is inconsistent. The product already has a recognizable visual language, but several screens still feel like a prototype or a partially resolved concept rather than a polished final app.

The most important global problem: the project now has **two different visual modes that are not equally resolved**:
1. screens with the fox + planet hero composition;
2. information-heavy functional screens.

The fox/planet screens have mood and identity, but the planet is still not fully solved. The functional screens are improved, but rhythm, spacing, hierarchy, and component standardization are still inconsistent. The result is visually attractive from afar, but under close review it still lacks the precision, discipline and compositional coherence expected from a premium 2026 product UI.

---

## Global critique — what is still wrong

### 1) The planet is still not truly resolved
The user request was to add a planet in the background so the composition feels whole. That part was understood, but the implementation still looks too much like a **generic glowing hemisphere** rather than a deliberately designed background object.

Problems:
- The planet is sometimes too close to the fox and competes with it.
- The rim light is too obvious and too evenly distributed, which makes it feel like a decorative halo instead of a real volume.
- The fill of the planet is too flat and too dark in some screens, so visually it reads as a large soft dome rather than a celestial body.
- The planet’s scale and crop are not fully systematized across hero screens.
- On some screens the fox sits on top of the planet, but the layering lacks a believable atmospheric depth relationship.
- The planet edge is too sharp/too visible in some places and disappears too abruptly in others.

Design conclusion: the planet needs to become a **controlled compositional anchor**, not just a background ornament.

### 2) Fox artwork still has some visual fragility
The fox remains the emotional anchor of the brand, but it still has some issues:
- The fox is sometimes too small relative to the overall canvas.
- On a few screens it floats in a way that feels disconnected from the page structure.
- The glow accents are beautiful, but in some cases the glow-to-background contrast becomes the most interesting thing on the page, which weakens the UI itself.
- The silhouette should be razor-clean and consistent across screens; currently the hero image sometimes looks more like a beautiful asset placed onto screens than like a fully integrated visual system.

### 3) Layout discipline is not yet strict enough
This is the biggest professional weakness of the current pass.

What still feels off:
- Vertical rhythm is inconsistent between header, hero, CTA, cards and supportive text.
- Some screens have too much empty space with no compositional logic; other screens are dense.
- Cards, paddings, line spacing and text block spacing are not fully standardized.
- Screen-to-screen transitions do not always feel like parts of one product family.
- There is still a difference between “marketing-ish screens” and “utility screens” that is bigger than it should be.

### 4) Hierarchy is not always precise
Some screens are strong at the first glance, but information priority is not always perfectly arranged.

Examples:
- On the disconnected and connected home states, the fox/planet hero is visually very strong, but the relationship between **state / permission notice / CTA / dashboard summary** is not yet perfect.
- On info-dense screens, some secondary labels compete too much with primary content.
- Section headers and row labels are improved, but they are still not consistently tuned for the same hierarchy model.

### 5) Typography is good, but not yet “locked”
The general typographic direction is right, yet the system still needs refinement.

Issues:
- Some large headings are strong, others feel slightly too heavy.
- Supporting descriptions are occasionally too light or too low-contrast for the amount of text.
- Capitalization style and emphasis system need stronger consistency.
- Some pages mix short-form labels and long explanatory copy without enough contrast in scale, line-height and spacing.
- English product names inside Russian UI still need careful handling so they feel intentional, not accidental.

### 6) Components are close, but not perfectly systematized
Buttons, cards, rows, chips, toggles, bottom sheets and segmented controls are much improved, but a premium product requires pixel-locked component logic.

Problems still visible:
- Card radii, stroke strength and inner padding are close but not mathematically unified.
- Some buttons feel too visually heavy because the fill is louder than the surrounding composition.
- Bottom sheet styling is good, but it could be more refined in top handle spacing, title spacing, and row breathing room.
- Switches and control elements should feel like part of the same family as buttons and segmented controls.
- Some list rows still feel slightly too tall or too loose.

### 7) Some screens still feel “almost there” rather than resolved
The most obvious examples are the more technical screens.
- Shadow improved, but it still behaves more like a nice feature page than a fully exceptional premium feature explanation screen.
- Smart Routing is much better in Russian now, but still feels text-heavy and slightly dry compared with the rest of the app.
- Applications & Rules has better structure, but the composition still needs tighter balancing between selector, search, list, meta rows and CTA.
- Connected / disconnected dashboards need more precise spacing and better hero-to-stats integration.

---

## Screen-by-screen critique

## 01 — Splash
### What works
- Strong brand moment.
- Fox asset remains premium and memorable.
- The brand color palette is consistent.

### What fails
- The planet still reads like a soft arc/dome more than a fully integrated planetary mass.
- The fox is beautiful but slightly too detached from the brand lockup below.
- There is too much unstructured empty space in the middle-to-lower screen.
- The logo block feels slightly low and isolated.
- Loading bar is visually acceptable, but it does not feel uniquely designed for the brand; it feels generic.

### Fix direction
- Make the planet read as a darker large volume with a softer, more cinematic copper edge.
- Improve relation between fox, planet and logotype so they feel like one intentional composition.
- Tighten vertical composition so the screen feels authored, not just sparse.

## 02 — Onboarding subscription start
### What works
- Good editorial simplicity.
- CTA is clear.
- Planet integration is better than older pass.

### What fails
- The hero cluster is still a little too small relative to the page.
- The copy block and hero do not yet feel perfectly balanced.
- The lower purchase option is too quiet and slightly detached.
- The screen still feels more like a wireframe with nice art than a fully composed onboarding step.

### Fix direction
- Re-balance title/copy/hero/CTA stack.
- Slightly increase hero impact or improve its relation to the text block.
- Tighten the secondary action placement.

## 03 — Best server automatically
### What works
- Good minimal screen.
- Clear focus.
- Clean CTA logic.

### What fails
- The screen is almost too empty.
- The network diagram could be more refined and better centered in the emotional structure of the page.
- The title breaks are not as elegant as they could be.
- The secondary action “Выбрать вручную” still feels weak and under-connected.

### Fix direction
- Preserve concept, but refine spacing and microcomposition.
- The illustration should feel more intentionally staged.

## 04 — All set
### What works
- One of the best screens.
- Clean and product-like.
- Simple, clear and premium.

### What fails
- Minor only: alignment and spacing can be micro-polished.
- The device illustration/check relationship can be tuned slightly.
- Permission note could be better integrated typographically.

### Fix direction
- Do not redesign. Only micro-polish.

## 05 — Home disconnected
### What works
- Strong direction overall.
- Main CTA is clear.
- Card stack is readable.

### What fails
- Planet still needs refinement.
- The hero section feels a bit visually separated from the control/dashboard section.
- “Нет разрешения VPN” is visually weak and too disconnected from CTA logic.
- The informational block at the bottom is not composed strongly enough.
- The top logo + heading + subheading block could be tighter.

### Fix direction
- Make the hero and dashboard feel like one system.
- Refine planet realism and depth.
- Clarify message hierarchy: state -> issue -> action -> settings overview -> helper info.

## 06 — Connecting
### What works
- Good idea with progress line and staged steps.
- Functional and understandable.

### What fails
- The composition is busy in the upper half and weak in the lower half.
- The progress line looks slightly too thin and generic.
- Step list alignment and visual rhythm need tightening.
- The ghosted content underneath is okay, but feels a bit muddy.

### Fix direction
- Improve progress component design.
- Tighten step list typography and spacing.
- Make the overlay state feel more deliberately designed.

## 07 — Home connected
### What works
- Green state headline has good contrast.
- Same dashboard pattern maintains consistency.

### What fails
- Same hero/planet issue as 05.
- Stats row is a bit too weak versus the size of the hero region.
- The flow diagram “Вы -> HotFox -> Интернет” is understandable, but visually slightly lightweight.
- There is too much low-contrast information packed into the lower half.

### Fix direction
- Increase coherence between hero, stats and flow model.
- Better integrate the dashboard card.
- Make the state feel more triumphant without becoming noisy.

## 08 — Disconnected with bottom sheet
### What works
- Sheet shape and overall logic are good.
- Menu options are understandable.

### What fails
- The large brown-orange CTA fill looks heavier/dirtier here than in other screens.
- The bottom sheet content is still a little under-spaced.
- The relationship between overlayed home screen and sheet could be more elegant.
- English “Always-on VPN / Kill Switch” inside Russian menu stands out too harshly.

### Fix direction
- Refine orange balance under dimming.
- Improve spacing in the sheet.
- Handle localization / mixed labels more intentionally.

## 09 — HTTPS subscription
### What works
- Clean, simple input flow.

### What fails
- The page is too empty.
- The fox/planet fragment at the bottom feels decorative rather than structurally useful.
- Input row and CTA do not yet feel like a premium form system.
- Explanatory text is tiny/weak.

### Fix direction
- Strengthen form hierarchy.
- Decide whether the visual fox accent helps or distracts; if kept, integrate it more meaningfully.

## 10 — Servers
### What works
- List is readable.
- Good use of restraint.

### What fails
- The top filter row and status line are somewhat cramped and not fully harmonized.
- List row rhythm can still be refined.
- The automatic best server item should feel more premium and distinct.
- Flags are fine, but supporting country/latency text can be cleaner.

### Fix direction
- Improve tab hierarchy and spacing.
- Make the special first row more designed.

## 11 — Server details
### What works
- Nice minimal detail page.
- Good information architecture.

### What fails
- Some rows feel too light and too similar in emphasis.
- The descriptive footer note is underpowered.
- The disabled button block near bottom feels visually muddy.

### Fix direction
- Sharpen primary vs secondary hierarchy.
- Improve row-value pairing and footer treatment.

## 12 — Subscription
### What works
- Better than before.
- Straightforward structure.

### What fails
- Still too empty and slightly brittle.
- The card content feels sparse rather than luxurious.
- The lower legal/payment note is too small and too washed out.
- Two buttons are serviceable, but their relationship could be more refined.

### Fix direction
- Make subscription screen feel higher value and more premium.
- Improve density without clutter.

## 13 — Settings
### What works
- Clean organization.
- Much better than older mixed-language version.

### What fails
- Slightly too plain compared with brand-rich hero screens.
- Section spacing can still improve.
- Some rows could use clearer grouping logic.
- Bottom nav + list + header relationship is okay but not exceptional.

### Fix direction
- Increase polish and hierarchy clarity.
- Keep simple, but more premium.

## 14 — Smart Routing / Умная маршрутизация
### What works
- Major improvement in localization.
- Screen finally feels more coherent.

### What fails
- It is still text-heavy.
- The section headers are a bit too loud or too separated compared to the content beneath.
- The toggle row is functional but not visually elegant.
- CTA at the bottom feels somewhat appended rather than integrated.

### Fix direction
- Improve breathing room and reduce the perception of density.
- Make technical content feel calmer and more “designed.”

## 15 — Applications & Rules
### What works
- Strong improvement in structure.
- Clearer top segmentation, search and list.

### What fails
- Still a little long and “listy.”
- App rows could be tighter and more systematized.
- The empty gap between app list and lower utility rows needs better compositional handling.
- Checkbox/toggle treatment should feel more premium.

### Fix direction
- Tighter list rhythm.
- Better use of vertical space.
- Stronger component consistency.

## 16 — Autopilot
### What works
- Solid list layout.
- Better balance than before.

### What fails
- Slightly too utilitarian.
- Toggle accent is somewhat disconnected in feel from the rest of the system.
- Supporting note at bottom is too weak.
- The screen could have a more defined identity.

### Fix direction
- Improve row styling and emphasis model.
- Make CTA anchoring cleaner.

## 17 — Shadow
### What works
- Much more informative.
- Better than earlier versions.

### What fails
- Still not emotionally premium enough for a flagship feature.
- The line illustration is okay, but not yet iconic.
- The entire top visual block needs stronger artistic resolution.
- Data rows are fine, but the page could do more to explain value through structure and tone.

### Fix direction
- Make this screen feel like a signature feature screen, not just another settings detail screen.
- Visual system around the shield must feel unique and elegant.

## 18 — Always-on VPN
### What works
- Minimal and understandable.

### What fails
- Still visually underdeveloped.
- The shield illustration is too generic and lacks brand character.
- Too much dead air without enough deliberate composition.
- Explanatory text is weak.
- CTA works functionally but not emotionally.

### Fix direction
- Give this screen more weight and visual confidence.
- Improve iconography, illustration and layout structure.

---

## Core mandatory fixes summary
1. Rebuild the planet so it looks like a real compositional volume, not a glowing dome.
2. Standardize hero composition across screens 01 / 02 / 05 / 07 / 08 / 09.
3. Tighten vertical rhythm everywhere.
4. Normalize text hierarchy screen to screen.
5. Standardize cards, buttons, list rows, toggles, segmented controls and sheets.
6. Make technical screens feel as premium as brand screens.
7. Refine Shadow and Always-on so they feel flagship, not secondary.
8. Preserve the current direction; do not invent a new style.
