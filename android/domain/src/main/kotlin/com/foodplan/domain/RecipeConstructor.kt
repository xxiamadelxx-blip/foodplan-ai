package com.foodplan.domain

/**
 * One offline entry point for the two normal user actions: assembling the
 * weekly menu, and previewing ingredient-first replacement recipes.
 *
 * This does not invent new culinary methods, prices or allergen evidence.
 * It reuses the same recipe eligibility, batching and shopping rules as
 * the existing production UI.
 */
data class RecipeReplacementPreview(
    val recipe: OfflineRecipe,
    /** Already represented by a positive weekly ingredient demand. */
    val reusedIngredientIds: Set<String>,
    /** Completely new types of goods added to this week's shopping. */
    val newIngredientIds: Set<String>,
    /** Existing shopping types whose quantities rise after the substitution. */
    val increasedPurchaseGrams: Map<String, Int>,
    val removedIngredientIds: Set<String>,
    /** Exact recalculation of all seven days, not a standalone recipe estimate. */
    val previewShopping: List<ShoppingNeed>,
    /** Unknown without complete source-matched offers for every purchased line. */
    val fitsBudget: Boolean?,
    val tasteScore: Int
)

object RecipeConstructor {
    fun constructWeek(
        profile: FoodProfile,
        votes: Map<String, TasteRating> = emptyMap(),
        pantryGrams: Map<String, Int> = emptyMap(),
        offers: List<RetailerOffer> = emptyList(),
        nowEpochMs: Long = 0L
    ): PlanningResult {
        val built = SevenDayPlanner.plan(profile, votes, pantryGrams)
        if (built !is PlanningResult.Success || offers.isEmpty()) return built
        val store = profile.stores.singleOrNull() ?: return built
        return BudgetWeekRefiner.refine(
            built, profile, votes, offers, store, nowEpochMs, pantryGrams
        ).plan
    }

    /**
     * Hard compatibility first; calculate a new whole-week shopping list for
     * every permitted candidate; prefer reusing existing goods without
     * assuming that already-listed grams are free or unlimited.
     */
    fun suggestReplacements(
        profile: FoodProfile,
        votes: Map<String, TasteRating>,
        current: PlanningResult.Success,
        day: Int,
        meal: Int,
        effort: CookingEffort,
        cookingDays: Int = 1,
        pantryGrams: Map<String, Int> = emptyMap(),
        offers: List<RetailerOffer> = emptyList(),
        nowEpochMs: Long = 0L,
        maxResults: Int = 12
    ): List<RecipeReplacementPreview> {
        require(maxResults > 0)
        if (day !in current.days.indices ||
            current.days[day].meals.getOrNull(meal) == null ||
            cookingDays !in 1..(current.days.size - day) ||
            pantryGrams.values.any { it < 0 }) return emptyList()

        val allergies = AllergenTerms.parse(profile.allergens)
        if (allergies.unknown.isNotEmpty() || allergies.terms.isNotEmpty())
            return emptyList()
        val dislikes = IngredientRestrictions.parse(profile.disliked)
        if (dislikes.unknown.isNotEmpty()) return emptyList()
        val normalized = profile.copy(
            allergens = "",
            disliked = dislikes.ingredientIds.joinToString(","),
            cookingEffort = effort
        )
        val original = current.days[day].meals[meal].recipe
        val candidatePool = OfflineRecipes.recipes.filter { recipe ->
            recipe.type == original.type && recipe.id != original.id
        }
        val weighted = RecipeRanker.rank(
            normalized, candidatePool.map(OfflineRecipes::toCandidate), votes
        ).ranked.associate { it.recipe.id to it.score }
        val before = current.shopping.filter { it.requiredGrams > 0 }
            .associateBy { it.ingredient.id }
        val oldTypes = before.keys
        val store = profile.stores.singleOrNull()
        val budget = WeeklyBudget.parse(profile.budgetRub)
        val options = ArrayList<RecipeReplacementPreview>()

        for (recipe in candidatePool) {
            val taste = weighted[recipe.id] ?: continue
            // Use the exact public replacement procedure, including potential
            // batch overlaps and the one-time weekly pantry deduction.
            val updated = RecipeBatchPlanner.apply(
                profile, votes, current, day, meal, recipe.id,
                cookingDays, effort, pantryGrams
            ) as? PlanningResult.Success ?: continue
            val shopping = updated.shopping
            val after = shopping.filter { it.requiredGrams > 0 }
                .associateBy { it.ingredient.id }
            val newGoods = after.keys - oldTypes
            val reusedGoods = recipe.ingredients.map { it.id }.toSet().intersect(oldTypes)
            val increased = after.mapNotNull { (id, need) ->
                val baseline = before[id]?.grams ?: return@mapNotNull null
                val delta = need.grams - baseline
                if (delta > 0) id to delta else null
            }.toMap()
            val removed = oldTypes - after.keys
            val fits = if (offers.isNotEmpty() && store != null && budget != null) {
                BudgetBasketOptimizer.choose(
                    shopping, offers, profile.city, store, budget.toLong(), nowEpochMs
                ).fitsBudget
            } else null
            options.add(RecipeReplacementPreview(
                recipe, reusedGoods, newGoods, increased, removed, shopping,
                fits, taste
            ))
        }
        return options.sortedWith(
            compareBy<RecipeReplacementPreview> { it.newIngredientIds.size }
                .thenBy { it.increasedPurchaseGrams.size }
                .thenByDescending { it.reusedIngredientIds.size }
                .thenByDescending { it.tasteScore }
                .thenBy { it.recipe.id }
        ).take(maxResults)
    }
}
