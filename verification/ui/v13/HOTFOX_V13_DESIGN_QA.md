# HotFox V13 — design QA self-check

Снято с настоящего debug UI, locale `ru-RU`, viewport **1032×2231** @420dpi, fontScale 1.0. Референсы 853×1844 масштабированы LANCZOS только для diff. **Pixel PASS: 0/3. Art-director 1:1 не заявляется. RELEASE READY: NO.**

## Общее

- [x] Одна сцена `hotfox_hero_scene.png`, без второй лисы, колец, орбит, halo.
- [x] Views/ViewBinding, не Compose.
- [x] Общий scaffold: шапка → статус → сцена/слот → деталь → CTA → сервер → Shadow+Smart → подписка → навигация.
- [x] Навигация «Главная» активна во всех трёх состояниях.
- [x] Debug fixtures не в `src/main`.
- [ ] Pixel-exact vs v2 PNG (system bars, PRO, demo-цифры).

## Состояния

| Состояние | Совпадает | Расхождения |
| --- | --- | --- |
| Disconnected | Сцена, копирайт, CTA, карточки, nav | Нет PRO (нет entitlement). Android status bar вместо iOS. |
| Connecting | Чип, «Подключаем…», «Отменить», горизонтальный progress | Демо-сервер Frankfurt не подставлен. Progress — native 42% при reduced motion. |
| Connected | «Вы защищены», «VPN активен», трафик, «Отключить» | Таймер 00:00:00 и 0 МБ — реальные debug-значения, не 00:24:18 / 128/24 с макета. |

## Не доказано

Runtime VPN E2E, физическое устройство, pixel-lock. Debug chrome ≠ verified CONNECTED production path.
