# HotFox V5 — geometry matrix

Base rhythm: 4/8dp. Content inset 20dp unless noted. Header 56dp. Bottom nav minHeight 56dp with 6/10 padding. Screen density target 1080×2400 @ 420dpi.

| ID | L/R inset | Header top | Wordmark | Title top | Body gap | Artwork box | CTA | Group | Nav | Bottom safe |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 01 | 0 (full bleed fox) | — | bottom stack, 160×44dp | — | tagline 12dp | fox 280dp tall, marginTop 72dp | loading 220×4dp, marginTop 28dp | — | none | paddingBottom 64dp |
| 02 | 20dp | 56dp header | 88×24dp | 8dp under header | 10dp | 220dp host over CTA | Primary 56dp full | spacer weight=1 above art | none | 24dp |
| 03 | 20dp | 56dp | 88×24dp | 8dp | 10dp | 220dp routing vector | Primary 56dp | same spacer | none | 24dp |
| 04 | 20dp | 56dp | 88×24dp | 8dp | 10dp | 220dp ready complete | Primary 56dp + 12dp note | same | none | 24dp |
| 05 | 20dp | 56dp + gear 48 hit / 14 pad ≈20dp glyph | 88×24dp | HomeHeadline in status_block padTop 4dp | 4dp | artwork_host 140dp, fox 168×140 | connect 48dp compact | row group light separators | 56dp+ | note above nav, no clip |
| 06 | 20dp | same | same | same | stages 8dp | ring 240dp when shown | ProgressPrimary 48dp | rows muted | 56dp+ | stages are hero |
| 07 | 20dp | same | same | same | metrics tighter | fox 168×140 | disconnect compact 48dp | light rows | 56dp+ home active | success hierarchy |
| 08 | sheet 20dp | handle 48×4 | — | 20sp title in 48dp row | — | none (live home behind) | rows 52dp | native sheet wrap_content | home nav dimmed | compact, dim 0.38 |
| 09 | 20dp | 56dp + back 48/14 | 88×24dp | 0–8dp | 10dp | fox 120dp bottom, not weight=1 | Primary 56dp | form stack | none | fox no longer full-weight |
| 10 | 20dp | 56dp | 88×24dp | 8dp | filters 14sp | none | none | server rows + AUTO | 56dp+ servers active | list fills; fixture 6 rows |
| 11 | 20dp | 56dp + back | 88×24dp | 8dp | 8–12dp | none | disabled Primary 56dp | key/value rows | none | values end-aligned |
| 12 | 20dp | 56dp | 88×24dp | 8dp | 8dp | none | disabled buy readable | details stack | 56dp+ crown | fixture fills lower zone |
| 13 | 20dp | 56dp + back | 88×24dp | title 20dp pad | body 6/10 | none | none | rows 48dp, icons 20dp, hairlines | none | privacy above bottom, no scrollbar |
| 14 | 20dp | 56dp + back | 88×24dp | 8dp | 8dp | none | PrimaryCompact 48dp | rows 52dp | none | compact CTA, less dead zone |
| 15 | 20dp | 56dp + back | 88×24dp | 8dp | 8dp | none | Primary 56dp save | app rows fixture | none | 8 populated rows |
| 16 | 20dp | 56dp + back | 88×24dp | 8dp | 8dp | none | PrimaryCompact 48dp | rows 52dp | none | quieter than onboarding |
| 17 | 20dp | 56dp + back | 88×24dp | 8dp | body secondary | topology 360×180 + shield 112×104 | PrimaryCompact 48dp | rows 52dp | none | paths outer band only |
| 18 | 20dp | 56dp + back | 88×24dp | 8dp | 8dp | shield modest | PrimaryCompact 48dp | rows 52dp | none | status values readable |

## Negative space rule

Lower-half emptiness is only acceptable when the approved board also uses it (splash bottom brand lockup). 03/04 use a weight=1 spacer *above* art so title/body sit with the header and art/CTA form the lower story. 10/12/15 use fixtures so the list is the visual endpoint. 14–18 keep compact CTAs near content instead of a giant orange footer.

## OWNER_OVERRIDE geometry

Pixel equality to boards that still show planet/orbit art on 01/02/03/04/05/07 is not required. Measure chrome, type, and remaining layout against those boards outside the overridden celestial region.
