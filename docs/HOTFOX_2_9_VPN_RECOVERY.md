# HotFox Proxy 2.9 — VPN Core Recovery / Real Connection Fix

Status: **PLANNED — starts only after 2.8 ENGINEERING COMPLETE**

Owner decision: this phase is inserted before Premium UI because real E2E testing exposed a production-path failure that must be fixed before visual pre-release work continues.

The previous Premium Android Experience phase is shifted to **3.0**. The previous Mature HotFox Platform phase is shifted to **3.1**. No previous scope is deleted.

---

# 1. WHY 2.9 EXISTS

HotFox currently has evidence that major pieces of the Android VPN pipeline start successfully, but the complete protected path does not return working Internet traffic.

The latest runtime acceptance evidence established the following baseline:

- APK installs and app starts;
- a real HTTPS subscription can be imported through runtime input;
- 8 valid servers were parsed in that test;
- server selection works;
- `CoreVpnService` starts;
- Android creates a VPN network and `tun0`;
- `tun0` was observed UP with IPv4/IPv6 routes;
- Xray 26.6.27 starts;
- HEV/tun2socks starts;
- HEV targets local SOCKS `127.0.0.1:10808`;
- Xray logs accept TCP/UDP requests from the local path;
- the truthful UI does not fabricate `CONNECTED` after readiness failure;
- teardown removes the stale TUN after failure.

But the same test also established:

- DNS through the TUN timed out;
- HTTP/HTTPS through the TUN timed out;
- an Xray HTTP-egress probe through local `127.0.0.1:10809` timed out;
- no successful browser traffic through VPN was proven;
- no `IP_DURING` was measured;
- `TRAFFIC PROXIED` failed;
- runtime evidence for outbound socket protection / loop prevention was incomplete;
- the generated selected outbound was not safely compared field-by-field with the working subscription data.

This failure was observed as the HotFox readiness error category previously reported as `HF-VPN-006 / tun-not-forwarded`.

**Important:** `tun-not-forwarded` is a symptom/category, not yet a proven technical root cause.

---

# 2. EXTERNAL REFERENCE FACT

The owner confirms that the same VPN subscription/server access works in another compatible VPN client.

Treat that as strong evidence that:

- the subscription is not generally invalid;
- at least one server in it is operational;
- the credentials/remote service can produce working VPN traffic in a correct client implementation.

Therefore do not close the investigation with `server unavailable`, `subscription broken`, or `remote endpoint issue` unless a controlled comparison actually proves that the specific selected endpoint has become unavailable during the HotFox test window.

The primary suspect scope is HotFox client behavior:

`subscription -> parser/model -> generated Xray config -> core inbound/outbound -> protect/bind -> TUN -> HEV -> DNS/routing -> Android app traffic`.

---

# 3. PRIMARY 2.9 GOAL

The phase answers one concrete question:

> Can a normal user install HotFox, import a known-working subscription, select a server, press Connect, and use Android Internet traffic through that VPN, then disconnect and reconnect reliably?

2.9 is not complete until the answer is:

`YES — VERIFIED BY REAL E2E IN THE AVAILABLE ENGINEERING RUNTIME`.

Build success alone is not acceptance.

---

# 4. KNOWN DEFECTS TO RECHECK AND FIX

Earlier E2E identified concrete implementation defects/candidates. Revalidate them against the current code because phases 2.4–2.8 may change relevant classes.

## 4.1 Xray detection must not depend incorrectly on branding

Earlier evidence showed a helper equivalent to:

`Utils.isXray() = applicationId.startsWith("com.v2ray.ang")`

which evaluates false for the HotFox package:

`com.hotfox.vpn`.

That is architecturally dangerous because core capability must not silently change only because the app was rebranded.

At 2.9:

- find all callers of `isXray()` or equivalent feature detection;
- determine which inbound/config/subscription/readiness branches it controls;
- prefer capability/build/core detection over package-name detection;
- if a minimal compatibility fix is needed, explicitly support HotFox without regressing upstream-compatible paths;
- add focused tests.

## 4.2 Subscription refresh must not rely on a dead local HTTP proxy

Earlier runtime evidence showed an initial subscription refresh trying `127.0.0.1:10809` while the local HTTP proxy was not ready, causing a security/connection exception before direct fallback.

Normal flow must not use an expected exception as its readiness mechanism.

Required behavior:

- if the local proxy is actually ready and policy requires it, use it;
- otherwise use the correct direct underlying-network path;
- preserve HTTPS certificate validation;
- do not expose the subscription secret.

## 4.3 Server list refresh after share/URL import

Earlier runtime evidence showed that after a URL/share import the server list could remain stale until process restart.

After import completion, observable UI state must refresh without force-stop/restart.

Use the existing state/event/lifecycle architecture rather than adding a polling loop.

## 4.4 Subscription title presentation

Earlier runtime evidence showed a generic imported name instead of available subscription profile metadata.

This is lower priority than the VPN datapath. Fix only after the blocker is under control, and never use a secret URL/token as a display title.

---

# 5. ROOT-CAUSE ISOLATION STRATEGY

Do not debug the entire stack as one black box.

Use progressively wider tests:

`A. physical/underlying Internet baseline`
→ `B. selected remote endpoint reachability`
→ `C. subscription semantic model`
→ `D. generated Xray outbound`
→ `E. Xray through local SOCKS with NO VPN/TUN/HEV`
→ `F. local HTTP inbound if it is part of the product path`
→ `G. protect()/underlying-network runtime proof`
→ `H. full TUN + HEV + SOCKS + Xray path`
→ `I. DNS`
→ `J. browser/device-wide traffic`
→ `K. disconnect/reconnect stability`.

The first failing layer is the primary investigation boundary.

---

# 6. TEST A — UNDERLYING NETWORK BASELINE

With HotFox VPN OFF, prove from the Android emulator/device environment:

- DNS works;
- HTTPS works;
- the subscription host is reachable;
- a public-IP endpoint returns a value;
- browser networking works.

Store sanitized baseline evidence.

If underlying Internet is broken, do not misclassify that as a VPN core defect.

---

# 7. TEST B — REMOTE ENDPOINT REACHABILITY

For the selected known-working node, derive without exposing credentials:

- protocol;
- hostname/IP;
- port;
- transport;
- security mode.

Prove DNS resolution and TCP reachability from the underlying network where meaningful.

Do not equate a generic TLS handshake with a valid VLESS/Reality handshake.

---

# 8. SUBSCRIPTION -> INTERNAL MODEL COMPARISON

For at least one node known to work in the other client, create a sanitized semantic snapshot from the subscription data.

Compare field-by-field with HotFox's internal model.

Fields as applicable include:

- protocol;
- address;
- port;
- network/transport;
- security;
- flow;
- SNI/serverName;
- fingerprint;
- ALPN;
- Reality enabled;
- public key PRESENT/MISSING;
- shortId PRESENT/MISSING;
- path;
- host/authority;
- gRPC serviceName;
- XHTTP mode/extra values;
- packetEncoding;
- mux;
- IPv4/IPv6 preferences.

Never print UUID/password/token/private key values. Represent them as `[REDACTED]` or `PRESENT/MISSING`.

If source data and internal model differ semantically, classify it as a parser/model bug and fix there before touching TUN/HEV.

---

# 9. INTERNAL MODEL -> GENERATED XRAY CONFIG COMPARISON

Produce a sanitized structural view of the generated selected outbound.

Compare semantic fields from the internal model to the config actually given to the embedded Xray core.

Check, where applicable:

- outbound protocol;
- address/port;
- flow;
- encryption/security;
- `streamSettings.network`;
- TLS/Reality settings;
- `serverName`/SNI;
- fingerprint;
- publicKey presence;
- shortId presence;
- WS settings;
- gRPC settings;
- XHTTP settings;
- socket options;
- DNS/routing dependencies.

If model is correct but generated config differs, fix config generation rather than compensating later in the pipeline.

---

# 10. EXACT EMBEDDED XRAY COMPATIBILITY

Earlier evidence reported Xray **26.6.27**. At 2.9, detect and record the actual embedded version used by the tested APK.

Validate generated config against that exact version, particularly for:

- VLESS;
- Reality;
- Vision flow;
- XHTTP;
- gRPC;
- packet encoding;
- security/transport schema changes.

Do not use outdated V2rayNG/Xray mapping assumptions merely because older upstream code compiled.

---

# 11. TEST E — XRAY OUTBOUND WITHOUT TUN

This is a decisive isolation test.

Start the selected Xray configuration with its local SOCKS inbound, but without establishing Android `VpnService`, TUN, or HEV.

Then perform real HTTPS through the SOCKS inbound, expected earlier at:

`127.0.0.1:10808`

The test must prove:

`local test client -> SOCKS -> Xray -> selected remote node -> Internet -> response`.

Use a debug/instrumentation client if Android shell tooling cannot do SOCKS correctly.

Test at minimum two independent HTTPS endpoints such as:

- `https://api.ipify.org`;
- `https://example.com`.

Record response success and public IP where available.

### Interpretation

If this SOCKS-only test FAILS:

Do not modify TUN/HEV first.

Investigate:

- subscription parsing;
- selected-node mapping;
- generated Xray config;
- transport/security fields;
- Reality/Vision/XHTTP/gRPC compatibility;
- remote endpoint reachability;
- Xray core behavior.

If this SOCKS-only test PASSES:

The remote node + generated Xray outbound are proven viable, so shift focus to:

- Android VPN routing;
- socket/process protection;
- underlying network binding;
- HEV/TUN forwarding;
- DNS path;
- response routing.

---

# 12. DO NOT CONFUSE 10808 AND 10809

Earlier runtime evidence showed:

- HEV targeting SOCKS `127.0.0.1:10808`;
- a separate Xray HTTP egress/proxy probe using `127.0.0.1:10809`.

These are different inbounds.

A failure on HTTP 10809 alone does not prove that the production SOCKS path used by HEV is broken.

2.9 must test them separately.

If SOCKS works and HTTP inbound fails, fix the HTTP-inbound/readiness/subscription branch without blaming the remote VPN path.

---

# 13. STARTUP ORDER AND READINESS BARRIERS

Verify actual runtime ordering.

The safe conceptual order is:

1. establish current connection intent/generation;
2. prepare underlying network / protection callbacks;
3. generate selected Xray config;
4. start Xray;
5. prove local SOCKS listener is ready;
6. establish/obtain TUN fd as required by implementation;
7. start HEV with the correct TUN fd and SOCKS target;
8. establish routing/DNS state;
9. run bounded readiness verification;
10. only then emit protected state.

Do not use arbitrary sleeps as synchronization when an actual readiness condition exists.

Ensure HEV cannot race ahead of an unbound local SOCKS port.

---

# 14. SOCKET PROTECT / LOOP PREVENTION — RUNTIME PROOF

The source may contain `VpnService.protect()` or a process/network binding mechanism. Source presence is not enough.

Instrument debug builds safely if necessary and prove runtime behavior.

Record non-secret diagnostics such as:

- protect callback invoked YES/NO;
- call count;
- success/failure count;
- network binding success/failure;
- selected underlying network identity in non-sensitive form;
- ordering relative to core connection attempts.

A protected outbound must conceptually follow:

`Xray remote socket -> protect/bind -> physical underlying network -> VPN server`.

It must not become:

`Xray -> tun0 -> HEV -> SOCKS -> Xray -> tun0 ...`.

If a routing feedback loop exists, it is a **BLOCKER**.

Do not claim `ROUTING LOOP: ABSENT` based only on reading a helper function.

---

# 15. UNDERLYING NETWORK

Verify the real interaction with Android `ConnectivityManager` and any use of:

- `activeNetwork`;
- `setUnderlyingNetworks()`;
- `bindProcessToNetwork()`;
- socket protection/binding callbacks.

Initialization order matters.

Prove that the remote Xray connection has a viable path outside the VPN TUN once full-tunnel routes are active.

---

# 16. TUN FD OWNERSHIP AND LIFETIME

Inspect the actual `ParcelFileDescriptor` / raw fd lifecycle.

Check:

- when the TUN is established;
- whether fd is detached/duplicated;
- who owns closing it;
- whether HEV receives the live fd;
- whether any premature close occurs;
- whether reconnect can accidentally reuse a stale fd.

Add diagnostics/tests where practical.

---

# 17. HEV CONFIGURATION

Capture a sanitized HEV configuration and compare it to runtime Xray inbounds.

Check:

- TUN parameters;
- MTU;
- IPv4/IPv6 addresses;
- SOCKS host;
- SOCKS port;
- UDP mode;
- TCP mode;
- timeout/lifecycle behavior.

The actual HEV SOCKS target must exactly match the listening Xray SOCKS inbound.

---

# 18. DNS IS A SEPARATE LAYER

Earlier test showed UDP DNS timeout through the TUN. Do not automatically make DNS the primary root cause before Xray TCP/HTTPS is isolated.

Use a matrix:

- Xray SOCKS HTTPS: PASS/FAIL;
- TUN HTTPS: PASS/FAIL;
- TUN UDP DNS: PASS/FAIL;
- TUN TCP DNS: PASS/FAIL;
- DoH: PASS/FAIL/N/A.

If Xray SOCKS HTTPS is healthy and TUN HTTPS works but DNS fails, then debug the DNS layer specifically.

Check the actual Xray/Android DNS policy, including any port-53 routing/hijack behavior if used.

Do not introduce FakeDNS or plaintext fallback without explicit architecture justification.

---

# 19. IPV4 / IPV6

The earlier VPN network exposed both IPv4 and IPv6 default routes.

Validate address-family behavior independently.

A broken IPv6 path must not stall a viable IPv4 protected connection where policy permits IPv4 fallback.

IPv6 must never silently bypass protection.

---

# 20. MTU

Earlier TUN MTU evidence was 1500.

Do not randomly change MTU to make tests green.

Only investigate MTU when evidence suggests packet-size/fragmentation behavior, for example:

- small requests work but larger TLS transfers stall;
- PMTU/fragmentation evidence exists.

If tested, compare controlled values and record evidence.

---

# 21. READINESS MUST TEST THE REAL PRODUCT PATH

Preserve fail-closed behavior, but verify that the readiness implementation itself is not testing the wrong local inbound, wrong route, or impossible condition.

A readiness probe must correspond to the path that determines whether Android application traffic is actually protected.

Do not require an optional HTTP inbound to pass if the production TUN path uses SOCKS and real device traffic is otherwise proven.

For diagnosis only, a debug-only bounded diagnostic window may delay teardown long enough to capture evidence. Production behavior must remain fail-closed.

---

# 22. MULTIPLE SERVERS / TRANSPORTS

Repeat core isolation on at least two subscription nodes.

Prefer nodes with different transport/security combinations if available.

Interpretation:

- one node works, one fails -> likely node/transport-specific mapping issue;
- all working-reference nodes fail identically -> likely shared HotFox client/routing/core issue.

Do not mass-edit transport mappings without evidence.

---

# 23. REAL ANDROID DEVICE-WIDE TRAFFIC IN ENGINEERING RUNTIME

Once the full VPN path is believed fixed, prove traffic outside the HotFox process.

Use Android Chrome/browser in the available emulator/device environment and load multiple sites.

At minimum:

- example.com;
- google.com;
- cloudflare.com.

A successful internal OkHttp request from HotFox is insufficient to prove device-wide VPN forwarding.

If another Android application is available, test it as additional evidence.

---

# 24. IP BEFORE / DURING / AFTER

Measure from Android networking:

- `IP_BEFORE` with VPN off;
- `IP_DURING` with VPN working;
- `IP_AFTER` after disconnect.

When the remote VPN exits through a different address, expected behavior is:

`IP_DURING != IP_BEFORE`

and after disconnect:

`IP_AFTER ~= IP_BEFORE`.

Do not use the Windows host public IP as evidence for Android guest VPN behavior.

---

# 25. DOWNLOAD / SUSTAINED TRAFFIC

After basic HTTPS succeeds, run a small sustained transfer through VPN to prove the path does not only pass one tiny request.

Avoid huge bandwidth tests during routine CI.

Record that bytes continue to move and no core/HEV/native crash occurs.

---

# 26. DISCONNECT / RECONNECT

A fixed one-shot connection is not enough.

Run at least three successful cycles:

`CONNECT -> HTTPS/BROWSER PASS -> DISCONNECT -> direct Internet PASS`.

Verify no:

- `address already in use`;
- stale TUN;
- stale Xray;
- stale HEV;
- duplicate service;
- leaked connection job;
- false UI state;
- stale selected-server mismatch.

---

# 27. SERVER SWITCH

After basic stability, switch between two valid nodes and prove that:

- UI selected server changes;
- generated config changes to the same selected node;
- the old outbound/session is torn down correctly;
- traffic works on the new node or reports a truthful bounded failure.

No state where UI says server B while core still uses server A.

---

# 28. SUBSCRIPTION REGRESSION

After networking fixes, recheck:

- runtime HTTPS import;
- refresh;
- duplicate handling;
- process restart persistence;
- server selection persistence;
- import UI refresh;
- no secret logging.

A networking fix must not break manual subscription compatibility.

---

# 29. SECURITY / SECRETS

The owner-provided subscription URL/token is a runtime secret.

Never commit it into:

- Kotlin/Java;
- XML/resources;
- assets;
- Gradle;
- BuildConfig;
- local.properties;
- GitHub Actions;
- docs/README;
- bootstrap payload;
- tests;
- raw logs.

Never include full UUID/password/private-key/Reality credentials in reports.

Before each production commit run a secret scan over changed files and sanitized artifacts.

Do not weaken TLS/certificate/Reality validation to make connection tests pass.

---

# 30. CANONICAL BOOTSTRAP / SOURCE OF TRUTH

If the project uses reconstructed `V2rayNG/` plus bootstrap/overlay as the canonical source model, every production fix must survive reconstruction.

For each fix record:

- reconstructed file;
- canonical source/overlay file;
- whether bootstrap reproduces it.

A local V2rayNG patch that disappears on bootstrap is not an accepted 2.9 fix.

---

# 31. BUILD AND TEST LOOP

For every real bug fix:

`observe`
→ `collect evidence`
→ `isolate root cause`
→ `minimal fix`
→ `unit/integration tests`
→ `reconstruct if applicable`
→ `assemblePlaystoreDebug`
→ `install fresh APK`
→ `run exact failed scenario`
→ `run related regression tests`
→ `secret scan`
→ `commit`.

Do not commit an unverified speculative networking patch as `fixed`.

---

# 32. COMMIT DISCIPLINE FOR 2.9

Keep fixes logically separated where practical.

Examples only; use the actual discovered cause in the final commit message:

- `fix(core): decouple Xray capability from HotFox package name`
- `fix(subscription): avoid unavailable local proxy during refresh`
- `fix(ui): refresh servers after shared subscription import`
- `fix(vpn): protect Xray outbound sockets from VPN feedback loop`
- `fix(core): preserve Reality fields in generated VLESS outbound`
- `fix(vpn): serialize SOCKS readiness before HEV startup`

Do not use vague messages like `fix vpn` when a precise root cause is known.

Ordinary 2.9 fix commits must not contain `[hotfox-phase-exit]` until the whole phase is ready for its exit gate.

---

# 33. REQUIRED BUG REPORT FORMAT

For every defect discovered in 2.9 retain both initial failure and final state.

Required fields:

- BUG ID;
- title;
- severity;
- area;
- initial status;
- reproduction;
- expected;
- actual;
- evidence;
- exact root cause;
- source files/symbols;
- fix;
- changed files;
- target retest;
- regression tests;
- final result;
- commit SHA.

Do not overwrite history from `FAIL` to `PASS`; show `INITIAL FAIL -> FIXED -> RETEST PASS`.

---

# 34. FINAL 2.9 ACCEPTANCE MATRIX

The final report must explicitly include:

- BUILD: PASS/FAIL;
- INSTALL: PASS/FAIL;
- APP START: PASS/FAIL;
- SUBSCRIPTION IMPORT: PASS/FAIL;
- SERVER LIST: PASS/FAIL;
- SERVER SELECT: PASS/FAIL;
- SOURCE -> MODEL config: PASS/FAIL;
- MODEL -> XRAY config: PASS/FAIL;
- XRAY SOCKS-ONLY HTTPS: PASS/FAIL;
- HTTP inbound if applicable: PASS/FAIL/N/A;
- VPN PERMISSION: PASS/FAIL;
- VPNSERVICE: PASS/FAIL;
- TUN: PASS/FAIL;
- HEV: PASS/FAIL;
- SOCKET PROTECT / LOOP PREVENTION: PASS/FAIL;
- UNDERLYING NETWORK: PASS/FAIL;
- DNS: PASS/FAIL;
- IPV4: PASS/FAIL;
- IPV6: PASS/FAIL/N/A according to policy;
- ANDROID HTTPS: PASS/FAIL;
- BROWSER: PASS/FAIL;
- IP BEFORE: value/redacted as appropriate;
- IP DURING: value/redacted as appropriate;
- IP AFTER: value/redacted as appropriate;
- TRAFFIC PROXIED: PASS/FAIL;
- DISCONNECT: PASS/FAIL;
- 3X RECONNECT: PASS/FAIL;
- SERVER SWITCH: PASS/FAIL;
- SUBSCRIPTION REFRESH: PASS/FAIL;
- CRASH/ANR/NATIVE CRASH: NONE/details;
- SECRET SCAN: PASS/FAIL;
- BOOTSTRAP REPRODUCTION: PASS/FAIL;
- FINAL APK SHA256;
- TESTED APK SHA256;
- FINAL APK == TESTED APK: YES/NO.

---

# 35. TRAFFIC PROXIED PASS CRITERIA

`TRAFFIC PROXIED: PASS` is allowed only when evidence establishes the complete functional path.

Minimum evidence:

- VpnService active;
- TUN active;
- HEV active;
- Xray active;
- HEV local SOCKS target correct;
- remote Xray outbound actually succeeds;
- loop prevention / underlying network works at runtime;
- real Android HTTPS succeeds;
- Android browser/device-wide traffic succeeds;
- response data returns successfully;
- no false protected state.

Prefer also a measured `IP_DURING` distinct from baseline when the server architecture makes that expected.

A VPN icon, TUN fd, running process, or `accepted tcp:` log alone is insufficient.

---

# 36. 2.9 EXIT GATE

2.9 cannot be marked engineering-complete while the observed HotFox datapath failure remains unresolved.

Required exit:

1. all P0/blocker networking defects resolved;
2. real emulator/runtime E2E passes in the available engineering environment;
3. tested APK is identified by SHA-256;
4. build/unit/integration/static/lint gates required by the repository pass;
5. no secrets are committed/logged;
6. bootstrap/source-of-truth reproduces the fixes;
7. final candidate commit contains `[hotfox-phase-exit]`;
8. trusted final review reports no substantiated P0/P1.

Then record:

`2.9 ENGINEERING COMPLETE — real engineering-runtime VPN E2E passed; final physical release validation deferred.`

and immediately begin:

`3.0 Premium Android Experience`.

---

# 37. 3.0 AND 3.1 AFTER THIS PHASE

After this owner roadmap change:

- **3.0** uses the full existing Premium Android Experience scope previously numbered 2.9;
- **3.1** uses the full existing Mature HotFox Platform scope previously numbered 3.0, including pre-release platform hardening;
- the `FINAL RELEASE DEVICE GATE` remains after 3.1 unless the owner explicitly cuts an earlier release candidate.

Do not skip or delete the existing Premium UI or Mature Platform work.

---

# 38. FINAL QUESTION FOR 2.9

The phase report must answer exactly:

`CAN A NORMAL USER INSTALL THIS APK, IMPORT THE KNOWN-WORKING SUBSCRIPTION, SELECT A SERVER, PRESS CONNECT, AND USE ANDROID INTERNET THROUGH THAT VPN?`

Allowed final answers:

- `YES — VERIFIED BY REAL E2E`;
- `NO — <exact remaining root cause>`;
- `NOT FULLY VERIFIED — <exact missing evidence>`.

No `probably`, no `should work`, and no build-only acceptance.
