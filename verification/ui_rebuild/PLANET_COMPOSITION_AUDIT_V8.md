# V8 planet composition audit

Resource: `drawable-nodpi/hf_native_planet_backdrop.png` (separate layer).
Foreground: `hf_fox_bust_transparent`.
Legacy composites **not restored**: `hf_fox_planet` = 0, `hotfox_art_fox_planet_*` = 0.
V7 vector double-arc `drawable/hf_native_planet_backdrop.xml` was deleted.

Asset QA (not acceptance): `verification/ui_rebuild/v8_assets/`.

In-app proof from `actual_v8/` ru-RU emulator captures:

| Screen | Planet present | Filled mass | One rim | Fox occludes rim | Fade endpoints | Orbits/stars | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 01 Splash | yes | yes | yes | yes (upper limb) | yes | no | Wordmark still sits on the body; not yet the cinematic sit-on-horizon crop |
| 02 Onboard Connect | yes | yes | yes | yes | yes | no | Hidden on AUTO/Ready steps |
| 03 AUTO | **no** | — | — | — | — | no | Owner anchor |
| 04 Ready | **no** | — | — | — | — | no | Owner anchor |
| 05 Disconnected | yes | yes | yes | yes | yes | no | Not a wire bowl |
| 06 Connecting | yes, static | yes | yes | yes | yes | no | Rail/stages remain separate; planet is not progress |
| 07 Protected | yes | yes | yes | yes | yes | no | Debug chrome only |
| 08 Add sheet | inherited 05 | yes | yes | yes | yes | no | Live home under scrim |
| 09 HTTPS | yes | yes | yes | partial | yes | no | Compact supporting; fox lower than 05 |
| 10–18 | **no** | — | — | — | — | no | 17 = shield + corridors only |

Remaining: rim still reads more like a limb highlight than a full occupied horizon; fox is not fully seated on the terminator the way the approved boards paint it.
