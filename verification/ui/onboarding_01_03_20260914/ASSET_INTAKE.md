# HotFox 01–03 asset intake — ONBOARDING-01-03-20260914

Source-of-truth PNGs are reused as-is. Bytes were not regenerated or renamed.

| Screen | Resource | Path | Density | Size | Mode | SHA-256 | Display |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 01, 02 | `hotfox_fox` | `bootstrap/hotfox_2_2_0/app/src/main/res/drawable-nodpi/hotfox_fox.png` | nodpi | 600×455, 255334 B | RGBA | `6c26d88a89520ba1a4706ec169c7a0facc0191060d59a4a8eb5d06f8cba4af27` | Single transparent fox in measured art slot via `HotFoxHeroArtwork` `hfHeroFitParent` |
| 01, 02 | `hf_native_planet_onboarding` | `drawable-nodpi/hf_native_planet_onboarding.png` | nodpi | 1080×1400, 483632 B | RGBA | `454ab14652c6c7f5c620e53103de3ae6b1b1d43789600dc66022ceaee91ba621` | Separate planet layer behind fox |
| 03 | `hf_auto_orbits` | `drawable-{mdpi,xhdpi,xxhdpi,xxxhdpi}/hf_auto_orbits.png` | density | xxhdpi 864×864 | RGBA | xxhdpi `6b5923f0bcd20e58d3c268d5088cd07304d040ab9b95745e9717e41b5cbe22b4` | Orbits/nodes; alpha ~0.42. Residual: filled orange disc in the PNG |
| 03 | `hf_globe_orange` | `drawable-{mdpi,xhdpi,xxhdpi,xxxhdpi}/hf_globe_orange.png` | density | xxhdpi 72×72 | RGBA | xxhdpi `b4d48e7ddc680aa22436b3b346756665bf598729f9575821af496db9600cf792` | Center globe overlay, 96dp |
| 01 | `hf_brand_wordmark` | existing vector/png | — | — | — | unchanged | Splash bottom + 02/03 top-left |
| 01 | `hf_loading_track` / `hf_loading_fill` | existing | — | — | — | unchanged | Decorative line, not a fake percent |
| 02/03 | `hf_arrow_right_ink` | existing | — | — | — | unchanged | Primary CTA trailing icon |

Inspected and **not used** for 01–03:

- `hotfox_fox_master.png` (1063×1186, white contour) — Home/master renderer, not this onboarding fox.
- `hotfox_hero_scene.png` (853×1844, RGB, baked V13 scene) — would be a second fox if stacked; Home only.
- `hf_fox_planet` — forbidden legacy composite.
- `hf_shadow_orbits` / `hf_ready_orbits` / `hf_native_auto_routing.xml`.

Missing qualifier: **hdpi** copies of `hf_auto_orbits` / `hf_globe_orange` were already absent; not generated.

Reference triptych (`design/hotfox_onboarding_01_03_20260914/reference_triptych.png`, 1448×1086, SHA `01c1cbc65e6e8dc3610a509e83184f67a60fa3dd8b1999600b74fb6c147bc846`) is **design-only**. It is never a runtime background.
