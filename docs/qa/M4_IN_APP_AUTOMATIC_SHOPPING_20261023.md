# FoodPlan AI · M4 Android: automatic in-app catalog → packages → shopping quote
Date: 2026-10-23. Scope: existing M4, not M7, not post-release-only GUI polish.

## Exact product requirement
User-selected store and exact budget lead to a **fully automatic read-only parser inside Android** that obtains official public product cards from previously verified catalog paths, parses current pack/price, and recalculates the user's actual 7-day grocery needs without asking ChatGPT to run CLI probes or handcraft a price per basket. The app's local deterministic planner must work even when the public store is offline or blocks access.

## Changes in development PR #3
- `CONTEXT.md` section 9 (also in main) and `docs/USER_JOURNEY_CONTRACT.md` pin this requirement.
- [3802fd0](https://github.com/xxiamadelxx-blip/foodplan-ai/commit/3802fd0ea4ef807501cdd012171f6ffd611fae88): `MagnitKnownProductPaths` now delegates URL discovery to **one canonical 35-SKU registry** `MagnitKnownSkuCatalog` instead of a narrower duplicate registry. 31 gram-package cases plus 4 special cases (egg, oil, banana with peel, canned chickpeas with brine) are identified; no price hardcoded.
- [7b1f4d4](https://github.com/xxiamadelxx-blip/foodplan-ai/commit/7b1f4d4cc01f49f19da193efb16abe2d1827f9b1): `MagnitAutomaticPriceUpdater` runs a network read for the selected *ingredient ID set*, **not** for every changed number of grams or ₽ budget. The successful scan is priced against the most recent quantities and budget, and source-backed mass offers are emitted for on-device DataStore persistence before the parent closes the scanning component.
- [c385ccf](https://github.com/xxiamadelxx-blip/foodplan-ai/commit/c385ccf7da2c4bfaebd5a1bc785db92ea341a41e): injected delayed network response test: change rice from 901 to 1601 grams and budget 356→100 while a single scan is underway. Expected: exactly one network pass, **three 800g packs**, 14,997 kopeks observed test cost, budget exceeded. These prices are fake *fixtures for test only*, never shipped as user prices.
- [bd8f2bc](https://github.com/xxiamadelxx-blip/foodplan-ai/commit/bd8f2bcf0719f2e3f70f19873235fefd16a1dade): Android official page fixture test confirms all 35 registered paths, including special banana and chickpeas offers with known pack **price** but unknown edible-gram count.
- [a91217a](https://github.com/xxiamadelxx-blip/foodplan-ai/commit/a91217a6751121626baebd7a0ab9457c2cea8dbb), [a03a87b](https://github.com/xxiamadelxx-blip/foodplan-ai/commit/a03a87ba6cbc3983893d3cb0f387913b63c66a8d): production `FoodPlanHomeScreen` injects the network seam for tests only. Actual main screen starts the built-in parser automatically, emits saved valid offers and renders real pack counts without a manual scanner tab. Android Compose integration scenario is independent from the raw HTML/robots parser tests.
- [bb40606](https://github.com/xxiamadelxx-blip/foodplan-ai/commit/bb406067d9f3e06008f06a16fa5d7ea1d1233da7): other stores explicitly report that automatic **price** integration is not yet implemented; VkusVill public search is name-only. No Mag price may become a price for Пятёрочка.
- [ff6a90d](https://github.com/xxiamadelxx-blip/foodplan-ai/commit/ff6a90d9250d110a6896ea76b5d668aaab19473d): Android regression test confirms unsupported store is still priced UNKNOWN.

## Gauntlet
- Code SHA under exact end-to-end QA: [ff6a90d](https://github.com/xxiamadelxx-blip/foodplan-ai/commit/ff6a90d9250d110a6896ea76b5d668aaab19473d).
- [Android CI #40021765748](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40021765748): **PENDING** at report creation; do not treat as PASS until JVM, test APK and emulator finish.
- [M4 public independent probe #40020949942](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40020949942): **PASS** for official source availability/parsing only. Such CLI probe is **not the application runtime**. [M6 unsigned #40020949947](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40020949947) PASS for packaging, not signed public release.

## Explicit remaining limitations
- The only currently price-capable tested embedded store is Magnit; VkusVill adapter discovers names without proof of price, other selected stores have no approved public price adapter. UI must not imply universal store coverage.
- For special SKU dimensions, egg/piece, oil/mL, banana unpeeled grams and chickpeas including brine, physical pack price does not establish how many packs cover edible recipe grams. That subtotal remains UNKNOWN. No unit fabrication or fake price.
- A successful Android fixture test does not prove real mobile internet access to the retailer from every location. HTTP refusal, robots denial, ambiguous pack/title must produce UNKNOWN; recipes, groceries and persistence remain fully offline-operable.
- M2 remains 0/150 independently reviewed cooking evidence; M3 nutrition remains partially unverified, M5 physical cold restart unverified, M6 permanent signing/public distribution blocked.

Next: obtain exact completed Android CI log on SHA ff6a90d; record PASS/FAIL. Fix one specific failure, <=3 Gauntlet cycles. After that, proceed to next independent non-blocked release criterion instead of indefinitely debugging one SKU.


## Gauntlet attempt 1 and targeted correction

- [Android #40021765748](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40021765748) JVM, debug APK and new tests compiled PASS. Emulator ran **71 tests, 70 passed, 1 failed**: the injected delayed response test read `901` grams while the test had just set `1601`. Other price paths and actual shopping UI tests passed. The external M4 source [#40021765811](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40021765811) and unsigned M6 [#40021765741](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40021765741) both PASS; source/network tests do not replace the Android UI gate.
- Root cause of that particular assertion: `runOnIdle` sets Compose test state, but returns before the next recomposition has necessarily applied the new `rememberUpdatedState` value. The fixture completed its HTTP deferred in that interval. [b5e3ac5](https://github.com/xxiamadelxx-blip/foodplan-ai/commit/b5e3ac59603767265773af8e88fbf24fca363ef2) adds a `SideEffect`-backed observed recomposition and only then releases the test response. Production code remains unchanged.
- **Attempt 2:** [Android #40022778907](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40022778907) PENDING. No PASS until 71/71 (or actual test total) and M5 process-death proof succeed. Repair cap: 3 cycles maximum; do not repeat the old blocked OS-font probe.


## Gauntlet attempt 2: PASS, Android installed runtime

- **Code SHA** [b5e3ac5](https://github.com/xxiamadelxx-blip/foodplan-ai/commit/b5e3ac59603767265773af8e88fbf24fca363ef2), which changes only the test timing after the first implementation. [Android CI #40022778907](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40022778907) **PASS**: JVM + debug APK and Android emulator **71/71 tests**, **0 skipped/0 failed**, `BUILD SUCCESSFUL`.
- This includes `MagnitAutomaticPriceAdapterTest` (official paths, robots, parsing and distinct physical units), `MagnitAutomaticPriceUpdaterTest` (901→1601g, three 800g retail packs at unchanged package price, budget change, **one** source pass), and `FoodPlanHomeScreenTest` (automatically observed product priced on the ordinary Products page; wrong selected store never inherits Magnit offers).
- Actual Android OS force-stop / cold relaunch in same run: **8/8 PASS**, `pid_before_stop=4917`, `pid_after_relaunch=4987`, 7-day week / 3-day batch/ linked day 2 and shopping still visible. [Artifact #12530899015](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40022778907/artifacts/12530899015), `evidence.json` checked independently. **Physical device not yet checked.**
- Actual debug APK archive [#12531037600](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40022778907/artifacts/12531037600) downloaded and extracted locally: `app-debug.apk`, **12,095,871 bytes**, SHA-256 `ed5bbfdb1c2a2db5bb2c1badfb280fcd8fcbd586cc55226d43066fba9db0e147`. This is debug, not store-ready signed release.
- **Independent official source read (separate from app!)** [M4 #40022778882](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40022778882) **PASS**: **35/35 observed package prices**, 36 HTTP GETs incl one shared robots; actual 7-day/21-meal basket **29/33 priced with known number of packages**, known subtotal **6,995.40 ₽**, missing full-week mass conversion **banana, chickpeas, egg, oil**. **Final total NULL/UNKNOWN and budget-eligibility UNKNOWN**, no stock, checkout or allergen guarantee.
- [M6 unsigned release CI #40022778883](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40022778883) PASS only technical packaging. No permanent production signature.

**Engineering acceptance:** the **in-app automatic Magnit source → quote → normal weekly grocery list → local basket repricing** implementation and its instrumented Android fixtures are **PASS**. Real phone public-page access by geography, true full-week price, dietary suitability of a specific physical SKU and other retailers' price adapters are **NOT VERIFIED / PARTIAL**. Do not label M2/M4 full-price/M6 public release PASS.

## 2026-10-24 · Follow-up fix: post-scan recalculation, independently PASS

The earlier 71/71 success on `b5e3ac5` demonstrated the in-progress scan test. A separate production risk remained: `FoodPlanHomeScreen` released the source scanner after publishing one quote but stored that quote by *ingredient set* only, potentially showing stale pack counts when grams or the budget changed with the same IDs.

- [65ae610](https://github.com/xxiamadelxx-blip/foodplan-ai/commit/65ae61096afbc2195918fd4c423b684287e1a705): separate the one-time scan result from reactive quantity/budget quoting.
- [ca8db0a](https://github.com/xxiamadelxx-blip/foodplan-ai/commit/ca8db0a996daad776d2868b45ea026cf84e98d77): retain source observations, then derive live product quotes from current weekly requirements while the source reader is unmounted; never claim extra sources.
- [6a04744](https://github.com/xxiamadelxx-blip/foodplan-ai/commit/6a0474412b10e98926aad5104f236b5a7689b7b7): instrumented regression checks a **second, post-completion re-quote** (2401g rice requiring 4 × 800g packs = 199.96 ₽, under 200 ₽), with network count still exactly 1. Test prices remain fixture-only.
- **Exact-code evidence:** [Android CI #40024028437](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40024028437) PASS JVM/debug APK, **71/71 Android UI tests** and **8/8 emulator OS cold-relaunch checks**, no skipped or failed tests. [M4 live public source #40024028403](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40024028403) PASS separately; [M6 unsigned #40024028462](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40024028462) PASS only for non-distributable packaging.

**Closure:** narrow automatic on-device source + local re-quote gate PASS. NOT claimed: real-price completeness of all ingredients, nutrition adequacy, independent cooking-yield verification, a physical handset run, or signed public release.
