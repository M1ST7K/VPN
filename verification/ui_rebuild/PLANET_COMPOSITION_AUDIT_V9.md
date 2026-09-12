# V9 planet composition audit

Family (separate layers, not `hf_fox_planet`):

- Home / splash / onboarding: `drawable-nodpi/hf_native_planet_backdrop.png`
- HTTPS supporting: `drawable-nodpi/hf_native_planet_support.png` (lower rim gain)
- Fox: `hf_fox_bust_transparent`

Generator: `verification/ui_rebuild/generate_planet_v9.py`  
Offline QA (not acceptance): `verification/ui_rebuild/v9_assets/`  
Legacy composites **not restored**: `hf_fox_planet` = 0, `hotfox_art_fox_planet_*` = 0.  
No V7 double-arc XML bowl.

In-app proof from `actual_v9/` ru-RU emulator captures:

| Screen | Resource | Fox bounds (layout) | Planet bounds | Rim | Body | Occlusion | Text/CTA | PASS/FAIL |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 01 Splash | backdrop | 280dp, top 72dp | match_parent crop | one copper, faded ends | dark filled, near bg | fox sits on upper limb | wordmark+tagline+track inside same FrameLayout | FAIL vs board crop; direction PASS |
| 02 Onboard Connect | backdrop | 220×220 center | remaining weight, bottom crop | one rim | filled dark | fox over horizon | title/body above; CTAs below | FAIL vs board; planet present |
| 03 AUTO | none | topology only | — | — | — | — | frozen concept | PASS (no planet required) |
| 04 Ready | none | server/check | — | — | — | — | frozen concept | PASS (no planet required) |
| 05 Disconnected | backdrop | 200×168 bottom+8dp | artwork_host 212dp | one rim | filled | fox on rim | permission+Connect below | FAIL vs board crop |
| 06 Connecting | backdrop static | same as 05 | same | one rim | filled | yes | rail/stages separate; not orbit | FAIL vs board; static PASS |
| 07 Protected | backdrop | same as 05 | same | one rim | filled | yes | debug chrome only | FAIL vs board |
| 08 Add sheet | inherited 05 | live 05 | live 05 | inherited | inherited | inherited | sheet dominant; dim 0.22 | FAIL vs board; inherit PASS |
| 09 HTTPS | support, α 0.55 | 168dp bottom | remaining weight | lower gain | filled, quieter | partial | form primary | FAIL vs board; supporting PASS |
| 10–18 | **none** | — | — | — | — | — | 17 = shield + routing paths | PASS (no planet required) |

No stars, satellites, orbit rings, radar, nebulae, or baked UI in the planet PNGs.

Remaining: rim still reads as a terminator highlight more than a fully occupied cinematic horizon; fox is not yet seated on the board’s exact crop.
