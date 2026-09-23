# Untested gaps — onboarding corrective pass 2 / 2026-09-23

- Runtime VPN E2E (TUN → HEV → Xray → real HTTPS) **NOT EXECUTED**.
- Physical Android device **NOT EXECUTED**.
- fontScale **1.30** was not rendered (1.0 and 1.15 were).
- `:app:assemblePlaystoreRelease` and lint were **NOT EXECUTED** in this pass.
- GitHub Actions for SHA `9dfff8d` is **not** claimed PASS (may be skipped/queued).
- KVM nested virt is broken here; emulator used **TCG** (`-accel off`). System UI ANR dialogs are an emulator defect; `hide_error_dialogs=1` was set.
- Pixel-exact match to the iOS triptych is **not** claimed.
- Debug screenshot harness remains `src/debug` only.
