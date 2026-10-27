# FoodPlan · M4 · вес до очистки и вес с заливкой

**Дата:** 2026-10-23. **Приоритет:** устранить регрессию покрытия после улучшения баланса БЖУ, не выдумывая выход продукта и цену.

## Обнаруженный реальный регресс

После мягкой M3 оптимизации реальные 7 дней / 21 блюдо всё ещё требуют 33 различных ингредиента, но в первой [M4 проверке #39993987240](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39993987240) **29/33** позиций имели доказанное количество магазинных единиц против прежних 31/33. В новую неделю вошли `banana` и `chickpeas`, не имевшие активных SKU в M4 реестре. Старые `egg` и `oil` также без подтверждённого пересчёта. Полная стоимость всегда **UNKNOWN**, даже при большом бюджете.

Подтверждённые источники идентичности новых товаров:
- [Официальная карточка Магнита: «Бананы 1кг»](https://magnit.ru/product/9072651501-banany). Указан вес **с кожурой** и цена за килограмм весового товара, **не** масса съедобной мякоти и **не** фиксированная готовая упаковка.
- [Официальная карточка Магнита: «Нут Green Ribbon 400г»](https://magnit.ru/product/8000013399-green_ribbon_nut_400g_zh_b_s_kl). Указаны 400 г консервов с заливкой; рецепт учитывает **нут без рассола**. Масса осушенных зерен этого конкретного SKU из карточки не доказана. Вес другого бренда нельзя переносить.
- Цены, доступность и коммерческие предложения меняются по времени, магазину и региону. Любая свежая сумма должна исходить из нового GET официальной страницы и сохранять SHA исходного HTML. Старый скриншот/поиск и другие магазины не заменяют эту проверку.

## Реализация

- Python `m4_live_chain_price_probe.py`: зарегистрированы только точные 2 официальных URL. Не возвращать `pack_grams`; физические единицы **unpeeled_g=1000** и **net_with_liquid_g=400**, признак `purchase_count_from_grams_proven=false`. Парсер требует соответствие SKU, заголовка, оригинальной фасовки и видимой цены.
- Python `m4_actual_week_coverage.py`: эти категории сохраняют `price_per_package_kopeks` и `PURCHASE_UNIT_CONVERSION_UNKNOWN`, `packages=null`, `estimated_cost_kopeks=null`; полный итог не создаётся. Смешение ед. запрещено, тесты отрицательных сценариев.
- Kotlin `PurchaseUnit`: отдельные `UNPEELED_GRAM` и `NET_WITH_LIQUID_GRAM`, **невозможно** конвертировать в съедобные граммы через общий `UnitConversionEvidence`.
- Android `MagnitPackOnlySkuCatalog`: фиксированные URL и проверка оригинального заголовка. Цена выводится за **килограмм бананов с кожурой** (весовой товар), либо за **банку 400 г с заливкой**, количество к покупке остаётся неизвестным.
- Kotlin `MagnitPublicSkuPriceReader`, `MagnitDomReader` и `MagnitScanCoordinator`: прямые карточки и обычные каталожные записи идут через один запрет переприсваивания масс. Нельзя обойти защиту считыванием списка товаров.
- Добавлены Python, JVM и Android-тесты, запрещающие подмены упаковок, фасоли вместо нута, ложную граммовую совместимость и недоказанный итог недели.

## Gauntlet

**Код:** [PR #3](https://github.com/xxiamadelxx-blip/foodplan-ai/pull/3), SHA после последней правки `6136e1a`. Workflow:
- [Android CI #39996462216](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39996462216) **PENDING** на момент документации.
- [M4 real public SKU #39996462227](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39996462227) **PENDING** на момент документации.
- [M6 unsigned #39996462265](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39996462265) **PENDING** на момент документации.

**Критерий:** новые реальные 2 страницы должны дать цену физической массы с подтверждённым SHA и свежей датой; JSON фактической недели должен показывать **4/4** неизвестных пересчёта (banana, chickpeas, egg, oil), **ноль ложных граммовых закупок**. Даже успех означает улучшение наблюдаемых цен, а НЕ закрытие недельного бюджета.

**Внешние барьеры:** M2 независимо проверенные 150 приготовленных рецептов 0/150, физическая проверка Android, постоянный signed release. Не подменять их Python-тестами.


## Результат независимого live source Gauntlet (код SHA 6136e1a)

[M4 Actions #39996462227](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39996462227) **workflow PASS по узкому source-контракту**; [оригинальные JSON evidence #12519404791](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39996462227/artifacts/12519404791) независимо разобраны:

- **35/35** официальных кандидатов запрошены (1 общая проверка robots, 35 GET SKU); **33/35** получили свежие исходные HTML SHA + видимую цену, два отказа сети: `cucumber = URLError`, `potato = RemoteDisconnected`. Это NOT VERIFIED, цены не перенесены из истории.
- `banana`: `Бананы 1кг`, 14 599 коп. за 1 кг **с кожурой**, `pack_unit=unpeeled_g`, `pack_quantity=1000`, `purchase_count_from_grams_proven=false`.
- `chickpeas`: `Нут Green Ribbon 400г`, 5 999 коп. за банку **вместе с заливкой**, `pack_unit=net_with_liquid_g`, `pack_quantity=400`, `purchase_count_from_grams_proven=false`.
- `egg`: 8 829 коп. за 10 шт., `oil`: 12 599 коп. за 900 мл, оба сохраняют UNKNOWN при пересчёте.
- Реальная неделя Kotlin `7/21/33`: **31/33** текущих ингредиентов имеют видимый ценник физической единицы, но **27/33** имеют доказанное количество закупаемых единиц; `missing=[banana,chickpeas,cucumber,egg,oil,potato]`, `unit_unknown=[banana,chickpeas,egg,oil]`.
- Строго известный подытог **688 648 коп. = 6 886,48 ₽**; полная сумма `null`, признак укладывания в бюджет `null`, 0 ₽ за неизвестные продукты не записаны.
- Это важное независимое разделение: SOURCE-M4 не FAIL на работу парсера, но CURRENT-BASKET-M4 **PARTIAL**, и пользователь не должен видеть фальшивую недельную оценку.
- [M6 unsigned #39996462265](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39996462265) **PASS по сборке, не по подписи**. [Android #39996462216](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39996462216) JVM/debug PASS, эмулятор ещё выполнялся на момент этой записи.

Следующая техническая работа допустима только при новом оригинальном подтверждении двух временно недоступных SKU или реальном доказательстве съедобного выхода для конкретного товара. Нельзя «закрыть 33/33» подстановкой исторического ценника, общим процентом кожуры или средней массой зерна чужой банки. После трёх неудачных независимых попыток источник фиксируется как BLOCKED, не запускается бессрочная проверка.


## Final Android Gauntlet (exact code SHA 6136e1a)

[Android CI #39996462216](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39996462216) **PASS**, `static-and-unit` **SUCCESS**, `emulator-smoke` **SUCCESS**. Emulator independently ran **53/53 Android instrumentation tests**, **0 failures, 0 skipped**, `BUILD SUCCESSFUL`. Two new UI tests prove public list and direct product card readers preserve non-edible unit bases, reject wrong weights, and don't reintroduce a gross SKU as edible grams.

The M5 separate host-side ADB `force-stop → cold relaunch` also **8/8 PASS** on the same SHA. This proves emulator restart persistence, not a physical-device release. [M6 unsigned #39996462265](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39996462265) PASS, production keystore NOT VERIFIED.

**Acceptance:** unit-source anti-hallucination code PASS; retailer public source 33/35 verified; current seven-day budget PARTIAL (27/33 quantity-countable, 31/33 observed physical pack prices, 6 unknown, total null). M2 recipe evidence BLOCKED 0/150; public release BLOCKED.
