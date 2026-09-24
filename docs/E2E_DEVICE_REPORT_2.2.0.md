# HotFox Proxy 2.2.0 — physical device E2E

Environment: cloud agent without an attached Android device or customer subscription.

**Result: NOT EXECUTED**

Do not treat compilation, APK packaging, VPN icon presence, Xray start, or HEV start as E2E proof.

## Required device gates

| Gate | Status |
|---|---|
| External IP before VPN recorded | NOT EXECUTED |
| Connect HotFox | NOT EXECUTED |
| External IP after VPN recorded | NOT EXECUTED |
| `IP before != IP after` and matches selected egress | NOT EXECUTED |
| Browser HTTPS traffic | NOT EXECUTED |
| Telegram or another real app | NOT EXECUTED |
| DNS resolution through VPN | NOT EXECUTED |
| UDP where supported | NOT EXECUTED |
| IPv4 | NOT EXECUTED |
| IPv6 proxy or fail-closed | NOT EXECUTED |
| DNS leak check | NOT EXECUTED |
| IPv6 leak check | NOT EXECUTED |
| Disconnect | NOT EXECUTED |
| Reconnect | NOT EXECUTED |
| Rapid connect/disconnect | NOT EXECUTED |
| Wi-Fi → cellular | NOT EXECUTED |
| Cellular → Wi-Fi | NOT EXECUTED |
| Network loss / restore | NOT EXECUTED |
| Screen lock | NOT EXECUTED |
| Background operation | NOT EXECUTED |
| Service restart | NOT EXECUTED |
| Multiple connection cycles | NOT EXECUTED |

PASS for the IP gate requires a real device, a valid subscription, and `external IP before != external IP after` corresponding to the selected server egress.
