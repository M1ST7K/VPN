# Emulator capture status

Emulator **did boot** after granting `/dev/kvm` world-writable access (`kvm` group was missing).

AVD: `hotfox34`, API 34 google_apis x86_64, 1080×2400, night mode.

Hard limits that remain:

- Splash has no stable frame (immediate continue).
- Connecting/Protected were not forced (no fake CONNECTED).
- Non-exported activities cannot be launched from adb on API 34.
- Pixel compare vs phone-chassis reference boards is not PASS.

Partial real screenshots live in `verification/ui_rebuild/actual/`.
