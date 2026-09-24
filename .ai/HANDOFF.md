# Executor handoff

TASK_ID: hotfox-final-release-validation-20260924
EXECUTOR: Cursor cloud agent
STATUS: BLOCKED — NOT RELEASE READY
BRANCH: cursor/hotfox-final-release-validation-20260924
COMMIT_SHA (app candidate): 2c3df36f7c6d788ff76d4d2dc4ed8ea6981a94c2

## Summary
Executed every gate the Cursor cloud VM permitted. Found and fixed one real
runtime defect via emulator E2E. Signed RC, runtime rerun on the fixed candidate,
GitHub VPN E2E and physical R1–R8 remain blocked by missing external
prerequisites / infrastructure. Evidence: `verification/release_gate_20260924/`.

## Verification actually performed
- CI push run 35990105659 on `2c3df36`: payload, review-cap tests, reconstruction,
  overlay verify, static check, debug assemble, unit tests, lint, unsigned release
  compile, sandbox flag check, publish — all PASS. Emulator UI smoke skipped (manual-only), not counted.
- Local VM (JDK 17, SDK 37, NDK 29.0.14206865): full reconstruction + debug build of
  `08b35ff`; after fix, debug build + unit tests PASS; static check guard verified
  (FAIL old / PASS new).
- `HOTFOX_REQUIRE_RELEASE_SIGNING=true` fails closed without keystore env — PASS.
- Emulator API 34 (KVM) engineering E2E run 1 on `08b35ff` with an ephemeral
  self-hosted VLESS+REALITY test server (credentials generated at runtime, never
  printed/committed; subscription served over publicly trusted TLS): **FAIL**
  `socks-only-https`.

## Defect fixed
- Symptom: proxy-only isolation never reached PROXY_ONLY; Xray start failed.
- Root cause: `geosite.dat`/`geoip.dat` are copied to the user asset dir only by
  `MainActivity` and `CoreVpnService`; a fresh install starts in onboarding, so
  `CoreProxyOnlyService`/`CoreRootService` launched Xray with geosite routing rules
  and no geo files → `failed to open geosite.dat` → whole config rejected.
- Fix: `CoreServiceManager.startCoreLoop()` installs assets before `doStartCoreLoop`
  (all modes, idempotent). Regression guard in `verification/static_check_2_2_0.py`.
- Commit: `2c3df36`. Runtime rerun: BLOCKED (below).

## Blockers (exact)
1. SIGNED_RC — no authorized keystore in cloud VM or Mac env; no CI signing path. Owner must supply existing production keystore securely.
2. VPN_E2E_GITHUB — `hotfox-vpn-e2e.yml` never dispatched; agent `gh` is read-only; test-secret presence unknown (403). Owner: ensure dedicated `HOTFOX_TEST_SUBSCRIPTION_URL` and dispatch on this branch.
3. EMULATOR_E2E current candidate — cloud VM kernel BUG `kvm_spurious_fault` (arch/x86/kvm/x86.c:702); emulator can no longer boot in this VM.
4. PHYSICAL R1–R8 — no authorized device; procedure in `PHYSICAL_DEVICE_R1_R8.md`.

## Known risks / limitations
- Public-IP comparison is non-informative on this VM (rotating NAT egress); the new
  strict harness gate (`ipChangedThroughTun`) needs a distinct-egress server (GitHub
  runner + remote test server or real device).
- DNS / IPv6 / lifecycle / handoff / commerce not executed.
- No secret values were read, printed or committed.

## Recommended next action
Dispatch `.github/workflows/hotfox-vpn-e2e.yml` on `cursor/hotfox-final-release-validation-20260924`
(with dedicated test secret) to rerun E2E on `2c3df36`; provide signing material for the
signed RC; then execute physical R1–R8.
