# CI preflight

## Current candidate `2c3df36f7c6d788ff76d4d2dc4ed8ea6981a94c2`

Push run: https://github.com/M1ST7K/VPN/actions/runs/35990105659 — **success**

| Step | Result |
|---|---|
| Payload integrity (`verify_payload.sh`) | PASS |
| Review-cap unit tests | PASS |
| Reconstruction (`bootstrap_source.sh`) | PASS |
| Overlay verify (`verify_hotfox_2_2_0.sh`) | PASS |
| Static check 2.2.0 (incl. new geo-asset guard, fixture/secret scan) | PASS |
| Debug assemble `:app:assemblePlaystoreDebug` | PASS |
| Unit tests `:app:testPlaystoreDebugUnitTest` | PASS |
| Lint `:app:lintPlaystoreDebug` | PASS |
| Unsigned release compile `:app:assemblePlaystoreRelease` | PASS |
| Sandbox commerce debug BuildConfig | PASS |
| Publish HotFox Dev Latest | PASS |
| Emulator UI smoke (manual-only job) | SKIPPED — not counted |
| AI Checkpoint Reviewer (pull_request_target) | success |

CI debug APK SHA-256 (artifact `hotfox-playstore-debug-apk-sha256`):

| APK | SHA-256 |
|---|---|
| universal | `3b4f74c4d69285b2a4f3a8f191031fa07e870c4e4d3e270f2bb09dc3c19f477e` |
| x86_64 | `cd10e907174afaf5056df5f42888ff50426ac47f26cb738c6f166627b5632187` |
| x86 | `2f4d44fb67248058375c8b283855a79ee4124bd652028f0a3923e9edf296e4a8` |
| arm64-v8a | `09b15a9292a5fb94e08826d5039e9a0311d3de091ef2dd5e5e07fed652579f9d` |
| armeabi-v7a | `9f2b6987548578cba57a424ed4402d5f3ea38b19522d523204c685474b33264d` |

## Gate-start baseline `08b35ffd75e36f30ae0ce0bd3c3fadda1ad748bd`

Push run 35981683777 — **success** (same job set; emulator smoke skipped).

## Local (this VM) — actually executed

- `08b35ff`: reconstruction, overlay verify, static check, `assemblePlaystoreDebug` — PASS.
- after fix: static check FAIL on old source / PASS on fixed source;
  `assemblePlaystoreDebug` + `testPlaystoreDebugUnitTest` — PASS.
- `HOTFOX_REQUIRE_RELEASE_SIGNING=true` configuration — fails closed as designed.

Later commits on the branch that only touch `verification/release_gate_20260924/`
and `.ai/` do not change app source; the app candidate stays `2c3df36`.
