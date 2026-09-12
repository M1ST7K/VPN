# HotFox: 18-screen reconstruction task

Начать с [полного задания Cursor](HOTFOX_CURSOR_FULL_UI_REBUILD_PIXEL_LOCK_PROMPT.txt).
Оно требует реализации всех 18 экранов в существующем Android-приложении,
сохранения VPN и фактического нулевого pixel diff в объявленном viewport.

- [Оригинальный ZIP](HOTFOX_18_FINAL_STYLE_REFERENCE.zip) — файл владельца без изменения байтов, имя нормализовано.
- [18 изображений](hotfox_18_final_style_reference/) — оригинальные JPEG/PNG.
- [Оригинальный recreation prompt](HOTFOX_18_PIXEL_LOCK_RECREATION_MASTER_PROMPT.txt) — полная неизменённая версия владельца.
- [Контрольные суммы и размеры](HOTFOX_REFERENCE_LOCK.json).
- [Выявленные расхождения](HOTFOX_REFERENCE_CONFLICTS.md) — обязательно читать перед реализацией.
- [Технический baseline](HOTFOX_TECHNICAL_BASELINE.json) — база `a729b1c`.

Проверки из корня репозитория (Python 3, Pillow; NumPy для `pixels`):

```bash
python3 verification/ui_rebuild/verify_reference_lock.py integrity
python3 verification/ui_rebuild/verify_reference_lock.py core
python3 verification/ui_rebuild/verify_reference_lock.py pixels --reference reference.png --actual actual.png --out comparison
```

`integrity` проверяет файлы; `core` — снимок защищённых путей;
`pixels` — точное равенство переданных растров. Они не подменяют
происхождение скриншотов, action tests и VPN E2E.
У `pixels` нет допуска, автоматической маски, resize или выравнивания.

Статус этого пакета: **задание и исходники готовы; 18 экранов в APK ещё не проверены**.
Ветка: `cursor/hotfox-ui-pixel-lock-rebuild`. Задача не мержит runtime PR #4,
не изменяет reviewer PR #8 и не объявляет приложение готовым к релизу.
