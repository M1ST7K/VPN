# V6 geometry matrix

Boards: `verification/ui_rebuild/reference_v4/*.png` at 1080×2400. Capture device: 1080×2400 API 34.

Density used for dp conversion: 1080/360 = 3.0 (xxhdpi).

| Token | Board px (approx) | Implementation |
|---|---|---|
| Left/right inset | 48–60 px | 20dp padding on most screens |
| Header height | 56–64 px content | `include_hf_header` 56dp |
| Wordmark | ~88×24 dp visual | `img_wordmark` 88×24dp |
| Back/settings hit | 48dp | 48dp ImageButton, glyph ~20–22dp via 13dp padding |
| Home fox box | large mid hero | `artwork_host` 176dp, bust 220×176dp |
| Connecting rail | under fox, not around | 28dp `img_connecting_rail` |
| Connecting stages | 4 rows under rail | `layout_connecting_stages` |
| Home CTA | full width, ~52dp tall, pill | `connect_action` 52dp `hf_native_primary` |
| Subpage CTA | compact, often not full-bleed visually | wrap_content min 200–220dp, 44dp |
| Row height | 48–56 dp | 48–52dp rows |
| Bottom nav | ~56–64 dp + home indicator | minHeight 56dp, 20dp icons |
| Apps list | no vertical scrollbar | `scrollbars=none`, 5 debug rows |
| Shadow art | 180dp band, shield center | topology + 112×104dp shield |

Do not apply one global `margin=20dp` as the only composition rule: onboarding art uses remaining `layout_weight` so the fox fills the editorial middle; home keeps a measured fox box plus rail/stages.
