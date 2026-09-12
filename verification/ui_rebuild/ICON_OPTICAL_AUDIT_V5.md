# HotFox V5 — icon optical audit

Same nominal dp does not mean the same perceived size. V5 uses padding inside 48dp hit targets so glyphs read ~18–22dp.

| Glyph | Where | Nominal dp | Hit target | Optical correction | Stroke / weight | Alignment |
| --- | --- | --- | --- | --- | --- | --- |
| Back chevron | subpage header | drawable in 48dp ImageButton, padding 14dp | 48dp | ~20dp visible | orange cream stroke | optical center, not raw 48 |
| Gear / settings | home header | padding 14dp on 48dp | 48dp | ~20dp | cream, lighter than V3 24-in-24 | end of header |
| Search | header (gone unless needed) | padding 12dp | 48dp | ~24dp | cream | hidden on most screens |
| Wordmark | header | 88×24dp | — | fitStart | brand orange | baseline with header |
| Home nav | bottom | 18dp | item fill | reduced from 24 | muted / orange active | optical center over 11sp label |
| Servers nav | bottom | 18dp | item fill | same family | same | same |
| Crown nav | bottom | 18dp | item fill | crown used to look larger; 18dp matches home | same | same |
| Settings nav | bottom | 18dp | item fill | same | same | same |
| Lock | HTTPS / privacy rows | 20dp settings, 18dp sheet | row height 48–52 | −4dp vs old 24 | cream | leading 12–16dp inset |
| Shield | 17/18 | 112×104 (17 art), row 20dp | — | thinner stroke in topology | cream/orange | 17: centered, routes around |
| Bolt / lightning | settings autopilot | 20dp | 48 row | −4dp | cream | leading |
| Network / hub | AUTO art | hub r=18vp in 320×200 vector | — | not a globe-planet | orange ring + node | asymmetric candidates |
| DNS / IPv6 | routing / always-on | 20dp | 52 row | optical match lock | cream | leading |
| Info | notes | 20dp | — | muted | muted | leading 8dp text |
| Chevron | rows / sheet | 16dp | — | quieter than 20 | muted | trailing |
| Arrow on CTA | PrimaryButton | hf_arrow_right_ink | 56/48 button | smaller than V3 | ink on orange | gravity center |
| Close | sheet 08 | padding 12dp on 48 | 48dp | less aggressive | cream | trailing |
| Flags | servers 10 | flag drawables | row | unchanged meaning | — | leading |
| Link | sheet HTTPS | 18dp | 52 row | compact sheet | cream | leading |

## Rules

- Do not change semantic meaning (crown remains subscription, shield remains protection).
- Do not mix icon libraries.
- Inactive nav must stay readable on `#0D0C12`; active is precise orange, not a filled pill.
- System bar icons remain light on all dark HotFox screens (`windowLightStatusBar=false`).
