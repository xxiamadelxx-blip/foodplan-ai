package com.foodplan.domain

import kotlin.math.ceil

/** Global shopping aggregation: pantry deducted once per week, never per meal. */
object ShoppingAggregation {
    fun fromDays(
        days: List<PlannedDay>, household: Int,
        pantryGrams: Map<String, Int> = emptyMap()
    ): List<ShoppingNeed> {
        require(household in 1..20)
        require(pantryGrams.values.all { it >= 0 })
        val aggregated = mutableMapOf<String, Double>()
        days.flatMap { it.meals }.forEach { meal ->
            meal.recipe.ingredients.forEach { part ->
                aggregated[part.id] = (aggregated[part.id] ?: 0.0) +
                    part.grams * meal.servings * household
            }
        }
        return aggregated.map { (id, grams) ->
            val totalNeed = ceil(grams).toInt()
            val pantryUsed = minOf(totalNeed, pantryGrams[id] ?: 0)
            val toBuy = totalNeed - pantryUsed
            val item = OfflineRecipes.ingredients.getValue(id)
            ShoppingNeed(item,toBuy,((toBuy.toLong() + item.packGrams - 1)/item.packGrams).toInt(),
                totalNeed, pantryUsed)
        }.sortedBy { it.ingredient.name }
    }
}
