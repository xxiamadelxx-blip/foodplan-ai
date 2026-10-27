# M2 YAR-6 · 4 further published home-cooking methods, 24 October 2026

## Independent public source → application correspondence

These are **published method comparisons**, not proof of restaurant food quality, physical cooked mass, allergen-safe product SKU, individual nutrient adequacy or exact pricing. Each method excerpt is **paraphrased in our own words** and SHA-256 bound to the exact local UTF-8 string in `M2PublishedMethodReviewBatch20261024.kt`. The local digest does not authenticate remote HTML. Original public pages were read on 2026-10-24 before recording their content. Local reconstructed raw-input totals are independently calculated using decimal arithmetic: `sum(grams_i × pinned USDA quantity per 100 g / 100)`.

| Authored ID / FoodPlan dish | Independent published cooking method | Explicit adaptation and open questions |
| --- | --- | --- |
| `cur_scrambled_mushrooms` · Яйца с шампиньонами | [Food.com, Scrambled Eggs with Spiced Mushrooms](https://www.food.com/recipe/scrambled-eggs-with-spiced-mushrooms-63704) | Same saute-mushrooms-first then add beaten eggs over reduced heat, stirring until set. Publication includes onion, spices and parsley absent from FoodPlan. Exact ingredient masses and 16-min duration are FoodPlan's estimates, NOT validated by source. |
| `cur_shakshuka` · Шакшука со сладким перцем | [The Mediterranean Dish, Shakshuka](https://www.themediterraneandish.com/shakshuka-recipe/) | Same onion and bell-pepper softening, tomato reduction and covered eggs-in-wells cooking. Published version contains garlic, fresh herbs, tomato sauce, seasonings and olive oil; FoodPlan excludes additions, uses sunflower oil and different quantities. |
| `cur_lentil_tomato_soup` · Чечевичный суп с томатами | [CSPI, Tomato Lentil Soup by Kate Sherwood](https://www.cspi.org/recipe/tomato-lentil-soup) | Both boil rinsed dry lentils, tomato, onion and carrots with water until soft, with oil-sauteed onions. Source puts carrot into boiling soup, FoodPlan sautees carrot briefly with onion; publication includes celery and tomato paste. Recipe timings 35 vs 42 min. Source's 160 kcal per cooked cup **not** substituted into our dry/raw estimate. |
| `cur_buckwheat_mushroom` · Гречка с грибами и луком | [GI Nutrition Foundation, Buckwheat with Mushrooms](https://www.ginutritionfoundation.org/recipes/buckwheat-with-mushrooms) | Same water-boiled buckwheat, separate onion/mushroom oil saute and warm combining. FoodPlan uses sunflower vs olive oil, less buckwheat, different seasoning and time (30 vs 55 min). Publication is a different portion scale and does not certify ours. |

## Independent raw edible input arithmetic per FoodPlan portion

Input food nutrient values are the exact current, pinned `OfflineRecipes.ingredients` references, **not values claimed by the cited published dishes**. Dry ingredients remain dry: lentils FDC 172420 and buckwheat FDC 170685. Egg amount is edible weight excluding shell (FDC 748967); tomato FDC 170457; mushrooms FDC 169251; oil FDC 171025; onion FDC 170000; pepper FDC 170108; carrot FDC 170393. All oil grams are treated as ingested in the provisional model, which may overestimate actual uptake. Cooking retention, cooked yield, real SKU allergen trace, portion verification and microbiological safety **UNKNOWN**.

| ID | Raw edible recipe grams | Source-linked total kcal | Protein g | Fat g | Carbs g |
| --- | --- | ---: | ---: | ---: | ---: |
| `cur_scrambled_mushrooms` | egg 120, raw mushroom 120, oil 6 | **251.04** | **18.588** | **18.360** | **5.064** |
| `cur_shakshuka` | edible egg 120, raw tomato 230, raw pepper 110, raw onion 55, oil 8 | **334.32** | **18.598** | **20.797** | **21.869** |
| `cur_lentil_tomato_soup` | **dry** lentils 85, raw tomato 180, raw carrot 90, raw onion 60, oil 7 | **454.38** | **24.0165** | **8.537** | **75.0755** |
| `cur_buckwheat_mushroom` | **dry toasted** buckwheat 90, raw mushroom 220, raw onion 75, oil 11 | **487.04** | **18.180** | **14.262** | **81.632** |

### Admission rules

All four records require the exact frozen `RecipeEvidenceAudit.fingerprint(recipe)`, local evidence-excerpt hash, source URL, method comparison, documented nutrient basis and independent 4-number calculations. Tampering with excerpts, ingredient amounts, preparation steps or nutrient numbers forces `documentedReady=false`. The external sources validate only methods and relevant adaptations; household scaling and 7-day nutrition are independently bounded tasks. Excluding spices from nutrient totals assumes ordinary unlisted flavorings; do not certify all additions, especially user allergies.

**Intake:** preceding `cur_omelette_tomato` 1/150, new four **4 candidate submissions**. Total **5/150 ONLY IF** Kotlin JVM and exact Android Gauntlet pass; otherwise new candidate batch remains pending and must not be reported as PASS.


## Gauntlet result · 24 October 2026 (batch 2)

**Engineering PASS:** [CI #40111932330](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40111932330), exact code SHA `e4818102f0b27a60d20a79a1e2141e09b6c2bd5e`: JVM and Android APK PASS; 77/77 emulator instrumentation PASS (0 skipped/failed), OS cold-restart 8/8 PASS. Documented count is **five of 150**: the previously checked tomato omelette plus the four published methods above. All other 145 curated dishes remain pending; 200 generated prototypes are excluded.

**Targeted fix:** prior #40111639788 encountered 2/295 JVM failures from inconsistent fixture-scoped versus production-catalog expected counts, **not** source-method rejection. Fixed with correct explicit scope in `RecipeDocumentaryGateTest`, no weakening of hash/recipe/nutrition gates. The fixed run #40111932330 is the accepted evidence.
