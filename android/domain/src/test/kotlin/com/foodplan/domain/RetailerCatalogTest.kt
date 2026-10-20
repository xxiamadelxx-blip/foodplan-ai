package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class RetailerCatalogTest {
    @Test fun parsesRenderedMagnitCatalogueExamples() {
        assertEquals(8999L,CatalogValueParser.parseDisplayedRubles("89,99 ₽"))
        assertEquals(17999L,CatalogValueParser.parseDisplayedRubles("179,99 ₽"))
        assertEquals(500,CatalogValueParser.packageGrams("Сода пищевая 500г в ассортименте"))
        assertEquals(800,CatalogValueParser.packageGrams("Рис Националь Здоровье 800г"))
        assertEquals(2500,CatalogValueParser.packageGrams("Картофель фасованный 2.5кг"))
        assertNull(CatalogValueParser.packageGrams("Молоко 1л"))
        assertNull(CatalogValueParser.packageGrams("Цена за 1 шт"))
        assertNull(CatalogValueParser.parseDisplayedRubles("сегодня"))
    }
    private fun offer(store: String="Магнит",city: String="Санкт-Петербург",
        grams: Int=800,price: Long=17999L,timestamp: Long=100_000L,
        addressConfirmed:Boolean=true) =
        RetailerOffer("rice",store,city,"Рис 800г","https://magnit.ru/product/100-rice",
            grams,price,timestamp,addressConfirmed)
    private val rice = OfflineRecipes.ingredients.getValue("rice")
    private val shopping = listOf(ShoppingNeed(rice,950,2))
    @Test fun observedPackSizeOverridesCatalogueAssumption() {
        val quote=RetailerBasketPricing.estimate(shopping,listOf(offer()),
            "Санкт-Петербург","Магнит","1000",110_000L)
        assertEquals(2L,quote.lines.single().packages)
        assertEquals(800,quote.lines.single().chosenPackGrams)
        assertEquals(35998L,quote.knownTotalKopeks)
        assertEquals(false,quote.verifiedForCheckout)
        assertEquals(true,quote.budgetEstimate)
    }
    @Test fun unknownCityOrStoreOrAddressNeverProvesBudget() {
        for (entry in listOf(offer(city="Москва"),offer(store="Лента"),offer(addressConfirmed=false))) {
            val quote=RetailerBasketPricing.estimate(shopping,listOf(entry),
                "Санкт-Петербург","Магнит","1000",110_000L)
            assertNull(quote.budgetEstimate)
            assertEquals(1,quote.missingLines)
        }
    }
    @Test fun stalePriceMustNotBeUsed() {
        val quote=RetailerBasketPricing.estimate(shopping,listOf(offer()),
            "Санкт-Петербург","Магнит","1000",
            100_000L + RetailerBasketPricing.MAX_OFFER_AGE_MS + 1)
        assertNull(quote.budgetEstimate)
    }
    @Test fun unpricedLinesAreNeverFreeProducts() {
        val quote=RetailerBasketPricing.estimate(shopping,emptyList(),
            "Санкт-Петербург","Магнит","500",110_000)
        assertEquals(0L,quote.knownTotalKopeks)
        assertEquals(1,quote.missingLines)
        assertNull(quote.budgetEstimate)
    }
    @Test fun quotedPriceIsNotCheckoutPrice() {
        val quote=RetailerBasketPricing.estimate(shopping,
            listOf(offer(price=99999L)),"Санкт-Петербург","Магнит","100",110_000L)
        assertEquals(false,quote.budgetEstimate)
        assertFalse(quote.verifiedForCheckout)
    }
    @Test fun unrelatedSodaCannotBeSilentlyPricedAsRice() {
        assertFalse(RetailerProductMatcher.matchesIngredient("rice","Сода пищевая 500г"))
        assertTrue(RetailerProductMatcher.matchesIngredient("rice","Рис Националь Золотистый 900г"))
        assertFalse(RetailerProductMatcher.matchesIngredient("rice","Рисовый уксус 400мл"))
        assertTrue(RetailerProductMatcher.matchesIngredient("buckwheat","Гречка ядрица 900г"))
    }

    @Test fun mismatchedProductNamesNeverConvertPotatoOrSodaPriceToRice() {
        val counterfeit=offer(price=100L).copy(productName="Сода пищевая 800г")
        val quote=RetailerBasketPricing.estimate(shopping,listOf(counterfeit),
            "Санкт-Петербург","Магнит","100",110_000L)
        assertEquals(1,quote.missingLines)
        assertNull(quote.budgetEstimate)
        assertNull(quote.lines.single().costKopeks)
    }

    @Test fun soldOutOrUntrustedRetailerLinksAreNeverAcceptedAsCheapOffers() {
        val invalid=listOf(
            offer(price=100L).copy(availability=ListingAvailability.OUT_OF_STOCK),
            offer(price=100L).copy(productUrl="https://fake-magnit.ru/product/rice"),
            offer(price=100L).copy(productUrl="https://magnit.ru@evil.test/product/rice"),
            offer(price=100L).copy(productUrl="https://magnit.ru/product/rice#old"),
            offer(price=100L).copy(ratingStars=Double.NaN),
            offer(price=100L).copy(reviewCount=-1)
        )
        invalid.forEach { bad ->
            val quote=RetailerBasketPricing.estimate(shopping,listOf(bad),
                "Санкт-Петербург","Магнит","100",110_000L)
            assertEquals(bad.toString(),1,quote.missingLines)
            assertNull(bad.toString(),quote.budgetEstimate)
        }
    }

    @Test fun acceptedKnownShopQuotesStayEstimatesNotConfirmedCheckout() {
        val vkusvill=offer(store="ВкусВилл",price=9000L)
            .copy(productUrl="https://vkusvill.ru/goods/real-rice")
        val quote=RetailerBasketPricing.estimate(shopping,listOf(vkusvill),
            "Санкт-Петербург","ВкусВилл","356",110_000L)
        assertEquals(18_000L,quote.knownTotalKopeks)
        assertEquals(true,quote.budgetEstimate)
        assertFalse(quote.verifiedForCheckout)
        val unsupported=RetailerBasketPricing.estimate(shopping,
            listOf(vkusvill.copy(storeName="Лента")),
            "Санкт-Петербург","Лента","356",110_000L)
        assertNull(unsupported.budgetEstimate)
    }

    @Test fun nonContractBudgetCannotBeMarkedAsAffordable() {
        for (amount in listOf("99","100001","3.56","")) {
            val quote=RetailerBasketPricing.estimate(shopping,listOf(offer()),
                "Санкт-Петербург","Магнит",amount,110_000L)
            assertNull(amount,quote.budgetEstimate)
        }
    }

    @Test fun rawAndDryFoodMustNotInheritPricesFromCookedOrProcessedProducts() {
        assertFalse(RetailerProductMatcher.matchesIngredient("rice","Рис вареный 800г"))
        assertFalse(RetailerProductMatcher.matchesIngredient("rice","Рис с овощами 800г"))
        assertFalse(RetailerProductMatcher.matchesIngredient("potato","Картофель фри 1кг"))
        assertFalse(RetailerProductMatcher.matchesIngredient("potato","Картофель пюре 500г"))
        assertFalse(RetailerProductMatcher.matchesIngredient("pasta","Паста томатная 500г"))
        assertFalse(RetailerProductMatcher.matchesIngredient("berries","Малина замороженная 300г"))
        assertFalse(RetailerProductMatcher.matchesIngredient("berries","Клубника свежая 300г"))
        assertFalse(RetailerProductMatcher.matchesIngredient("berries","Клубника замороженная с сахаром 300г"))
        assertTrue(RetailerProductMatcher.matchesIngredient("rice","Рис длиннозерный сухой 800г"))
        assertTrue(RetailerProductMatcher.matchesIngredient("potato","Картофель свежий 2кг"))
        assertTrue(RetailerProductMatcher.matchesIngredient("berries","Клубника замороженная 300г"))
    }

    @Test fun cheapReadyMealDoesNotPassAsRawRiceAtAnyBudget() {
        val wrong=offer(price=100L).copy(productName="Рис отварной 800г")
        val basket=RetailerBasketPricing.estimate(shopping,listOf(wrong),
            "Санкт-Петербург","Магнит","100",110_000L)
        assertEquals(1,basket.missingLines)
        assertNull(basket.budgetEstimate)
    }

}
