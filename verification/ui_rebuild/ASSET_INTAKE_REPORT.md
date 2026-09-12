# HotFox ASSET INTAKE GATE

Date: 2026-09-11  
Branch: `cursor/hotfox-ui-pixel-lock-rebuild`  
HEAD: `8d6aeb6f4f09d8498c4d1c27083e054f0ddb4d68`  
Unpack: `python3 design/UNPACK_HOTFOX_ASSET_PARTS.py` → OK, 1101 files  
Kit root: `design/HOTFOX_APP_ASSET_KIT_EXTRACTED/HOTFOX_APP_ASSET_KIT/`  
Validator: `verification/ui_rebuild/run_asset_intake_gate.py`

## Status

**PASS**

No SHA mismatch, no corrupt PNG/SVG, no missing required files, no duplicate asset IDs, no production full-screen screenshot bitmaps.

## ZIP parts present in `design/`

| Part | Present |
| --- | --- |
| `HOTFOX_ASSETS_PART_A_CORE.zip` | YES |
| `HOTFOX_ASSETS_PART_B_REFERENCES_01_09.zip` | YES |
| `HOTFOX_ASSETS_PART_C_REFERENCES_10_14.zip` | YES |
| `HOTFOX_ASSETS_PART_D_REFERENCES_15_18.zip` | YES |

## Top-level structure

Expected parts all present: `README_START_HERE_RU.md`, `CURSOR_TASK_RU.md`, `png/`, `sources/svg/`, `manifest/`, `android/`, `scripts/`, `preview/`, `references/`, `docs/`. Extra kit folder `qa/` is present and harmless.

Required files all present:

- `manifest/assets.json`
- `manifest/screens.json`
- `manifest/components.json`
- `manifest/tokens.json`
- `manifest/SHA256SUMS.txt`
- `android/README_RU.md`
- `README_START_HERE_RU.md`
- `references/MANIFEST.txt`

## Counts

| Metric | Expected | Actual | Result |
| --- | ---: | ---: | --- |
| Files total | >1100 | 1101 | PASS |
| Unique asset IDs (`manifest/assets.json`) | 212 | 212 | PASS |
| Duplicate asset IDs | 0 | 0 | PASS |
| PNG in `png/` tree | 842 | 842 | PASS |
| PNG variants in manifest | 842 | 842 (822 production + 20 `fixture_apps`) | PASS |
| SVG | kit source geometry | 210 | PASS |
| XML | Android export helpers | 8 | PASS |
| JSON | manifests | 6 | PASS |
| JPEG (original boards) | references | 5 | expected |
| Screens in `screens.json` | 18 | 18 (`01`–`18`) | PASS |

PNG by density inside `png/` (excluding 2 nodpi artwork originals):

| Density | Count |
| --- | ---: |
| mdpi | 210 |
| xhdpi | 210 |
| xxhdpi | 210 |
| xxxhdpi | 210 |
| artwork originals (`fox_planet.png`, `fox_bust.png`) | 2 |

PNG by category:

| Category | Count |
| --- | ---: |
| icons | 608 |
| controls | 100 |
| surfaces | 40 |
| illustrations | 36 |
| flags | 24 |
| fixture_apps | 20 |
| navigation | 8 |
| brand | 4 |
| artwork | 2 |
| references (original boards, not production) | 13 |

Note: kit README “842 production-oriented PNG variants” counts every file under `png/`, including 20 `fixture_apps` variants that Android export **excludes**. Unique IDs remain 212. This is classification, not a missing-file defect.

`SHA256SUMS.txt` lists 1100 paths. The remaining kit file is `manifest/SHA256SUMS.txt` itself (self-hash not listed). All hashed PNG/SVG are listed.

## SHA-256

- Listed hashes: 1100
- Missing hashed files: 0
- Mismatches vs `SHA256SUMS.txt`: 0
- Mismatches vs `manifest/assets.json` variant `sha256`: 0
- Unlisted production PNG/SVG: 0

**SHA status: PASS**

## PNG validation

- Unreadable / corrupt: 0
- width/height ≤ 0: 0
- Density vs `logical_size_dp`: 0 mismatches
- Manifest `alpha=true` without alpha channel: 0
- Extra PNG not in `assets.json`: 13 original reference boards under `references/` (comparison-only, not production)

Heuristic “full-screen” sizes (>300×500 px) found only in high-density **surface** assets:

- `hf_surface_group` xhdpi/xxhdpi/xxxhdpi (card surface, 336×288 dp)
- `hf_surface_sheet` xhdpi/xxhdpi/xxxhdpi (bottom-sheet surface, 360×440 dp)

Visual inspection of mdpi surfaces: rounded dark cards with alpha corners, **not** phone screenshots, **not** baked UI text. Artwork foxes are opaque decorative illustrations as documented (`alpha=false`).

**PNG status: PASS**  
Suspicious production screenshot-as-UI: **none**

## SVG validation

- Count: 210
- Invalid XML: 0
- Missing `viewBox`: 0
- Embedded raster (`data:image/png|jpeg|…`): 0
- External `http(s)` dependency (excluding xmlns): 0
- `<script>` / `javascript:`: 0

**SVG status: PASS**

## Duplicates / corrupt

- Duplicate SHA among manifest PNG variants: 0
- Corrupt files: 0

## Gate decision

ASSET INTAKE GATE = **PASS**

Allowed next steps: Android export to `build/hotfox_asset_export/`, collision audit, legacy-art inventory, then native 01→18 rebuild. Original `references/` boards must not be copied into `app/src/main/res`. `fixture_apps` stay out of production export.
