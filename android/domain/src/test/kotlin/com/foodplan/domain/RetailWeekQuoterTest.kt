package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class RetailWeekQuoterTest {
    private val now = 10_000_000L
    private fun needs(vararg ids: String) = AnonymousRetailPlanner.build(
        ids.map { ShoppingNeed(OfflineRecipes.ingredients.getValue(it), 900, 1) }
    )
    private fun rice(priceRub: Long = 120L, url: String = "https://magnit.ru/product/rice900") =
        RetailProductEvidence(
            ObservedProduct("Рис длиннозерный белый шлифованный 1кг", url,
                1000, priceRub * 100),
            "Магнит", "Санкт-Петербург", now, true,
            manufacturer = "Комбинат", ingredientsText = "Рис шлифованный",
            identitySpec = "Рис длиннозерный белый шлифованный",
            labelSourceUrl = url, labelChecked = true
        )

    private fun chicken(): RetailProductEvidence {
        val url = "https://magnit.ru/product/breast500"
        return RetailProductEvidence(
            ObservedProduct("Филе куриное грудки 500г", url, 500, 25000),
            "Магнит", "Санкт-Петербург", now, true,
            manufacturer = "Птицефабрика", ingredientsText = "Филе грудки куриное",
            identitySpec = "Филе грудки куриное сырое без кожи без костей",
            labelSourceUrl = url, labelChecked = true
        )
    }

    private fun quote(
        budget: Long, inputs: List<RetailProductEvidence>,
        vararg ids: String, strict: Boolean = false
    ) = RetailWeekQuoter.build(
        needs(*ids), inputs, "Магнит", "Санкт-Петербург", budget, now,
        strictAllergenVerification = strict
    )

    @Test fun realWholePackCostsAndOneRubleBudgetAccuracy() {
        // 900g rice requires 1kg bag (120 RUB); 900g breast needs two
        // 500g packs (2*250 RUB). Total published estimate = 620 RUB.
        val under = quote(356L, listOf(rice(), chicken()), "rice", "chicken")
        assertEquals(2, under.pricedIngredientCount)
        assertTrue(under.hasCompleteQuotedWeek)
        assertEquals(62000L, under.knownSubtotalKopeks)
        assertEquals(62000L, under.completeEstimateKopeks)
        assertEquals(false, under.estimatedWithinBudget)
        assertEquals(2L, under.lines.first { it.ingredientId == "chicken" }.packages)
        val justEnough = quote(620L, listOf(rice(), chicken()), "rice", "chicken")
        assertEquals(true, justEnough.estimatedWithinBudget)
        val larger = quote(63218L, listOf(rice(), chicken()), "rice", "chicken")
        assertEquals(true, larger.estimatedWithinBudget)
        assertFalse(larger.actualAddressBasketVerified)
    }

    @Test fun partiallyPricedWeekNeverSaysBudgetFitsEvenAt100000() {
        val quote = quote(100000L, listOf(rice()), "rice", "chicken")
        assertEquals(1, quote.pricedIngredientCount)
        assertEquals(listOf("chicken"), quote.missingIngredientIds)
        assertEquals(12000L, quote.knownSubtotalKopeks)
        assertNull(quote.completeEstimateKopeks)
        assertNull(quote.estimatedWithinBudget)
        assertFalse(quote.hasCompleteQuotedWeek)
    }

    @Test fun incompleteWeekCanAlreadyBeProvenTooExpensive() {
        val q = quote(100L, listOf(rice()), "rice", "chicken")
        assertEquals(listOf("chicken"), q.missingIngredientIds)
        assertEquals(12000L, q.knownSubtotalKopeks)
        assertNull(q.completeEstimateKopeks)
        assertNull(q.estimatedWithinBudget)
        assertTrue(q.knownSubtotalExceedsBudget)
        val rich = quote(100000L, listOf(rice()), "rice", "chicken")
        assertFalse(rich.knownSubtotalExceedsBudget)
        assertNull(rich.estimatedWithinBudget)
    }

    @Test fun conflictingPriceOrSupplierBlocksPriceQuote() {
        val priceMismatch = rice(120L).copy(
            product = rice(120L).product.copy(priceKopeks = 40000L)
        )
        val conflict = quote(63218L, listOf(rice(), priceMismatch), "rice")
        assertEquals(0, conflict.pricedIngredientCount)
        assertNull(conflict.completeEstimateKopeks)
        assertEquals(listOf("rice"), conflict.missingIngredientIds)

        // Multiple suppliers can share an approximate *price*; their
        // ingredient and nutrient identity must still remain unverified.
        val supplier = rice().copy(publicCardSupplierAmbiguity = true)
        val estimateOnly = quote(63218L, listOf(supplier), "rice")
        assertEquals(12000L, estimateOnly.completeEstimateKopeks)
        assertTrue(estimateOnly.hasCompleteQuotedWeek)
        assertFalse(estimateOnly.physicalAllergenClearance)
    }

    @Test fun publishedPriceRemainsVisibleWithoutAllergenClearance() {
        val result = quote(100000L, listOf(rice()), "rice", strict = true)
        assertEquals(12000L, result.completeEstimateKopeks)
        assertTrue(result.hasCompleteQuotedWeek)
        assertFalse(result.physicalAllergenClearance)
        assertFalse(result.actualAddressBasketVerified)
    }

    @Test fun approximateChainPriceWorksWithoutAddressManufacturerOrComposition() {
        val generic = rice().copy(
            city = "Москва",
            addressSelectedByUser = false,
            manufacturer = null,
            ingredientsText = null,
            identitySpec = null,
            labelSourceUrl = null,
            labelChecked = false,
            publicCardSupplierAmbiguity = true
        )
        val estimate = quote(356L, listOf(generic), "rice")
        assertTrue(estimate.hasCompleteQuotedWeek)
        assertEquals(12000L, estimate.completeEstimateKopeks)
        assertEquals(true, estimate.estimatedWithinBudget)
        assertFalse(estimate.actualAddressBasketVerified)
        assertFalse(estimate.physicalAllergenClearance)
    }

    @Test fun approximatePriceOnlyAllowsLocalOutOfStockButNotFakeFoodIdentity() {
        val stock = rice().copy(
            addressSelectedByUser = false,
            product = rice().product.copy(availability = ListingAvailability.OUT_OF_STOCK)
        )
        val estimate = quote(356L, listOf(stock), "rice")
        assertEquals(12000L, estimate.completeEstimateKopeks)
        assertFalse(estimate.actualAddressBasketVerified)
        val wrong = stock.copy(product = stock.product.copy(productName = "Рис отварной 1кг"))
        val invalid = quote(356L, listOf(wrong), "rice")
        assertNull(invalid.completeEstimateKopeks)
    }

    @Test fun zeroProductsNotAFreeOrCompleteSevenDayDiet() {
        val empty = quote(100L, emptyList())
        assertEquals(0, empty.requiredIngredientCount)
        assertNull(empty.completeEstimateKopeks)
        assertNull(empty.estimatedWithinBudget)
        assertFalse(empty.hasCompleteQuotedWeek)
    }

    @Test fun selectedSupplierIsNotProofOfCheckoutStock() {
        val result = quote(100000L, listOf(rice()), "rice")
        assertEquals(1, result.pricedIngredientCount)
        assertFalse(result.actualAddressBasketVerified)
        assertFalse(result.physicalAllergenClearance)
    }

    private fun officialCountOrVolume(
        ingredientId: String, title: String, priceKopeks: Long
    ): RetailProductEvidence {
        val entry = MagnitPackOnlySkuCatalog.entries.single {
            it.ingredientId == ingredientId
        }
        return RetailProductEvidence(
            ObservedProduct(title, entry.url, null, priceKopeks,
                purchasePack = entry.pack),
            "Магнит", "Санкт-Петербург", now, false
        )
    }

    @Test fun officialEggAndOilUnitPricesDoNotBecomeFakeGramPrices() {
        val egg = officialCountOrVolume("egg", "Куриное яйцо С1 10шт", 8539L)
        val oil = officialCountOrVolume("oil", "Масло подсолнечное 900мл", 12599L)
        val basket = quote(63218L, listOf(rice(), egg, oil), "rice", "egg", "oil")
        assertEquals(1, basket.pricedIngredientCount)
        assertEquals(3, basket.requiredIngredientCount)
        assertEquals(listOf("egg", "oil"), basket.missingIngredientIds)
        assertEquals(12000L, basket.knownSubtotalKopeks)
        assertNull(basket.completeEstimateKopeks)
        assertNull(basket.estimatedWithinBudget)
        val eggLine = basket.lines.single { it.ingredientId == "egg" }
        assertEquals(PurchasePack(PurchaseUnit.PIECE, 10), eggLine.observedPack)
        assertEquals(8539L, eggLine.observedPackPriceKopeks)
        assertNull(eggLine.packages)
        assertNull(eggLine.estimatedCostKopeks)
        val oilLine = basket.lines.single { it.ingredientId == "oil" }
        assertEquals(PurchasePack(PurchaseUnit.MILLILITRE, 900), oilLine.observedPack)
        assertEquals(12599L, oilLine.observedPackPriceKopeks)
        assertNull(oilLine.packages)
        assertNull(oilLine.estimatedCostKopeks)
    }

    @Test fun wrongUnitTitleOrUnapprovedUrlCanNeverEnterWeeklyQuote() {
        val wrongEgg = officialCountOrVolume("egg", "Куриное яйцо С1 30шт", 8539L)
        val wrongOil = officialCountOrVolume("oil", "Масло подсолнечное 900г", 12599L)
        val basket = quote(63218L, listOf(wrongEgg, wrongOil), "egg", "oil")
        assertEquals(0, basket.pricedIngredientCount)
        assertTrue(basket.lines.all { it.observedPackPriceKopeks == null })
        val rightEgg = officialCountOrVolume("egg", "Куриное яйцо С1 10шт", 8539L)
        val forged = rightEgg.copy(product = rightEgg.product.copy(
            productUrl = "https://magnit.ru/product/unlisted"))
        assertNull(quote(63218L, listOf(forged), "egg")
            .lines.single().observedPackPriceKopeks)
    }

    @Test fun conflictingObservedUnitPricesAreNotTrusted() {
        val source = officialCountOrVolume("egg", "Куриное яйцо С1 10шт", 8539L)
        val conflict = source.copy(product = source.product.copy(priceKopeks = 9999L))
        val basket = quote(63218L, listOf(source, conflict), "egg")
        assertNull(basket.lines.single().observedPackPriceKopeks)
        assertEquals(listOf("egg"), basket.missingIngredientIds)
    }
}
