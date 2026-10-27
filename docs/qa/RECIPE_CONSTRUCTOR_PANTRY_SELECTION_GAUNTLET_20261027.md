# FoodPlan · Product-first recipe replacement from home stock · 2026-10-27

## Bug reproduced

`RecipeConstructor.suggestReplacements` previously intersected every explicitly selected ingredient with `current.shopping` (the products still needing purchase). A positive home-stock quantity can suppress a shopping line; selecting that real food in the replacement constructor then returned **zero alternatives** even when eligible recipes used it. Moreover, selected-link ranking counted only ingredients already in the shopping list.

[RED Android CI #40462315394](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40462315394): `RecipeConstructorProductSelectionTest` failed, 337 JVM tests, exactly 1 failure. This was the intended reproduction, not an intermittent failure.

## Small fix, no AI

- Production `RecipeConstructor.kt` treats nonempty explicitly selected `selectedIngredientIds` as user constraints, without intersecting away pantry-only IDs. Unknown IDs still fail closed.
- Empty selection keeps the existing weekly-basket default.
- Recommendation soft priority for explicit choices counts matching recipe ingredients, not merely items already present on the buy list.
- Replacements still call `RecipeBatchPlanner.apply`, check diet/allergens/dislikes/equipment, preserve selected effort/servings/batch span, recompute all seven days and purchasable shopping quantities, and never label missing retailer prices as zero/affordable.
- Production three-tab `FoodPlanHomeScreen` and legacy `WeeklyPlanScreen` add positive home-stock ingredient chips. Items present only at home are shown with the suffix `· дома`. No technical extra tab or external AI added.
- `RecipeReplacementPantryAndroidTest` proves a pantry-only tofu chip can be selected inside the actual Android recipe-replacement dialog. `RecipeConstructorProductSelectionTest` checks product outside buy list, full-week preview, unchanged original basket and allergen fail-closed logic.

## Iterations and independent GitHub evidence

| Iteration | Exact evidence | Outcome |
|---|---|---|
| RED | [#40462315394](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40462315394) | New domain regression fails; baseline behavior reproduced |
| First fix | [#40462792576](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40462792576) | Product logic compiles, but test APK rejected a bad added `assertExists` import; import removed |
| Android audit | [#40463097230](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40463097230) | JVM/debug APK success; emulator 75/78, three old tests expecting now-obsolete UI sentences. New pantry UI test passed. Old text assertions updated, without skipping tests |
| FINAL | **SHA `edbe05cb475ea679b2f576a921e3c45f32c361b2`** [#40464156225](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40464156225) | `static-and-unit` SUCCESS; Android emulator **78/78 PASS, 0 failed/skipped**; **8/8 OS force-stop → cold-relaunch PASS**. Debug APK is uploaded as `foodplan-m1-debug-apk` artifact |

The release-candidate build and M4 public chain price tests on the exact final source SHA also completed SUCCESS, but these tests **do not demonstrate physical retailer inventory/pack price completeness or a signed release**.

## Scope limits

This closes **only this recipe replacement defect**, not the FoodPlan public-release milestones. M2 source-method review remains **91/150 documentary methods, 59 pending**; M3 complete diet sufficiency; M4 real chosen-store stock/price/package completeness; M5 **physical** Android handset; and M6 reproducible permanently signed distribution APK remain distinct open gates. Raw ingredient nutrition is an approximation. A source-link method review never certifies allergens of an actual SKU.

## Next stage

Prioritize user-visible release blockers (M3 diet adequacy, M4 actual pack completeness and M6 signing) while continuing M2 with 20–25 distinct remaining recipes per independent batch. Never reopen passed evidence or stall on a single casserole; after three failed corrections mark that candidate BLOCKED and proceed to another independent work item.
