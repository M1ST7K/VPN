# HOTFOX — FINAL RELEASE VALIDATION EXECUTION
Date: 2026-09-24
Base: main after final UI/Onest integration
Branch: cursor/hotfox-final-release-validation-20260924

## 0. Mission

This is the FINAL RELEASE VALIDATION GATE.

Do not add product features.
Do not redesign the approved UI.
Do not move fox/hero geometry.
Do not reopen completed roadmap phases unless a real regression is discovered.

The job is to prove the actual shipping product and produce one factual release verdict.

Until every required gate passes:
RELEASE READY = NO.

## 1. Canonical sources

Read first:
- AGENTS.md
- docs/CURRENT_PHASE.md
- docs/phases/final-release-validation-gate.md
- docs/HOTFOX_ROADMAP.md -> FINAL RELEASE DEVICE GATE
- docs/HOTFOX_RUNTIME_REPAIR_STATUS.md
- docs/HOTFOX_AUTOMATED_VPN_E2E.md
- verification/ui/typography_onest_pass4_20260923/VALIDATION.md
- verification/ui/typography_onest_pass4_20260923/UNTESTED_GAPS.md
- .cursor/rules/22-hotfox-owner-release-validation-gate.mdc
- .cursor/rules/38-hotfox-final-release-validation.mdc

Historical UI rules 23–37 are archived and must not be treated as active work.

## 2. Hard truth rules

Never label missing evidence PASS.

Use only:
- PASS
- FAIL
- BLOCKED
- NOT EXECUTED

Never infer VPN proof from:
- VPN icon;
- TUN establishment alone;
- running Xray;
- listening SOCKS port;
- screenshot;
- successful compile;
- debug fixture CONNECTED state.

Protected state is valid only through the canonical verified production path.

Never expose:
- subscription URL;
- UUID/token/password;
- signing key/keystore bytes;
- keystore passwords;
- API/private keys;
- payment-provider secrets.

No secret value may appear in logs, screenshots, committed files, PR comments, or final report.

## 3. Candidate freeze

Start from the exact branch SHA.

Record:
- commit SHA;
- versionName/versionCode;
- applicationId;
- channel;
- source/overlay provenance.

Do not silently move the candidate after validation starts.

If a blocking fix is required:
1. commit the fix;
2. declare previous evidence stale;
3. record the new candidate SHA;
4. rerun every affected gate.

Create:
verification/release_gate_20260924/CANDIDATE.md

## 4. CI preflight

Verify current main/integration CI on the exact baseline:
- payload integrity;
- reconstruction;
- overlay verify;
- static check;
- debug assemble;
- unit tests;
- lint;
- unsigned release compile;
- fixture leak;
- relevant review-cap/checkpoint checks.

Do not call skipped emulator jobs PASS.

Create a concise machine-readable gate ledger:
verification/release_gate_20260924/gate_status.json

## 5. Signing / release candidate

Inspect existing release signing contract in app/build.gradle.kts.

Required environment contract already exists:
- HOTFOX_KEYSTORE_PATH
- HOTFOX_KEYSTORE_PASSWORD
- HOTFOX_KEY_ALIAS
- HOTFOX_KEY_PASSWORD
- HOTFOX_REQUIRE_RELEASE_SIGNING=true

Do not invent a signing identity.

Do not generate a new production signing key unless the owner explicitly authorizes key creation.

Check whether authorized signing material exists in the execution environment without printing secret values.

If complete authorized signing material exists:
- build playstoreRelease with required signing enabled;
- verify APK signature with apksigner;
- record certificate SHA-256/fingerprint only if safe/public for release identity;
- record APK SHA-256;
- freeze this signed APK as the release candidate.

If signing material is unavailable:
- mark SIGNED_RC = BLOCKED;
- continue engineering-runtime E2E using the exact debug candidate where safe;
- do not claim physical release acceptance or RELEASE READY.

Never commit the keystore.

## 6. Existing automated VPN E2E

Use the existing:
- .github/workflows/hotfox-vpn-e2e.yml
- verification/ci_run_vpn_e2e.sh
- verification/emulator_vpn_e2e.sh
- HotfoxEngineeringRuntimeE2e
- HotfoxEngineeringE2eActivity

This flow expects only a dedicated test subscription via HOTFOX_TEST_SUBSCRIPTION_URL.

Never use a personal/production subscription in repository files or logs.

First determine test-secret availability WITHOUT reading/printing the value.

If GitHub secret is configured:
- dispatch the existing workflow against this branch/candidate;
- inspect secret gate;
- inspect KVM availability;
- inspect build artifact SHA;
- inspect E2E diagnostics.

If secret is absent:
VPN_E2E = BLOCKED (missing dedicated test secret).

If /dev/kvm is absent:
VPN_E2E = NOT EXECUTED / infrastructure blocked, never PASS.

## 7. Required emulator/runtime VPN proof

When runtime test infrastructure is available, prove:

A. Baseline
- establish normal network access before VPN;
- query public IP using a safe test endpoint;
- store only redacted evidence.

B. Import
- inject/import dedicated test subscription only at runtime;
- never print URL/token.

C. Connect
- start through real UI/runtime or engineering E2E entrypoint while preserving production datapath;
- grant Android VPN permission through test automation;
- wait only on bounded canonical readiness.

D. Datapath
Prove:
Android traffic
-> VpnService/TUN
-> HEV/tun2socks
-> local SOCKS
-> Xray
-> remote server
-> Internet
-> response.

E. Public IP
- before VPN;
- during verified VPN;
- after disconnect.
When environment supports a distinct egress:
before != during
after returns to expected non-VPN path.

F. Real traffic
- HTTPS request;
- browser/device-wide request where emulator automation permits.

G. DNS
- no silent DNS bypass under protected state;
- validate configured DNS policy with available diagnostics/tests.

H. IPv6
- verify ::/0 routing or fail-closed behavior;
- include NAT64-aware checks where environment supports it;
- absence of IPv6 in the test network must be reported as NOT EXECUTED for live leak proof, not PASS.

I. Lifecycle
- disconnect restores normal networking;
- run at least 3 connect/disconnect/reconnect cycles;
- no stale protected state;
- no false Защищено.

J. Diagnostics
- redact secrets;
- preserve relevant service/core/path evidence.

## 8. Local Mac execution

The owner's authorized Mac may be used for local validation.

Current known environment at gate start:
- Android SDK present at ~/Library/Android/sdk;
- adb present;
- emulator binary present;
- no AVD detected at preflight;
- no Android device detected at preflight;
- release signing env variables were unset at preflight;
- HOTFOX_TEST_SUBSCRIPTION_URL env was unset at preflight.

Re-check rather than assuming this remains true.

If no AVD exists:
- inspect installed system images;
- create a clean API 34 AVD if possible without destructive changes;
- if necessary install the minimum required emulator system image;
- do not delete unrelated owner AVDs/data.

Local emulator E2E may be used if a dedicated runtime test subscription is securely available through an authorized environment variable/file/secret mechanism.

Do not copy subscription material into the repo.

## 9. Physical-device gate R1–R8

Physical validation is mandatory before RELEASE READY.

If no authorized Android device is connected/available:
- create the complete executable test plan;
- mark R1–R8 physical execution BLOCKED/NOT EXECUTED as appropriate;
- continue every non-physical gate;
- do not claim release ready.

When a real device becomes available, execute:

R1 Install/update
- install exact signed RC;
- update from supported prior version if available;
- launch;
- VPN permission;
- notification/foreground service.

R2 Traffic proof
- real external IP before/during/after;
- browser traffic;
- app traffic;
- internet through tunnel;
- disconnect recovery;
- no false protected state.

R3 DNS leak
- real-device DNS policy/leak acceptance.

R4 IPv6 leak
- IPv6 routed or fail-closed;
- test on IPv6-capable network when available.

R5 Handoff
- Wi-Fi -> cellular;
- cellular -> Wi-Fi;
- network loss/restore;
- bounded recovery;
- no duplicate contradictory sessions.

R6 AUTO / Shadow
- real AUTO selection;
- failover where safely inducible;
- at least one Shadow/alternate recovery path where infrastructure permits.

R7 Commerce
If HotFox-managed purchase is in release scope:
- plan;
- order;
- hosted checkout;
- authoritative backend verification;
- entitlement;
- recovery/sync.
Manual HTTPS subscription remains independently functional.
Never expose payment secrets.

R8 Lifecycle
- foreground/background;
- process recreation;
- reboot/restore policy;
- rapid connect/disconnect;
- permission denial/revocation;
- notification actions;
- Quick Settings Tile if shipped.

## 10. UI regression smoke

Do not redesign.

Smoke:
- Splash;
- Connect HotFox;
- AUTO;
- HTTPS import;
- Home Disconnected/Connecting/Connected;
- screens 08–18 basic launch/navigation;
- Onest loads;
- no obvious clipping/overlap.

This is a regression gate only.

## 11. Fix loop

Any actual blocking defect:
FAIL
-> root-cause
-> smallest correct fix
-> unit/static coverage
-> rebuild
-> new candidate SHA
-> rerun affected runtime/device gates.

Forbidden shortcuts:
- fake protected state;
- direct internet bypass for readiness;
- disable DNS or IPv6 fail-closed;
- weaken TLS/REALITY;
- skip path verification;
- convert timeout into success;
- hardcode test IP/traffic;
- disable entitlement authority.

## 12. Evidence directory

Create:
verification/release_gate_20260924/

At minimum:
- CANDIDATE.md
- gate_status.json
- CI.md
- SIGNING.md
- EMULATOR_E2E.md
- PHYSICAL_DEVICE_R1_R8.md
- SECURITY_PRIVACY.md
- UI_SMOKE.md
- FINAL_VERDICT.md
- sanitized machine-readable reports/log references where safe.

Do not commit secret-bearing raw logs.

## 13. Final verdict

Only two acceptable end states for this task:

A. RELEASE READY
Only if:
- exact signed RC is available;
- required CI is green;
- required emulator/runtime E2E actually ran and passed;
- physical R1–R8 required acceptance actually ran and passed;
- blocking findings fixed and rerun;
- no secret exposure.

B. NOT RELEASE READY
With an exact blocking list:
- what is BLOCKED;
- what is NOT EXECUTED;
- what FAILED;
- what needs owner/device/infrastructure action.

Never convert missing device/secret/signing material into PASS.

## 14. Execution behavior

Do not stop at a plan.
Do not re-open UI redesign.
Do not ask the owner to manually inspect code.

Execute all gates that the environment permits now.

For unavailable external prerequisites:
record the blocker exactly and proceed with every independent gate.

