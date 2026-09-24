# Untested gaps

- 04 Paste action: NOT EXECUTED (no clipboard write via ADB on API 34).
- 04 invalid input gives no visible error message; possible transient Toast was not captured. Import logic unchanged by this pass.
- fontScale 1.30 and 393/412/430dp widths: NOT EXECUTED.
- Tablet viewports: NOT EXECUTED in this pass.
- Home Connecting/Connected are debug presentation states, not real VPN sessions.
- Real VPN E2E (TUN → HEV → Xray → server) and physical device: NOT EXECUTED.
- GitHub Actions: must be read from the PR; a skipped run is not PASS.
- RELEASE READY: NO.
