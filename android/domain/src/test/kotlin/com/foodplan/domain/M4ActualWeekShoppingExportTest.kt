package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * Source for the independent public-retailer acceptance run: an actual week
 * produced by FoodPlan's production planner, NOT a hand-picked rice-only diet.
 * Exported JSON has no identifying user data; only anonymized ingredient IDs
 * and gram quantities. Prices are joined later from live, separately audited
 * public retailer sources. Never promote a partial source into a full quote.
 */
class M4ActualWeekShoppingExportTest {
    @Test fun exportActualSevenDayPlanAndAllRequiredShoppingQuantities() {
        val profile = FoodProfile(
            sex = "Женский", age = "30", heightCm = "165",
            weightKg = "65", activity = "Умеренная",
            goal = "Поддержание", budgetRub = "356",
            city = "Санкт-Петербург", stores = setOf("Магнит"),
            diet = "Всеядный", mealsPerDay = "3",
            cookingMinutes = "55", cookingEffort = CookingEffort.INVOLVED,
            appliances = setOf("Плита", "Духовка", "Сковорода", "Кастрюля")
        )
        val result = SevenDayPlanner.plan(profile) as? PlanningResult.Success
        assertNotNull("Production planner must actually create a week", result)
        val week = result!!
        assertEquals(7, week.days.size)
        assertEquals(21, week.days.sumOf { it.meals.size })
        // These are product needs produced from 21 real planned meals.
        // Recipes remain M2 provisional; the source check is strictly M4.
        val shopping = week.shopping.filter { it.grams > 0 }
            .sortedBy { it.ingredient.id }
        assertTrue("Actual week has zero products", shopping.isNotEmpty())
        assertEquals(shopping.size, shopping.map { it.ingredient.id }.distinct().size)
        assertTrue(shopping.all {
            it.grams > 0 && it.requiredGrams >= it.grams &&
                it.ingredient.id.matches(Regex("[a-z_]+")) &&
                it.packGramsForBaseline() > 0
        })
        val destination = File("build/reports/m4-actual-week/shopping.json")
        destination.parentFile.mkdirs()
        val records = shopping.joinToString(",\n") { row ->
            """    {"ingredient_id":"${row.ingredient.id}","required_grams":${row.grams},"pantry_used_grams":${row.pantryUsed}}"""
        }
        destination.writeText(
            """{
  "schema_version":1,
  "provenance":"PRODUCTION_SEVEN_DAY_PLANNER_SHOPPING_AGGREGATION",
  "scenario":"anonymized-one-adult-three-meals-no-pantry",
  "days":7,
  "meals":${week.days.sumOf { it.meals.size }},
  "ingredient_count":${shopping.size},
  "ingredients":[
${records}
  ]
}
""", Charsets.UTF_8)
        assertTrue("Export missing", destination.isFile)
    }

    private fun ShoppingNeed.packGramsForBaseline(): Int =
        ingredient.packGrams
}
