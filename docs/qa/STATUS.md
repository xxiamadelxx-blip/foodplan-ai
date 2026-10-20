# FoodPlan AI · Gauntlet CI Evidence · 20 October 2026

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
