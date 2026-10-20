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
}
