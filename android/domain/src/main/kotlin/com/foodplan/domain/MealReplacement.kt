package com.foodplan.domain

import kotlin.math.ceil

/**
 * A replacement must pass exactly the same domain filters as initial meal selection.
 * Then all seven-day totals and shopping lines are recomputed from the changed week.
 */
object MealReplacement {
    fun alternatives(
        profile: FoodProfile, votes: Map<String, TasteRating>,
        plan: PlanningResult.Success, day: Int, meal: Int,
        maxResults: Int = 12
    ): List<OfflineRecipe> {
        require(maxResults in 1..200)
        val original = plan.days.getOrNull(day)?.meals?.getOrNull(meal)?.recipe ?: return emptyList()
        val parsed = AllergenTerms.parse(profile.allergens)
        if (parsed.unknown.isNotEmpty()) return emptyList()
        val disliked = IngredientRestrictions.parse(profile.disliked)
        if (disliked.unknown.isNotEmpty()) return emptyList()
        val normalized = profile.copy(
            allergens=parsed.terms.joinToString(","),
            disliked=disliked.ingredientIds.joinToString(",")
        )
        val candidates = OfflineRecipes.recipes.filter { it.type == original.type && it.id != original.id }
        val ranked = RecipeRanker.rank(
            normalized, candidates.map(OfflineRecipes::toCandidate), votes
        )
        return ranked.ranked.take(maxResults).map { scored ->
            candidates.first { it.id == scored.recipe.id }
        }
    }

    fun replace(
        profile: FoodProfile, votes: Map<String, TasteRating>,
        plan: PlanningResult.Success, day: Int, meal: Int,
        newRecipeId: String, pantryGrams: Map<String, Int> = emptyMap()
    ): PlanningResult {
        val old = plan.days.getOrNull(day)?.meals?.getOrNull(meal)
            ?: return PlanningResult.Blocked(listOf("Такого приёма пищи нет."))
        val replacement = alternatives(profile,votes,plan,day,meal,maxResults=200).firstOrNull { it.id == newRecipeId }
            ?: return PlanningResult.Blocked(listOf("Рецепт нельзя использовать с текущими ограничениями."))
        val days = plan.days.mapIndexed { index, item ->
            if (index != day) item else {
                val updated = item.meals.toMutableList()
                updated[meal] = PlannedMeal(replacement, old.servings,
                    OfflineRecipes.nutrition(replacement) * old.servings)
                PlannedDay(index,updated)
            }
        }
        val shopping = ShoppingAggregation.fromDays(days,profile.household.toInt(),pantryGrams)
        return PlanningResult.Success(days, shopping,
            plan.calorieTargetEstimate, plan.warnings)
    }
}
