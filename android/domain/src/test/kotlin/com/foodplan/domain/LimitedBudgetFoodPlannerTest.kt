package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class LimitedBudgetFoodPlannerTest {
    private fun profile(budget:String="100")=FoodProfile(
        sex="Женский",age="30",heightCm="165",weightKg="65",budgetRub=budget,
        city="Санкт-Петербург",stores=setOf("Магнит")
    )
    private fun offer(id:String,price:Long,grams:Int,name:String,
                      availability:ListingAvailability=ListingAvailability.IN_STOCK,
                      city:String="Санкт-Петербург",
                      timestamp:Long=100_000L): RetailerOffer =
        RetailerOffer(id,"Магнит",city,name,
            "https://magnit.ru/product/synthetic-$id",grams,price,timestamp,true,
            availability,null,null)
    private val potato=offer("potato",10_000L,2000,"Картофель 2 кг")
    private val chicken=offer("chicken",11_000L,800,"Куриное мясо 800 г")
    @Test fun oneHundredRublesCanBuyOnlyAnActuallyPricedPackWithoutPretendingToFeedThreeDays() {
        val r=LimitedBudgetFoodPlanner.choose(profile(),"Магнит",
            listOf(potato,chicken),emptyMap(),110_000L,2000)
        assertEquals(LimitedFoodStatus.PARTIAL_AFFORDABLE_FOOD,r.status)
        assertEquals(10_000L,r.spentKopeks)
        assertTrue(r.spentKopeks<=r.budgetKopeks)
        assertEquals(listOf("potato"),r.lines.map { it.ingredientId })
        assertEquals(1540.0,r.totalNutritionEstimate.kcal,0.001)
        assertEquals(41.0,r.totalNutritionEstimate.protein,0.001)
        assertEquals(1.8,r.totalNutritionEstimate.fat,0.001)
        assertEquals(349.8,r.totalNutritionEstimate.carbs,0.001)
        assertEquals(0.77,r.equivalentDaysByCalories!!,0.0001)
        assertEquals(12_460.0,r.remainingWeekEnergyGapKcal!!,0.0001)
        assertTrue(r.warnings.any { it.contains("не полноценный рацион") })
    }
    @Test fun unknownAndStalePricesNeverProduceInvented100RublePotatoes() {
        val missing=LimitedBudgetFoodPlanner.choose(profile(),"Магнит",
            emptyList(),emptyMap(),110_000L,2000)
        assertEquals(LimitedFoodStatus.NO_RELIABLE_PRICES,missing.status)
        assertTrue(missing.lines.isEmpty())
        assertNull(missing.equivalentDaysByCalories)
        val stale=LimitedBudgetFoodPlanner.choose(profile(),"Магнит",
            listOf(potato.copy(observedAtEpochMs=1L)),emptyMap(),
            RetailerBasketPricing.MAX_OFFER_AGE_MS+200_000L,2000)
        assertEquals(LimitedFoodStatus.NO_RELIABLE_PRICES,stale.status)
    }
    @Test fun expensiveFullPackCannotBeSilentlySplitIntoFractions() {
        val r=LimitedBudgetFoodPlanner.choose(profile(),"Магнит",
            listOf(potato.copy(priceKopeks=10_001L)),emptyMap(),110_000L,2000)
        assertEquals(LimitedFoodStatus.NOTHING_AFFORDABLE,r.status)
        assertEquals(0L,r.spentKopeks)
    }
    @Test fun exactBudgetLike356And63218IsPreserved() {
        for(amount in listOf(100,356,63_218,100_000)){
            assertEquals(amount,WeeklyBudget.parse("$amount"))
        }
        assertNull(WeeklyBudget.parse("99"))
        assertNull(WeeklyBudget.parse("100001"))
        assertNull(WeeklyBudget.parse("356.2"))
        val r=LimitedBudgetFoodPlanner.choose(profile("356"),"Магнит",
            listOf(potato,chicken),emptyMap(),110_000L,2000)
        assertEquals(35_600L,r.budgetKopeks)
        assertTrue(r.spentKopeks<=35_600L)
    }
    @Test fun typedPriceIsNeverMisrepresentedAsObservedShopPrice() {
        val r=LimitedBudgetFoodPlanner.choose(profile(),"Магнит",emptyList(),
            mapOf("potato" to 10_000L),110_000L,2000)
        assertEquals(LimitedFoodStatus.PARTIAL_AFFORDABLE_FOOD,r.status)
        assertEquals(LimitedPriceOrigin.MANUALLY_ENTERED,r.lines.single().origin)
        assertEquals(ListingAvailability.UNKNOWN,r.lines.single().availability)
    }
    @Test fun allergiesAndDislikedFoodsStayFailClosedEvenUnderExtremeBudget() {
        val allergy=LimitedBudgetFoodPlanner.choose(profile().copy(allergens="молоко"),
            "Магнит",listOf(potato),emptyMap(),110_000L,2000)
        assertEquals(LimitedFoodStatus.BLOCKED_SAFETY_RESTRICTIONS,allergy.status)
        assertTrue(allergy.lines.isEmpty())
        val dislikes=LimitedBudgetFoodPlanner.choose(profile().copy(disliked="картофель"),
            "Магнит",listOf(potato),emptyMap(),110_000L,2000)
        assertEquals(LimitedFoodStatus.NO_RELIABLE_PRICES,dislikes.status)
    }
    @Test fun religiousFoodRulesNeverBypassVerificationWith100RublesOrLargeBudget() {
        for (diet in listOf("Халяль", "Кошерное", "Постное", "НЕИЗВЕСТНЫЙ_ТИП")) {
            for (amount in listOf("100", "63218")) {
                val result = LimitedBudgetFoodPlanner.choose(
                    profile(amount).copy(diet = diet), "Магнит",
                    listOf(potato, chicken), mapOf("potato" to 1000L),
                    110_000L, 2000
                )
                assertEquals("$diet / $amount",
                    LimitedFoodStatus.BLOCKED_SAFETY_RESTRICTIONS, result.status)
                assertEquals(0L, result.spentKopeks)
                assertTrue(result.lines.isEmpty())
                assertTrue(result.warnings.any { it.contains("подтверждение состава") })
            }
        }
    }

    @Test fun veganChoiceExcludesMeatAndOutOfStockItemsNeverPurchased() {
        val r=LimitedBudgetFoodPlanner.choose(profile("356").copy(diet="Веганский"),
            "Магнит",listOf(chicken,potato.copy(
                availability=ListingAvailability.OUT_OF_STOCK)),emptyMap(),110_000L,2000)
        assertEquals(LimitedFoodStatus.NO_RELIABLE_PRICES,r.status)
        assertTrue(r.lines.isEmpty())
    }
}
