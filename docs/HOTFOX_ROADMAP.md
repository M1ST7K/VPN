# HotFox Proxy — Canonical Engineering Roadmap

Status: **CANONICAL**

Current engineering phase: **FINAL RELEASE VALIDATION GATE** (3.1 ENGINEERING COMPLETE — runtime and physical release validation deferred)

This document defines the sequential HotFox engineering roadmap from the already completed truthful core and commercial foundation through the mature 3.0 platform.

The roadmap is intentionally progressive. Each phase builds on the previous one. Cursor must not skip ahead, blend multiple future phases into one uncontrolled refactor, or reopen a completed phase merely because a later phase has not yet been implemented.

Historical repository state and previous phase evidence remain valid and must not be deleted or rewritten merely to match this document. Earlier commits, previous reviewer checkpoints, existing 2.2/2.3 documentation and implementation history remain part of the project record.

---

# GLOBAL ROADMAP RULES

## G1. Canonical order

The engineering order is:

`2.2 Truthful Core`
→ `2.3 Commercial Foundation`
→ `2.4 Smart Connection`
→ `2.5 Privacy Controls / Smart Routing`
→ `2.6 HotFox Shadow / Stealth & Resilience`
→ `2.7 Operations / Release Infrastructure`
→ `2.8 HotFox Autopilot / Adaptive Protection`
→ `2.9 VPN Core Recovery / Real Connection Fix`
→ `3.0 Premium Android Experience`
→ `3.1 Mature HotFox Platform / Pre-release Engineering`
→ `FINAL RELEASE DEVICE GATE`

Owner override: `.cursor/rules/21-hotfox-roadmap-2.9-vpn-recovery.mdc` and `docs/HOTFOX_2_9_VPN_RECOVERY.md` win for numbering from 2.9 onward. Legacy headings «2.9 Premium» / «3.0 Mature» in later sections of this file are the 3.0 / 3.1 scopes.

Do not reorder these phases unless the owner explicitly changes the roadmap.

## G2. Engineering completion gate

For every engineering phase from 2.4 through 3.0, the phase may be marked `ENGINEERING COMPLETE` when:

- required implementation for that phase exists;
- affected code builds successfully;
- required unit tests pass;
- required integration tests pass in the available engineering environment;
- lint/static/build checks required by the repository pass;
- critical regressions in earlier truthful VPN/commercial paths have not been introduced;
- one final `[hotfox-review]` reports no substantiated P0 or P1 findings.

Canonical phase exit:

`implementation`
→ `automated/integration validation`
→ final `[hotfox-review]`
→ `P0 = 0, P1 = 0`
→ `<phase> ENGINEERING COMPLETE`
→ immediately begin the next roadmap phase.

P2-only findings do not block phase progression unless the owner explicitly promotes them.

## G3. Physical Android validation is NOT a per-phase blocker

Physical validation on a real Android device is deliberately deferred to the single final release device gate.

Do NOT require a physical Android test to close 2.4, 2.5, 2.6, 2.7, 2.8, 2.9 or 3.0 engineering gates.

Do NOT ask the user to perform a device test between engineering phases merely because the phase changed.

Do NOT remain idle waiting for a physical device test when the current engineering phase otherwise satisfies its exit gate.

Do NOT claim that physical validation has occurred when it has not.

Until the final release gate, truthful status wording is:

`ENGINEERING COMPLETE — physical release validation deferred.`

Only the final `RELEASE READY` state requires the physical Android acceptance suite defined at the end of this document.

## G4. Earlier guarantees remain binding

No later roadmap phase may regress the 2.2 truthful VPN guarantees or 2.3 commercial guarantees.

Production VPN path remains conceptually:

`Android apps -> VpnService/TUN -> HEV/tun2socks -> local Xray SOCKS -> Xray outbound -> VPN server -> Internet`

A VPN icon, a TUN fd, a running Xray process, a listening SOCKS port, a selected server, a successful ping, a completed checkout redirect or a client-side `success=true` is never sufficient proof of the corresponding protected/paid state.

`CONNECTED` / `Защищено` remains derived from the canonical verified VPN session state.

Payment entitlement remains backend-authoritative.

Secrets must not be embedded in the APK or logged.

Manual HTTPS subscription import remains a supported independent path unless an explicitly approved future migration changes it.

## G5. One canonical state machine

New automation, routing, Shadow, release and UX layers must integrate with the existing canonical connection/session state. Do not create a second contradictory VPN state source of truth.

## G6. Bounded behavior

Reconnect, failover, probing, fallback, self-healing, transport switching and network-recovery loops must be bounded, serialized and cancellable.

No infinite reconnect loops.
No unbounded coroutine fan-out.
No stale asynchronous result may overwrite a newer session decision.

## G7. No fake product state

Do not introduce production fake values for:

- ping;
- server health;
- traffic counters;
- subscription status;
- expiry;
- path health;
- transport availability;
- routing state;
- protection state;
- payment state.

If the value is unknown, show or represent it as unavailable/unknown.

---

# 2.2 — TRUTHFUL CORE

Status: **ENGINEERING COMPLETE**

Historical phase. Preserve existing implementation and reviewer evidence.

## Purpose

Make HotFox a real Android VPN rather than a UI that merely creates a TUN or starts Xray.

## Preserved guarantees

- Android `VpnService` owns the VPN interface.
- Xray starts before HEV/tun2socks consumes the local SOCKS path.
- local SOCKS readiness is checked before the session may progress to protected state;
- HEV/tun2socks forwards TUN traffic into the local Xray path;
- DNS cannot silently bypass the intended route while protection is claimed;
- IPv6 is routed correctly or fails closed according to project policy;
- stale Xray/HEV/TUN resources are cleaned on disconnect/reconnect;
- network changes do not create duplicate contradictory sessions;
- user-visible protected state comes from canonical bounded readiness logic;
- TLS/REALITY/security is not weakened to make a connection appear successful.

## Historical release status

Engineering completion does not mean physical release acceptance. The historical physical-device acceptance remained deferred and is now consolidated into the final release device gate after 3.0.

---

# 2.3 — COMMERCIAL FOUNDATION

Status: **ENGINEERING COMPLETE / historical phase** when the repository's final 2.3 checkpoint has P0=0 and P1=0.

Preserve previous 2.3 implementation, tests, commits and reviewer evidence.

## Purpose

Allow a user to acquire HotFox-managed access from the application without pasting a private subscription URL, while preserving the existing manual subscription path.

## Core flow

`plan`
→ `order`
→ `hosted checkout / sandbox`
→ `backend-authoritative payment verification`
→ `entitlement`
→ `safe local credential handling`
→ `subscription/manifest sync`
→ `AUTO preserved`
→ `truthful VPN path from 2.2`

## Preserved commercial guarantees

- no payment-provider private key in the APK;
- no HotFox backend admin secret in the APK;
- no raw private user subscription URL committed into source/resources;
- checkout redirect is not payment proof;
- client-side `success=true` is not entitlement;
- backend webhook/provider verification is authoritative;
- order/webhook processing is idempotent;
- duplicate webhook delivery cannot duplicate entitlement or corrupt order state;
- lost checkout response can recover from authoritative backend state;
- checkout cancellation does not fabricate entitlement;
- successful payment can recover after cold start/process loss;
- sensitive entitlement/subscription credentials use Keystore-backed protection where required;
- manual HTTPS subscriptions remain functional if HotFox commercial backend is unavailable;
- HotFox-managed access ultimately returns to the same truthful VPN path.

## Important historical test scenarios

- duplicate webhook;
- repeated order callback;
- provider retry;
- lost client response;
- checkout cancellation;
- successful payment with lost browser callback;
- restart before entitlement refresh;
- manifest/subscription refresh after entitlement recovery;
- unavailable billing backend with manual subscription path still usable.

---

# 2.4 — SMART CONNECTION

Status: **ENGINEERING COMPLETE — physical release validation deferred**

## Purpose

Turn `AUTO` into a real intelligent server-selection mode instead of a cosmetic row, first-item selection or random choice.

2.4 answers:

> Which eligible server is the best server to use right now?

It does NOT yet own the full multi-transport censorship-resilience logic of 2.6. It selects the best concrete eligible server and integrates that decision into the existing truthful VPN path.

## 2.4.1 AUTO as a first-class persisted mode

`Авто-выбор сервера` remains the first row in the server list.

AUTO must be persisted as a mode, not replaced in storage by the concrete server it resolved to for one session.

Manual server selection remains a distinct persisted user intent.

AUTO may resolve to a concrete server for an active session, but it must never silently overwrite a manual selection.

## 2.4.2 Eligible candidate set

AUTO may rank only servers that are valid candidates for the current user and runtime.

Candidate filtering must account for, as applicable:

- entitlement;
- manifest validity;
- parser/config-generation validity;
- supported transport/security fields for the embedded Xray version;
- disabled/maintenance state when available;
- obviously invalid server entries;
- current route/config compatibility.

One invalid server entry must not poison all valid candidates.

## 2.4.3 Real health observations

Collect real, bounded observations where practical:

- reachability;
- latency;
- last successful probe/connect;
- last failed probe/connect;
- consecutive failure count;
- health freshness timestamp;
- optional smoothed latency (for example EWMA) rather than reacting to one noisy sample.

Do not fabricate ping values.

A probe is evidence for server selection only; it is not proof that the VPN tunnel is protected.

## 2.4.4 Bounded concurrent probing

Health checks must not block the Android main thread.

Use bounded concurrency and cancellation.

Requirements:

- no unbounded coroutine creation;
- per-probe timeout;
- overall selection deadline;
- cancellation when a newer selection request supersedes the old one;
- stale probe results cannot overwrite a new network/session decision.

## 2.4.5 Server score

Introduce a deterministic testable server-scoring policy.

The exact weights may evolve, but the policy should be based on explicit signals such as:

- healthy/reachable state;
- latency;
- failure penalty;
- recent successful connection history;
- freshness of the observation;
- optional server preference metadata.

Do not scatter scoring magic numbers across UI/service classes. Keep scoring as isolated pure/testable logic where practical.

## 2.4.6 Hysteresis and stickiness

AUTO must not flap between servers because of tiny measurement changes.

If the current resolved server remains healthy and reasonably competitive, keep it.

Switch only when one of the defined conditions is met, for example:

- current server becomes unhealthy;
- connection fails;
- score degradation passes a meaningful threshold;
- network environment changed and a new selection is required.

A 5 ms improvement alone should not normally tear down a healthy session.

## 2.4.7 Last-good memory

Remember useful recent success information, such as the last good AUTO target, without treating it as permanent truth.

Cold start may bias toward a known-good eligible candidate, but current health/failure evidence can override it.

## 2.4.8 Cold-start fallback

AUTO must still choose a valid candidate when no fresh health observations exist yet.

Unknown health must not make the app permanently unable to connect.

Define deterministic fallback ordering.

## 2.4.9 Bounded server failover

If the resolved AUTO server cannot establish a valid VPN session, AUTO may try another eligible candidate.

Failover must be:

- bounded;
- serialized;
- cancellation-aware;
- integrated with the canonical session controller;
- protected from simultaneous reconnect/failover jobs.

Do not create infinite loops.

## 2.4.10 Network-context changes

Wi-Fi/cellular/loss/restore can invalidate previous health data.

The selection layer must account for network changes and must not blindly reuse stale latency observations from a different network context.

## 2.4.11 Truthful UI

The user-facing state must reflect real application state.

Possible sequence:

`AUTO`
→ `Подбираем сервер…`
→ `Finland 01`
→ `Подключение…`
→ `Защищено`

The selected/resolved AUTO target should be visible.

`Защищено` is still emitted only by the truthful VPN session state, never by the selector itself.

## 2.4.12 Diagnostics

Provide safe diagnostic events sufficient to explain AUTO decisions without exposing credentials.

Examples:

- candidate count;
- candidate filtered reason category;
- measured latency/health where safe;
- selected server identifier using non-secret metadata;
- failover reason category.

Never log private subscription URLs, access tokens, private UUID credentials, private keys or complete sensitive Xray configuration.

## 2.4.13 Required engineering validation

Automated tests should cover important pure logic and race-prone behavior, including:

- deterministic ranking;
- failure penalty;
- stale-data handling;
- tie handling;
- hysteresis;
- stickiness;
- manual selection not overwritten by AUTO;
- cold-start fallback;
- entitlement filtering;
- invalid candidate filtering;
- bounded failover;
- rapid connect/disconnect/reconnect;
- network-change invalidation;
- obsolete async work not overwriting newer state.

## 2.4 exit gate

When implementation and engineering validation are complete, run one final `[hotfox-review]`.

If P0=0 and P1=0:

`2.4 ENGINEERING COMPLETE`

Then immediately start 2.5.

Physical Android validation is not required for this transition.

---

# 2.5 — PRIVACY CONTROLS / SMART ROUTING

Status: **ENGINEERING COMPLETE — physical release validation deferred**

## Purpose

Give the user real control over which traffic uses the VPN while preserving truthful routing, DNS and IPv6 guarantees.

2.5 answers:

> Which traffic should go through HotFox, which traffic may go direct, and which traffic should be blocked?

The UI must represent real Android/Xray routing behavior. Decorative privacy toggles are forbidden.

## 2.5.1 Routing modes

Implement explicit routing modes such as:

- `Весь трафик`;
- `Smart`;
- `Только выбранные приложения`;
- `Исключить приложения`.

Names can be refined by final UX work, but each mode must map to an explicit deterministic policy.

## 2.5.2 Android app split tunneling

Support app-level include/exclude behavior using appropriate Android `VpnService` app policy and Xray routing where necessary.

Examples:

- Telegram -> VPN;
- browser -> VPN;
- selected banking app -> direct;

or inverse selected-app-only behavior.

App package changes/uninstalls must fail gracefully.

An invalid package rule must not break the whole VPN session.

## 2.5.3 Domain routing

Introduce a real domain policy model supporting explicit outcomes such as:

- proxy;
- direct;
- block.

Advanced domain rules may be hidden from the default UI but must be represented by testable data structures rather than ad-hoc string checks.

## 2.5.4 Deterministic routing priority

Define and test conflict resolution.

One acceptable model might be:

`BLOCK > APP-SPECIFIC > DOMAIN-SPECIFIC > GLOBAL MODE`

The exact policy must be explicitly documented in code/tests before implementation is considered complete.

The result must never depend accidentally on hash-map iteration order, subscription ordering or race timing.

## 2.5.5 DNS routing policy

DNS behavior must follow the intended privacy/routing model.

Requirements:

- no silent plaintext/system-DNS bypass while full protection is claimed;
- DNS path is explicit per routing mode;
- fallback does not secretly violate the selected privacy policy;
- unavailable secure DNS fails according to explicit policy rather than silently leaking.

Optional encrypted DNS transports may be introduced only when integrated and tested correctly.

## 2.5.6 IPv4 / IPv6 policy

Preserve 2.2 leak guarantees.

IPv6 must be either:

- correctly routed under the selected policy; or
- explicitly fail-closed/disabled where the product policy requires it.

Smart routing must never accidentally create an IPv6 bypass.

## 2.5.7 Local network / LAN access

Add an explicit LAN/local-network policy.

Use cases:

- allow home printer/NAS/Chromecast access on trusted networks;
- prevent unnecessary LAN reachability on public networks;
- make behavior visible and understandable.

Do not silently bypass arbitrary private ranges unless the policy says so.

## 2.5.8 Basic blocker capability

A basic tracker/ad/domain block capability may be added through the same deterministic DNS/routing policy layer.

Scope discipline:

- do not turn 2.5 into a giant ad-blocking project;
- keep block lists/versioning modular;
- avoid collecting browsing history;
- failures in optional blocking must not corrupt core VPN routing.

## 2.5.9 Routing state in UI

Expose truthful concise routing state, for example:

- `Весь трафик`;
- `Smart`;
- `3 приложения через VPN`;
- `2 приложения напрямую`.

The UI must be derived from the actual active policy, not a stale preference toggle.

## 2.5.10 Persistence and migration

Routing preferences must survive restart/process recreation.

Schema changes must include safe migration/default behavior.

Corrupt or obsolete routing entries must degrade gracefully.

## 2.5.11 Reconfiguration behavior

Changing routing mode while connected must use a deliberate controlled transition.

Do not mutate live routing state partially and leave UI/service disagreement.

If a VPN restart/reconfigure is required, represent that transition truthfully.

## 2.5.12 Required engineering validation

Automated/integration coverage should include:

- app include mode;
- app exclude mode;
- missing/uninstalled app;
- domain proxy/direct/block;
- conflicting rules;
- deterministic priority;
- DNS policy;
- IPv4/IPv6 policy logic;
- LAN allow/deny policy;
- reconnect with active routing rules;
- server change with active routing rules;
- process recreation/persistence;
- rapid routing-mode changes;
- no stale config overwriting a newer policy.

## 2.5 exit gate

`implementation + CI + automated/integration validation + final [hotfox-review] + P0=0/P1=0`

→ `2.5 ENGINEERING COMPLETE`

→ immediately start 2.6.

No physical Android test is required between 2.5 and 2.6.

---

# 2.6 — HOTFOX SHADOW / STEALTH & RESILIENCE

Status: **ENGINEERING COMPLETE — physical release validation deferred**

## Purpose

Make HotFox resilient when the fastest/default VPN path is unavailable, filtered, unstable or degraded.

2.4 selects the best server.

2.6 expands that decision to:

> Which server + transport + route is the best usable protected path in the current network environment?

This is a technical reliability feature, not a license to weaken TLS or security.

## 2.6.1 Server capabilities model

Extend server/manifest capability data to describe only combinations actually supported by the backend and exact embedded Xray version.

Possible fields/capabilities include:

- region;
- endpoint metadata;
- supported transports;
- supported security mode;
- IPv4 capability;
- IPv6 capability;
- stealth capability;
- entry-node capability;
- exit-node capability;
- maintenance/availability flags;
- optional weights/priority metadata.

Do not advertise capabilities the client cannot actually configure.

## 2.6.2 Supported transport paths

Support multiple production-ready transport paths where justified by infrastructure.

Examples may include combinations built from the exact Xray features we ship, such as appropriate VLESS/REALITY/RAW/XHTTP combinations.

Do not blindly enable every Xray transport just because it exists.

Each supported path must have:

- parser/config support;
- validation;
- safe defaults;
- automated coverage;
- compatibility with our truthful VPN pipeline.

## 2.6.3 Path model

Introduce a first-class concept of a connection path:

`server + transport + security + optional entry/exit route`

Do not overload the plain server object until the architecture becomes impossible to reason about.

## 2.6.4 PathScore

Extend selection from `ServerScore` toward a deterministic `PathScore`.

Potential signals:

- server health;
- transport reachability;
- recent path success;
- path latency;
- consecutive failures;
- network capability history;
- entry/exit compatibility;
- current entitlement/capability constraints.

Keep scoring policy isolated/testable.

## 2.6.5 Adaptive transport fallback

Define a bounded ordered fallback strategy.

Conceptually:

`preferred normal path`
→ if unavailable, `alternate supported path`
→ if unavailable, `alternate server/path`
→ if policy permits and needed, `Shadow route`

Requirements:

- strict retry budget;
- no infinite loops;
- cancellation on explicit user disconnect;
- newer connection intent supersedes old fallback jobs;
- no simultaneous independent recovery loops.

## 2.6.6 Network capability cache

Maintain local technical observations about the current/recent network environment, not a user identity fingerprint.

Examples:

- transport A recently succeeded;
- transport B repeatedly timed out;
- IPv6 path currently unusable;
- particular route class recently worked.

Rules:

- bounded retention;
- stale observations expire;
- network-context change can invalidate or reduce trust in old data;
- do not upload browsing behavior;
- do not turn SSID/BSSID history into unnecessary tracking telemetry.

## 2.6.7 HotFox Shadow AUTO mode

Expose a simple user concept such as:

`Shadow: Авто`

Default users should not need to understand VLESS, REALITY, XHTTP, SNI or transport internals.

The UI should communicate the result, for example:

`Подбираем защищённый маршрут…`

rather than dumping low-level transport vocabulary.

Advanced diagnostics may expose more detail without becoming the default UX.

## 2.6.8 Adaptive multihop / Shadow Route

When direct access to the desired exit path is unavailable or meaningfully unreliable, allow a policy-driven entry/exit route where the infrastructure supports it:

`device -> reachable HotFox entry -> HotFox exit -> Internet`

Requirements:

- multihop is not forced merely for marketing;
- AUTO may use it only when policy/conditions justify the overhead;
- entry and exit capabilities must be explicit;
- no accidental loops;
- no same-node invalid route unless intentionally supported;
- bounded route candidate count.

## 2.6.9 Manual multihop for advanced users

Optional advanced control may allow manual entry/exit selection.

This must not be required for normal operation.

Manual multihop state must remain distinct from Shadow AUTO decisions.

## 2.6.10 Self-healing VPN

Add controlled recovery from meaningful active-session degradation.

Potential triggers:

- transport failure;
- repeated readiness loss;
- verified path death;
- sustained failure signals over thresholds.

Do not react to one noisy packet or tiny latency change.

Use thresholds + hysteresis + cooldown.

Recovery order must be explicit and bounded.

## 2.6.11 Connection Doctor

Provide a diagnostic model that can explain broad failure categories and safe suggested action.

Example categories:

- no underlying Internet;
- VPN permission unavailable;
- entitlement unavailable;
- candidate server unavailable;
- primary transport unavailable;
- alternate path available;
- DNS bootstrap failure;
- configuration unsupported;
- local VPN pipeline failure.

Do not expose secret configuration values.

Where safe, provide an `Исправить автоматически` action that invokes real bounded recovery logic rather than a cosmetic retry.

## 2.6.12 DNS bootstrap resilience

Ensure the client can resolve/reach required backend/VPN endpoints under difficult DNS conditions without creating an untracked DNS leak.

Possible techniques may include:

- signed endpoint metadata;
- cached validated addresses with TTL;
- explicitly configured protected resolver strategies;
- bounded fallback.

The exact implementation must preserve the project's privacy/security policy.

## 2.6.13 IPv4 / IPv6 path intelligence

Track usable address-family path conditions as part of path selection.

A bad IPv6 route must not prevent a working IPv4 protected path when policy allows it.

Conversely, IPv6 must never silently escape the VPN merely because the chosen path is IPv4-oriented.

## 2.6.14 Security guardrails

Never weaken:

- certificate verification;
- TLS validation;
- REALITY semantics;
- transport security;
- secret handling;

merely to increase connection success rate.

Any fragmentation/noise/stealth mechanism must be intentional, bounded, compatible with the exact Xray build and covered by tests/config validation.

## 2.6.15 Required engineering validation

Automated/integration coverage should include:

- path capability filtering;
- unsupported transport rejection;
- PathScore determinism;
- primary path success;
- primary path failure -> alternate transport;
- alternate server/path fallback;
- retry budget exhaustion;
- cancellation during fallback;
- network-context cache invalidation;
- stale path result cannot overwrite newer intent;
- Shadow AUTO decision;
- entry/exit validity;
- multihop loop prevention;
- self-healing thresholds/cooldown;
- DNS bootstrap fallback logic;
- IPv4/IPv6 policy logic;
- no TLS/security weakening in generated config.

## 2.6 exit gate

`implementation + CI + automated/integration validation + final [hotfox-review] + P0=0/P1=0`

→ `2.6 ENGINEERING COMPLETE`

→ immediately start 2.7.

No physical Android test is required between 2.6 and 2.7.

---

# 2.7 — OPERATIONS / RELEASE INFRASTRUCTURE

Status: **ENGINEERING COMPLETE — physical release validation deferred**

## Purpose

Make HotFox operable as a real service: buildable, diagnosable, updateable and safely releasable without depending on manual ad-hoc developer actions.

This phase builds release infrastructure but does NOT itself perform the final physical release device gate.

## 2.7.1 Release channels

Define explicit channels such as:

- dev;
- beta;
- stable.

Channel behavior must not silently mix production and test backends/credentials.

## 2.7.2 Versioning

Standardize:

- `versionCode`;
- `versionName`;
- artifact naming;
- release metadata;
- release notes linkage.

Every distributed artifact should be uniquely traceable to source revision/build metadata.

## 2.7.3 CI release pipeline

Build a deterministic pipeline conceptually covering:

`source checkout`
→ `dependency/build setup`
→ `unit tests`
→ `integration tests`
→ `lint/static checks`
→ `APK build`
→ `artifact verification`
→ `release/signing step when authorized`
→ `published artifact metadata`

Do not “fix” CI by disabling meaningful VPN functionality or broad critical checks.

## 2.7.4 Signing security

Production signing material must not be committed.

Requirements:

- signing passwords/secrets outside repository;
- no private keystore in public source;
- clear separation of debug and release signing;
- documented secure release process;
- failure to access release signing must fail honestly rather than silently produce an incorrectly signed artifact.

## 2.7.5 Artifact integrity

Produce/record integrity information such as SHA-256 for distributed APKs where appropriate.

The update/install path must be able to distinguish authentic expected artifact metadata from arbitrary downloaded bytes.

## 2.7.6 Controlled update mechanism

Because local/non-Play distribution is supported, provide a safe update experience.

Capabilities may include:

- signed/update manifest;
- latest stable version metadata;
- minimum supported version where truly needed;
- download URL metadata;
- integrity hash;
- clear user-visible update state.

Do not create an arbitrary remote-code execution mechanism.

## 2.7.7 Rollback / rollout control

Design for stopping or rolling back a bad release.

Examples:

- staged channel promotion;
- server-side update manifest can stop offering a known-bad version;
- previous known-good artifact remains identifiable;
- feature rollout flags fail safe.

Do not silently downgrade user security without explicit policy.

## 2.7.8 Privacy-safe diagnostics

Introduce structured operational diagnostics for categories such as:

- VPN startup failure class;
- AUTO selection failure class;
- Shadow fallback class;
- billing/backend failure class;
- manifest failure class;
- crash/ANR class;
- reconnect count bucket;
- version/build metadata.

Never log/transmit:

- private subscription URLs;
- raw entitlement tokens;
- private UUID credentials;
- private keys;
- signing passwords;
- full sensitive Xray configs;
- unnecessary browsing/domain history.

## 2.7.9 Redaction layer

Centralize redaction for logs/diagnostics.

Redaction must have automated tests for known sensitive patterns/fields.

## 2.7.10 Service health model

Define health/status representation for operational components such as:

- HotFox API;
- billing;
- entitlement service;
- manifest/config service;
- VPN node pools;
- Shadow entry pools where applicable.

This allows diagnosis of systemic incidents rather than assuming every user failure is local.

## 2.7.11 Server draining / maintenance

Allow infrastructure to remove a node/path from new AUTO selection without requiring an APK release.

Requirements:

- signed/authenticated authoritative metadata;
- existing sessions handled according to explicit policy;
- no client trust in arbitrary unsigned remote config;
- graceful fallback to valid nodes.

## 2.7.12 Build artifact contract

A change intended for engineering retest should produce an installable debug/test artifact through CI where the project environment supports it.

Production release signing remains separately authorized.

## 2.7.13 Required engineering validation

Coverage should include:

- version parsing/comparison;
- update manifest verification;
- hash mismatch handling;
- channel isolation;
- missing/invalid update metadata;
- rollback/known-bad version behavior;
- log redaction;
- no secrets in generated diagnostic payload;
- node maintenance/drain behavior in selection;
- CI produces expected artifact without disabling core VPN checks.

## 2.7 exit gate

`implementation + CI + automated/integration validation + final [hotfox-review] + P0=0/P1=0`

→ `2.7 ENGINEERING COMPLETE`

→ immediately start 2.8.

Do not run or request the final physical Android release acceptance yet solely because release infrastructure now exists.

---

# 2.8 — HOTFOX AUTOPILOT / ADAPTIVE PROTECTION

Status: **ENGINEERING COMPLETE — physical release validation deferred**

## Purpose

Make protection zero-touch for normal users.

The product should increasingly answer:

> Given the current network, user policy and VPN state, what should HotFox do automatically to keep the intended protection level?

This phase is about connection intent and context-aware protection, not about inventing a second session controller.

## 2.8.1 Trusted networks

Allow the user to classify network contexts according to a privacy-respecting policy, for example:

- trusted home Wi-Fi;
- trusted office Wi-Fi;
- unknown Wi-Fi;
- cellular.

Avoid collecting/storing more persistent network identifiers than required for the feature.

## 2.8.2 Auto-connect policies

Support clear policies such as:

- connect on unknown Wi-Fi;
- connect on cellular;
- reconnect after network restore;
- connect after boot/unlock when permitted and configured;
- remain off on explicitly trusted network when user policy says so.

All automatic actions go through the canonical VPN session controller.

## 2.8.3 Network profiles

Provide simple profile concepts such as:

### Home
- AUTO server;
- LAN allowed;
- normal protected DNS;
- user-selected routing profile.

### Public Wi-Fi
- VPN required by policy;
- stricter LAN policy;
- strict protected DNS;
- Shadow AUTO available when needed.

### Cellular
- AUTO;
- efficiency/speed bias;
- avoid unnecessary heavy routing when user policy does not require it.

Exact UI wording may evolve in 2.9.

## 2.8.4 Protection level profiles

Provide high-level choices rather than dozens of technical toggles, for example:

- `Скорость`;
- `Баланс`;
- `Максимальная защита`.

Each profile must map to explicit real routing/connection behavior.

Advanced settings may override profile defaults in a deterministic manner.

## 2.8.5 Seamless network handoff

Handle:

- Wi-Fi -> cellular;
- cellular -> Wi-Fi;
- network loss;
- network restore;
- airplane mode transitions;
- captive portal transitions.

Technical sessions may need to restart, but state transitions must remain serialized and truthful.

No duplicate sessions.
No parallel reconnect storms.

## 2.8.6 Captive portal awareness

Detect/represent when the underlying Wi-Fi requires user authentication before normal Internet access.

HotFox should not trap the user in a state where the VPN blocks the portal while claiming a generic VPN failure.

Provide explicit state such as:

`Сеть требует авторизации`

and resume configured protection after the portal/network becomes usable.

## 2.8.7 Pause protection

Support temporary pause options such as:

- 5 minutes;
- 15 minutes;
- 1 hour;
- until network changes.

After expiry/trigger, protection returns according to the user's policy.

A pause must be clearly distinct from permanent disable.

## 2.8.8 ConnectionIntentEngine

Introduce pure/testable decision logic combining relevant state, for example:

`Network context`
+ `User policy`
+ `Entitlement`
+ `Routing mode`
+ `Current session`
+ `AUTO/Shadow capability`
+ `Pause state`
→ `ConnectionIntent`

Possible intents:

- `KEEP_CURRENT`;
- `CONNECT_AUTO`;
- `RECONNECT`;
- `WAIT_FOR_NETWORK`;
- `WAIT_FOR_CAPTIVE_PORTAL`;
- `PAUSED`;
- `DISCONNECT_BY_POLICY`.

Names may differ in implementation, but the architecture should avoid scattered lifecycle `if` statements producing conflicting actions.

## 2.8.9 Priority and conflict rules

Define how explicit user actions interact with automation.

Examples:

- explicit user disconnect may temporarily suppress immediate auto-reconnect according to policy;
- active pause suppresses auto-connect until expiry;
- revoked VPN permission blocks connection attempts until permission lifecycle is resolved;
- no network supersedes connect intent;
- captive portal wait supersedes normal auto-connect when required;
- user manual server remains compatible with Autopilot without being silently rewritten to AUTO.

## 2.8.10 Battery and background discipline

Autopilot must not become a permanent polling loop.

Prefer platform/network callbacks and event-driven decisions.

Requirements:

- no busy waits;
- no excessive background probes;
- WorkManager/alarm usage only where appropriate;
- foreground-service behavior compliant with supported Android versions.

## 2.8.11 Boot/restart/process recreation

Persist only necessary policy state.

After process recreation/boot:

- recover user policy;
- derive fresh intent from current conditions;
- do not blindly resurrect a stale session id;
- do not duplicate a running VPN service;
- handle missing entitlement/network/permission truthfully.

## 2.8.12 Zero-Touch HotFox product goal

Default successful behavior should increasingly become:

`configure once -> HotFox keeps applying intended protection automatically`

The user should not need to open the app for every ordinary Wi-Fi/cellular transition.

## 2.8.13 Required engineering validation

Coverage should include:

- trusted vs unknown network policy;
- cellular policy;
- pause/expiry;
- explicit user disconnect behavior;
- network loss/restore;
- Wi-Fi/cellular handoff intent;
- captive portal wait/resume logic;
- process recreation;
- boot policy logic;
- permission unavailable/revoked;
- entitlement unavailable;
- manual server preserved;
- AUTO behavior preserved;
- no duplicate connection intents;
- no reconnect storm;
- stale event cannot override a newer intent.

## 2.8 exit gate

`implementation + CI + automated/integration validation + final [hotfox-review] + P0=0/P1=0`

→ `2.8 ENGINEERING COMPLETE`

→ immediately start 2.9.

No physical Android test is required between 2.8 and 2.9.

---

# 2.9 — VPN CORE RECOVERY / REAL CONNECTION FIX

Status: **ENGINEERING COMPLETE — runtime and physical release validation deferred** (round 17 `APPROVED`, SHA `4524207`)

Canonical specification: `docs/HOTFOX_2_9_VPN_RECOVERY.md` and `.cursor/rules/21-hotfox-roadmap-2.9-vpn-recovery.mdc`.

This phase proves real Internet through TUN → HEV → SOCKS `127.0.0.1:10808` → Xray. The next heading named «2.9 Premium Android Experience» is the shifted **3.0** scope and must not start until this recovery phase is ENGINEERING COMPLETE.

---

# 2.9 — PREMIUM ANDROID EXPERIENCE

Status: **ENGINEERING COMPLETE — runtime and physical release validation deferred** (3.0 Premium Android Experience; former 2.9 Premium heading; round 19 `APPROVED`, SHA `21f3419`)

## Purpose

Perform the major cohesive UX/product-polish pass only after the core behavior of commerce, Smart Connection, routing, Shadow, operations and Autopilot is known.

The goal is not to hide technical failure. The goal is to make truthful system behavior feel simple, deliberate and premium.

## 2.9.1 Preserve HotFox visual language

Canonical direction:

- modern premium 2026 Android product;
- charcoal / purple-black background;
- warm cream/off-white typography;
- restrained orange accent;
- soft green only for real success/protected state;
- deliberate negative space;
- typography-first editorial hierarchy;
- thin separators;
- minimal heavy cards;
- no neon gamer/cyberpunk aesthetic;
- no generic stock-v2rayNG look;
- no return to legacy vertical side rail on phone unless owner explicitly requests it.

## 2.9.2 Primary navigation

Preserve the three primary phone destinations:

- `Соединение`;
- `Серверы`;
- `Подписка`.

Secondary routing/privacy/Autopilot/settings surfaces should be integrated without turning the primary navigation into a crowded dashboard.

## 2.9.3 Connection state presentation

Map the real canonical state machine into a polished visual sequence, for example:

`Отключено`
→ `Подбираем маршрут`
→ `Подключение`
→ `Проверяем защиту`
→ `Защищено`

The exact labels may change, but UI must not jump directly to success because a process started.

## 2.9.4 Resolved AUTO / Shadow truth

When AUTO resolves to a concrete target, show the real resolved destination appropriately.

When Shadow/fallback is active, communicate useful high-level state without dumping protocol internals on normal users.

Example:

`AUTO · Finland`

or

`Защищённый маршрут восстановлен`

rather than fake simplified server state.

## 2.9.5 Connection visualization

Preserve/refine the connection-bars / route visualization so the main screen is not excessively empty.

Animation states must derive from real session state and should be cancellable/restartable without visual glitches during rapid state changes.

## 2.9.6 Onboarding

Design a coherent first-run flow around real requirements:

`welcome`
→ `VPN permission`
→ `HotFox access/subscription or existing manual subscription`
→ `AUTO`
→ `first connection`

Handle permission denial/retry truthfully.

Do not ask for unrelated permissions just for visual onboarding completion.

## 2.9.7 Subscription UX

Polish:

- plan presentation;
- active entitlement state;
- real expiry where supplied;
- unknown expiry only when metadata truly lacks usable expiry;
- checkout launch/return;
- payment pending;
- payment recovery;
- expired state;
- backend unavailable state;
- manual subscription fallback where supported.

Never use browser success redirect as entitlement proof.

## 2.9.8 Server list UX

Server rows remain:

- single-column;
- editorial;
- contemporary;
- clean;
- readable;
- with AUTO first.

Show real health/latency when available, otherwise unavailable state.

Do not populate fake ping to make rows look complete.

## 2.9.9 Routing/privacy UX

Make 2.5 capabilities understandable without exposing every internal rule by default.

Default surface should prioritize high-level modes.

Advanced app/domain rules may live deeper.

Active routing state on the connection screen must be derived from the actual policy.

## 2.9.10 Autopilot UX

Expose simple controls for:

- automatic protection;
- trusted networks;
- pause;
- protection profile;
- captive portal state.

Avoid a settings wall of dozens of switches.

## 2.9.11 Error and recovery states

Design explicit states for:

- no network;
- VPN permission missing/revoked;
- entitlement unavailable;
- subscription expired;
- billing backend unavailable;
- no eligible nodes;
- Smart Connection exhausted candidates;
- Shadow recovery in progress;
- DNS/bootstrap failure;
- captive portal required;
- update required where applicable.

Where a real recovery action exists, provide it clearly.

## 2.9.12 Motion and haptics

Add restrained premium motion for:

- connect;
- protected confirmation;
- disconnect;
- server/AUTO resolution;
- Shadow recovery;
- navigation transitions;
- subscription state changes.

Haptics should reinforce intentional user actions, not fire continuously during background state churn.

Respect system reduced-motion/accessibility settings where applicable.

## 2.9.13 Accessibility

Validate:

- TalkBack/content descriptions;
- focus order;
- minimum touch targets;
- text scaling;
- contrast;
- state not communicated by color alone;
- meaningful labels for connection controls;
- disabled/loading state semantics.

## 2.9.14 Android lifecycle polish

Ensure coherent UX across:

- Activity recreation;
- rotation where supported;
- process death;
- foreground/background;
- return from hosted checkout;
- notification -> app;
- deep links;
- VPN permission flow;
- network changes during UI transitions.

## 2.9.15 Foreground notification

Provide a high-quality compliant VPN foreground notification derived from the real session state.

Useful actions may include `Disconnect` and navigation back to the connection screen.

Do not show protected notification state before the canonical session is protected.

## 2.9.16 Quick Settings Tile

Add a truthful Quick Settings Tile if supported by project architecture.

Tile state must follow canonical VPN state.

Rapid tile taps must not create duplicate sessions.

## 2.9.17 Edge-to-edge and system integration

Polish:

- system bars;
- edge-to-edge layout;
- insets;
- keyboard behavior;
- dark theme consistency;
- Android API 24 compatibility where required;
- modern behavior on current Android versions.

## 2.9.18 Screenshot/UI regression coverage

Add practical screenshot/golden or UI regression coverage for key stable surfaces where maintainable.

Prioritize:

- connection main states;
- server list;
- subscription state;
- routing summary;
- major error states.

Do not make brittle pixel-perfect tests block every harmless OS text-rendering difference without reason.

## 2.9.19 Required engineering validation

Coverage should include UI/state mapping for:

- disconnected;
- selecting;
- connecting;
- verifying;
- protected;
- disconnecting;
- error/recovery;
- AUTO resolved target;
- manual target;
- Shadow recovery;
- entitlement active/pending/expired;
- process recreation;
- notification state;
- Quick Tile state;
- accessibility semantics for key controls.

## 2.9 exit gate

`implementation + CI + automated/integration/UI validation + final [hotfox-review] + P0=0/P1=0`

→ `2.9 ENGINEERING COMPLETE`

→ immediately start 3.0.

Do not request a physical Android test merely to enter 3.0.

---

# 3.0 — MATURE HOTFOX PLATFORM

Status: **ENGINEERING COMPLETE — runtime and physical release validation deferred** (3.1 Mature HotFox Platform / Pre-release Engineering; former 3.0 Mature heading; round 25 `APPROVED`, SHA `7192b04d441d5f2203c0efc47e37bc0f87d9cda4`)

## Purpose

Evolve HotFox from a strong Android VPN application plus backend into a mature, operable VPN platform with clearer process boundaries, fleet/control-plane capabilities and scalable device/account operations.

3.0 must preserve the simplicity achieved in 2.x; platform complexity stays behind a controlled interface.

## 3.0.1 VPN engine process boundary

Strengthen separation between UI lifecycle and the VPN engine/service lifecycle.

Goals:

- Activity destruction must not imply VPN loss;
- UI process/state recreation should re-observe canonical engine state;
- IPC/state synchronization must not create two contradictory session owners;
- engine crashes/failures transition truthfully;
- resource cleanup remains deterministic.

The exact process model must remain compatible with Android `VpnService` requirements and project architecture.

## 3.0.2 Control plane

Introduce a real HotFox control-plane model for authoritative non-secret operational metadata such as:

- server inventory;
- capabilities;
- node maintenance state;
- capacity/weight information;
- regional pools;
- Shadow entry/exit pools;
- policy versions;
- safe rollout controls.

Remote metadata must be authenticated/signed according to the chosen architecture.

Do not create an unsigned remote switch capable of disabling client security guarantees.

## 3.0.3 Capacity-aware Smart Connection

Extend Smart Connection beyond local latency alone.

Where trustworthy control-plane data exists, selection may account for:

- capacity/load bucket;
- maintenance state;
- regional availability;
- path capability;
- client-observed health;
- current network conditions.

Do not route every user to one low-latency overloaded node.

Client-side truthful health remains important; server-reported metadata is not a substitute for connection reality.

## 3.0.4 Fleet management

Build operational concepts for managing node pools safely:

- add node;
- drain node;
- maintenance;
- disable broken capability;
- gradual restore;
- region/pool membership;
- Shadow entry/exit role.

Changes should propagate through authoritative metadata without requiring an APK release for ordinary fleet operations.

## 3.0.5 Device management

Allow entitlement/backend to understand authorized devices where the subscription model requires it.

Potential capabilities:

- device registration;
- device limit enforcement;
- revoke old device;
- rename device;
- last-active coarse metadata where privacy policy allows.

Do not collect hardware identifiers unnecessarily.

Use generated scoped device identifiers/tokens where practical rather than invasive fingerprinting.

## 3.0.6 Optional account sync

If accounts are introduced, keep them product-justified and privacy-conscious.

Possible synced data:

- entitlement/account state;
- authorized devices;
- selected high-level preferences;
- subscription metadata.

Do not require an account solely to collect identity data if anonymous/token-based access remains sufficient.

Do not sync sensitive VPN secrets without an explicit secure design.

## 3.0.7 Signed remote configuration

Allow limited remote policy/config evolution without APK release, for example:

- server weights;
- maintenance flags;
- supported capability rollout;
- feature rollout flags;
- minimum backend protocol version.

Guardrails:

- authenticated/signed source;
- schema versioning;
- safe defaults;
- unknown fields tolerated where appropriate;
- invalid signature fails closed/safely;
- remote config cannot silently disable certificate verification, DNS leak protection, IPv6 policy or other release-critical guarantees.

## 3.0.8 Feature flags and staged rollout

Support controlled rollout for risky non-security-critical features.

Requirements:

- deterministic assignment where needed;
- kill switch for broken optional feature;
- no dependency on a remote flag for base VPN safety;
- offline-safe defaults;
- telemetry respects privacy policy.

## 3.0.9 Backend/API versioning

Define explicit API compatibility/version behavior for:

- commerce/entitlement;
- manifest/control plane;
- device management;
- update metadata.

Old clients must fail gracefully when a new backend feature is unavailable.

Do not create silent semantic changes under the same field that break older APKs.

## 3.0.10 Resilience and offline behavior

Define what remains functional during partial backend outage.

Examples:

- existing valid entitlement can continue within explicit expiry/offline policy;
- cached signed server metadata may remain usable within TTL;
- manual subscriptions remain available where supported;
- billing outage does not fabricate entitlement;
- control-plane outage does not automatically erase a known-good local server set unless security policy requires it.

## 3.0.11 Privacy architecture review

Before mature-platform closure, review data inventory:

- what client data is stored locally;
- what backend data is stored;
- retention;
- identifiers;
- diagnostics;
- payment linkage;
- device linkage;
- server logs under our control.

Minimize collection to what is operationally/product necessary.

## 3.0.12 Security architecture review

Review:

- token scopes;
- rotation/revocation;
- backend authorization boundaries;
- signed manifest/control-plane metadata;
- device registration abuse;
- update integrity;
- build/signing pipeline;
- remote-config authority limits;
- secret redaction;
- least privilege.

## 3.0.13 Migration strategy

3.0 architecture changes must include migration from existing 2.x state.

Requirements:

- preserve valid entitlement;
- preserve manual subscriptions where compatible;
- preserve AUTO/manual selection intent;
- preserve routing/privacy settings where compatible;
- safe defaults for new Autopilot/Shadow fields;
- rollback-safe schema handling where practical.

## 3.0.14 Operational observability

Expand privacy-safe service observability sufficiently to answer:

- is a region unhealthy?;
- is a transport failing?;
- did a release increase connection failures?;
- is billing failing?;
- are update manifests failing verification?;
- are Shadow fallbacks spiking?;

Do this without collecting user browsing content or secrets.

## 3.0.15 Required engineering validation

Automated/integration coverage should include:

- engine/UI process state synchronization;
- engine restart/failure state;
- signed control-plane metadata verification;
- invalid/expired metadata;
- capacity/maintenance-aware selection;
- device registration/revocation logic;
- device limit behavior;
- API version compatibility;
- offline cached metadata policy;
- entitlement continuity rules;
- feature-flag safe defaults;
- remote-config authority restrictions;
- migration from current 2.x persisted state;
- secret/redaction checks;
- update/control-plane failure scenarios.

## 3.0 exit gate

`implementation + CI + automated/integration validation + final [hotfox-review] + P0=0/P1=0`

→ `3.0 ENGINEERING COMPLETE`

Engineering roadmap development is then complete.

Do NOT claim `RELEASE READY` yet if the final physical Android acceptance suite has not been performed.

---

# FINAL RELEASE DEVICE GATE

Status: **OPEN AFTER 3.1 ENGINEERING COMPLETE — runtime and physical execution NOT STARTED**

This is the single canonical stage at which physical validation on real Android hardware is required for `RELEASE READY`.

Until this gate is passed, engineering stages may be complete, but production release acceptance is not complete.

## R1. Real-device install/update

Validate the actual release candidate artifact on supported real Android hardware.

At minimum cover the project's supported Android range with representative devices when available, including API-24 compatibility expectations and a modern Android version.

Validate:

- installation;
- update from supported previous version;
- launch;
- required permission flow;
- foreground service/notification behavior.

## R2. Real VPN traffic proof

Validate the real production pipeline on device.

Evidence should include, as appropriate:

- traffic actually traverses VPN;
- external IP changes as expected;
- internet works through the tunnel;
- disconnect restores expected normal networking;
- no false `Защищено` state.

A VPN icon alone is not proof.

## R3. DNS leak acceptance

Perform real-device DNS leak validation appropriate to the selected routing mode/policy.

## R4. IPv6 leak acceptance

On networks/devices where IPv6 is available, verify IPv6 behavior matches the product policy and does not silently bypass protection.

## R5. Network handoff acceptance

Exercise real Wi-Fi/cellular transitions, network loss/restore and relevant Autopilot behavior.

## R6. Smart Connection / Shadow acceptance

Exercise representative AUTO selection/failover and, where infrastructure permits, at least one real alternate/Shadow recovery path.

## R7. Commercial release flow acceptance

For the release environment or approved production-like environment, validate the end-user commercial journey with real backend authority semantics:

- plan;
- order;
- checkout;
- authoritative verification;
- entitlement;
- app recovery/sync.

Do not expose production secrets during testing.

## R8. Lifecycle acceptance

Exercise:

- background/foreground;
- process recreation where practical;
- reboot/restore behavior according to configured policy;
- rapid connect/disconnect;
- permission denial/revocation behavior;
- notification actions;
- Quick Settings Tile if shipped.

## R9. Release verdict

Only after required real-device acceptance is actually performed and blocking findings are resolved may the project claim:

`RELEASE READY`

Never fabricate this state.

If the owner chooses to ship a release candidate before completing all planned 3.0 platform work, the owner may explicitly cut a release branch and invoke this final device gate for that release candidate. That decision is separate from the sequential engineering roadmap and must be explicit.

---

# CURSOR EXECUTION CONTRACT

1. Read this roadmap before starting a new roadmap phase.
2. Work only on the current phase plus required regressions/fixes from earlier phases.
3. Do not implement future-phase scope merely because it is described here unless it is a small prerequisite necessary for correct architecture.
4. Do not report future unimplemented roadmap items as current P0/P1 defects.
5. At current phase completion, run one final `[hotfox-review]`.
6. If final review has P0=0 and P1=0, mark the phase `ENGINEERING COMPLETE`.
7. Immediately move to the next canonical phase without asking the owner for a physical Android test.
8. Keep physical device validation deferred to the final release device gate.
9. Continue to report truthfully which validations were automated/integration versus not physically validated.
10. Do not claim `RELEASE READY` until the final release device gate actually passes.
11. Preserve prior phase history and existing working paths.
12. Never sacrifice truthful VPN behavior, payment authority, leak protection or secret handling to make roadmap progress appear faster.
