# YAR-6 fourth batch · 25 authored source candidates (2026-10-25)

**Previous full Gauntlet:** [#40143752638](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40143752638), 66/150 reviewed PASS, 84 still pending.

This is an independent **25-recipe research worklist**, not 25 source reviews. The 25 authored IDs are unaccepted in the current production registry. External discovery covered three leads: two partial-method sources and one unsuitable source; two originally selected dishes were already accepted by the latest repository milestone and were replaced with genuinely pending IDs. The remaining 22 are explicitly UNSEARCHED (no evidence fabricated). **No new recipes are promoted by this file.** It is deliberately fail-closed and the accepted count stays 66/150 until separately pinned recipe SHA, actual method excerpt, notes, raw edible K/P/F/C and JVM+Android PASS.

| Priority | Authored ID | Status | Source lead | Evidence blocker |
|---:|---|---|---|---|
| 1 | `b5_buckwheat_banana_casserole` | `UNSEARCHED` | not yet searched | Banana buckwheat oven bake method not independently compared |
| 2 | `b5_baked_tomato_eggs_cottage` | `UNSEARCHED` | not yet searched | Baked tomato stuffed with egg and cottage cheese needs source |
| 3 | `b5_savory_millet_mushrooms` | `PARTIAL_METHOD_ONLY` | [publication](https://samobranka.info/recipe/pshennaya-kasha-v-gorshochke-s-gribami) | Published method boils millet and sautés mushrooms/onions with cheese, but adds raw egg and finishes in oven, whereas FoodPlan mixes on stovetop. Cooking-method difference prevents immediate admission. |
| 4 | `b4_chicken_cabbage_noodle_soup` | `PARTIAL_METHOD_ONLY` | [publication](https://www.maggi.ru/recipes/kurinyy-sup-s-domashney-lapshoy) | Published chicken broth and noodle soup includes carrot/onion, but does not establish the FoodPlan cabbage addition and uses homemade egg-flour noodles. Need closer source or explicit safe adaptation. |
| 5 | `b4_savory_carrot_oat_bake` | `SOURCE_NOT_APPLICABLE` | [publication](https://www.iamcook.ru/section/13262) | Retrieved zucchini and cheese casseroles, not oats-carrot-milk-egg-cheese oven method. Must find direct source. |
| 6 | `b4_buckwheat_apple_cups` | `UNSEARCHED` | not yet searched | Unverified combined cooked buckwheat, apple, egg and flour in small oven molds |
| 7 | `b4_banana_cottage_oven_pancakes` | `UNSEARCHED` | not yet searched | No exact oven baked cottage-cheese banana oatmeal fritter method independently inspected |
| 8 | `b4_rice_tomato_breakfast_cakes` | `UNSEARCHED` | not yet searched | No independent source for rice and tomato breakfast patties |
| 9 | `b4_millet_pear_pudding` | `UNSEARCHED` | not yet searched | No independently verified dry-millet-to-pear pudding technique |
| 10 | `b4_egg_cucumber_buckwheat_breakfast` | `UNSEARCHED` | not yet searched | Warm cooked buckwheat and raw cucumber/egg serving sequence needs publication |
| 11 | `b4_rolled_cottage_omelette` | `UNSEARCHED` | not yet searched | Egg roll with cottage cheese filling needs separate published method |
| 12 | `b4_oat_crepes_pear` | `UNSEARCHED` | not yet searched | Oat crepes and pear serving method not externally compared |
| 13 | `b4_chicken_broccoli_oat_balls` | `UNSEARCHED` | not yet searched | Raw poultry oat broccoli balls require actual cooking-safety source |
| 14 | `b4_turkey_mushroom_roulade` | `UNSEARCHED` | not yet searched | Raw turkey roulade with mushroom filling needs actual safe oven source |
| 15 | `b4_turkey_millet_cabbage_soup` | `UNSEARCHED` | not yet searched | Turkey millet cabbage soup method not externally confirmed |
| 16 | `b4_beef_zucchini_meatballs` | `UNSEARCHED` | not yet searched | Beef-zucchini meatball cooking sequence and tomato simmer need source |
| 17 | `b4_beef_broccoli_patties` | `UNSEARCHED` | not yet searched | Beef broccoli patty and mashed potato two-part recipe not externally confirmed |
| 18 | `b4_pork_oat_schnitzel` | `UNSEARCHED` | not yet searched | Raw pork with oat crumb coating needs verified full cooking method |
| 19 | `b4_pork_potato_roast_lemon` | `UNSEARCHED` | not yet searched | Whole peeled lemon nutrient basis vs extracted juice risk; must normalize |
| 20 | `b4_cod_stuffed_tomatoes` | `UNSEARCHED` | not yet searched | Fish stuffed tomato safe cooking method not yet sourced |
| 21 | `b4_cod_broccoli_yogurt_bake` | `UNSEARCHED` | not yet searched | Yogurt fish oven bake needs verified temperature and order |
| 22 | `b4_salmon_millet_fish_soup` | `UNSEARCHED` | not yet searched | Salmon and millet soup method not externally confirmed |
| 23 | `b4_salmon_zucchini_oven_fritters` | `UNSEARCHED` | not yet searched | Salmon-zucchini patties need safe cooking source |
| 24 | `b4_lentil_zucchini_cakes` | `UNSEARCHED` | not yet searched | Dry-vs-cooked lentil basis and pan patties need published method |
| 25 | `b4_lentil_pepper_millet_stuff` | `UNSEARCHED` | not yet searched | Lentil millet stuffed pepper method not yet confirmed |

## Next actionable acceptance gate

Start with the two genuinely pending replacement IDs `b5_buckwheat_banana_casserole` and `b5_baked_tomato_eggs_cottage`; published-method leads for previously accepted dumplings and potato nests were excluded. Do not re-review previously accepted evidence.

## Test requirements

`M2FourthBatchSourceTriageTest` verifies exactly 25 distinct authored IDs, 25 pending, no source-lead promotion, priority order, explicit blocker for each, and immutable history when a future separately validated review is added. M2 remains OPEN. This worklist is not a replacement for published-method audit or finished-dish validation.
