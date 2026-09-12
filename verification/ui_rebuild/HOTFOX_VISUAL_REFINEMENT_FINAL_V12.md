# HotFox V12 — unified fox+planet hero

Date: 2026-09-12  
Branch: `cursor/hotfox-ui-pixel-lock-rebuild`

## Goal

Replace the round-card home/onboarding/HTTPS crops with one full-bleed
`HotfoxHeroView` composition: oversized filled planet sphere + uncropped
transparent fox. Splash, disconnected, connecting, connected, permission and
HTTPS share the same focal system.

## Production changes

- `HotfoxHeroComposition` — viewport math (planet overflow, fox fit box)
- `HotfoxHeroView` — reusable layered hero (`hf_native_planet_sphere` + `hf_fox_bust_transparent`)
- Home `artwork_host` is taller and full-bleed; no `centerCrop` on the fox
- Onboarding 02 uses the same PAGE variant; 03/04 still hide planet and show AUTO/Ready art
- Splash uses SPLASH variant of the same sphere+fox, not a separate crop
- HTTPS uses SUPPORT variant (same crop, quieter alpha)
- Primary CTA height/radius/padding unified via `hf_cta_height` / `hf_native_primary`
- Bottom nav locked to `hf_bottom_nav_height`

## Not changed

VPN datapath, Xray/HEV, entitlement, payment, CONNECTED truth, debug fixture containment.

## Evidence

Phone captures: `verification/ui_rebuild/actual_v12/`  
Locale: ru-RU (`V12_LOCALE_PROOF.txt`)  
Runtime/physical VPN E2E: NOT EXECUTED  
RELEASE READY: NO
