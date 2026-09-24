# HotFox bootstrap

This directory makes the repository reproducible for Cursor Cloud Agents and GitHub Actions without committing large third-party binaries.

## What it reconstructs

`bootstrap_source.sh` performs a fail-closed reconstruction of the HotFox Proxy 2.1.0 baseline:

- validates the embedded payload against SHA-256 `c9dd4656985b73084e3ff9bf16459c93d55e0a2cfb0ce8dd083ac1d1dea85260`;
- restores the 7712-line Ultra Master Prompt and the final HotFox UI reference;
- checks out exact upstream v2rayNG tag `2.2.6` / commit `15b4fff8e45da9bc0acaa5cc1d80a1d3531e8712`;
- copies the Android project from the upstream `V2rayNG/` subdirectory;
- applies the verified HotFox 2.1.0 source overlay;
- installs the vector `hotfox_logo` branding drawable omitted from the text overlay;
- builds HEV/tun2socks for `arm64-v8a`, `armeabi-v7a`, `x86`, and `x86_64` with Android NDK `29.0.14206865`;
- downloads and verifies `libv2ray.aar` v26.6.27 using SHA-256 `7846eb7f663d1d8ae931034faa7a56cccc82d618c2d029198e6e91a77fd8de1e`;
- runs structural, native-library, UI-source, and secret checks.

Run from repository root:

```bash
bash bootstrap/verify_payload.sh
bash bootstrap/bootstrap_source.sh
bash bootstrap/verify_reconstruction.sh
```

The bootstrap proves source reconstruction and build prerequisites. It does **not** prove physical-device VPN E2E. External-IP change, DNS/IPv6 leak behavior, transport correctness, reconnect behavior, and Wi-Fi/mobile handoff remain device-level release gates.
