# CURRENT PHASE — FINAL RELEASE VALIDATION GATE

Status: **ACTIVE — engineering implementation and final UI/Onest integration are complete; runtime and physical release validation remain open.**

Canonical gate:

`docs/phases/final-release-validation-gate.md`

Canonical roadmap:

`docs/HOTFOX_ROADMAP.md` → `FINAL RELEASE DEVICE GATE`

Runtime repair ledger:

`docs/HOTFOX_RUNTIME_REPAIR_STATUS.md`

Latest UI/typography evidence:

`verification/ui/typography_onest_pass4_20260923/VALIDATION.md`

Known untested UI/runtime gaps:

`verification/ui/typography_onest_pass4_20260923/UNTESTED_GAPS.md`

## Current truth

- Final UI composition and Onest typography pass are integrated in `main`.
- Completed historical UI rules 23–37 are archived with `alwaysApply: false`; they are regression context only.
- VPN/core/runtime engineering phases through 3.1 are engineering-complete.
- Merge-main CI for the integrated HotFox branch passed reconstruction, static checks, debug build, unit tests, lint, unsigned release compile and publish.
- Emulator UI smoke is not VPN E2E.
- Real production VPN E2E is **NOT YET PROVEN** on this integrated release candidate.
- Physical Android device validation is **NOT YET EXECUTED**.
- Signed release-candidate install/update and full R1–R8 device acceptance are **NOT YET COMPLETE**.
- `RELEASE READY` is **NOT CLAIMED**.

## Active objective

Prove the shipping HotFox product instead of adding new features or redesigning UI.

Required validation sequence:

1. Produce/identify one immutable release-candidate SHA and artifact.
2. Verify signing/release provenance without exposing signing secrets.
3. Run emulator/runtime VPN E2E where infrastructure permits:
   - TUN → HEV → SOCKS → Xray → server → Internet;
   - real HTTPS traffic;
   - before/during/after public IP when environment permits;
   - DNS behavior;
   - IPv6/fail-closed behavior;
   - disconnect/reconnect and no false protected state.
4. Run physical-device R1–R8 from the canonical roadmap:
   - install/update/permissions/foreground service;
   - real VPN traffic proof;
   - DNS leak;
   - IPv6 leak;
   - Wi-Fi/cellular handoff;
   - AUTO/Shadow recovery;
   - commercial flow if included in the release;
   - lifecycle/background/reboot/QS/notification behavior.
5. Fix any blocking findings on the same truthful architecture.
6. Rebuild and repeat failed gates.
7. Claim `RELEASE READY` only after required emulator + physical acceptance actually pass.

## Non-goals

Do not:
- redesign the approved UI;
- move the approved fox/hero geometry;
- reopen completed roadmap phases without an actual regression;
- fake CONNECTED/protection/payment/server-health evidence;
- weaken DNS/IPv6/path verification to obtain a pass;
- commit or print subscription URLs, credentials, signing material, API keys, UUID secrets or private keys;
- treat a VPN icon, TUN establishment, SOCKS listen socket, Xray process, emulator screenshot or successful compile as VPN E2E proof.

## Required reporting

Every validation handoff must distinguish:
- PASS;
- FAIL;
- BLOCKED;
- NOT EXECUTED.

Evidence must be tied to the exact candidate SHA/artifact.

No missing runtime/device evidence may be converted into PASS.

