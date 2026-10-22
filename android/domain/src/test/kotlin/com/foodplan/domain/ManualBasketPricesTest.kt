package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class ManualBasketPricesTest {
    private val ingredient = OfflineRecipes.ingredients.getValue("rice")
    @Test fun userPricesAreOnlyEstimates() {
        val need = ShoppingNeed(ingredient, 1200, 2)
        val p = ManualBasketPrices.estimate(listOf(need), mapOf("rice" to 14000L), "500")
        assertEquals(28000L,p.knownTotalKopeks)
        assertEquals(true,p.estimateFitsBudget)
        assertFalse(p.verifiedShopPrices)
    }
    @Test fun missingPricesCannotProveBudget() {
        val need = ShoppingNeed(ingredient, 1200, 2)
        val p = ManualBasketPrices.estimate(listOf(need), emptyMap(), "500")
        assertEquals(0L,p.knownTotalKopeks)
        assertEquals(1,p.missingLines)
        assertNull(p.estimateFitsBudget)
    }
    @Test fun zeroAndMissingManualPriceNeverPretendToCoverBasket() {
        val need = ShoppingNeed(ingredient, 1200, 2)
        for (value in listOf(0L, -1L, 10_000_001L)) {
            val estimate = ManualBasketPrices.estimate(
                listOf(need), mapOf("rice" to value), "100"
            )
            assertEquals(0L, estimate.knownTotalKopeks)
            assertEquals(0, estimate.pricedLines)
            assertEquals(1, estimate.missingLines)
            assertNull(estimate.estimateFitsBudget)
            assertFalse(estimate.verifiedShopPrices)
        }
        for (raw in listOf("0", "0,00", "100000,01", "9999999", "-1")) {
            assertNull(raw, ManualBasketPrices.fromRubles(raw))
        }
        assertEquals(1L, ManualBasketPrices.fromRubles("0,01"))
        assertEquals(10_000_000L, ManualBasketPrices.fromRubles("100000"))
    }

    @Test fun invalidOrExtremeWeeklyBudgetCannotGetAffordabilityPass() {
        val need = ShoppingNeed(ingredient, 1200, 2)
        val priced = mapOf("rice" to 100L)
        for (budget in listOf("0", "99", "100001", "-1", "100.50", "")) {
            val estimate = ManualBasketPrices.estimate(listOf(need), priced, budget)
            assertEquals(200L, estimate.knownTotalKopeks)
            assertEquals(1, estimate.pricedLines)
            assertNull(budget, estimate.estimateFitsBudget)
        }
        for (budget in listOf("100", "356", "63218", "100000")) {
            val estimate = ManualBasketPrices.estimate(listOf(need), priced, budget)
            assertEquals(true, estimate.estimateFitsBudget)
        }
    }

    @Test fun strictRublesParsing() {
        assertEquals(12050L,ManualBasketPrices.fromRubles("120,5"))
        assertEquals(7999L,ManualBasketPrices.fromRubles("79.99"))
        assertEquals("79.99",ManualBasketPrices.formatKopeks(7999))
        assertNull(ManualBasketPrices.fromRubles("120,345"))
        assertNull(ManualBasketPrices.fromRubles("-10"))
        assertNull(ManualBasketPrices.fromRubles("абв"))
    }
}
