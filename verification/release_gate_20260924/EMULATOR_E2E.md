# Emulator / runtime VPN E2E

## A. GitHub secret-driven workflow (`.github/workflows/hotfox-vpn-e2e.yml`)

| Check | Result |
|---|---|
| Workflow present and `active` | yes |
| Ever dispatched | **never** (no runs in history) |
| `HOTFOX_TEST_SUBSCRIPTION_URL` secret present | **unknown** — agent token cannot list secrets (HTTP 403) and the workflow never ran |
| Dispatch by agent | **not possible** — agent `gh` is read-only |

Re-check 2026-09-24 11:10 UTC after the owner requested an explicit dispatch attempt:

- repo API reports the agent integration token permissions as
  `admin/maintain/push/triage/pull = false`; `workflow_dispatch` needs
  `actions: write`, and the agent's GitHub access is read-only by policy, so no
  dispatch was sent (no write attempted);
- `GET actions/secrets/HOTFOX_TEST_SUBSCRIPTION_URL` → HTTP 403; org secrets → 403;
  repository environments: none;
- `hotfox-vpn-e2e.yml` run history: still empty.

Secret presence is therefore **UNKNOWN**, not absent. Only a dispatch by the owner
(or an actor with `actions: write`) lets the workflow's own gate decide.

Candidate to dispatch: branch head (app source identical to `2c3df36`; later
commits touch only `verification/release_gate_20260924/` and `.ai/`). Dispatching
on `08b35ff` would re-hit the geo-asset defect below, because a fresh emulator
install has no geo files regardless of subscription.

`VPN_E2E_GITHUB = BLOCKED (dispatch not permitted from agent; secret presence unknown)` — owner must add a dedicated test-only
`HOTFOX_TEST_SUBSCRIPTION_URL` secret (if absent) and dispatch the workflow on
`cursor/hotfox-final-release-validation-20260924` at the current candidate SHA.

## B. Local emulator run on this Cursor cloud VM

Infrastructure actually used:

- Android emulator 37.1.11, AVD `hotfox34` (`system-images;android-34;google_apis;x86_64`), KVM, headless;
- existing harness `verification/emulator_vpn_e2e.sh` → `HotfoxEngineeringE2eActivity` →
  `HotfoxEngineeringRuntimeE2e` (unchanged, secret file pushed then deleted);
- **ephemeral self-hosted test server**: Xray-core 26.3.27 VLESS + REALITY
  (`xtls-rprx-vision`, SNI `www.cloudflare.com`) on the VM, reached from the
  emulator as `10.0.2.2:8443`. UUID / x25519 keys / shortId were generated at
  runtime into a `0700` temp dir, never printed, never committed. The base64
  subscription was served over a Cloudflare quick tunnel (publicly trusted TLS,
  app `network_security_config` unchanged: system CAs only, cleartext off) from a
  random 128-bit path, and removed after the run.
- Server independently proven from the host: host Xray SOCKS client →
  REALITY → server → Internet returned HTTPS 200 / public IP.

This is **not** the owner's remote server and not a dedicated external test
subscription; it only exercises the production client datapath. Public-IP
comparison is **non-informative** here: the VM egress NAT rotates per connection
(5 consecutive direct requests returned 4 different `/24`s).

### Run 1 — candidate `08b35ff`, universal debug APK `3dd1e308…0f48`

Result: **FAIL** `reason=socks-only-https` (report: `evidence/e2e_run1_08b35ff_report.redacted.txt`).

- Symptom: proxy-only isolation never reached `PROXY_ONLY`; `socksHttps=none`, no TUN started (correct fail-closed; no false protected state).
- Root cause (logcat, `evidence/e2e_run1_08b35ff_rootcause_logcat.redacted.txt`):
  `StartCore-Manager: config error … illegal domain rule: geosite:google … failed to open geosite.dat`.
  `geosite.dat`/`geoip.dat` ship inside the APK but are copied to the user asset
  dir only by `MainActivity` (`MainViewModel.initAssets`) and `CoreVpnService`.
  A fresh install lands in `HotfoxOnboardingActivity`, so `CoreProxyOnlyService`
  and `CoreRootService` launched Xray with routing rules referencing geo files
  that did not exist → Xray rejects the entire config. Confirmed on device:
  `/sdcard/Android/data/com.hotfox.vpn/files/assets/` empty after launch.
- Fix (`2c3df36`): `CoreServiceManager.startCoreLoop()` calls
  `SettingsManager.initAssets(service, service.assets)` before `doStartCoreLoop`
  for every mode (idempotent: copies only missing files). Regression:
  `verification/static_check_2_2_0.py` now fails if `startCoreLoop` launches Xray
  before installing assets (verified: FAIL on old source, PASS on fixed source).
  Local `:app:assemblePlaystoreDebug :app:testPlaystoreDebugUnitTest` PASS after fix.

### Run 2 — candidate `2c3df36`

**BLOCKED (infrastructure)**. While the fixed APK was being built, the cloud VM
kernel hit `kernel BUG at arch/x86/kvm/x86.c:702` / `kvm_spurious_fault`
(`evidence/vm_kvm_kernel_bug.txt`). The running emulator and tmux server died;
every subsequent emulator launch hung before guest boot (qemu at ~0% CPU,
adb never saw the device), including `-no-snapshot -wipe-data`. Nested KVM in this
VM is unusable until the VM is replaced.

## Required proof items for the current candidate

| Item | Status |
|---|---|
| Baseline network / public IP before | NOT EXECUTED (current candidate) |
| Runtime subscription import | NOT EXECUTED (current candidate); run 1 import succeeded (servers present, selection made) |
| SOCKS-only Xray egress | NOT EXECUTED (current candidate); run 1 FAIL → fixed |
| TUN → HEV → SOCKS → Xray → server → Internet | NOT EXECUTED |
| Real HTTPS / browser traffic | NOT EXECUTED |
| Public IP before/during/after | NOT EXECUTED (non-informative in this VM even when runnable) |
| DNS policy / no bypass | NOT EXECUTED |
| IPv6 routed or fail-closed | NOT EXECUTED (no IPv6 in emulator network) |
| Disconnect restore, ≥3 cycles | NOT EXECUTED |
| No false `CONNECTED` / `Защищено` | run 1: no protected state claimed while path failed (consistent), not a full proof |

`EMULATOR_E2E = BLOCKED`.

## To reproduce on working infrastructure

```bash
# dedicated test subscription only; never echo it
export HOTFOX_TEST_SUBSCRIPTION_URL="$(cat /secure/path/test-sub.url)"
HOTFOX_E2E_CYCLES=3 bash verification/emulator_vpn_e2e.sh dist/HotFox_Proxy_2.2.0_universal.apk
```
