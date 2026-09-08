# HotFox Proxy 2.2.0 — verification report

Baseline: reconstructed HotFox 2.1.0 overlay on v2rayNG `2.2.6` (`15b4fff8e45da9bc0acaa5cc1d80a1d3531e8712`), then committed `bootstrap/hotfox_2_2_0` overlay.

Package: `com.hotfox.vpn`  
minSdk: 24  
versionName: 2.2.0  
versionCode: 22000

## Automated checks (local)

| Check | Command | Result |
|---|---|---|
| Debug APK | `./gradlew :app:assemblePlaystoreDebug` | PASS |
| Unit tests | `./gradlew :app:testPlaystoreDebugUnitTest` | pending at report draft / see later update |
| Lint | `./gradlew :app:lintPlaystoreDebug` | pending at report draft / see later update |
| Release APK (unsigned) | `./gradlew :app:assemblePlaystoreRelease` | pending at report draft / see later update |
| Reconstruction SHA (2.1.0) | `bootstrap/verify_reconstruction.sh` | PASS before overlay |
| Overlay | `bootstrap/verify_hotfox_2_2_0.sh` | PASS |
| Static | `python3 verification/static_check_2_2_0.py` | PASS |

## Debug APK artifacts

Generated under `V2rayNG/app/build/outputs/apk/playstore/debug/`:

- `HotFox_Proxy_2.2.0_arm64-v8a.apk`
- `HotFox_Proxy_2.2.0_armeabi-v7a.apk`
- `HotFox_Proxy_2.2.0_x86.apk`
- `HotFox_Proxy_2.2.0_x86_64.apk`
- `HotFox_Proxy_2.2.0_universal.apk`

Native HEV/Xray libraries are packaged via `jniLibs` (`libhev-socks5-tunnel.so`, `libhevsockstun.so`, `libgojni.so`).

## Physical device E2E

See `docs/E2E_DEVICE_REPORT_2.2.0.md`. **NOT EXECUTED**.

## Security

No personal subscription URL, VLESS UUID, keystore, or signing password is committed. `SecretRedactor` covers proxy URIs, HTTPS URLs, UUIDs, query tokens, and Authorization headers.

## Merge readiness

Source/build candidate after CI is green. Not a fully verified production release until physical-device E2E PASS.
