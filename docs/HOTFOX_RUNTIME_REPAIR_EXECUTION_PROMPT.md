# HOTFOX RUNTIME REPAIR — AUTONOMOUS CURSOR EXECUTION PROMPT

## ROLE AND MISSION
You are the implementation owner for HotFox Proxy in M1ST7K/VPN. Continue autonomously on this branch until the repair acceptance gates below are satisfied or a genuinely external blocker exists. Do not stop after analysis, planning, one commit, one review round, or one green compile. Implement, test, inspect failures, fix them, rerun gates, request/re-run review, and repeat.

The owner has explicitly opened the FINAL RELEASE VALIDATION / runtime repair gate for the defects below. Previous text saying runtime/emulator validation was deferred must no longer be used as a reason to no-op. Do not declare RELEASE READY until the specified runtime evidence exists.

Never print, commit, upload, echo, snapshot, or place in logs any subscription URL, credentials, tokens, UUIDs, private keys, API keys, server secrets, or user-specific configuration. Use synthetic fixtures and repository/CI secrets only through existing secret-safe interfaces.

## SOURCE OF TRUTH / IMPORTANT DISTINCTION
There are two classes of findings. Do not confuse them:

A. REAL PRODUCT/RUNTIME DEFECTS already found in the reconstructed HotFox code:
1. P0 — native Xray sockets are not reliably protected from re-entering the VPN TUN. HotFox changed upstream self-package exclusion and relies on process network binding, while Go/Xray sockets can bypass the libc mechanism. This can create the exact symptom: VPN/TUN exists but HTTPS fails / internet disappears / public IP does not change.
2. P1 — ConnectivityManager.bindProcessToNetwork(Network) Boolean result is ignored; false is currently reported as success.
3. P1 — outbound validation incorrectly compares container ConfigType (CUSTOM/POLICYGROUP/PROXYCHAIN) with the actual Xray network protocol, rejecting valid composite/full-JSON profiles before Xray starts.
4. P1 — routing policy has split sources of truth. New HotfoxRoutingStore mode and legacy PREF_SMART_ROUTING_MODE can disagree; GLOBAL may retain direct .ru/.su/.рф/local rules; generated rules may reference nonexistent literal tags proxy/direct/block instead of actual provider tags.

B. AUDIT/ENVIRONMENT BLOCKERS from a separate Windows QA run:
- target SHA could not be proven because that local E:\VPN-main snapshot was not a git clone and git.exe was absent;
- Android production source/bootstrap was absent in that local snapshot;
- no SHA-tied candidate APK was installed;
- therefore thousands of emulator cases were BLOCKED/NOT EXECUTED.
These are NOT evidence that the GitHub branch lacks the reconstructable app and are NOT substitutes for fixing A. In GitHub CI the app is reconstructed from upstream + pinned HotFox payload/overlay. Preserve that model unless deliberately improving it.

## NON-NEGOTIABLE SAFETY / ANTI-REGRESSION RULES
1. Never make CONNECTED/Protected a cosmetic state. CONNECTED may be published only after the service has evidence that the required datapath is alive according to the existing state machine and the strengthened checks below.
2. Never bypass, delete, weaken, catch-and-ignore, or hardcode a validation merely to make tests green.
3. Never disable TLS verification, certificate validation, routing validation, outbound validation, VPN permission checks, socket protection, DNS checks, or fail-closed behavior.
4. Never solve a failing test by changing the expected value unless the old expectation is demonstrably wrong and the production invariant is documented.
5. Preserve package id com.hotfox.vpn, minSdk 24, current supported ABIs, HotFox UI/visual design, subscription UX, existing routing features, entitlement behavior, and bootstrap reproducibility unless a change is required for correctness.
6. Do not regress upstream v2rayNG behavior without an explicit HotFox reason and regression coverage.
7. No secrets in source, test fixtures, workflow logs, artifacts, screenshots, comments, or diagnostics.
8. All new native/library binaries must be reproducibly tied to pinned source/revision and integrity metadata. Do not introduce opaque untracked AAR/SO blobs.
9. Every fix must have regression tests before the task is considered complete.
10. Prefer small reviewable commits grouped by defect, but continue automatically through the whole plan.

## PHASE 0 — ESTABLISH PROVENANCE AND BASELINE
Before editing product code:
- fetch refs and record current branch HEAD, origin/main, merge-base, dirty status;
- inspect bootstrap/bootstrap_source.sh and verify pinned upstream/payload hashes;
- reconstruct the exact Android tree exactly as CI does;
- identify all effective versions: v2rayNG, AndroidLibXrayLite, Xray-core, Kotlin/Gradle/AGP, compile/target/min SDK;
- run the existing unit tests, lint, assembleDebug, and unsigned release compile once to establish baseline;
- inspect existing HotFox AI review findings and do not reopen already-fixed issues unless the runtime repair invalidates them.
If reconstruction fails, fix reconstruction first. Do not proceed against a guessed or orphan APK.

## PHASE 1 — P0: GUARANTEE NATIVE XRAY SOCKET ESCAPE FROM TUN
This is the highest priority. Trace the real path end-to-end:
Android app traffic -> VpnService TUN -> HEV/tun2socks -> local Xray SOCKS -> Xray outbound socket -> physical/default network -> VPN server -> Internet.

Required engineering outcome: every Xray outbound TCP and UDP socket that must reach the remote network must be excluded/protected from the HotFox TUN before connect / first packet, or an equivalently robust architecture must prove that it cannot re-enter the TUN.

Do not assume bindProcessToNetwork alone protects Go-created sockets. Inspect the pinned Xray/Go Android behavior and AndroidLibXrayLite callback surface. Implement one robust strategy and document why it works.

Preferred strategy when feasible:
- add/extend a pinned source-controlled AndroidLibXrayLite/Xray integration that exposes the outbound socket fd before connect/send;
- bridge Go/native -> Android callback -> active VpnService.protect(fd);
- if required, bind that socket to the selected underlying Network using an fd/socket-specific mechanism rather than only process-wide binding;
- TCP and UDP both must be covered;
- protection/binding failure must be fail-closed: abort that outbound connection and propagate a meaningful HotFox diagnostic; never log success after failure;
- callback lifetime must be race-safe across connect/disconnect/reconnect and process/service teardown;
- no callback may retain a dead Activity/Service instance;
- validate all supported ABIs and packaging.

If the architecture instead restores upstream self-UID/package exclusion, prove that this does not invalidate HotFox's datapath health checks. Move health probing to an appropriate UID/path or otherwise design a check that proves tunneled user traffic, not merely direct traffic from the excluded VPN process. Do not call direct self-UID HTTPS proof of VPN success.

Add tests for:
- protect success;
- protect false;
- protect exception;
- TCP and UDP;
- stale/dead VpnService callback;
- network switch while dialing;
- reconnect after teardown;
- no recursive capture of Xray outbound;
- no false CONNECTED when protection fails.

Add diagnostics that identify stage and reason without exposing host credentials/config secrets. Preserve or extend HF-VPN-* codes consistently.

## PHASE 2 — P1: FIX bindProcessToNetwork TRUTHFULNESS
In VpnLoopPrevention and callers:
- capture the actual Boolean returned by ConnectivityManager.bindProcessToNetwork(underlying);
- diagnostic bindOk must equal the actual result;
- return the actual result;
- false is a failure, not success;
- exceptions remain failures;
- disappearing/stale Network is a failure;
- ensure cleanup unbinds/reset process network as appropriate and cannot leave later non-VPN app/service work pinned accidentally.

Regression matrix: true / false / SecurityException-or-runtime exception / stale network / network becomes unavailable / reconnect / disconnect cleanup.

## PHASE 3 — P1: FIX PROFILE/OUTBOUND SEMANTICS WITHOUT WEAKENING DRIFT DETECTION
The invariant is: compare the selected logical profile/plan with the actual generated Xray outbound(s). ConfigType is a container/model type, not necessarily a network protocol.

Implement semantic comparison:
- ordinary single-node VLESS/VMess/Trojan/SS/SOCKS/etc: normalized selected node vs corresponding generated outbound;
- CUSTOM/full JSON: derive expected outbound semantics from the supplied JSON itself; do not compare string CUSTOM to vless/vmess/etc;
- POLICYGROUP: validate the resolved group/selection plan and its actual outbound members/tags;
- PROXYCHAIN: validate ordered chain semantics and each required hop, not the word PROXYCHAIN against a network protocol;
- preserve checks for protocol, endpoint/address, port, transport, TLS/REALITY/security parameters, identifiers/keys only via safe equality without logging secret values, and any other connection-critical fields;
- normalize aliases/defaults/case only where Xray semantics make them equivalent;
- real mismatches must still fail closed with useful field names but secret-safe values.

Regression fixtures must include: valid VLESS, invalid port, valid CUSTOM VLESS JSON, valid CUSTOM with multiple outbounds, valid POLICYGROUP, valid PROXYCHAIN, deliberate protocol mismatch, deliberate address/port mismatch, TLS/REALITY mismatch, malformed JSON, missing required outbound.

## PHASE 4 — P1: ONE CANONICAL ROUTING POLICY + REAL TAG RESOLUTION
Eliminate split-brain policy generation.

Create/propagate one immutable canonical routing snapshot for a connection attempt. Every generator/injector must consume that same snapshot. Do not independently reread legacy preferences after the snapshot is created. Migrate legacy PREF_SMART_ROUTING_MODE carefully if compatibility requires it, but it must not override the canonical current selection.

Resolve actual outbound/balancer tags from the final Xray configuration before emitting routing rules. Never assume literal proxy/direct/block exists.

Required invariants:
- every outboundTag referenced by routing exists in final outbounds;
- every balancerTag referenced exists in final balancers;
- identify proxy/direct/block semantics by actual resolved role/protocol/tag mapping, not only literal tag equality;
- GLOBAL means no unintended legacy direct routes (.ru, .su, .рф, local) unless an explicit current user/system exclusion requires them;
- SMART preserves intended split-routing behavior;
- per-app INCLUDE/EXCLUDE semantics remain correct and do not accidentally capture/protect HotFox/Xray in a loop;
- user/provider custom tags such as provider-proxy, hotfox-direct, hotfox-block work;
- provider-authored routing is preserved only where compatible with the selected HotFox mode and explicit documented precedence;
- no rule may silently point to a nonexistent outbound.

Add a final-config validator that runs before core start and rejects dangling outboundTag/balancerTag references with a secret-safe error.

Regression tests: GLOBAL with legacy SMART pref present; GLOBAL with .ru/.su/.рф; SMART; DIRECT/BLOCK rules; custom provider tags; missing proxy tag; missing direct tag; balancer; app include/exclude; imported full JSON with provider routing; transition SMART->GLOBAL->SMART; process restart with persisted state.

## PHASE 5 — CONNECTION STATE MUST REPRESENT REALITY
Audit the complete connection state machine. A created TUN, running service, running Xray process, or successful local SOCKS accept alone is insufficient for CONNECTED.

Define explicit stages such as PREPARING, TUN_CREATED, XRAY_STARTED, SOCKS_READY, HEV_STARTED, DATAPATH_VALIDATING, CONNECTED, DISCONNECTING, ERROR. Keep UI mapping compatible with HotFox design.

Before CONNECTED, require the strongest deterministic secret-safe datapath validation practical in automated tests. At minimum ensure:
- Xray core running;
- local SOCKS reachable;
- HEV/tun path active;
- no detected native-socket loop/protection failure;
- an HTTPS request that is intended to traverse the VPN datapath succeeds;
- when an IP echo endpoint is used in integration/E2E, compare before/during and require expected change where server topology permits; never embed credentials.
On validation failure, surface ERROR/DISCONNECTED and tear down safely; never leave UI Protected.

## PHASE 6 — DNS / IPV6 / LEAK AND DISCONNECT CORRECTNESS
After datapath repairs, test and fix:
- DNS A/AAAA through intended policy;
- Android Private DNS modes supported by project expectations;
- IPv6: either correctly tunnel it or fail closed/block it when unsupported; never silently leak;
- no DNS leak caused by route/tag mismatch;
- disconnect restores ordinary networking;
- failed connect restores networking;
- repeated reconnect does not leave stale TUN/routes/process network binding;
- Wi-Fi/network changes trigger correct rebinding/reconnect behavior.

## PHASE 7 — BUILD/CI/EMULATOR GATE MUST BE SHA-TIED
Repair the audit provenance gap by making validation reproducible from GitHub HEAD, not by testing an orphan APK.

For the exact final candidate SHA:
1. canonical bootstrap reconstruction succeeds;
2. unit tests pass;
3. lint passes (do not blanket-disable lint);
4. assembleDebug passes;
5. unsigned release compile passes;
6. produce debug APK artifact;
7. record candidate commit SHA and APK SHA-256 in CI evidence/metadata;
8. install exactly that APK on the emulator;
9. verify installed package/version corresponds to candidate;
10. capture secret-safe logcat and test evidence.

If emulator infrastructure is unavailable in GitHub-hosted CI, do not fabricate PASS. Mark the runtime gate BLOCKED with exact reason while keeping all host-executable gates green. If a configured emulator job exists, run it.

## PHASE 8 — EMULATOR REGRESSION CATALOG
Execute all feasible mandatory emulator cases from the project's QA catalog, prioritizing runtime truth over pixel trivia:
- install/launch/no immediate FATAL/ANR;
- permission accept/cancel;
- valid/invalid/malformed profile import;
- single-node and CUSTOM/POLICYGROUP/PROXYCHAIN starts;
- GLOBAL/SMART/app routing;
- connect/disconnect;
- 30 reconnect cycles minimum;
- process death during connect and connected state;
- network loss/restore;
- stale underlying Network;
- forced Xray/HEV failure;
- DNS/IPv6 cases feasible on emulator;
- UI state never says Protected when datapath failed;
- font scale/TalkBack/basic accessibility if automation supports it;
- memory/process cleanup after cycles.

Do not turn NOT EXECUTED into PASS. Device-only cases remain explicitly device-only.

## PHASE 9 — STATIC / SECURITY REGRESSION AUDIT
Scan effective reconstructed production source, not only overlay docs, for:
- TODO/FIXME in critical VPN paths;
- mock/fake hardcoded CONNECTED;
- TrustAll / permissive HostnameVerifier / TLS bypass;
- secrets or subscription URLs;
- accidental verbose logging of configs;
- dangerous broad exception swallowing;
- lifecycle leaks in Service/callback/network references;
- QUERY_ALL_PACKAGES/CAMERA permissions: retain only if justified by an actual HotFox feature; otherwise remove carefully and test impacted flows.
Do not publish raw secret scan matches.

## PHASE 10 — REVIEW/FIX LOOP
After implementation:
- run HotFox AI Review against the actual final diff;
- any P0 or P1 means NOT DONE;
- fix every valid P0/P1, add regression coverage, rerun full relevant gates, then request another review round;
- repeat until P0=0 and P1=0;
- P2/P3 must be triaged explicitly; fix regressions/security/correctness issues now, defer only genuinely non-release-blocking items with rationale;
- after any post-review code change, invalidate earlier runtime evidence if the changed code can affect it and rerun the affected tests.

## REQUIRED COMMIT DISCIPLINE
Suggested sequence (adapt if needed):
1. fix(vpn): protect native Xray sockets from TUN recursion
2. fix(vpn): honor underlying network bind failures
3. fix(config): validate composite profiles semantically
4. fix(routing): unify canonical policy and resolve real outbound tags
5. test(vpn): add datapath and routing regression matrix
6. ci(vpn): produce SHA-tied APK and runtime evidence
7. fix(review): address HotFox AI Review P0/P1 findings
Do not squash away useful provenance while actively debugging unless repository policy requires it.

## DEFINITION OF DONE — ALL MUST BE TRUE
Engineering fix is complete only when:
- native Xray TCP/UDP outbound cannot recursively re-enter HotFox TUN, with tests/evidence;
- bindProcessToNetwork false is treated as failure;
- valid CUSTOM/POLICYGROUP/PROXYCHAIN are no longer rejected because of container-type/protocol confusion;
- GLOBAL has no unintended legacy direct .ru/.su/.рф routes;
- generated routing contains zero dangling outbound/balancer tags;
- existing single-node profiles still work;
- CI unit/lint/debug/release-compile gates pass;
- a debug APK is produced and cryptographically tied to final candidate SHA;
- feasible emulator runtime tests pass and no false CONNECTED occurs;
- HotFox AI Review reports P0=0/P1=0 on the final relevant code;
- documentation states exactly what was executed vs deferred;
- no secrets were exposed.

RELEASE READY is a stricter label. Do not set it solely from host CI or emulator success if the roadmap still requires physical-device acceptance. Physical-device items (real cellular handover, OEM battery/background managers, carrier IPv6, OEM VPN icon/notification behavior, hardware-backed keystore nuances, thermal/endurance) must remain an explicit final device gate until actually executed.

## AUTONOMY / STOP CONDITIONS
Continue automatically. Do not ask the owner for confirmation between phases. Do not stop because one review says APPROVED if runtime acceptance is still unexecuted. Do not stop because compilation is green. Do not stop after generating a report.

You may stop only when either:
A) all engineering/emulator gates above that are possible in the available infrastructure are complete, final review is P0=0/P1=0, artifact/evidence is published, and remaining device-only work is explicitly isolated; or
B) a truly external blocker prevents progress (for example unavailable required infrastructure or credential-controlled server access). In case B, first complete every task that does not depend on the blocker, then leave one concise blocker report containing exact completed SHA, passing gates, failing/blocked gate, evidence path, and the smallest owner action required. Never request or expose secret values in that report.

Start now with provenance + reconstruction, then P0 socket protection. Treat the observed user symptom — VPN appears to connect but does not proxy traffic/change public IP and can lose internet — as the primary release-blocking behavior until disproven by SHA-tied runtime evidence.