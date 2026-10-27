package com.foodplan.domain

/**
 * Reviewed published cooking-method records: batch 2 of M2.
 * Fingerprints and nutrient totals are frozen outside the planner, with the
 * method assertions independently checked against the linked publications.
 * Captured excerpts are own-language summaries, NOT source HTML snapshots.
 */
object M2PublishedMethodReviewBatch20261024 {
    private val scrambledFacts =
            "Source review on 2026-10-24, Food.com scrambled eggs with spiced mushrooms (recipe 63" +
            "704): heat skillet oil, saute sliced mushrooms until tender and lightly browned; beat" +
            " eggs separately, lower heat, add eggs and stir/scramble until set. Publication also " +
            "calls for onion, paprika, cumin, cayenne and parsley; FoodPlan deliberately excludes " +
            "these seasonings and onion, keeping the mushroom-first low-heat egg finish. No cooked" +
            " weight is supplied by FoodPlan."

    private val shakshukaFacts =
            "Source review on 2026-10-24, The Mediterranean Dish authentic shakshuka: in oil softe" +
            "n sliced onions and bell peppers (plus garlic and spices), simmer added chopped tomat" +
            "oes until a thick sauce, then make wells, crack eggs into the sauce and cook covered " +
            "until egg whites are set. FoodPlan has fresh tomatoes, peppers, onion, eggs and sunfl" +
            "ower oil; omits tomato sauce, garlic and seasoning; weight and per-portion timings ar" +
            "e different, but the core cooking sequence is unchanged."

    private val lentilFacts =
            "Source review on 2026-10-24, Center for Science in the Public Interest Tomato Lentil " +
            "Soup by Kate Sherwood: saute onion in oil, add dry rinsed lentils, chopped tomatoes a" +
            "nd carrots with water, bring to boil then simmer until lentils soften. The source inc" +
            "ludes celery and tomato paste and sautes only onion, while FoodPlan also warms carrot" +
            " with onion, omits paste/celery and simmers more slowly. Lentil grams remain DRY and " +
            "tomato/carrot/onion RAW in the FoodPlan nutrition calculation."

    private val buckwheatFacts =
            "Source review on 2026-10-24, GI Nutrition Foundation Buckwheat with Mushrooms: rinse " +
            "dry buckwheat groats and boil in water until tender; in a skillet saute chopped onion" +
            " and button mushrooms in oil until mushrooms soften and moisture reduces; combine the" +
            " two and heat briefly. FoodPlan uses the same basic boil, saute, combine workflow, wi" +
            "th sunflower oil instead of olive and different buckwheat/mushroom/onion mass and tot" +
            "al time. Cooking liquid is not counted as energy."

    val reviews: Map<String, RecipeDocumentaryReview> = listOf(
        RecipeDocumentaryReview(
            recipeId = "cur_scrambled_mushrooms",
            recipeFingerprintSha256 = "d0d9ac77337c982360d26e4ef74373380879904bd25afda7a2dc641a75e66190",
            publishedMethodSource = "https://www.food.com/recipe/scrambled-eggs-with-spiced-mushrooms-63704",
            methodSnapshotSha256 = "3de5883d1c829e60fe9dcf81a3ab9484db0b92b82388af697576431a0d3a68fa",
            methodSnapshotText = scrambledFacts,
            methodApplicabilityNotes = "Publication fries mushrooms before stirring in whisked eggs; FoodPlan omits its onion" +
            " and flavoring spices, with a different portion size and 16-minute total estimate. No" +
            " cooked yield or food hygiene certification inferred.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_METHOD_BATCH2_20261024.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Raw edible whole egg 120g (FDC 748967), raw white button mushrooms 120g (FDC 169251)," +
            " sunflower oil 6g (FDC 171025); separately re-summed decimal kcal and P/F/C values us" +
            "ing each pinned per-100g basis. All oil provisionally assumed retained; cooked nutrie" +
            "nt retention UNKNOWN.",
            independentlyRecalculatedRawNutrition =
                Nutrition(251.04, 18.588, 18.36, 5.064)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_shakshuka",
            recipeFingerprintSha256 = "4294a43da5cbe099b9561f6d3d318b4707d220de0d640fb533b68eadb2090203",
            publishedMethodSource = "https://www.themediterraneandish.com/shakshuka-recipe/",
            methodSnapshotSha256 = "c41fae130a069c5a7cbcab2a7eee17e69bec3725a206a21b621185a081a2b048",
            methodSnapshotText = shakshukaFacts,
            methodApplicabilityNotes = "Source confirms pepper/onion saute, tomato reduction and lidded egg poaching in wells" +
            ". FoodPlan omits garlic, extra tomato sauce, spice and herb garnish and changes olive" +
            " to sunflower oil. No claim that source's cooked nutritional values or temperatures c" +
            "arry over.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_METHOD_BATCH2_20261024.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent decimal summation of edible egg 120g FDC 748967, raw tomato 230g FDC 1704" +
            "57, raw bell pepper 110g FDC 170108, raw onion 55g FDC 170000 and sunflower oil 8g FD" +
            "C 171025. Nutrients represent input ingredients before cooking, no loss or yield assu" +
            "mptions.",
            independentlyRecalculatedRawNutrition =
                Nutrition(334.32, 18.598, 20.797, 21.869)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_lentil_tomato_soup",
            recipeFingerprintSha256 = "376c8e81e73e7ebe9ba5c62439373d4a8307acd579c5de5ea9c5eb06305ed444",
            publishedMethodSource = "https://www.cspi.org/recipe/tomato-lentil-soup",
            methodSnapshotSha256 = "7ecc50b8ee269c16861095e43c91155471b1afcc4d17b427a2be9e14297b18d2",
            methodSnapshotText = lentilFacts,
            methodApplicabilityNotes = "FoodPlan adapts the publication by warming carrot with oil and onion before adding ot" +
            "her ingredients, omitting celery and tomato paste, extending simmer (42 vs 35 min). D" +
            "ocumentary soup technique suitable, exact cooked nutrition/portion is NOT transferred" +
            " from publication.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_METHOD_BATCH2_20261024.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent decimal sum of dried uncooked lentils 85g (USDA FDC 172420), raw tomato 1" +
            "80g (170457), raw carrot 90g (170393), raw onion 60g (170000) and sunflower oil 7g (1" +
            "71025). Source cooked soup cup has unrelated nutrition; water excluded as zero energy" +
            ". Retention and yield UNKNOWN.",
            independentlyRecalculatedRawNutrition =
                Nutrition(454.38, 24.0165, 8.537, 75.0755)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_buckwheat_mushroom",
            recipeFingerprintSha256 = "87e4f0555321ad29361a294795a9f245eeea8ba1b13a9100363bfcfe0566757f",
            publishedMethodSource = "https://www.ginutritionfoundation.org/recipes/buckwheat-with-mushrooms",
            methodSnapshotSha256 = "16fcf5b106cb62f778b24d0de8adf874915e3d1772c50b13eaf15e044f30e44e",
            methodSnapshotText = buckwheatFacts,
            methodApplicabilityNotes = "Publication performs boil dry groats, oil-fry onion and sliced mushrooms until soften" +
            "ed, and combine briefly; FoodPlan omits optional herb and salt, switches to sunflower" +
            " oil, changes 55 to 30 total minutes and all quantities. This method is accepted only" +
            " as applicable technique.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_METHOD_BATCH2_20261024.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently summed raw ingredients: dry roasted buckwheat groats 90g USDA FDC 17068" +
            "5 = 346 kcal/100g, raw white button mushrooms 220g FDC 169251, raw onion 75g FDC 1700" +
            "00, sunflower oil 11g FDC 171025. Cooking water is zero-energy, finished dish mass re" +
            "mains UNKNOWN.",
            independentlyRecalculatedRawNutrition =
                Nutrition(487.04, 18.18, 14.262, 81.632)
        ),
    ).associateBy { it.recipeId }
}
