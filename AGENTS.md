# HOTFOX PROXY — MANDATORY AGENT INSTRUCTIONS

You are working on the production Android VPN application **HotFox Proxy**.

The repository is not a demo. Do not replace it with a prototype, web app, mockup, or simplified VPN implementation.

## Mandatory first actions

Before editing production code:

1. Read this file completely.
2. Run `bash bootstrap/bootstrap_source.sh` if the `V2rayNG/` project is not already present.
3. Read `docs/CURSOR_PRO_ULTRA_MASTER_PROMPT_HOTFOX_PROXY_2.2.0.txt` completely.
4. Inspect `docs/HOTFOX_UI_REFERENCE.jpeg`.
5. Inspect the existing Android/VPN implementation, verification reports, security notes, build configuration and Git status.
6. Continue from the existing HotFox 2.1.0 implementation; do not rebuild the product from scratch.

The master specification is normative. Do not skim it or silently replace requirements with your own interpretation.

## Execution mode

Do not merely produce a plan. Work directly on the repository. Inspect code, modify code, create required files, build, run tests, inspect failures, fix failures, rerun checks, review the diff and leave the repository in a coherent buildable state.

Do not claim completion merely because the source compiles.

## Core product requirement

HotFox Proxy must be a real Android VPN. The production networking path must result in real Android application traffic being routed through the VPN transport. Conceptually:

Android applications -> Android VpnService -> TUN -> HEV/tun2socks -> local SOCKS -> Xray -> VLESS/supported transport -> HotFox server -> Internet.

The UI may show `Защищено` / connected only when the production tunnel path is actually operational. A VPN notification, started Xray process, open local SOCKS port, or established TUN interface alone is not sufficient proof of a successful end-to-end VPN connection.

## Non-negotiable acceptance condition

When physical-device E2E is executed, the implementation must be capable of satisfying:

`external IP before VPN != external IP after successful VPN connection`

and the post-connect IP must correspond to the selected VPN egress. Traffic must not silently bypass the VPN. DNS leakage, IPv6 leakage, routing bypass, false connected states and VPN feedback loops are release-blocking defects where the configured protection policy requires fail-closed behavior.

## UI requirement

The supplied HotFox UI reference is the visual source of truth. Do not redesign it into generic Material VPN UI. Preserve editorial minimalism, charcoal background, warm cream typography, restrained orange accent, thin separators, vertical navigation, generous negative space, precision alignment, connection visualization, real traffic counters, the server-list treatment and the Premium screen treatment.

Every important interaction must have truthful idle, pressed, loading, connecting, connected, disconnecting, reconnecting, degraded, error and disabled states where applicable.

## Engineering quality

Never fake ping, bandwidth, connection success, traffic counters, server availability or subscription validity. Avoid placeholders and TODO implementations in release-critical code. Avoid mock networking in production source sets.

## Security

Never commit or expose signing private keys, keystore passwords, API secrets, private tokens, personal subscription URLs, user credentials or production secrets. Never log complete VLESS URLs, UUIDs, tokens or credentials. Redact sensitive values in diagnostics. Do not weaken TLS, Reality or certificate validation merely to make tests pass.

## Release discipline

Before declaring the task complete, run an appropriate clean build, applicable unit tests, lint/static analysis, the project verifier, secret scans and a final diff review. If the environment does not permit real-device testing, explicitly mark device-level checks as NOT EXECUTED. Never report a device test as PASS unless it actually ran successfully.

## Final report

Report changed architecture, files changed, bugs fixed, UI work completed, VPN/routing work completed, tests executed, exact build commands, build result, unresolved issues, remaining physical-device tests, security review result and release-readiness assessment.

Until the applicable requirements are satisfied, continue working.
