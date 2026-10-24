# M5: сохраняем выбор блюд после закрытия приложения

Дата: 21.10.2026. PR #1, кодовый коммит `c375878c7d74a63a26d1261f3c70765054c84be2`.

**M5: сохранение локально выбранных рецептов и пакетной готовки = ENGINEERING PASS.**
23.10.2026 дополнительный **реальный emulator force-stop → новый PID → холодный
запуск PASS**; физический телефон и общий релиз пока НЕ ПРИНЯТЫ.
Ниже сохранены все исторические результаты отдельных проверок.

## Реализация

- `SavedWeekPlanner.kt`: versioned snapshot для 7 дней, выбранных recipe ID, масштабирования порций, интервалов приготовления и уровня усилий **каждого** выбранного блюда.
- `SavedWeekSelectionCodec.kt`: ограниченный по размеру строгий JSON, UNKNOWN/повреждённые записи → нет восстановления.
- `ProfileStore.kt`: локальный Android DataStore, транзакционная запись snapshot, загрузка из другого экземпляра хранилища; команда очистки для будущей функции «новая неделя».
- `MainActivity.kt`: передача сохранённого snapshot в пользовательский экран и сохранение после принятого изменения.
- `FoodPlanHomeScreen.kt`: загрузка меню в единственном трёхраздельном пользовательском интерфейсе; запись после «Другое блюдо»/«Сохранить на N дней»; SKU, парсер и граф остаются скрытыми.
- **Не хранятся как истина** цены, наличие, магазинные URL, старые суммарные калории и покупные упаковки. После восстановления все данные рецептов и недельная продуктовая корзина рассчитываются заново.

## Безопасность и детерминированность

- Отпечаток профиля SHA-256 нормализует порядок кухонных приборов, вкусов и остатков.
- Изменение **только бюджета/магазина/города** не сбрасывает выбранные блюда.
- Изменение размера семьи, целей, исключений, аллергенов, кухни, вкусов и домашних запасов отменяет применение старого snapshot.
- Неизвестный рецепт, некорректный ID, повреждённый диапазон/группа дней, невозможная порция или несовместимое блюдо → **отказ от полного восстановления**; существующий профиль и ограничения имеют приоритет.
- Восстановленный рецепт обязан пройти тот же `RecipeRanker` с реальными диетой, приборным оснащением, временем и выбранной сложностью.

## Gauntlet результаты

| Проверка | Результат | Подтверждение |
| --- | --- | --- |
| Новые JVM-тесты: сохранение и восстановление блюд | **PASS** | `SavedWeekPlannerTest` |
| 3 дня × 2 человека; граммы и корзина после восстановления | **PASS** | `SavedWeekPlannerTest`, `RecipeBatchPlannerTest` |
| Сохранение заменённого рецепта | **PASS** | `SavedWeekPlannerTest` |
| Изменение бюджета/магазина сохраняет меню | **PASS** | `SavedWeekPlannerTest` |
| Изменение профиля/аллергии/вкусов/остатков не сохраняет опасный старый план | **PASS** | Негативные JVM-тесты |
| Повреждённый snapshot или нарушенная batch-связь отклоняется | **PASS** | JVM-тесты |
| Изменённый порядок оборудования после перезапуска | **PASS** | Детерминированный fingerprint, JVM |
| Запись Android DataStore → новая инстанция ProfileStore → восстановление и пересчёт закупки | **PASS** | `ProfilePersistenceTest`, Android emulator |
| Полный пересозданный пользовательский экран с сохранённым выбором | **PASS** | `FoodPlanHomeScreenTest`, Android emulator |
| JVM, Android APK, androidTest APK | **PASS** | [GitHub Actions #39587959041](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39587959041) |
| Android-эмулятор `connectedDebugAndroidTest` | **PASS** | Exact code SHA `c375878c7d74a63a26d1261f3c70765054c84be2` |
| Насильственное завершение отдельного процесса Android и повторный запуск на физическом телефоне | **NOT VERIFIED** | В этой итерации проверены реинициализация хранилища и пересоздание Compose-экрана, но не физический телефон |
| Пользовательская команда «Начать новую неделю» с подтверждением | **PASS** | Настройки → «Начать новую неделю», отмена, подтверждение; [Actions #39596857523](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39596857523) |
| Ошибка записи DataStore не удаляет текущую неделю | **PASS** | UI-тест `failedNewWeekStorageResetDoesNotDeleteCurrentMealChoices` |
| Очистка недели сохраняет профиль, оценки, запасы, магазин, бюджет и ценовые наблюдения | **PASS** | `clearingNewWeekDeletesNoPersonalDataOrStoreEvidence`, Android DataStore test |
| Независимые ≥150 проверенных рецептов | **M2 FAIL** | Сохранение выбора не является пищевой верификацией |
| Автономный разбор ≥30 категорий / ≥100 реальных SKU магазина | **M4 FAIL** | Отдельный этап |

**Не приписывать успех модульных тестов полному релизу.** Финальная пользовательская приёмка и реальный магазин остаются отдельными требованиями.

## 21.10.2026: пользовательская команда начала новой недели

**Подтверждено: exact-code SHA `248a969fab19a086b2b8bc097b87452a22a576bd`**: JVM/JUnit, debug APK, androidTest APK, Android emulator `connectedDebugAndroidTest` → **PASS** ([GitHub Actions #39596857523](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39596857523)).

Функция находится в третьем пользовательском разделе «Настройки». При подтверждении приложение сначала вызывает атомарную очистку *только* `saved_week_selection_v1` в DataStore, затем заново рассчитывает 7 дней, сбрасывает выбор дня и открывает «Рецепты». При отмене ничего не меняется. При ошибке записи прежнее меню и настройки остаются на месте, показывается сообщение без технических подробностей. Предложение «Новая неделя» означает пересборку исходного детерминированного меню из текущего профиля, не обещает гарантированно новые блюда.

Gauntlet: первый Android прогон [#39595761699](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39595761699) обнаружил два **UI-тестовых FAIL**: кнопка была вне видимой области прокручиваемых настроек и её перекрывала нижняя навигация. Исправление в тестах: `onNodeWithTag("home-start-new-week").performScrollTo().performClick()`. После исправления, повторный exact-SHA прогон **PASS** на #39596857523.

**Границы:** реальное завершение процесса ОС и запуск на физическом устройстве пока **NOT VERIFIED**. Отдельная проверка источников M2 продолжает возвращать FAIL по реальным названиям публичных карточек и не связана с этой UI-функцией. М2 и М4 не закрыты.

## 23.10.2026: ADB OS force-stop с холодным запуском, независимо доказан PASS

**Код:** [SHA 306c7a0](https://github.com/xxiamadelxx-blip/foodplan-ai/commit/306c7a0c0194315c4a3106e5e96b59e29966a997).
**Точный успешный прогон:** [Android CI #550](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39883827124).
**Артефакт доказательств:** [m5-emulator-force-stop-cold-relaunch-evidence (ID 12471529576)](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39883827124/artifacts/12471529576).

Проверка не является повторным `remember`/Compose recreation. В тесте
`M5ColdRestartSeedTest` с одним синтетическим профилем в **настоящем
Android DataStore** сохранены и повторно прочитаны семидневный план,
рецепт и пакетная готовка первого завтрака на три последовательных дня.
Вне приложения скрипт `tools/m5_emulator_cold_restart.py` после
настоящего запуска `MainActivity` получил PID `4914`, выполнил
`adb shell am force-stop com.foodplan.ai`, проверил отсутствие процесса,
запустил приложение через `adb shell am start -W` и получил новый
PID `5009`. После запуска через UIAutomator на *реальном экране*:

1. Прямой старт раздела «Покупки на неделю», без повторной анкеты.
2. «Рецепты»: в День 1 сохранено «Приготовить сразу на 3 дня».
3. День 2: «Блюдо готовится заранее на 3 дня».
4. «Продукты»: после прокрутки найдены минимум две видимые строки
   «Нужно купить» и соответствующие ориентиры числа упаковок.

Скачан и независимо проверен артефакт GitHub: JSON
`status=PASS_EMULATOR_OS_FORCE_STOP_COLD_RELAUNCH`, 8 пройденных
подэтапов, PID до/после разные, 4 PNG и последний XML интерфейса.
Флаг `physical_android_verified=false` в том же JSON исключает
ложную физическую приёмку. Параллельно: **47/47 Android UI tests PASS**,
**5/5 тестов Python-хоста PASS**, JVM/подготовка APK PASS.

### Отчёт трёх адресных исправлений (не более трёх)

- Первый [CI #546](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39881810439):
  47/47 Android PASS; ошибка CWD скрипта после отдельной оболочки
  `reactivecircus/android-emulator-runner`. Исправлен путь.
- Второй [CI #548](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39882734307):
  реальный `force-stop`, новый PID, восстановленные 3 дня **уже
  доказаны**. Ложный FAIL в проверке прокрученного заголовка.
  Сохранённый XML/скриншот содержал 5 реальных товаров и упаковок.
- Третий [CI #550](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39883827124):
  тест проверяет именно видимые строки покупки, а не прокрученный
  заголовок; отрицательный Python-тест предотвращает ложный PASS.
  **Полный E2E PASS; дальнейших итераций этой проблемы не нужно.**

**Границы:** физический телефон после ручного force-stop **NOT VERIFIED**,
обычное закрытие/открытие сохранения уже подтверждено пользователем;
M2 0/150 внешних независимых приготовленных рецептов BLOCKED,
M3/M4 полное бюджетно-пищевое покрытие BLOCKED, M6 настоящий
подписанный публичный релиз NOT ACCEPTED.


## 24.10.2026 · Safe physical-phone cold-restart evidence harness

The historical M5 OS proof is **emulator-only** and deliberately contains `pm clear` in its synthetic fixture setup. It must never be pointed at someone's actual phone. A separate physical probe now exists: `tools/m5_physical_cold_restart.py`.

It requires an **explicit ADB serial**, an explicit `--allow-force-stop` opt-in, a truly non-emulated Android with an already installed FoodPlan build, and a previously saved first-day breakfast with **3-day batch cooking**. Before touching the process it verifies a real visible saved week on Day 1 / Day 2, at least two real grocery demands and explicit pack information. It then runs Android OS `am force-stop com.foodplan.ai`, requires that the old PID disappear and a new PID appear, and checks the same batch and matching weekly grocery demands after cold relaunch.

- It **does not install, uninstall, run `pm clear`, reset a profile, seed synthetic data, call AI, or upload screen contents**. Temporary accessibility dumps are cleaned from device shared storage after reading; local evidence JSON records status, the hashed OS build fingerprint, number of demands compared and PASS/FAIL steps, not private recipe names or quantities.
- Rejection cases: wrong/missing device serial, emulator, unverifiable hardware, FoodPlan missing, unsaved 3-day user choice, inaccessible shopping rows, failed force-stop, reused PID, changed grocery quantities. A failed or unrun probe never sets `physical_android_verified=true`.
- Engineering invocation **only when a real phone is attached to an authorized ADB QA host**: `python tools/m5_physical_cold_restart.py --serial <ANDROID_SERIAL> --allow-force-stop --output m5-physical-local-evidence`. The `evidence.json` is local by default, never auto-uploaded. This command is not part of the emulator CI and should not be invoked with user data without the device owner's consent.
- The new host-side negative tests `tools/test_m5_physical_cold_restart.py` cover the refusal of emulators, missing installation, no explicit opt-in, stale week, non-preserved groceries, malicious serials and falsely accepted empty screens.
- **Exact-code engineering Gauntlet PASS:** [Android CI #40030500548](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40030500548), SHA `50d0015`, Python safety tests + JVM + debug APK PASS, Android emulator **71/71 PASS**, emulator OS cold restart **8/8 PASS**. **Real physical test NOT RUN**: no attached handset or authorized device-farm connection. Unit mocks cannot close the real-device gate.

An alternative for **independent physical hardware compatibility tests** is Firebase Test Lab (Spark offers a no-cost quota of five physical device test runs/day as of 24.10.2026): https://firebase.google.com/docs/test-lab/usage-quotas-pricing . That service requires project access and supports instrumentation; its ordinary instrumentation pass alone is **not** proof of this external ADB force-stop→new PID workflow. Do not equate the two.
