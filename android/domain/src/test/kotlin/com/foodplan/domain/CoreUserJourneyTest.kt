package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

/**
 * Product-level acceptance: FoodPlan must assemble a complete usable week
 * without having to scrape even ONE retail SKU. Price uncertainty must be
 * visible, never hold recipe planning hostage. No runtime AI is involved.
 */
class CoreUserJourneyTest {
    private fun profile(budget: String = "356") =
        M3ActualWeekExport.fixtureProfile().copy(
            budgetRub = budget,
            household = "2",
            cookingMinutes = "120",
            cookingEffort = CookingEffort.ALL_OUT
        )

    private fun week(p: FoodProfile) =
        SevenDayPlanner.plan(p) as? PlanningResult.Success
            ?: error("Valid adult profile must produce an offline week")

    @Test fun fullWeekRecipesAndShoppingDoNotNeedStoreConnectionOrPriceEvidence() {
        for (budget in listOf("100", "356", "63218", "100000")) {
            val p = profile(budget).copy(
                city = "Санкт-Петербург", stores = setOf("Пятёрочка"))
            val result = week(p)
            assertEquals(7, result.days.size)
            assertEquals((0..6).toList(), result.days.map { it.index })
            assertEquals(21, result.days.sumOf { it.meals.size })
            assertTrue(result.days.flatMap { it.meals }.all { meal ->
                meal.recipe.name.isNotBlank() &&
                    meal.recipe.steps.isNotEmpty() &&
                    meal.recipe.ingredients.isNotEmpty() &&
                    meal.servings > 0.0 && meal.nutrition.kcal > 0.0
            })
            assertEquals(ShoppingAggregation.fromDays(
                result.days, p.household.toInt()), result.shopping)
            assertTrue(result.shopping.any { it.grams > 0 })
            assertTrue(result.shopping.all {
                it.ingredient.id in OfflineRecipes.ingredients
            })
            val noPrices = RetailerBasketPricing.estimate(
                result.shopping, emptyList(), p.city, p.stores.single(),
                budget, 1_800_000_000_000L, indicativePriceOnly = true)
            assertNull("No known shop price means no certified budget result",
                noPrices.budgetEstimate)
            assertTrue(noPrices.missingLines > 0)
            assertEquals(0L, noPrices.knownTotalKopeks)
        }
    }

    @Test fun cookingForThreeDaysRebuildsWeeklyListWithoutLosingDays() {
        val p = profile()
        val original = week(p)
        val first = original.days[0].meals[1]
        val modified = RecipeBatchPlanner.apply(
            p, emptyMap(), original, 0, 1, first.recipe.id, 3,
            CookingEffort.ALL_OUT
        ) as PlanningResult.Success
        assertEquals(7, modified.days.size)
        assertEquals(21, modified.days.sumOf { it.meals.size })
        assertTrue((0..2).all { day ->
            modified.days[day].meals[1].recipe.id == first.recipe.id &&
                modified.days[day].meals[1].batchDays == 3 &&
                modified.days[day].meals[1].batchStartDay == 0
        })
        assertEquals(ShoppingAggregation.fromDays(modified.days, 2),
            modified.shopping)
        assertEquals(original.days[0].meals[0].recipe.id,
            modified.days[0].meals[0].recipe.id)
        assertTrue(modified.shopping.any { it.grams > 0 })
    }

    @Test fun replacingMealRecalculatesGroceriesAndNeverChangesOtherSlots() {
        val p = profile()
        val original = week(p)
        val alternate = MealReplacement.alternatives(
            p, emptyMap(), original, 0, 1).first()
        val replaced = MealReplacement.replace(
            p, emptyMap(), original, 0, 1, alternate.id
        ) as PlanningResult.Success
        assertEquals(alternate.id, replaced.days[0].meals[1].recipe.id)
        assertEquals(21, replaced.days.sumOf { it.meals.size })
        assertEquals(ShoppingAggregation.fromDays(replaced.days, 2),
            replaced.shopping)
        for (day in 0..6) for (slot in 0..2) {
            if (day != 0 || slot != 1)
                assertEquals(original.days[day].meals[slot].recipe.id,
                    replaced.days[day].meals[slot].recipe.id)
        }
    }

    @Test fun dietaryRestrictionIsEnforcedBeforePriceOrTasteOptimization() {
        val vegan = week(profile().copy(diet = "Веганский"))
        assertEquals(21, vegan.days.sumOf { it.meals.size })
        assertTrue(vegan.days.flatMap { it.meals }.all { meal ->
            "Веганский" in OfflineRecipes.diets(meal.recipe)
        })
        val forbidden = profile().copy(allergens = "рыба")
        assertTrue(SevenDayPlanner.plan(forbidden) is PlanningResult.Blocked)
    }
}
