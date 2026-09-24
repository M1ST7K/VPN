# CI preflight

## Frozen candidate `1d4c3ea8f09fae058a8def9474ad88d1f92e7739` (branch HEAD)

Push run https://github.com/M1ST7K/VPN/actions/runs/35992752740 — **success**
(payload integrity, reconstruction, overlay verify, static check, debug assemble,
unit tests incl. strict-gate coverage, lint, unsigned release compile, publish;
emulator UI smoke skipped/not counted). Includes outer-harness hardening `3c40127`.

| APK | SHA-256 |
|---|---|
| universal | `5402e8907e1ad10f396035e6d418eb2de32fb68b873718560fe04895c7bdc395` |
| x86_64 | `fde931b93c67030c574094a647daf25af8d118cc56befa2a489cd3d3a50727a7` |
| x86 | `5e2a20f7a5e41a66cb69ed0f02cc78663c8e2f6e75ea1ef5f8fc46f695c26e80` |
| arm64-v8a | `b62c32b097316b02e40ce6f44f401706b32c55291649267640dda7762e069375` |
| armeabi-v7a | `5a263e29492fe0798a96b50d5be8c1daf11ab53535b007c54f27d7cee3d646f0` |

## Superseded `1a15511abff8e5f06bcc1706f77145708216c3e4`

Push run https://github.com/M1ST7K/VPN/actions/runs/35991592077 — **success**
(payload integrity, reconstruction, overlay verify, static check, debug assemble,
unit tests incl. strict-gate coverage, lint, unsigned release compile, publish;
emulator UI smoke skipped/not counted).

| APK | SHA-256 |
|---|---|
| universal | `ec4b847b51b5d278af13920b7006feabfa0909ef226faa272f399a98397b43b0` |
| x86_64 | `162f27215351e694442ee51c5ab1f8809779fbcdbf9a21448e5ff67eebb27089` |
| x86 | `bb71c03473bca516fe41c4c361823ed70fb9bfc953dcab905e4ac8ba4bd881a7` |
| arm64-v8a | `305619f8fdab4f572c0b68d358e2b5b3cd063d500fe53bda0067dd86dba9bb23` |
| armeabi-v7a | `7e8a839c81a3f78c4333a962bcbe8289f0238c9022e68a6d0a9108feae3ffc84` |

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
CI universal debug APK SHA-256 `d60ff74b89e2d0f04175132758d824e339612b5dc51d6f9138a6564e373ba052`
(verified from the run artifact). The local VM build of the same SHA hashes
differently (`3dd1e308…0f48`) because debug builds are not bit-reproducible across
environments; the local APK was used only for emulator run 1. This candidate is
stale: it contains the geo-asset defect fixed in `2c3df36`.

## Local (this VM) — actually executed

- `08b35ff`: reconstruction, overlay verify, static check, `assemblePlaystoreDebug` — PASS.
- after fix: static check FAIL on old source / PASS on fixed source;
  `assemblePlaystoreDebug` + `testPlaystoreDebugUnitTest` — PASS.
- `HOTFOX_REQUIRE_RELEASE_SIGNING=true` configuration — fails closed as designed.

Later commits on the branch that only touch `verification/release_gate_20260924/`
and `.ai/` do not change app source; the app candidate stays `2c3df36`.
