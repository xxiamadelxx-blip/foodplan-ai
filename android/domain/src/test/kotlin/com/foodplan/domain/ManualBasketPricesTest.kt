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
    @Test fun strictRublesParsing() {
        assertEquals(12050L,ManualBasketPrices.fromRubles("120,5"))
        assertEquals(7999L,ManualBasketPrices.fromRubles("79.99"))
        assertEquals("79.99",ManualBasketPrices.formatKopeks(7999))
        assertNull(ManualBasketPrices.fromRubles("120,345"))
        assertNull(ManualBasketPrices.fromRubles("-10"))
        assertNull(ManualBasketPrices.fromRubles("абв"))
    }
}
