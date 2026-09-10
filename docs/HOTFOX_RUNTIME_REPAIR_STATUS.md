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
3. **protect broker** — `HotfoxSocketProtect` publishes an immutable
   `(attempt, protector)` snapshot via `AtomicReference` + CAS detach.
   `CoreVpnService` keeps an instance `protectAttempt` and tears down only
   that token, so an older attempt cannot clear or invoke a newer callback
   (`HF-VPN-015`).
4. **Datapath proof** remains `VpnReadiness.injectThroughVpn` on `TRANSPORT_VPN`
   bindSocket — not process-direct HTTPS from the excluded UID.
5. **lifecycle ownership** — pipeline receives the attempt from
   `onStartCommand`, never `currentAttempt()`. `claimTeardown(owned)` is
   exclusive (second same-attempt claim fails) and runs before start-lock
   release. Replacement admission joins the prior pipeline on `serviceScope`,
   not via `runBlocking` on Android lifecycle callbacks.

### P1 — bindProcessToNetwork truth

`interpretBindAttempt`: only `true` with no throwable is success.

### P1 — composite profile semantics

`HotfoxOutboundCompare` does not compare `CUSTOM` / `POLICYGROUP` / `PROXYCHAIN`
to a generated `vless`/`vmess` protocol string.

Type-specific expected-plan comparison (round 27):

- CUSTOM derives hops/tags from the supplied raw JSON (`expectedJson` /
  `MmkvManager.decodeServerRaw`); an unrelated valid proxy or missing source
  plan is blocking;
- POLICYGROUP compares the resolved member hop set (missing/changed member
  is blocking);
- PROXYCHAIN compares ordered hops (reorder or omitted hop is blocking);
- secret-safe transport/security/REALITY fields remain in the compare;
- missing `expectedJson` fail-closes (`generated:missing-expected-plan`).

AUTO persist captures a selection generation with the request. `selectManual`
bumps that generation. A late AUTO result is committed only if AUTO mode and
the generation are unchanged; otherwise the manual GUID wins
(`stale-auto-superseded`).

### P1 — canonical tags

`HotfoxXrayTagResolver` maps actual outbound/balancer tags
(`provider-proxy`, `hotfox-direct`, `hotfox-block`, …).
`HotfoxXrayConfigInjector` rewrites literal `proxy`/`direct`/`block` to those tags.
GLOBAL/SMART drop legacy `.ru` / `.su` / `.рф` / geosite:cn / geoip:private DIRECT rules.
`HotfoxXrayConfigValidator.requireValid` rejects dangling tags (`HF-VPN-016`) before core start.

## Host CI evidence

Do not cite `e85af2d`, `b91db24`, `3d21a27`, or `bc4e366` as this candidate.
Those heads are historical.

The required host record for **this** commit is the push-CI artifact
`candidate-evidence.txt` (`candidate_sha=${GITHUB_SHA}`) plus APK SHA-256 from
the same run that reconstructed, tested, linted, assembled debug, and compiled
unsigned release. That workflow is `.github/workflows/hotfox-bootstrap-ci.yml`.

## Executed vs deferred

| Gate | Status |
| --- | --- |
| Overlay unit tests for exclusive teardown + async admission | in this HEAD; host CI of this SHA is the execution record |
| Host reconstruction / lint / assembleDebug / unsigned release | this SHA via `hotfox-bootstrap-ci.yml` (`candidate_sha=${GITHUB_SHA}`) |
| SHA-tied debug APK + `candidate-evidence.txt` | this SHA via the same workflow artifact |
| HotFox AI phase-exit review | requested on this coherent head |
| Emulator VPN E2E | **NOT EXECUTED** (no SDK/KVM here; e2e workflow is secret-gated) |
| Physical device | **NOT EXECUTED** — still `FINAL RELEASE DEVICE GATE` |
| `RELEASE READY` | **not claimed** |

If CI emulator infrastructure is unavailable, that gate stays **BLOCKED / NOT EXECUTED**, never PASS.
