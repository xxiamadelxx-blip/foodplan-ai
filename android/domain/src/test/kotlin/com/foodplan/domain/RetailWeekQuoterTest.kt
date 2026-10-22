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

    @Test fun conflictingPriceOrSupplierBlocksPriceQuote() {
        val priceMismatch = rice(120L).copy(
            product = rice(120L).product.copy(priceKopeks = 40000L)
        )
        val conflict = quote(63218L, listOf(rice(), priceMismatch), "rice")
        assertEquals(0, conflict.pricedIngredientCount)
        assertNull(conflict.completeEstimateKopeks)
        assertEquals(listOf("rice"), conflict.missingIngredientIds)

        val supplier = rice().copy(publicCardSupplierAmbiguity = true)
        val unsafe = quote(63218L, listOf(supplier), "rice")
        assertNull(unsafe.completeEstimateKopeks)
        assertFalse(unsafe.hasCompleteQuotedWeek)
    }

    @Test fun strictAllergenReviewIsIndependentOfPublishedDescription() {
        val result = quote(100000L, listOf(rice()), "rice", strict = true)
        assertTrue(result.missingIngredientIds.contains("rice"))
        assertNull(result.completeEstimateKopeks)
        assertFalse(result.physicalAllergenClearance)
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
}
