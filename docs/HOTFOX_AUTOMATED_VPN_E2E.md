# Automated VPN E2E (secret-driven, not physical)

This is the engineering-runtime VPN E2E job. It is **not** physical-device acceptance and must never be labeled as such.

## What this job is

A dedicated `workflow_dispatch` workflow that, when a **test-only** subscription URL exists as a repository secret:

1. Reconstructs the app and installs a debug APK on a GitHub-hosted emulator.
2. Imports `HOTFOX_TEST_SUBSCRIPTION_URL` (secret; never committed, never logged in full).
3. Connects through the production TUN → HEV → SOCKS → Xray path.
4. Records sanitized SOCKS/TUN evidence, IP before/during/after (last octet redacted), and three reconnect cycles.

Implementation:

- workflow: `.github/workflows/hotfox-vpn-e2e.yml`
- host script: `verification/emulator_vpn_e2e.sh`
- debug harness: `HotfoxEngineeringRuntimeE2e` / `HotfoxEngineeringE2eActivity`

If the secret is absent or `/dev/kvm` is missing, the job exits `NOT EXECUTED` and does not claim VPN proof.

## Secrets (never in git)

| Secret | Purpose |
|---|---|
| `HOTFOX_TEST_SUBSCRIPTION_URL` | Dedicated **test** subscription, not a personal/production URL |
| Optional later: `HOTFOX_TEST_EXPECTED_EGRESS_CIDR` | Optional expected egress check |

Do not reuse owner personal subscriptions. Do not print the URL, UUID, or token. Log only redacted length / host if needed.

## What this job is not

- Not Wi-Fi ↔ cellular handover (physical device only).
- Not owner UX acceptance (icons, AUTO row, expiry copy).
- Not a substitute for `external IP before != after` on a real phone.
- Not `RELEASE READY`.

## Enablement

Dispatch `.github/workflows/hotfox-vpn-e2e.yml` manually. Default is no network VPN connect unless the secret is present.

Physical-device E2E remains **NOT EXECUTED** until a real Android device proves IP change and leak behavior.
