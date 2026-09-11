# HOTFOX V5.1 — CORRECTIVE AUDIT

Status: **FAIL / DO NOT MERGE / V5 CODE UNVERIFIED**

Reviewed head: `d3ef460963743198497b32993d2ca0014fc31cae`

## What is good and must be preserved

- V4 evidence remains intact: 18/18 captures and comparisons.
- V5 removed production references to the old planet/orbit assets.
- V5 created a transparent fox candidate and alpha diagnostics.
- Production `CONNECTED` is still gated by the real VPN readiness path.
- V5 did not claim visual PASS after the emulator failure.

## P0-1 — Debug fixture code leaked into `src/main`

This violates the V5 contract that reference-state fixtures are debug-only.

Current production source contains `HotfoxUiVisualOverride` under `app/src/main`. It is not a no-op interface: it contains mutable fixture state and a public `installDebugPresentation(...)` writer. Production `MainActivity`, `HotfoxServerDetailsActivity`, `GroupServerFragment`, `MainRecyclerAdapter`, and `PerAppProxyActivity` branch on those fixture flags/data.

`HotfoxServerDetailsActivity` also contains hard-coded visual fixture values in production source (`Amsterdam`, `18 ms`, `12%`) inside `renderFixture()`.

This is unacceptable even if the values start empty in release. The release source tree must not contain test presentation state, fixture rows, fake latency/load values, or production branches whose purpose is screenshot fixture rendering.

### Required fix

Refactor so that:

- all fake/reference data lives physically under `src/debug`;
- production source contains no `ReferenceServerRow`, `ReferenceAppRow`, `serversFixture`, `subscriptionFixture`, `appsFixture`, `serverDetailsFixture`, `referenceServers`, `referenceApps`, `renderFixture`, or hard-coded fixture values;
- production presentation code can expose generic/testable seams if absolutely necessary, but no fake data and no screenshot-specific naming;
- release behavior is independent of whether a debug screenshot harness ever existed;
- debug fixture entry points cannot mutate VPN/entitlement/payment/server stores.

## P0-2 — Existing leak test does not test the real leak

`HotfoxUiScreenshotFixtureLeakTest` currently scans `src/main` only for `HotfoxUiScreenshotHarness` and `HotfoxUiScreenshotScenario`. It does **not** reject `HotfoxUiVisualOverride`, fixture flags, fake server/app rows, or hard-coded fixture values. The test therefore passes while the fixture mechanism itself is compiled into production source.

Required: rewrite the leak gate to scan the release/main source for fixture-specific symbols and safe fake values. It must fail the current V5 implementation before the refactor and pass only after fixture-specific logic is removed from `src/main`.

## P0-3 — V5 visual changes are unverified

V5 implemented a large visual delta but produced **0/18 V5 screenshots** and **0/18 V5 comparisons** because the AVD failed to boot. Therefore:

- transparent fox integration is not accepted in-app yet;
- new AUTO and Ready vectors are not accepted yet;
- new fixtures for 10/11/12/15 are not accepted yet;
- typography/CTA/grid changes are not accepted yet.

Do not continue broad visual restyling blindly. First restore a capture environment, capture the current V5 code, then refine from evidence.

## P1 — Emulator recovery was not exhausted correctly

The old `hotfox34` AVD was repeatedly reused after the environment became unhealthy. `-wipe-data` is not the same as rebuilding the AVD definition and system image state from scratch.

Before declaring another hard blocker:

1. kill all emulator/qemu/netsimd/adb processes;
2. remove stale AVD lock files;
3. restart adb server;
4. create a **new AVD with a new name** from the API 34 x86_64 system image;
5. boot it once with a minimal command, no snapshots;
6. if KVM path is suspect, try one software-acceleration diagnostic boot (`-accel off`) only to distinguish KVM from image/emulator problems;
7. if the same installed image is suspect, reinstall/recreate the API 34 system image in the agent environment;
8. only if the fresh AVD still cannot reach adb, use a separate clean CI/emulator runner for screenshot QA (no secrets; debug fixture APK only).

Do not spend another visual pass changing XML while there is no renderer available.

## P1 — Transparent fox needs in-app proof, not only alpha statistics

The alpha report is useful, but it acknowledges a gap near the ear tuft and a notch at the neck. Numeric alpha thresholds do not prove the silhouette looks correct at app scale.

Acceptance requires checkerboard + exact HotFox background + actual emulator screens 01/02/05/07/09. If any contour damage is visible, repair the mask; do not regenerate a different fox.

## P1 — AUTO / Ready vectors require visual proof

The new no-orbit vectors are conceptually correct, but they were never rendered on-device. Do not mark them accepted based on XML or prose. Capture 03/04 before additional redesign.

## P1 — Reference-state fixtures must match board density, not arbitrary demo data

Debug fixture values are allowed, but the number/order/visual density of rows must be derived from the approved board. Do not invent six servers or eight apps merely to fill space unless that matches the reference-state composition.

## Exit gate

V5.1 can proceed to visual refinement only after:

- fixture-specific code/data removed from `src/main`;
- strengthened leak check passes;
- debug build/tests/static gates pass;
- a fresh capture environment produces at least a smoke set for 01, 03, 04, 05, 10, 12, 15, 17;
- then 18/18 V5.1 screenshots and comparisons are produced.

`RELEASE READY` remains **NO**. Do not merge.