package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class StoreCartTransferTest {
    private val rice = OfflineRecipes.ingredients.getValue("rice")
    private val buckwheat = OfflineRecipes.ingredients.getValue("buckwheat")
    private val url = "https://magnit.ru/product/1000000000-ris_800g"
    private fun quote(id:String="rice",city:String="Санкт-Петербург", shop:String="Магнит",
        at:Long=100000L)=RetailerOffer(id,shop,city,"Рис 800г",
            url,800,15000L,at,true)

    @Test fun buildsRealSkuQueueAndMarksMissing() {
        val plan = StoreCartTransfer.build(
            listOf(ShoppingNeed(rice,1000,2),
                ShoppingNeed(buckwheat,500,1)),
            listOf(quote()),"Санкт-Петербург","Магнит",110000L
        )
        assertEquals(1,plan.readyCount)
        assertEquals(2L,plan.ready.single().units)
        assertEquals(30000L,plan.estimatedTotalKopeks)
        assertEquals(listOf("buckwheat"),plan.missing)
        assertFalse(plan.completeCoverage)
    }

    @Test fun rejectsWrongStoreRegionAndStaleSku() {
        val shopping=listOf(ShoppingNeed(rice,1000,2))
        for (offer in listOf(quote(city="Москва"),quote(shop="Лента"),
            quote(at=100000L))) {
            val now=if (offer.city=="Санкт-Петербург" && offer.storeName=="Магнит")
                100000L+RetailerBasketPricing.MAX_OFFER_AGE_MS+1L else 110000L
            val plan=StoreCartTransfer.build(shopping,listOf(offer),
                "Санкт-Петербург","Магнит",now)
            assertEquals(0,plan.readyCount)
            assertEquals(listOf("rice"),plan.missing)
        }
    }

    @Test fun zeroShoppingNeedsNothing() {
        val plan=StoreCartTransfer.build(listOf(ShoppingNeed(rice,0,0)),emptyList(),
            "Санкт-Петербург","Магнит",110000L)
        assertTrue(plan.completeCoverage)
        assertTrue(plan.ready.isEmpty())
        assertEquals(0L,plan.estimatedTotalKopeks)
    }

    @Test fun strictMagnitHostAndProductPath() {
        assertTrue(StoreCartTransfer.magnitProductUrlValid(url))
        assertFalse(StoreCartTransfer.magnitProductUrlValid("https://magnit.ru.evil.tld/product/1"))
        assertFalse(StoreCartTransfer.magnitProductUrlValid("http://magnit.ru/product/1"))
        assertFalse(StoreCartTransfer.magnitProductUrlValid("https://magnit.ru/profile"))
    }
}
