package com.foodplan.domain

/**
 * Audited bulk M2 intake: 17 confirmed published cooking method comparisons
 * from a 20-candidate batch; 3 unresolved candidates not counted.
 * Each source facts excerpt is original summary, not site HTML.
 * The exact frozen SHA-256 of both authored recipe and local excerpt
 * is checked by JVM tests. No physical cooking or SKU clearance implied.
 */
object M2PublishedMethodBulk20261024 {
    val reviews: Map<String, RecipeDocumentaryReview> = listOf(
        RecipeDocumentaryReview(
            recipeId = "cur_syrniki",
            recipeFingerprintSha256 = "46202fa64ec61ec605dca18d98dc575c575aa551f84767ac599baecd89da5d54",
            publishedMethodSource = "https://www.babaganosh.org/syrniki-russian-cheese-pancakes",
            methodSnapshotSha256 = "8a40f6019b4b7913547427b06e71cb9f875903946a64fd789fdfa98d5a070b9d",
            methodSnapshotText = "Babaganosh published cottage-cheese syrniki method: knead farmer cheese with eggs and flour, shape and dredge small pancakes in flour, gently fry both sides in vegetable oil, serve as a breakfast. FoodPlan uses the same essential preparation, substitutes 5 percent tvorog for farmer cheese, omits sugar and raisins, and serves plain yogurt.",
            methodApplicabilityNotes = "Fry complete small curd pancakes using specified 180 g cottage cheese, 45 g edible egg, 30 g flour, 8 g oil and 70 g yogurt. Published ingredient amounts, sugar and toppings differ; yogurt is an addition at plating, not in batter.",
            reviewRecordReference = "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK20_INTAKE_20261024.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently tabulated raw edible input contributions from pin-identified ingredients in OfflineRecipes: cottage_cheese:180g, egg:45g, flour:30g, oil:8g, yogurt:70g. Four final totals = sum(grams * per100 / 100). Values are raw/dry as explicitly stated in ingredient definitions, oil is assumed fully retained provisionally. Finished cooked mass and nutrient retention UNKNOWN. Publication nutrition totals NOT copied.",
            independentlyRecalculatedRawNutrition =
                Nutrition(504.17, 40.409, 24.267, 31.474)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_tofu_veg_rice",
            recipeFingerprintSha256 = "0c1c342e168935ea67f95e77c06cd61d6229a46de709b389d21ab7f4a7061f6f",
            publishedMethodSource = "https://www.homechef.com/meals/tofu-and-broccoli-stir-fry-standard",
            methodSnapshotSha256 = "8d56582a8fecd21bb1d5b6bfa98817a3d1a88302d484bbc803633f361bc65d7b",
            methodSnapshotText = "Home Chef published broccoli tofu grain-bowl method: pat tofu cubes dry, cut broccoli florets, cook broccoli with a little water in a covered skillet until tender and warm tofu through, then serve over already cooked grains. FoodPlan substitutes rice for the commercial mixed-grain blend, uses garlic and sunflower oil, and leaves out sweet dressing, apple, lime and peanuts.",
            methodApplicabilityNotes = "Technique basis is dried tofu, water-softened broccoli followed by tofu addition over cooked rice. Exact Home Chef sauce and protein cooking differ; FoodPlan does not claim equivalent caramelization or identical time.",
            reviewRecordReference = "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK20_INTAKE_20261024.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently tabulated raw edible input contributions from pin-identified ingredients in OfflineRecipes: tofu:200g, broccoli:180g, rice:75g, garlic:6g, oil:10g. Four final totals = sum(grams * per100 / 100). Values are raw/dry as explicitly stated in ingredient definitions, oil is assumed fully retained provisionally. Finished cooked mass and nutrient retention UNKNOWN. Publication nutrition totals NOT copied.",
            independentlyRecalculatedRawNutrition =
                Nutrition(579.60875, 28.3823375, 19.74955, 78.9488625)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_chicken_mushroom_pasta",
            recipeFingerprintSha256 = "dc51a28ac49aa5bca0e953aa3e6b5e4db7f08fa9d74938d01385232ab3dc3547",
            publishedMethodSource = "https://cravingsomethinghealthy.com/skillet-tarragon-chicken-with-noodles/",
            methodSnapshotSha256 = "1566f46ff90f80a30f4ff603fe22493800841025c15773627b48ccf857bd074d",
            methodSnapshotText = "Craving Something Healthy published One Pan Creamy Chicken Mushroom Pasta: slices chicken breast and browns until cooked, sautes mushrooms, cooks pasta and makes a creamy sauce with tangy Greek yogurt added after heating. FoodPlan cooks pasta in a separate pot, omits tomato sauce and tarragon, changes dairy to plain yogurt, and keeps the chicken fully cooked before mixing.",
            methodApplicabilityNotes = "Core safe cooking is chicken cooked through, sauteed mushrooms, cooked pasta combined with yogurt away from high heat. Publication uses a one-pot method and additional tomato/tarragon; serving size and cooking time differ.",
            reviewRecordReference = "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK20_INTAKE_20261024.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently tabulated raw edible input contributions from pin-identified ingredients in OfflineRecipes: chicken:165g, pasta:85g, mushroom:140g, yogurt:90g, oil:9g. Four final totals = sum(grams * per100 / 100). Values are raw/dry as explicitly stated in ingredient definitions, oil is assumed fully retained provisionally. Finished cooked mass and nutrient retention UNKNOWN. Publication nutrition totals NOT copied.",
            independentlyRecalculatedRawNutrition =
                Nutrition(655.5661, 55.69925, 16.8756, 72.2275)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_beef_mushroom_stew",
            recipeFingerprintSha256 = "f2b42e758bab20e952c0d83789cff7a147644e1838622a51b5c2e215079fab25",
            publishedMethodSource = "https://www.foodnetwork.com/recipes/old-fashioned-beef-stew-with-mushrooms-3361717",
            methodSnapshotSha256 = "2d2169b8a3ed90057b61c3f646a737d51b509054168a730c2605cf81695a0b4b",
            methodSnapshotText = "Food Network published beef stew with mushrooms sears chunks of beef in oil, adds aromatics, liquid and mushrooms, then simmers meat until tender. FoodPlan adapts this browned beef-and-mushroom stew and boils plain potatoes as a separate side instead of simmering them in the stew. It deliberately omits wine, broth, tomato paste and other vegetables.",
            methodApplicabilityNotes = "Searing beef before slowly cooking mushrooms/onion with moisture is a comparable household method. The published recipe contains many extra vegetables, stock and wine; FoodPlan is a smaller water-braised adaptation, cooking time only an estimate.",
            reviewRecordReference = "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK20_INTAKE_20261024.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently tabulated raw edible input contributions from pin-identified ingredients in OfflineRecipes: beef:200g, mushroom:160g, onion:80g, potato:220g, oil:10g. Four final totals = sum(grams * per100 / 100). Values are raw/dry as explicitly stated in ingredient definitions, oil is assumed fully retained provisionally. Finished cooked mass and nutrient retention UNKNOWN. Publication nutrition totals NOT copied.",
            independentlyRecalculatedRawNutrition =
                Nutrition(623, 53.774, 23.402, 51.166)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_pork_cabbage",
            recipeFingerprintSha256 = "190976998d5d3b5bb64751941d76297faee91d229a218a4b9b7527f293353d71",
            publishedMethodSource = "https://paleoglutenfreeguy.com/pork-cabbage-stew-paleo-whole30-keto",
            methodSnapshotSha256 = "4341eb0dc549e9ead0e46494fcacb430b669da9769820347691eeb9fa94b2bbf",
            methodSnapshotText = "Paleo Gluten-Free Guy published pork-and-cabbage stew: cut pork into small chunks, sear in hot oil, soften chopped onion and carrots, add cabbage and liquid, and simmer covered until the meat and cabbage are tender. FoodPlan follows the sear-then-braise sequence using 180g pork loin, without celery or garlic.",
            methodApplicabilityNotes = "Pork shoulder in publication and pork loin in FoodPlan have different cooking and tenderness behavior; the FoodPlan 55-minute claim is not experimentally verified. Only sear and covered cabbage braising method is sourced.",
            reviewRecordReference = "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK20_INTAKE_20261024.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently tabulated raw edible input contributions from pin-identified ingredients in OfflineRecipes: pork:180g, cabbage:250g, carrot:80g, onion:60g, oil:10g. Four final totals = sum(grams * per100 / 100). Values are raw/dry as explicitly stated in ingredient definitions, oil is assumed fully retained provisionally. Finished cooked mass and nutrient retention UNKNOWN. Publication nutrition totals NOT copied.",
            independentlyRecalculatedRawNutrition =
                Nutrition(580.16875, 39.339125, 33.46475, 29.227375)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_vegetable_chickpea_stew",
            recipeFingerprintSha256 = "e999ab544e342b462602e972220bfdcdd90eae7756dfcded3b6fd1ac65c17b07",
            publishedMethodSource = "https://itsnotcomplicatedrecipes.com/chickpea-and-zucchini-stew",
            methodSnapshotSha256 = "52826cf18f4c5e6f341f1c2d6542f3616e45355288df4bc04c631cdab187dbcf",
            methodSnapshotText = "It's Not Complicated Recipes published Chickpea and Vegetable Stew: saute onion in olive oil, add tomato passata and simmering liquid, then canned drained chickpeas with zucchini and aromatics, cook until vegetables are tender. FoodPlan uses fresh chopped tomatoes rather than passata, adds bell pepper and leaves out other seasonings.",
            methodApplicabilityNotes = "Core tomato-chickpea-zucchini onion saute and gentle simmer method matches. FoodPlan uses raw zucchini 180g, tomato 170g, bell pepper 110g, onion 60g, drained canned chickpeas 220g, 10g oil and no listed stock/garlic.",
            reviewRecordReference = "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK20_INTAKE_20261024.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently tabulated raw edible input contributions from pin-identified ingredients in OfflineRecipes: chickpeas:220g, zucchini:180g, tomato:170g, bell_pepper:110g, onion:60g, oil:10g. Four final totals = sum(grams * per100 / 100). Values are raw/dry as explicitly stated in ingredient definitions, oil is assumed fully retained provisionally. Finished cooked mass and nutrient retention UNKNOWN. Publication nutrition totals NOT copied.",
            independentlyRecalculatedRawNutrition =
                Nutrition(508, 20.933, 17.4, 74.014)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_zucchini_fritters",
            recipeFingerprintSha256 = "7566f16e7f72d5550ee05ef4444206193359477df3dcc8de683464caeab85045",
            publishedMethodSource = "https://www.theoriginaldish.com/2025/09/09/zucchini-fritters-with-lemon-yogurt-sauce",
            methodSnapshotSha256 = "b17097a2f725b257bd1dd87eb1f8acaf22c908e77369fa8da33cfb8803f9dd7b",
            methodSnapshotText = "The Original Dish published zucchini fritters with yogurt sauce: grate zucchini and wring out liquid, mix with beaten eggs and wheat flour, spoon patties into hot oiled skillet and fry both sides, serve with a yogurt accompaniment. FoodPlan uses a much plainer natural yogurt instead of the published lemon, garlic and herb sauce.",
            methodApplicabilityNotes = "Exact fritter method matches draining shredded zucchini, egg/flour batter and pan frying both sides. FoodPlan simplifies spice/sauce, uses 270g zucchini and raw egg 65g, 45g wheat flour, 12g sunflower oil and 90g yogurt.",
            reviewRecordReference = "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK20_INTAKE_20261024.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently tabulated raw edible input contributions from pin-identified ingredients in OfflineRecipes: zucchini:270g, egg:65g, flour:45g, oil:12g, yogurt:90g. Four final totals = sum(grams * per100 / 100). Values are raw/dry as explicitly stated in ingredient definitions, oil is assumed fully retained provisionally. Finished cooked mass and nutrient retention UNKNOWN. Publication nutrition totals NOT copied.",
            independentlyRecalculatedRawNutrition =
                Nutrition(462.73, 19.85, 23.028, 46.785)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_broccoli_egg_bake",
            recipeFingerprintSha256 = "fc7608316fa953bddf27d36741f59a95b2916e0f719ed83a01ec82ead9b56d20",
            publishedMethodSource = "https://kristineskitchenblog.com/broccoli-casserole-from-scratch",
            methodSnapshotSha256 = "0db7e166acd4c8bff4c182cf8cbe93f129a47bb6b46ed103e4f2ed47161cb179",
            methodSnapshotText = "Kristine's Kitchen published broccoli casserole: preheat and grease baking dish, blanch broccoli briefly, mix with beaten egg and grated cheddar in a creamy base, then bake to a set and browned casserole. FoodPlan substitutes a simple milk-and-egg custard for the heavier cream sauce and omits mushroom, onion and breadcrumb topping.",
            methodApplicabilityNotes = "Published method supports blanching broccoli and oven baking in egg-cheese mixture. FoodPlan uses 260g broccoli, 125g raw edible egg, 85g milk, 65g Gouda and 8g oil, without cheddar/cream sauce and extra vegetables.",
            reviewRecordReference = "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK20_INTAKE_20261024.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently tabulated raw edible input contributions from pin-identified ingredients in OfflineRecipes: broccoli:260g, egg:125g, milk:85g, cheese:65g, oil:8g. Four final totals = sum(grams * per100 / 100). Values are raw/dry as explicitly stated in ingredient definitions, oil is assumed fully retained provisionally. Finished cooked mass and nutrient retention UNKNOWN. Publication nutrition totals NOT copied.",
            independentlyRecalculatedRawNutrition =
                Nutrition(615.92, 41.0705, 41.9495, 23.06)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_chicken_cabbage_stew",
            recipeFingerprintSha256 = "3081ae5c3e740eb782838ba98f6998e0681cc1cf44c84e2ed0cebabddd6e9f92",
            publishedMethodSource = "https://momsdish.com/recipe/557/braised-cabbage-and-veggies",
            methodSnapshotSha256 = "c85b4578917be8cf198bba0c917fed2b83bdd4b9a4601f1fa125b15631e3af37",
            methodSnapshotText = "Momsdish published Braised Cabbage with Chicken: brown bite-sized chicken until opaque in a deep pot, add chopped vegetables, combine with thinly sliced cabbage, then simmer covered until chicken is cooked and cabbage tender. FoodPlan uses sunflower oil instead of butter, carrots and fresh tomatoes, omits optional mushrooms and spices.",
            methodApplicabilityNotes = "Published source supports sear chicken, add shredded cabbage and braise until done. FoodPlan has chicken breast 180g, cabbage 260g, carrot 120g, onion 65g, tomato 130g and oil 9g; added tomato is not source-equivalent to mushrooms.",
            reviewRecordReference = "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK20_INTAKE_20261024.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently tabulated raw edible input contributions from pin-identified ingredients in OfflineRecipes: chicken:180g, cabbage:260g, carrot:120g, onion:65g, tomato:130g, oil:9g. Four final totals = sum(grams * per100 / 100). Values are raw/dry as explicitly stated in ingredient definitions, oil is assumed fully retained provisionally. Finished cooked mass and nutrient retention UNKNOWN. Publication nutrition totals NOT copied.",
            independentlyRecalculatedRawNutrition =
                Nutrition(450.7327, 46.01925, 13.6857, 39.22175)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_chicken_pilaf_carrot",
            recipeFingerprintSha256 = "28eccb12e29dcca4de99ecce6f3506d22fce583aa18c55619c376dc931aac22f",
            publishedMethodSource = "https://www.olgasflavorfactory.com/autumn-favorites/chicken-rice-pilaf-plov",
            methodSnapshotSha256 = "a9c938df4bbd0893f6fb85a64d4b0d9cb6cd4f12da4f8a65f98e766331a78adb",
            methodSnapshotText = "Olga's Flavor Factory published Chicken and Rice Pilaf (Plov): rinse dry rice, brown cut chicken in hot oil, cook chopped onions and carrots, add garlic and dry rice with hot liquid, and cover until rice cooks and poultry is fully done. FoodPlan keeps this order with specified dry white rice, chicken breast, carrot, onion, garlic and sunflower oil.",
            methodApplicabilityNotes = "Published pilaf technique includes chicken, carrot, onion, garlic, dry rice and cooking water under cover. FoodPlan smaller quantities and 55-minute estimate are adaptations; source uses other chicken cuts and seasoning.",
            reviewRecordReference = "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK20_INTAKE_20261024.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently tabulated raw edible input contributions from pin-identified ingredients in OfflineRecipes: chicken:190g, rice:95g, carrot:140g, onion:85g, garlic:8g, oil:15g. Four final totals = sum(grams * per100 / 100). Values are raw/dry as explicitly stated in ingredient definitions, oil is assumed fully retained provisionally. Finished cooked mass and nutrient retention UNKNOWN. Publication nutrition totals NOT copied.",
            independentlyRecalculatedRawNutrition =
                Nutrition(777.67435, 52.2510075, 20.10735, 99.9044925)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_egg_cabbage_skilet",
            recipeFingerprintSha256 = "4322f2697eddecbc5576b453be0e320b55390041dbf6c26c2a06cc6db900cd1d",
            publishedMethodSource = "https://www.incredibleegg.org/recipe/cabbage-egg-skillet/",
            methodSnapshotSha256 = "098c1fbc22f5b2396f5d3d09f025fb7ad00422043bcd6585baa918de0f3ef06e",
            methodSnapshotText = "American Egg Board published Cabbage and Egg Skillet: in oil saute thinly sliced cabbage and shiitake mushrooms until tender, beat eggs separately, pour into the vegetable skillet, lower heat, cover and cook until set. FoodPlan includes onion and white button mushrooms and first softens the cabbage with water.",
            methodApplicabilityNotes = "FoodPlan follows vegetable saute, add whisked eggs and cook until fully set; uses raw cabbage 260g, edible egg 120g, raw button mushrooms 160g, onion 65g and sunflower oil 11g, omitting published soy and sesame dressing.",
            reviewRecordReference = "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK20_INTAKE_20261024.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently tabulated raw edible input contributions from pin-identified ingredients in OfflineRecipes: cabbage:260g, egg:120g, mushroom:160g, onion:65g, oil:11g. Four final totals = sum(grams * per100 / 100). Values are raw/dry as explicitly stated in ingredient definitions, oil is assumed fully retained provisionally. Finished cooked mass and nutrient retention UNKNOWN. Publication nutrition totals NOT copied.",
            independentlyRecalculatedRawNutrition =
                Nutrition(411.7515, 23.03825, 24.1525, 29.03675)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_potato_broccoli_soup",
            recipeFingerprintSha256 = "dde6202e44f95cdeed670f1491d7d3e03f7ba08c2d5fc6539e42c94005f66d41",
            publishedMethodSource = "https://www.skinnytaste.com/broccoli-cheese-and-potato-soup/",
            methodSnapshotSha256 = "fcd23761184a68a9aefbe4b08e7ec2ff7db78720a89edf13eb00fdb197152c08",
            methodSnapshotText = "Skinnytaste published Broccoli Cheese and Potato Soup: soften onion and other aromatic vegetables, simmer chopped potatoes in liquid, then add broccoli and milk, and melt grated cheddar until vegetables are tender and soup creamy. FoodPlan follows that order but omits celery, carrots, flour and broth, does not claim the same blended texture.",
            methodApplicabilityNotes = "The source supports boiling diced potato first and adding broccoli, milk and cheese to finish. FoodPlan uses Gouda instead of cheddar, sunflower oil instead of butter and water instead of stock; cooked yield and exact 40-min result unverified.",
            reviewRecordReference = "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK20_INTAKE_20261024.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently tabulated raw edible input contributions from pin-identified ingredients in OfflineRecipes: potato:250g, broccoli:210g, onion:65g, milk:150g, cheese:55g, oil:6g. Four final totals = sum(grams * per100 / 100). Values are raw/dry as explicitly stated in ingredient definitions, oil is assumed fully retained provisionally. Finished cooked mass and nutrient retention UNKNOWN. Publication nutrition totals NOT copied.",
            independentlyRecalculatedRawNutrition =
                Nutrition(626.04, 29.679, 27.001, 71.396)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_chickpea_pasta",
            recipeFingerprintSha256 = "c583285df254fc883275e5bc80c10013f488748d9f4fbce78a7700efeb45bf7d",
            publishedMethodSource = "https://www.oliveandmango.com/quick-and-delicious-pasta-and-chickpeas-pasta-e-ceci",
            methodSnapshotSha256 = "c2f34fddfda84751f98992737cfe3049e9edd9c76f8ddbc1763718cb01003534",
            methodSnapshotText = "Olive and Mango published Pasta e Ceci: cook garlic in olive oil with aromatics, add crushed tomatoes and drained cooked chickpeas, simmer into a tomato chickpea base and incorporate pasta until tender. FoodPlan separately boils dry wheat pasta, prepares garlic tomato chickpea sauce and tosses together, without stock, chili, herbs or Parmesan.",
            methodApplicabilityNotes = "Published tomato-garlic-chickpea pasta method is an acceptable adaptation for sauce and serving with pasta; FoodPlan separate pasta boiling differs from one-pot method, pasta is measured dry 85g, chickpeas drained 180g, tomato raw 210g.",
            reviewRecordReference = "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK20_INTAKE_20261024.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently tabulated raw edible input contributions from pin-identified ingredients in OfflineRecipes: chickpeas:180g, pasta:85g, tomato:210g, garlic:7g, oil:9g. Four final totals = sum(grams * per100 / 100). Values are raw/dry as explicitly stated in ingredient definitions, oil is assumed fully retained provisionally. Finished cooked mass and nutrient retention UNKNOWN. Publication nutrition totals NOT copied.",
            independentlyRecalculatedRawNutrition =
                Nutrition(692.92, 26.0854, 15.7161, 114.1665)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_lentil_vegetable_curry",
            recipeFingerprintSha256 = "cb4481dece66d363e35d84f33fbc3fdf96dfb9545398d5ddf5d40be597fde2a7",
            publishedMethodSource = "https://www.allrecipes.com/recipe/69027/lentils-with-tomatoes",
            methodSnapshotSha256 = "20d201568f0122ae25f4f8ef0568e901cb70ccfe16ed1c7e5560692a55563f93",
            methodSnapshotText = "Allrecipes published Lentils with Tomatoes: simmer dried lentils in water and drain; saute chopped bell pepper and onion in oil until tender; add chopped tomatoes and previously cooked lentils, simmer together until soft. FoodPlan follows exactly that sequence, adding minced garlic and a small amount of water.",
            methodApplicabilityNotes = "Dry lentil grams (100g) are not 100g cooked lentils. FoodPlan adds garlic and uses sunflower oil, with a separate 42-minute estimate; no unlisted curry paste or spices are added by the code.",
            reviewRecordReference = "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK20_INTAKE_20261024.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently tabulated raw edible input contributions from pin-identified ingredients in OfflineRecipes: lentils:100g, bell_pepper:170g, tomato:180g, onion:75g, garlic:7g, oil:10g. Four final totals = sum(grams * per100 / 100). Values are raw/dry as explicitly stated in ingredient definitions, oil is assumed fully retained provisionally. Finished cooked mass and nutrient retention UNKNOWN. Publication nutrition totals NOT copied.",
            independentlyRecalculatedRawNutrition =
                Nutrition(557.01, 29.1854, 12.0316, 89.582)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_tomato_lentil_pasta",
            recipeFingerprintSha256 = "7adf1f7eb2043b0927a96d466c297cb1005ea9db6038645296e0b0b84874c8a2",
            publishedMethodSource = "https://www.croftonsignpost.co.uk/tomato-and-lentil-pasta",
            methodSnapshotSha256 = "612d189c123413da39a5c5a17745a5ea7137c508438b56dbc13d8fdd3d0354b5",
            methodSnapshotText = "Crofton Signpost published Tomato and Lentil Pasta: uses dried red lentils, dried pasta, diced tomato and chopped onion with oil, softens onion then cooks washed dry lentils in tomato-water liquid and serves over cooked pasta. FoodPlan separately boils dry lentils and dry wheat pasta and makes tomato-onion-garlic sauce before recombining.",
            methodApplicabilityNotes = "Adaptation retains lentil and tomato base and dry pasta but differs in cooking lentils separately, adding garlic and omitting carrots/herbs/bouillon; no assumptions on finished dish weights.",
            reviewRecordReference = "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK20_INTAKE_20261024.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently tabulated raw edible input contributions from pin-identified ingredients in OfflineRecipes: pasta:80g, lentils:75g, tomato:210g, onion:65g, garlic:7g, oil:10g. Four final totals = sum(grams * per100 / 100). Values are raw/dry as explicitly stated in ingredient definitions, oil is assumed fully retained provisionally. Finished cooked mass and nutrient retention UNKNOWN. Publication nutrition totals NOT copied.",
            independentlyRecalculatedRawNutrition =
                Nutrition(723.01, 31.9309, 12.5146, 123.4625)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_oven_chicken_vegetables",
            recipeFingerprintSha256 = "2c2ce7b0f2424331e019ce4887d13b9c6a91d578d439d355ec50e7219e3599f0",
            publishedMethodSource = "https://www.watchwhatueat.com/one-pan-baked-chicken-and-vegetables",
            methodSnapshotSha256 = "dc7a78c9ef052d91da39bbde867effd8a27c0bc8aa05120d29b57f04b84cfdbc",
            methodSnapshotText = "Watch What U Eat published One Pan Baked Chicken and Vegetables: preheat oven, cut chicken and vegetables including carrots and bell peppers, coat with oil, spread in one layer and roast until chicken is safely cooked and vegetables tender. FoodPlan replaces zucchini/onion with small potato wedges and uses sunflower oil, 50-minute total.",
            methodApplicabilityNotes = "The original baking tray method establishes one-layer chicken and vegetable roasting; FoodPlan adds 250g raw potato needing full tenderness and uses chicken fillet rather than thighs. These adaptations change time and must not imply original cooktime.",
            reviewRecordReference = "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK20_INTAKE_20261024.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently tabulated raw edible input contributions from pin-identified ingredients in OfflineRecipes: chicken:200g, potato:250g, carrot:110g, bell_pepper:130g, oil:12g. Four final totals = sum(grams * per100 / 100). Values are raw/dry as explicitly stated in ingredient definitions, oil is assumed fully retained provisionally. Finished cooked mass and nutrient retention UNKNOWN. Publication nutrition totals NOT copied.",
            independentlyRecalculatedRawNutrition =
                Nutrition(589.548, 52.485, 16.747, 62.102)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_apple_cottage_cheese_bake",
            recipeFingerprintSha256 = "6c098d9fee7c498a74dfa5931928d85edfb2da362e8b9ef4cb5334e838320cc0",
            publishedMethodSource = "https://withloveandflavor.com/apple-cinnamon-cottage-cheese-bake",
            methodSnapshotSha256 = "51450ea94cb71492a8ab267f5422ebbe0ad8a6c5f79c0b7d6d786367f98367e2",
            methodSnapshotText = "With Love and Flavor published Apple Cinnamon Cottage Cheese Bake: preheat and prepare baking form, mix cottage cheese with eggs and wheat flour, fold diced apples into batter, spread mixture in pan and bake until set. FoodPlan grates raw apple, skips sugar and cinnamon, uses yogurt only as a serving accompaniment.",
            methodApplicabilityNotes = "FoodPlan uses cottage cheese 180g, apple raw 155g, egg edible part 65g, flour dry 25g and yogurt 50g; the baking sequence matches but sweetness and final texture are adapted. Published recipe's sugar calories must not be transferred.",
            reviewRecordReference = "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK20_INTAKE_20261024.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independently tabulated raw edible input contributions from pin-identified ingredients in OfflineRecipes: cottage_cheese:180g, apple:155g, egg:65g, flour:25g, yogurt:50g. Four final totals = sum(grams * per100 / 100). Values are raw/dry as explicitly stated in ingredient definitions, oil is assumed fully retained provisionally. Finished cooked mass and nutrient retention UNKNOWN. Publication nutrition totals NOT copied.",
            independentlyRecalculatedRawNutrition =
                Nutrition(512.35, 41.998, 17.7875, 48.4095)
        ),
    ).associateBy { it.recipeId }
}
