package com.foodplan.domain

/**
 * YAR-6 pilot: original recipes composed from independently published
 * operation methods. Source-method summaries have local SHA integrity, NOT
 * cryptographic publisher HTML provenance or finished-dish certification.
 */
object M2PublishedMethodComposed20261026 {
    val reviews: Map<String, RecipeDocumentaryReview> = listOf(
        RecipeDocumentaryReview(
            recipeId = "b4_savory_carrot_oat_bake",
            recipeFingerprintSha256 = "6acfabfe57e0e9447d411f10403b4fb5144bcd440171d3f63dabce2f16ea5dfa",
            publishedMethodSource = "https://coffeewiththechickenladies.com/farm-fresh-egg-recipes/savory-baked-oatmeal-and-veg/",
            methodSnapshotSha256 = "73c55838f051c365c1fbf2b55df6de189011230760ac471ce5b90d3d322ce95c",
            methodSnapshotText = "Captured 2026-10-26. Cross-publication method reconstruction: Quaker carrot baked oats includes finely shredded carrot in oats; an independent savory baked-oat preparation mixes rolled oats with egg, dairy milk, vegetables and grated cheese; Canadian Oats Everyday spreads the combined batter in an oven dish; FSIS describes checking a casserole centre with a thermometer. FoodPlan skips optional soda, oil, pre-sautéing and other vegetables, and is not described as the exact publisher's formula.",
            methodApplicabilityNotes = "The published component preparations are combined into one authored savory oat/carrot/egg/milk/Gouda bake. Quaker differs with sweet additions; the savory oat bake normally sautés vegetables first; FoodPlan adds grated raw carrot and omits leavening. The 36-minute total and finished texture have not been kitchen tested. A thermometer reading >=74C is the independent conservative casserole safety gate, not a cooked macro or mass validation.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_COMPOSED_TECHNIQUE_PILOT_20261026.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Separately recomputed raw edible input oats=65g, carrot=100g, egg=60g, cheese=35g, milk=65g from pinned FDC per-100g kcal/P/F/C; values never represent cooked yield or retained nutrients. No SKU allergy or cost assurance.",
            independentlyRecalculatedRawNutrition = Nutrition(537.4, 27.694, 22.1835, 58.045),
            methodBasis = RecipeMethodBasis.COMPOSED_PUBLISHED_TECHNIQUES,
            requiredCoreTemperatureC = 74,
            safeTemperatureSource =
                "https://www.fsis.usda.gov/food-safety/safe-food-handling-and-preparation/food-safety-basics/safe-temperature-chart",
            composedTechniqueSteps = listOf(
                PublishedTechniqueStep(
                    stepNumber = 1,
                    authoredStepSha256 = "e16906e375a1f9d8696c79674bbecf966ea9c7fb7220a5943eb28b30961bc201",
                    publishedMethodSource = "https://www.quakeroats.com/cooking-and-recipes/carrot-cake-baked-oats",
                    sourceMethodExcerpt = "The published Quaker carrot cake baked oats use ordinary oat flakes and finely shredded carrot in one ramekin baked mixture. The author first blends oats with other batter ingredients and then incorporates carrot in the dish before oven cooking. FoodPlan instead places grated raw carrot directly into oats and omits all sweet additions.",
                    sourceExcerptSha256 = "8e3655e91c96a5aafe92eb670f66a3ece71871c7709815555ad572b823d55a81",
                    applicabilityNotes = "Source supports grated carrot with oats during oven preparation, not a published savoury Gouda recipe. FoodPlan combines carrot before the liquid rather than in the ramekin.",
                    coveredIngredientIds = setOf("oats", "carrot")
                ),
                PublishedTechniqueStep(
                    stepNumber = 2,
                    authoredStepSha256 = "4f1b5868d5bb4781e2f5a9208b4d0b266299cfd64bea0d8ba6fe8b6b9f136ce6",
                    publishedMethodSource = "https://coffeewiththechickenladies.com/farm-fresh-egg-recipes/savory-baked-oatmeal-and-veg/",
                    sourceMethodExcerpt = "The published Savory Baked Oatmeal and Veg recipe combines rolled oats with milk, beaten eggs and shredded cheese, adds vegetables and waits briefly for oats to hydrate. The mixture is not a dry crumble but a thick moist batter suitable for spooning into a baking pan before heating.",
                    sourceExcerptSha256 = "d618d0ce5210b0f1badc1f3af04ed4ff984b3556879d0e8f5e37ff21ab929d72",
                    applicabilityNotes = "Published oatmeal batter has oats, milk, beaten eggs and cheese. FoodPlan adds grated carrot raw, removes pre-sautéed broccoli/asparagus/peas and uses Gouda in smaller weights.",
                    coveredIngredientIds = setOf("egg", "milk", "cheese", "oats")
                ),
                PublishedTechniqueStep(
                    stepNumber = 3,
                    authoredStepSha256 = "e857b5ffbce6c5625058fee86957df122b539bd6e90e4d6cc66bb0c16e8bf04c",
                    publishedMethodSource = "https://oatseveryday.com/recipes/mediterranean-baked-oats/",
                    sourceMethodExcerpt = "Oats Everyday Mediterranean Baked Oats mixes oats, eggs, vegetables and soft cheese into a uniform batter. It then directs the cook to spread the mixture evenly in a greased oven dish rather than cook it on a stovetop. FoodPlan uses a small baking mould with carrot, Gouda cheese and milk instead of feta, broth and peppers.",
                    sourceExcerptSha256 = "2734249e49be8147834feb2d0a4e1b7eeacf41afbcef1019978d8df2785a5047",
                    applicabilityNotes = "Supports spreading an egg- and grain-based mass in an ovenproof dish; vegetables and liquid composition differ. No claim that the exact five FoodPlan ingredients were published together.",
                    coveredIngredientIds = setOf("oats", "carrot", "egg", "milk", "cheese")
                ),
                PublishedTechniqueStep(
                    stepNumber = 4,
                    authoredStepSha256 = "534b36af5e858d090817e0dec629627d565cad7347bcf6a890d96b19e78c6e7f",
                    publishedMethodSource = "https://oatseveryday.com/recipes/mediterranean-baked-oats/",
                    sourceMethodExcerpt = "The source uses oven heat to bake a savory oat, egg and cheese mixture until the middle is set and the top is lightly golden, then lets it rest before slicing. FoodPlan independently adds a conservative thermometer condition of at least 74 Celsius in the centre under FSIS casserole safety guidance.",
                    sourceExcerptSha256 = "ff02be19f188688fe0e107a47f368348953a52330f9e739a22990e49727c1fe4",
                    applicabilityNotes = "Publisher's bake is much larger and lists around 45 minutes in a wider pan; a FoodPlan 36-minute small pan is unmeasured. Do not claim full doneness from appearances alone; check centre >=74 C.",
                    coveredIngredientIds = setOf("egg", "oats", "carrot", "milk", "cheese")
                )
            )
        ),
        RecipeDocumentaryReview(
            recipeId = "b4_buckwheat_apple_cups",
            recipeFingerprintSha256 = "b15c308719c0ffbaf7f87750db935821d0c03fe30fe8c6da72de27aa20280eed",
            publishedMethodSource = "https://ediblevermont.ediblecommunities.com/recipe/recipes-buckwheat-muffins-apples-and-walnuts",
            methodSnapshotSha256 = "eedaa630a321b0fcd27667b25bb8e69563c88d640efa980f2ee7335461bca46b",
            methodSnapshotText = "Captured 2026-10-26. A published preparation for dry buckwheat groats boils them in water, an independent Vermont cooked-kasha muffin uses already cooked buckwheat and diced apples in an egg/flour batter, and the cooked mixture is portioned into oven muffin molds before baking. FoodPlan combines these operations with less flour, no added sweetening or baking powder and cold plain yogurt served as a separate topping after baking.",
            methodApplicabilityNotes = "Cooked grain not buckwheat flour: FoodPlan correctly boils 65g dry groats and then folds cooked cereal with chopped apples, egg and 20g wheat flour. Published muffins contain additional flour, oil and cider or other wet ingredients, so their rise, texture, temperature and times are not transferable. Yogurt is a cold garnish, not a binder or moisture source during cooking.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_COMPOSED_TECHNIQUE_PILOT_20261026.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Separately recomputed raw edible input buckwheat=65g, apple=140g, egg=55g, flour=20g, yogurt=75g from pinned FDC per-100g kcal/P/F/C; values never represent cooked yield or retained nutrients. No SKU allergy or cost assurance.",
            independentlyRecalculatedRawNutrition = Nutrition(494.5, 19.811, 10.255, 86.9945),
            methodBasis = RecipeMethodBasis.COMPOSED_PUBLISHED_TECHNIQUES,
            requiredCoreTemperatureC = 74,
            safeTemperatureSource =
                "https://www.fsis.usda.gov/food-safety/safe-food-handling-and-preparation/food-safety-basics/safe-temperature-chart",
            composedTechniqueSteps = listOf(
                PublishedTechniqueStep(
                    stepNumber = 1,
                    authoredStepSha256 = "598cd98a70c25b089ba0a4a9393cbf69350eafd81f0b0d4c9b49c477228f06e4",
                    publishedMethodSource = "https://www.bobsredmill.com/recipes/how-to-make/basic-preparation-instructions-for-buckwheat-groats",
                    sourceMethodExcerpt = "Bob's Red Mill's independent preparation guide brings water to a boil, adds buckwheat groats, lowers the heat and simmers covered for about ten to fifteen minutes until the grains are tender. FoodPlan rinses its dry buckwheat before cooking, then allows the cooked grain to cool for the later muffin-like egg mixture.",
                    sourceExcerptSha256 = "2bc0d29166c38a6d7fd6dd538c350651319226a17a1b7a0969ec0c945fcecebe",
                    applicabilityNotes = "Buckwheat groats are cooked first as dry cereal, not flour or cooked-weight input; FoodPlan adds a cooling step. Published water amounts and cooking time may vary with the raw groats.",
                    coveredIngredientIds = setOf("buckwheat")
                ),
                PublishedTechniqueStep(
                    stepNumber = 2,
                    authoredStepSha256 = "31a324c901da1368d52c2d62c8d6155e3facdfd9495b88b059012d933b729ba3",
                    publishedMethodSource = "https://ediblevermont.ediblecommunities.com/recipe/recipes-buckwheat-muffins-apples-and-walnuts",
                    sourceMethodExcerpt = "Edible Vermont's published buckwheat apple muffin preparation includes cooked roasted buckwheat groats, diced apples and beaten eggs with wheat flour in a portionable batter. FoodPlan uses this combination after separately cooking the groats, but omits its other muffin ingredients such as baking powder, sweeteners and nuts.",
                    sourceExcerptSha256 = "ac9270a058bb4625ec294d02ea930f7111d7bb99a705ecf7d7883755216d33c8",
                    applicabilityNotes = "Published method supports integrating cooked buckwheat, small raw apple dice, egg and wheat flour; the FoodPlan proportions and lack of baking powder change the shape and may give denser cups.",
                    coveredIngredientIds = setOf("buckwheat", "apple", "egg", "flour")
                ),
                PublishedTechniqueStep(
                    stepNumber = 3,
                    authoredStepSha256 = "3277d500c5abc182b19f12d1c73a91820f7a8269a081c93f90bf5ab11288d059",
                    publishedMethodSource = "https://ediblevermont.ediblecommunities.com/recipe/recipes-buckwheat-muffins-apples-and-walnuts",
                    sourceMethodExcerpt = "The independent Vermont muffin method apportions an apple, flour, egg and cooked buckwheat mixture into small muffin-sized moulds before baking. FoodPlan uses smaller molds and presses the no-leavening mixture gently into them instead of expecting a fluffy raised buckwheat muffin.",
                    sourceExcerptSha256 = "b779b40d3558c40621dfd7f16c76bc51bb299eb0996988a6b1942ac9269c1100",
                    applicabilityNotes = "Source describes molded baked portions, not physical confirmation of FoodPlan cup shape or ability to bind. Different batter fat and liquid content are declared; prevent overstated rise expectations.",
                    coveredIngredientIds = setOf("buckwheat", "apple", "egg", "flour")
                ),
                PublishedTechniqueStep(
                    stepNumber = 4,
                    authoredStepSha256 = "fea886753d88870bd28e96d5d800d7b8c74ab99fb8ec73378c3d742d3d5201e7",
                    publishedMethodSource = "https://ediblevermont.ediblecommunities.com/recipe/recipes-buckwheat-muffins-apples-and-walnuts",
                    sourceMethodExcerpt = "Edible Vermont bakes egg-bound buckwheat and apple muffins until cooked, rather than simply warming uncooked egg in a skillet. FoodPlan adapts the oven method and has an explicit thermometer checkpoint of at least 74 Celsius in the centre; after baking it serves plain yogurt separately, without implying the source used yogurt as a muffin ingredient.",
                    sourceExcerptSha256 = "e85a55ce623f5025e448a546b0c5e82052fa6888098b1ff8110c8e8790eedc71",
                    applicabilityNotes = "Cooking with egg is externally evidenced, but the cold yogurt garnish is FoodPlan's serving adaptation, not an ingredient reported by the Vermont publication. Core temperature is supported independently by FSIS food safety tables.",
                    coveredIngredientIds = setOf("buckwheat", "egg", "apple", "flour", "yogurt")
                )
            )
        )
    ).associateBy { it.recipeId }
}
