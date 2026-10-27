package com.foodplan.domain

/**
 * Sixth YAR-6 batch, 25 distinct authored candidate IDs; 12 independently
 * published method comparisons and raw edible input arithmetic; 13 rejected.
 * Not a physical cooking trial, finished-dish nutrition, or SKU safety proof.
 */
object M2PublishedMethodBulkSixth20261026 {
    val reviews: Map<String, RecipeDocumentaryReview> = listOf(
        RecipeDocumentaryReview(
            recipeId = "b5_savory_millet_mushrooms",
            recipeFingerprintSha256 = "62b35e2c1a2991cee37cce4d40330aef1e7af0a293941d21f2fe4faaa156c2d0",
            publishedMethodSource = "https://www.coop.ch/en/brands-inspiration/recipes/mushrooms/millet-risotto-with-fried-mushrooms/rc/447/r/23932?context=recipe",
            methodSnapshotSha256 = "e64ea27a8d82f118426fd58365dbcad9121193401519ab94c173820e64fe7321",
            methodSnapshotText = "Source-checked 2026-10-26. Published Coop method boils millet to softness while mushrooms and shallots fry in a second pan and grated cheese is mixed into cooked grains. FoodPlan separately boils millet in water, fries champignons with onion in sunflower oil, combines all with Gouda and heats covered.",
            methodApplicabilityNotes = "Source cooks millet risotto in stock/wine with rosemary while FoodPlan plain-boils it in water; omit almonds and herbs. Separate mushroom pan/cheese finish match. No claim same cook time.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK25_SIXTH_20261026.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent raw-edible input arithmetic: millet=75g, mushroom=160g, cheese=40g, onion=45g, oil=7g. Each K/P/F/C value is computed as sum of ingredient grams / 100 × pinned per-100g constants in OfflineRecipes. Dry millet/rice/buckwheat/pasta are DRY inputs; fresh meats RAW, canned chickpeas drained, egg edible without shell, raw fruit/vegetables. Oil is entered grams, not verified uptake. Finished cooked weight/retention and SKU allergen-trace UNKNOWN.",
            independentlyRecalculatedRawNutrition =
                Nutrition(540.98, 23.68, 21.73, 64.9445)
        ),
        RecipeDocumentaryReview(
            recipeId = "b5_buckwheat_banana_casserole",
            recipeFingerprintSha256 = "46b2d5880a6f9401d1594c022be09a6feda366e1c7a09109bbc854c6337a8e61",
            publishedMethodSource = "https://www.flaxfarm.co.uk/flaxfarm/baked-special-buckwheat-linseed-quinoa-porridge-recipe/",
            methodSnapshotSha256 = "16c143e554dc784f98040cee31a4a132252693bd9d9fedfa5ea5c837e03e6f9c",
            methodSnapshotText = "Source-checked 2026-10-26. Flax Farm baked buckwheat-containing breakfast mixes sliced banana, milk, egg and a buckwheat-linseed-quinoa porridge grain mixture in a casserole dish and oven-bakes at about 180 C. FoodPlan first boils pure dry buckwheat groats, then adds mashed banana, egg, milk and oven-bakes until set, serving yogurt separately.",
            methodApplicabilityNotes = "Published cereal is a multigrain FLAX-BUCKWHEAT-QUINOA blend and not whole buckwheat, and publisher uses no initial water boil; this is a substantial grain-state adaptation explicitly recorded. Do not copy published cooked nutrition.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK25_SIXTH_20261026.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent raw-edible input arithmetic: buckwheat=75g, banana=125g, egg=60g, milk=150g, yogurt=50g. Each K/P/F/C value is computed as sum of ingredient grams / 100 × pinned per-100g constants in OfflineRecipes. Dry millet/rice/buckwheat/pasta are DRY inputs; fresh meats RAW, canned chickpeas drained, egg edible without shell, raw fruit/vegetables. Oil is entered grams, not verified uptake. Finished cooked weight/retention and SKU allergen-trace UNKNOWN.",
            independentlyRecalculatedRawNutrition =
                Nutrition(578.55, 24.06, 14.951, 94.8385)
        ),
        RecipeDocumentaryReview(
            recipeId = "b5_apple_yogurt_layer_crepes",
            recipeFingerprintSha256 = "a297d3e692112573c4fe9df49deb4336d7ec13c32919a03d07729c8467643da1",
            publishedMethodSource = "https://rangemoors.co.uk/2025/12/16/indulgent-crepe-cake-recipe-with-apples-and-butterscotch-sauce-from-esse/",
            methodSnapshotSha256 = "02f26d9f2de0d815bd45d2f89db71b257de084948e0fc5a946915650a8c0dd2f",
            methodSnapshotText = "Source-checked 2026-10-26. Philippa Vine published apple crepe cake combines milk eggs and flour into thin pan-fried crepes, softens sliced apples in a pan with a little water, then stacks crepes with apple between layers. FoodPlan likewise pan-fries flour-milk-egg crepes, softens apple in water and layers with yogurt.",
            methodApplicabilityNotes = "FoodPlan replaces sweet butterscotch/cream with unsweetened natural yogurt, omits cinnamon/sugar/butter and final oven baking of layered stack. The core flour crepe and softened apple layer technique is retained.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK25_SIXTH_20261026.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent raw-edible input arithmetic: flour=75g, egg=65g, milk=165g, apple=155g, yogurt=110g, oil=6g. Each K/P/F/C value is computed as sum of ingredient grams / 100 × pinned per-100g constants in OfflineRecipes. Dry millet/rice/buckwheat/pasta are DRY inputs; fresh meats RAW, canned chickpeas drained, egg edible without shell, raw fruit/vegetables. Oil is entered grams, not verified uptake. Finished cooked weight/retention and SKU allergen-trace UNKNOWN.",
            independentlyRecalculatedRawNutrition =
                Nutrition(665.84, 26.4775, 22.983, 90.9925)
        ),
        RecipeDocumentaryReview(
            recipeId = "b5_chicken_zucchini_boats_millet",
            recipeFingerprintSha256 = "550a0ef5b84d572da3fac2948736b82e9389a7836da23dd69040c83fe9bf8fd2",
            publishedMethodSource = "https://www.campbells.com/prego/recipes/chicken-alfredo-zucchini-boats/",
            methodSnapshotSha256 = "5aeab85db11f935b931e5f0a4f3de911e56fe12ca0b37a11e626870b46434bb5",
            methodSnapshotText = "Source-checked 2026-10-26. Campbell's published chicken zucchini boats hollows zucchini, combines RAW chicken with a moist filling and vegetables, fills the boats, and oven-bakes around 35 minutes until poultry is fully cooked. FoodPlan hollows raw zucchini, pre-boils millet and packs chopped RAW chicken with onion and fresh tomato into the boats, then bakes to full poultry doneness.",
            methodApplicabilityNotes = "Crucial raw-chicken-stuffed zucchini oven technique matches. Millet replaces breadcrumb/egg/Alfredo binder, other flavors and cheese omitted. Source times/finished weight and retail allergens are not transferable.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK25_SIXTH_20261026.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent raw-edible input arithmetic: zucchini=325g, chicken=180g, millet=65g, tomato=150g, onion=55g, oil=7g. Each K/P/F/C value is computed as sum of ingredient grams / 100 × pinned per-100g constants in OfflineRecipes. Dry millet/rice/buckwheat/pasta are DRY inputs; fresh meats RAW, canned chickpeas drained, egg edible without shell, raw fruit/vegetables. Oil is entered grams, not verified uptake. Finished cooked weight/retention and SKU allergen-trace UNKNOWN.",
            independentlyRecalculatedRawNutrition =
                Nutrition(602.6912, 53.5655, 14.6192, 68.432)
        ),
        RecipeDocumentaryReview(
            recipeId = "b5_turkey_cabbage_oven_patties",
            recipeFingerprintSha256 = "f8821157f127504b29341d1880afd1f8b6040162c108aff6b159abd6890b54be",
            publishedMethodSource = "https://www.ginutritionfoundation.org/recipes/baked-turkey-cutlets",
            methodSnapshotSha256 = "0d032f95be95b5bdf48aedbfb5066e833ff97cdccc424c034429b4e098fcd42c",
            methodSnapshotText = "Source-checked 2026-10-26. GI Nutrition Foundation baked turkey cutlets finely chops RAW turkey fillet with vegetables and egg, shapes cutlets in a lightly oiled baking dish and bakes at 190 C until cooked. FoodPlan chops turkey, shreds cabbage, adds egg and flour, shapes patties and bakes, with tomato on the side.",
            methodApplicabilityNotes = "Vegetable substitution: cabbage instead of onion and pepper; flour binder added and optional seasonings omitted. Both use raw turkey+egg formed and oven-cooked; no diet-specific safety inference or exact thermal equivalence.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK25_SIXTH_20261026.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent raw-edible input arithmetic: turkey=195g, cabbage=170g, egg=60g, flour=35g, tomato=175g, oil=8g. Each K/P/F/C value is computed as sum of ingredient grams / 100 × pinned per-100g constants in OfflineRecipes. Dry millet/rice/buckwheat/pasta are DRY inputs; fresh meats RAW, canned chickpeas drained, egg edible without shell, raw fruit/vegetables. Oil is entered grams, not verified uptake. Finished cooked weight/retention and SKU allergen-trace UNKNOWN.",
            independentlyRecalculatedRawNutrition =
                Nutrition(590.44675, 60.951125, 18.19375, 44.618875)
        ),
        RecipeDocumentaryReview(
            recipeId = "b5_turkey_broccoli_rice_milk_soup",
            recipeFingerprintSha256 = "9983a50bc6ba2bda696a4715c72cdffd90748854c6f478c1219adf7ab59f4aa1",
            publishedMethodSource = "https://dairyfarmersofcanada.ca/en/canadian-goodness/recipes/homemade-turkey-and-rice-soup",
            methodSnapshotSha256 = "0d8f85d5ae685a563b69d110e0e60e432161dbfc24cb984220af27e25e69e1be",
            methodSnapshotText = "Source-checked 2026-10-26. Dairy Farmers of Canada's fresh turkey rice soup cooks raw turkey with vegetables, simmers uncooked rice in stock and adds milk near the end until meat and rice are fully cooked. FoodPlan simmers raw turkey and dry rice in water, adds grated carrot and broccoli, then heats milk into the soup.",
            methodApplicabilityNotes = "Source turkey is browned first and briefly removed; FoodPlan instead poaches raw cubes in soup. Broccoli substituted for celery/onion, no flour/stock thickener. Source finished macros and cooking time cannot be reused.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK25_SIXTH_20261026.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent raw-edible input arithmetic: turkey=165g, rice=70g, broccoli=180g, milk=180g, carrot=85g. Each K/P/F/C value is computed as sum of ingredient grams / 100 × pinned per-100g constants in OfflineRecipes. Dry millet/rice/buckwheat/pasta are DRY inputs; fresh meats RAW, canned chickpeas drained, egg edible without shell, raw fruit/vegetables. Oil is entered grams, not verified uptake. Finished cooked weight/retention and SKU allergen-trace UNKNOWN.",
            independentlyRecalculatedRawNutrition =
                Nutrition(641.4435, 55.052695, 9.8671, 84.519205)
        ),
        RecipeDocumentaryReview(
            recipeId = "b5_beef_stuffed_mushroom_caps",
            recipeFingerprintSha256 = "d1f4f64493ddbf6b0e9d2c17d0d647340348ac243b4531e3b4f6654d1c85b1ea",
            publishedMethodSource = "https://www.iabeef.org/recipes/recipe/4900/beef-and-blue-cheese-stuffed-mushrooms",
            methodSnapshotSha256 = "f6bef634018b5b7e557df1fe07cbfc863d6d33b41b16911d1ef8985e0c7ed150",
            methodSnapshotText = "Source-checked 2026-10-26. Iowa Beef Industry Council removes and chops mushroom stems, combines them with RAW ground beef and cheese, fills mushroom caps with that mix, then oven-bakes until the beef is cooked. FoodPlan finely chops beef and onion with stems, adds raw egg, fills caps and finishes with shredded Gouda.",
            methodApplicabilityNotes = "Ground meat replaced by finely chopped raw beef, source breadcrumbs/chives omitted, egg and onion added, Gouda replaces blue cheese. Core RAW BEEF in mushroom caps plus cheese and complete oven cooking agree.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK25_SIXTH_20261026.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent raw-edible input arithmetic: mushroom=260g, beef=180g, onion=70g, egg=45g, cheese=50g, oil=7g. Each K/P/F/C value is computed as sum of ingredient grams / 100 × pinned per-100g constants in OfflineRecipes. Dry millet/rice/buckwheat/pasta are DRY inputs; fresh meats RAW, canned chickpeas drained, egg edible without shell, raw fruit/vegetables. Oil is entered grams, not verified uptake. Finished cooked weight/retention and SKU allergen-trace UNKNOWN.",
            independentlyRecalculatedRawNutrition =
                Nutrition(657.63, 65.95, 37.478, 16.556)
        ),
        RecipeDocumentaryReview(
            recipeId = "b5_pork_millet_meatballs",
            recipeFingerprintSha256 = "fa3d3737d997c4f858bd49af1455bd9e6f030dba7f90134fe9690e7a86282735",
            publishedMethodSource = "https://stsidwells.org.uk/food/cookery-school/pork-meatballs-and-tomato-sauce",
            methodSnapshotSha256 = "83e8768a94fdf3f311fb1cf05d1b72838b8c2209f267d19a4da4186918292a06",
            methodSnapshotText = "Source-checked 2026-10-26. St Sidwell's cookery school mixes RAW chopped pork mince and onion into meatballs, drops uncooked balls into prepared chopped tomato sauce, covers and simmers until pork fully cooked. FoodPlan mixes chopped raw pork with onion, egg and partly boiled millet, shapes balls and simmers in tomatoes, serving rest of millet on side.",
            methodApplicabilityNotes = "Millet and egg are additional binders, source uses other herbs, garlic and stock; source pre-cooks tomato sauce separately. Raw pork balls plus lidded tomato simmer sequence matches; no publisher nutritional values transferred.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK25_SIXTH_20261026.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent raw-edible input arithmetic: pork=190g, millet=65g, tomato=220g, onion=65g, egg=45g, oil=7g. Each K/P/F/C value is computed as sum of ingredient grams / 100 × pinned per-100g constants in OfflineRecipes. Dry millet/rice/buckwheat/pasta are DRY inputs; fresh meats RAW, canned chickpeas drained, egg edible without shell, raw fruit/vegetables. Oil is entered grams, not verified uptake. Finished cooked weight/retention and SKU allergen-trace UNKNOWN.",
            independentlyRecalculatedRawNutrition =
                Nutrition(813.73, 52.9, 38.632, 62.4135)
        ),
        RecipeDocumentaryReview(
            recipeId = "b5_tofu_cabbage_tomato_pasta",
            recipeFingerprintSha256 = "9d03c69668dcd927db4cc0a995dde1cadad6d3c89a87dbb91fa1f563696e89d6",
            publishedMethodSource = "https://veganlovlie.com/tofu-cabbage-tomato-sauce/",
            methodSnapshotSha256 = "3d30cc5970f8cf9d6272559d8b26c62003fb3691713e4cf58bce5a1f2500dd15",
            methodSnapshotText = "Source-checked 2026-10-26. Veganlovlie Tofu and Cabbage in Tomato Sauce explicitly serves tofu and cabbage in tomato sauce OVER PASTA; published steps cube firm tofu and cook with cabbage in a tomato-rich pan sauce. FoodPlan boils pasta separately, braises thinly sliced white cabbage and onion with fresh tomatoes, adds tofu and the cooked pasta.",
            methodApplicabilityNotes = "FoodPlan uses white vs publisher red cabbage, fresh tomato rather than jarred tomato sauce, omits soy-marination, mushrooms/spinach and herbs, combines in pan instead of serving over pasta. No allergen label certification.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK25_SIXTH_20261026.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent raw-edible input arithmetic: tofu=205g, pasta=85g, cabbage=210g, tomato=160g, onion=60g, oil=10g. Each K/P/F/C value is computed as sum of ingredient grams / 100 × pinned per-100g constants in OfflineRecipes. Dry millet/rice/buckwheat/pasta are DRY inputs; fresh meats RAW, canned chickpeas drained, egg edible without shell, raw fruit/vegetables. Oil is entered grams, not verified uptake. Finished cooked weight/retention and SKU allergen-trace UNKNOWN.",
            independentlyRecalculatedRawNutrition =
                Nutrition(682.44775, 33.702625, 20.68975, 94.545875)
        ),
        RecipeDocumentaryReview(
            recipeId = "b5_chickpea_potato_gratin",
            recipeFingerprintSha256 = "0e78db5240460689c22cd941f338321cecf24f882a663a1c54199dc0d31f7213",
            publishedMethodSource = "https://recipeguide.info/Potato-gratin-with-spinach-R37979",
            methodSnapshotSha256 = "96991c07e529889bb480bd8e737ff71f70ca8079929653c343fbd8fa1ef7bff2",
            methodSnapshotText = "Source-checked 2026-10-26. Published potato gratin drains canned chickpeas, lightly cooks onion and potato slices, layers them with sliced tomatoes, tops with shredded cheese and bakes until potatoes soften. FoodPlan parboils sliced potato, drains canned chickpeas and layers these with onion/tomato/Gouda for oven baking.",
            methodApplicabilityNotes = "FoodPlan omits published spinach, parboils rather than oil-fries potatoes and mixes fresh tomatoes across layers; both use sliced potatoes, drained canned chickpeas, tomato, onion, cheese and oven bake.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK25_SIXTH_20261026.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent raw-edible input arithmetic: potato=285g, chickpeas=210g, tomato=155g, onion=70g, cheese=55g, oil=9g. Each K/P/F/C value is computed as sum of ingredient grams / 100 × pinned per-100g constants in OfflineRecipes. Dry millet/rice/buckwheat/pasta are DRY inputs; fresh meats RAW, canned chickpeas drained, egg edible without shell, raw fruit/vegetables. Oil is entered grams, not verified uptake. Finished cooked weight/retention and SKU allergen-trace UNKNOWN.",
            independentlyRecalculatedRawNutrition =
                Nutrition(842.61, 36.4985, 30.5455, 110.948)
        ),
        RecipeDocumentaryReview(
            recipeId = "b5_lentil_cabbage_pepper_soup",
            recipeFingerprintSha256 = "da601701589b96830ae0de065c6cae8fd801bfc7a498f3c8b250417633dd38ef",
            publishedMethodSource = "https://www.visionmatters.org.uk/downloads/nehw25-vegetable-and-lentil-soup-recipe-card-a5.pdf",
            methodSnapshotSha256 = "244bb2b729e2a0fdb55f1f96301ba12d27435e5184051fe30f6e63958450ccaf",
            methodSnapshotText = "Source-checked 2026-10-26. NHS Better Health reproduced vegetable and lentil soup includes dried lentils, fresh carrot and red bell pepper simmered in stock until tender, then finished for serving. FoodPlan simmers dried lentils in water, adds carrot, red pepper and shredded cabbage, then garlic and sunflower oil and serves without blending.",
            methodApplicabilityNotes = "Source red lentils-carrot-pepper pot method matches; FoodPlan adds cabbage, garlic and oil, omits onion/stock/yogurt and leaves chunks instead of blending. This is adapted soup, not NHS nutrition or complete nutrition certification.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK25_SIXTH_20261026.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent raw-edible input arithmetic: lentils=95g, cabbage=200g, bell_pepper=160g, carrot=110g, garlic=7g, oil=8g. Each K/P/F/C value is computed as sum of ingredient grams / 100 × pinned per-100g constants in OfflineRecipes. Dry millet/rice/buckwheat/pasta are DRY inputs; fresh meats RAW, canned chickpeas drained, egg edible without shell, raw fruit/vegetables. Oil is entered grams, not verified uptake. Finished cooked weight/retention and SKU allergen-trace UNKNOWN.",
            independentlyRecalculatedRawNutrition =
                Nutrition(564.685, 28.3914, 10.2326, 95.11)
        ),
        RecipeDocumentaryReview(
            recipeId = "b5_mushroom_zucchini_stuffed_potatoes",
            recipeFingerprintSha256 = "838986a158d6c5ecb917ecdee2c556a7d5eafa2876ab34cca61299fcd9b1269e",
            publishedMethodSource = "https://thehappyfoodie.co.uk/recipes/janes-patisseries-loaded-potato-skins/",
            methodSnapshotSha256 = "3bc97d9c183d5e60d57b76e481645b096c4eebbc002e8f920374821924d238d8",
            methodSnapshotText = "Source-checked 2026-10-26. Jane's Patisserie loaded potato skins bakes jacket potatoes, slices and hollows them, mixes saved potato flesh with raw egg, cheese and a cooked filling, returns the filling to the potato shells and oven-bakes until set. FoodPlan separately fries mushrooms and zucchini, mixes with potato flesh, egg and grated Gouda, fills shells and oven-bakes.",
            methodApplicabilityNotes = "FoodPlan substitutes sauteed mushroom and zucchini for bacon/onions/soft cheese and omits sour cream and final garnish; potato skin method with raw egg and two-stage oven cooking is aligned. No exact total time equivalence.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK25_SIXTH_20261026.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent raw-edible input arithmetic: potato=310g, mushroom=185g, zucchini=135g, cheese=50g, egg=55g, oil=8g. Each K/P/F/C value is computed as sum of ingredient grams / 100 × pinned per-100g constants in OfflineRecipes. Dry millet/rice/buckwheat/pasta are DRY inputs; fresh meats RAW, canned chickpeas drained, egg edible without shell, raw fruit/vegetables. Oil is entered grams, not verified uptake. Finished cooked weight/retention and SKU allergen-trace UNKNOWN.",
            independentlyRecalculatedRawNutrition =
                Nutrition(629.72, 32.995, 28.538, 66.0865)
        ),
    ).associateBy { it.recipeId }
}
