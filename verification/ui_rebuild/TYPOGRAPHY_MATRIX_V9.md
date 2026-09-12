# HotFox V9 typography matrix (ru-RU emulator captures)

Locale: `cmd locale set-app-locales com.hotfox.vpn --locales ru-RU`.
Production strings are not hardcoded to Russian.

| Screen | Brand | Title | Weight / break | Body | Row / value | CTA | Nav | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 01 | Wordmark 132×36 | none | grouped with hero | tagline 10sp tracked 0.18 | — | 2dp track | — | Cream/orange, not clinical white |
| 02 | header wordmark | ScreenTitle ~30sp 2-line | medium | 16sp muted | — | full 52dp + text | — | Planet behind fox |
| 03 | header | ScreenTitle 2-line | frozen | 16sp | — | full + quiet text | — | Anchor; no enlarge |
| 04 | header | ScreenTitle | frozen | 16sp + 12sp note | — | full primary | — | Anchor |
| 05 | header | HomeHeadline 32sp | — | 14–16sp status | 16sp / 12sp muted | full 52dp | 11sp | Permission 11sp |
| 06 | header | same | — | stage 13sp | faded options | outlined Stop | 11sp | Neutral Stop |
| 07 | header | green success | success only | timer 14sp | 16sp | secondary Disconnect | Соединение | No VPN-permission contradiction |
| 08 | live 05 | sheet ScreenTitleSecondary 20sp | — | 16sp rows | — | — | — | Over live home |
| 09 | header back | ScreenTitleSecondary 26sp | — | 16sp | input 14sp | compact Add | — | Form first |
| 10 | header | ScreenTitleSecondary 26sp | — | 16sp / ping 14sp muted | filters 14sp | — | 11sp | Ping not auto-green |
| 11 | header | ScreenTitleSecondary | — | 16sp / 20sp identity | 12sp values | compact select | 11sp | Debug 18ms/12% |
| 12 | header | ScreenTitleSecondary | — | 14sp | hairline rows | compact + secondary | 11sp | Still open vs board |
| 13 | header | ScreenTitleSecondary | — | eyebrow 12sp / 16sp | chevrons | — | 11sp | Not giant cards |
| 14 | header | ScreenTitleSecondary | — | 16sp intro | eyebrows + 13sp values | compact Apply | — | Localized title |
| 15 | header | ScreenTitleSecondary two-line | exact break | 13sp body | 14sp name / 11sp pkg | compact Save | — | Segment 12sp / 36dp |
| 16 | header | ScreenTitleSecondary | — | 16sp | title+caption / 13sp | compact Done | — | Captive copy localized |
| 17 | header | ScreenTitleSecondary | — | 13sp secondary | title+caption | compact AUTO | — | No planet |
| 18 | header | ScreenTitleSecondary | — | 13sp legal | 16sp / Unknown | compact Android | 11sp | Unknown not faked |

03/04 remain the restraint benchmark. Utility CTAs use `HotFox.SubpagePrimary` (wrap, 44dp) rather than one giant full-width pill.
