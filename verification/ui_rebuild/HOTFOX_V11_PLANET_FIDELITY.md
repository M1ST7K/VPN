# HotFox V11 — planet fidelity report

Date: 2026-09-12  
Cues: `design/assets/v11/planet/hotfox_planet_reference_a.webp` (SHA256 e80ba822…) and `hotfox_planet_reference_b.webp` (SHA256 491d419a…).  
Boards: `design/hotfox_18_final_style_reference/` / normalized `reference_v4/`.  
Generator: `verification/ui_rebuild/generate_planet_v11.py`  
Proof: `verification/ui_rebuild/v11_planet_reconstruct/`

## Method

Compact 420px webps were **not** LANCZOS-stretched into the APK. The generator samples cue lighting/albedo, reconstructs a 2048px sphere (FBM grain + upper-right copper terminator + night falloff), then exports screen crops:

- `hf_native_planet_backdrop.png` — splash cinematic 1080×2400
- `hf_native_planet_onboarding.png` — 02 crop 1080×1400
- `hf_native_planet_home.png` — 05/06/07 band 1080×900
- `hf_native_planet_support.png` — 09 quieter 1080×720

Fox remains `hf_fox_bust_transparent.png` as a separate ImageView. Historical `hf_fox_planet.png` was not restored as a production composite.

## Character check (from ru-RU emulator captures)

| Criterion | Result |
| --- | --- |
| Dark filled spherical mass | YES — 01/02/05/06/07/09 |
| Restrained surface grain | YES — not a flat Lambert dome |
| Copper/orange rim upper-right | YES — one principal limb, not double-arc |
| Left/lower near-black | YES |
| Soft atmospheric limb | YES |
| Stars / orbits / radar | NO |
| Vector/wire bowl / V10 purple dome | NO — replaced |
| Fox in front, occludes planet | YES |
| Alpha leak through fur/ears/neck | Not observed on 01/05/07 |
| Matte rectangle around fox | NO |

## Per-screen crop

- **01** strongest cinematic sphere; fox centered; wordmark/loader below. MAD 16.68 vs board.
- **02** same family, quieter host; fox centered on the mass. MAD 31.12.
- **05–07** home band, full-bleed horizontally; vertical host still windows the sphere. Progress on 06 is the separate rail. 07 debug `Защищено` without `Нет разрешения VPN`.
- **08** live 05 behind the add-connection sheet (Always-on VPN, no Kill Switch).
- **09** lower-contrast support planet; form stays primary.
- **03/04/10–18** no planet.

## Remaining fidelity gap

Crops and fox scale still differ from the exact approved boards (MAD 16–34). Home artwork is a 208–236dp band, so the sphere is vertically cropped. Not pixel-locked. Not art-director PASS.
