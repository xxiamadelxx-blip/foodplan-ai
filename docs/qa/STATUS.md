# FoodPlan AI · Gauntlet CI Evidence · 20 October 2026

Source repository: https://github.com/xxiamadelxx-blip/foodplan-ai

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
