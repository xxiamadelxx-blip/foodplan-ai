package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class BudgetBasketOptimizerTest {
    private val rice=OfflineRecipes.ingredients.getValue("rice")
    private val chicken=OfflineRecipes.ingredients.getValue("chicken")
    private val groceries=listOf(ShoppingNeed(rice,1000,2),
        ShoppingNeed(chicken,800,1))
    private fun offer(id:String,sku:String,pack:Int,price:Long,stars:Double?,reviews:Int?,
        city:String="Санкт-Петербург",availability:ListingAvailability=ListingAvailability.IN_STOCK)=
        RetailerOffer(id,"Магнит",city,
            if(id=="rice") "Рис $sku ${pack}г" else "Курица $sku ${pack}г",
            "https://magnit.ru/product/$sku",pack,price,100000L,
            true,availability,stars,reviews)

    @Test fun weekly2500ChoosesAffordableWhile25000UsesBetterReviewedVariants() {
        // Fictional fixture. Prices/reviews here are NOT real quotes.
        val data=listOf(
            offer("rice","budget-rice",500,30000,3.7,300),
            offer("rice","premium-rice",1000,135000,4.9,600),
            offer("chicken","budget-chicken",800,150000,4.0,200),
            offer("chicken","premium-chicken",800,500000,4.9,1000)
        )
        val low=BudgetBasketOptimizer.choose(groceries,data,"Санкт-Петербург","Магнит",2500,110000L)
        val high=BudgetBasketOptimizer.choose(groceries,data,"Санкт-Петербург","Магнит",25000,110000L)
        assertEquals(true,low.fitsBudget)
        assertEquals(true,high.fitsBudget)
        assertEquals(0,low.budgetResponsiveUpgrades)
        assertTrue(high.budgetResponsiveUpgrades > low.budgetResponsiveUpgrades)
        assertTrue(high.estimatedTotalKopeks > low.estimatedTotalKopeks)
        assertTrue(high.estimatedTotalKopeks < high.budgetKopeks)
        assertEquals(groceries.size,low.items.size)
        assertEquals(groceries.size,high.items.size)
    }

    @Test fun expensiveBadlyRatedOfferIsNotForcedOnBigBudget() {
        val input=listOf(offer("rice","cheap-good",1000,17000,4.8,300),
            offer("rice","bad-expensive",1000,90000,3.0,400))
        val result=BudgetBasketOptimizer.choose(listOf(groceries.first()),
            input,"Санкт-Петербург","Магнит",25000,110000L)
        assertTrue(result.items.single().productUrl.endsWith("cheap-good"))
        assertEquals(17000L,result.estimatedTotalKopeks)
    }

    @Test fun impossible2500MustReturnOverBudgetAndNotDropIngredient() {
        val costly=listOf(offer("rice","costly-rice",1000,120000,4.9,300),
            offer("chicken","costly-chicken",800,200000,4.9,300))
        val r=BudgetBasketOptimizer.choose(groceries,costly,
            "Санкт-Петербург","Магнит",2500,110000L)
        assertEquals(false,r.fitsBudget)
        assertEquals(2,r.items.size)
        assertEquals(320000L,r.cheapestCompleteBasketKopeks)
    }

    @Test fun unknownPriceOrRegionMustNotClaimBudgetSuccess() {
        val result=BudgetBasketOptimizer.choose(groceries,
            listOf(offer("rice","r",1000,15000,4.9,200,city="Москва")),
            "Санкт-Петербург","Магнит",25000,110000L)
        assertEquals(2,result.missingIngredientIds.size)
        assertNull(result.fitsBudget)
    }

    @Test fun outOfStockMustNotBePickedAndUnknownStockNotConfirmed() {
        val input=listOf(
            offer("rice","sold-out",1000,10000,5.0,200,availability=ListingAvailability.OUT_OF_STOCK),
            offer("rice","uncertain",1000,20000,4.5,250,availability=ListingAvailability.UNKNOWN)
        )
        val r=BudgetBasketOptimizer.choose(listOf(groceries.first()),input,
            "Санкт-Петербург","Магнит",25000,110000L)
        assertEquals("Рис uncertain 1000г",r.items.single().productName)
        assertFalse(r.stockConfirmed)
        assertEquals(true,r.fitsBudget) // indicative price only, not verified store stock
    }

    @Test fun familyBudgetIsForWholeBasketNotEachProduct() {
        val twoItems=listOf(
            offer("rice","rice",1000,170000,4.5,100),
            offer("chicken","chicken",800,170000,4.5,100)
        )
        val r=BudgetBasketOptimizer.choose(groceries,twoItems,
            "Санкт-Петербург","Магнит",2500,110000L)
        assertEquals(false,r.fitsBudget)
        assertEquals(510000L,r.cheapestCompleteBasketKopeks)
    }
}
