package com.foodplan.domain

/**
 * Manually reviewed, externally published cooking methods. This registry is
 * intentionally small: never promote 150 draft recipes merely because they
 * have generated instructions or nutrient URLs.
 *
 * Each fingerprint and evidence SHA is a frozen literal verified independently
 * of the runtime recipe calculation. A change to a method invalidates it.
 */
object M2PublishedMethodReviews {
    private const val omeletteCapture =
        "Captured method facts from 1000.menu/cooking/50028-omlet-s-pomidorami-na-skovorode on 2026-10-24: " +
        "the published tomato omelette uses eggs, milk, tomatoes and olive oil. " +
        "Tomatoes are prepared and softened in a skillet first; eggs are mixed with milk separately; " +
        "the liquid egg mixture is poured into the pan and cooked covered until set. " +
        "FoodPlan adaptation omits optional seasoning, uses sunflower rather than olive oil, " +
        "and changes ingredient weights without reversing the published method."

    val reviews: Map<String, RecipeDocumentaryReview> = listOf(
        RecipeDocumentaryReview(
            recipeId = "cur_omelette_tomato",
            // Frozen hash of name, raw ingredients, steps, equipment and times.
            recipeFingerprintSha256 =
                "8f711d7e9336b2d01cfaa37731fb51d10c784f225642d5a867fd32c7fb693592",
            publishedMethodSource =
                "https://1000.menu/cooking/50028-omlet-s-pomidorami-na-skovorode",
            // Digest of the factual METHOD EXCERPT above; NOT the page HTML.
            methodSnapshotSha256 =
                "14731f00cdb252a84b9bc3f0114ebe767d28bae170d434cb35864c7346b0f2ce",
            methodSnapshotText = omeletteCapture,
            methodApplicabilityNotes =
                "Same egg-milk mixture and tomato-first covered-skillet technique; " +
                "FoodPlan uses 120 g edible egg, 40 g milk, 120 g tomato and 6 g " +
                "sunflower oil rather than the publication's different quantities, " +
                "olive oil and optional seasoning. Time is 16 rather than 12 min. " +
                "No equivalence of egg count to edible mass or oil mL to grams claimed.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_FIRST_DOCUMENTARY_REVIEW_20261024.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes =
                "Independent exact decimal sum of raw edible egg 120g, milk 40g, " +
                "raw tomato 120g and sunflower oil 6g, each multiplied by published " +
                "per-100g pinned ingredient values from FDC IDs 748967, 172217, " +
                "170457, 171025. Oil stays edible grams, egg excludes shell. " +
                "Cooked nutrient retention, finished dish yield and SKU safety UNKNOWN.",
            // Independently recomputed from pinned per-100g nutrient values.
            independentlyRecalculatedRawNutrition =
                Nutrition(270.64, 17.196, 19.5, 7.732)
        )
    ).associateBy { it.recipeId }
}
