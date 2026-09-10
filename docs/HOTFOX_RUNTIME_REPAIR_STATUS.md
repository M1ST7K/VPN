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

## Executed vs deferred

| Gate | Status |
| --- | --- |
| Overlay unit tests for the matrix above | added; run in reconstruct CI |
| Host reconstruction / lint / assembleDebug / unsigned release | CI on this SHA |
| SHA-tied debug APK + `candidate-evidence.txt` | CI artifact |
| Emulator VPN E2E | **NOT EXECUTED** in this agent (no SDK/KVM). Existing secret-gated workflow only. |
| Physical device | **NOT EXECUTED** — still `FINAL RELEASE DEVICE GATE` |
| `RELEASE READY` | **not claimed** |

If CI emulator infrastructure is unavailable, that gate stays **BLOCKED / NOT EXECUTED**, never PASS.
