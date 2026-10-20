# FoodPlan AI · Gauntlet CI Evidence · 20 October 2026

## M2 independent audit report · 20 October 2026

- **PASS, domain unit tests and debug APK build**: [GitHub Actions #39468768873](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39468768873).
- **PASS, independently read CI artifact**: [m2-recipe-evidence-review, artifact #12286093607](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39468768873/artifacts/12286093607), CSV file `recipe-review.csv`. Direct independent CSV parsing confirmed 276 data rows, 200 `GENERATED_TEMPLATE`, 76 `CURATED_DRAFT`, and **0** `nutrition_verified=true`.
- **PASS, weight basis**: no `UNKNOWN_BASIS` strings in 276 report rows. A new ingredient-state registry records raw, dry, canned/drained, frozen and packaged weights and labels these on the Android recipe screen.
- **PASS, release fail-closed**: each of 276 rows still shows `ALLERGENS_UNREVIEWED`, `NUTRIENTS_UNSOURCED`, `RECIPE_SOURCE_MISSING`, `UNVERIFIED_COOKED_YIELD` and `UNVERIFIED_RECIPE`. No recipe is independently accepted.
- **PENDING, Android emulator**: the `emulator-smoke` job from Actions #39468768873 had not finished at time of evidence export. Do not report it PASS unless its status is verified.
- **M2 GATE = FAIL**. CSV is an inspectable evidence inventory, not user-ready certified nutrition information. Do not issue final APK.


## Текущая приёмка M2: 76 самостоятельных черновиков

- **Рецепты**: 76 отдельно составленных черновиков + 200 комбинаторных шаблонов = 276 элементов каталога. Шаблоны не считаются выполнением рецептурного M2.
- **Источники**: 7 из 38 ингредиентов содержат точные ссылки на записи USDA Foundation Foods: рис, куриная грудка, цельное яйцо, брокколи, белокочанная капуста, огурец и чеснок. Это не подтверждение российского SKU или следовых аллергенов.
- **PASS (промежуточный)**: новая партия из 16 рецептов прошла JVM unit-тесты [GitHub Actions #39466828594](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39466828594) до более поздних изменений. Полный PASS самой последней ревизии проверять отдельно.
- **Gauntlet FAIL (выпуск заблокирован)**: 0 из 150 рецептов прошли независимую проверку рецепта, происхождения КБЖУ, режима массы сырья и маркировки аллергенов.
- **USDA candidate research**: [29 групп продуктов](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39467347614) выгружены в отдельный артефакт. Попутно обнаружена и исправлена ошибка формата JSON-манифеста; новый CI должен проверить его чтение.
- **Экономия CI**: добавлено предотвращение параллельных устаревших Android-прогонов. CANCELLED для старой ревизии не равно FAIL исходного кода.
- Этап M3 не принимается раньше M2. Корзина остаётся внутри FoodPlan AI, без оформления магазинного заказа.


## M2 current gate · 76 authored drafts, 20 October 2026

- **IMPLEMENTED**: 76 independently written, source-unverified recipe drafts split across curated recipe batches, plus 200 generated templates. Total 276 records; **independently verified: 0/150**. Prior sections listing 24 recipes are historical.
- **IMPLEMENTED**: independent tests for added 16 recipes, distinct ID, weights, allergen tagging and required kitchen equipment. Added an explicit mass-state registry for all 37 ingredients (raw/dry/drained/frozen/from package) and visible labels in Android recipe view.
- **PENDING QA**: [Android CI #39467869243](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39467869243) for latest mass-state UI test build. No acceptance is inferred until all jobs finish successfully.
- **M2 RELEASE GATE FAIL**: raw/cooked yield and label-level allergen provenance, complete source-checked macros, independent preparation review and 150 real verified recipes remain incomplete. M3–M6 cannot be accepted.


## M2 cookbook milestone · 20 October 2026

- **PASS Kotlin/JVM**: 24 independently authored recipe drafts added with raw ingredient weights, cooking directions, time, effort and kitchen equipment. 200 generated templates remain separate for regression; total in-memory catalogue: 224.
- **PASS Kotlin/JVM**: `CuratedRecipeDraftsTest` checks unique dishes, ingredient IDs and quantities, equipment, diet exclusions and declared allergens. Fixed 3 ambiguous/over-short directions and corrected no-appliance scenario to use cold breakfasts and salads.
- **PASS Kotlin/JVM**: `RecipeEvidenceAuditTest` rejects recipes with unsourced ingredients, unreviewed allergens or absent recipe sources. Independent verified count is **0/150**. All 24 curated drafts are **unverified**, not ready for safety/clinical claims.
- **PASS CI**: [Actions #39456309444](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39456309444) completed static/JVM, debug APK and Android emulator successfully for corrected curated drafts.
- **PASS CI static + JVM + APK**: [Actions #39457005938](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39457005938) includes offline USDA FDC candidate-import tests and quality gate; latest emulator job was still running at report time.
- **PARTIAL USDA SOURCE MATCH**: GitHub Actions #39458032749 downloaded the official April 2026 Foundation Foods CSV and produced 5 candidate lists after fixing false substring matches and incomplete nutrient rows. Two ingredient-level values were matched by exact raw-food description and FDC ID: white long-grain rice #2512381, skinless boneless chicken breast #2646170. Allergen/brand equivalence and entire recipe provenance remain unverified. No complete recipe may count as independently verified.
- **PASS**: [Actions #39458032749](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39458032749) exported real, complete-nutrient USDA candidates; downloadable artifact #12280039876 remains NOT VERIFIED.
- **M2 status**: IN PROGRESS / RELEASE FAIL. M3–M6 must not be accepted until independent content validation is complete.
- Source and acceptance details: `docs/qa/M2_SOURCE_PROVENANCE.md`.


## Budget-based internal shopping · updated 20 October 2026

**Current contract:** FoodPlan keeps grocery list internally. Retailer websites are data sources for product variants, pack sizes, observed prices, star ratings/review counts and availability; no transfers to retailer shopping carts. Obsolete cart-handoff source/tests removed from main.

**Gauntlet evidence:**
- **PASS** [CI #39450399813](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39450399813) after retry: static audit, domain unit tests, debug APK and Android emulator (first attempt failed during Android SDK/ADB startup; rerun finished successfully).
- **PASS** [CI #39451567023](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39451567023) static/unit build on most recent source test changes; emulator job scheduled/running at report time.
- **PASS** budget-fixture comparison 2 500 ₽ vs 25 000 ₽ under the same weekly ingredient requirements: low budget does not pick out-of-budget high-rated luxury SKU; high budget can improve review quality while not forcibly spending the full ceiling.
- **PASS** price integrity: unknown city/store, stale or missing quotes do not establish budget compliance; out-of-stock excluded; no fictitious zero-price products.
- **PASS** budget-week refiner prototype: over-budget plan attempts bounded safe meal substitutions; generous budgets may increase ingredient/protein diversity if rated source products cover a complete basket; otherwise MISSING_PRICES.
- **PASS** removed all five obsolete store-cart-transfer source and test files from GitHub repository.
- **PASS** Magnit catalog DOM extract parses observed rating and review count from HTML structure; ratings missing from actual cards remain missing, not zero.
- **NOT ACCEPTED** live address-scoped store inventory, complete real catalog, textual review analysis, 150 independently verified recipes and clinical allergen safety, global optimal nutrition/price plan, all-Russian-store support, production release.

**No user APK release** until independent evidence-based M2–M6 acceptance.


## АКТУАЛЬНЫЙ статус M3/M4 · 20 октября 2026

**Scope исправлен:** корзина существует только внутри FoodPlan AI. Отменены все планы автоматического наполнения магазинной корзины, интерфейс и код handoff удалены. Старые пункты ниже сохранены как исторические записи и не являются действующими требованиями.

**Реализовано в ветке main:**
- BudgetBasketOptimizer: совокупная стоимость полной недельной корзины по фасовкам, отличие 2 500/25 000 ₽, выбор альтернатив с учётом рейтинга и числа отзывов, неподтверждённое наличие/пропущенные цены.
- BudgetWeekRefiner: ограниченный пересчёт блюд на дорогой неделе и разнообразие белков при большом бюджете; запрещено считать отсутствующие цены нулевыми.
- Магнит WebView: чтение видимых карточек, рейтинга и числа отзывов (при наличии), локальное сохранение; магазинная корзина не меняется.
- UI: самостоятельная продуктовая корзина, цены по городам и магазинам, отзывы и уровень уверенности, кнопка пересчёта меню по бюджету.

**Предельные ограничения:** каталог неполный, наличие конкретной торговой точки пока не подтверждено, рецепты/КБЖУ ещё черновые. M3, M4, M6 остаются IN PROGRESS; релиз закрыт. Независимые тесты см. docs/qa/BUDGET_GAUNTLET.md и [Actions](https://github.com/xxiamadelxx-blip/foodplan-ai/actions).

Исторический журнал разработки:


Source repository: https://github.com/xxiamadelxx-blip/foodplan-ai

## M2 work in progress
- Added domain taste survey (25 dishes), deterministic ranking and candidate rejection reasons. Await CI confirmation.
- M2 remains NOT ACCEPTED: recipe catalogue with nutrition/provenance and actual in-app candidate recommendation not complete.

## M2–M5 implementation checkpoint · 20 October 2026

Code in main now contains the following, under **prototype** status:
- Survey of 25 dishes with persisted votes and deterministic tag scoring.
- 200 **generated recipe combinations**, not 150 independently validated full recipes; estimated nutrient values and recipe steps.
- End-to-end generation of 7 days with meal scaling and diet/ingredient/time/kitchen filters.
- Aggregated groceries with family-size scaling, full-week pantry deduction, provisional package counts.
- Manual pack prices in RUB, independently scoped by city and selected retailer, always unverified as live prices.
- Meal replacement recalculates day macros and full-week shopping; strict allergen/dislike/known ingredient checks.
- Reports and release gate: docs/qa/END_TO_END.md, docs/qa/RELEASE_GATE.md.

**Most recent confirmed CI:** GitHub Actions #39432169349 successfully completed JVM/static + emulator for recipes and instruction scaling. #39431844471 confirmed manual pack-price UX. Newer modifications for Russian dislikes and stronger taste ranking are undergoing further CI, see GitHub Actions #39432856803. Do not mark these new changes Android-pass before result arrives.

**Not accepted:** 150 independently verified recipes, traceable nutritional facts, allergen trace/cross-contamination guarantees, live Russian shop prices, budget-constrained optimization, full independent safety QA, signed user release.

## Experimental retailer catalog capture · 20 October 2026

- **PASS (code/JVM)**: Catalogue price model, current displayed price parser, pack-size extraction, matching of Russian product titles, city/store association, six-hour expiry, product-specific pack arithmetic and budget estimate. [CI #39438089262](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39438089262) has completed static + JVM checks successfully.
- **PASS (earlier Android milestone)**: Android WebView integration compiled; save/read observed offers by SKU, city, shop; shop quotes presented separately from manual prices. [CI #39436881434](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39436881434) passed.
- **PENDING (latest Android)**: Browser DOM WebView instrumentation test + matcher safety change; final emulator result of [#39438089262](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39438089262) is still running at report time.
- **NOT VERIFIED**: Actual direct catalog scraping in user-selected Russian delivery address on physical Android. Browser DOM selectors were inspected on a live Magnit catalog HTML capture, but saved offer from a live Android WebView is not independently demonstrated.
- **NOT IMPLEMENTED**: Product catalogs for other Russian retailers; background unattended full-catalog crawl, price comparison including delivery and loyalty discounts, real final checkout/stock validation.
- **Source behavior**: Perekrestok and 5ka direct technical fetch returned JavaScript challenge pages. Do not bypass provider protections.
- **Release decision**: M4 remains **IN PROGRESS** and M6 remains **NOT ACCEPTED**. No user APK release.

## Cart handoff progress · 20 October 2026

- **PASS (Android emulator simulation)**: [Actions #39445619093](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39445619093) includes a WebView test using a simulated product page with the observed Magnit `v-product-detail-add-to-basket-button`. App JavaScript located the official button and clicked exactly once. This does **not** verify a real seller-side cart.
- **PASS (JVM and APK)**: StoreCartTransfer builds a SKU/quantity queue from weekly shopping, omits stale/unavailable offers, preserves exact URL, handles packing and total. Unit tests and CI for this code passed.
- **IN DEVELOPMENT**: In-app shop handoff opens each selected real SKU in Magnit WebView and can attempt a user-requested official add-button click. User must verify actual shop quantity and confirm each SKU. Confirmed proposal matching from a visible catalogue page is possible in one tap.
- **NOT PROVEN**: no automated check of real logged-in Magnit basket, zero proof of 10-item unattended population, no final retailer-stock/checkout validation, no other-network cart builders. Release gate is NOT met.
- **Most recent code**: bulk confirmation of suggested SKUs from the current visible catalogue, see `445f5a9d2848ea8d73d81348dc7219a4d538db2b`; CI results for this commit require independent recheck.

## M1.1: техника и усилия готовки (20.10.2026)

- **PASS**: 23 кухонных инструмента/прибора доступны в анкете.
- **PASS**: три уровня с полными пользовательскими названиями, отдельными от длительности готовки.
- **PASS**: хранение `cookingEffort` и выбранного оборудования через Android DataStore, round-trip в инструментальном тесте; старые профили без ключа получают FAST.
- **PASS**: фильтр будущих рецептов учитывает требуемое оборудование, сложность (active time, techniques, dishes to wash) и отдельно время.
- **PASS**: JVM-тесты, сборка APK, компиляция Android-тестов, UI-тесты и сохранение анкеты в эмуляторе API 35.
- **PASS**: [GitHub Actions #39425235582](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39425235582), commit `0d07462b6e480bfa375505473d9fe52f7bf1c580`, artifact `foodplan-m1-debug-apk` ID `12265024946`.
- **NOT IMPLEMENTED**: фактический недельный планировщик и готовый каталог рецептов; M2/M3 должны подключить усилия к подбору реальных блюд.
- **NOT VERIFIED**: восстановление после убийства процесса отдельно от перезапуска DataStore; тестовая Debug-APK не является выпуском Play Store.

## Final verification for M0 + M1 initial APK
- **PASS**: Static schema/fixture audit and repository structure.
- **PASS**: Kotlin domain unit tests, including package pricing, pantry subtraction, city mismatch, unconfirmed price and allergen exclusions.
- **PASS**: Android debug APK assembled for API 26+.
- **PASS**: Instrumented Android UI smoke test using cloud emulator API 35.
- **PASS**: DataStore roundtrip for all profile fields in a fresh ProfileStore instance.
- **PASS**: GitHub CI workflow complete success, run [#39423600455](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39423600455), commit `96640f18ca9545914db5be71b9cc9fb2856b2371`.
- **PASS**: `foodplan-m1-debug-apk` artifact ID `12264942871` from same run.
- **NOT VERIFIED**: Separate adb force-stop/process-death restore test. DataStore reopen test is not equivalent to killing app process.
- **NOT IMPLEMENTED**: Real-time Russian retailer price adapters, 25 food-preference cards, 150+ verified recipes, seven-day plan; covered by later milestones.
- **NOT RELEASE**: Debug APK is a milestone test artifact, not signed release.

## Gauntlet corrections and independent checks
1. Initial CI [#39422189757](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39422189757) **FAIL** SDK setup installed removed `tools` package. Removed obsolete Android SDK action, used runner-preinstalled SDK.
2. Run [#39422415729](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39422415729) **FAIL** Compose 2026 BOM required AGP 9.1 / compileSdk 37. Pinned compatible Compose BOM 2025.09.01 with AGP 8.13.
3. Run [#39422818345](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39422818345) **PARTIAL** APK PASS, emulator test compilation FAIL due invalid test import.
4. Run [#39423275736](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39423275736) **PARTIAL** updated persistence test present, emulator still fails old import.
5. Final run [#39423600455](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39423600455) **PASS** static, Kotlin, APK and Android emulator after test-import correction.

**Note:** Actual app currently implements **M1 onboarding only**. M0/M1 milestone PASS does not mean nutritional planning/retail prices are complete.
