package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class BudgetWeekRefinerTest {
    private fun profile(budget:String) = FoodProfile(
        age="31",sex="Женский",heightCm="170",weightKg="70",
        budgetRub=budget,city="Санкт-Петербург",stores=setOf("Магнит"),
        cookingEffort=CookingEffort.INVOLVED,cookingMinutes="55",
        appliances=setOf("Плита","Кастрюля","Сковорода","Духовка")
    )
    private fun syntheticCatalog(): List<RetailerOffer> =
        OfflineRecipes.ingredients.values.map { ingredient ->
            // Fixtures are synthetic, NOT current prices.
            val packPrice=when(ingredient.id) {
                "salmon" -> 110000L
                "beef" -> 75000L
                "chicken" -> 30000L
                "turkey" -> 40000L
                "cod" -> 60000L
                "pork" -> 46000L
                "tofu" -> 23000L
                else -> 11000L
            }
            RetailerOffer(ingredient.id,"Магнит","Санкт-Петербург",
                ingredient.name.replace(" сухой","").replace(" сырое","")+
                    " ${ingredient.packGrams}г",
                "https://magnit.ru/product/fixture-${ingredient.id}",
                ingredient.packGrams,packPrice,100000L,true,
                ListingAvailability.IN_STOCK,4.5,100)
        }
    @Test fun incompletePricingNeverClaimsBudgetIsMet() {
        val p=profile("25000")
        val initial=SevenDayPlanner.plan(p) as PlanningResult.Success
        val result=BudgetWeekRefiner.refine(initial,p,emptyMap(),emptyList(),
            "Магнит",110000L)
        assertEquals(BudgetWeekStatus.MISSING_PRICES,result.status)
        assertNull(result.basket.fitsBudget)
        assertEquals(0,result.numberOfMealChanges)
    }

    @Test fun budgetNeverExceededWhenDeclaredAffordable() {
        val p=profile("25000")
        val initial=SevenDayPlanner.plan(p) as PlanningResult.Success
        val result=BudgetWeekRefiner.refine(initial,p,emptyMap(),syntheticCatalog(),
            "Магнит",110000L)
        if(result.status==BudgetWeekStatus.WITHIN_BUDGET) {
            assertTrue(result.basket.estimatedTotalKopeks <= 2_500_000L)
            assertTrue(result.plan.days.size==7)
        }
        assertTrue(result.basket.items.all { it.packs>0 })
    }

    @Test fun tooSmallBudgetNeverGetsFakePass() {
        val p=profile("1")
        val initial=SevenDayPlanner.plan(p) as PlanningResult.Success
        val result=BudgetWeekRefiner.refine(initial,p,emptyMap(),syntheticCatalog(),
            "Магнит",110000L)
        assertNotEquals(BudgetWeekStatus.WITHIN_BUDGET,result.status)
    }
    @Test fun budget2500CannotPretendToFitOverpricedFullWeek() {
        val p=profile("2500")
        val initial=SevenDayPlanner.plan(p) as PlanningResult.Success
        val checked=BudgetWeekRefiner.refine(initial,p,emptyMap(),syntheticCatalog(),
            "Магнит",110000L)
        if (checked.status==BudgetWeekStatus.WITHIN_BUDGET) {
            assertTrue(checked.basket.estimatedTotalKopeks<=250000L)
        } else {
            assertTrue(checked.status in setOf(
                BudgetWeekStatus.OVER_BUDGET,BudgetWeekStatus.MISSING_PRICES))
        }
    }

}
