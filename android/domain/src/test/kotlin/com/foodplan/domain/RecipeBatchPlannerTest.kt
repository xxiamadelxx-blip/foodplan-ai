package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.ceil

class RecipeBatchPlannerTest {
    private fun profile() = FoodProfile(
        sex="Мужской", age="30", heightCm="170", weightKg="72",
        budgetRub="5000", household="2", mealsPerDay="3",
        cookingMinutes="90", cookingEffort=CookingEffort.ALL_OUT,
        appliances=KitchenEquipment.choices.toSet()
    )

    @Test fun threeDayBatchReusesTheSameLunchWithoutTriplingItTwice() {
        val p = profile()
        val initial = SevenDayPlanner.plan(p) as PlanningResult.Success
        val recipeId = initial.days[1].meals[1].recipe.id
        val updated = RecipeBatchPlanner.apply(p,emptyMap(),initial,1,1,
            recipeId,3,CookingEffort.ALL_OUT) as PlanningResult.Success

        assertEquals(7,updated.days.size)
        assertEquals(21,updated.days.sumOf { it.meals.size })
        for (day in 1..3) {
            val meal=updated.days[day].meals[1]
            assertEquals(recipeId,meal.recipe.id)
            assertEquals(3,meal.batchDays)
            assertEquals(1,meal.batchStartDay)
        }
        assertNull(updated.days[0].meals[1].batchStartDay)
        assertEquals(
            ShoppingAggregation.fromDays(updated.days,2),
            updated.shopping
        )
        val chosen=updated.days[1].meals[1]
        val part=chosen.recipe.ingredients.first()
        // Exactly 3 daily servings * 2 people, not 3^2 or multiple pack charges.
        val batchGrams = (1..3).sumOf { day ->
            val prepared=updated.days[day].meals[1]
            prepared.recipe.ingredients.first { it.id==part.id }.grams *
                prepared.servings * 2
        }
        assertEquals(part.grams*chosen.servings*6,batchGrams,0.01)
    }

    @Test fun editingMiddleOfExistingBatchClearsStaleBatchLabels() {
        val p=profile()
        val initial=SevenDayPlanner.plan(p) as PlanningResult.Success
        val id=initial.days[0].meals[1].recipe.id
        val first=RecipeBatchPlanner.apply(p,emptyMap(),initial,0,1,id,3,
            CookingEffort.ALL_OUT) as PlanningResult.Success
        val edited=RecipeBatchPlanner.apply(p,emptyMap(),first,1,1,id,1,
            CookingEffort.ALL_OUT) as PlanningResult.Success
        assertNull(edited.days[0].meals[1].batchStartDay)
        assertEquals(1,edited.days[0].meals[1].batchDays)
        assertNull(edited.days[2].meals[1].batchStartDay)
        assertEquals(1,edited.days[2].meals[1].batchDays)
        assertEquals(1,edited.days[1].meals[1].batchDays)
        assertEquals(ShoppingAggregation.fromDays(edited.days,2),edited.shopping)
    }

    @Test fun overnightBatchCannotWriteIntoNonexistentDayEight() {
        val p=profile()
        val initial=SevenDayPlanner.plan(p) as PlanningResult.Success
        val result=RecipeBatchPlanner.apply(p,emptyMap(),initial,6,2,
            initial.days[6].meals[2].recipe.id,2,CookingEffort.ALL_OUT)
        assertTrue(result is PlanningResult.Blocked)
    }

    @Test fun everySingleDayCanBeRetainedWithoutChangingWeek() {
        val p=profile()
        val original=SevenDayPlanner.plan(p) as PlanningResult.Success
        val recipeId=original.days[2].meals[0].recipe.id
        val result=RecipeBatchPlanner.apply(p,emptyMap(),original,2,0,
            recipeId,1,CookingEffort.ALL_OUT) as PlanningResult.Success
        assertEquals(original.shopping,result.shopping)
        assertEquals(recipeId,result.days[2].meals[0].recipe.id)
    }

    @Test fun dietaryExclusionCannotBeBypassedWithMultiDayCooking() {
        val p=profile()
        val initial=SevenDayPlanner.plan(p) as PlanningResult.Success
        val recipeId=initial.days[0].meals[1].recipe.id
        val blocked=RecipeBatchPlanner.apply(
            p.copy(allergens="молоко"),emptyMap(),initial,0,1,
            recipeId,4,CookingEffort.ALL_OUT)
        assertTrue(blocked is PlanningResult.Blocked)
    }

    @Test fun cannotUseRecipeWithInsufficientTimeOrEquipment() {
        val p=profile()
        val initial=SevenDayPlanner.plan(p) as PlanningResult.Success
        val candidate=initial.days[0].meals[1].recipe
        val restricted=p.copy(cookingMinutes="5",appliances=emptySet())
        val blocked=RecipeBatchPlanner.apply(restricted,emptyMap(),initial,
            0,1,candidate.id,2,CookingEffort.FAST)
        assertTrue(blocked is PlanningResult.Blocked)
    }

    @Test fun changingToVeganCannotKeepIncompatibleMealsInRestOfWeek() {
        val p = profile()
        val previous = SevenDayPlanner.plan(p) as PlanningResult.Success
        val veganProfile = p.copy(diet = "Веганский")
        val veganWeek = SevenDayPlanner.plan(veganProfile) as PlanningResult.Success
        assertTrue(previous.days.flatMap { it.meals }.any {
            "Веганский" !in OfflineRecipes.diets(it.recipe)
        })
        // The selected new meal is compatible, but the other 20 may not be.
        val compatible = veganWeek.days[0].meals[1].recipe.id
        val result = RecipeBatchPlanner.apply(veganProfile, emptyMap(),
            previous, 0, 1, compatible, 1, CookingEffort.ALL_OUT)
        assertTrue(result is PlanningResult.Blocked)
        assertTrue((result as PlanningResult.Blocked).reasons.any {
            it.contains("пересоберите всю неделю")
        })
    }

    @Test fun newDislikeCannotRemainInUnchangedDaysAfterReplacement() {
        val p = profile()
        val previous = SevenDayPlanner.plan(p) as PlanningResult.Success
        val selectedBreakfast = previous.days[0].meals[0].recipe
        val breakfastIds = selectedBreakfast.ingredients.map { it.id }.toSet()
        val forbidden = previous.days[2].meals[2].recipe.ingredients
            .first { it.id !in breakfastIds }.id
        val result = RecipeBatchPlanner.apply(p.copy(disliked = forbidden),
            emptyMap(), previous, 0, 0, selectedBreakfast.id,
            1, CookingEffort.ALL_OUT)
        assertTrue(result is PlanningResult.Blocked)
        assertTrue((result as PlanningResult.Blocked).reasons.any {
            it.contains("пересоберите всю неделю")
        })
    }

    @Test fun corruptProfileAndPantryAreBlockedInsteadOfCrashing() {
        val p = profile()
        val previous = SevenDayPlanner.plan(p) as PlanningResult.Success
        val meal = previous.days[0].meals[1].recipe.id
        assertTrue(RecipeBatchPlanner.apply(p.copy(household = "unknown"),
            emptyMap(), previous, 0, 1, meal, 1,
            CookingEffort.ALL_OUT) is PlanningResult.Blocked)
        assertTrue(RecipeBatchPlanner.apply(p, emptyMap(), previous, 0, 1,
            meal, 1, CookingEffort.ALL_OUT, mapOf("rice" to -1))
            is PlanningResult.Blocked)
    }

    @Test fun pantryIsDeductedOnlyOnceFromWeeklyIngredients() {
        val p=profile()
        val initial=SevenDayPlanner.plan(p) as PlanningResult.Success
        val pantry=mapOf("rice" to 500)
        val result=RecipeBatchPlanner.apply(p,emptyMap(),initial,0,1,
            initial.days[0].meals[1].recipe.id,2,CookingEffort.ALL_OUT,pantry)
                as PlanningResult.Success
        assertEquals(ShoppingAggregation.fromDays(result.days,2,pantry),result.shopping)
    }
}
