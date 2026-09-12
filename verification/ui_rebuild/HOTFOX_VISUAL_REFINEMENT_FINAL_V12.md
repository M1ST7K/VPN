# HotFox V12 — unified fox+planet hero

Date: 2026-09-12  
Branch: `cursor/hotfox-ui-pixel-lock-rebuild`  
HEAD at evidence: recorded after commit  

## Goal

Replace the round-card home/onboarding/HTTPS crops with one full-bleed
`HotfoxHeroView` composition: oversized filled planet sphere + uncropped
transparent fox. Splash, disconnected, connecting, connected, permission and
HTTPS share the same focal system.

## Production changes

- `HotfoxHeroComposition` — viewport math (planet overflow, fox fit box)
- `HotfoxHeroView` — reusable layered hero (`hf_native_planet_sphere` 1024px + `hf_fox_bust_transparent`)
- Fox uses `fitCenter` inside a padded box; never `centerCrop`
- Planet uses `MATRIX` overflow so the sphere can leave the frame as a mass, not a complete disk in a short band
- Home `artwork_host` is taller (300/360/440/520dp by breakpoint) and clips so the title stays readable
- Onboarding 02 uses PAGE variant; 03/04 still hide planet and show AUTO/Ready art
- Splash uses SPLASH variant of the same sphere+fox
- HTTPS uses SUPPORT variant (same crop, quieter alpha)
- Primary CTA height/radius/padding unified via `hf_cta_height` / `hf_native_primary`
- Bottom nav locked to `hf_bottom_nav_height`

## Evidence (ru-RU)

Phone 1080×2400 @420dpi: `verification/ui_rebuild/actual_v12/`
Captured: 01, 02, 03, 04, 05, 06, 07, 08, 09
Compact ~360dp: `actual_v12_compact/05.png`
Locale: `V12_LOCALE_PROOF.txt`

## Gates

- `:app:assemblePlaystoreDebug` PASS
- `:app:testPlaystoreDebugUnitTest` PASS (368/368)
- `check_no_fixture_leak_v51.py` PASS
- `static_check_2_2_0.py` PASS
- Physical/runtime VPN E2E: NOT EXECUTED
- RELEASE READY: NO

## Honest visual notes

- 01/02/05/06/07/09 no longer read as a round thumbnail card.
- Fox ears, muzzle and neck stay inside the hero on captured phone and compact frames.
- 05/06/07 share the same fox focal point; connecting/protected only change chrome under the hero.
- 03/04 remain without planet (concept freeze).
- Pixel-lock vs the 18-board set is still not claimed; this pass is a composition repair.

## Not changed

VPN datapath, Xray/HEV, entitlement, payment, CONNECTED truth, debug fixture containment.
