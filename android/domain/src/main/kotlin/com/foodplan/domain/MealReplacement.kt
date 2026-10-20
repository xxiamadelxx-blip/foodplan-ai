package com.foodplan.domain

import kotlin.math.ceil

/**
 * A replacement must pass exactly the same domain filters as initial meal selection.
 * Then all seven-day totals and shopping lines are recomputed from the changed week.
 */
object MealReplacement {
    fun alternatives(
        profile: FoodProfile, votes: Map<String, TasteRating>,
        plan: PlanningResult.Success, day: Int, meal: Int
    ): List<OfflineRecipe> {
        val original = plan.days.getOrNull(day)?.meals?.getOrNull(meal)?.recipe ?: return emptyList()
        val parsed = AllergenTerms.parse(profile.allergens)
        if (parsed.unknown.isNotEmpty()) return emptyList()
        val normalized = profile.copy(allergens=parsed.terms.joinToString(","))
        val candidates = OfflineRecipes.recipes.filter { it.type == original.type && it.id != original.id }
        val ranked = RecipeRanker.rank(
            normalized, candidates.map(OfflineRecipes::toCandidate), votes
        )
        return ranked.ranked.take(12).map { scored ->
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
        val replacement = alternatives(profile,votes,plan,day,meal).firstOrNull { it.id == newRecipeId }
            ?: return PlanningResult.Blocked(listOf("Рецепт нельзя использовать с текущими ограничениями."))
        val days = plan.days.mapIndexed { index, item ->
            if (index != day) item else {
                val updated = item.meals.toMutableList()
                updated[meal] = PlannedMeal(replacement, old.servings,
                    OfflineRecipes.nutrition(replacement) * old.servings)
                PlannedDay(index,updated)
            }
        }
        val shoppingGrams = mutableMapOf<String, Double>()
        for (entry in days.flatMap { it.meals })
            for (part in entry.recipe.ingredients)
                shoppingGrams[part.id] = (shoppingGrams[part.id] ?: 0.0) +
                    part.grams * entry.servings
        val shopping = shoppingGrams.mapNotNull { (id, quantity) ->
            val requested = ceil(quantity).toInt()
            val shortage = (requested - (pantryGrams[id] ?: 0).coerceAtLeast(0)).coerceAtLeast(0)
            if (shortage == 0) null else {
                val item = OfflineRecipes.ingredients.getValue(id)
                ShoppingNeed(item, shortage,
                    ((shortage.toLong() + item.packGrams - 1) / item.packGrams).toInt())
            }
        }.sortedBy { it.ingredient.name }
        return PlanningResult.Success(days, shopping,
            plan.calorieTargetEstimate, plan.warnings)
    }
}
