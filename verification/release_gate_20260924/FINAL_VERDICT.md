# FINAL VERDICT — NOT RELEASE READY

Candidate: `1a15511abff8e5f06bcc1706f77145708216c3e4` (app source == `2c3df36`; CI artifact universal `ec4b847b…43b0`; see `CANDIDATE.md`).

## FAILED (and fixed, rerun pending)

- Emulator engineering E2E run 1 on `08b35ff`: `socks-only-https` — Xray rejected
  config because geo assets were never installed for proxy-only/root starts on a
  fresh install. Fixed in `2c3df36`; runtime rerun **BLOCKED** (below).

## BLOCKED

| Gate | Blocker | Owner / infra action |
|---|---|---|
| SIGNED_RC | No authorized signing identity in any available environment; no CI signing path | Provide existing production keystore securely; build `playstoreRelease` with `HOTFOX_REQUIRE_RELEASE_SIGNING=true`; `apksigner verify` |
| VPN_E2E_GITHUB | Workflow never dispatched; agent cannot dispatch; test-secret presence unknown | Ensure dedicated `HOTFOX_TEST_SUBSCRIPTION_URL` secret; dispatch `hotfox-vpn-e2e.yml` on this branch |
| EMULATOR_E2E (current candidate) | Cloud VM nested KVM kernel BUG (`kvm_spurious_fault`) — emulator cannot boot | Rerun on GitHub runner (above) or owner Mac AVD with runtime test subscription |
| PHYSICAL_R1_R8 | No authorized physical device; no signed RC | Attach device; execute `PHYSICAL_DEVICE_R1_R8.md` |

## NOT EXECUTED

DNS leak, IPv6 leak, handoff, AUTO/Shadow, commerce, lifecycle/reboot/QS, full UI
smoke, public IP before/during/after on the current candidate.

## PASS (repository/CI level only)

See `CI.md` and `gate_status.json`. Engineering CI passing is **not** runtime or
release proof.

`RELEASE READY = NO`.
