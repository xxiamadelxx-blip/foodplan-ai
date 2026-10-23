# M2: независимая приёмка 150 авторских рецептов

Дата: 21.10.2026. Этот протокол дополняет [M2_SOURCE_PROVENANCE.md](M2_SOURCE_PROVENANCE.md) и обязательный [USER_JOURNEY_CONTRACT.md](../USER_JOURNEY_CONTRACT.md). В пользовательском приложении он **не создаёт четвёртый экран**.

## Состояние на 21.10.2026

| Критерий | Доказательство | Статус |
|---|---|---|
| Авторских блюд с уникальными ID и названиями | `CuratedRecipeDrafts` + `Part2` + `Batch2` + `Batch3` + `Batch4` + `Batch5` | **150/150 черновиков** |
| Синтетических комбинаций, только для regression | `OfflineRecipes.generatedPrototypes` | 200, **0 учитываются как подтверждённые** |
| Ингредиентов с привязанным источником *эталонных сырых* нутриентов | `m2_fdc_reference_evidence`, `m2_sr_legacy_evidence`, `m2_russian_tvorog_candidate` | 37/37 source-linked |
| Независимо подтверждённых технологий приготовления | External cookbook / documented trial per recipe | **0/150; FAIL** |
| Измеренный реальный выход готового блюда в граммах | Batch-level finished cooked yield, **not** sum of raw ingredients | **0/150; FAIL** |
| Подтверждение реальной маркировки аллергенов каждого SKU | Изготовитель, GTIN, состав, следы аллергенов, фото конкретной упаковки | **FAIL** |
| Итог M2 | 150 независимо валидированных рецептов с подтверждённой технологией и составом | **FAIL, не готово** |

## Следующая проверяемая работа

В облачном CI задача `generateM2IndependentReviewQueue` экспортирует `m2-independent-recipe-review-queue` со 150 строками: оригинальные рецепты, полный текст всех шагов, масса сырья с basis registry, список эталонных пищевых источников, оценочная энергия **исходных ингредиентов**, потенциальные известные аллергены, оборудование и время.

Этот CSV преднамеренно содержит **пустые** колонки независимых доказательств, а `independent_verdict = PENDING_EXTERNAL_REVIEW`. Редактирование CSV **не изменяет Kotlin** и **не повышает** evidence status.

Экспертная проверка каждого блюда должна использовать первичные материалы, а не наши же заготовки:

1. **Независимая технология.** Внешний подробный рецепт или отчёт испытания с источником и неизменяемым snapshot hash; соответствие конкретным продуктам, исходным граммам, реальному процессу, времени и оборудованию. Рецепт с совпадающим названием, но другим соусом или способом нагрева, не подтверждает наш вариант.
2. **Состояние сырья.** Проверить, что 100 г относится к сырому мясу/овощу, сухой крупе, продукту без рассола или съедобной части. Документировать кости, кожуру, рассол и добавляемую воду отдельно от магазинной массы упаковки.
3. **Настоящий приготовленный выход.** Замерить массу всей партии **после приготовления**, с учётом выбранного способа охлаждения/стекания жидкости, и массу одной выдаваемой порции. Сохранить протокол с исходными весами, фото/доказательством и проверяющим. Сумма сырых граммов не равна cooked yield.
4. **Пищевая оценка готовой порции.** Исходные значения USDA/производителя, изменения массы при готовке, потери/увеличение жира и влаги, отдельно применимые коэффициенты сохранности нутриентов. Нельзя автоматически считать, что состав приготовленных 100 г совпадает с сырыми 100 г. Не применять чужой коэффициент выхода к многокомпонентному рецепту целиком без доказательства.
5. **Фактические аллергены.** Для конкретного российского SKU, если имеется упакованный товар, проверить торговую карточку против реальной упаковки, изготовителя/GTIN, состав, прямые аллергены и *следы*. Разные производители под одной карточкой не тождественны. Неподтверждённая маркировка = UNKNOWN, особенно при аллергиях.
6. **Независимая рецензия.** Записать источник, идентичность/отчёт первого проверяющего и независимого второго, дату, исходные артефакты и конкретные расхождения. Требования безопасности нельзя закрывать собственной автоматической оценкой.
7. **Отказ по неполным данным.** Недостающая ссылка на метод, спорная фасовка, неясный производитель, подозрительный cooked yield, недостоверная энергия или небезопасная маркировка остаются FAIL. Никогда не заменять пропущенный факт средним по магазину или самогенерированным текстом.

## Базовые методические источники (не индивидуальные доказательства 150 рецептов)

- FAO, *Food Composition Data*, процедура расчёта многокомпонентных блюд: https://www.fao.org/4/y4705e/y4705e23.htm
- USDA Agricultural Research Service, *The USDA Table of Cooking Yields for Meat and Poultry*, 2012: https://www.ars.usda.gov/research/publications/publication/?seqNo115=287023
- USDA ARS, *Table of Nutrient Retention Factors, Release 6*, для рассмотрения потерь отдельных нутриентов: https://www.ars.usda.gov/northeast-area/beltsville-md-bhnrc/beltsville-human-nutrition-research-center/methods-and-application-of-food-composition-laboratory/mafcl-site-pages/nutrient-retention-factors/
- USDA FoodData Central: https://fdc.nal.usda.gov/

Перечисленные источники доказывают **методологию**. Они не подтверждают cooked yield наших самостоятельно написанных 150 блюд и не гарантируют содержание российских товарных упаковок.

## Воспроизводимость / Gauntlet

- `RecipeEvidenceAudit.report()` должен сохранять `independentlyVerified = 0` до появления доказательств.
- `M2IndependentRecipeReviewExportTest` проверяет 150 строк, полный текст шагов, ссылочную пригодность 37 ингредиентов, отсутствие выдуманных значений и невозможность включения технических шаблонов.
- `CuratedRecipeBatch4And5Test` проверяет 50 новых рецептов, 15 завтраков / 35 основных блюд, ингредиенты, граммы, время, оборудование, учёт известных аллергенов и запрет на ложную сертификацию.
- Итог требует независимого GitHub Actions JVM+APK+эмулятор на **конкретном кодовом SHA**. PASS сборки не эквивалентен PASS пищевой приёмки.

Следующий научный шаг: отдельно рецензировать технологию и выход каждого из 150 блюд по оригинальным данным, а не добавлять ещё сотни генерированных комбинаций ради большого числа записей.


## 24.10.2026 · Immutable source-queue checks for returned reviews

A reviewer must **not** be able to make an unverified draft look approved by changing the original recipe columns or editing `missing_exact_product_allergen_trace_review` to `false`. `tools/m2_review_intake.py` now audits all three original missing-evidence flags and can compare a returned external review CSV with the freshly exported, unedited Kotlin baseline.

- Original baseline: `android/domain/build/reports/m2/independent-review-queue.csv`, generated by `gradle :domain:generateM2IndependentReviewQueue`.
- Independent reviewer returns a **separate** CSV with identical 150 recipe IDs and unchanged title, meal category, ingredients, cooking steps, raw basis, source linkages, timestamps/flags and unverified nutrition estimates; only the six external proof columns and `independent_verdict` may change.
- Run `python tools/m2_review_intake.py returned-review.csv --baseline android/domain/build/reports/m2/independent-review-queue.csv --output external-intake-audit.json` to reject tampered original recipe facts.
- A method URL and declared SHA-256, different reviewer IDs, measured cooked dish weight, yield/retention source and producer GTIN/label trace are still just submitted data. **A CSV with all these strings filled in is NOT independently authenticated.** The intake always reports `independently_certified=0` until a distinct evidence-backed human/external review process is actually available. No autogenerated draft receives `INDEPENDENTLY_VERIFIED` merely by editing text fields.
- Unit tests cover mutated original ingredient lists, self-awarded verdicts, missing SKU-allergen flag manipulation and identical reviewer identities. **Status: implementation, awaiting exact-code CI; M2 remains BLOCKED 0/150**.

Do not loosen the dietary/allergen gate to unblock product release without real package-level evidence.
