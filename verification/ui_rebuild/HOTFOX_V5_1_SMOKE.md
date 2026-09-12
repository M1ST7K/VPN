# HotFox V5.1 smoke capture

Status: **8/8 smoke files captured** on AVD `HotFox_V51_API34_Clean` (`-accel off`).  
Visual/pixel PASS: **not claimed**. RELEASE READY: **NO**. Merge: **NO**.

## Renderer

KVM (`-accel on`) still hangs at bluetooth packet streamer with qemu RSS ≈295 MB.  
Software emulation (`-accel off`) boots (`adb device`, `sys.boot_completed=1`) and can capture screens. Slow; system ANR dialogs appear and must be dismissed via `dumpsys window` + Wait tap. Android 12+ splash window can precede the first real frame.

Details: `verification/ui_rebuild/V5_1_CAPTURE_ENVIRONMENT.md`.

## Smoke 01 / 03 / 04 / 05 / 10 / 12 / 15 / 17

| ID | Activity | Visual notes |
| --- | --- | --- |
| 01 | Splash | Transparent fox, no rectangular matte, no planet. Ear/neck contour intact at this scale. |
| 03 | Onboarding AUTO | Routing topology (T-node), **no** concentric orbits. |
| 04 | Onboarding Ready | Completion/cards + Continue. **No** orbits. Truthful copy: path verified before Protected. |
| 05 | Home disconnected | Fox-only, **Не защищено**, no planet. Production CONNECTED not shown. |
| 10 | Servers | Debug fixture: AUTO + 6 cities. Latency labels from debug painter only. |
| 12 | Subscription | Debug fixture after repeating painter: `31.12.2026`, 110 дней, 6 servers, masked URL. Not written to stores. |
| 15 | Apps | Debug rows (Chrome/Messages selected visually). Does not write per-app store. |
| 17 | Shadow | Topology around shield, not celestial orbits. |

## Not accepted yet

- Pixel-perfect vs boards
- Screens 02/06/07/08/09/11/13/14/16/18 (full 18/18 still required)
- Runtime VPN E2E / physical device
- Transparent fox on 02/07/09 (only 01/05 in this smoke)

Production `CONNECTED` remains gated by real readiness. Screen 07 is still a debug chrome fixture when captured later.
