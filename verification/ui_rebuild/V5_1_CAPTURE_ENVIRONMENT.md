# HotFox V5.1 capture environment

Status: **local renderer recovered** (not RELEASE READY).

## What failed as a primary path

- AVD `hotfox34` remains unhealthy and is **not** used as the capture device.
- A **new** AVD `HotFox_V51_API34_Clean` (API 34 `google_apis` x86_64, 1080×2400) was created after killing qemu/netsimd/adb **by PID**, removing AVD locks, and restarting the adb server.
- `/dev/kvm` is world-writable (`crw-rw-rw-`). `os.access('/dev/kvm', R_OK|W_OK)` is true.
- Boot with `-accel on` / KVM still stalls after `Activated packet streamer for bluetooth emulation`: qemu RSS ≈295 MB, `adb devices` empty. Same stall as the old AVD, so the old name was not the unique cause.

## Diagnostic that distinguished KVM from image/emulator install

- One diagnostic boot: `emulator -avd HotFox_V51_API34_Clean ... -accel off`
- Result: qemu RSS grew to ~2.8 GB, CPU busy, `adb devices` showed `emulator-5554 device`, `sys.boot_completed=1` after several minutes, `ro.build.version.sdk=34`, ABI `x86_64`.
- Conclusion: the installed API 34 image and emulator binary **can** boot; **KVM acceleration hangs in this VM**. Software CPU emulation is slow but is the working local renderer.

## Capture configuration used

- Display: `DISPLAY=:1` (QT window; not `-headless`)
- `XDG_RUNTIME_DIR` unset for the emulator process
- GPU: `swiftshader_indirect`
- Flags: `-no-audio -no-boot-anim -no-snapshot-load -no-snapshot-save -netdelay none -netspeed full -no-metrics`
- Guest: night mode, animation scales 0, debug-fixture APK `HotFox_Proxy_2.2.0_x86_64.apk` only
- No production secrets / subscription URL

`-accel off` is **not** the preferred production performance setup. It is used here only because KVM does not complete boot in this cloud agent VM.

CI screenshot runner remains a documented fallback if this software-emulated guest later dies; it is not required while local adb works.
