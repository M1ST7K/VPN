# HotFox V7 — owner art-director review

Date: 2026-09-12
Starting point: V6 HEAD `1686630d68c1c95e7e377414544bc81927aefd04`
Branch: `cursor/hotfox-ui-pixel-lock-rebuild`

## Verdict

V6 is an important technical step forward but is still **NOT visually accepted**.
Repository evidence itself says:
- 18/18 emulator captures in ru-RU;
- 18/18 comparisons;
- visual/pixel PASS 0/18;
- art-director PASS 0/18;
- RELEASE READY = NO.

Screens 03 and 04 are the strongest compositions in the current set and are now owner-approved directionally. They must be treated as visual anchors and should receive only micro-polish, not conceptual redesign.

The latest owner direction explicitly changes one earlier art rule: on the remaining screens where the fox hero is present, add a **subtle planet behind the fox** to make the composition feel complete. This is a deliberate owner override of older blanket no-planet instructions. The planet is allowed only as a restrained background composition layer on fox screens; it is not a global motif and does not authorize orbital/radar/celestial decoration elsewhere.

Do not revive the old screenshot-derived `hf_fox_planet` composite. The fox and planet must remain separate clean layers so layout, scale and opacity are controllable.

## Global design critique

1. V6 still alternates between premium editorial screens and generic Android utility screens. The product does not yet feel authored as one system.
2. Fox screens still feel compositionally incomplete: transparent fox is clean, but on 01/02/05/06/07/09 it often floats in an empty field. A restrained dark planet behind the fox should provide visual mass and depth.
3. Negative space is inconsistent. Several screens use intentional calm space, while others simply look under-filled.
4. Typography still varies too much between hero screens, settings screens and list screens. A single editorial type hierarchy must connect all 18.
5. CTA hierarchy improved, but some actions remain too template-like. Orange must remain precise and contextual.
6. Settings/list screens still drift toward stock Android. Use subtle editorial grouping, optical icon sizing and exact vertical rhythm without returning to heavy card stacks.
7. Screens 03 and 04 should become anchors for restraint, spacing and abstraction quality.
8. The current V6 QA state for screen 10 still does not reliably show the intended populated server fixture; V7 must fix the debug QA binding while preserving truthful production data.

## Screen-specific review

### 01 Splash
- Clean fox cutout is good.
- Current hero and HotFox wordmark feel separated by too much dead space.
- Add a low-contrast planet behind fox, partly cropped, with restrained warm rim.
- Tighten fox/brand/loading relationship into one scene.

### 02 Onboarding — Connect HotFox
- Current layout is clearer, but the fox still floats.
- Add subtle planet behind fox.
- Keep copy, art and CTA on one measured vertical rhythm.
- Secondary action should remain quiet but clearly intentional.

### 03 AUTO
- Owner considers this screen strong.
- Freeze concept.
- Only micro-polish line quality, optical alignment, type rhythm and CTA geometry.
- Do not add a planet or redesign the topology.

### 04 Ready
- Owner considers this screen strong.
- Freeze concept.
- Only micro-polish spacing, server/check alignment, note and CTA balance.
- Do not add a planet.

### 05 Disconnected
- Add subtle planet behind fox.
- Hero zone must feel integrated with headline and CTA.
- Main option group should be lighter and more editorial.
- Bottom helper notice should not feel like an unrelated extra card.

### 06 Connecting
- Add subtle planet behind fox.
- Keep the V6 removal of the orbital progress ring.
- Planet must be a static background mass, not an orbit/progress device.
- Progress rail/stages should feel calm and intentional.
- Stop action should be neutral and controlled, not destructive.
- De-emphasize lower settings while connecting.

### 07 Protected
- Add subtle planet behind fox.
- Keep green limited to success semantics.
- Hero, timer, traffic and path should read as one calm protected state.
- Keep the V6 fix that removes the contradictory VPN-permission warning from debug protected presentation.

### 08 Add connection sheet
- Keep native sheet behavior.
- Background is screen 05; if fox is visible, the fox+planet composition must remain coherent under scrim.
- Tighten title, row density, close glyph, handle and scrim opacity.
- Do not regress into stock Material sheet proportions.

### 09 HTTPS subscription
- Add subtle planet behind fox if fox remains.
- Form is primary; artwork is supporting.
- Avoid a decorative fox isolated in a large empty zone.
- Improve trust/privacy feel and task focus.

### 10 Servers
- V6 QA still failed to reliably bind populated debug rows.
- Fix QA fixture binding in debug only.
- Production catalog remains truthful.
- Refine tabs, row density, latency treatment and automatic server row.

### 11 Server details
- Preserve simplicity.
- Improve value-column alignment and information rhythm.
- Avoid feeling like a raw preference list.

### 12 Subscription
- Improve premium product hierarchy between plan status, expiry/device summary, primary action and recovery actions.
- Debug visual state may match approved board under src/debug only.

### 13 Settings
- Keep content-over-containers.
- Current screen is still close to stock Android settings.
- Improve section rhythm, icon optical weight, row density and brand tone without heavy cards.

### 14 Smart Routing
- Reduce dry/empty composition.
- Make mode/rules/LAN relationship clearer.
- Contextual CTA, not generic template button.

### 15 Apps & Rules
- Preserve V6 improvements: real-looking debug icons, no unintended scrollbar.
- Reduce visual noise in segmented control/search/rule summary.
- Improve app row density and checkbox optical alignment.

### 16 Autopilot
- Rebalance the sparse lower half.
- Refine switch optical size and row hierarchy.
- CTA should belong to the decision flow.

### 17 Shadow
- Keep fox absent.
- Keep planet absent.
- Refine branching topology and shield; network lines must feel intentional rather than random.
- No orbit bands around shield.

### 18 Always-on VPN
- Keep fox absent.
- Keep planet absent.
- Rebalance shield, truthful Android status rows and CTA into one finished composition.

## Planet composition contract

Allowed fox+planet screens: 01, 02, 05, 06, 07, 09. Screen 08 may show it only indirectly in the live background inherited from 05.

Planet rules:
- separate drawable/layer from fox;
- dark, restrained, mostly background;
- partial sphere or horizon allowed;
- subtle warm orange/copper rim permitted;
- no stars, nebulae, satellite dots, orbital rings, radar circles or space-poster treatment;
- must not be used on 03, 04, 10–18;
- must not appear around Shadow shield;
- must not be used as connection progress;
- do not restore legacy screenshot-derived fox+planet composites.

## Acceptance

V7 remains a visual task, not a release gate. Before visual acceptance:
- 18/18 new ru-RU captures;
- 18/18 comparison evidence;
- owner-approved planet treatment proved in-app on 01/02/05/06/07/09;
- screens 03/04 show no conceptual regression;
- production fixture containment remains PASS;
- build/tests/static checks PASS;
- VPN/runtime/security semantics unchanged;
- RELEASE READY remains NO until separate runtime/device validation.
