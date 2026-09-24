# Release candidate freeze — final release validation gate 2026-09-24

## Current candidate

| Field | Value |
|---|---|
| Commit SHA | `1a15511abff8e5f06bcc1706f77145708216c3e4` (branch HEAD at freeze; app source identical to `2c3df36`) |
| CI run / artifact | [35991592077](https://github.com/M1ST7K/VPN/actions/runs/35991592077) — success; universal debug APK SHA-256 `ec4b847b51b5d278af13920b7006feabfa0909ef226faa272f399a98397b43b0` |
| Includes | strict E2E gate `091dcbd`, `73e90f9`, `242c0e2`, `3c40127` + geo-asset fix `2c3df36` |
| Branch | `cursor/hotfox-final-release-validation-20260924` |
| applicationId | `com.hotfox.vpn` (playstore flavor) |
| versionName / versionCode | `2.2.0` / `22000` (per-ABI playstore overrides `1000000*abi + 22000`) |
| Channel | `dev` (`HOTFOX_CHANNEL`, CI default) |
| Source provenance | `bootstrap/bootstrap_source.sh`: upstream v2rayNG `2.2.6` @ `15b4fff8e45da9bc0acaa5cc1d80a1d3531e8712`, payload SHA-256 `c9dd4656…a85260`, 2.1.0 overlay `fe597332…c80ed`, `libv2ray.aar` v26.6.27 `7846eb7f…de1e`, then `bootstrap/hotfox_2_2_0/` overlay |
| Signed RC artifact | **none** — see `SIGNING.md` (BLOCKED) |

## Candidate history (evidence staleness)

| SHA | Why it changed | Evidence status |
|---|---|---|
| `08b35ffd75e36f30ae0ce0bd3c3fadda1ad748bd` | gate start | CI green; local build; emulator E2E run 1 **FAIL** (found defect below). Stale for release. |
| `3c4012707c0461131d524816969af2805009a562` | orchestrator pushed stricter E2E harness (TUN egress + public-IP gate) during validation | superseded before own evidence was collected |
| `2c3df36f7c6d788ff76d4d2dc4ed8ea6981a94c2` | blocking fix: geo assets installed before every Xray start | CI green (run 35990105659); artifact superseded by HEAD build of the same app source |
| `1a15511abff8e5f06bcc1706f77145708216c3e4` | HEAD after evidence-only commits; owner: use latest HEAD artifact | **current**; CI green; runtime E2E BLOCKED (dispatch/infra) |

Later commits that only touch `verification/release_gate_20260924/` or `.ai/` do
not change the app; the `1a15511` CI artifact stays the frozen candidate unless app
source changes.

All earlier runtime evidence is declared **stale** for the current candidate.

## Local debug build of `08b35ff` (this VM, JDK 17, SDK 37, NDK 29.0.14206865)

`HotFox_Proxy_2.2.0_universal.apk` SHA-256 `3dd1e30844a277f03fbe44b92a1a118941f559baab73b5702ea6743901d50f48`

## Local debug build after fix (overlay of `2c3df36` applied on the same reconstruction)

| APK | SHA-256 |
|---|---|
| universal | `cf3dfac8f10195946c9c7ae404bdda8017ee068eb2fad91d1a5be1acdd18060e` |
| x86_64 | `f3fc9976075372fb9bfdc29818d813e3fc43a8ef8342bcf16f0757b5ae186b0e` |
| arm64-v8a | `7fdd5ec8ed29099b70ea511932d35fe11cebd89996e66cb07b90765446857f89` |

Local APKs are debug-signed engineering artifacts, not release candidates. The CI
`hotfox-playstore-debug-apk-sha256` artifact of the candidate push run is the
authoritative debug-artifact hash.
