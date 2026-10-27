# FoodPlan M2 · composition of independently sourced cooking operations

**Date:** 2026-10-26. **Baseline:** 87/150 authored published-method records accepted by exact-SHA Android Gauntlet #40281353345. **Pilot:** two previously pending authored dishes, `b4_savory_carrot_oat_bake` and `b4_buckwheat_apple_cups`. Both candidate records have exact recipe/step SHA-256, source-specific factual excerpts with SHA-256, ingredient-step coverage and independent raw-edible kcal/P/F/C. Neither publisher claims to have prepared FoodPlan's exact composite dish. Final new count **89/150 is contingent on exact code/test CI PASS**; until then the confirmed count remains 87.

## Why this is NOT fabricated recipe validation

Unlike the previous one-cookbook-method model, `COMPOSED_PUBLISHED_TECHNIQUES` explicitly means FoodPlan authored the combination, while **each actual preparation operation** was checked against at least one published technique and pinned to the current ingredient/step text. Only local excerpts are SHA-verified: this is **not authenticated publisher HTML** nor physical cooking. A changed dish, changed cooking step, changed source capture, invalid publication URL, duplicate/missing step, unaccounted-for ingredient or unsupported core temperature causes fail-closed rejection. The `SINGLE_PUBLISHED_METHOD` path is unchanged.

Full source methods and material substitutions are documented per step in `android/domain/src/main/kotlin/com/foodplan/domain/M2PublishedMethodComposed20261026.kt`. Export CSV now records `method_review_basis`, `composed_published_step_count`, `composed_published_step_source_links` rather than implying one whole-recipe publisher.

## Composed dish 1 · baked savoury oats, raw carrot and Gouda

- Authored `b4_savory_carrot_oat_bake`, recipe SHA `6acfabfe57e0e9447d411f10403b4fb5144bcd440171d3f63dabce2f16ea5dfa`.
- Original raw edible masses: oats 65g, carrot 100g, egg 60g, Gouda 35g, milk 65g.
- Source operation for grated carrot with oven oats: [Quaker's published carrot baked oats](https://www.quakeroats.com/cooking-and-recipes/carrot-cake-baked-oats). FoodPlan omits sweet ingredients, adds raw carrot earlier; the source's sweet frosting is NOT transposed.
- Source operation for oats+beaten eggs+milk+cheese: [Savory Baked Oatmeal and Veg](https://coffeewiththechickenladies.com/farm-fresh-egg-recipes/savory-baked-oatmeal-and-veg/). Original recipe uses pre-softened other vegetables, while FoodPlan grates raw carrots and skips the additional sauté, baking powder and toppings.
- Form/baking method: [Oats Everyday Mediterranean baked oats](https://oatseveryday.com/recipes/mediterranean-baked-oats/). Batter is evenly spread and oven cooked; FoodPlan uses a smaller dish and different liquid ratios, so the cited 45-minute timing is not automatically FoodPlan's 36-minute measured time.
- Ingredient-summed raw nutrition: **537.4 kcal, 27.694g protein, 22.1835g fat, 58.045g carbs** (before actual cooking and oil/water/yield changes).

## Composed dish 2 · buckwheat and apple baked cups

- Authored `b4_buckwheat_apple_cups`, recipe SHA `b15c308719c0ffbaf7f87750db935821d0c03fe30fe8c6da72de27aa20280eed`.
- Dry buckwheat 65g, raw apple 140g, egg 55g, wheat flour 20g, plain yogurt 75g served **cold after baking**, not in the oven batter.
- Boiling dry buckwheat groats first: [Bob's Red Mill procedure](https://www.bobsredmill.com/recipes/how-to-make/basic-preparation-instructions-for-buckwheat-groats). The cited 10–15 minutes is not guaranteed for all grain packs.
- Combining **already cooked** buckwheat/kasha with diced apple, egg and flour, portioning into muffin cups, then baking: [Edible Vermont buckwheat and apple muffins](https://ediblevermont.ediblecommunities.com/recipe/recipes-buckwheat-muffins-apples-and-walnuts). Other publisher additives (extra flour/fat/sweetener/nuts) are omitted, making FoodPlan's result likely denser. FoodPlan's cold yogurt service is an *authored garnish*, not a publisher-endorsed baking ingredient.
- Ingredient-summed raw nutrition: **494.5 kcal, 19.811g protein, 10.255g fat, 86.9945g carbs**. Neither calculation claims cooked weights or thermal nutrient loss.

## Safety and acceptance limits

A separate published [USDA FSIS safe-temperature chart](https://www.fsis.usda.gov/food-safety/safe-food-handling-and-preparation/food-safety-basics/safe-temperature-chart) recommends temperature-based cooking, with 74°C (165°F) for casseroles. FoodPlan now explicitly tells users to verify **74°C at the centre with a food thermometer** for both egg casseroles, instead of relying on visually "set" eggs. Requiring a reading does not prove anyone actually took one. The offline checker requires a documented official safety source and literal instruction corresponding to the temperature. It rejects missing/too-low/forged temperature citations.

Even a documentary PASS here **does not establish** the recipe's real cooking success, palatability, precise preparation time, yield, finished dish nutrition, a specific store's allergen-trace label, weekly M3 nutritional adequacy or a signed public APK. The 200 generator prototypes remain excluded. Insufficiently matched components are not accepted, and after three unsuccessful correction cycles a candidate stays PENDING/BLOCKED while the next recipe proceeds.

## Gauntlet evidence

The frozen test `M2ComposedMethodReviewTest.kt` pins both author recipes, independent raw-food totals, method excerpt SHA and includes bad/missing/double/stale/source/temperature cases. **Exact Android CI run and tested SHA: PENDING** until actual JVM and emulator jobs pass. Release milestones M3–M6 remain open.


## FINAL exact executable SHA Gauntlet · 2026-10-26

**PASS** [FoodPlan Android CI #40317023735](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40317023735) with exact compiled/Android-tested code and regression test SHA **`dfba2c5bea83f66d884b0f7341c0ffbb68883857`**: `static-and-unit` **SUCCESS** (JVM + debug APK + 150-authored documentary export), `emulator-smoke` **SUCCESS** (**77/77** instrumented Android tests, no failures/skips; **8/8** emulator OS force-stop + cold restart PASS). **87+2 = 89/150 documentary-ready PASS at this exact tested SHA; 61 pending.** This is a validated checkpoint, not a claim about any later branch commits. The 200 generated templates remain excluded.

Intermediate [CI #40316377908](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40316377908) was **FAIL** due to historical JVM test fixtures still comparing against global 87/63 while the new registry correctly returned 89/61. Targeted fixes only aligned global counts, and the later exact-source CI #40317023735 passed. Source comparisons and food-safety constraints were not relaxed. External physical handset QA, real SKU allergens, cooked food mass/nutritional retention, full M3/M4 and distribution signed APK still open.
