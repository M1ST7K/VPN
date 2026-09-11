# HotFox V5 — typography matrix

Measured from code after the V5 shared-style pass. Values are native `sp`/`dp` tokens, not screenshot pixels. Warm cream (`hf_asset_cream`) for titles; muted (`hf_asset_muted`) for body/captions; orange reserved for brand/CTA/active.

| ID | Title style | Size | Weight | Line extra | Max width | Intended break | Body | Body LH | Eyebrow | Row title | Row caption | Runtime value | Nav label |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 01 | wordmark asset, not ScreenTitle | 16sp wordmark / 11sp tagline | medium | — | 160dp wordmark | none | — | — | 11sp letterSpacing 0.12 | — | — | — | none |
| 02 | HotFox.ScreenTitle | 24sp | medium | +3dp | match_parent − 40dp | 2 lines if needed | 15sp Body | 1.15× +4dp | — | — | — | — | none |
| 03 | HotFox.ScreenTitle | 24sp | medium | +3dp | same | exact AUTO wrap | 15sp | same | — | — | — | — | none |
| 04 | HotFox.ScreenTitle | 24sp | medium | +3dp | same | 2 lines | 15sp | same | 12sp note | — | — | — | none |
| 05 | HotFox.HomeHeadline | 26sp | medium | +2dp / −0.01 tracking | match_parent | 1 line «Не защищено» | 15sp Body | same | — | 16sp RowTitle | 13sp | 13sp RuntimeValue | 11sp |
| 06 | HotFox.HomeHeadline | 26sp | medium | +2dp | match_parent | 1 line connecting | 13sp stages | tighter | — | 16sp | 13sp | 13sp | 11sp |
| 07 | HotFox.HomeHeadline | 26sp green success | medium | +2dp | match_parent | 1 line «Защищено» | 13sp metrics | tighter | — | 16sp | 13sp | 13sp | 11sp |
| 08 | ScreenTitle override | 20sp | medium | +3dp | sheet − 40dp | 1 line | — | — | — | 16sp | — | — | none |
| 09 | HotFox.ScreenTitle | 24sp | medium | +3dp | −40dp | 2 lines | 15sp | same | — | — | 12sp legal | — | none |
| 10 | HotFox.ScreenTitle | 24sp | medium | +3dp | servers pane | AUTO 2 lines | 14sp filters | — | 11sp SectionEyebrow | 16sp | 13sp ping | 13sp | 11sp |
| 11 | HotFox.ScreenTitle | 24sp | medium | +3dp | −40dp | 1–2 lines | 15sp | same | — | 16sp | 13sp | 13sp end-aligned | none |
| 12 | HotFox.ScreenTitle | 24sp | medium | +3dp | subscription pane | 1 line | 15sp | same | 11sp | 16sp | 13sp | 13sp | 11sp |
| 13 | HotFox.ScreenTitle | 24sp | medium | +3dp | −40dp | 1 line | 15sp settings body | same | 11sp all-caps | 16sp | 13sp | — | none (subpage) |
| 14 | HotFox.ScreenTitle | 24sp | medium | +3dp | −40dp | 1–2 lines | 15sp | same | — | 16sp | 13sp | 13sp | none |
| 15 | HotFox.ScreenTitle | 24sp | medium | +3dp | −40dp | 1 line | 15sp | same | 11sp | 16sp app name | 13sp pkg | — | none |
| 16 | HotFox.ScreenTitle | 24sp | medium | +3dp | −40dp | 1–2 lines | 15sp | same | — | 16sp | 13sp | 13sp | none |
| 17 | HotFox.ScreenTitle | 24sp | medium | +3dp | −40dp | 2 lines | 13sp BodySecondary | +3dp | — | 16sp | 13sp | — | none |
| 18 | HotFox.ScreenTitle | 24sp | medium | +3dp | −40dp | 1 line | 15sp | same | — | 16sp | 13sp | 13sp | none |

## Shared families

- `HotFox.HomeHeadline` — home connection story only (05/06/07).
- `HotFox.ScreenTitle` — 24sp, not the old 28sp display token.
- `HotFox.ScreenTitleSecondary` — 22sp reserved, unused on V5 screens.
- `HotFox.SectionEyebrow` — 11sp medium, 0.08 tracking, all caps.
- `HotFox.NavLabel` — 11sp medium.

## CTA type (not one pill)

- `HotFox.PrimaryButton` — 16sp / 56dp onboarding & import (02/09/11).
- `HotFox.PrimaryCompact` — 15sp / 48dp subpages 14–18 and quieter home.
- `HotFox.ProgressPrimary` — 48dp outline orange text for connecting.
- `HotFox.TextButton` — secondary onboarding / sheet-adjacent.
- Disabled primary remains readable ink-on-muted, not invisible.

Filters on 10 are 14sp (not 11sp) so they stay legible.
