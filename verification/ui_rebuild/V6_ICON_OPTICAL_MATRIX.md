# V6 icon optical matrix

Touch target ≠ glyph. Header actions are 48dp; visible glyph is ~20–22dp (`padding=13dp`).

| Icon | Nominal | Optical note |
|---|---|---|
| Back | 48dp hit / ~22dp glyph | orange arrow, same padding as gear |
| Gear / settings | 48dp hit / ~22dp glyph | cream, header and rows 20dp |
| Nav home/servers/crown/settings | 20dp | was 18dp; labels 12sp |
| Server stack / crown / shield / route | 20–24dp in rows | row icons 20–24dp, chevrons 16dp |
| Fox bust | fitCenter in measured box | transparent PNG, no matte |
| Shadow shield | 112×104dp | topology feeds it, does not orbit it |
| Debug app glyphs | 40dp | `src/debug` only |
| Connecting rail nodes | 2–3dp | route, not satellites around fox |

Same nominal dp is not the goal; nav glyphs were optically small next to 12sp labels, so they were bumped together.
