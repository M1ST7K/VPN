# OLD → NEW interaction map

Канонический VPN/state не менялся. Ниже — только presentation/navigation adapters.

| Действие | Старый control | Callback | Новый control | Источник state | Evidence |
|---|---|---|---|---|---|
| Connect | `connect_action` / hidden FAB | `handleFabAction()` → `CoreServiceManager.startVService` | CTA «Подключить» на экранах 05 | `HotfoxEngineFacade` / `ConnectionUiMapper` | existing unit tests + MainActivity wiring |
| Stop connecting | CTA while busy | `handleFabAction()` stop | CTA «Остановить» на экране 06 | canonical session, not UI percent | `applyRunningState` uses `ConnectionUiMapper` |
| Disconnect | CTA «Отключить» | `CoreServiceManager.stopVService` | CTA экрана 07 | canonical teardown | existing restart/stop tests |
| Add subscription | Material `showAddDialog` | HTTPS/clipboard/QR/file/always-on | `HotfoxAddConnectionSheet` (08) | import managers unchanged | `HotfoxUiRebuildContractTest` |
| HTTPS paste/import | nested Material EditText | `importBatchConfig` | `HotfoxHttpsSubscriptionActivity` (09) | `AngConfigManager.importBatchConfig` + `HotfoxHttpsImportPolicy` | `HotfoxHttpsImportPolicyTest` |
| QR | add dialog item | `importQRcode()` | sheet row QR | unchanged | interaction preserved |
| File | add dialog item | `importConfigLocal()` | sheet row file | unchanged | interaction preserved |
| Server list | bottom nav Серверы | `showSection(SERVERS)` | экран 10 | `MainViewModel` / AUTO row | `HotfoxServerListContract` |
| AUTO | first server row | `HotfoxServerSelection.selectAuto` | row 0 экрана 10 | persisted AUTO | existing AUTO tests |
| Manual server | tap row | `selectManual` | tap row; details chevron → 11 | sticky manual | `HotfoxServerSelection` |
| Subscription tab | bottom nav | `showSection(SUBSCRIPTION)` | экран 12 | `CommerceCoordinator` / subscription MMKV | existing commerce tests |
| Buy/renew/manage | premium actions | checkout coordinator | subscription actions + onboarding secondary buy (`CommerceCoordinator.startCheckout`) | backend-authoritative; browser return not proof | existing commerce tests |
| Settings | Material item list from toolbar | launches settings/apps/routing/ads/… | `HotfoxSettingsActivity` (13), 4-tab Настройки; ads/updates remain in Прочее | same destinations including ads toggle | activity rows |
| Routing | Material mode dialog / routing activity | `HotfoxRoutingStore` | экраны 14 + mode rows | existing routing snapshot | `HotfoxRoutingTest` |
| Per-app rules | `PerAppProxyActivity` | include/exclude | экран 15 `HotfoxAppsRulesActivity` inlines real `AppManagerUtil` / `PerAppProxyViewModel` | `PREF_PER_APP_PROXY_SET` | existing routing tests |
| DNS | routing store pref | `PREF_DNS_VPN` | row DNS on 14 | `HotfoxRoutingStore.saveDns` | existing `dnsThroughVpn` policy tests |
| IPv6 | settings pref | `PREF_IPV6_ENABLED` | row IPv6 on 14 | existing fail-closed path | CoreVpnService tests |
| LAN | routing LAN toggle | `saveLan` | row LAN on 14 | existing | routing tests |
| Shadow | settings/autopilot profile | `HotfoxShadowStore` | экран 17 + connection row | `isShadowAuto` | `HotfoxShadowTest` |
| Autopilot | `HotfoxAutopilotActivity` | store + runtime | экран 16 | existing Autopilot engine | `HotfoxAutopilotTest` |
| Pause | Autopilot pause dialog | `startPause` | Autopilot pause control | existing | autopilot tests |
| Trusted networks | Autopilot trusted | `markTrusted` | Autopilot trusted control | existing | autopilot tests |
| Notifications | NotificationManager | `NotificationUiMapper` | unchanged | canonical session | `HotfoxPremiumViewStateTest` |
| Diagnostics | settings item | `HotfoxDiagnosticsBuilder` | settings row | redacted builder | diagnostics tests |
| Updates | CheckUpdateActivity | existing | settings row | existing ops | ops tests |
| Always-on / Kill Switch | Material message + VPN settings | `ACTION_VPN_SETTINGS` | экран 18 | system-derived, not faked | activity CTA |
| Android VPN settings | positive button | `Settings.ACTION_VPN_SETTINGS` | CTA экрана 18 | Android-owned | no fake ownership |

Ни одна прежняя функция не удалена: add/import/QR/file, AUTO/manual, routing/apps/DNS/IPv6/LAN, Shadow, Autopilot, diagnostics, updates, Always-on.

Нижняя навигация: 4 destinations (`Главная / Серверы / Подписка / Настройки`) по экрану 05.
