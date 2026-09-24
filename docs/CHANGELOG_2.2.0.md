# HotFox Proxy 2.2.0

## Networking / VPN
- Canonical `VpnSessionCoordinator` is the connection source of truth.
- CONNECTED / «Защищено» is published only after Xray SOCKS readiness, HEV start, and a bounded path verify.
- HEV still starts only after the local SOCKS listener is reachable.
- Network handover keeps the Android TUN and waits for SOCKS again before returning to CONNECTED.
- Stale connect pipelines are cancelled/invalidated on disconnect and error.
- Session traffic uses HEV tx/rx with session baseline and reset accumulation.

## Lifecycle
- Session timer is service-owned (`elapsedRealtime`) and survives Activity recreation.
- Reconnect does not reset the session timer or traffic counters.
- Foreground VPN notification and `VpnService.protect` / self-exclusion are preserved.

## UI
- Editorial 3-screen HotFox UI is retained.
- Headlines follow session state: Не защищено / Подключение / Защищено / Переподключение / Нет соединения.
- Server country parsing is conservative; ping stays «— ms» when unmeasured.
- Subscription expiry uses `dd.MM.yyyy`; remaining days never go negative.
- Subscription URL is masked on screen; copy uses the stored URL.
- Diagnostics export is redacted through `SecretRedactor`.

## Build
- `versionName` 2.2.0, `versionCode` 22000, `applicationId` `com.hotfox.vpn`, `minSdk` 24.
- Lint NewApi/Orientation defects fixed; `MissingTranslation` disabled because the product is Russian-primary with upstream locales.
