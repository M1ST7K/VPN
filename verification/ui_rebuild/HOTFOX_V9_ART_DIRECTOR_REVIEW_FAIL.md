# HOTFOX V9 — OWNER ART-DIRECTOR REVIEW
## STATUS: V8 FAIL / REQUIRES FINAL PAGE-BY-PAGE REFINEMENT

Date: 2026-09-12
Reviewed branch: `cursor/hotfox-ui-pixel-lock-rebuild`
Reviewed V8 head: `d1ebc4e78904150106f8eeded4a0a9c867b1acd7`

## Evidence status

The V8 engineering report is honest and remains the baseline:
- 18/18 ru-RU emulator captures exist;
- 18/18 comparison sets exist;
- visual/pixel PASS = 0/18;
- art-director PASS = 0/18;
- build/tests/static/leak gates pass;
- RELEASE READY = NO;
- physical/runtime VPN E2E was not executed.

V8 is a real improvement, but it is not yet visually accepted.

## Executive verdict

The product is finally recognizably HotFox, but the last 15–20% of design quality is still missing. The remaining gap is not “add more graphics”. It is precision: composition, hierarchy, localization consistency, optical alignment, contextual CTA weight, and making the utility screens feel authored rather than themed Android settings.

The fox + planet direction is correct and must remain. However, the planet must continue to be treated as an atmospheric compositional mass, not a decorative space motif. The current filled planet is substantially better than the old double-arc implementation, but several fox screens still need scale/crop/rim tuning to match the references more naturally.

Screens 03 and 04 remain the strongest directionally and should be treated as anchors. Do not conceptually redesign them.

## System-level problems visible in V8

1. **Planet integration still varies by page.** On 01/02/05/06/07/09, the planet is present but the horizon height, fox scale, rim brightness and crop relationship are not yet one consistent family.
2. **Fox and planet sometimes read as one illustration block rather than part of the page composition.** The hero must feel connected to title/body/CTA.
3. **Typography is inconsistent in density.** Hero screens are generally stronger than utility screens. Utility rows often resemble standard Android preference layouts.
4. **Mixed-language copy remains unacceptable.** A ru-RU approval capture cannot show English screen/row labels such as `Smart Routing` or `Custom rules` unless the approved product terminology explicitly requires English. V9 must audit all visible strings.
5. **CTA hierarchy remains too heavy on several subpages.** The same orange visual mass cannot dominate onboarding, home, routing, rules, Shadow and system handoff equally.
6. **Utility screens 11/12/13/14/16/18 still feel under-authored or sparse.** They need better editorial grouping, not more giant cards.
7. **Screen 15 improved materially but still needs a final geometry and density pass.** It must stop looking like a debug app list and become a polished rules manager.
8. **Shadow is still not a flagship feature screen.** The topology lines look decorative rather than semantically protective.
9. **Negative space is still uneven.** Some pages use air intentionally; others simply have empty lower fields.
10. **Header / nav / icon optical consistency needs one final pass.** Shared chrome should visibly feel shared.

## Owner planet direction — V9 lock

The owner explicitly wants the planet behind the fox to make the composition whole, as in the references.

Planet-backed fox screens:
- 01 Splash
- 02 Onboarding Connect
- 05 Disconnected
- 06 Connecting
- 07 Protected
- 09 HTTPS Subscription

Screen 08 inherits the real 05 background under the sheet.

No planet on:
03, 04, 10, 11, 12, 13, 14, 15, 16, 17, 18.

### Planet quality requirements

The V8 filled-planet direction is correct. V9 must refine it, not replace it with the old double-arc/vector bowl.

Planet must have:
- a dark filled body only slightly separated from the app background;
- one principal copper/orange atmospheric rim;
- soft falloff, not a hard glowing ring;
- fox natural occlusion over the horizon;
- page-specific crop/scale while remaining one family;
- no stars, satellites, orbit rings, radar circles, nebulae or sci-fi clutter;
- no baked text/UI;
- fox and planet remain separate layers.

The planet is a compositional foundation, not a second hero.

## Page-by-page art-director diagnosis

### 01 — Splash
V8 is more cinematic, but the upper hero still floats away from the wordmark block. The planet horizon is visually strong enough that it competes with the fox. Refine planet opacity/rim, hero vertical position, wordmark scale, tagline tracking and loading track. The final image should read as one continuous brand entrance rather than illustration + separate logo footer.

### 02 — Onboarding Connect
Direction is good. Planet and fox give needed mass, but the hero block still feels inserted between copy and actions. Tighten title/body measure, hero scale, planet crop and action spacing. Secondary “Купить доступ” must feel deliberate rather than leftover text.

### 03 — AUTO
Owner anchor. Keep concept. Only optical micro-polish: line weights, node sizes, chosen path, exact title wrap, CTA and secondary-action spacing. No planet.

### 04 — Ready
Owner anchor. Keep concept. Only optical micro-polish: server/check geometry, title/body rhythm, permission note and CTA. No planet.

### 05 — Disconnected
The page is close but still generic in the lower half. Hero planet/fox needs more seamless transition into permission text and CTA. The four-row option group is too preference-like; refine surface weight, separators, value column, icon sizing and helper notice. The home screen must feel product-authored.

### 06 — Connecting
The hero is improved, but the stage rail still reads like diagnostics. The connection process should dominate; the option group should recede. Stop action must feel safe and controlled. Planet is static background only; progress is separate and non-orbital.

### 07 — Protected
Too many small information systems compete: success title, timer, hero, traffic, route chain, disconnect control, options and nav. Calm the hierarchy. Green only for genuine success semantics. Planet must be atmospheric, not a visible ring graphic.

### 08 — Add Connection Sheet
Functionally good, visually still close to a system sheet. Scrim currently muddies the orange content beneath. Tighten handle/title/close geometry, row density, icon/chevron columns, corner radius and dim behavior. The underlying live 05 hero should remain perceptible but subordinate.

### 09 — HTTPS Subscription
Still one of the weakest compositions. The form and hero feel like separate zones. Make the import task primary; use fox+planet as trust/privacy support. Reduce empty field, improve URL field/paste/helper/CTA geometry, and make hero scale/crop reference-aware.

### 10 — Servers
One of the strongest utility screens. Do not over-design. Polish header/tabs/section label, AUTO row, flags, ping column, separators and chevrons. Keep ping neutral unless the exact reference explicitly uses green.

### 11 — Server Details
Still too sparse and preference-like. Strengthen information hierarchy with typography, aligned right-value column and restrained separators. Do not solve with a giant card. Keep safe debug fixture values only in debug.

### 12 — Subscription
Still reads like generic billing/account UI. Strengthen plan identity, entitlement/expiry/device/server hierarchy, primary and secondary actions and legal/support copy. Use space intentionally so the lower half no longer feels unfinished.

### 13 — Settings
Still too close to stock Android Settings. Improve section eyebrows, row density, visible icon scale, separator rhythm, chevron alignment and header/body footprint. Keep content-over-containers; do not reintroduce giant cards.

### 14 — Smart Routing
Critical visual/copy pass. All visible copy in ru-RU must be consistently localized. The current information density is high and hierarchy weak. Group top-level mode, apps/rules, DNS/LAN/IPv6/custom/reconnect into a clear control architecture. CTA must belong to the flow instead of dominating the page.

### 15 — Apps & Rules
Much improved, but not finished. Finalize title/intro, segmented control, search, app-list density, real-looking fixture icons, title/package/category hierarchy, selection control geometry, summary rows and Save action. No scrollbar or content clipping. Production installed-app behavior stays real.

### 16 — Autopilot
Still generic and a little sparse. Improve master-switch hierarchy, rule grouping, helper/status language and CTA placement. The page should read as a coherent policy editor, not a stack of settings rows.

### 17 — Shadow
Still below flagship quality. The topology around the shield looks ornamental. Rebuild as intentional protected-network routing geometry with purposeful nodes/paths. Refine shield stroke, copy width, row hierarchy and CTA weight. No planet, no orbit, no cyberpunk.

### 18 — Always-on VPN
Truthful but visually under-composed. Rebalance shield, two Android-state rows, handoff CTA and helper note. Keep truthful `Unknown`/system state if that is what Android exposes. No fake lockdown state.

## V9 priorities

### P0
- complete ru-RU copy audit;
- preserve planet on fox screens and refine atmospheric composition;
- no production fixture leak;
- no VPN/security/runtime regression;
- preserve 03/04 direction;
- fresh 18/18 evidence.

### P1
- rebuild utility-screen hierarchy 11/12/13/14/16/17/18;
- final screen 15 geometry/density pass;
- contextual CTA hierarchy;
- header/nav/icon optical consistency.

### P2
- baseline alignment, divider positions, micro-spacing, button arrows, copy line breaks, animation polish.

## Acceptance

V9 is accepted visually only when the new contact sheet looks like one authored product, not a mixture of strong hero pages and themed system pages. Build/test success alone is not visual acceptance. Do not merge. RELEASE READY remains a separate runtime/physical-device gate.
