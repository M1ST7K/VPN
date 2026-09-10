# HotFox runtime repair — execution status

Owner opened the gate from HEAD `d723f65` via `docs/HOTFOX_RUNTIME_REPAIR_EXECUTION_PROMPT.md`.
Previous “runtime deferred / no-op” wording is not a stop reason for this repair.

## Provenance (Phase 0)

- Branch: `cursor/hotfox-2.4-sync-rules-22f7`
- Prompt HEAD: `d723f65c8d62548323240538c61ec49fdb997a35`
- Trusted `origin/main` at start: `d516623c436c4c4ed1a6b010f451917a76c2f30e`
- Merge-base with `origin/main`: `d516623`
- Pinned upstream v2rayNG: tag `2.2.6` / `15b4fff8e45da9bc0acaa5cc1d80a1d3531e8712`
- Pinned AndroidLibXrayLite AAR: `v26.6.27` (`libv2ray.aar` SHA-256 in `bootstrap/bootstrap_source.sh`)
- Xray-core bundled by that AAR: `v26.6.27`
- `CoreCallbackHandler` in that AAR: `Startup` / `Shutdown` / `OnEmitStatus` only — **no protect(fd)**
- Local Android SDK/NDK/emulator in this agent: **absent** (`NOT EXECUTED`)
- Reconstruction/unit/lint/assembleDebug: **CI** (`hotfox-bootstrap-ci.yml`)

## Engineering fixes in overlay (`bootstrap/hotfox_2_2_0`)

### P0 — native Xray sockets vs TUN recursion

AndroidLibXrayLite cannot callback `VpnService.protect(fd)` for Go sockets.
`bindProcessToNetwork` is libc-only and is **not** treated as sufficient.

Implemented architecture:

1. **UID exclusion** — `HotfoxTunSelfExclusion` always keeps `com.hotfox.vpn` off TUN
   (GLOBAL/SMART disallow-self; INCLUDE omit-self from allow-list; EXCLUDE disallow-self).
2. **Defense-in-depth bind** — `VpnLoopPrevention` records the real Boolean from
   `bindProcessToNetwork`; false / exception / stale Network fail-closed (`HF-VPN-012`).
3. **protect broker** — `HotfoxSocketProtect` race-safe attach/detach; false / exception /
   stale service / stale generation fail-closed (`HF-VPN-015`).
4. **Datapath proof** remains `VpnReadiness.injectThroughVpn` on `TRANSPORT_VPN`
   bindSocket — not process-direct HTTPS from the excluded UID.

### P1 — bindProcessToNetwork truth

`interpretBindAttempt`: only `true` with no throwable is success.

### P1 — composite profile semantics

`HotfoxOutboundCompare` does not compare `CUSTOM` / `POLICYGROUP` / `PROXYCHAIN`
to a generated `vless`/`vmess` protocol string. Containers require a real proxy
outbound; single-node drift (port/protocol/TLS/REALITY) still fail-closed.

### P1 — canonical tags

`HotfoxXrayTagResolver` maps actual outbound/balancer tags
(`provider-proxy`, `hotfox-direct`, `hotfox-block`, …).
`HotfoxXrayConfigInjector` rewrites literal `proxy`/`direct`/`block` to those tags.
GLOBAL/SMART drop legacy `.ru` / `.su` / `.рф` / geosite:cn / geoip:private DIRECT rules.
`HotfoxXrayConfigValidator.requireValid` rejects dangling tags (`HF-VPN-016`) before core start.

## Host CI evidence (green)

Push run `34526242362` on SHA `e85af2dff3026be258d66bfa04fafa0fa8fcd2e7`: reconstruct, static, unit, lint, unsigned release, debug APK, publish `hotfox-dev-latest` — SUCCESS.

SHA-256 of that APK set:

- `arm64-v8a` `00dcd4c8d2d367607bb164ba59b4540acda38a1ef745111cf7dc2c29a05ad457`
- `armeabi-v7a` `e48a1a8a4c0b545e4de660f5959732154570e5c4ce5df13db8ec5673cde20898`
- `universal` `5aaa0ed3d8f6cbbfd32943f8da7b69199db2ca9c74c1b537f2abdb1b6de3967a`
- `x86` `558b99e89f8ddc93d3cd57030817ca46f59fecaab5e84951b099b6c34a9ef897`
- `x86_64` `3aa31256820492847b2cda67cc6ecbd2cd18317319684554d2203052df613b3f`

Download (dev prerelease, not release-ready): https://github.com/M1ST7K/VPN/releases/tag/hotfox-dev-latest

## Executed vs deferred

| Gate | Status |
| --- | --- |
| Overlay unit tests for the matrix above | PASS on `e85af2d` |
| Host reconstruction / lint / assembleDebug / unsigned release | PASS on `e85af2d` |
| SHA-tied debug APK + `candidate-evidence.txt` | PASS on `e85af2d` |
| HotFox AI phase-exit review | requested on the next coherent head |
| Emulator VPN E2E | **NOT EXECUTED** (no SDK/KVM here; e2e workflow is secret-gated) |
| Physical device | **NOT EXECUTED** — still `FINAL RELEASE DEVICE GATE` |
| `RELEASE READY` | **not claimed** |

If CI emulator infrastructure is unavailable, that gate stays **BLOCKED / NOT EXECUTED**, never PASS.
