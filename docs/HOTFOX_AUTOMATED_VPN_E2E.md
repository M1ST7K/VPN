# Automated VPN E2E (future, secret-driven)

This is a **design** for a later GitHub Actions / Cloud Agent job. It is **not** physical-device acceptance and must never be labeled as such.

## What this job is

A dedicated workflow that, when a **test-only** subscription URL exists as a repository secret, can later:

1. Reconstruct the app and install a debug APK on an emulator or instrumented device.
2. Import `HOTFOX_TEST_SUBSCRIPTION_URL` (secret; never committed, never logged in full).
3. Connect through the production path.
4. Compare external IP before vs after, plus DNS/IPv6 leak probes.

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

## Enablement

The workflow `.github/workflows/hotfox-vpn-e2e.yml` is **manual** (`workflow_dispatch`) and exits `NOT EXECUTED` unless the secret is present **and** an implementation is wired. Default is no network VPN connect.

Physical-device E2E remains **NOT EXECUTED** until a real Android device proves IP change and leak behavior.
