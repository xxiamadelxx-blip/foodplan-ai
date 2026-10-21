package com.foodplan.domain

import java.security.MessageDigest
import kotlin.math.abs

/**
 * One on-device saved *selection* of meals. No prices, URLs, nutrient totals,
 * retailer status or derived shopping list are stored as persistent truth.
 * All these values must be recalculated using current recipe data and profile.
 */
data class SavedMealSelection(
    val recipeId: String,
    val servings: Double,
    val batchStartDay: Int?,
    val batchDays: Int,
    val selectedEffort: CookingEffort? = null
)

data class SavedWeekSelection(
    val version: Int,
    val profileFingerprint: String,
    val mealsByDay: List<List<SavedMealSelection>>
)

object SavedWeekPlanner {
    const val VERSION = 1

    /**
     * Changing only shop/city/weekly budget should NOT erase chosen dishes.
     * Changes to food safety, personal goals, equipment, household, votes or
     * pantry invalidate a stale plan instead of silently keeping unsafe choices.
     */
    fun profileFingerprint(
        profile: FoodProfile,
        votes: Map<String, TasteRating>,
        pantryGrams: Map<String, Int>
    ): String {
        val identity = profile.copy(budgetRub = "", city = "", stores = emptySet())
        val input = buildString {
            append("foodplan-week-v1:")
            append(identity.toString())
            append("|votes:")
            votes.toSortedMap().forEach { (id, vote) ->
                append(id.length).append(':').append(id)
                append('=').append(vote.name).append(';')
            }
            append("|pantry:")
            pantryGrams.toSortedMap().forEach { (id, grams) ->
                append(id.length).append(':').append(id)
                append('=').append(grams).append(';')
            }
        }
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun capture(
        plan: PlanningResult.Success,
        profile: FoodProfile,
        votes: Map<String, TasteRating>,
        pantryGrams: Map<String, Int>
    ): SavedWeekSelection {
        require(plan.days.size == 7)
        return SavedWeekSelection(
            VERSION, profileFingerprint(profile,votes,pantryGrams),
            plan.days.map { day ->
                day.meals.map { meal ->
                    SavedMealSelection(
                        meal.recipe.id, meal.servings, meal.batchStartDay, meal.batchDays,
                        meal.selectedEffort)
                }
            }
        )
    }

    /**
     * Restore never overrides a blocked baseline, and is all-or-nothing.
     * Any unknown recipe, changed setup, corrupt batch or ineligible replacement
     * invalidates the complete snapshot without resurrecting unsafe meals.
     */
    fun restore(
        profile: FoodProfile,
        votes: Map<String, TasteRating>,
        pantryGrams: Map<String, Int>,
        base: PlanningResult,
        saved: SavedWeekSelection?
    ): PlanningResult {
        if (base !is PlanningResult.Success || saved == null) return base
        if (saved.version != VERSION ||
            saved.profileFingerprint != profileFingerprint(profile,votes,pantryGrams) ||
            saved.mealsByDay.size != base.days.size) return base

        val allergies = AllergenTerms.parse(profile.allergens)
        val dislikes = IngredientRestrictions.parse(profile.disliked)
        if (allergies.unknown.isNotEmpty() || allergies.terms.isNotEmpty() ||
            dislikes.unknown.isNotEmpty()) return base
        val normalized = profile.copy(allergens = "",
            disliked = dislikes.ingredientIds.joinToString(","))

        val recipesById = OfflineRecipes.recipes.associateBy { it.id }
        val newDays = mutableListOf<PlannedDay>()
        saved.mealsByDay.forEachIndexed { dayIndex, savedMeals ->
            val originals = base.days[dayIndex].meals
            if (savedMeals.size != originals.size) return base
            val reconstructed = mutableListOf<PlannedMeal>()
            savedMeals.forEachIndexed { mealIndex, selection ->
                val recipe = recipesById[selection.recipeId] ?: return base
                val original = originals[mealIndex]
                if (recipe.type != original.recipe.type ||
                    !selection.servings.isFinite() ||
                    selection.servings !in 0.75..2.5 ||
                    selection.batchDays !in 1..7 ||
                    selection.batchStartDay != null &&
                        (selection.batchStartDay !in 0..dayIndex ||
                            selection.batchStartDay + selection.batchDays > base.days.size ||
                            dayIndex >= selection.batchStartDay + selection.batchDays) ||
                    selection.batchStartDay == null && selection.batchDays != 1)
                    return base

                val perMealProfile = normalized.copy(
                    cookingEffort = selection.selectedEffort ?: normalized.cookingEffort)
                val eligible = RecipeRanker.rank(perMealProfile,
                    listOf(OfflineRecipes.toCandidate(recipe)),votes)
                if (eligible.ranked.size != 1) return base
                reconstructed += PlannedMeal(recipe,selection.servings,
                    OfflineRecipes.nutrition(recipe)*selection.servings,
                    selection.batchStartDay, selection.batchDays,
                    selection.selectedEffort)
            }
            newDays += PlannedDay(dayIndex,reconstructed)
        }
        // Every meal in a batch must point to the same start and have identical
        // recipe, scaling and span. No orphaned or phantom "cook for N days" UI.
        saved.mealsByDay.forEachIndexed { day, meals ->
            meals.forEachIndexed { slot, chosen ->
                val start = chosen.batchStartDay ?: return@forEachIndexed
                for (otherDay in start until start + chosen.batchDays) {
                    val other = saved.mealsByDay[otherDay][slot]
                    if (other.recipeId != chosen.recipeId ||
                        other.batchStartDay != start || other.batchDays != chosen.batchDays ||
                        abs(other.servings - chosen.servings) > 1e-9 ||
                        other.selectedEffort != chosen.selectedEffort)
                        return base
                }
            }
        }
        val shopping = ShoppingAggregation.fromDays(newDays,
            profile.household.toInt(),pantryGrams)
        return PlanningResult.Success(newDays,shopping,
            base.calorieTargetEstimate,base.warnings)
    }
}
