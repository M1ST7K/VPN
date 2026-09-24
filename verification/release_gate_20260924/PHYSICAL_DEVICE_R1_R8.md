# Physical device acceptance R1–R8

No authorized physical Android device is attached to the Cursor cloud VM, and the
owner Mac preflight found none. Every R-item is therefore **NOT EXECUTED** and
the physical gate is **BLOCKED**. Physical execution also requires the signed RC
(`SIGNING.md`, BLOCKED).

| Item | Status | Executable procedure when a device is available |
|---|---|---|
| R1 Install/update | NOT EXECUTED | `adb install` exact signed RC; if a prior signed build exists, install it first then `adb install -r` RC; launch; grant VPN consent; confirm foreground notification while connected. |
| R2 Traffic proof | NOT EXECUTED | Browser `https://api.ipify.org` before / during / after; app traffic (e.g. Play Store/YouTube); record redacted IPs; disconnect → direct IP restored; `Защищено` only after path verification. |
| R3 DNS leak | NOT EXECUTED | Browser DNS-leak test while connected; resolvers must belong to the VPN path, never ISP resolvers. |
| R4 IPv6 leak | NOT EXECUTED | On an IPv6-capable Wi-Fi/cellular network: `https://api6.ipify.org` while connected must fail or show VPN egress, never native v6. |
| R5 Handoff | NOT EXECUTED | Wi-Fi→cellular, cellular→Wi-Fi, airplane on/off; bounded recovery; single session; no stale protected state. |
| R6 AUTO / Shadow | NOT EXECUTED | AUTO pick with real subscription; block current server (firewall/remove) to observe failover; one Shadow/alternate recovery where infra permits. |
| R7 Commerce | NOT EXECUTED | Only if HotFox-managed purchase ships: plan → order → hosted checkout → backend-authoritative verification → entitlement → restore/sync. Manual HTTPS subscription must work independently. |
| R8 Lifecycle | NOT EXECUTED | Background/foreground, `am kill` process recreation, reboot + restore policy, rapid connect/disconnect ×10, VPN permission revoke/deny, notification actions, Quick Settings tile. |

Record evidence per item under `verification/release_gate_20260924/physical/` with
device model, Android version, RC SHA-256, and redacted IPs only.
