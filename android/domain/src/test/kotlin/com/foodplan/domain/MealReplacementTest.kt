package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class MealReplacementTest {
    private fun profile() = FoodProfile(
        sex="Мужской", age="31", heightCm="175", weightKg="75",
        goal="Поддержание", budgetRub="7000", cookingMinutes="55",
        cookingEffort=CookingEffort.INVOLVED,
        appliances=setOf("Плита","Кастрюля","Сковорода","Духовка")
    )
    @Test fun swapChangesSevenDayIngredientsAndSummary() {
        val p = profile()
        val initial = SevenDayPlanner.plan(p) as PlanningResult.Success
        val before = initial.days[0].meals[0]
        val candidates = MealReplacement.alternatives(p,emptyMap(),initial,0,0)
        assertTrue(candidates.isNotEmpty())
        val chosen = candidates.first { it.ingredients != before.recipe.ingredients }
        val result = MealReplacement.replace(p,emptyMap(),initial,0,0,chosen.id)
        assertTrue(result is PlanningResult.Success)
        val after = result as PlanningResult.Success
        assertEquals(7,after.days.size)
        assertEquals(21,after.days.sumOf { it.meals.size })
        assertEquals(chosen.id,after.days[0].meals[0].recipe.id)
        assertNotEquals(before.recipe.id,after.days[0].meals[0].recipe.id)
        assertTrue(after.shopping.isNotEmpty())
        assertTrue(after.shopping.all { it.purchaseGrams >= it.grams })
    }
    @Test fun allergyCannotBeBypassedByManualReplacement() {
        val initial = SevenDayPlanner.plan(profile()) as PlanningResult.Success
        val p = profile().copy(allergens="рыба")
        assertTrue(SevenDayPlanner.plan(p) is PlanningResult.Blocked)
        assertTrue(MealReplacement.alternatives(p,emptyMap(),initial,0,1).isEmpty())
        val blocked = MealReplacement.replace(p,emptyMap(),initial,0,1,
            "main_cod_rice_broccoli_pan")
        assertTrue(blocked is PlanningResult.Blocked)
    }
    @Test fun invalidSlotDoesNotProduceFabricatedPlan() {
        val p=profile()
        val initial=SevenDayPlanner.plan(p) as PlanningResult.Success
        assertTrue(MealReplacement.replace(p,emptyMap(),initial,9,2,"invalid")
            is PlanningResult.Blocked)
    }
}
