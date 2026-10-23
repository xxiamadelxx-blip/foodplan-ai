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
