# V6 typography matrix

Viewport for measurement: approved `reference_v4` boards normalized to **1080×2400**. Capture baseline font scale 1.0.

Tokens are production styles in `values/themes.xml`. Shared tokens are used only where boards share a role.

| Role | Style | Size | Weight / tracking | Screens |
|---|---|---|---|---|
| BrandWordmark | `HotFox.BrandWordmark` + `hf_brand_wordmark` | 16sp / 88×24dp glyph | medium, +0.02 | 01–18 headers |
| DisplayHero | `HotFox.HomeHeadline` | 32sp | medium, −0.015 | 05, 06, 07 |
| RootTitle / onboarding | `HotFox.ScreenTitle` | 30sp | medium, −0.015 | 02, 03, 04, 09, 10, 13–18 |
| SubpageTitle | `HotFox.ScreenTitleSecondary` | 26sp | medium | dense subpages if needed |
| BodyLead | `HotFox.Body` | 16sp | regular, 1.18 line | 02–04, 09, 13, 14, 17 |
| BodySupport | `HotFox.BodySecondary` | 13sp | regular | captions under rows |
| SectionEyebrow | `HotFox.SectionEyebrow` | 12sp | medium, +0.12, caps | 10, 13 |
| RowTitle | `HotFox.RowTitle` | 16sp | medium | lists |
| RowCaption | `HotFox.RowCaption` | 13sp | regular | lists |
| RuntimeValue | `HotFox.RuntimeValue` | 13sp | medium, end aligned | 05, 07, 10, 11 |
| ButtonPrimary | `HotFox.PrimaryButton` | 16sp | medium | 02–05 |
| ButtonSecondary / compact | `HotFox.PrimaryCompact` / `SubpagePrimary` | 14–15sp | medium | 09, 11, 14–18 |
| NavLabel | `HotFox.NavLabel` | 12sp | medium | 05–08, 10, 12, 13 |
| LegalCaption | `HotFox.LegalCaption` | 12sp | regular | notes |

## Per-screen title lock (from boards, content px ≈ excluding phone chrome)

| Screen | Title copy (ru-RU) | Line break |
|---|---|---|
| 01 | HotFox wordmark + tagline | single brand block |
| 02 | Подключите / HotFox | two lines |
| 03 | AUTO routing title from `hotfox_onboarding_auto_title_ui` | as resource |
| 04 | Ready title from `hotfox_onboarding_ready_title_ui` | as resource |
| 05 | Не защищено | one line |
| 06 | Подключаем... | one line |
| 07 | Защищено | one line, green |
| 09 | HTTPS title from resources | one/two as resource |
| 10 | Серверы | one line |
| 13 | Настройки | one line |
| 15 | Приложения и правила | two lines in resource |
| 17 | Shadow | English feature name kept |

Muted text uses cream-warm `#A39EAE`, not clinical gray. Primary labels must not ellipsize.
