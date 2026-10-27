package com.foodplan.domain

/**
 * YAR-6 2026-10-25 fourth batch: deterministic 25-ID source-research worklist.
 * Source candidates are NEVER documentary approvals until an immutable review
 * passes RecipeEvidenceAudit.inspect. This list does not affect M2 accepted count.
 */
object M2FourthBatchSourceTriage {
    enum class Disposition { METHOD_CANDIDATE, PARTIAL_METHOD_ONLY, SOURCE_NOT_APPLICABLE, UNSEARCHED }
    data class Candidate(
        val recipeId: String,
        val priority: Int,
        val disposition: Disposition,
        val publicationUrl: String?,
        val blocker: String
    )
    val candidates: List<Candidate> = listOf(
        Candidate("b5_buckwheat_banana_casserole", 1, Disposition.UNSEARCHED, null, "Banana buckwheat oven bake published method not yet compared"),
        Candidate("b5_baked_tomato_eggs_cottage", 2, Disposition.UNSEARCHED, null, "Baked tomato stuffed with egg and cottage cheese requires direct published method"),
        Candidate("b5_savory_millet_mushrooms", 3, Disposition.PARTIAL_METHOD_ONLY, "https://samobranka.info/recipe/pshennaya-kasha-v-gorshochke-s-gribami", "Published method boils millet and sautés mushrooms/onions with cheese, but adds raw egg and finishes in oven, whereas FoodPlan mixes on stovetop. Cooking-method difference prevents immediate admission."),
        Candidate("b4_chicken_cabbage_noodle_soup", 4, Disposition.PARTIAL_METHOD_ONLY, "https://www.maggi.ru/recipes/kurinyy-sup-s-domashney-lapshoy", "Published chicken broth and noodle soup includes carrot/onion, but does not establish the FoodPlan cabbage addition and uses homemade egg-flour noodles. Need closer source or explicit safe adaptation."),
        Candidate("b4_savory_carrot_oat_bake", 5, Disposition.SOURCE_NOT_APPLICABLE, "https://www.iamcook.ru/section/13262", "Retrieved zucchini and cheese casseroles, not oats-carrot-milk-egg-cheese oven method. Must find direct source."),
        Candidate("b4_buckwheat_apple_cups", 6, Disposition.UNSEARCHED, null, "Unverified combined cooked buckwheat, apple, egg and flour in small oven molds"),
        Candidate("b4_banana_cottage_oven_pancakes", 7, Disposition.UNSEARCHED, null, "No exact oven baked cottage-cheese banana oatmeal fritter method independently inspected"),
        Candidate("b4_rice_tomato_breakfast_cakes", 8, Disposition.UNSEARCHED, null, "No independent source for rice and tomato breakfast patties"),
        Candidate("b4_millet_pear_pudding", 9, Disposition.UNSEARCHED, null, "No independently verified dry-millet-to-pear pudding technique"),
        Candidate("b4_egg_cucumber_buckwheat_breakfast", 10, Disposition.UNSEARCHED, null, "Warm cooked buckwheat and raw cucumber/egg serving sequence needs publication"),
        Candidate("b4_rolled_cottage_omelette", 11, Disposition.UNSEARCHED, null, "Egg roll with cottage cheese filling needs separate published method"),
        Candidate("b4_oat_crepes_pear", 12, Disposition.UNSEARCHED, null, "Oat crepes and pear serving method not externally compared"),
        Candidate("b4_chicken_broccoli_oat_balls", 13, Disposition.UNSEARCHED, null, "Raw poultry oat broccoli balls require actual cooking-safety source"),
        Candidate("b4_turkey_mushroom_roulade", 14, Disposition.UNSEARCHED, null, "Raw turkey roulade with mushroom filling needs actual safe oven source"),
        Candidate("b4_turkey_millet_cabbage_soup", 15, Disposition.UNSEARCHED, null, "Turkey millet cabbage soup method not externally confirmed"),
        Candidate("b4_beef_zucchini_meatballs", 16, Disposition.UNSEARCHED, null, "Beef-zucchini meatball cooking sequence and tomato simmer need source"),
        Candidate("b4_beef_broccoli_patties", 17, Disposition.UNSEARCHED, null, "Beef broccoli patty and mashed potato two-part recipe not externally confirmed"),
        Candidate("b4_pork_oat_schnitzel", 18, Disposition.UNSEARCHED, null, "Raw pork with oat crumb coating needs verified full cooking method"),
        Candidate("b4_pork_potato_roast_lemon", 19, Disposition.UNSEARCHED, null, "Whole peeled lemon nutrient basis vs extracted juice risk; must normalize"),
        Candidate("b4_cod_stuffed_tomatoes", 20, Disposition.UNSEARCHED, null, "Fish stuffed tomato safe cooking method not yet sourced"),
        Candidate("b4_cod_broccoli_yogurt_bake", 21, Disposition.UNSEARCHED, null, "Yogurt fish oven bake needs verified temperature and order"),
        Candidate("b4_salmon_millet_fish_soup", 22, Disposition.UNSEARCHED, null, "Salmon and millet soup method not externally confirmed"),
        Candidate("b4_salmon_zucchini_oven_fritters", 23, Disposition.UNSEARCHED, null, "Salmon-zucchini patties need safe cooking source"),
        Candidate("b4_lentil_zucchini_cakes", 24, Disposition.UNSEARCHED, null, "Dry-vs-cooked lentil basis and pan patties need published method"),
        Candidate("b4_lentil_pepper_millet_stuff", 25, Disposition.UNSEARCHED, null, "Lentil millet stuffed pepper method not yet confirmed")
    )

    /** Fail closed: a search lead alone is not an accepted documentary review. */
    fun remainingAgainst(
        curated: List<OfflineRecipe> = OfflineRecipes.curatedDrafts,
        accepted: Map<String, RecipeDocumentaryReview> = M2PublishedMethodReviews.reviews
    ): List<Candidate> {
        val authored = curated.map { it.id }.toSet()
        require(candidates.size == 25 && candidates.map { it.recipeId }.toSet().size == 25)
        require(candidates.all { it.recipeId in authored })
        return candidates.filterNot { it.recipeId in accepted }
    }
}
