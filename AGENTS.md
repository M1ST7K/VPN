# HOTFOX PROXY — MANDATORY AGENT RULES

You are working on the production Android VPN application **HotFox Proxy** (`com.hotfox.vpn`, Android API 24+).

This repository is not a demo. Do not replace the current app with a prototype, web app, mock VPN, simplified tunnel, or a ground-up rewrite.

## Read order — keep context small

Before editing production code:

1. Read this file completely.
2. Read `docs/CURRENT_PHASE.md` completely.
3. Read the phase document linked from `docs/CURRENT_PHASE.md`.
4. Inspect only the relevant implementation/tests/reports for the task.
5. Read `docs/HOTFOX_MASTER_ROADMAP.md` only when you need cross-phase/product context.
6. Read the older full Ultra Master Prompt only when `CURRENT_PHASE.md` or a concrete task explicitly requires a historical requirement from it.

Do **not** reread the entire giant product roadmap for every small change.

## Core product truth

HotFox must be a real Android VPN. The intended protected path remains conceptually:

`Android apps -> Android VpnService/TUN -> HEV/tun2socks -> local SOCKS -> Xray -> supported VPN outbound -> server -> Internet`

The UI may show `CONNECTED` / `Защищено` only when the canonical current VPN session has passed the strongest bounded path verification implemented for the production path.

A VPN icon, established TUN, running Xray, listening SOCKS port, or HEV stats array alone is not proof of protection.

Physical-device acceptance must be capable of proving:

`external IP before VPN != external IP after successful VPN connection`

plus real browser/app traffic and acceptable DNS/IPv6 behavior.

## Canonical state and lifecycle

- `VpnSessionCoordinator` (or its deliberate successor) is the canonical protection-state authority.
- Stale async work must never overwrite a newer generation.
- Startup, reconnect/handover and teardown must be serialized/owned explicitly.
- Repeated start/stop actions must be idempotent.
- Permission revocation, service destruction, process recreation and network handover must not leave stale protected state or live obsolete resources.
- Fail closed where the configured protection policy requires it.

## AUTO server contract

`Авто-выбор сервера` is a first-class persisted mode and must remain the **first server-list row** (`HotfoxServerListContract.AUTO_ROW_INDEX = 0`).

- AUTO is not a fake server GUID.
- Manual selection must remain manual until the user changes it.
- AUTO may resolve to a concrete healthy server for a session.
- The connection screen must truthfully show the resolved AUTO target.
- Never invent ping, health, load, region or availability.

## Phone UI contract

Primary phone navigation is exactly the four editorial bottom destinations (bottom bar):

`Главная / Серверы / Подписка / Настройки`

This 4-destination chrome is locked by the approved 18-screen visual references (screen 05 is the style anchor). Do not restore a 3-tab bar, the old side rail/drawer, or a competing second primary navigation system.

Preserve the HotFox direction: charcoal/purple-black canvas, warm cream typography, restrained orange accent, green only for real success, thin separators, clean contemporary server rows, visible state-aware route bars, no black-on-black icons and no square/missing-glyph placeholders.

Do not trade VPN correctness for visual polish.

## Subscription and future commerce

Runtime subscription data stays runtime-driven. Never embed a private user subscription URL/token in the APK or repository.

Future HotFox purchase/entitlement work is defined in later phase docs. When implemented:

- payment provider secrets remain backend-only;
- app-side browser redirects never prove payment;
- backend verification/webhook/reconciliation is authoritative;
- HotFox-managed access and external/manual subscriptions remain supported paths;
- bearer entitlement/subscription secrets use Keystore-backed storage.

Do not implement future billing/product milestones while `docs/CURRENT_PHASE.md` says they are out of scope.

## Security

Never commit or expose:
- API keys;
- signing private keys/keystores/passwords;
- private UUIDs/tokens/passwords;
- personal subscription URLs;
- payment-provider secrets.

Never log complete VLESS links, credentials, bearer tokens or full subscription URLs. Redact diagnostics. Do not weaken TLS/Reality/certificate validation to get green.

## Engineering quality

Never fake:
- ping;
- bandwidth/traffic;
- connection success;
- server health/load;
- subscription validity/expiry;
- payment success;
- device E2E evidence.

Avoid production TODOs/mocks in release-critical paths. Prefer bounded structured concurrency over sleeps, busy waits, detached scopes or global mutable flags.

## Build/test loop

After relevant implementation changes, run the applicable project gates yourself. Do not ask the owner to click Build.

Typical 2.2 Android gates:

- reconstruct/apply overlay when needed;
- `:app:assemblePlaystoreDebug`;
- `:app:testPlaystoreDebugUnitTest`;
- `:app:lintPlaystoreDebug`;
- `:app:assemblePlaystoreRelease` (unsigned compile gate);
- `python3 verification/static_check_2_2_0.py`;
- `bash bootstrap/verify_hotfox_2_2_0.sh`;
- relevant secret/static checks;
- inspect GitHub CI.

Emulator smoke proves install/navigation only. It is **not** VPN E2E.

## AI checkpoint review policy

Ordinary fix/build commits should rely on GitHub CI and focused tests. They must **not** spend OpenAI review budget automatically.

Request the trusted GPT-5.6 Sol checkpoint review only after a coherent task block is ready and applicable CI/tests are green by making the **final checkpoint commit message contain**:

`[hotfox-review]`

If a trusted checkpoint review reports substantiated P0/P1 findings:

1. fix all of them;
2. use ordinary commits while iterating/repairing CI;
3. do not request a new AI review on every fix commit;
4. once the whole fix block is green, make one final checkpoint commit containing `[hotfox-review]`.

P2-only feedback is not a blocker and must not create an autonomous fix loop.

## Completion/reporting

Do not claim a task complete because source compiles. Report what changed, tests/builds actually executed, unresolved blockers, physical-device checks still NOT EXECUTED, and release-readiness truthfully.

Until the current phase exit gate is satisfied, prioritize that phase instead of adding unrelated future features.
