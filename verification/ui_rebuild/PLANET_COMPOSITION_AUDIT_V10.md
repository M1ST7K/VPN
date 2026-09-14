# V10 planet composition audit

Generator: `verification/ui_rebuild/generate_planet_v10.py`  
Assets: `hf_native_planet_backdrop.png`, `hf_native_planet_support.png`  
Fox: `hf_fox_bust_transparent.png` (unchanged)  
Legacy `hf_fox_planet` / `hotfox_art_fox_planet_*`: **not restored**

## Intent

Dark atmospheric **filled** planetary mass with **one** short directional copper crescent. Fox remains the hero. Not a uniform glowing dome/arc.

## Parameters

| Variant | Size | Center | Radius | Rim gain | Screens |
| --- | --- | --- | --- | --- | --- |
| backdrop | 1600×1100 | (800, 900) | 700 | 0.40 | 01, 02, 05, 06, 07 |
| support | 1600×1100 | (800, 980) | 640 | 0.24 | 09; 08 inherits live 05 |

Interior Lambert volume is lifted off `(13,12,18)` so the body reads as mass. Rim window is a tight upper-left/right crescent, not an even halo.

## Presence

| Screen | Planet |
| --- | --- |
| 01 Splash | backdrop |
| 02 Onboarding connect | backdrop |
| 03 AUTO | **none** |
| 04 Ready | **none** |
| 05 Home disconnected | backdrop |
| 06 Connecting | backdrop, **static** (not progress) |
| 07 Home protected (debug chrome) | backdrop |
| 08 Add sheet | inherits live 05 |
| 09 HTTPS | support, α 0.72 |
| 10–18 | **none** |

## QA stills

`verification/ui_rebuild/v10_assets/planet_on_black.png`  
`verification/ui_rebuild/v10_assets/planet_with_fox_01.png`  
`verification/ui_rebuild/v10_assets/planet_with_fox_05.png`

Emulator proof: `actual_v10/01.png`, `02.png`, `05.png`, `06.png`, `07.png`, `08.png`, `09.png`.
