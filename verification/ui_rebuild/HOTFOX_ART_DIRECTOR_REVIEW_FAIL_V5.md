# HOTFOX — ART DIRECTOR REVIEW V5
## Status: FAIL / VISUALLY NOT ACCEPTED / NEXT REFINEMENT REQUIRED

Date: 2026-09-11
Branch reviewed: `cursor/hotfox-ui-pixel-lock-rebuild`
Reviewed V4 head: `81567d59071612c2fc5772d0bbd94f500081ddb6`

V4 is a meaningful engineering improvement, but the UI is still not visually accepted. The current build has 18/18 emulator captures and 18/18 comparisons, yet 0/18 screens pass visual/pixel review.

## Global findings

1. The fox artwork still looks pasted in. On screens 01/02/05/07/09 the fox sits inside an opaque near-black rectangle. This matte is visible against the page background and immediately breaks the premium look. The replacement must be genuinely transparent: alpha 0 outside the silhouette, alpha ≈255 inside the fox, partial alpha only on the 1–2 px anti-aliased outer contour. Do not repeat the earlier broken extraction where dark fur became translucent.

2. Typography is still not one coherent editorial system. Some root titles are oversized, while filters/captions on screens 10/14/15/16 are too small. Line lengths, weights, section labels and runtime values feel tuned independently rather than art-directed.

3. Full-width orange pill CTAs are overused. The same loud primitive appears across onboarding, home and subpages. Orange should be a precise brand accent, not the automatic answer for every action.

4. Several screens have dead space rather than intentional negative space. The lower halves of 03/04/10/12/14/15/16/17/18 often feel empty because content is clustered at the top without a deliberate visual endpoint.

5. Headers are inconsistent: HotFox-only, back+HotFox, HotFox+gear, different top offsets and glyph sizes. Variants are allowed, but they must be explicit templates driven by the per-screen reference, not accidental XML drift.

6. Bottom navigation is improved but still reads as generic app chrome. Icon optical weight, inactive contrast, active orange and label scale need refinement. Exact per-screen presence must win over global normalization.

7. Icon family needs optical sizing. Same nominal 24dp produces different perceived sizes for gear/crown/server/lock/shield/network/DNS/IPv6/info/chevrons.

8. Visual QA still uses the wrong presentation state on some screens. Production truth must remain truthful, but the debug screenshot harness should render the approved reference state. Screens 10/12/15 should use deterministic reference-state fixtures for visual comparison while release remains real and unfaked.

9. Screen 03 still reads like a small solar system: a central globe surrounded by closed orbital circles. Screen 04 also uses concentric radar/orbit rings. The owner’s “never use planet/orbit aesthetics” directive is now extended to these compositions. Routing/network visuals may use paths and nodes, but not closed planetary/orbital systems.

## Screen-by-screen review

### 01 Splash
- Fox matte rectangle visible.
- Wordmark sits too low and feels detached from hero.
- Loading line is generic.
- Large middle gap lacks tension.
- Rebalance hero/wordmark/loading as one composition.

### 02 Onboarding Connect
- Fox matte visible.
- Art dominates while title/body are comparatively weak.
- CTA is the same generic pill.
- Secondary action feels like an afterthought.
- Rebalance art, copy and actions.

### 03 AUTO
- Title wraps awkwardly.
- Globe + multiple orbit rings reads like a planetary system.
- Lower half feels empty.
- Replace with intelligent routing topology: candidates, nodes, one chosen route, no closed orbit rings.

### 04 Ready
- Concentric rings read like radar/orbit language.
- Server/check symbol is visually lost inside large circles.
- Title/body/art/note/CTA feel disconnected.
- Keep completion symbol; remove planetary/radar composition.

### 05 Disconnected
- `Не защищено` is too large/heavy.
- Body is too close in visual weight.
- Fox matte rectangle is obvious.
- CTA is too large and dominant.
- Main row group is still heavy; second info card adds more container weight.
- Gear is optically small; nav still generic.
- Make it calmer, lighter and more editorial.

### 06 Connecting
- Too crowded: title, ring, stages, CTA, settings rows and nav all compete.
- Connection state should be the single focal story.
- Make stages clearer but quieter; de-emphasize background controls.

### 07 Protected
- Too dense.
- Green state title is useful, but metrics are tiny.
- Fox matte remains.
- Route/traffic details are cramped.
- Simplify and create a calm success hierarchy.

### 08 Add Connection Sheet
- Sheet feels almost full-screen.
- Title and row icons are too large.
- Rows are too tall with excessive gaps.
- Close icon is visually aggressive.
- Scrim is too dark; background context nearly disappears.
- Make the sheet more compact and editorial while preserving native semantics.

### 09 HTTPS Subscription
- Huge fox matte rectangle at bottom.
- Artwork competes with the actual form task.
- Input/action feel generic.
- Compact the workflow; keep focus/error/paste native.

### 10 Servers
- Filter labels too small.
- Selected state too weak.
- Row is squeezed.
- Lower half is dead.
- Empty production state is truthful, but visual QA must use a debug populated reference state if the approved board is populated.

### 11 Server Details
- Sparse utility list.
- Right values too weak.
- Disabled CTA treatment is too faint.
- Use deterministic debug data matching reference density and align values consistently.

### 12 Subscription
- Production-unavailable state makes the visual-QA capture look unfinished.
- Tiny gray text and weak disabled control.
- Giant empty lower zone.
- Keep production truth, but use a debug reference entitlement state for board comparison.

### 13 Settings
- V4 removed heavy cards but overshot into a raw Android list.
- Row heights and icons are too large.
- Header/title/body stack consumes too much vertical space.
- Section labels are too weak.
- Add subtle hairlines/group rhythm without returning to big cards.

### 14 Smart Routing
- Small cluster floating in huge empty area.
- Generic settings-row feel.
- CTA sits awkwardly.
- Build a clearer routing hierarchy and use a debug selected reference state when needed.

### 15 Apps & Rules
- Massive empty field.
- Search/segmented control are generic.
- Too few list rows.
- CTA floats alone.
- Screenshot harness should provide fixture apps/rules matching reference density; production remains real installed apps.

### 16 Autopilot
- Large dead space.
- Switch is optically heavy.
- Rows look generic.
- CTA is disconnected from the decision flow.

### 17 Shadow
- Network lines are messy and cross behind the shield without intentional geometry.
- Shield is too thick.
- Body copy is too large and wide.
- Rows float without subtle structure.
- CTA dominates.
- Redraw topology so it frames/supports the shield; no orbit language.

### 18 Always-on
- Shield/rows/CTA feel stranded in a large empty screen.
- Status values are too tiny.
- CTA dominates.
- Rebalance vertical composition and exact nav presence.

## V5 priority

P0:
- remove fox matte;
- remove orbital/planetary visual language from AUTO/Ready;
- use debug reference-state fixtures on 10/11/12/15 where needed;
- preserve production truth.

P1:
- typography matrix;
- contextual CTA hierarchy;
- dead-space correction;
- header/nav templates;
- icon optical sizing.

P2:
- fine geometry, baselines, separators, disabled states, animation polish.

V5 cannot be accepted merely because 18 captures exist. It needs a coherent authored product feel, zero matte rectangles, zero planetary/orbit language, reference-state visual fixtures, contextual CTA hierarchy, and an explicit 18-screen art-director score.
