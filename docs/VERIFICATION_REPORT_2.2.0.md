# HotFox Proxy 2.2.0 — verification report

Baseline: reconstructed HotFox 2.1.0 overlay on v2rayNG `2.2.6` (`15b4fff8e45da9bc0acaa5cc1d80a1d3531e8712`), then committed `bootstrap/hotfox_2_2_0` overlay.

Package: `com.hotfox.vpn`  
minSdk: 24  
versionName: 2.2.0  
versionCode: 22000

## Automated checks (local)

| Check | Command | Result |
|---|---|---|
| Debug APK | `./gradlew --no-daemon --stacktrace :app:assemblePlaystoreDebug` | PASS |
| Unit tests | `./gradlew --no-daemon --stacktrace :app:testPlaystoreDebugUnitTest` | PASS (49 tests, 0 failures) |
| Lint | `./gradlew --no-daemon --stacktrace :app:lintPlaystoreDebug` | PASS |
| Release APK (unsigned) | `./gradlew --no-daemon --stacktrace :app:assemblePlaystoreRelease` | PASS |
| Overlay | `bash bootstrap/verify_hotfox_2_2_0.sh` | PASS |
| Static | `python3 verification/static_check_2_2_0.py` | PASS |
| GitHub Actions (`384682f`) | `HotFox bootstrap and Android CI` | PASS (Payload integrity + Reconstruct and build Android app) |

## Debug APK artifacts

`V2rayNG/app/build/outputs/apk/playstore/debug/`

| File | Bytes |
|---|---|
| HotFox_Proxy_2.2.0_arm64-v8a.apk | 34411954 |
| HotFox_Proxy_2.2.0_armeabi-v7a.apk | 34885426 |
| HotFox_Proxy_2.2.0_x86.apk | 35916546 |
| HotFox_Proxy_2.2.0_x86_64.apk | 35553292 |
| HotFox_Proxy_2.2.0_universal.apk | 77962790 |

## Release APK artifacts (unsigned)

`V2rayNG/app/build/outputs/apk/playstore/release/`

| File | Bytes |
|---|---|
| HotFox_Proxy_2.2.0_arm64-v8a.apk | 28857476 |
| HotFox_Proxy_2.2.0_armeabi-v7a.apk | 29235476 |
| HotFox_Proxy_2.2.0_x86.apk | 30297596 |
| HotFox_Proxy_2.2.0_x86_64.apk | 29824446 |
| HotFox_Proxy_2.2.0_universal.apk | 68339548 |

Native HEV/Xray libraries are packaged via `jniLibs` (`libhev-socks5-tunnel.so`, `libhevsockstun.so`, `libgojni.so`).

## Physical device E2E

See `docs/E2E_DEVICE_REPORT_2.2.0.md`. **NOT EXECUTED**.

## Security

No personal subscription URL, VLESS UUID, keystore, or signing password is committed. `SecretRedactor` covers proxy URIs, HTTPS URLs, UUIDs, query tokens, and Authorization headers.

## Merge readiness

Source/build candidate after GitHub Actions is green. Not a fully verified production release until physical-device E2E PASS.
