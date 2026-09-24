# V9 locale audit (ru-RU capture evidence)

Capture lock: `cmd locale set-app-locales com.hotfox.vpn --locales ru-RU`  
Proof: `verification/ui_rebuild/V9_LOCALE_PROOF.txt`  
Production code does **not** call `AppCompatDelegate.setApplicationLocales`. Russian lives in `values-ru/strings.xml`.

| Screen | Visible strings (from actual_v9) | Resource source | Mixed-language found/fixed | Intentional English exceptions |
| --- | --- | --- | --- | --- |
| 01 | БОЛЬШЕ, ЧЕМ VPN; HotFox | `hotfox_tagline_more_than_vpn`, wordmark | none | HotFox, VPN |
| 02 | Подключите HotFox; У меня уже есть подписка; Купить доступ | onboarding strings | none | HotFox |
| 03 | Лучший сервер — автоматически; Использовать AUTO | onboarding AUTO | none | AUTO, HotFox |
| 04 | Всё готово; Далее; VPN-подключение | onboarding ready | none | VPN |
| 05 | Не защищено; Нет разрешения VPN; Подключить; Весь трафик · Smart; Shadow | home + routing labels | none remaining | Smart (mode), Shadow, VPN, LAN in caption |
| 06 | Подключаемся…; stages; Остановить | connecting strings | none | Smart/Shadow as on 05 |
| 07 | Защищено; Отключить; Соединение | protected chrome | none | HotFox, Shadow, Smart |
| 08 | Добавить подключение; HTTPS-подписка; Always-on VPN / Kill Switch | sheet strings | none remaining | HTTPS, QR, Always-on VPN, Kill Switch |
| 09 | HTTPS-подписка; Вставить; Добавить | https import | none | HTTPS, URL |
| 10 | Серверы; Все/Избранные/Страны; Мои серверы | filters now `@string` not hardcoded XML | hardcoded RU filters moved to resources | city names (Amsterdam…) |
| 11 | Детали сервера; Умная маршрутизация | `hotfox_smart_routing_screen` ru | **fixed** vs V8 English Smart Routing | AUTO, Shadow, Smart, ms |
| 12 | Подписка; Управлять подпиской; HotFox Premium | subscription strings | hardcoded «Статус неизвестен» → `@string/hotfox_status_unknown` | HotFox Premium, HTTPS |
| 13 | Настройки; Always-on VPN; DNS; IPv6 | settings + product terms | none | Always-on VPN, DNS, IPv6 |
| 14 | **Умная маршрутизация**; **Пользовательские правила**; Режим/Сеть/Правила | `hotfox_smart_routing_screen`, `hotfox_custom_rules`, group eyebrows | **P0 fixed**: no Smart Routing / Custom rules in ru-RU | Smart, DNS, IPv6, LAN, VPN |
| 15 | Приложения и правила; Пользовательские правила; Сохранить правила | apps strings | none | package names, Telegram/Chrome… |
| 16 | Автопилот; Авторизация сети; Ожидания авторизации сети нет | `hotfox_captive_portal`, `hotfox_autopilot_captive_idle` | **fixed** mixed “captive portal” | Wi‑Fi |
| 17 | Shadow; Оставить AUTO | shadow strings | none | Shadow, AUTO |
| 18 | Always-on VPN; Неизвестно; Блокировать без VPN | always-on strings | none | Always-on VPN, Android, kill switch in body |

P0 screen 14: V8 showed English `Smart Routing` / `Custom rules`. V9 ru-RU capture shows Russian product wording from `values-ru`.
