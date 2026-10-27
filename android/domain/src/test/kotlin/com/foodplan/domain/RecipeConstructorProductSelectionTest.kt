package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

/**
 * Regression: the user may choose food already in the pantry that no longer
 * appears on the BUY list. The ingredient->recipe graph must still use it.
 * Preview must not pretend that food has a confirmed retailer price.
 */
class RecipeConstructorProductSelectionTest {
    private fun profile() = FoodProfile(
        sex = "Женский", age = "30", heightCm = "170", weightKg = "70",
        city = "Санкт-Петербург", stores = setOf("Магнит"),
        diet = "Всеядный", budgetRub = "63218", mealsPerDay = "3",
        household = "2", cookingMinutes = "120",
        cookingEffort = CookingEffort.ALL_OUT,
        appliances = setOf("Плита", "Кастрюля", "Сковорода", "Духовка")
    )

    @Test fun explicitlySelectedIngredientOutsideWeeklyBuyListStillFindsRecipes() {
        val p = profile()
        val base = RecipeConstructor.constructWeek(p) as PlanningResult.Success
        val current = base.days[0].meals[1].recipe
        val alreadyBuying = base.shopping.filter { it.requiredGrams > 0 }
            .map { it.ingredient.id }.toSet()
        val eligible = RecipeRanker.rank(
            p, OfflineRecipes.recipes.filter {
                it.type == current.type && it.id != current.id
            }.map(OfflineRecipes::toCandidate), emptyMap()
        ).ranked
        val selected = eligible.asSequence()
            .flatMap { it.recipe.safety.ingredientIds.asSequence() }
            .firstOrNull { it !in alreadyBuying }
            ?: error("Fixture must have an eligible lunch ingredient outside weekly shopping")
        val before = base.shopping.toList()
        val choices = RecipeConstructor.suggestReplacements(
            p, emptyMap(), base, 0, 1, CookingEffort.ALL_OUT,
            selectedIngredientIds = setOf(selected), maxResults = 200
        )
        assertTrue("Explicit ingredient outside shopping must not be dropped", choices.isNotEmpty())
        assertTrue(choices.all { choice ->
            choice.recipe.ingredients.any { it.id == selected }
        })
        assertTrue(choices.any { it.newIngredientIds.contains(selected) })
        assertTrue(choices.all { it.fitsBudget == null })
        assertEquals(before, base.shopping)
    }

    @Test fun excludedOrAllergenicIngredientSelectionCannotBypassSafety() {
        val p = profile()
        val base = RecipeConstructor.constructWeek(p) as PlanningResult.Success
        val blocked = RecipeConstructor.suggestReplacements(
            p.copy(disliked = "рыба"), emptyMap(), base, 0, 1,
            CookingEffort.ALL_OUT, selectedIngredientIds = setOf("salmon"),
            maxResults = 100
        )
        assertTrue(blocked.all {
            it.recipe.ingredients.none { part -> part.id == "salmon" }
        })
        assertTrue(RecipeConstructor.suggestReplacements(
            p.copy(allergens = "рыба"), emptyMap(), base, 0, 1,
            CookingEffort.ALL_OUT, selectedIngredientIds = setOf("salmon")
        ).isEmpty())
    }
}
