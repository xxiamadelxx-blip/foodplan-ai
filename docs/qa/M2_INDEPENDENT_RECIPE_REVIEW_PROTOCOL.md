# M2 · приёмка 150 домашних рецептов (актуальная редакция 24.10.2026)

> **Исправление чрезмерного критерия:** физически приготовить, измерить и сфотографировать все 150 блюд **не требуется**. Ранее записанное требование обязательного измеренного выхода каждой партии и двух персональных независимых проверяющих каждого блюда **отменено**. Оно не исходило от пользователя и блокировало разработку по неоправданной причине.
>
> Источник решения: [CONTEXT.md, §10](../../CONTEXT.md); [MILESTONES.md](../../MILESTONES.md). Эту редакцию применять вместо прежней модели «150 лабораторных приготовлений».

## Смысл M2

Дать приложению **не менее 150 разных, воспроизводимых, понятных домашних рецептов**, пригодных для детерминированного подбора, с корректными количествами сырья, инструкциями и **ориентировочным**, честно обозначенным КБЖУ. Проверка источников и арифметики обязательна, но «настоящий готовый выход каждой нашей партии, измеренный своими весами» для обычного потребительского приложения не обязателен.

### Критерии на каждый рецепт

| Проверяемый аспект | Что принимается | Что запрещено |
|---|---|---|
| Самостоятельность рецепта | Уникальный рецепт, не случайная комбинация из 200 прототипов | Повторы под новым названием, бесконечные комбинаторные шаблоны |
| Готовка | Ясные шаги, приемлемые порции, время, оборудование и ссылка на независимый опубликованный рецепт либо подходящую проверенную технологию приготовления с документированными адаптациями | Самодельная «независимая экспертиза» без источника; ссылки на неродственную технологию |
| Ингредиенты | Количества, физическое состояние, съедобная часть и корректные справочные коды/источники; особенности стоков, кожи, костей, рассола и воды фиксируются там, где значимы | Сухую крупу считать варёной массой; вес покупной упаковки считать съедобным без основания |
| КБЖУ | Расчёт по нутриентным базам с указанным basis, применимым YF/RF или опубликованными cooked food values; маркировка «ориентировочно» | Выдавать расчёт за лабораторный анализ, произвольно умножать YF на всё смешанное блюдо, заявлять непроверенную точность |
| Конечный выход блюда | **Необязательное** индивидуальное измерение. Если есть документарный коэффициент выхода, применимый к конкретному способу, допускается **оценка**, помеченная отдельно; иначе итоговые готовые граммы **UNKNOWN** | Записывать сырые граммы как фактически приготовленную массу |
| Безопасность | Жёсткая фильтрация аллергий/диеты по известным ингредиентам до ранжирования. Маркировку следов аллергенов проверять **отдельно** для выбранного SKU, и UNKNOWN не означает «безопасно» | Объявлять конкретную упаковку безопасной по ссылке на рецепт или цену |
| Повторная проверка | Автоматическая независимая проверка арифметики и ограничений + документированный выборочный экспертный просмотр существенных расхождений | Требовать двух внешних рецензентов и приготовление каждой позиции как стандартный gate |

### Выборочная практическая проверка

Если возникли спорные сроки готовки, масштаб порции, впитывание масла, влажность или необычное сочетание, готовку конкретного блюда **можно** проверить отдельно. Это *risk-based QA*, а не требование приготовить 150 раз. **Число образцов пока не утверждено** и не должно автоматически вводиться как новый блокер. Для очевидных обычных домашних блюд предпочтительна проверка по справочной технологии и расчётам.

Внешняя публикация тоже не гарантирует съедобности/безопасности произвольной адаптации. Спорный рецепт остаётся `NEEDS_REVIEW` или исключается из пользовательского каталога до устранения сомнений; нельзя повышать его статус путём заполнения фиктивных CSV-полей.

## Методика расчётного КБЖУ

1. Сопоставить съедобный исходный продукт (сухой/сырой/консервированный без жидкости и т.п.) с конкретным источником (USDA FoodData Central, достоверная маркировка, опубликованная таблица).
2. Умножить КБЖУ на массу съедобного продукта, учесть реально используемые масла/жиры и отбрасываемые жидкости в зависимости от метода.
3. Для обработанных ингредиентов использовать доступные опубликованные значения **приготовленного** продукта либо подходящие коэффициенты изменения выхода `YF` и сохранности нутриентов `RF` для метода и группы продуктов. Указывать происхождение коэффициентов. Вода влияет на концентрацию на 100 г, но не добавляет энергию.
4. Для многокомпонентного блюда не считать автоматически, что сумма массы сырья = масса после готовки. Если нет оснований вывести finished yield, не показывать точный готовый вес или КБЖУ «на 100 г готового блюда»; для рецепта/порции можно показывать **оценочную** энергию из фактически учитываемых ингредиентов с ограничениями расчёта.
5. Фиксировать неопределённости без поддельной точности и не сертифицировать микроэлементы/врачебные диеты по одному примерному БЖУ.

Метод подтверждён FAO/INFOODS: таблицы коэффициентов YF/RF можно получать из опубликованной литературы, а не только путём собственного измерения всех блюд. EuroFIR рекомендует учитывать способ приготовления и удержание нутриентов; простое суммирование сырых данных не считается достаточным точным анализом готового блюда.

- FAO/INFOODS *Food Composition Study Guide*, раздел 2.3 «Recipes»: https://www.fao.org/4/ap810e/ap810e.pdf
- FAO *Food Composition Data*, приложение «Calculation of composition of dishes prepared from recipes»: https://www.fao.org/4/y4705e/y4705e23.htm
- EuroFIR *Report on recipe calculation procedures*: https://www.eurofir.org/report-on-collection-of-rules-on-use-of-recipe-calculation-procedures-including-the-use-of-yield-and-retention-factors-for-imputing-nutrient-values-for-composite-foods/
- USDA FoodData Central: https://fdc.nal.usda.gov/

## Текущий код, миграция и доказательства

**На 24.10.2026:** присутствуют 150 авторских черновиков, 200 тестовых комбинаторных прототипов и 37 пищевых эталонов с привязанными источниками сырья. Это ещё не 150 документарно проверенных технологий. Ничего самовольно не повышаем до `INDEPENDENTLY_VERIFIED`.

**Существующий `RecipeEvidenceAudit.kt`:** enum `UNVERIFIED_COOKED_YIELD`, `cookedYieldGrams` (по договору типа именно измеренная масса), `cookedYieldSourceReference` и общий флаг `INDEPENDENTLY_VERIFIED` привязаны к **старому** строгому подходу. **Переопределять значение `cookedYieldGrams` как литературный прогноз нельзя**. Нужна отдельная модель `estimatedFinishedYield`/источник YF, отдельные уровни `documented method`, `calculated nutrition`, `physically tested` и `SKU label clearance`, затем обновление аудита и тестов. Без этой миграции не объявлять код M2 PASS.

**Существующие CSV и `tools/m2_review_intake.py`:** это историческая очередь **углублённой внешней проверки**, а не обязательный путь приёмки всех 150 рецептов. Нельзя автоматически вводить в release gate её колонки «двое экспертов», «измеренный выход», «проверка SKU-этикетки» для всех без разбора. Корректность этих полей и защита от подделок нужны там, где добровольное исследование действительно проводится.

**Следующие шаги:** (1) обновить программные стадии evidence без ложных измерений, (2) обеспечить 150 документированных рецептов/технологий из независимых опубликованных источников, (3) добавить доступные коэффициенты/оценки и прозрачные UNKNOWN, (4) проверить каталог алгоритмами, выборочной экспертизой спорных случаев и Android QA. Проверка аллергенов конкретных SKU остаётся отдельным жёстким блоком там, где пользователь указал аллергию.

**Текущий статус M2:** основной каталог и эталонные данные **частично PASS**; документарное покрытие технологий и корректный расчёт cooked values **OPEN**; прежнее требование 150 реальных приготовлений **REMOVED FROM GATE**; полный M2 **NOT YET ACCEPTED**. Изменение критерия не является доказательством реализации.


## 24.10.2026 · Kotlin M2 document-only gate: implementation

The actual gate now lives in `RecipeEvidenceAudit.kt`, not in the historical optional kitchen-trial CSV. Review records are separate `RecipeDocumentaryReview` objects, bound to the exact recipe content with a SHA-256 fingerprint of recipe ID/name, ingredient quantities, steps, appliances, and time. A recipe edit invalidates the old review; a bare `nutritionVerified=true` or `INDEPENDENTLY_VERIFIED` enum does not waive the documented source requirement.

- `publishedMethodSource` + `methodSnapshotSha256` + `methodApplicabilityNotes` + `reviewRecordReference`: written evidence that the external published cooking method is applicable to our precise recipe. URL/sha syntax is only a preliminary check, **not** automatic authentication of the site's contents.
- `nutritionBasis=RAW_EDIBLE_INPUT_ESTIMATE` means numbers are approximate **input edible grams**, not grams of the cooked food or medically complete nutrition. An `independentlyRecalculatedRawNutrition` set of four macro/calorie values is mandatory for documentary acceptance and must agree with the original production Kotlin calculation within 0.001. This checks arithmetic only; external nutrient sources and raw/cooked basis must also be manually matched.
- `INGREDIENT_SPECIFIC_PUBLISHED_FACTORS` requires references keyed to actual recipe ingredient IDs, not a generic factor silently applied to the whole cooked dish. Existing on-screen raw calculation does **not** magically turn into cooked values merely because factor references exist.
- Optional `estimatedFinishedYieldGrams` is distinct from original `OfflineRecipe.cookedYieldGrams` (actually measured weight). Neither an optional estimate nor a physical trial is required for `documentedReviewed`. Their absence appears as **nonblocking notices**. A provided but invalid estimate or a bogus claimed physical trial remains a hard audit error.
- `SKU_LABEL_TRACES_UNKNOWN` is separate from cookbook acceptance, remains never cleared by recipe evidence, and still has to be handled by the retailer / allergen filter for the exact product.
- `CatalogAudit.meetsM2Gate` now requires **at least 150 authored documentary-reviewed recipe IDs**, without mandatory 150 physical trials. Combinatorial templates are excluded. No sources have been invented; actual default documentary acceptance is **0/150** until sourced records are reviewed.

**Current export:** `gradle :domain:generateM2DocumentaryReviewQueue`, artifact `m2-documentary-review-queue` contains the 150 original recipe IDs, content fingerprints, all raw ingredient source links, preparation steps, provisional input kcal, blank external method/hash/checklist/reference, blank independently recomputed 4/9/4 totals and OPTIONAL blank trial/finished-yield fields. `PENDING_DOCUMENTARY_REVIEW` is always the default. The previous `generateM2IndependentReviewQueue` + `tools/m2_review_intake.py` describes a **deeper optional** investigation and cannot impose physical trials on the current M2 gate.

**Gauntlet:** Kotlin regression tests cover no mandatory cooked yield or SKU label, invalid factors, stale source reviews, independent arithmetic mismatch, 200 templates excluded, no accidental zero-proof promotion and 150-row source queue. Exact-code Android CI is required before declaring this engineering change PASS.


## Обновление 24.10.2026 · первый подтверждённый документарный метод

**Первая действующая запись:** `cur_omelette_tomato`, [источник технологии](https://1000.menu/cooking/50028-omlet-s-pomidorami-na-skovorode), [проверяемый отчёт](M2_FIRST_DOCUMENTARY_REVIEW_20261024.md). Исходный рецепт FoodPlan и краткая проверенная фактологическая выжимка метода имеют независимые, замороженные SHA-256; оригинальную полную HTML-страницу хеш выжимки не подтверждает. КБЖУ сырого съедобного сырья пересчитано отдельно; cooked yield и SKU-label безопасность UNKNOWN.

**Gauntlet PASS:** [Android #40109755175](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40109755175), executable/test SHA `605863fd7c9d7b16e2a3a9372ad668bb0264661c`: JVM+APK, 77/77 Android instrumentation, 8/8 emulator cold restart. **M2 documentary gate 1/150; remaining 149 source checks OPEN.** Старые записи `0/150` до этого SHA исторические, не новая текущая сводка. Не требуется готовить все 150 рецептов; источник методики/правдоподобная адаптация не превращает блюдо в сертифицированную диету.


### YAR-6 status after second source batch (2026-10-24)

`RecipeEvidenceAudit.report()`: **5/150** documented source-backed authored methods after [Android CI #40111932330](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40111932330), exact `e4818102`, 77/77 Android emulator PASS and 8/8 OS cold start. See [M2 batch2 audit](M2_METHOD_BATCH2_20261024.md) for sources, adaptation differences and independent dry/raw edible input calculations. Remaining 145 methods are OPEN, not pre-approved. Physical cooking of every recipe remains **unnecessary**.


### 2026-10-24 · Третья партия с фактическим Gauntlet

[Третий документарный разбор](M2_METHOD_BATCH3_20261024.md) подтвердил ещё три авторских метода внешними опубликованными источниками и отдельной арифметикой **сырого съедобного сырья**, без выдуманных cooked yield/SKU labels. [Exact-SHA Android CI #40118929391](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40118929391), `3ea53ae8ba36611bc7968a3bac4fd62f02e9f12a`: JVM и APK PASS, **77/77 Android PASS, 8/8 emulator force-stop→cold-relaunch PASS**. **M2 documented methods 8/150 PASS, 142 OPEN**. Lemon juice/peeled whole lemon nutrition mismatch and 15-minute oats versus overnight recipe remain blocked for those drafts.


## 24 October 2026 · Bulk intake rule (user-requested)

Review **20 distinct authored method candidates in one batch** rather than only 3; independently source-check each published cooking method, compare steps, ingredients and substitutions, independently tabulate edible raw K/P/F/C, store exact frozen fingerprints and source excerpt SHA, and gate all accepted candidates in one Kotlin/Android CI. **Reviewed candidates are not automatically admitted**: fail-closed unsafe/no-method/adaptation mismatches individually, continue with the others. Evidence and explicit three rejected candidates in [M2 bulk20 report](M2_BULK20_INTAKE_20261024.md). First 20 reviewed, 17 submitted, 3 rejected. **Accepted total remains prior 8/150 until CI #40120951034 concludes**, potentially 25/150. Avoid physical cooking of all 150 and no fabricated post-cooked weights/SKU labels.


### Bulk intake Gauntlet, 24 October 2026

M2 source comparison now reviews **20 independently researched authored-method candidates per batch**, with SHA-pinned source-fact excerpts, recipe fingerprints, explicit adaptation notes and raw ingredient nutrient calculations. Three source mismatches in the first batch rejected individually; **17/20 admitted**. [Evidence and reasons](M2_BULK20_INTAKE_20261024.md). [CI #40120951034](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40120951034), source SHA `536bbecd1944658313479b80e9fc017271e91f7e`: JVM/APK PASS, 77/77 Android instrumentation PASS, emulator 8/8 restart PASS. **25/150 documentary accepted, 125 still pending.** Batch size concerns candidates, not automatically accepted recipes. No physical preparation of all 150 mandated.
