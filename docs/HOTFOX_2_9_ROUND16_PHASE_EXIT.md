# HotFox 2.9 — round 16 phase-exit checkpoint

This checkpoint exists only to request the next canonical phase-exit review after the round-16 fix.

- Round-16 blocking finding: IPv6/NAT64 traffic could escape the intended fail-closed TUN capture when LAN bypass used partial IPv6 routes.
- Fix commit: `f8de16e8fb2fd5e41104c934bc3f8a60fed90488`.
- Fix behavior: IPv6 Internet capture remains `::/0`; LAN bypass is IPv4-only at the TUN layer; NAT64 remains captured; regression/static coverage was added.
- Exact fix push CI: `34453476204` — SUCCESS (reconstruction/build, tests, lint/release gates as configured).
- Runtime emulator VPN E2E: `NOT EXECUTED / deferred` under the owner validation-timing override.
- Physical Android VPN E2E: `NOT EXECUTED / deferred` until `FINAL RELEASE VALIDATION GATE`.

If approved, this checkpoint closed 2.9 as:

`2.9 ENGINEERING COMPLETE — runtime and physical release validation deferred.`

Round 17 `APPROVED` (P0=0, P1=0) on SHA `45242077fc1e286901b712b31cf7df4915b620f6`. Current phase is `3.0 Premium Android Experience`.
