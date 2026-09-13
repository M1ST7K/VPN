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
