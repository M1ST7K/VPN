# HotFox asset collision report

Staging export: `build/hotfox_asset_export/res`  
Target overlay: `bootstrap/hotfox_2_2_0/app/src/main/res`  
Compared also against reconstructed `V2rayNG/app/src/main/res` after overlay rsync.

Export: 207 unique asset IDs, 822 PNG, 8 XML (fixture_apps excluded).  
All exported filenames use the `hf_` prefix.

## Filename collisions

| Filename | Old SHA | New SHA | Referenced | Action | Reason |
| --- | --- | --- | --- | --- | --- |
| — | — | — | — | none | **0 collisions.** No existing overlay or reconstructed `res` file shares an exported `hf_*` name. |

Silent overwrite: **not performed**. Resources were copied into empty `hf_` namespace only.

## Notes

- `fixture_apps` PNG were not exported (kit contract).
- Original `references/` boards were not copied into `res`.
- Existing XML drawables such as `hotfox_connect_action_bg.xml` / `hotfox_logo.xml` keep their names; production layouts will stop depending on screenshot-era composites and use `hf_*` instead.
