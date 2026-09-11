# HOTFOX UI BUGS

Open visual/engineering items after the 18-screen reconstruction pass.

## Blockers (environment)

- P0 visual verification — emulator/device screenshots and pixel-diff against SHA-256 originals are **NOT EXECUTED**. Originals themselves are present. Do not treat goldens as an exact-pixel PASS.

## Non-blocking

- P2 — Some secondary pickers (routing mode, Autopilot pause/protection/trusted, ads toggle) still use themed Material dialogs under `HotFoxSheetDialog` rather than a fully custom locked sheet. Add-connection (08) and HTTPS (09) no longer use default blue/purple AlertDialog.
- P2 — Screen 13 keeps extra Прочее rows (Updates, ads) so those real destinations stay reachable.
- P2 — Screens 10/11 have no country-flag assets; emoji flags are not used.
- P2 — Screen 12 expiry/device values are live (or «Нет данных»), not the board’s example `12 марта 2026` / `2 из 5`.
- P2 — Screen 01 is not held with a fake splash delay.
- P2 — Screen 07 board labels the active tab «Соединение»; chrome follows screen 05 («Главная»).
