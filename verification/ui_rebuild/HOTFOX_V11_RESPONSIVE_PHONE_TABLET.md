# HotFox V11 — phone / tablet responsive report

Date: 2026-09-12  
Breakpoints (resource qualifiers):

| Profile | Qualifier | Capture | wm |
| --- | --- | --- | --- |
| Compact phone | `values/` (sw<360) | `actual_v11_compact/` 720×1480 @360dpi | sw=320 |
| Regular phone | `values-sw360dp` | `actual_v11/` 1080×2400 @420dpi | sw=411 |
| Small tablet portrait | `values-sw600dp` | `tablet_v11/` 1200×1920 @320dpi | sw=600 |
| Large tablet portrait | `values-sw720dp` | `tablet_v11_large/` 1600×2560 @320dpi | sw=800 |

Independent dimens: fox size, artwork host height, gutters, CTA max width, content max width. Phone canvas is not uniformly stretched.

`HotfoxSystemUi.constrainReadingWidth` adds extra horizontal padding when `hf_content_max_width` is smaller than the view. Home planet sits in a **zero-gutter** `artwork_host` so the sphere can go edge-to-edge; title/CTA/rows keep `hf_page_gutter`.

## Phone

- Compact 01/02/05/07/09 captured. Fox remains dominant; planet readable; CTA/nav not colliding.
- Regular 18/18 captured. Splash fox 320dp; home fox 220×188 in 236dp host; HTTPS fox 208dp.

## Tablet

Required hero set captured: 01, 02, 05, 06, 07, 08, 09.  
Utility sample: 10, 14, 15, 17, 18.  
Large tablet sample: 01, 05, 07, 18.

Splash fox grows to 400/460dp so the hero is not a postage stamp. Home artwork host 340/420dp. Text column capped at 560/640dp.

## Remaining

- Tablet primary CTA still reads wide relative to the board.
- Home planet remains a vertical window (band), not a full-page sphere behind the whole home column.
- Emulator tablet captures include the system taskbar; that is device chrome, not app UI.
