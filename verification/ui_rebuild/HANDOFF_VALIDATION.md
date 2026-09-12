# Проверка пакета задания — 2026-09-11

Это проверка исходников/задания и инструмента сравнения, не отчёт о готовом UI.

- Просмотрены все 18 исходных изображений.
- ZIP сохранён побайтно; переименован только файл `(...1).zip` → `.zip`.
- Исходный master prompt сохранён полностью и побайтно.
- SHA-256 всех 18 файлов совпадают с исходным архивным MANIFEST.
- Проверены размеры: 05 = 1024×1536; остальные = 1448×1086.
- `verify_reference_lock.py integrity`: PASS, 18/18.
- `verify_reference_lock.py core`: UNCHANGED, 234 protected files против `a729b1c`.
- Production source/runtime/build/workflow файлы этим пакетом не изменены.
- `git diff --check`: PASS.
- Comparator: identical raster проходит; один RGB-канал одного пикселя с delta=1
  отклоняется; alpha-only delta=1 отклоняется; разные размеры и отсутствие actual
  завершаются ошибкой. Все пять проверок инструмента прошли.

## Что ещё не выполнялось в этом handoff

- Реализация 18 экранов: NOT EXECUTED.
- Сравнение фактического UI из APK с референсами: NOT EXECUTED.
- Android rebuild/unit/lint: NOT EXECUTED в этом handoff (production не менялся).
- VPN runtime/device E2E: NOT EXECUTED в этом handoff.
- `RELEASE READY`: не заявляется.

Дальнейшие evidence должны относиться к фактическому candidate SHA Cursor.
Не переносить результаты тестов comparator в таблицу pixel acceptance приложения.
