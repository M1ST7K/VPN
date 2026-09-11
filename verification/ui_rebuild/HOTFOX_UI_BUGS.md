# HOTFOX UI BUGS

Open visual/engineering items after the 18-screen reconstruction pass.

## Blockers (owner)

- P0 visual verification — full-resolution originals identified by `design/hotfox_18_final_style_reference/MANIFEST.txt` are not in the checkout. Pixel-diff cannot be computed. Do not treat preview-less geometry as an exact-pixel PASS.

## Non-blocking

- P2 — Screen 15 chrome (`HotfoxAppsRulesActivity`) opens the existing installed-app editor (`PerAppProxyActivity`) rather than inlining the full app recycler on the same locked layout. Production list remains real packages; `com.example.*` placeholders are not introduced.
- P2 — Some secondary pickers (routing mode, Autopilot pause/protection/trusted, ads toggle) still use themed Material dialogs under `HotFoxSheetDialog` rather than a fully custom locked sheet. Add-connection (08) and HTTPS (09) no longer use default blue/purple AlertDialog.
- P2 — Exact inner-phone metrics (title baseline, CTA radius, artwork crop) cannot be calibrated until originals are supplied.
