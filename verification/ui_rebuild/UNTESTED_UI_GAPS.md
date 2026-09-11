# UNTESTED UI GAPS

## Owner-only blocker: full-resolution locked originals are absent

`design/hotfox_18_final_style_reference/` in this checkout contains only `MANIFEST.txt`.

Missing immutable originals (SHA-256 from the manifest):

| screen | original filename | sha256 |
|---|---|---|
| 01 | 01_splash_brand_entry.jpeg | 8b07bd2fdaba1618ce1ab9ccff07dda678e14df336db401328accb9a22bd200a |
| 02 | 02_onboarding_connect_hotfox.jpeg | 1bc006c1b48996e79d706118ab4e9aa35ca35a7999ccdd4cf157fcd4ca6145b4 |
| 03 | 03_onboarding_auto.jpeg | 466ba7c4ddcded73bf1c9d55e14604f40825e1103bef3fa54f7394ac664fcd82 |
| 04 | 04_onboarding_ready.jpeg | 2af79525cf77459cbcef71db80b22f59a02fd86e8ece9d433d760f6f1ee923fb |
| 05 | 05_main_not_protected.jpeg | 0ab581bb6d810a48b912eac3134f8df0438ee16995db6f1bb7620c8fa2ba0998 |
| 06 | 06_connecting.png | d8e25ac3bec19d24d1d25e2e93912bc592807530db48c72a2913b73ab4ff4997 |
| 07 | 07_protected.png | b685d00b6311d51e2b8ec9402058a9128450f8a4ce8ad68a62e3c3be77f8eb1e |
| 08 | 08_add_connection.png | 9e48bcb4dbb9001e91efdd0c425b8620e010406f2b6d75e65eac38f596cdcbab |
| 09 | 09_https_subscription.png | 41655c64d1a43ae6114ed416d60728058bbd6d40dcd8db33acde35af6baecedb |
| 10 | 10_servers.png | 6c2eb01c8a0644e8f44fb08b29c2e8631f40ea4a65ab8d39e0f29c2db0cd62ff |
| 11 | 11_server_details.png | 9e248acb335e9ec5b2682563f4b4566d137c3e5c3f2c1766cec5a8a21d0170e8 |
| 12 | 12_subscription.png | 4e33f704fbe3a02d4cc3115c2aaca561a9981d76a68a8c03a6d64f9b6bb3b509 |
| 13 | 13_settings.png | 7a25e843463f90aa16a9a51b070e877fb0ea1e153e8a1af8d96354f86282f6cf |
| 14 | 14_smart_routing.png | 180eb867456219206f492dbb7592d27411ff4b828cd5491ef6bc1a6b760e4b52 |
| 15 | 15_apps_and_rules.png | e1d195efdfab00161e10d832685ae088062f5be5fbe1ee2d31523c584a553e7f |
| 16 | 16_autopilot.png | 3c50bf3d57d2fff638c388ea4cad28f6f10b0b4be63011b21f03c3d77a83567c |
| 17 | 17_shadow.png | cce2c1b6f70daf8fbe0f1ab742e0db4fb736b920afaae866e51ccd914acd94f0 |
| 18 | 18_always_on_kill_switch.png | 799b3acec810f39c747a62fa31deb339068945a380b0a918b78a3dc96904feb8 |

Repository `*.preview.webp` copies are also absent.

Therefore:

- inner-phone golden crops cannot be extracted from locked originals
- exact pixel-diff / overlay / heatmap against SHA-256 originals is **NOT EXECUTED**
- this agent must **not** claim pixel-perfect completion or the master-prompt 18/18 visual gate wording

Implementation continued from the secondary textual contract (screen structure, tokens, negative prompts, functional bindings).

## Other gaps

- Local reconstruct / `:app:assemblePlaystoreDebug` / unit tests / lint / release compile: **NOT EXECUTED** in this cloud environment at implementation time (Android SDK/NDK/Java 17 bootstrap not fully installed here). Push CI is the reconstruct/build evidence path.
- Emulator screenshot capture of the 18 screens: **NOT EXECUTED**
- Runtime VPN E2E / physical device: **NOT EXECUTED / deferred** (unchanged; UI work must not claim RELEASE READY)
- Exact fox/artwork bitmaps from originals: cannot be extracted; splash uses the existing geometric HotFox mark; other screens do not invent a planet/globe motif and do not copy the fox onto screens where the original is unknown
