package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class ShoppingAggregationTest {
    @Test fun userPantryDeductedOnceAcrossFullWeek() {
        val d = PlannedDay(0,listOf(PlannedMeal(
            OfflineRecipes.recipes.first { it.id=="breakfast_rice_apple_water" },
            1.0, Nutrition.ZERO)))
        val without=ShoppingAggregation.fromDays(listOf(d,d),2)
        val with=ShoppingAggregation.fromDays(listOf(d,d),2,mapOf("rice" to 200))
        val rice=with.first { it.ingredient.id=="rice" }
        assertEquals(260,rice.requiredGrams)
        assertEquals(200,rice.pantryUsed)
        assertEquals(60,rice.grams)
        assertTrue(rice.packsToBuy>0)
        assertTrue(without.first { it.ingredient.id=="rice" }.grams>rice.grams)
    }
    @Test fun fullPantryMeansNoPurchaseOrPriceRequirement() {
        val d=PlannedDay(0,listOf(PlannedMeal(
            OfflineRecipes.recipes.first(),1.0,Nutrition.ZERO)))
        val lines=ShoppingAggregation.fromDays(listOf(d),1,
            OfflineRecipes.ingredients.keys.associateWith { 10000 })
        assertTrue(lines.all { it.grams==0 && it.packsToBuy==0 })
        val cost=ManualBasketPrices.estimate(lines,emptyMap(),"5000")
        assertEquals(0,cost.missingLines)
        assertEquals(0L,cost.knownTotalKopeks)
        assertEquals(true,cost.estimateFitsBudget)
        assertFalse(cost.verifiedShopPrices)
    }
}
