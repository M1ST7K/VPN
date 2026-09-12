# HOTFOX V11 — Acceptance Checklist

## Commit/package
- [ ] V11 art-direction TXT present.
- [ ] V11 implementation-spec TXT present.
- [ ] V11 execution-prompt TXT present.
- [ ] V11 acceptance checklist present.
- [ ] V11 owner-override Cursor rule present.
- [ ] Planet reference A committed and readable.
- [ ] Planet reference B committed and readable.
- [ ] Existing clean production fox foreground preserved.
- [ ] Existing combined fox+planet retained only as a historical reference, not production composite.
- [ ] Asset manifest matches committed bytes.

## Planet fidelity
- [ ] No simplified vector arc/dome used in place of approved planet.
- [ ] Dark spherical mass visible.
- [ ] Surface texture preserved in character.
- [ ] Directional copper/orange rim matches approved references.
- [ ] No uniform neon outline.
- [ ] No double parallel arc.
- [ ] No stars/orbit/radar decoration.
- [ ] Fox remains foreground.
- [ ] No planet fragments leak through fox alpha edges.
- [ ] No matte rectangle around fox.

## Required planet screens
- [ ] 01 uses approved V11 planet treatment.
- [ ] 02 uses approved V11 planet treatment.
- [ ] 05 uses approved V11 planet treatment.
- [ ] 06 uses approved V11 planet treatment; progress remains separate.
- [ ] 07 uses approved V11 planet treatment.
- [ ] 08 shows the real 05 composition behind the sheet.
- [ ] 09 uses approved treatment if fox hero remains.

## Screens protected from unnecessary planet use
- [ ] 03 no forced planet; micro-polish only.
- [ ] 04 no forced planet; micro-polish only.
- [ ] 10–18 no planet unless exact approved reference proves otherwise.

## Phone responsiveness
- [ ] Compact phone checked.
- [ ] Regular phone checked.
- [ ] No text/hero collisions.
- [ ] No CTA/card/navigation collisions.
- [ ] No crop seams.
- [ ] Hero scale feels intentional.

## Tablet responsiveness
- [ ] Small tablet portrait checked.
- [ ] Large tablet portrait checked.
- [ ] Hero does not become tiny in a huge empty canvas.
- [ ] Text reading width is constrained.
- [ ] Planet/fox relative geometry is preserved.
- [ ] CTA width/position adapts intentionally.
- [ ] Bottom navigation/cards remain balanced.

## Localization/UI polish
- [ ] Approval captures are ru-RU.
- [ ] No accidental mixed-language generic labels.
- [ ] 03/04 concept not regressed.
- [ ] Utility screens remain premium and readable.
- [ ] Bottom-sheet scrim does not dirty brand orange.

## Technical regression
- [ ] Debug fixtures remain under src/debug.
- [ ] No fake production server/subscription/traffic/ping/CONNECTED state.
- [ ] VPN datapath unchanged.
- [ ] DNS/IPv6 fail-closed unchanged.
- [ ] Routing/Shadow/Autopilot semantics unchanged.
- [ ] Build PASS.
- [ ] Unit tests PASS.
- [ ] Static check PASS.
- [ ] Fixture leak check PASS.

## Required output to owner
- [ ] 18 individual phone screenshots provided separately.
- [ ] One contact sheet provided.
- [ ] Tablet screenshots for 01/02/05/06/07/08/09 provided separately.
- [ ] Representative tablet utility screenshots provided.
- [ ] Screen-by-screen changelog provided.
- [ ] Planet fidelity report provided.
- [ ] Responsive phone/tablet report provided.

## Release status
- [ ] No merge performed by this task.
- [ ] No RELEASE READY claim based on UI work alone.
