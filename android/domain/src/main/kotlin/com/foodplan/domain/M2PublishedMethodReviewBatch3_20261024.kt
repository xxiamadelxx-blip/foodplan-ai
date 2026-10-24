package com.foodplan.domain

/**
 * M2 third documentary batch: real externally published method matches.
 * Captures are own-language factual summaries, not a snapshot of HTML.
 * Fixed SHA256 values and independent raw-nutrient arithmetic audited at
 * docs/qa/M2_METHOD_BATCH3_20261024.md.
 * No SKU allergy clearance, cooked yield or nutritional sufficiency implied.
 */
object M2PublishedMethodReviewBatch3_20261024 {
    val reviews: Map<String, RecipeDocumentaryReview> = listOf(
        RecipeDocumentaryReview(
            recipeId = "cur_oat_pancakes",
            recipeFingerprintSha256 = "18f82cf2b6ad8865df6499d99f06841c6d5c83f97a89242da522771dbccce179",
            publishedMethodSource = "https://lifehacker.ru/recipe/ovsyanye-oladi-s-bananami/",
            methodSnapshotSha256 = "93bc08b734f88835aa302757b18947c8e92eaae08e008aeda9fd306719cb5917",
            methodSnapshotText = "Source checked 2026-10-24: Lifehacker banana-oat pancakes mash ripe bananas with a fork, add eggs and rolled oats, rest the batter five minutes, spoon portions onto a lightly oiled skillet and fry both sides. FoodPlan preserves this sequence but excludes vanilla extract and cinnamon. Different weights and total time are explicitly documented.",
            methodApplicabilityNotes = "Recipe matches the source's banana/egg/oats mixture, rest and small pancakes pan-fried on both sides. FoodPlan omits optional cinnamon and vanilla, scales to oats 65g, banana 110g, edible egg 60g and sunflower oil 8g; published 25-minute recipe and our 22-minute total are separate estimates.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_METHOD_BATCH3_20261024.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent decimal reconstruction from exactly 65g DRY oats FDC 173904, 110g raw banana FDC 173944, 60g edible egg without shell FDC 748967 and 8g sunflower oil FDC 171025. All oil counted as raw recipe input, not established absorption or post-cook yield.",
            independentlyRecalculatedRawNutrition =
                Nutrition(500.77, 17.1865, 18.577, 69.705)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_mushroom_pasta",
            recipeFingerprintSha256 = "52160b8976d3f5f49ecf6bc626aece5d818ef31794844ae3ab465c4ab5ee45b7",
            publishedMethodSource = "https://anitalianinmykitchen.com/fusilli-with-fresh-tomatoes-and-mushrooms/",
            methodSnapshotSha256 = "ce90189ec5572e856107d271838671269f687f4cb704a5a76dc1768146d432dd",
            methodSnapshotText = "Source checked 2026-10-24: An Italian in my Kitchen Fresh Tomatoes and Mushroom Fusilli sautes sliced mushrooms with olive oil and garlic until tender, mixes in chopped fresh tomatoes and cooks until the sauce thickens, then tosses with separately cooked fusilli. FoodPlan follows these stages but uses sunflower oil and excludes herbs and parmesan.",
            methodApplicabilityNotes = "Both boil pasta separately, saute sliced raw mushrooms and fresh garlic, cook fresh tomatoes until saucy and combine cooked pasta. Source uses olive oil, basil, parsley, oregano and optional parmesan; FoodPlan omits garnishes and uses sunflower oil, dry pasta 90g (not 90g cooked), raw tomato/mushroom 180g each and 30-minute total.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_METHOD_BATCH3_20261024.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent decimal reconstruction from 90g DRY pasta FDC 168927, 180g raw white button mushrooms FDC 169251, 180g raw red tomato FDC 170457, 7g raw garlic FDC 1104647 and 10g sunflower oil FDC 171025. No source cooked-pasta serving or per-100g values substituted.",
            independentlyRecalculatedRawNutrition =
                Nutrition(504.31, 19.3454, 12.3576, 82.047)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_rice_egg_veg",
            recipeFingerprintSha256 = "df28e9b221b20192f3d2884d27b0573d77f678c625dc59a203c11c9e725441dc",
            publishedMethodSource = "https://www.whiskaffair.com/egg-fried-rice-recipe/",
            methodSnapshotSha256 = "5a5030ac804b3c83a63e306de294a80e103e1707d3c994df3b5d7913ebe89156",
            methodSnapshotText = "Source checked 2026-10-24: Whisk Affair Egg Fried Rice uses cooked and cooled rice, stir-fries chopped vegetables, moves the vegetables aside and scrambles whisked eggs in the cleared skillet, then folds in the cooked rice and reheats. Carrot is used and broccoli is an explicitly supported vegetable substitution. FoodPlan excludes soy sauce and chili paste and starts by boiling dry rice.",
            methodApplicabilityNotes = "Core order matches: cook rice, fry vegetables, move vegetables aside to scramble whisked eggs, fold in cooked rice and warm. Broccoli is named as an approved vegetable substitution in publication. FoodPlan skips sauces, aromatics and sesame-oil finish and uses freshly cooked rice from 80g DRY grain rather than cold day-old rice. Texture and exact frying time not certified.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_METHOD_BATCH3_20261024.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent decimal reconstruction from exactly 80g DRY white rice FDC 2512381, 110g edible raw egg FDC 748967, 90g raw carrot FDC 170393, 130g raw broccoli FDC 321900 and 10g sunflower oil FDC 171025. No added soy/ginger or extra ingredients in this version; raw-input nutrition not cooked.",
            independentlyRecalculatedRawNutrition =
                Nutrition(611.164, 23.44908, 22.4404, 82.10552)
        ),
    ).associateBy { it.recipeId }
}
