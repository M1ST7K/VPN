# HotFox V6 — QA locale lock (`ru-RU`)

Visual comparison against approved Russian boards is valid only when the capture process resolves `values-ru` resources. Production strings stay bilingual (`values` + `values-ru`); Russian is **not** hardcoded into Kotlin.

## Capture configuration (mandatory)

Package: `com.hotfox.vpn`

Commands used by `verification/ui_rebuild/capture_harness_v6.py` before every screen:

```bash
adb wait-for-device
adb shell cmd locale set-app-locales com.hotfox.vpn --locales ru-RU
adb shell am force-stop com.hotfox.vpn
```

Debug harness also applies AndroidX per-app locales once at process start:

```kotlin
AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ru-RU"))
```

This lives only in `src/debug` (`HotfoxUiScreenshotHarnessActivity`). Release/production localization behavior is unchanged.

## Proof dump

After lock, capture writes `verification/ui_rebuild/V6_LOCALE_PROOF.txt` with:

- `cmd locale get-app-locales com.hotfox.vpn`
- `getprop persist.sys.locale`
- `dumpsys package com.hotfox.vpn | grep -i locale` (best-effort)
- a resource dump from a resumed onboarding/settings activity if available

Expected per-app locales: `ru-RU`.

## What this is not

- Not a rewrite of `values/strings.xml` into Russian-only.
- Not a fake CONNECTED/VPN-permission grant.
- System Settings UI language may remain English; that does not invalidate in-app `values-ru` copy.

Feature names that approved boards keep in English (`Shadow`, `Smart`, `AUTO`, `Always-on VPN` where present) stay as in resources.
