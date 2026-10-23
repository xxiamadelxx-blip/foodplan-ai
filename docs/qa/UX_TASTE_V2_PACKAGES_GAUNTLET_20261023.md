# FoodPlan · UI and DataStore migration Gauntlet, 23.10.2026

**Code under test:** [d40ae76](https://github.com/xxiamadelxx-blip/foodplan-ai/commit/d40ae76a9b0c9c92a71bb561674f3316aba17157), working PR #3. This is **not merged to main** and is **not a public release**.

## Implementation

- `TasteSurvey.kt` catalog v2: exactly 25 recognizable dishes (15 everyday, 5 flavor contrasts, 5 varied); old restaurant-centered survey IDs no longer appear as mandatory cards. Existing unchanged IDs keep their meaning. New concepts get new v2 IDs.
- Old-known v1 votes are retained when decoding `ProfileStore` DataStore, so a historical `SavedWeekSelection` fingerprint can still match. `TasteSurvey.tasteWeights` scores **only current IDs**; removed dishes cannot influence meal ranking. Survey progress counts only current cards. This avoids a regression where filtering legacy votes invalidates a previously saved weekly choice.
- `ShoppingDisplayNames.kt`: 37/37 nutrient-reference IDs have separate human-readable UI titles. In groceries and recipe ingredients, no display of "куриное яйцо (съедобная часть)" or other technical raw reference names.
- Main grocery rows prioritize a **real observed package count, mass and indicative cost** only where verified by existing offer validation; otherwise display `Фасовка уточняется · Цена неизвестна`. Internal gram requirements appear in smaller secondary text. The ingredient's synthetic `packGrams` value is not misrepresented as a store SKU.
- M5 real OS cold restart script now accepts either confirmed retail packs or honest unknown packaging as a visible restored shopping row.

## Gauntlet evidence

1. **FAIL reproduced in old UI:** [Android CI #39944324392](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39944324392) showed 2 failed Android tests: hardcoded v1 first dish `Борщ` and removed `ramen` ID in DataStore write test. Both fixtures were corrected; no functional M0/M1/M2 tests were weakened.
2. **PASS current code:** [Android CI #39949084176](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39949084176), `static-and-unit` and `emulator-smoke` SUCCESS. Android emulator **48/48 tests**, **0 failed, 0 skipped**, plus debug APK and domain JVM tests.
3. **PASS actual restart:** host-side ADB on emulator, [artifact 12500157034](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39949084176/artifacts/12500157034): `PASS_EMULATOR_OS_FORCE_STOP_COLD_RELAUNCH`, **8 evidence steps**, PID changed `4911` → `4998`, the 3-day prepared meal, linked day 2 and grocery rows survived. Archive has `evidence.json`, `last-ui.xml` and 4 screenshots. `physical_android_verified=false`.
4. **PASS independent M4 sources:** [CI #39949084100](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39949084100). Retail price observations **33/33**, full-package weekly prices **31/33**, `egg` and `oil` still UNKNOWN, total weekly budget UNKNOWN, known subtotal 7 836.78 RUB from earlier independent artifact.
5. **PASS unsigned M6 technical build:** [CI #39949084206](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39949084206). This is **not** a signed user release.

## Scope boundaries and remaining acceptance

- **PASS:** code-level and emulator checks for the current taste examples, old-vote neutrality, human shopping labels and pack-first/fail-closed UI.
- **NOT ACCEPTED:** 25/25 offline meal illustrations and accessibility visual QA (P0-TASTE-02), independent testing of migration from a *previously installed v1 APK* on a physical Android phone, and remaining polish/QA of presentation across budgets (P0-PACKS-01).
- **M2 stays BLOCKED 0/150** independently reviewed cooked recipes; M3 nutrition sufficiency not independently accepted; M4 weekly total UNKNOWN for egg/oil; physical-device M5 NOT VERIFIED; production signing/public M6 RELEASE BLOCKED.
- No new M7/M8/M9 introduced. No photo/image assets were added in this cycle.

**Next independently executable task:** 25 meaningful offline visual assets with stable image-ID mapping, accessible fallback and UI tests. Continue only under the recorded P0 backlog, without calling it a release prerequisite or claiming the full M2/M6 gate passed.
