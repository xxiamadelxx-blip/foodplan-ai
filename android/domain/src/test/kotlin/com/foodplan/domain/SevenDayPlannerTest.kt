package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class SevenDayPlannerTest {
    private fun profile() = FoodProfile(
        sex="Женский", age="30", heightCm="165", weightKg="65", activity="Умеренная",
        goal="Поддержание", budgetRub="5000", city="Санкт-Петербург",
        stores=setOf("Пятёрочка"), diet="Всеядный", mealsPerDay="3", cookingMinutes="55",
        cookingEffort=CookingEffort.INVOLVED,
        appliances=setOf("Плита","Духовка","Сковорода","Кастрюля")
    )
    @Test fun seedCatalogHas200UniqueCompletePrototypes() {
        assertEquals(200, OfflineRecipes.recipes.size)
        assertEquals(200, OfflineRecipes.recipes.map { it.id }.toSet().size)
        assertTrue(OfflineRecipes.recipes.none { it.nutritionVerified })
        assertTrue(OfflineRecipes.recipes.all { OfflineRecipes.nutrition(it).kcal > 0.0 })
    }
    @Test fun endToEndSevenDaysAndShoppingPacks() {
        val result = SevenDayPlanner.plan(profile(), mapOf("chicken_katsu" to TasteRating.LIKE))
        assertTrue(result is PlanningResult.Success)
        val success = result as PlanningResult.Success
        assertEquals(7, success.days.size)
        assertEquals(21, success.days.sumOf { it.meals.size })
        assertTrue(success.shopping.isNotEmpty())
        assertTrue(success.shopping.all { it.packsToBuy > 0 &&
            it.purchaseGrams >= it.grams })
        assertTrue(success.days.all { it.nutrition.kcal > 0 })
        assertTrue(success.warnings.any { it.contains("Стоимость не подтверждена") })
    }
    @Test fun allergenFilteringWorksInRealPlanner() {
        val outcome = SevenDayPlanner.plan(profile().copy(allergens="рыба, соя"))
        assertTrue(outcome is PlanningResult.Success)
        val recipes = (outcome as PlanningResult.Success).days.flatMap { it.meals }.map { it.recipe }
        assertTrue(recipes.none { "fish" in OfflineRecipes.allergens(it) || "soy" in OfflineRecipes.allergens(it) })
    }
    @Test fun unknownAllergenFailsClosed() {
        val result = SevenDayPlanner.plan(profile().copy(allergens="продукт неизвестного состава"))
        assertTrue(result is PlanningResult.Blocked)
    }
    @Test fun noCompatibleKitchenReturnsBlockNotFakeWeek() {
        val result = SevenDayPlanner.plan(profile().copy(appliances=emptySet()))
        assertTrue(result is PlanningResult.Blocked)
    }
    @Test fun veganWeekNeverContainsAnimalProtein() {
        val result = SevenDayPlanner.plan(profile().copy(diet="Веганский"))
        assertTrue(result is PlanningResult.Success)
        val recipes = (result as PlanningResult.Success).days.flatMap { it.meals }.map { it.recipe }
        assertTrue(recipes.all { "Веганский" in OfflineRecipes.diets(it) })
    }
    @Test fun noAgeBasedDietAdviceForMinors() {
        val result = SevenDayPlanner.plan(profile().copy(age="14"))
        assertTrue(result is PlanningResult.Blocked)
    }
    @Test fun pantryDeductedFromPurchase() {
        val noPantry = SevenDayPlanner.plan(profile()) as PlanningResult.Success
        val withPantry = SevenDayPlanner.plan(profile(), emptyMap(), mapOf("rice" to 5000)) as PlanningResult.Success
        assertTrue(withPantry.shopping.none { it.ingredient.id=="rice" })
        assertTrue(withPantry.shopping.sumOf { it.grams } <= noPantry.shopping.sumOf { it.grams })
    }
    @Test fun noFakeCaloriesIfSexNotSpecified() {
        val result = SevenDayPlanner.plan(profile().copy(sex="Не указывать")) as PlanningResult.Success
        assertNull(result.calorieTargetEstimate)
    }
}
