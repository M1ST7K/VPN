# HotFox Proxy

Production Android VPN client repository for **HotFox Proxy**.

This repository is prepared for Cursor Cloud/Background Agents. The current implementation baseline is HotFox Proxy 2.1.0; the target release described by the master specification is 2.2.0.

## Cursor first run

1. Read `AGENTS.md` completely.
2. Run `bash bootstrap/bootstrap_source.sh`.
3. Read `docs/CURSOR_PRO_ULTRA_MASTER_PROMPT_HOTFOX_PROXY_2.2.0.txt` completely.
4. Inspect `docs/HOTFOX_UI_REFERENCE.jpeg`.
5. Work directly on the reconstructed Android project under `V2rayNG/`.
6. Build, test, fix failures, and continue until the applicable acceptance criteria in the master specification are satisfied.

The bootstrap script reconstructs the HotFox 2.1.0 Android project from versioned source payload chunks, verifies the source SHA-256, and downloads the official `libv2ray.aar` v26.6.27 from the upstream AndroidLibXrayLite release with SHA-256 verification.

## Security

Never commit signing keys, passwords, personal subscription URLs, credentials, tokens, `.env` files, `local.properties`, keystores, or production secrets.

The repository intentionally does not contain the HotFox release private signing key.
