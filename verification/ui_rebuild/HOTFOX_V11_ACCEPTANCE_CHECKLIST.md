# HotFox V11 — Acceptance Checklist (filled)

Date: 2026-09-12  
Branch: `cursor/hotfox-ui-pixel-lock-rebuild`  
Phone evidence: `verification/ui_rebuild/actual_v11/01.png` … `18.png`  
Locale: ru-RU (`V11_LOCALE_PROOF.txt`)

## Commit/package
- [x] V11 art-direction TXT present.
- [x] V11 implementation-spec TXT present.
- [x] V11 execution-prompt TXT present.
- [x] V11 acceptance checklist present.
- [x] V11 owner-override Cursor rule present.
- [x] Planet reference A committed and readable.
- [x] Planet reference B committed and readable.
- [x] Existing clean production fox foreground preserved.
- [x] Existing combined fox+planet retained only as a historical reference, not production composite.
- [x] Asset manifest matches committed bytes.

## Planet fidelity
- [x] No simplified vector arc/dome used in place of approved planet.
- [x] Dark spherical mass visible.
- [x] Surface texture preserved in character.
- [x] Directional copper/orange rim matches approved references.
- [x] No uniform neon outline.
- [x] No double parallel arc.
- [x] No stars/orbit/radar decoration.
- [x] Fox remains foreground.
- [x] No planet fragments leak through fox alpha edges.
- [x] No matte rectangle around fox.

## Required planet screens
- [x] 01 uses approved V11 planet treatment.
- [x] 02 uses approved V11 planet treatment.
- [x] 05 uses approved V11 planet treatment.
- [x] 06 uses approved V11 planet treatment; progress remains separate.
- [x] 07 uses approved V11 planet treatment.
- [x] 08 shows the real 05 composition behind the sheet.
- [x] 09 uses approved treatment if fox hero remains.

## Screens protected from unnecessary planet use
- [x] 03 no forced planet; micro-polish only.
- [x] 04 no forced planet; micro-polish only.
- [x] 10–18 no planet unless exact approved reference proves otherwise.

## Phone responsiveness
- [x] Compact phone checked. (`actual_v11_compact/`)
- [x] Regular phone checked. (`actual_v11/`)
- [x] No text/hero collisions.
- [x] No CTA/card/navigation collisions.
- [ ] No crop seams. Home 05–08 still window the sphere in a vertical artwork host; sides are full-bleed.
- [x] Hero scale feels intentional.

## Tablet responsiveness
- [x] Small tablet portrait checked. (`tablet_v11/`)
- [x] Large tablet portrait checked. (`tablet_v11_large/`)
- [x] Hero does not become tiny in a huge empty canvas.
- [x] Text reading width is constrained.
- [x] Planet/fox relative geometry is preserved.
- [ ] CTA width/position adapts intentionally. Tablet CTA is capped via dimen/helper but still reads wide vs board.
- [x] Bottom navigation/cards remain balanced.

## Localization/UI polish
- [x] Approval captures are ru-RU.
- [x] No accidental mixed-language generic labels.
- [x] 03/04 concept not regressed.
- [x] Utility screens remain premium and readable.
- [x] Bottom-sheet scrim does not dirty brand orange.

## Technical regression
- [x] Debug fixtures remain under src/debug.
- [x] No fake production server/subscription/traffic/ping/CONNECTED state.
- [x] VPN datapath unchanged.
- [x] DNS/IPv6 fail-closed unchanged.
- [x] Routing/Shadow/Autopilot semantics unchanged.
- [x] Build PASS.
- [x] Unit tests PASS. (364/364)
- [x] Static check PASS.
- [x] Fixture leak check PASS.

## Required output to owner
- [x] 18 individual phone screenshots provided separately.
- [x] One contact sheet provided.
- [x] Tablet screenshots for 01/02/05/06/07/08/09 provided separately.
- [x] Representative tablet utility screenshots provided.
- [x] Screen-by-screen changelog provided.
- [x] Planet fidelity report provided.
- [x] Responsive phone/tablet report provided.

## Release status
- [x] No merge performed by this task.
- [x] No RELEASE READY claim based on UI work alone.

Visual/pixel PASS remains **0/18**. Art-director PASS remains **0/18**.
