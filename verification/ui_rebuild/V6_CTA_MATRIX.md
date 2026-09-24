# V6 CTA matrix

Stop using one giant full-width orange pill on every screen.

| Variant | Style / drawable | Height | Width | Screens |
|---|---|---|---|---|
| OnboardingPrimary | `HotFox.PrimaryButton` + `hf_native_primary` | 52dp | match_parent | 02, 03, 04 |
| HomeConnectPrimary | `hf_native_primary` on `connect_action` | 52dp | match_parent | 05 |
| HomeDisconnectQuiet | `hf_native_secondary` | 52dp | match_parent | 07 |
| ProgressAction | `hf_native_progress` + stop icon | 52dp | match_parent | 06 |
| SubpagePrimaryCompact | `HotFox.SubpagePrimary` / compact drawable | 44–48dp | wrap_content min ~200dp or match_parent compact | 09, 11, 14, 16, 17, 18 |
| AppsSaveCompact | compact orange, still full width (board save bar) | 48dp | match_parent | 15 |
| TextAction | `HotFox.TextButton` | 48dp | match_parent | 02 buy access |
| DisabledAction | compact/primary disabled stroke | 44–52dp | as parent | enabled=false states |

Hit target remains ≥48dp via `minHeight`/`minWidth` on the control or wrapper. Visible compact controls can be 44dp with 48dp minimum from the style parent where needed.

Connecting uses progress/outline, not a second orange fill competing with the brand.
