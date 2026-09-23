# HotFox typography — Onest

## Source

- Typeface: **Onest** by Dmitri Voloshin, Andrey Kudryavtsev (The Onest Project Authors).
- Upstream: https://github.com/simpals/onest — release **2.001**,
  `https://github.com/simpals/onest/releases/download/2.001/onest-2.001.zip`
  (zip SHA256 `17b59e19c349e603b7d113a596b6d8e08427e97a7b5235668b69e9d8a06a4267`).
- Google Fonts cross-check: `google/fonts` `ofl/onest` at commit
  `4c1db3aec83c67dd223dd82a68c039cab30917a9` ("Onest: Version 2.001 added"), METADATA.pb
  points at the same archive (upstream commit `8739b1910618a15335e4cc48842052d0ee739ade`).
  The variable `Onest[wght].ttf` in the release zip and in google/fonts are byte-identical
  (SHA256 `966c5c29b4755da84b6854d5c21dd4eaa2420225d0e9874de602de176d4a9f31`).
- The bundled files are the release's own static instances from `fonts/ttf/`; no outlines were
  generated or modified.

## License

SIL Open Font License 1.1. Upstream text (identical in release zip and google/fonts):
`bootstrap/hotfox_2_2_0/app/src/main/assets/licenses/ONEST_OFL.txt` (shipped in the APK at
`assets/licenses/ONEST_OFL.txt`).

## Bundled weights

Path: `bootstrap/hotfox_2_2_0/app/src/main/res/font/`

| Resource | Upstream file | Weight | SHA256 |
|---|---|---|---|
| `onest_regular.ttf` | `Onest-Regular.ttf` | 400 | `fb4adfa4cd56d1bd4b36df74f915b4cfd3482287c14a121fa384ce9bdf385b00` |
| `onest_medium.ttf` | `Onest-Medium.ttf` | 500 | `ed31ddb7ca730c68c78c5f2b11b8ecb614d3fe8eab68d17ef6f0a163bc6933ad` |
| `onest_semibold.ttf` | `Onest-SemiBold.ttf` | 600 | `a2b3339cc901cbd63597df0b9319898c8f1071c30fa87013f1dac480797ce6fb` |
| `onest_bold.ttf` | `Onest-Bold.ttf` | 700 | `f59dfd26f42eaa07bc19c92b3ef17173ab4bb8f21c500d2736a88088fd9a845e` |

`hotfox_onest.xml` is the font family (both `android:` and `app:` attributes, so AppCompat
loads it on API 24–25). No variable font, no downloadable fonts, no Play Services dependency.

## UI mapping

Theme default (`HotFoxEditorialTheme`): `@font/hotfox_onest` — normal text 400, `textStyle="bold"`
on secondary screens resolves to the real 700 file.

Semantic roles (`res/values/hf_typography.xml`) reference one static file each and force
`textStyle=normal`, so 500/600 are never synthesized:

| Role | Weight | Used for |
|---|---|---|
| `HotFox.Type.Display` | 700 | Home state title, onboarding/HTTPS titles (`HotFox.ScreenTitle*`) |
| `HotFox.Type.CardTitle` | 600 | server / Shadow / Smart / subscription titles, stats values, «Вставить», PRO |
| `HotFox.Type.Button` | 600 | Home CTA; `HotFox.PrimaryButton` / `SecondaryButton` |
| `HotFox.Type.BodyMedium` | 500 | splash tagline, nav (`HotFox.NavLabel`), `HotFox.TextButton` |
| `HotFox.Type.Chip` | 500 | status chip, AUTO badge |
| `HotFox.Type.Overline` | 500 | «СЕРВЕР» section label (letterSpacing 0.10) |
| `HotFox.Type.Body` / `Caption` | 400 | subtitles, captions, helper and security notes, URL input |

Material 3 `textAppearance*` theme attributes are mapped to Onest variants for library widgets.

## Brand exception

Graphical wordmarks (`hf_brand_wordmark`, `hotfox_brand_mark`) are unchanged. The Home header
«Hot»«Fox» text wordmark keeps its previous `sans-serif` bold on purpose.
