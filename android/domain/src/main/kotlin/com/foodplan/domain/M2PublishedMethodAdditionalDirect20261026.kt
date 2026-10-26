package com.foodplan.domain

/**
 * YAR-6 two independently source-matched methods from previously pending
 * authored dishes. They are not assertions of physical cooking trials,
 * finished-product nutrient laboratory assays, or retailer-SKU allergen safety.
 */
object M2PublishedMethodAdditionalDirect20261026 {
    val reviews: Map<String, RecipeDocumentaryReview> = listOf(
        RecipeDocumentaryReview(
            recipeId = "cur_mushroom_cheese_buckwheat_bake",
            recipeFingerprintSha256 = "d8318b258464e01ad2788ea7650fe5beb0e8c85465a9da9c855efc5ec7ec6b51",
            publishedMethodSource = "https://lifehacker.ru/recipe/zapekanka-s-grechkoj-gribami-i-syrom/",
            methodSnapshotSha256 = "8957acba4e9f0297ff433967afea4cd490b71fa9c3e5112637c93e0188e7563a",
            methodSnapshotText = "Captured 2026-10-26. Lifehacker published a buckwheat, champignon, onion, oil, egg and grated-cheese casserole: boil buckwheat, fry chopped onion and mushrooms in vegetable oil, combine with beaten eggs in an oven form, sprinkle cheese and bake. FoodPlan uses the same operations but omits published milk and optional seasoning, uses a smaller egg binder and independent thermometer-guided casserole doneness.",
            methodApplicabilityNotes = "Lifehacker cooks buckwheat first, then separate onion/mushroom frying with oil, and an egg-and-milk custard before topping with cheese. FoodPlan omits milk, fries onion and mushrooms together, and uses 74 C center as an independent casserole doneness requirement. Source recipe masses, cook time, egg binding and absorbed oil are NOT transferred.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_DIRECT_TWO_20261026.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently recomputed raw edible input from FDC per-100g pinned OfflineRecipes ingredients: 90g dry buckwheat, 200g raw champignons, 70g raw onion, 75g Gouda, 65g raw edible egg (no shell), 9g sunflower oil. Each grams / 100 times source kcal/P/F/C and sum. Actual absorbed frying oil, cooked retention, yield and SKU allergen traces UNKNOWN.",
            independentlyRecalculatedRawNutrition = Nutrition(822.91000000, 44.27200000, 39.24300000, 82.80200000)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_oat_waffles_pear",
            recipeFingerprintSha256 = "3c6825cd7139eee4e080142d16afd49a9e36ef4663ca2f29f11b80b94a0b02d6",
            publishedMethodSource = "https://www.arlafoods.co.uk/recipes/no-cheat-treat-waffles/",
            methodSnapshotSha256 = "0ed1dcc76ffa611e2be59a6809f6b11a61dee953f4d7c1941032396563235ef4",
            methodSnapshotText = "Captured 2026-10-26. Arla UK publishes oat waffles made from ground or blended oat flakes, eggs and milk: mix the batter, leave it to hydrate, oil a hot waffle maker and cook waffle portions, then serve with the cook's chosen topping. Vitamix independently documents blending rolled oats into waffle batter and preheating the waffle iron. FoodPlan adds raw sliced pear topping, uses sunflower oil, no baking powder or sugar and a shorter five-minute rest.",
            methodApplicabilityNotes = "Arla's oat-flour egg-and-milk waffle method and Vitamix rolled-oat blending both apply. FoodPlan does not add the original baking powder, spices, sugar or extra wheat flour, rests for five minutes instead of fifteen and chooses sliced fresh pear as a topping. Cooked waffle texture and absorbed oil are not validated by source nutrition.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_DIRECT_TWO_20261026.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent edible-raw composition: 70g dry rolled oats, 60g raw edible whole egg, 90g whole milk, 120g Anjou pear with edible peel, 6g sunflower oil. Each grams / 100 multiplied by independently pinned FDC kcal/P/F/C. Oil consumption and cooked yield, food safety thermometer reading and allergens of sold SKU not laboratory verified.",
            independentlyRecalculatedRawNutrition = Nutrition(535.43356000, 19.85500000, 19.92856000, 69.98888000)
        ),
    ).associateBy { it.recipeId }
}
