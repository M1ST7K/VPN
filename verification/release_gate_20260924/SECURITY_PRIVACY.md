# Security / privacy handling during the gate

- No owner subscription URL, UUID, token, password, private key, keystore or
  payment secret was read, printed, logged or committed.
- Ephemeral test-server credentials lived only in a `0700` temp dir on the VM;
  the served subscription file was deleted and the tunnel/test-server processes
  were stopped after the run.
- Harness outputs were grep-checked: the subscription URL string occurs 0 times in
  host output, report and logcat; no UUID pattern in the committed report.
- Committed logcat excerpt already passes through `SecretRedactor` (path shown as
  `<secret>`); committed IPs are redacted to `/24` (`x.x.x.x` last octet) and belong
  to the cloud VM NAT, not to the owner.
- App network policy untouched: `cleartextTrafficPermitted=false`, system CAs only.
  No TLS/REALITY weakening, no DNS/IPv6 fail-closed change, no readiness bypass.
- `static_check_2_2_0.py` embedded-secret scan: PASS.
