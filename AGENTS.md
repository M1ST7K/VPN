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

The supplied HotFox UI reference is the visual source of truth. Do not redesign it into generic Material VPN UI. Preserve editorial minimalism, charcoal background, warm cream typography, restrained orange accent, thin separators, generous negative space, precision alignment, connection visualization, real traffic counters, the server-list treatment and the Premium screen treatment.

**Phone navigation (owner requirement, do not regress):** the three primary destinations — Соединение / Серверы / Подписка — use a compact editorial **bottom bar** (`hotfox_bottom_nav`). Do not restore a side rail, drawer, or a second competing navigation system as the phone IA. The leftover Material `BottomNavigationView` and `NavigationView` stay gone and unwired. The settings gear on the toolbar is not a fourth destination.

**Авто-выбор сервера** must remain the **first row** of the server list (`HotfoxServerListContract.AUTO_ROW_INDEX = 0`), above every real server. AUTO is a persisted mode distinct from a manual GUID. Adapter positions for real servers are `dataIndex + 1`. Do not drop, hide, or sort AUTO below imported servers.

Do not regress the real-device UI fixes: no accidental square placeholder/boxed surfaces, no black-on-black icons, cream icon contrast on charcoal, visible state-aware route bars, a useful (not empty, not fake) connection screen, editorial server rows rather than legacy v2rayNG cards, and subscription expiry rendered when the source actually supplies it.

Every important interaction must have truthful idle, pressed, loading, connecting, connected, disconnecting, reconnecting, degraded, error and disabled states where applicable.

## Engineering quality

Never fake ping, bandwidth, connection success, traffic counters, server availability or subscription validity. Avoid placeholders and TODO implementations in release-critical code. Avoid mock networking in production source sets.

## Security

Never commit or expose signing private keys, keystore passwords, API secrets, private tokens, personal subscription URLs, user credentials or production secrets. Never log complete VLESS URLs, UUIDs, tokens or credentials. Redact sensitive values in diagnostics. Do not weaken TLS, Reality or certificate validation merely to make tests pass.

## Mandatory completion loop

A successful source edit without a successful build is not completion. Before stopping:

1. Reconstruct with `bash bootstrap/bootstrap_source.sh` if `V2rayNG/` is missing, then `rsync -a bootstrap/hotfox_2_2_0/ V2rayNG/` after overlay edits.
2. Run, from `V2rayNG/` with `ANDROID_HOME` set:
   - `./gradlew --no-daemon --stacktrace :app:assemblePlaystoreDebug`
   - `./gradlew --no-daemon --stacktrace :app:testPlaystoreDebugUnitTest`
   - `./gradlew --no-daemon --stacktrace :app:lintPlaystoreDebug`
   - `./gradlew --no-daemon --stacktrace :app:assemblePlaystoreRelease`
3. Run `python3 verification/static_check_2_2_0.py` and `bash bootstrap/verify_hotfox_2_2_0.sh`.
4. Inspect GitHub CI yourself (`gh run list` / failed logs). Do not wait for the owner to click Actions. If CI is red, fix, push, and repeat.
5. Emulator UI smoke (if present) is install/navigation only. It is **not** VPN E2E. Physical-device E2E remains **NOT EXECUTED** until a real device proves external IP change and DNS/IPv6 leak behavior.

Trusted OpenAI reviewer P0/P1 findings remain blockers. Do not suppress meaningful checks to get green.

## Android emulator harness — one-time capability proof

Set up a reusable Android emulator smoke-test harness for Cursor/CI when the environment permits it. Prefer repository-controlled scripts/configuration (for example `.cursor/environment.json` plus a deterministic emulator/ADB smoke script) rather than manual GUI setup.

The emulator capability itself must be **proved only once** for this project/environment. On the first successful setup, capture enough evidence to show the harness is genuinely usable:

- `adb devices` shows a booted emulator;
- the current HotFox debug APK installs successfully;
- `com.hotfox.vpn` launches successfully;
- the smoke flow can visit Соединение / Серверы / Подписка and confirm AUTO is the first server row;
- capture at least one screenshot and relevant crash-free `logcat` evidence;
- record the successful one-time proof in a durable text report or PR comment and, where practical, expose screenshot/logcat as an artifact.

**Do not repeat this capability-proof bundle on every future commit.** Once a successful proof exists and the emulator harness/environment has not materially changed, future agents should simply reuse the harness. Run ordinary smoke checks when relevant to UI/runtime changes and report only PASS/FAIL plus actionable failures; do not regenerate the full proof package and do not ask the owner to reconfirm emulator capability each time.

Repeat the full capability proof only if the emulator harness, Android image, Cursor environment, or execution architecture materially changes, or if the previously working harness stops functioning.

Emulator success is never a substitute for physical-device VPN E2E. Do not claim external-IP/DNS/IPv6 acceptance from emulator-only evidence.

## Release discipline

Before declaring the task complete, run an appropriate clean build, applicable unit tests, lint/static analysis, the project verifier, secret scans and a final diff review. If the environment does not permit real-device testing, explicitly mark device-level checks as NOT EXECUTED. Never report a device test as PASS unless it actually ran successfully.

## Final report

Report changed architecture, files changed, bugs fixed, UI work completed, VPN/routing work completed, tests executed, exact build commands, build result, unresolved issues, remaining physical-device tests, security review result and release-readiness assessment.

Until the applicable requirements are satisfied, continue working.
