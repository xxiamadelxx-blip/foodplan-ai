package com.foodplan.domain

/** Second batch of 20 submitted authored recipes. Only 14 source-checked methods pass.
 * Each excerpt is own-language documentation, not authenticated external HTML.
 */
object M2PublishedMethodBulkSecond20261024 {
    val reviews: Map<String, RecipeDocumentaryReview> = listOf(
        RecipeDocumentaryReview(
            recipeId = "cur_cottage_apple",
            recipeFingerprintSha256 = "31d4e2f2dbfeb93b1b010f50ade7d370f8fbfdda05e4c4702bcac71311ad33ab",
            publishedMethodSource = "https://lillieeatsandtells.com/apple-crisp-yogurt-and-cottage-cheese-bowl/",
            methodSnapshotSha256 = "12fc14d13e9631cea3674b628216dbe09caf7c35cbdc50a5d654a17680825a5d",
            methodSnapshotText = "Published apple cottage-cheese breakfast bowl starts with cottage cheese and plain yogurt stirred together and diced fresh apple added as topping. FoodPlan follows the unheated assembly and omits optional granola sweetener and cinnamon.",
            methodApplicabilityNotes = "Yogurt and cottage cheese with raw apple is same cold preparation; exact dairy type and amounts 170g curd, 140g raw apple, 60g yogurt differ from source.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK20_SECOND_20261024.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent raw edible input sum across cottage_cheese 170g; apple 140g; yogurt 60g, using exact FDC-pointer ingredient per-100g macro data. Oil provisional assumed fully retained; cooked yield, actual retention, and SKU allergy trace UNKNOWN.",
            independentlyRecalculatedRawNutrition = Nutrition(315.1, 29.646, 10.688, 27.23)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_egg_potato_hash",
            recipeFingerprintSha256 = "5ae248ba9e3a6b25b7ccd2a5c762802e431b167d05ce5dbdd71f960b2e479405",
            publishedMethodSource = "https://www.egglandsbest.com/recipes/scrambled-eggs-potatoes-onions",
            methodSnapshotSha256 = "f21bac8264aceb72a99b60efa55526d870c5b219e09a7fccc86200bf1989f631",
            methodSnapshotText = "Published potato onion scrambled eggs cook diced raw potato until soft in oil, soften onions, add beaten eggs and cook until they set. FoodPlan adds red bell pepper, briefly steams potatoes under a lid, and cooks eggs to full doneness.",
            methodApplicabilityNotes = "Core cooked potato and onion before beaten eggs matches. FoodPlan additionally uses 100g bell pepper and some water under lid, changes amounts and oil and makes no claim that published 25-minute cook is ours.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK20_SECOND_20261024.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent raw edible input sum across potato 220g; egg 115g; bell_pepper 100g; onion 60g; oil 10g, using exact FDC-pointer ingredient per-100g macro data. Oil provisional assumed fully retained; cooked yield, actual retention, and SKU allergy trace UNKNOWN.",
            independentlyRecalculatedRawNutrition = Nutrition(472.25, 20.42, 22.012, 51.216)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_cod_tomato_pan",
            recipeFingerprintSha256 = "06bee288d1605144f9996e1a3d2d7d99f9952baa19e8b576434f1ecf324a088c",
            publishedMethodSource = "https://www.mangotomato.com/2018/07/healthy-eating-zucchini-tomato-cod.html",
            methodSnapshotSha256 = "2af2f32fac7a52daa6e132e19ddeb61c259810cf918dab179e6b5640e6598238",
            methodSnapshotText = "Published zucchini tomato cod sautés zucchini and onion, adds tomatoes to make a wet vegetable mixture, then places cod fillets on the mixture and cooks covered until the fish is done. FoodPlan replaces canned tomatoes with fresh and omits extra summer squash basil and garlic.",
            methodApplicabilityNotes = "Vegetables first, cod afterwards and covered gentle simmer are applicable. FoodPlan uses raw fresh tomato and sunflower oil and must still check actual fish doneness.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK20_SECOND_20261024.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent raw edible input sum across cod 210g; tomato 210g; zucchini 210g; onion 65g; oil 12g, using exact FDC-pointer ingredient per-100g macro data. Oil provisional assumed fully retained; cooked yield, actual retention, and SKU allergy trace UNKNOWN.",
            independentlyRecalculatedRawNutrition = Nutrition(344.14955, 38.848375, 14.55875, 20.771)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_chicken_tomato_rice",
            recipeFingerprintSha256 = "70ec7402c370e085a69290294c1d68270bd1a6a971edeb99ca9b6fe0b5174b50",
            publishedMethodSource = "https://thecozyapron.com/peruvian-skillet-chicken",
            methodSnapshotSha256 = "f04bea17e79b98947650f009d540ef892c33d8441d5fe5ad9e5d4d74b39211f0",
            methodSnapshotText = "Published rustic tomato skillet chicken sears chicken breast cutlets, softens onions and fresh tomatoes into sauce, finishes chicken in the sauce and recommends cooked white rice as a separate side. FoodPlan chops breast into pieces and omits garlic cilantro and other seasonings.",
            methodApplicabilityNotes = "Same seared chicken breast cooked in onion-tomato sauce and served over separately cooked rice. Differences in cut size, seasoning and quantities are explicit.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK20_SECOND_20261024.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent raw edible input sum across chicken 180g; rice 80g; tomato 200g; onion 60g; oil 9g, using exact FDC-pointer ingredient per-100g macro data. Oil provisional assumed fully retained; cooked yield, actual retention, and SKU allergy trace UNKNOWN.",
            independentlyRecalculatedRawNutrition = Nutrition(617.3852, 48.59608, 13.7676, 77.63452)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_turkey_zucchini_pasta",
            recipeFingerprintSha256 = "a552b18bc773e3b39dea9c3a2a89dd8a46754d57cd21dd5c563a3a1330035336",
            publishedMethodSource = "https://djalalicooks.com/pasta-with-turkey-and-zucchini",
            methodSnapshotSha256 = "9ad15c2a36b3544e2b784d474d02fe1f7b0c305b94fadde36bd98572c6638ff2",
            methodSnapshotText = "Published pasta with turkey and zucchini separately cooks pasta, browns turkey in a skillet with oil, cooks zucchini and garlic, then combines turkey and vegetables with drained pasta. FoodPlan uses cubed turkey breast rather than minced turkey, without the butter Parmesan and onion.",
            methodApplicabilityNotes = "Ground turkey in source vs diced turkey breast in FoodPlan is a substantial texture and time adaptation. Only boil-pasta and cook-turkey-zucchini separately and combine is sourced; full meat doneness mandatory.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK20_SECOND_20261024.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent raw edible input sum across turkey 170g; pasta 85g; zucchini 200g; garlic 6g; oil 10g, using exact FDC-pointer ingredient per-100g macro data. Oil provisional assumed fully retained; cooked yield, actual retention, and SKU allergy trace UNKNOWN.",
            independentlyRecalculatedRawNutrition = Nutrition(640.13, 54.1232, 14.4623, 71.6195)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_lentil_mushroom_soup",
            recipeFingerprintSha256 = "017824acf7424a06ab02691aa4dcb924026a7035643160b090e7983931d6fa51",
            publishedMethodSource = "https://www.feastingathome.com/lentil-mushroom-soup/",
            methodSnapshotSha256 = "6b8a6acb38844a95981b9e73da480afdcf6cfafce99ed8fa5508e15164454cc2",
            methodSnapshotText = "Published mushroom lentil soup first sautés onion and fresh mushrooms in oil, then adds dry lentils and water or stock, simmering until tender. It allows diced carrots and potatoes as additions. FoodPlan omits fennel sage and broth and uses sunflower oil.",
            methodApplicabilityNotes = "Method appropriate for dry lentils with sautéed mushroom, onion, added root vegetables then water simmer. Dry lentils not cooked weight; FoodPlan time differs.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK20_SECOND_20261024.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent raw edible input sum across lentils 85g; mushroom 170g; potato 150g; onion 65g; carrot 90g; oil 7g, using exact FDC-pointer ingredient per-100g macro data. Oil provisional assumed fully retained; cooked yield, actual retention, and SKU allergy trace UNKNOWN.",
            independentlyRecalculatedRawNutrition = Nutrition(576.88, 30.8155, 8.895, 100.3175)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_tofu_pepper_buckwheat",
            recipeFingerprintSha256 = "0380b01a0d47e1861aa671c12f715b2bfdf02cb8d22e33ee557bb5b56c333d55",
            publishedMethodSource = "https://teamnutrition.ca/recipe-nutritionist-dietitian/buckwheat-rice-shredded-tofu",
            methodSnapshotSha256 = "25df23568fe95c4e0f6a70c6b78f402fd4633bc2c6cc75450e677e89aadaace1",
            methodSnapshotText = "Published buckwheat grain bowl boils dry buckwheat and separately cooks green vegetables, explicitly allowing bell pepper, with tofu then served over groats. FoodPlan uses cubed firm tofu instead of shredded, adds carrot and garlic and omits vinegar and herbs.",
            methodApplicabilityNotes = "Same boiled dry buckwheat with cooked bell pepper/tofu served as grain bowl, but no claims of equivalence for shredded versus cubed tofu or source's sauces.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK20_SECOND_20261024.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent raw edible input sum across tofu 220g; bell_pepper 170g; buckwheat 85g; carrot 85g; garlic 7g; oil 10g, using exact FDC-pointer ingredient per-100g macro data. Oil provisional assumed fully retained; cooked yield, actual retention, and SKU allergy trace UNKNOWN.",
            independentlyRecalculatedRawNutrition = Nutrition(643.16, 32.7954, 22.2181, 90.3455)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_pork_mushroom_pasta",
            recipeFingerprintSha256 = "212fb204c390920bbcd40e4c8d21c3c1e3756438e8e971445546546cf06b00b1",
            publishedMethodSource = "https://dairyfarmersofcanada.ca/en/canadian-goodness/recipes/pork-tenderloin-pasta-creamy-tomato-sauce",
            methodSnapshotSha256 = "986a0ff8051b84b485e0537c5811c6da1d9113d548f5bccd6b3bfb210b7bebed",
            methodSnapshotText = "Published pork tenderloin mushroom pasta cooks pork with onions and sliced mushrooms, tomato sauce and penne. FoodPlan prepares pork and mushroom/tomato sauce in a pan and adds separately boiled pasta, omitting published creamy milk, butter, flour, herbs and sweet pepper.",
            methodApplicabilityNotes = "Published creamy one-pot sauce is materially different from FoodPlan water-braised pork and fresh tomatoes; basic cooked pork mushroom tomato pasta technique is the supported correspondence. Do not transfer cooked nutrition.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK20_SECOND_20261024.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent raw edible input sum across pork 190g; mushroom 160g; pasta 85g; onion 70g; tomato 140g; oil 9g, using exact FDC-pointer ingredient per-100g macro data. Oil provisional assumed fully retained; cooked yield, actual retention, and SKU allergy trace UNKNOWN.",
            independentlyRecalculatedRawNutrition = Nutrition(859.51, 55.536, 35.0795, 80.6695)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_chicken_lentil_bowl",
            recipeFingerprintSha256 = "47139160f65479087bdc6cd5365f78eb2584d7e5ca44ecb8f21affe6e98d7179",
            publishedMethodSource = "https://cafedelites.com/wprm_print/warm-chicken-lentil-stew",
            methodSnapshotSha256 = "426445d06f960cc6b6639ceec3ccc1db9bb28e7e67e92e19713e242bc87a9e71",
            methodSnapshotText = "Published chicken lentil stew uses chopped chicken, sweet red bell pepper, onion and tomatoes in a pan with water and lentils until fully cooked. FoodPlan separately boils dry lentils and combines them later, using breast rather than thighs and omitting carrots pumpkin spinach and garlic.",
            methodApplicabilityNotes = "Source supports chicken lentil pepper tomato stew ingredients and simmering; lentils cooked separately vs inside stew and lean chicken breast are disclosed substantial differences.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK20_SECOND_20261024.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent raw edible input sum across chicken 180g; lentils 95g; bell_pepper 150g; tomato 155g; onion 60g; oil 9g, using exact FDC-pointer ingredient per-100g macro data. Oil provisional assumed fully retained; cooked yield, actual retention, and SKU allergy trace UNKNOWN.",
            independentlyRecalculatedRawNutrition = Nutrition(695.7212, 67.4525, 14.3082, 80.861)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_cottage_pear_millet_bowl",
            recipeFingerprintSha256 = "2fd821daeb4c2ff83c6e0884b163fa0e4e7b3053429cbb92f4df5d5da6355bef",
            publishedMethodSource = "https://www.diabetes.org.nz/recipes-index/millet-porridge",
            methodSnapshotSha256 = "08c3fc2814e054fbdd41388948af17380f188f0f5538942e9adc7641f45c53a7",
            methodSnapshotText = "Published millet porridge washes dry millet, simmers in water, adds milk after initial cooking, and folds in cottage cheese after removing from heat. FoodPlan substitutes sliced fresh pear for the source's raisins and omits syrup and salt.",
            methodApplicabilityNotes = "Millet-water-milk preparation and off-heat cottage cheese addition match; fresh pear in FoodPlan replaces raisins and sweet syrup, source portions differ.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK20_SECOND_20261024.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent raw edible input sum across millet 70g; cottage_cheese 130g; pear 140g; milk 140g, using exact FDC-pointer ingredient per-100g macro data. Oil provisional assumed fully retained; cooked yield, actual retention, and SKU allergy trace UNKNOWN.",
            independentlyRecalculatedRawNutrition = Nutrition(596.42582, 33.3615, 14.55182, 82.26136)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_savory_oats_egg_mushroom",
            recipeFingerprintSha256 = "5defda1cc60facff9d6952ec5bbd56c88ca6d55871416f036de1179a6d1e9306",
            publishedMethodSource = "https://www.ice.edu/blog/oats-with-sauteed-mushrooms-and-fried-eggs",
            methodSnapshotSha256 = "a56aee25ee23176339692c149a7b35c82d810434373b5ef5720b3558f7261531",
            methodSnapshotText = "Published savory oatmeal prepares hot cooked oats, sautés mushrooms and cooks eggs, then serves them together for breakfast. FoodPlan additionally sautés onion and scrambles its eggs into the mushrooms rather than serving fried eggs on top, and omits kale Parmesan and garlic.",
            methodApplicabilityNotes = "The oats-mushrooms-eggs preparation components agree, but the egg texture changes from separately fried to scrambled among mushrooms. This is an explicit variation, not an exact source duplicate.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK20_SECOND_20261024.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent raw edible input sum across oats 70g; egg 110g; mushroom 130g; onion 40g; oil 7g, using exact FDC-pointer ingredient per-100g macro data. Oil provisional assumed fully retained; cooked yield, actual retention, and SKU allergy trace UNKNOWN.",
            independentlyRecalculatedRawNutrition = Nutrition(529.08, 27.302, 23.002, 56.42)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_baked_cottage_berries",
            recipeFingerprintSha256 = "e7a658cae21cb3e94df9a0425ccad73b2deb9475e8b16b655cf3e0cda668a8a8",
            publishedMethodSource = "https://en.ipepper.org/recipe/cottage-cheese-casserole-with-blueberries/",
            methodSnapshotSha256 = "19617ee5a18a2bdcf61e3d5e939a315e66818dfb6c05113ed29bf0c509578a69",
            methodSnapshotText = "Published cottage cheese berry oven casserole mixes cottage cheese with eggs, a thickening binder and berries, transfers to a baking dish and cooks until set. FoodPlan uses frozen unsweetened strawberries instead of fresh blueberries and omits sugar and vanilla.",
            methodApplicabilityNotes = "Same curd-egg-and-fruit oven-set casserole technique; different berry, dry wheat flour and no added sugar are disclosed ingredient substitutions.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK20_SECOND_20261024.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent raw edible input sum across cottage_cheese 190g; egg 60g; flour 25g; berries 110g; oil 4g, using exact FDC-pointer ingredient per-100g macro data. Oil provisional assumed fully retained; cooked yield, actual retention, and SKU allergy trace UNKNOWN.",
            independentlyRecalculatedRawNutrition = Nutrition(480.06, 41.313, 20.022, 34.969)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_egg_mushroom_roll",
            recipeFingerprintSha256 = "29fc4d74b7f6996f66805446f1d2d003fc21084706bf37cdcfd17ba3386911c9",
            publishedMethodSource = "https://nephrocare.com/en/patients-home/your-diet/good-food-recipe-library/recipes/mushroom-omelette-rolls-with-shiitake-and-sesame",
            methodSnapshotSha256 = "b2077661d9ac36034af23907bd5afa6ecdabdabc578dded55e3218e6160e1321",
            methodSnapshotText = "Published mushroom omelette roll cooks chopped mushrooms in oil, separately beats eggs, lets the egg layer set and then rolls the omelette before serving. FoodPlan uses raw button mushrooms instead of shiitake and mixes yogurt into eggs rather than soy sauce, omitting ginger sesame and spring onion.",
            methodApplicabilityNotes = "Mushrooms cooked, egg layer set and wrapped into roll align; dairy yogurt instead of soy seasoning changes moisture and allergen profile, no use of the published renal diet as safety certification.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK20_SECOND_20261024.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent raw edible input sum across egg 130g; mushroom 110g; yogurt 45g; oil 7g, using exact FDC-pointer ingredient per-100g macro data. Oil provisional assumed fully retained; cooked yield, actual retention, and SKU allergy trace UNKNOWN.",
            independentlyRecalculatedRawNutrition = Nutrition(299.43, 21.0805, 21.7845, 6.931)
        ),
        RecipeDocumentaryReview(
            recipeId = "cur_millet_pumpkinless_pear",
            recipeFingerprintSha256 = "243153bef0e6b8ddb889fec9d421ef2394a988de66e6832fd8df69a626ca5e67",
            publishedMethodSource = "https://veganheaven.org/recipe/millet-porridge",
            methodSnapshotSha256 = "31e8facc6647cd13ebf40586665be19db823b7f63b3f49c67549141a54a5ed40",
            methodSnapshotText = "Published millet porridge rinses and simmers millet grains in water or plant milk until tender and serves with ripe pear slices. FoodPlan adds dairy milk later, finishes with yogurt and pear, omitting optional maple syrup nuts and apples.",
            methodApplicabilityNotes = "Cooking dry millet to softness and serving pear agree; unlike published vegan original, FoodPlan uses dairy milk and yogurt and is NOT a vegan dish.",
            reviewRecordReference =
                "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_BULK20_SECOND_20261024.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Independent raw edible input sum across millet 65g; pear 150g; yogurt 100g; milk 140g, using exact FDC-pointer ingredient per-100g macro data. Oil provisional assumed fully retained; cooked yield, actual retention, and SKU allergy trace UNKNOWN.",
            independentlyRecalculatedRawNutrition = Nutrition(487.59195, 15.51175, 11.12795, 80.8556)
        ),
    ).associateBy { it.recipeId }
}
