package com.foodplan.domain

/**
 * YAR-6 bulk documentary batch #3, independently sourced on 25 Oct 2026.
 * These excerpts are faithful own-language method comparisons, not site HTML.
 * Documentary review != cooking/lab proof, cooked yield or SKU safety.
 */
object M2PublishedMethodBulkThird20261025 {
    val reviews: Map<String, RecipeDocumentaryReview> = listOf(
        RecipeDocumentaryReview(
            recipeId = "cur_turkey_buckwheat",
            recipeFingerprintSha256 = "80ec410b79e44a4618d203562f361aab93596cee5a54dd177cba213ccb4003f1",
            publishedMethodSource = "https://www.mrcook.app/en/recipes/clpnyad4h001q08ifb12hgwr5",
            methodSnapshotSha256 = "8cd22ff6b17039ee9664fd7b6f3e306431681d3bb1a97496313fb0f86bef79ba",
            methodSnapshotText = "Captured 2026-10-25. Independent published Turkey Buckwheat method (Mr. Cook, Nikolai Redkozubov) sautés turkey with onion, grated carrot and other vegetables in oil while dry rinsed buckwheat cooks separately in water; the two are combined and heated. FoodPlan keeps the independently boiled grain and turkey-carrot-onion pan stew, omits extra vegetables, garlic and seasonings, and uses sunflower oil.",
            methodApplicabilityNotes = "Turkey breast chunks and carrot/onion with separately boiled dry buckwheat correspond. The source browns vegetables with turkey then folds buckwheat in the same pan; FoodPlan serves grain alongside and adds water to stew. Omitted pepper/zucchini/herbs, weights and 36-min total not taken as source guarantees.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK20_THIRD_20261025.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently tabulated raw edible input: turkey=180g, buckwheat=85g, carrot=140g, onion=50g, oil=8g. Multiply pinned OfflineRecipes FoodData Central per-100g nutrient constants for turkey, buckwheat, carrot, onion, oil by each raw ingredient weight /100; sum kcal/P/F/C. Dry grains stay dry, meat and vegetables raw, no calculated cooked mass or retention. Actual oil absorption and SKU allergens UNKNOWN.",
            independentlyRecalculatedRawNutrition =
                Nutrition(647.42, 54.4105, 13.3535, 82.0415)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_baked_potato_chickpeas",
            recipeFingerprintSha256 = "4930f844e2237b9a42382a8762f836a22d0fe7786ca1150233ef33b32981f28f",
            publishedMethodSource = "https://www.thedoctorskitchen.com/recipes/roast-potato-red-onion-and-chickpea-traybake",
            methodSnapshotSha256 = "b88005c19ba45fa2a2a29477a3d35c9a82ac98a9038399f494ca609de9841d54",
            methodSnapshotText = "Captured 2026-10-25. The Doctor's Kitchen Roast Potato Red Onion and Chickpea Traybake puts chopped potato, chickpeas and red onion in an oiled baking tray, roasts until potatoes soften, and incorporates fresh tomatoes at the end. FoodPlan pre-roasts sliced potato at 200 C and adds already-cooked drained canned chickpeas, tomato and onion later for a final roast; its staged sequence is a declared adaptation.",
            methodApplicabilityNotes = "Same oven-roasted potato with onion, chickpeas, oil and tomatoes; FoodPlan starts potatoes first before adding ready-to-eat drained chickpeas and fresh tomatoes and continues roasting, while the source starts chickpeas with potato and adds tomato after. No equivalence of times or roasted oil uptake is assumed.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK20_THIRD_20261025.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently tabulated raw edible input: potato=330g, chickpeas=180g, tomato=130g, onion=60g, oil=11g. Multiply pinned OfflineRecipes FoodData Central per-100g nutrient constants for potato, chickpeas, tomato, onion, oil by each raw ingredient weight /100; sum kcal/P/F/C. Dry grains stay dry, meat and vegetables raw, no calculated cooked mass or retention. Actual oil absorption and SKU allergens UNKNOWN.",
            independentlyRecalculatedRawNutrition =
                Nutrition(648.94, 21.259, 16.603, 108.932)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_baked_apple_oats",
            recipeFingerprintSha256 = "31db426d327d1a19e3d0a17c5c661cd3e96360d3d69af113ee0dbd209239f72d",
            publishedMethodSource = "https://reformjudaism.org/reform-jewish-life/food-recipes/baked-stuffed-apples",
            methodSnapshotSha256 = "420ca4e29245f236118f0aa2520e0a2243b7dc835485eed5312c0ea104a05e8a",
            methodSnapshotText = "Captured 2026-10-25. Chef Katie Simmons's Baked Stuffed Apples on ReformJudaism.org cores whole apples, stuffs the cavity with rolled oats plus optional dried fruit, surrounds apples with water in a covered baking dish and bakes until softened. FoodPlan pre-moistens dry oats with some water, omits dried fruit and nuts, roasts apples until soft at 180 C, then adds plain yogurt as a separate serving accompaniment.",
            methodApplicabilityNotes = "The core baked-apple-with-moist-oats method is applicable; FoodPlan uses water to moisten filling before baking rather than water in the covered pan, omits spices and raisins, and adds yogurt separately. Source's much longer covered baking schedule and FoodPlan's 36-minute estimate are different: time is NOT externally validated.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK20_THIRD_20261025.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently tabulated raw edible input: apple=230g, oats=50g, yogurt=130g. Multiply pinned OfflineRecipes FoodData Central per-100g nutrient constants for apple, oats, yogurt by each raw ingredient weight /100; sum kcal/P/F/C. Dry grains stay dry, meat and vegetables raw, no calculated cooked mass or retention. Actual oil absorption and SKU allergens UNKNOWN.",
            independentlyRecalculatedRawNutrition =
                Nutrition(388.4, 11.684, 7.876, 71.671)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_turkey_lentil_stew",
            recipeFingerprintSha256 = "e156d988f982d27129f56f030f74f31f91547d3cab46210788e7fb772e4c94ae",
            publishedMethodSource = "https://my-pcos-story.org/blogs/recipes/warming-turkey-lentil-veggie-stew",
            methodSnapshotSha256 = "c62a72defb5023909a954ba67317da7936a37e4a79e02a1da55a1c0bea151d18",
            methodSnapshotText = "Captured 2026-10-25. The published Warming Turkey Lentil Veggie Stew sautés diced onion and garlic, browns ground turkey, adds washed dry lentils, chopped tomatoes and zucchini with stock, brings to a boil then simmers until lentils and vegetables soften. FoodPlan uses cubed raw turkey breast, adds carrot, omits other seasonings and greens, and cooks everything covered until lentils and poultry are fully cooked.",
            methodApplicabilityNotes = "Published method supports turkey-lentil-tomato-zucchini one-pot oil saute and simmer. Ground turkey versus diced raw breast, extra carrot, the omit of stock/spinach/pepper and exact 57-minute total are clear modifications. Ground turkey nutrition and publisher cooked macro values are not transferred; raw turkey breast FDC is used.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK20_THIRD_20261025.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently tabulated raw edible input: turkey=170g, lentils=75g, zucchini=170g, tomato=160g, carrot=100g, onion=65g, oil=10g. Multiply pinned OfflineRecipes FoodData Central per-100g nutrient constants for turkey, lentils, zucchini, tomato, carrot, onion, oil by each raw ingredient weight /100; sum kcal/P/F/C. Dry grains stay dry, meat and vegetables raw, no calculated cooked mass or retention. Actual oil absorption and SKU allergens UNKNOWN.",
            independentlyRecalculatedRawNutrition =
                Nutrition(670.9, 63.8045, 14.48, 74.9125)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_beef_cabbage_rolls",
            recipeFingerprintSha256 = "01f0060e0327022567c7f4db42c304644a4b9127671b0b0efce874580fe76225",
            publishedMethodSource = "https://ryboedov.ru/recipes/govyadina/golubtsy-klassicheskie/",
            methodSnapshotSha256 = "d2a12a23a14c40166abf380a6e05430cef28ae21838f44818ed2f219a6fb5578",
            methodSnapshotText = "Captured 2026-10-25. The published classic cabbage rolls recipe blanches cabbage leaves, boils rice partway, mixes beef mince with diced onion and rice, wraps firm rolls, prepares onion/carrot tomato sauce and simmers rolls in a lidded pot for about forty to sixty minutes. FoodPlan does the same sequential operations with very finely chopped lean beef rather than commercial minced meat and fresh chopped tomatoes instead of paste.",
            methodApplicabilityNotes = "Explicit match of blanched leaves, parboiled rice, raw beef/onion filling, hand-rolled envelopes, onion-carrot-tomato sauce and covered-pot simmer. FoodPlan uses very finely cut beef rather than minced, and fresh tomatoes not concentrated paste. Both filling and sauce draw on the one onion weight without independently measured split; complete internal meat cooking is essential.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK20_THIRD_20261025.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently tabulated raw edible input: beef=200g, cabbage=300g, rice=75g, onion=90g, carrot=110g, tomato=180g, oil=13g. Multiply pinned OfflineRecipes FoodData Central per-100g nutrient constants for beef, cabbage, rice, onion, carrot, tomato, oil by each raw ingredient weight /100; sum kcal/P/F/C. Dry grains stay dry, meat and vegetables raw, no calculated cooked mass or retention. Actual oil absorption and SKU allergens UNKNOWN.",
            independentlyRecalculatedRawNutrition =
                Nutrition(889.73125, 55.1998875, 27.75125, 105.3321125)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_chicken_potato_gratin",
            recipeFingerprintSha256 = "a135971a88790b4c7d79c1a90be1ac044197d18861a40d5aab908c33e0ab6976",
            publishedMethodSource = "https://www.bda.uk.com/resource/chicken-leek-and-mushroom-bake.html",
            methodSnapshotSha256 = "50217b719d75774396fa058565ac417bdf65a9175a698a603ef06883cf32a1a9",
            methodSnapshotText = "Captured 2026-10-25. British Dietetic Association Chicken Leek and Mushroom Bake pre-cooks diced chicken, softens mushrooms, mixes flour and milk into a creamy sauce, slices potatoes thinly, layers chicken mushroom sauce and potatoes in a baking dish, and bakes until the potato cooks. FoodPlan substitutes onion for leek, adds cheese as a topping and maintains the chicken cooked-first, mushroom, flour-milk sauce and potato bake sequence.",
            methodApplicabilityNotes = "Independent authoritative preparation shares diced cooked poultry, mushrooms, flour-thickened milk sauce, thin potato layers and oven finish. FoodPlan uses onion instead of leek, cheese topping and 190 C/95 minutes rather than source's 180 C schedule. Exact finished yield or safe poultry temperature not established by publisher equivalence.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK20_THIRD_20261025.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently tabulated raw edible input: chicken=195g, potato=300g, mushroom=190g, onion=70g, milk=110g, cheese=75g, flour=20g, oil=12g. Multiply pinned OfflineRecipes FoodData Central per-100g nutrient constants for chicken, potato, mushroom, onion, milk, cheese, flour, oil by each raw ingredient weight /100; sum kcal/P/F/C. Dry grains stay dry, meat and vegetables raw, no calculated cooked mass or retention. Actual oil absorption and SKU allergens UNKNOWN.",
            independentlyRecalculatedRawNutrition =
                Nutrition(1020.1463, 81.28475, 41.2743, 87.045)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_cabbage_mushroom_dumplings",
            recipeFingerprintSha256 = "c93eb7838c293c0ffc0f65bf67d2d0e4d3c7d8da67c92b8ff5d30821f0a515dc",
            publishedMethodSource = "https://www.tesco.com/recipes/en-GB/recipe/mushroom-and-cabbage-pierogi",
            methodSnapshotSha256 = "a664fb188d1bf1eaf54022f9cccd19f0977d91cb69d53bfcbf4e7bcb9949269f",
            methodSnapshotText = "Captured 2026-10-25. Tesco Mushroom and Cabbage Pierogi teaches wheat flour and water dumpling dough, cooking a fine-chopped onion/cabbage/mushroom filling until soft, rolling dough thin and enclosing small portions with sealed edges, then boiling the stuffed dumplings until done. FoodPlan uses fresh cabbage rather than the publisher's sauerkraut and no egg or butter, stewing the fresh cabbage and mushrooms before sealing and boiling.",
            methodApplicabilityNotes = "Method match is knead-and-rest wheat dough, cook mushrooms/cabbage/onion filling, seal round dumplings, boil. Source uses sauerkraut instead of FoodPlan's raw white cabbage, an egg-containing dough and optional butter finish; FoodPlan omits eggs and butter and uses water-only dough. Taste, elasticity and timing may differ. Not a gluten-allergen clearance.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK20_THIRD_20261025.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently tabulated raw edible input: flour=140g, mushroom=195g, cabbage=180g, onion=80g, oil=13g. Multiply pinned OfflineRecipes FoodData Central per-100g nutrient constants for flour, mushroom, cabbage, onion, oil by each raw ingredient weight /100; sum kcal/P/F/C. Dry grains stay dry, meat and vegetables raw, no calculated cooked mass or retention. Actual oil absorption and SKU allergens UNKNOWN.",
            independentlyRecalculatedRawNutrition =
                Nutrition(753.1895, 25.43575, 16.5325, 129.75975)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_rice_milk_banana",
            recipeFingerprintSha256 = "d9b89beace47d5ee9afe4c76abd76e86f01cb087b1b9faff6154d10d14929984",
            publishedMethodSource = "https://food.ru/recipes/40986-kasha-risovaja-molochnaja",
            methodSnapshotSha256 = "6020ce1f30a48431b4cc519f831d6f4f1765f19242ca9309bf0cd1edfb8805fa",
            methodSnapshotText = "Captured 2026-10-25. Food.ru published milk rice porridge rinses raw rice, simmers it first in water, pours in milk and cooks until soft, then adds fresh cut banana as a sweet garnish. FoodPlan rinses dry rice, cooks it in milk plus a little water until creamy, removes from heat and tops with banana. Butter and sugar from the published version are omitted, and the original rice cooking is water-first.",
            methodApplicabilityNotes = "Applicable boiled dry rice with milk and banana before serving, but FoodPlan uses milk and water concurrently instead of sequential water then milk, omits source butter and sugar, and has different dry rice/milk amounts. No transfer of published finished nutrition or kitchen timing.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK20_THIRD_20261025.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently tabulated raw edible input: rice=65g, milk=240g, banana=115g. Multiply pinned OfflineRecipes FoodData Central per-100g nutrient constants for rice, milk, banana by each raw ingredient weight /100; sum kcal/P/F/C. Dry grains stay dry, meat and vegetables raw, no calculated cooked mass or retention. Actual oil absorption and SKU allergens UNKNOWN.",
            independentlyRecalculatedRawNutrition =
                Nutrition(481.90825, 13.3887525, 8.89895, 89.9415475)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_broccoli_pepper_eggs",
            recipeFingerprintSha256 = "bfc2af2037e62b9627fd531fddc87d2c4f106b8e1ccfd2afbdc8b06f970acd77",
            publishedMethodSource = "https://kamelenta.ru/recipe/52-Yaichnica-v-perce",
            methodSnapshotSha256 = "0d30b615c14d95a0ff872d40ae4696dde3031d1e61f398ade2f289301be2e222",
            methodSnapshotText = "Captured 2026-10-25. Kamelenta published eggs with sweet pepper and broccoli warms pepper pieces on an oiled skillet, softens broccoli, places whole eggs among the vegetables and fries gently under a lid if the yolk must set. FoodPlan uses cut pepper strips and small broccoli florets, moistens with some water, makes two wells among softened vegetables, adds eggs and cooks covered until set.",
            methodApplicabilityNotes = "Published technique confirms softened pepper and broccoli before whole eggs and covered finishing. The source uses large pepper rings framing eggs and may blanch raw broccoli first; FoodPlan instead uses strips, a splash of water and a vegetable bed. Cooking until complete yolk and white set is explicitly part of FoodPlan, not a claim of 21-minute source equivalence.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK20_THIRD_20261025.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently tabulated raw edible input: egg=125g, broccoli=135g, bell_pepper=95g, oil=7g. Multiply pinned OfflineRecipes FoodData Central per-100g nutrient constants for egg, broccoli, bell_pepper, oil by each raw ingredient weight /100; sum kcal/P/F/C. Dry grains stay dry, meat and vegetables raw, no calculated cooked mass or retention. Actual oil absorption and SKU allergens UNKNOWN.",
            independentlyRecalculatedRawNutrition =
                Nutrition(308.53, 19.91, 20.194, 15.42)
        ),
    ).associateBy { it.recipeId }
}
