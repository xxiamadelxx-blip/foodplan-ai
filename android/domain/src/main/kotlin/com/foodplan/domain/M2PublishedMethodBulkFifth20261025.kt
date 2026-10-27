package com.foodplan.domain

/**
 * M2 YAR-6, next 25 distinct authored cooking-method candidates.
 * Exactly 9 externally compared publications admitted, 16 rejected.
 * These excerpts are own-language documentary summaries, NOT fetched HTML hashes.
 * Input K/P/F/C only; cooked nutrient/yield and actual SKU allergen clearance unknown.
 */
object M2PublishedMethodBulkFifth20261025 {
    val reviews: Map<String, RecipeDocumentaryReview> = listOf(
        RecipeDocumentaryReview(
            recipeId = "b4_rice_tomato_breakfast_cakes",
            recipeFingerprintSha256 = "861d3d4815de838e618d01bd99afaac170ed6f72277d19922472b511e31a0b3a",
            publishedMethodSource = "https://cnz.to/recipes/vegetables-grains/rice-cakes-recipe/",
            methodSnapshotSha256 = "1c543f2e3957f69fae76145906713a6b0c98974f69459637990687e22a350a2b",
            methodSnapshotText = "Captured from independently published cooking method on 2026-10-25. Published Clotilde Dusoulier Rice Cakes mixes previously cooked rice, egg and diced de-seeded fresh tomato, chills mixture, shapes small flat patties and skillet-fries on both sides. FoodPlan cooks 65g DRY rice before cooling, adds Gouda cheese, omits onion, garlic and herb garnishes, and uses sunflower oil.",
            methodApplicabilityNotes = "Core cooked rice with egg, drained chopped tomato, small pan-fried patties matches. Published uses cooked leftover rice and chills mixture 15 min; FoodPlan boils dry rice and includes cheese. Source nutrition not transferred.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK25_FIFTH_20261025.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent raw-edible ingredient arithmetic, each rice=65g, egg=115g, tomato=110g, cheese=35g, oil=7g. For each kcal, protein, fat and carbs: sum exactly weight/100 times current OfflineRecipes pinned per-100g ingredient standard; grain/pasta DRY as authored, chicken/beef/pork RAW, egg edible part and oil as RAW input. No measured oil uptake, finished mass, thermal retention or SKU-label evidence.",
            independentlyRecalculatedRawNutrition =
                Nutrition(603.88825, 28.5322525, 28.94945, 58.3635475)
        ),
        RecipeDocumentaryReview(
            recipeId = "b4_millet_pear_pudding",
            recipeFingerprintSha256 = "c09bd22f5b5446f80dbb539145ff81133bed4e7865b9d96d91497dedabd6958c",
            publishedMethodSource = "https://povar.ru/recipes/puding_iz_pshennoi_kashi-25886.html",
            methodSnapshotSha256 = "fd7455f16e78d7254b8b233a0082a170a7167f90d23ac411b5498176ca2a6906",
            methodSnapshotText = "Captured from independently published cooking method on 2026-10-25. Published millet pudding simmers dry millet in milk until soft, combines cooked porridge with egg and grated apple, puts mixture in greased dish, then bakes until set. FoodPlan uses chopped pear instead of apple, whole egg rather than separately whipped whites, water with milk, and serves natural yogurt on the side.",
            methodApplicabilityNotes = "Same milk millet porridge plus fresh fruit and egg then oven-set pudding method. Publisher additionally uses sugar and butter, apple and separated whites; FoodPlan omits sugar/butter and uses pear/yogurt. Texture and timings differ.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK25_FIFTH_20261025.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent raw-edible ingredient arithmetic, each millet=65g, pear=145g, egg=60g, milk=155g, yogurt=50g. For each kcal, protein, fat and carbs: sum exactly weight/100 times current OfflineRecipes pinned per-100g ingredient standard; grain/pasta DRY as authored, chicken/beef/pork RAW, egg edible part and oil as RAW input. No measured oil uptake, finished mass, thermal retention or SKU-label evidence.",
            independentlyRecalculatedRawNutrition =
                Nutrition(548.858885, 21.673625, 15.950885, 79.08023)
        ),
        RecipeDocumentaryReview(
            recipeId = "b4_rolled_cottage_omelette",
            recipeFingerprintSha256 = "bdd139bc947f2c5a8b30959a2219db7fd65cefddecc67ba33871bcb49222e1b2",
            publishedMethodSource = "https://www.iamcook.ru/showrecipe/3073",
            methodSnapshotSha256 = "263dc30ef60e4c1d50ad419850b6fd5c5fdb054a70ca7d215cc590634947a03c",
            methodSnapshotText = "Captured from independently published cooking method on 2026-10-25. Published omelet roll pours thin beaten egg on oiled skillet, covers until set, adds cheese and fresh tomato then rolls the pliable egg sheet. FoodPlan also prepares a covered thin egg sheet but whisks with milk and fills it afterward with mashed cottage cheese and fresh tomato.",
            methodApplicabilityNotes = "Match is thin covered pan omelet, dairy and tomato filling, rolled warm. Main source uses grated hard/semihard cheese in egg before setting, and water, while FoodPlan uses cottage cheese after cooking and milk. Moisture profile not validated.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK25_FIFTH_20261025.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent raw-edible ingredient arithmetic, each egg=130g, cottage_cheese=105g, tomato=90g, milk=35g, oil=7g. For each kcal, protein, fat and carbs: sum exactly weight/100 times current OfflineRecipes pinned per-100g ingredient standard; grain/pasta DRY as authored, chicken/beef/pork RAW, egg edible part and oil as RAW input. No measured oil uptake, finished mass, thermal retention or SKU-label evidence.",
            independentlyRecalculatedRawNutrition =
                Nutrition(412.38, 34.8145, 26.5225, 9.572)
        ),
        RecipeDocumentaryReview(
            recipeId = "b4_oat_crepes_pear",
            recipeFingerprintSha256 = "07c395675282447e2eabf40e1c61055ceacdd675d4861d095abae65cf6cdcb84",
            publishedMethodSource = "https://foodanmood.com/oatmeal-crepes/",
            methodSnapshotSha256 = "5de0d00f8e72acf91d63e26cf79840942aeddca9b9f3b72040e1133c2a264986",
            methodSnapshotText = "Captured from independently published cooking method on 2026-10-25. Published rolled-oat crepes puree dry rolled oats, milk and egg into pourable batter, rest briefly to hydrate, then fry thin pancakes in lightly oiled skillet on both sides and serve with optional fruit or yogurt. FoodPlan warms sliced pear, wraps it in the pancakes and adds yogurt.",
            methodApplicabilityNotes = "Original method verifies blended oats, milk, egg, rested crepe batter and shallow skillet frying. FoodPlan omits sweet maple/vanilla and uses cooked pear filling and natural yogurt. Dry oat input basis retained.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK25_FIFTH_20261025.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent raw-edible ingredient arithmetic, each oats=65g, egg=60g, milk=140g, pear=145g, yogurt=65g, oil=5g. For each kcal, protein, fat and carbs: sum exactly weight/100 times current OfflineRecipes pinned per-100g ingredient standard; grain/pasta DRY as authored, chicken/beef/pork RAW, egg edible part and oil as RAW input. No measured oil uptake, finished mass, thermal retention or SKU-label evidence.",
            independentlyRecalculatedRawNutrition =
                Nutrition(593.708885, 23.106125, 22.442885, 75.71473)
        ),
        RecipeDocumentaryReview(
            recipeId = "b4_chicken_cabbage_noodle_soup",
            recipeFingerprintSha256 = "47267d550de0e94a0013ce5b3093bcdfeea72d637bf07811d9543a66ef533679",
            publishedMethodSource = "https://gipfel.ru/blog/recipe/domashniy-uyut-sup-lapsha-s-kuritsey-na-lyuboy-sluchay",
            methodSnapshotSha256 = "008fc5d393cfa497a4b552021b3374860d9621b5ea6019cbd93282f75e2a0270",
            methodSnapshotText = "Captured from independently published cooking method on 2026-10-25. Published Gipfel soup simmers cut chicken in water, adds carrot and onion, cooks vegetable broth, then adds dry noodles only toward the end. FoodPlan adds thinly sliced fresh cabbage and uses DRY plain pasta in place of noodles, without sautéing vegetables in oil.",
            methodApplicabilityNotes = "Chicken water-broth then carrot/onion stage then dry pasta last is aligned. Publisher first sautés vegetable mixture in oil, which FoodPlan omits; extra cabbage is softened directly in broth. Full chicken doneness still necessary.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK25_FIFTH_20261025.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent raw-edible ingredient arithmetic, each chicken=165g, pasta=65g, cabbage=160g, carrot=90g, onion=55g. For each kcal, protein, fat and carbs: sum exactly weight/100 times current OfflineRecipes pinned per-100g ingredient standard; grain/pasta DRY as authored, chicken/beef/pork RAW, egg edible part and oil as RAW input. No measured oil uptake, finished mass, thermal retention or SKU-label evidence.",
            independentlyRecalculatedRawNutrition =
                Nutrition(525.2901, 48.62225, 4.8076, 72.5085)
        ),
        RecipeDocumentaryReview(
            recipeId = "b4_chicken_broccoli_oat_balls",
            recipeFingerprintSha256 = "db12b33eb9712ad7c8d84e3602e2e86efefb9cd704c515541d059380987f18b5",
            publishedMethodSource = "https://www.saltycanary.com/gluten-free-eggless-chicken-meatballs-recipe/",
            methodSnapshotSha256 = "bc10e5ab9f5540138e683bd96c18cad16b6cfe3b482528388111e05dbe308c3f",
            methodSnapshotText = "Captured from independently published cooking method on 2026-10-25. Published Salty Canary meatballs mixes raw chicken, finely chopped broccoli and cooked oatmeal, shapes balls, bakes on lined sheet until fully cooked. Separately, Small Dishes documents baking chicken meatballs beside separated broccoli on the same pan. FoodPlan combines raw chicken and dry oats plus egg, and bakes balls surrounded by broccoli florets.",
            methodApplicabilityNotes = "Original source has COOKED oats and minced broccoli inside balls; FoodPlan has DRY rolled oats, egg and separate florets, not an identical formulation. Separate tray arrangement published at https://smalldishes.com/sheet-pan-chicken-meatballs-and-broccoli/ . Finished internal chicken temperature required, no source macro copied.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK25_FIFTH_20261025.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent raw-edible ingredient arithmetic, each chicken=185g, oats=35g, egg=50g, broccoli=190g, oil=7g. For each kcal, protein, fat and carbs: sum exactly weight/100 times current OfflineRecipes pinned per-100g ingredient standard; grain/pasta DRY as authored, chicken/beef/pork RAW, egg edible part and oil as RAW input. No measured oil uptake, finished mass, thermal retention or SKU-label evidence.",
            independentlyRecalculatedRawNutrition =
                Nutrition(522.9929, 57.35675, 18.4859, 36.126)
        ),
        RecipeDocumentaryReview(
            recipeId = "b4_turkey_mushroom_roulade",
            recipeFingerprintSha256 = "65c1b341d735e191f9873252c14fbac9a931a3afd5c1d3458b05fe21240e1eff",
            publishedMethodSource = "https://bluejeanchef.com/wprm_print/turkey-roulade-with-leeks-mushrooms-and-marsala",
            methodSnapshotSha256 = "cdaf1154fe4c412c80a61b9795c9019435231c0637ed357dd9622d37744a1de8",
            methodSnapshotText = "Captured from independently published cooking method on 2026-10-25. Published Meredith Laurence roulade sautés chopped mushrooms until liquid evaporates, flattens raw turkey breast, spreads cooked mushroom stuffing, rolls and secures poultry and cooks it fully; the source supports oven roasting instead of its rotisserie-air-fryer option. FoodPlan adds cheese and potato wedges.",
            methodApplicabilityNotes = "Turkey flattened, cooked mushroom filling, wrapped and roasted fully matches core method. Published leeks, celery, herbs and Marsala omitted; FoodPlan uses Gouda cheese and oven-roasts potato wedges separately. Internal safe poultry cooking not inferred from time alone.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK25_FIFTH_20261025.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent raw-edible ingredient arithmetic, each turkey=195g, mushroom=125g, cheese=45g, potato=230g, oil=8g. For each kcal, protein, fat and carbs: sum exactly weight/100 times current OfflineRecipes pinned per-100g ingredient standard; grain/pasta DRY as authored, chicken/beef/pork RAW, egg edible part and oil as RAW input. No measured oil uptake, finished mass, thermal retention or SKU-label evidence.",
            independentlyRecalculatedRawNutrition =
                Nutrition(657.82, 65.9375, 23.866, 45.574)
        ),
        RecipeDocumentaryReview(
            recipeId = "b4_beef_zucchini_meatballs",
            recipeFingerprintSha256 = "481e583a7ec58a8ba103ae5720a2837dd469f7b158603757e8b77ee2923159f2",
            publishedMethodSource = "https://littlesunnykitchen.com/beef-meatballs-tomato-sauce-rice",
            methodSnapshotSha256 = "7c5b23533776ef973a9dc34d267d61e38453919deaafcee895012722c3b945db",
            methodSnapshotText = "Captured from independently published cooking method on 2026-10-25. Published Little Sunny Kitchen drops raw egg-bound beef meatballs directly into simmering tomato sauce, covers until the meatballs are cooked through, and serves them with starch. Independently Sula and Spice publishes grated-squeezed zucchini incorporated into raw beef-egg meatballs. FoodPlan combines both techniques and serves boiled dry pasta.",
            methodApplicabilityNotes = "Directly poaching raw beef egg-bound balls in tomato sauce sourced here; zucchini-beef binder independently sourced at https://sulaandspice.com/beef-and-zucchini-meatballs . FoodPlan omits breadcrumbs and Parmesan, uses finely chopped lean beef, grated drained zucchini, fresh tomato and DRY pasta instead of rice.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK25_FIFTH_20261025.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent raw-edible ingredient arithmetic, each beef=185g, egg=55g, zucchini=145g, tomato=205g, pasta=75g, oil=7g. For each kcal, protein, fat and carbs: sum exactly weight/100 times current OfflineRecipes pinned per-100g ingredient standard; grain/pasta DRY as authored, chicken/beef/pork RAW, egg edible part and oil as RAW input. No measured oil uptake, finished mass, thermal retention or SKU-label evidence.",
            independentlyRecalculatedRawNutrition =
                Nutrition(755.98, 60.3405, 26.121, 69.0145)
        ),
        RecipeDocumentaryReview(
            recipeId = "b4_pork_oat_schnitzel",
            recipeFingerprintSha256 = "e80101df85f9731c35003467ce8fddc5b2cac87e3221ad9266db58abf45bd5a9",
            publishedMethodSource = "https://www.pork.co.nz/recipes/nz-pork-schnitzel-with-apple-fennel-slaw",
            methodSnapshotSha256 = "76118851d1023348f1ecc81f73bfd708fd0b1c0d24e65b3bf827f53fd9ef1fed",
            methodSnapshotText = "Captured from independently published cooking method on 2026-10-25. Published NZ Pork schnitzel pounds pork into a thin cutlet, dips into beaten egg and fine crumbs, then pan-fries in oil until cooked. Independently Oats Everyday publishes making fine savory cutlet coating by blending dry rolled oats and using beaten egg to adhere crumbs. FoodPlan replaces panko with blended oats and serves sliced cucumber-yogurt salad.",
            methodApplicabilityNotes = "Main source thin pork-egg-crumb pan method; oat-crumb substitution supported by https://oatseveryday.com/recipes/wprm-oat-crusted-potato-cutlets/ where substrate is potato, not pork. No claim identical crust adhesion. FoodPlan omits flour, panko, Parmesan, apple-fennel slaw and uses raw pork FDC and yoghurt cucumber side.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK25_FIFTH_20261025.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent raw-edible ingredient arithmetic, each pork=190g, oats=55g, egg=55g, cucumber=150g, yogurt=80g, oil=12g. For each kcal, protein, fat and carbs: sum exactly weight/100 times current OfflineRecipes pinned per-100g ingredient standard; grain/pasta DRY as authored, chicken/beef/pork RAW, egg edible part and oil as RAW input. No measured oil uptake, finished mass, thermal retention or SKU-label evidence.",
            independentlyRecalculatedRawNutrition =
                Nutrition(842.04125, 55.272, 47.83225, 45.91975)
        ),
    ).associateBy { it.recipeId }
}
