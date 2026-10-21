package com.foodplan.domain

/**
 * One meal preparation reused on consecutive days at the same meal slot.
 * Each day still records one serving per person: weekly shopping aggregation
 * counts the total ONCE per consumed serving, not once for each day cooked.
 */
object RecipeBatchPlanner {
    data class Preparation(
        val days: Int,
        val firstDay: Int,
        val effort: CookingEffort
    )

    fun apply(
        profile: FoodProfile,
        votes: Map<String, TasteRating>,
        current: PlanningResult.Success,
        startDay: Int,
        mealIndex: Int,
        recipeId: String,
        cookingDays: Int,
        effort: CookingEffort,
        pantryGrams: Map<String, Int> = emptyMap()
    ): PlanningResult {
        if (startDay !in current.days.indices || cookingDays !in 1..7 ||
            startDay + cookingDays > current.days.size ||
            current.days[startDay].meals.getOrNull(mealIndex) == null) {
            return PlanningResult.Blocked(listOf("Выбранное число дней не помещается в неделю."))
        }
        val allergy = AllergenTerms.parse(profile.allergens)
        if (allergy.unknown.isNotEmpty() || allergy.terms.isNotEmpty()) {
            return PlanningResult.Blocked(listOf(
                "Для пищевой аллергии нужен подтверждённый состав упаковок."))
        }
        val disliked = IngredientRestrictions.parse(profile.disliked)
        if (disliked.unknown.isNotEmpty()) {
            return PlanningResult.Blocked(listOf("Неизвестные исключённые продукты."))
        }
        val start = current.days[startDay].meals[mealIndex]
        val sameType = OfflineRecipes.recipes.firstOrNull {
            it.id == recipeId && it.type == start.recipe.type
        } ?: return PlanningResult.Blocked(listOf("Блюдо не найдено для выбранного приёма пищи."))
        val adjusted = profile.copy(cookingEffort = effort,
            allergens = "", disliked = disliked.ingredientIds.joinToString(","))
        val allowed = RecipeRanker.rank(adjusted,
            listOf(OfflineRecipes.toCandidate(sameType)), votes)
        if (allowed.ranked.isEmpty()) {
            return PlanningResult.Blocked(listOf(
                "Блюдо не подходит по ограничениям, кухонной технике, времени или усилиям."))
        }
        val baseServings = start.servings
        val updated = current.days.mapIndexed { dayIndex, day ->
            val meals = day.meals.mapIndexed { slotIndex, meal ->
                // A pre-existing batch is separated when any part is re-planned.
                val oldBatchTouched = meal.batchStartDay?.let { batchStart ->
                    batchStart in startDay until startDay + cookingDays ||
                        dayIndex in startDay until startDay + cookingDays &&
                        batchStart < startDay && batchStart + meal.batchDays > startDay
                } ?: false
                when {
                    slotIndex != mealIndex -> meal
                    dayIndex in startDay until startDay + cookingDays ->
                        PlannedMeal(sameType, baseServings,
                            OfflineRecipes.nutrition(sameType) * baseServings,
                            batchStartDay = startDay, batchDays = cookingDays)
                    oldBatchTouched -> meal.copy(batchStartDay = null, batchDays = 1)
                    else -> meal
                }
            }
            PlannedDay(day.index, meals)
        }
        val shopping = ShoppingAggregation.fromDays(
            updated, profile.household.toInt(), pantryGrams)
        return PlanningResult.Success(updated, shopping,
            current.calorieTargetEstimate, current.warnings)
    }

    fun alternatives(
        profile: FoodProfile, votes: Map<String, TasteRating>,
        plan: PlanningResult.Success, day: Int, slot: Int,
        effort: CookingEffort
    ): List<OfflineRecipe> = MealReplacement.alternatives(
        profile.copy(cookingEffort = effort), votes, plan, day, slot)
}
