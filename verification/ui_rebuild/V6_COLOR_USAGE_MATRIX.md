# V6 color usage matrix

| Role | Hex / resource | Allowed screens |
|---|---|---|
| Background | `hf_asset_background` warm near-black | all |
| Cream text | `hf_asset_cream` | titles, row titles |
| Muted text | `hf_asset_muted` `#A39EAE` | body, captions, ping values |
| Brand orange | `hf_asset_orange` / `#FF9255` | wordmark, selected nav, AUTO, active stage |
| Primary fill orange | `hf_native_primary` gradient | onboarding primary, home connect only |
| Compact orange | `hf_native_primary_compact` | subpage primary actions |
| Quiet/progress | `hf_native_progress` dark + orange stroke | connecting stop, disconnect sibling |
| Success green | `hf_asset_green` / `hotfox_success_bright` | **only** protected headline and explicit healthy state |
| Ping values | muted cream | screen 10 debug rows — not auto-green |

Orange must not simultaneously dominate wordmark + giant CTA + nav + decoration on subpages. Home 05 may combine wordmark + connect CTA + active nav because that is the product home action.

Green is forbidden as a default network/latency paint.
