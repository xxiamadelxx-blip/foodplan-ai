package com.foodplan.domain

/**
 * YAR-6: 18 source-reviewed methods out of 25 independent new candidate dishes.
 * See docs/qa/M2_BULK25_FOURTH_20261025.md; own-language excerpts, NOT publisher HTML snapshots.
 * The SHA256 of an excerpt verifies captured text integrity only.
 * Each nutrient field is independently tabulated raw-edible food input, not
 * laboratory cooked food output, whole-week adequacy or SKU-label clearance.
 */
object M2PublishedMethodBulkFourth20261025 {
    val reviews: Map<String, RecipeDocumentaryReview> = listOf(
        RecipeDocumentaryReview(
            recipeId = "cur_zucchini_cheese_omelette",
            recipeFingerprintSha256 = "8a0864cb15f876ed0dbcc2571b99f6712d20d14a144d79ca4b5cf31606d8a48f",
            publishedMethodSource = "https://www.epicurious.com/recipes/food/views/herbed-cheese-and-zucchini-omelet-479",
            methodSnapshotSha256 = "7617294e4793556b1c81c0a504466fac55965039d7a6423f8451cebf6cec9a23",
            methodSnapshotText = "Source inspected on 2026-10-25: The published herbed cheese and zucchini omelet softens zucchini in a hot skillet, makes a beaten egg omelet, uses grated cheddar cheese, and finishes the folded omelet under a lid until set.",
            methodApplicabilityNotes = "FoodPlan softens grated zucchini, keeps it in the skillet and pours beaten whole eggs over it; its source removes zucchini, separately whips egg whites and folds the omelet. Butter and herb cheeses are replaced with sunflower oil and Gouda. Exact texture/time are not transferred.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK25_FOURTH_20261025.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently recomputed edible-raw food totals, kcal, protein, fat and carbohydrate from the fixed FoodData Central/producer ingredient reference per 100g in OfflineRecipes.ingredients: egg 125g; zucchini 160g; cheese 45g; oil 6g. Dry grains remain dry, meat raw, eggs exclude shell, oil entered at full recipe-input weight; no post-cooking yield or individual SKU allergen certification.",
            independentlyRecalculatedRawNutrition =
                Nutrition(419.19, 28.659, 31.31, 7.175)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_chicken_rice_soup",
            recipeFingerprintSha256 = "8fdb2e7680370d4b4508e7bb3ca37d7870fcb037efec5bc5c05ab1895affccd2",
            publishedMethodSource = "https://kamelena.com/en/recipe/Chicken-Soup-with-Rice",
            methodSnapshotSha256 = "334d4dcd348864ad7c37909e9e03a8934693aee06c8cbcbb91b862c8aa425298",
            methodSnapshotText = "Source inspected on 2026-10-25: Published chicken rice soup simmers chicken pieces in water first, then uses fresh chopped carrots, onion, potatoes and rinsed dry rice in the same broth until grain and vegetable soften.",
            methodApplicabilityNotes = "The source begins with bone-in chicken and removes the bones before serving, FoodPlan starts with raw boneless sliced chicken breast and leaves it in the pot. Published garnish is omitted. Same chicken-first, vegetables-then-dry-rice soup technique; no direct timing transfer.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK25_FOURTH_20261025.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently recomputed edible-raw food totals, kcal, protein, fat and carbohydrate from the fixed FoodData Central/producer ingredient reference per 100g in OfflineRecipes.ingredients: chicken 155g; rice 60g; carrot 95g; onion 60g; potato 150g. Dry grains remain dry, meat raw, eggs exclude shell, oil entered at full recipe-input weight; no post-cooking yield or individual SKU allergen certification.",
            independentlyRecalculatedRawNutrition =
                Nutrition(558.0257, 43.75556, 4.0405, 89.12789)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_broccoli_cheese_puree",
            recipeFingerprintSha256 = "1664e7b2ddcff2907a921d28b76da9d27dd5b49ef3cff51b3db2cba9e1210d06",
            publishedMethodSource = "https://www.arlafoods.co.uk/recipes/broccoli-and-potato-soup/",
            methodSnapshotSha256 = "d4b08660d164691c997b741c3d2ee5e08521df26c4f3763d8931bba94a00add8",
            methodSnapshotText = "Source inspected on 2026-10-25: Arla's broccoli and potato soup softens leek with fat, simmers potatoes in broth, adds broccoli late to cook through, blends the soup smooth and finally melts grated cheddar cheese into the hot puree.",
            methodApplicabilityNotes = "FoodPlan uses onion instead of leek, sunflower oil instead of butter, no cream/stock, Gouda instead of cheddar. It adds potatoes before broccoli, purees, and melts cheese at the end. Only this sequence is method-confirmed, not the exact consistency.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK25_FOURTH_20261025.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently recomputed edible-raw food totals, kcal, protein, fat and carbohydrate from the fixed FoodData Central/producer ingredient reference per 100g in OfflineRecipes.ingredients: broccoli 300g; potato 180g; cheese 55g; onion 65g; oil 7g. Dry grains remain dry, meat raw, eggs exclude shell, oil entered at full recipe-input weight; no post-cooking yield or individual SKU allergen certification.",
            independentlyRecalculatedRawNutrition =
                Nutrition(518.28, 25.832, 23.339, 57.644)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_beef_chickpea_braise",
            recipeFingerprintSha256 = "1693d8f01b5ee981b53370822f87e217dab99b98daeaba43d57f8f5baf76c1f4",
            publishedMethodSource = "https://akispetretzikis.com/en/recipe/8843/moscharaki-kokkinisto-me-revithia",
            methodSnapshotSha256 = "18bcf0c0cf9522897b24ae38be268ba52b9478b336f6ff80d1de9851b4c9094d",
            methodSnapshotText = "Source inspected on 2026-10-25: Akis Petretzikis published beef and chickpea tomato stew slow-cooks cubed beef with onion, carrot, tomato, water and chickpeas until the beef is soft and the sauce is reduced.",
            methodApplicabilityNotes = "Published chickpeas are frozen and stewed with beef, and the publication includes wine, spice, garlic, tomato juice. FoodPlan browns lean beef first, stews with raw tomatoes and carrots, and heats drained already-cooked canned chickpeas only after beef is tender. No duration/finished nutrient equivalence.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK25_FOURTH_20261025.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently recomputed edible-raw food totals, kcal, protein, fat and carbohydrate from the fixed FoodData Central/producer ingredient reference per 100g in OfflineRecipes.ingredients: beef 200g; chickpeas 170g; tomato 170g; onion 70g; carrot 90g; oil 10g. Dry grains remain dry, meat raw, eggs exclude shell, oil entered at full recipe-input weight; no post-cooking yield or individual SKU allergen certification.",
            independentlyRecalculatedRawNutrition =
                Nutrition(718.2, 58.528, 27.915, 60.074)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_vegetable_chickpea_pilaf",
            recipeFingerprintSha256 = "82da630f0a60d02ef647c2f068e87191f18461d8a1a1823549701b14e9ddce89",
            publishedMethodSource = "https://tollbit.bbcgoodfood.com/recipes/1959653/spiced-carrot-chickpea-and-almond-pilaf",
            methodSnapshotSha256 = "2cd1f0c1247cba74a01a149838099c026e144e8e33296d15bec1341281255a69",
            methodSnapshotText = "Source inspected on 2026-10-25: BBC Good Food's carrot chickpea pilaf softens onion and carrot in oil, adds rinsed dry rice and liquid to cook under a lid, stirs in drained canned chickpeas and rests pilaf covered.",
            methodApplicabilityNotes = "FoodPlan adds raw sweet pepper and pre-cooked canned chickpeas at the start rather than just before rice is finished. Harissa, nuts and yogurt garnish are omitted, sunflower replaces olive oil. Water/rice ratios remain dependent on actual dry grain.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK25_FOURTH_20261025.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently recomputed edible-raw food totals, kcal, protein, fat and carbohydrate from the fixed FoodData Central/producer ingredient reference per 100g in OfflineRecipes.ingredients: rice 90g; chickpeas 180g; carrot 140g; onion 75g; bell_pepper 110g; oil 13g. Dry grains remain dry, meat raw, eggs exclude shell, oil entered at full recipe-input weight; no post-cooking yield or individual SKU allergen certification.",
            independentlyRecalculatedRawNutrition =
                Nutrition(803.9545, 22.240965, 19.6567, 139.885835)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_tofu_tomato_pasta",
            recipeFingerprintSha256 = "ed28b42c1d5c440c3ae3c12c68f638026142323116c6475ed82ba83bfe3b56f2",
            publishedMethodSource = "https://khinskitchen.com/tofu-pasta/",
            methodSnapshotSha256 = "297b93b77fc9477d1565452667ba074dd5d6fcbaa218cb9c6da00c45abd09d7c",
            methodSnapshotText = "Source inspected on 2026-10-25: Khin's Kitchen tofu pasta with tomato sauce drains firm tofu and slices cubes, pan-fries tofu golden, separately boils pasta according to its pack, simmers garlic and tomato sauce and finally mixes sauce with pasta and tofu.",
            methodApplicabilityNotes = "Source takes fried tofu out temporarily and returns at plating; FoodPlan leaves cubes in the pan, then adds raw onion, garlic and fresh chopped tomato rather than passata. Cheese, stock and herbs are omitted. FoodPlan's separate pasta boil and tofu-fry technique corresponds, with fresh-sauce substitution.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK25_FOURTH_20261025.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently recomputed edible-raw food totals, kcal, protein, fat and carbohydrate from the fixed FoodData Central/producer ingredient reference per 100g in OfflineRecipes.ingredients: tofu 190g; pasta 85g; tomato 210g; garlic 7g; onion 65g; oil 10g. Dry grains remain dry, meat raw, eggs exclude shell, oil entered at full recipe-input weight; no post-cooking yield or individual SKU allergen certification.",
            independentlyRecalculatedRawNutrition =
                Nutrition(625.76, 31.2864, 19.7181, 85.0985)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_pork_pepper_wok",
            recipeFingerprintSha256 = "6c6fbb7b40541796f8ec259c6cc56508da2394e3d5c6f9ecec536d84d2e49d86",
            publishedMethodSource = "https://www.foodreference.com/html/sw-pp-sf.html",
            methodSnapshotSha256 = "4df655be7bf8a4cdb3ebcd43c897001ff99ab721d97af16ad788c4375e4c1462",
            methodSnapshotText = "Source inspected on 2026-10-25: The National Pork Board Southwestern pork and pepper stir-fry slices boneless pork loin into strips, cooks it quickly in oil at high heat, then adds sliced sweet bell peppers and onion. The source suggests serving with cooked rice.",
            methodApplicabilityNotes = "FoodPlan boils its measured dry rice separately and cooks pork to completion before adding pepper and onion. Chili powder and cumin are omitted, quantities and fat composition differ, sunflower oil used. Published rice is optional serving suggestion, not an identical grain portion.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK25_FOURTH_20261025.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently recomputed edible-raw food totals, kcal, protein, fat and carbohydrate from the fixed FoodData Central/producer ingredient reference per 100g in OfflineRecipes.ingredients: pork 170g; rice 85g; bell_pepper 170g; onion 65g; oil 10g. Dry grains remain dry, meat raw, eggs exclude shell, oil entered at full recipe-input weight; no post-cooking yield or individual SKU allergen certification.",
            independentlyRecalculatedRawNutrition =
                Nutrition(800.09925, 41.9390225, 32.83905, 84.5881775)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_oat_apple_pear_porridge",
            recipeFingerprintSha256 = "37178e14629de59ac02adf49a5fff49579872f87ccc34c0008a70d5bc60d6a95",
            publishedMethodSource = "https://www.jamieoliver.com/recipes/fruit/porridge-lots-of-ways/",
            methodSnapshotSha256 = "8b6bba491468f6b7b898015ad7bd367ee1271d2be7ba158c3a66ed17f86186a7",
            methodSnapshotText = "Source inspected on 2026-10-25: Jamie Oliver porridge guidance simmers dry rolled oats with milk or water while stirring until creamy and offers apple and other fresh fruit variations for topping.",
            methodApplicabilityNotes = "FoodPlan simmers oats with full-fat milk and water, puts diced apple and fresh pear into the porridge after thickening and warms fruit, whereas the source suggests grated apple topping. Pear is an explicit food substitution and publisher's brief boil is not used to claim all oats soften equally.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK25_FOURTH_20261025.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently recomputed edible-raw food totals, kcal, protein, fat and carbohydrate from the fixed FoodData Central/producer ingredient reference per 100g in OfflineRecipes.ingredients: oats 65g; apple 105g; pear 100g; milk 160g. Dry grains remain dry, meat raw, eggs exclude shell, oil entered at full recipe-input weight; no post-cooking yield or individual SKU allergen certification.",
            independentlyRecalculatedRawNutrition =
                Nutrition(462.2113, 14.173, 10.0198, 80.9209)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_lentil_carrot_cream_soup",
            recipeFingerprintSha256 = "13919913f35a2a9e4257342194a5076c2527269b89171cf695d10ea7c8681a1a",
            publishedMethodSource = "https://www.canada.ca/en/health-canada/services/food-guide/eating-support/kitchen/recipes/carrot-ginger-lentil-soup.html",
            methodSnapshotSha256 = "e206aa9899a27df584cec11254a360af86050b06b21551e9bbbafd6ee4d1884f",
            methodSnapshotText = "Source inspected on 2026-10-25: Health Canada's published carrot and dry-lentil soup gently sautés onion and carrot in oil, adds washed uncooked lentils and water/stock, simmers to softness and blends into a smooth soup, and suggests a plain yogurt dollop.",
            methodApplicabilityNotes = "FoodPlan omits ginger, garlic, cumin, pepper and stock ingredients, uses water and sunflower oil, and mixes plain yogurt only on the served dish. Raw dry lentils and carrot weights remain independent of source portions; the cooked yield is not established.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK25_FOURTH_20261025.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently recomputed edible-raw food totals, kcal, protein, fat and carbohydrate from the fixed FoodData Central/producer ingredient reference per 100g in OfflineRecipes.ingredients: lentils 90g; carrot 190g; onion 75g; yogurt 70g; oil 7g. Dry grains remain dry, meat raw, eggs exclude shell, oil entered at full recipe-input weight; no post-cooking yield or individual SKU allergen certification.",
            independentlyRecalculatedRawNutrition =
                Nutrition(529.28, 27.188, 10.76, 85.484)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_buckwheat_mushroom_potato_soup",
            recipeFingerprintSha256 = "675546d1f3a9b51b27df208904914be921f16ebe500c855b796e1250f825e1ac",
            publishedMethodSource = "https://eng.obozrevatel.com/section-food/news-easy-buckwheat-soup-without-meat-a-perfect-dish-for-lunch-09-03-2024.html",
            methodSnapshotSha256 = "cfe48c9746e9aa572e0eebead0170d6ebff008e419c454a5b76ce7555b549d9b",
            methodSnapshotText = "Source inspected on 2026-10-25: The published OBOZ mushroom buckwheat soup uses potatoes, raw champignon mushrooms, dry buckwheat groats, carrot, onion and vegetable oil; mushrooms and onions are sautéed, while potatoes and buckwheat simmer in water until tender.",
            methodApplicabilityNotes = "FoodPlan sautés mushrooms and onion in the soup pot first then boils potatoes, carrots and uncooked buckwheat with them. Publication cooks potato/buckwheat before adding mushroom saute separately. The order differs, but the same oil-softened fungi and simmered grain/potato soup components remain applicable.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK25_FOURTH_20261025.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently recomputed edible-raw food totals, kcal, protein, fat and carbohydrate from the fixed FoodData Central/producer ingredient reference per 100g in OfflineRecipes.ingredients: buckwheat 65g; mushroom 150g; potato 165g; carrot 80g; onion 65g; oil 6g. Dry grains remain dry, meat raw, eggs exclude shell, oil entered at full recipe-input weight; no post-cooking yield or individual SKU allergen certification.",
            independentlyRecalculatedRawNutrition =
                Nutrition(496.79, 17.101, 8.677, 96.201)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_potato_egg_cucumber_salad",
            recipeFingerprintSha256 = "4369b651192d813daa58e3d65fa0daf0ee93aca0e3224dcb9a1d519e7627d523",
            publishedMethodSource = "https://www.zwilling.com/uk/z-cut-recipes/potato-cucumber-salad.html",
            methodSnapshotSha256 = "a8a7dd6c5de77480610e73a8719afe9ecaa7c40da2812258338a1804299c18e2",
            methodSnapshotText = "Source inspected on 2026-10-25: ZWILLING potato and cucumber salad boils potatoes, hard-boils eggs, cuts fresh cucumber and onion and combines vegetables and chopped egg with a natural-yogurt-based dressing.",
            methodApplicabilityNotes = "The source also uses mayonnaise, radish, pickles, mustard and two-hour chilling; FoodPlan excludes these additions and mixes plain yogurt before immediate serving. It is a simplified salad, not identical acidity or food-storage method. Source weights/cooked portion values not transferred.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK25_FOURTH_20261025.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently recomputed edible-raw food totals, kcal, protein, fat and carbohydrate from the fixed FoodData Central/producer ingredient reference per 100g in OfflineRecipes.ingredients: potato 260g; egg 115g; cucumber 165g; yogurt 80g; onion 35g. Dry grains remain dry, meat raw, eggs exclude shell, oil entered at full recipe-input weight; no post-cooking yield or individual SKU allergen certification.",
            independentlyRecalculatedRawNutrition =
                Nutrition(453.697375, 23.78225, 14.615875, 58.446625)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_zucchini_lentil_tomato_boats",
            recipeFingerprintSha256 = "2abad320b8dc30f7c1185d81a76bdb563ca799b913f00609a056f50cee67c215",
            publishedMethodSource = "https://theplantbasedschool.com/zucchini-boats/",
            methodSnapshotSha256 = "f50b632f19ca093648c5034a026d36e989e8a0a1ce577cd9b75736b379180f7f",
            methodSnapshotText = "Source inspected on 2026-10-25: The Plant Based School's lentil zucchini boats hollow fresh zucchini, prepare a cooked lentil tomato-onion mixture, fill cavities with lentils and removed zucchini flesh, top with grated cheese and oven-bake until zucchini becomes tender.",
            methodApplicabilityNotes = "FoodPlan boils dry lentils then combines them with finely chopped raw fresh tomatoes, onion and pulp rather than sauce-sautéing these vegetables, and omits source walnuts, vinegar and seasonings. Additional moisture and cooking time differ; filling-then-baking technique is documented.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK25_FOURTH_20261025.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently recomputed edible-raw food totals, kcal, protein, fat and carbohydrate from the fixed FoodData Central/producer ingredient reference per 100g in OfflineRecipes.ingredients: zucchini 340g; lentils 80g; tomato 165g; onion 70g; cheese 40g. Dry grains remain dry, meat raw, eggs exclude shell, oil entered at full recipe-input weight; no post-cooking yield or individual SKU allergen certification.",
            independentlyRecalculatedRawNutrition =
                Nutrition(539.5, 36.016, 13.312, 75.0985)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_broccoli_cheese_pasta_sauce",
            recipeFingerprintSha256 = "604ed9d1f699d6c118273320823e2decb909e1dfdefac1446dbfb42932cc2933",
            publishedMethodSource = "https://rebeccawilson.com/recipe/cheesy-broccoli-10-minute-pasta/",
            methodSnapshotSha256 = "50432fdcce8aa7d3c131c02fbf201135a46bf2469758fe0903adc11bcd9699e2",
            methodSnapshotText = "Source inspected on 2026-10-25: Rebecca Wilson's cheesy broccoli pasta boils dry pasta, softens broccoli with milk, explicitly allows stick-blending the cooked broccoli/milk to a smooth sauce, adds grated cheese to melt and coats cooked pasta with that sauce.",
            methodApplicabilityNotes = "FoodPlan boils fresh broccoli in water before pureeing with milk and raw garlic instead of grating broccoli and heating in milk, omits cornflour, stock and butter, uses Gouda. Explicit blend option supports the homogeneous broccoli cheese sauce; actual thickness remains untested.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK25_FOURTH_20261025.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently recomputed edible-raw food totals, kcal, protein, fat and carbohydrate from the fixed FoodData Central/producer ingredient reference per 100g in OfflineRecipes.ingredients: pasta 85g; broccoli 235g; cheese 50g; milk 115g; garlic 6g. Dry grains remain dry, meat raw, eggs exclude shell, oil entered at full recipe-input weight; no post-cooking yield or individual SKU allergen certification.",
            independentlyRecalculatedRawNutrition =
                Nutrition(647.28, 33.6132, 19.5858, 86.55)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_baked_potato_cottage_cucumber",
            recipeFingerprintSha256 = "e7a3d2111762a6d29edcb4b40f1ffb9249efedc005f82c2a90154300dc178a62",
            publishedMethodSource = "https://www.coop.co.uk/recipes/jacket-potatoes-with-herby-cottage-cheese",
            methodSnapshotSha256 = "f4eb4b400219962152d1989988cb3ec59a18c2feda7abb41f9b2ead5a5f065b5",
            methodSnapshotText = "Source inspected on 2026-10-25: Co-op's jacket potatoes with cottage cheese first prick whole potatoes, bake until soft, mix a cold curd topping with diced fresh cucumber and seasoning, slice open the baked potato and serve topped with creamy cheese.",
            methodApplicabilityNotes = "FoodPlan adds natural yogurt and raw garlic to curd, with sliced cucumber beside the filling, not inside it. Publication uses herbs, spring onion and lemon; FoodPlan omits them. The source's baked duration is longer than FoodPlan total, so softness is mandatory endpoint, not fixed source time.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK25_FOURTH_20261025.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently recomputed edible-raw food totals, kcal, protein, fat and carbohydrate from the fixed FoodData Central/producer ingredient reference per 100g in OfflineRecipes.ingredients: potato 320g; cottage_cheese 135g; cucumber 120g; garlic 5g; yogurt 55g. Dry grains remain dry, meat raw, eggs exclude shell, oil entered at full recipe-input weight; no post-cooking yield or individual SKU allergen certification.",
            independentlyRecalculatedRawNutrition =
                Nutrition(469.539, 31.1495, 9.0575, 67.534)
        ),
        RecipeDocumentaryReview(
            recipeId = "b5_potato_egg_nests_oven",
            recipeFingerprintSha256 = "8171c201a7441b4eb2a3d3642620cb6351b6164a39e57fc690d230f25e0fdc55",
            publishedMethodSource = "https://www.lovefromtheland.com/baked-eggs-nests",
            methodSnapshotSha256 = "5b88db069f2711fdc1506b5762fb656c1be7e8452d3ead2e3eb0c3e47148c63b",
            methodSnapshotText = "Source inspected on 2026-10-25: Love from the Land's baked egg nests squeeze liquid from grated potato, blend shredded potato and cheddar with oil, pack into nest shapes in a muffin tin, bake potatoes in advance then crack eggs into the nest centres and finish baking eggs.",
            methodApplicabilityNotes = "Source also includes grated yam, FoodPlan only raw potato. FoodPlan serves raw tomatoes separately and requires completely cooked eggs; shortest published baking time allows softer yolks. The two-stage grated potato bake, then add egg method is the evidence, not a fixed safe egg time.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK25_FOURTH_20261025.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently recomputed edible-raw food totals, kcal, protein, fat and carbohydrate from the fixed FoodData Central/producer ingredient reference per 100g in OfflineRecipes.ingredients: potato 240g; egg 115g; cheese 30g; tomato 105g; oil 7g. Dry grains remain dry, meat raw, eggs exclude shell, oil entered at full recipe-input weight; no post-cooking yield or individual SKU allergen certification.",
            independentlyRecalculatedRawNutrition =
                Nutrition(536.83, 27.586, 27.112, 47.8305)
        ),
        RecipeDocumentaryReview(
            recipeId = "b5_lazy_cottage_dumplings_pear",
            recipeFingerprintSha256 = "b8326b8885cc850b4ef20351cd235ea54eba866dc2d91b3e145ac8e24e29252c",
            publishedMethodSource = "https://www.polishyourkitchen.com/polish-lazy-pierogi-pierogi-leniwe",
            methodSnapshotSha256 = "879252e896920cb889cfbfeaf3a88048384f0fb6d9796f7ab2aeb5c0c1980d90",
            methodSnapshotText = "Source inspected on 2026-10-25: Polish Your Kitchen's lazy pierogi works farmer cheese with eggs and wheat flour into soft dough, rolls it into logs, cuts small pieces and boils gently until the dumplings float and finish setting.",
            methodApplicabilityNotes = "FoodPlan uses packaged 5%-fat Russian curd of potentially different moisture and serves raw pear cubes and yogurt rather than buttered breadcrumbs and sugar. Wheat flour quantity affects shape; source supports boil and cut procedure but cannot certify flour level with another curd.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK25_FOURTH_20261025.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently recomputed edible-raw food totals, kcal, protein, fat and carbohydrate from the fixed FoodData Central/producer ingredient reference per 100g in OfflineRecipes.ingredients: cottage_cheese 190g; flour 60g; egg 50g; pear 150g; yogurt 70g. Dry grains remain dry, meat raw, eggs exclude shell, oil entered at full recipe-input weight; no post-cooking yield or individual SKU allergen certification.",
            independentlyRecalculatedRawNutrition =
                Nutrition(656.79195, 46.69775, 18.33195, 76.3531)
        ),
        RecipeDocumentaryReview(
            recipeId = "b5_berry_banana_smoothie_bowl",
            recipeFingerprintSha256 = "480e63f0b024ba95509b9d953c8994e381796923d79422b698d320ba4293b522",
            publishedMethodSource = "https://dairyfarmersofcanada.ca/en/canadian-goodness/recipes/breakfast-muesli-smoothie-bowl",
            methodSnapshotSha256 = "5d41dcf72e98752b469ce88545ff7c53a629d29e4cb02e7f0bd4741acfd48365",
            methodSnapshotText = "Source inspected on 2026-10-25: Dairy Farmers of Canada's published smoothie bowl blends thawed berries, banana and plain yogurt until thick, serves in a cold bowl and scatters a reserved portion of rolled oats on top as a breakfast garnish.",
            methodApplicabilityNotes = "In the publication part of the oats is blended and some reserved for garnish; FoodPlan leaves all oats dry on top, adds some dairy milk, and omits honey and seeds. This is a no-cook smoothie with crunchy topping, not an overnight-oat method or cooked-oat nutrition claim.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK25_FOURTH_20261025.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently recomputed edible-raw food totals, kcal, protein, fat and carbohydrate from the fixed FoodData Central/producer ingredient reference per 100g in OfflineRecipes.ingredients: banana 150g; berries 155g; yogurt 220g; oats 55g; milk 90g. Dry grains remain dry, meat raw, eggs exclude shell, oil entered at full recipe-input weight; no post-cooking yield or individual SKU allergen certification.",
            independentlyRecalculatedRawNutrition =
                Nutrition(585.3, 20.003, 14.3445, 100.2005)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_baked_pear_cottage_berries",
            recipeFingerprintSha256 = "19636d48efc7d2b5d9a76160ab8bdb87ed2e98f760c003e0da8a1d104007121e",
            publishedMethodSource = "https://ls-eng.obozrevatel.com/section-ls-food/news-tastier-than-any-dessert-healthy-baked-pears-with-cheese-and-honey-12-09-2024.html",
            methodSnapshotSha256 = "8a51e4883111f0b6438c89ca2d84bab968a6c1afe0045019c9c15fdbea0b751b",
            methodSnapshotText = "Source inspected on 2026-10-25: The published baked pear dessert halves and cores pears, fills cavities with ricotta and berries, then oven-bakes around 180 C until tender, producing warm soft fruit with fresh dairy cheese filling.",
            methodApplicabilityNotes = "FoodPlan uses 5% cottage cheese mixed with yogurt instead of ricotta, bakes the cheese-stuffed pear without berries and adds defrosted berries afterward, omitting sweetener and nuts. Source is applicable to pear cheese filling technique but not exact dairy moisture or cooked berry texture.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK25_FOURTH_20261025.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently recomputed edible-raw food totals, kcal, protein, fat and carbohydrate from the fixed FoodData Central/producer ingredient reference per 100g in OfflineRecipes.ingredients: pear 190g; cottage_cheese 130g; berries 65g; yogurt 55g. Dry grains remain dry, meat raw, eggs exclude shell, oil entered at full recipe-input weight; no post-cooking yield or individual SKU allergen certification.",
            independentlyRecalculatedRawNutrition =
                Nutrition(334.55647, 23.58175, 9.06447, 40.45556)
        ),
    ).associateBy { it.recipeId }
}
