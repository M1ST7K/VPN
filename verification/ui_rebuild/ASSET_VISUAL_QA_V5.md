# HotFox V5 — asset visual QA

## Fox (`hf_fox_bust_transparent`)

Source: opaque `hf_fox_bust.png` (1254×1254 RGB, matte ~`[11,11,16]`).
Extractor: `verification/ui_rebuild/extract_fox_transparent_v5.py`.

Algorithm (must not punch holes in dark fur):

1. Median border color as matte seed.
2. Obvious foreground = color distance from seed **or** warm/orange (`R > G+8`, `R > B+8`, `R > 35`).
3. Morphological close radius 10, fill holes not connected to the edge, close radius 2.
4. Interior alpha 255; 1px contour ~180; 2px outer ~80.

### Alpha stats (from committed transparent PNG)

```
pct_alpha_0=78.6079
pct_alpha_255=20.4369
pct_partial=0.9552
pct_dark_fg_alpha_lt_220=0.9225
dark_fg_pixels=253012
dark_translucent_pixels=14507
```

Reject threshold for dark-fur translucency is 1.5%. Current 0.92% **PASS** that gate.

Diagnostics:

- `verification/ui_rebuild/v5_assets/fox_checkerboard.png` — no rectangular matte.
- `verification/ui_rebuild/v5_assets/fox_hotfox_bg.png` — fox on `#0D0C12`.
- `verification/ui_rebuild/v5_assets/fox_alpha_diagnostic.png` — green=opaque, orange=feather, dark=transparent.

Known leftover from flattened source (not a matte): gap between ear tuft and head, small notch at neck. Identity is the same fox, not a redrawn character.

Production layout refs to opaque `@drawable/hf_fox_bust"`: **0**. Screens 01/02/05/07/09 use `hf_fox_bust_transparent`.

## AUTO / Ready — no orbital language

| Screen | Asset | Forbidden leftover | Read |
| --- | --- | --- | --- |
| 03 | `hf_native_auto_routing.xml` | `hf_auto_orbits`, globe-as-planet | hub + asymmetric candidates, one selected orange path |
| 04 | `hf_native_ready_complete.xml` | `hf_ready_orbits`, radar rings | stacked servers + check disc |

Static check needles: `hf_fox_planet`, `hf_shadow_orbits`, `hf_auto_orbits`, `hf_ready_orbits`, opaque `hf_fox_bust`.

## Shadow topology

`hf_native_network_topology.xml`: curves only in the outer band. Shield 112×104dp. No closed orbit ellipse. No line through the shield center.

## Density / scaling

- Fox: `drawable-nodpi` so it is not density-bucket stretched.
- Vectors: AUTO 320×200, Ready 220×160, topology 360×180 — `fitCenter` in hosts.
- No screenshot-derived full-screen bitmap cheating.
- No `hotfox_art_fox_planet_*` production refs.

## Remaining asset risk

If emulator captures later show a rectangle around the fox, it is a layout background (`ImageView` parent) not the PNG matte — inspect `artwork_host` / onboarding FrameLayout, do not re-run the broken dark-fur flood fill.
