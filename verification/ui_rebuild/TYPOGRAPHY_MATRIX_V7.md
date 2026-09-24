# HotFox V7 typography matrix (from ru-RU emulator captures)

Locale: `ru-RU` via `cmd locale set-app-locales com.hotfox.vpn --locales ru-RU`.
Production strings were not hardcoded to Russian.

| Screen | Title | Body | CTA | Notes |
| --- | --- | --- | --- | --- |
| 01 Splash | Wordmark art 44dp | Tagline 11sp tracked | Progress track | Hero+brand grouped; no on-screen title token |
| 02 Connect | HotFox.ScreenTitle ~30sp | Body 16sp muted | Full primary 52dp + quiet text | Title wraps to 2 lines |
| 03 AUTO | Same title token | Body 16sp | Full primary + quiet secondary | Anchor; do not enlarge title |
| 04 Ready | Same title token | Body 16sp + 12sp note | Full primary | Anchor |
| 05 Home | Screen title cream | Status 14–16sp | Full primary 52dp | Permission line 12sp muted |
| 06 Connecting | Same | Stage 13sp | Neutral outlined Stop | Stages over CTA |
| 07 Protected | Green success title | Timer 14sp | Secondary Disconnect | Green only on success |
| 08 Sheet | 20sp sheet title | Row 16sp | N/A | Over live 05 |
| 09 HTTPS | Screen title | Body 16sp | Compact subpage primary | Form first |
| 10 Servers | Screen title | Row title 16sp / ping 14sp muted | Filters 14sp | AUTO first row |
| 11 Details | Screen title | Row 16sp / values muted | Disabled-looking select | Debug values only |
| 12 Subscription | Screen title | 14sp labels | Full manage + secondary renew | Debug labels |
| 13 Settings | Screen title | Eyebrow 12sp caps / rows 16sp | Chevrons | No giant cards |
| 14 Routing | Screen title | Body 16sp | Compact Apply | Sparse lower half |
| 15 Apps | Two-line title | Search 14sp | Full save | Segmented 14sp |
| 16 Autopilot | Screen title | Body 16sp | Compact Done | Sparse lower half |
| 17 Shadow | Screen title | Secondary body 13sp | Compact AUTO | No planet |
| 18 Always-on | Screen title | Rows 16sp / Unknown muted | Compact Android settings | Truthful status |

No single universal title size beyond the existing `HotFox.ScreenTitle` / `ScreenTitleSecondary` / sheet 20sp split.
