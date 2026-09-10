# HotFox Proxy — Master Product Roadmap

This document is the high-level product/engineering map. It is intentionally much shorter than the historical giant execution prompt. Agents should not reread the giant prompt for every task.

## Source-of-truth order

When requirements conflict, use this order:

1. explicit current owner instruction;
2. `AGENTS.md` permanent safety/product rules;
3. `docs/CURRENT_PHASE.md` current scope;
4. the linked phase document in `docs/phases/`;
5. trusted reviewer guardrails/current-phase scope on `main`;
6. this master roadmap;
7. older Ultra Master Prompt / historical specs.

Newer code/evidence also beats stale documentation. Never revert a newer verified fix because an older prompt described an earlier state.

## Product vision

HotFox should become a modern Android privacy product that is simple on the surface and technically rigorous underneath.

The normal user journey should eventually be:

1. install HotFox;
2. buy HotFox access **or** add an external subscription;
3. leave `Авто-выбор сервера` enabled;
4. tap `Подключить`;
5. see a truthful `Защищено`;
6. stop thinking about Xray, HEV, DNS, routing and server health.

The expert journey remains available through advanced settings: manual servers, external configs, per-app routing, custom rules, diagnostics and protocol options.

## Permanent product principles

- Truth before cosmetics.
- AUTO is the default product path, manual selection is an explicit override.
- `Защищено` is earned by runtime evidence, never inferred from an icon/process.
- HotFox-managed purchase and external/manual subscriptions may coexist.
- Payment truth comes from a trusted backend/provider verification, never from an app redirect.
- Secrets are not UI/log data.
- Hosted checkout is preferred; do not handle card details in the APK.
- Last-known-good data is preserved on transient failures where safe.
- Android API 24 compatibility and `com.hotfox.vpn` remain unless deliberately migrated.
- No giant rewrite for architectural fashion.

# Release train

## 2.2 — Truthful Core

Purpose: make the current VPN trustworthy enough to release/test seriously.

Deliverables:
- generation-safe lifecycle;
- real bounded TUN datapath verification;
- correct Xray/HEV/TUN teardown;
- DNS/IPv6 fail-closed behavior;
- safe reconnect/handover;
- truthful UI state;
- AUTO server binding correctness;
- exact-sha APK provenance;
- physical-device E2E.

Detailed spec: `docs/phases/2.2-truthful-core.md`

## 2.3 — Commercial Foundation

Purpose: a new user can buy HotFox access without manually pasting a subscription URL, while external subscriptions remain supported.

Deliverables:
- plan catalog;
- HotFox backend API contract;
- order/idempotency state machine;
- provider-hosted checkout through Custom Tabs;
- verified webhooks/reconciliation;
- entitlement issuance/extension;
- secure credential provisioning;
- Keystore-backed SecretStore;
- purchase restore;
- renewal;
- promo/redeem support if actually needed;
- transactional subscription refresh;
- no-subscription purchase UX.

Detailed spec: `docs/phases/2.3-commercial-foundation.md`

## 2.4 — Smart Connection

Purpose: AUTO becomes a real reliability engine instead of a lowest-cached-ping selector.

Deliverables:
- ServerHealthRepository;
- bounded parallel probes;
- latency EWMA;
- jitter/failure/staleness penalties;
- TTL/freshness;
- health states;
- hysteresis;
- bounded automatic failover;
- reconnect/backoff policy;
- live ping progress;
- better server metadata/presentation.

Detailed spec: `docs/phases/2.4-smart-connection.md`

## 2.5 — Privacy Controls

Purpose: useful routing/privacy controls without exposing raw Xray complexity.

Deliverables:
- Smart / Global / Custom routing;
- per-app VPN/bypass;
- domain/IP rules with deterministic DIRECT/VPN/BLOCK precedence;
- DNS through VPN;
- optional DoH;
- IPv6 policy;
- LAN access toggle;
- optional maintained ad/tracker blocking;
- Always-on / Block-connections-without-VPN guidance.

Detailed spec: `docs/phases/2.5-privacy-controls.md`

## 2.6 — Premium Android Experience

Purpose: make HotFox feel complete and modern.

Deliverables:
- concise onboarding;
- complete loading/empty/error states;
- polished connection/server/subscription screens;
- accessibility/TalkBack;
- large font support;
- reduced motion;
- responsive/inset-safe layouts;
- screenshot regression;
- Quick Settings tile;
- notification actions;
- trusted-network/auto-connect options if stable;
- temporary pause;
- sideload update discovery.

Detailed spec: `docs/phases/2.6-premium-android.md`

## 2.7 — Operations

Purpose: operate a real distributed product safely.

Deliverables:
- stable/beta/dev channels;
- one authoritative mutable publisher;
- signed release workflow;
- checksums/provenance;
- update manifest integrity;
- SBOM/license artifacts;
- dependency/security scanning;
- safe diagnostics/support;
- optional privacy-safe crash reporting;
- backend health/reconciliation procedures;
- incident/status handling.

Detailed spec: `docs/phases/2.7-operations.md`

## 3.0 — Premium Android Experience

Purpose: make truthful HotFox behavior feel simple, deliberate and premium without hiding technical failure.

Closed as **ENGINEERING COMPLETE — runtime and physical release validation deferred** (round 19 `APPROVED`).

Detailed spec: `docs/phases/3.0-premium-android.md`

## 3.1 — Mature HotFox Platform / Pre-release Engineering

Purpose: remove remaining inherited-product friction after VPN correctness, commerce, AUTO, routing, Shadow, operations, Autopilot and premium UX are already stable.

Candidates implemented in-repository: engine/UI facade, signed control-plane metadata, capacity-aware AUTO, generated device registry, API versioning, offline policy, privacy-safe observability, 2.x migration.

Still deferred without evidence: dedicated VPN engine process split, R8/minification, optional account secret sync.

Detailed spec: `docs/phases/3.1-mature-platform.md`

# Target product surfaces

Primary bottom navigation remains:

`Соединение / Серверы / Подписка`

Secondary surfaces may include:
- onboarding;
- settings;
- routing;
- per-app rules;
- DNS/privacy;
- purchase/renew/restore;
- diagnostics;
- About/licenses/build info.

Android system surfaces may include:
- foreground notification;
- Quick Settings tile;
- app shortcuts;
- update/install handoff;
- Android Always-on settings guidance.

# Commercial product model

Two access origins must coexist:

### HotFox-managed access
Purchased via HotFox UI and verified backend entitlement.

### External/manual access
User imports an HTTPS subscription or compatible individual configuration.

No-subscription state should eventually show plans and actions such as:
- `Купить HotFox`;
- `У меня уже есть подписка`;
- `Ввести промокод` if enabled;
- `Восстановить доступ`.

The APK must never contain payment-provider private keys/secrets or treat `success=true` in a browser return as proof of payment.

# Modern feature backlog by domain

## Connection/reliability
- AUTO health scoring;
- failover;
- reconnect backoff;
- network handover;
- captive portal handling;
- trusted-network policies;
- optional pause VPN;
- connection health based on real evidence;
- long soak/reconnect stress testing.

## Privacy/routing
- DNS through VPN;
- DoH;
- IPv6 proxy/fail-closed policy;
- per-app rules;
- domain/CIDR DIRECT/VPN/BLOCK;
- LAN access;
- optional maintained blocklists;
- deterministic precedence tests.

## Subscription/commerce
- transactional refresh;
- expiry reasoning;
- last sync;
- plan catalog;
- hosted checkout;
- order/webhook idempotency;
- entitlement provisioning;
- restore/renew;
- promo codes;
- optional account/device management;
- SecretStore.

## Android UX
- Quick Settings;
- notification actions;
- onboarding;
- empty/error states;
- haptics;
- reduced motion;
- TalkBack;
- large text;
- RU/EN localization;
- gesture/system-bar insets;
- tablet/foldable sanity;
- screenshot regression.

## Security/operations
- exact-sha build info;
- APK checksum;
- signed stable releases;
- update-manifest verification;
- SBOM/dependency inventory;
- license compliance;
- secret/log/diagnostic redaction;
- optional privacy-safe crash reporting;
- backend rate limits/idempotency/audit/reconciliation;
- incident/status mode.

# Features to avoid unless a real requirement appears

Do not copy generic VPN marketing patterns merely because competitors use them:
- fake speedometer;
- decorative world map as a core UI;
- fake `military grade` claims;
- `AI VPN` branding for deterministic ping sorting;
- fake threat counters;
- crypto wallet/social feed;
- invasive ad/attribution SDKs;
- dozens of themes;
- animation that competes with protection state.

# Quality gates

## Gate A — static/build
- compile;
- unit tests;
- lint;
- verifier;
- secret checks.

## Gate B — integration
- lifecycle/routing/subscription logic;
- relevant deterministic race tests;
- CI green.

## Gate C — emulator UX
- install/launch/navigation;
- AUTO first;
- screenshots where relevant;
- no obvious visual regressions.

Emulator does not establish real VPN E2E.

## Gate D — physical VPN
- before/after external IP;
- browser/other app traffic;
- DNS;
- IPv6;
- disconnect;
- rapid reconnect;
- handover;
- permission/service lifecycle.

## Gate E — release
- trusted reviewer has no P0/P1;
- physical E2E PASS;
- artifact exact SHA;
- signed release when stable channel requires it;
- provenance/checksum;
- owner retest of the exact candidate.

# AI/CI cost discipline

GitHub CI handles mechanical checks on ordinary pushes.

GPT-5.6 Sol is a senior checkpoint reviewer, not a per-commit lint bot.

Use `[hotfox-review]` only on the final commit of a coherent green task block. The reviewer receives compact guardrails/current phase + relevant diff/previous findings, not this entire roadmap on every call.

This keeps model usage focused while preserving deep review where it matters.
