# CURRENT OWNER TASK — SCREENS 01–03 / EXISTING ASSETS / 2026-09-14

Status: **ACTIVE — implement the newly selected Splash, Connect HotFox and AUTO onboarding screens.**

Execution prompt: `design/HOTFOX_CURSOR_SCREENS_01_03_EXISTING_ASSETS.txt`
Scoped rule: `.cursor/rules/34-hotfox-onboarding-01-03-existing-assets.mdc`
Latest visual: `design/hotfox_onboarding_01_03_20260914/reference_triptych.png`
Integrity/provenance: `design/hotfox_onboarding_01_03_20260914/REFERENCE_LOCK.json`

The owner explicitly corrected the numbering: **01 Splash, 02 Connect HotFox, 03 AUTO from the 18-screen collection**. These are not the three V13 Home states. Implement this bounded task now in the existing Views/ViewBinding bootstrap overlay, using existing runtime artwork/resources. The new image is a design reference only and must never become a runtime screenshot background.

This task updates composition and interaction wiring for 01–03, with no new artwork generation. A restrained globe/orbit illustration is specifically authorized on onboarding screen 03 by the new selected image; older blanket orbit bans do not override this local choice. Do not introduce fox rings or spread this exception to other screens.

V13 remains the visual source for Home 05/06/07. Preserve its implementation and later legitimate fixes. Other screens retain their requirements; adapt only necessary existing onboarding/import/server-picker navigation. Preserve VPN/security/entitlement truth, required engineering/review gates and the runtime-repair ledger below.

Continue in PR #10 on `cursor/hotfox-ui-pixel-lock-rebuild`. Build, exercise real actions, capture all three production layouts using debug-only capture support where needed, compare, fix and provide commit-bound APK/evidence. No merge or release claim. Do not stop after a plan or answer with old Home screenshots.

---

# INHERITED HOME CONTRACT — V13 (not the current 01–03 assignment)

The following historical task text is retained for the unchanged Home contract. Its active-task wording is historical; the current work scope is the 01–03 task above.

# CURRENT OWNER TASK — HOTFOX V13 APPROVED HOME IMPLEMENTATION

Status: **ACTIVE — owner approved three new Home concepts and requested committing their PNG assets and starting Cursor implementation to match them 1:1.**

Active execution prompt: `design/HOTFOX_CURSOR_V13_APPROVED_HOME_EXECUTION.md`

Active rule: `.cursor/rules/33-hotfox-v13-approved-home.mdc`

Latest visual references: `design/hotfox_home_v13/design/references/`, all three v2 PNGs, 853×1844. Checksums: `design/hotfox_home_v13/REFERENCE_LOCK.json`.

For Home Disconnected / Connecting / Connected visuals, **V13 supersedes conflicting V12 and earlier geometry/art/layout instructions**. This is the new owner-authorized design, not a return to the old 05/06/07 art. Preserve measured non-overlapping layout and all runtime/security/accessibility contracts. Other screens retain their existing requirements.

The full asset kit is under `design/hotfox_home_v13/`; 22 runtime PNGs are also in the versioned bootstrap overlay. The actual Home uses Views/ViewBinding; the included Compose example is a visual reference, not authorization to migrate the framework or use absolute-positioned production controls.

Start implementation on this PR branch. Build, capture all three real UI states, compare to the approved references, fix discrepancies and report evidence. Current asset-package checks are not an APK build or visual acceptance. Runtime/physical release gates remain open as documented below.

---

# HISTORICAL HOME TASK — V12

The following V12 task is superseded by V13 for conflicting Home visuals. Its no-overlap and measured-layout principles remain binding. Its older status labels and geometry are historical, not the current task.

# CURRENT OWNER TASK — HOTFOX V12 NO-OVERLAP HOME REBUILD

Status: **ACTIVE — owner rejected the latest Home composition because functional layers still overlap. Rebuild the Home layout architecture now.**

Active execution prompt:

`design/HOTFOX_CURSOR_V12_NO_OVERLAP_LAYOUT_REBUILD.md`

Active mandatory rule:

`.cursor/rules/32-hotfox-v12-no-overlap-home.mdc`

## Priority / conflict rule

For Home-screen UI architecture, positioning, hero geometry, state-panel layout, quick-settings placement, footer placement, and bottom-navigation placement:

**V12 > every older HotFox Home/hero/layout prompt or Cursor rule wherever they conflict.**

Older V8/V9/V10/V11 screenshot-specific geometry and per-state offset experiments are historical only. Do not reapply them.

## Owner requirement

The defect is architectural: Disconnected / Connecting / Connected currently use independent visual layers that repeatedly collide. Do not patch another `offset`. Build one shared measured scaffold with non-overlapping regions. Only fox/planet artwork may layer, and only inside a bounded HeroSlot. State content belongs to a stable StatePanelSlot. Quick settings and bottom nav must remain in normal layout flow and at stable positions across all three states.

## Completion proof required

Do not report completion on compile alone. Before stopping:

1. build the app;
2. run the available emulator if infrastructure exists;
3. capture fresh Disconnected / Connecting / Connected screenshots at the same resolution;
4. compare them side-by-side;
5. continue fixing if any functional regions overlap, hero art bleeds into controls/nav, row text collides, or major slot positions jump between states;
6. report changed files, build result, screenshot paths, and invariant verification.

## Technical boundary

This V12 assignment is UI-only. Preserve the runtime-repair baseline and all VPN/network/security/business guarantees. Do not rewrite transport, routing, DNS, entitlement, subscription, server-selection, or connection-truth logic to satisfy screenshots. Do not claim RELEASE READY from UI evidence.

---

# PRIOR OWNER TASK — 18-SCREEN UI RECONSTRUCTION

Status: **SUPERSEDED FOR HOME LAYOUT GEOMETRY BY V12; retained as broader visual/reference context.**

The owner explicitly requested rebuilding the Android UX/UI from the 18 supplied
images while preserving every technical guarantee. This UI assignment may start now
on `cursor/hotfox-ui-pixel-lock-rebuild`; it is not a new roadmap product phase.

The older broad prompt `design/HOTFOX_CURSOR_FULL_UI_REBUILD_PIXEL_LOCK_PROMPT.txt`,
`.cursor/rules/23-hotfox-ui-pixel-lock.mdc` and original images under
`design/hotfox_18_final_style_reference/` remain reference material, but they MUST NOT
override V12 Home architecture or reintroduce overlapping/offset-based Home geometry.
Preserve runtime repair baseline `a729b1cbce719278840caf9ab09a5350e6d554e4`.
Do not rewrite core/transport/state/security behavior for visual matching.

The runtime-repair ledger below is retained as the prior technical status; this
handoff does not close it, claim VPN E2E, or authorize RELEASE READY. UI fixture
screenshots and zero-diff comparison do not replace runtime/device acceptance.

---

# CURRENT PHASE — RUNTIME REPAIR GATE (owner opened)

Status: **ROUND 32 P0/P1 FIXES — phase-exit candidate; host CI/APK evidence must be this HEAD; emulator/physical still NOT EXECUTED**

The owner explicitly opened `docs/HOTFOX_RUNTIME_REPAIR_EXECUTION_PROMPT.md` from HEAD `d723f65`.
That supersedes the previous no-op / deferred-runtime instruction **for this engineering/runtime repair only**.

3.1 remains **ENGINEERING COMPLETE — runtime and physical release validation deferred** as a product-phase ledger fact (round 25 `APPROVED` on `7192b04`; ledger confirmed by round 26 on `23e547a`). This gate does **not** start a new roadmap product phase and does **not** claim `RELEASE READY`.

Linked prompt: `docs/HOTFOX_RUNTIME_REPAIR_EXECUTION_PROMPT.md`
Status ledger: `docs/HOTFOX_RUNTIME_REPAIR_STATUS.md`
Previous phase spec: `docs/phases/3.1-mature-platform.md`
Final device suite (still required before RELEASE READY): `docs/phases/final-release-validation-gate.md`
Owner validation override: `.cursor/rules/22-hotfox-owner-release-validation-gate.mdc` (timing of *release* READY; this repair is an explicit owner exception for implementation/runtime evidence)

`docs/AI_REVIEW_PHASE_ID` on this branch remains `2.4` to match trusted `main`.

## Goal

Fix the real product defects that make VPN appear connected while traffic/public IP do not change:

1. P0 — native Xray sockets must not re-enter TUN;
2. P1 — `bindProcessToNetwork` false is failure;
3. P1 — CUSTOM/POLICYGROUP/PROXYCHAIN are not compared as network protocols;
4. P1 — one canonical routing snapshot and real outbound/balancer tags;
5. SHA-tied debug APK + host CI gates;
6. feasible emulator evidence when infrastructure exists;
7. iterate review until P0=0/P1=0 **and** do not stop on compile-only green if runtime evidence is still missing.

Never print or commit subscription URLs, credentials, API keys, UUIDs, or private keys.

## Inherited guarantees (still binding)

- truthful `VpnService`/TUN → HEV → SOCKS `127.0.0.1:10808` → Xray;
- `Защищено` only after path verification;
- DNS / IPv6 `::/0` fail-closed;
- AUTO persisted; manual sticky;
- backend-authoritative entitlement;
- no secrets in APK/logs;
- 2.9 SOCKS/TUN harnesses kept.

## Work allowed now

All phases in the runtime-repair prompt, including reconstruction, P0/P1 implementation, tests, CI, SHA-tied APK, and review/fix loop.

## Not now

- claiming `RELEASE READY` without the physical-device suite actually running;
- fake CONNECTED / fake ping / TLS bypass;
- exposing secrets;
- changing `docs/AI_REVIEW_PHASE_ID` on this feature branch.

