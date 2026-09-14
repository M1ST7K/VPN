# Untested gaps — ONBOARDING-01-03-20260914

Honest remainder after this pass.

- **KVM nested virt is broken** in this cloud VM (`kvm_spurious_fault`). Emulator ran under **TCG** (`-accel off`). Slow; System UI ANR dialogs are an environment defect, not a HotFox datapath bug.
- Layout profiles 412×915, 320×568, fontScale 1.3/1.5, three-button nav, API 24 launch: **NOT EXECUTED**.
- T01 splash→02 without VPN start: splash hold harness used; production auto-advance not timed on device beyond code review.
- T02 returning completed user: unit/store only.
- T04 import cancel/invalid: not driven through the importer UI.
- T05 successful import → AUTO: needs a real subscription URL (runtime secret; not used).
- T03 02 primary → import: **PASS** on this pass without ANR overlay; no `FATAL EXCEPTION` in logcat.
- T06 purchase return: `RenewalActivity` launched from code; live tap not recaptured. Payment not executed.
- T08 picker commit/cancel: code paths in `MainActivity`/`GroupServerFragment`; live tap **NOT EXECUTED** this pass.
- T09 VPN permission step 04: not visually recaptured in this task (out of 01–03 screens).
- T10 rotation/double-tap on device: code present (`CLICK_GUARD_MS`, `STATE_STEP`); not exercised on emulator beyond compile.
- T11 Home V13: not recaptured; overlay Home files were not rolled back.
- Runtime VPN E2E and physical device: **NOT EXECUTED**.
- Pixel EXACT vs triptych: **FAIL / not claimed**.
