# HOTFOX V8 — OWNER ART-DIRECTOR REVIEW
## STATUS: FAIL / V7 NOT VISUALLY ACCEPTED / PAGE-BY-PAGE REFINEMENT REQUIRED

Date: 2026-09-12
Reviewed branch: `cursor/hotfox-ui-pixel-lock-rebuild`
Reviewed V7 head: `ab6606f83a584fa5f4a3c61819cfcb7c54e82ca1`

## Executive verdict

V7 is materially better than V6, but it is still not the final HotFox design. The repository evidence itself is honest: 18/18 ru-RU captures exist, 18/18 comparisons exist, but visual/pixel PASS is 0/18 and art-director PASS is 0/18. Build/tests/static/leak checks are green, while RELEASE READY remains NO.

The problem is no longer missing screens. The problem is visual authorship and exact composition. Several screens are close, but the set still alternates between premium editorial hero screens and utility screens that look too much like a themed Android application.

## Latest owner direction — planet behind fox

The owner explicitly wants the planet behind the fox on fox-led screens, as in the references, because the fox alone feels compositionally incomplete.

Required fox+planet screens: 01, 02, 05, 06, 07, 09. Screen 08 inherits live screen 05 behind the sheet.

03 AUTO and 04 Ready remain owner-approved directionally and must not gain a planet. 10–18 remain non-planet screens.

### V7 planet problem

The V7 planet is directionally useful but visually unfinished. It often reads as two mechanical semicircular wires / an outlined bowl beneath the fox. It feels like a vector UI shape rather than a deep background planet.

V8 must use:
- a real dark filled planetary mass, only slightly separated from the app background;
- one principal restrained copper/orange rim;
- optional very soft atmospheric falloff inside the rim;
- natural fox overlap/occlusion over the rim;
- fading rim endpoints;
- no second hard parallel outline;
- no star field, nebula, satellites, orbit rings, radar circles, texture-heavy space art or cyberpunk glow.

Fox and planet must remain separate layers. Never restore old baked `hf_fox_planet` / `hotfox_art_fox_planet_*` composites.

## Global V7 issues visible in the supplied captures

1. Typography is still not one coherent editorial system. Hero titles, utility titles, body copy, tabs and tiny runtime values have uneven optical weight.
2. CTA hierarchy is improved, but several actions still feel like templates rather than components authored for their page.
3. List/card treatment is inconsistent: some pages are still heavy containers, others are raw Android-like lists.
4. Negative space is frequently dead rather than intentional. 11, 12, 14, 16, 17 and 18 feel under-composed.
5. Header and bottom-navigation geometry still varies between screens that should share chrome.
6. Planet-backed fox scenes need one shared visual law instead of the current wire-arc backdrop.
7. Screen 15 remains the largest visual mismatch and needs real reconstruction, not micro-tweaks.
8. Screen 09 still looks like a form plus detached art instead of one trusted import flow.
9. Shadow topology still radiates around the shield too randomly.
10. V7 art-director scores remain below the 4.0/4.3 gate on every screen, which is consistent with the visible result.

## Screen-by-screen diagnosis

### 01 Splash
The planet improves the idea but the current scene is split: a large empty top field, fox+wire planet, then a separate oversized HotFox brand block and loading track. The planet needs a filled body and one subtle rim. Hero, wordmark, tagline and loading bar should read as a single vertical brand entrance, not two stacked compositions.

### 02 Onboarding Connect
The fox+planet gives the page mass, but the hero still feels mechanically inserted between text and actions. The planet is too arc-like. Copy, hero and CTA must share one measured rhythm. Secondary `Купить доступ` should look intentionally subordinate rather than simply leftover text.

### 03 AUTO
Owner likes this screen. Freeze the concept. No planet. Only micro-polish: topology line weights, node optical alignment, title wrapping, body spacing, CTA geometry and secondary action spacing.

### 04 Ready
Owner likes this screen. Freeze the concept. No planet. Only micro-polish server/check geometry, copy spacing, permission note and CTA placement.

### 05 Disconnected
The new hero is better, but the planet still looks like a drawn bowl. `Нет разрешения VPN` reads too technically. The option group is still heavy and generic; the helper notice feels like another unrelated block. The hero, permission state, CTA, option group and helper notice need one continuous editorial flow.

### 06 Connecting
The static planet is allowed, but the rail/stage stack still feels diagnostic. Connection progress must become the single visual story. Lower settings should be deliberately de-emphasized while preserving functionality. Stop must remain neutral/controlled, not destructive.

### 07 Protected
Too many small elements compete: green state, timer, fox+planet, transfer values, user→HotFox→Internet route, disconnect, option group and nav. The protected state should be calmer than disconnected. Planet must become deep background mass, not a visible ring graphic. Green remains success-only.

### 08 Add Connection Sheet
Functional but still system/Material-like. The scrim makes the orange underneath look muddy brown. Handle/title/close and rows need tighter premium rhythm. Background 05 must remain perceptible and preserve fox+planet composition under the scrim.

### 09 HTTPS Subscription
High mismatch. Form and art look like separate zones. The URL task must be primary; fox+planet should support trust/privacy rather than occupy a random lower gap. Input, paste action, helper copy, CTA and hero need one composition.

### 10 Servers
One of the stronger utility screens. Keep no planet. Improve header/tabs/`Мои серверы` rhythm, AUTO row, flag/city/country/ping/chevron columns and separator precision. Pings stay muted unless reference explicitly says otherwise.

### 11 Server Details
Too sparse and preference-like. Improve hierarchy between server identity, status/load/routing/security rows, right-column values and explanatory note. Use typography/hairlines rather than big cards.

### 12 Subscription
Still generic billing-page territory. Strengthen plan identity, expiry/device/server hierarchy, manage/renew/HTTPS actions and vertical balance. Remove dead lower area through composition, not invented data.

### 13 Settings
Still too stock Android. Section eyebrows, row density, optical icon size, separators and chevrons need a stronger HotFox rhythm without returning to card-heavy UI.

### 14 Smart Routing
Among the weakest compositions. The controls float in the upper area and the lower half is empty. Mode, apps/rules, LAN and Apply must become one deliberate control system.

### 15 Apps & Rules
Largest V7 diff. Needs the deepest utility reconstruction. Segmented selector, search, summary rows, app icons, labels, selection controls and Save CTA need one exact geometry system. No scrollbar. Debug fixture icons/data stay debug-only.

### 16 Autopilot
Sparse and generic. Master switch, Wi-Fi/mobile/pause rules, balance/status copy and CTA must be grouped into a coherent automation flow.

### 17 Shadow
Shield is understandable but topology still feels arbitrary/radiating. Build an intentional asymmetric protective network, not a starburst. No planet. Copy, status rows and CTA need stronger composition and less dead lower area.

### 18 Always-on VPN
Too sparse. Rebalance shield, truthful Android status rows, Android Settings action and helper copy. Exact reference decides nav. No planet and no fake Android status.

## V8 acceptance

V8 is not accepted because code changed or tests pass. It requires 18/18 new ru-RU captures and comparisons, no regression of 03/04, a genuinely atmospheric planet treatment on fox screens, a major correction of 15, intentional composition on sparse pages, preserved debug-fixture containment, passing build/tests/static/leak gates, unchanged VPN/security/runtime semantics, no merge and RELEASE READY remaining a separate gate.