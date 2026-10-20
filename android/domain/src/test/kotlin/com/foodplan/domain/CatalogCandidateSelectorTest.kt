package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class CatalogCandidateSelectorTest {
    private val rice=OfflineRecipes.ingredients.getValue("rice")
    private fun product(name:String,grams:Int,price:Long,url:String):ObservedProduct =
        ObservedProduct(name,url,grams,price)
    @Test fun picksCheapestTotalPackCostNotUnitStickerPrice() {
        val matches=CatalogCandidateSelector.suggest(
            listOf(ShoppingNeed(rice,950,2)),
            listOf(product("Рис 500г",500,10000,"https://magnit.ru/product/1-rice"),
                product("Рис 1кг",1000,17000,"https://magnit.ru/product/2-rice"),
                product("Сода 1кг",1000,1000,"https://magnit.ru/product/3-soda"))
        )
        assertEquals(1,matches.size)
        assertEquals("Рис 1кг",matches.single().product.productName)
        assertEquals(17000L,matches.single().approximateCostKopeks)
    }

    @Test fun excludesUnrelatedAndProcessedIngredients() {
        val matches=CatalogCandidateSelector.suggest(
            listOf(ShoppingNeed(rice,500,1)),
            listOf(product("Рисовый соус 500г",500,9000,"https://magnit.ru/product/4-rice"),
                product("Готовый рис 500г",500,11000,"https://magnit.ru/product/5-rice"))
        )
        assertTrue(matches.isEmpty())
    }

    @Test fun noQuoteForMissingVisibleSku() {
        assertTrue(CatalogCandidateSelector.suggest(
            listOf(ShoppingNeed(rice,200,1)),emptyList()).isEmpty())
    }
    @Test fun preservesMultiplePricesAndReviewsForOneIngredient() {
        val needs=listOf(ShoppingNeed(rice,950,2))
        val products=listOf(
            ObservedProduct("Рис эконом 500г","https://magnit.ru/product/rice-econom",
                500,8500,ListingAvailability.UNKNOWN,4.1,220),
            ObservedProduct("Рис премиум 1кг","https://magnit.ru/product/rice-premium",
                1000,19000,ListingAvailability.UNKNOWN,4.9,980),
            ObservedProduct("Сода 500г","https://magnit.ru/product/soda",
                500,2000)
        )
        val options=CatalogCandidateSelector.allMatchingVariants(needs,products)
        assertEquals(2,options.size)
        assertEquals(setOf(4.1,4.9),options.mapNotNull { it.product.ratingStars }.toSet())
        assertEquals(setOf(220,980),options.mapNotNull { it.product.reviewCount }.toSet())
        val cheap=CatalogCandidateSelector.suggestForBudget(
            needs,products,"Санкт-Петербург",180,100000L)
        val premium=CatalogCandidateSelector.suggestForBudget(
            needs,products,"Санкт-Петербург",25000,100000L)
        assertEquals(1,cheap.size)
        assertEquals("Рис эконом 500г",cheap.single().product.productName)
        assertEquals(1,premium.size)
        assertEquals("Рис премиум 1кг",premium.single().product.productName)
    }

    @Test fun variantListRejectsProcessedFoodAndUnknownPackSizes() {
        val choices=CatalogCandidateSelector.allMatchingVariants(
            listOf(ShoppingNeed(rice,700,1)),
            listOf(ObservedProduct("Готовый рис 700г",
                "https://magnit.ru/product/ready-rice",700,12000),
                ObservedProduct("Рис без веса",
                    "https://magnit.ru/product/rice-no-weight",null,12000))
        )
        assertTrue(choices.isEmpty())
    }

}
