package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class RetailScanCoverageTest {
    private val now = 9_000_000L
    private val store = "Магнит"
    private val city = "Санкт-Петербург"
    private fun queries(vararg ids: String): List<AnonymousRetailQuery> =
        AnonymousRetailPlanner.build(ids.map {
            ShoppingNeed(OfflineRecipes.ingredients.getValue(it), 900, 1)
        })
    private fun rice(
        url: String = "https://magnit.ru/product/rice123",
        title: String = "Рис длиннозерный шлифованный 1кг",
        price: Long = 12000L
    ): RetailProductEvidence = RetailProductEvidence(
        ObservedProduct(title, url, 1000, price), store, city, now, true,
        manufacturer = "Изготовитель", ingredientsText = "Рис шлифованный",
        identitySpec = "Рис белый длиннозерный шлифованный",
        labelChecked = true, labelSourceUrl = url
    )

    @Test fun missingChickenCannotBeHiddenByMultipleRiceCards() {
        val report = RetailScanCoverage.assess(
            queries("rice", "chicken"),
            listOf(rice(), rice(url = "https://magnit.ru/product/rice456")),
            store, city, now)
        assertEquals(2, report.requiredIngredients)
        assertEquals(1, report.ingredientsWithObservedSku)
        assertEquals(1, report.ingredientsWithComparablePublishedSku)
        assertEquals(listOf("chicken"), report.unresolvedIngredientIds)
        assertFalse(report.addressLocalBasketVerified)
    }

    @Test fun duplicateSkuAndMissingDetailNeverCreateFakeCompleteness() {
        val weak = rice().copy(manufacturer = null, labelChecked = false)
        val report = RetailScanCoverage.assess(
            queries("rice"), listOf(weak, weak), store, city, now)
        assertEquals(1, report.rows.single().observedSkuCount)
        assertEquals(0, report.ingredientsWithComparablePublishedSku)
        assertEquals(listOf("rice"), report.unresolvedIngredientIds)
    }

    @Test fun staleOrOutOfStockOfferNotComparable() {
        val expired = rice().copy(observedAtEpochMs = now - 7 * 60 * 60 * 1000)
        val noStock = rice(url = "https://magnit.ru/product/rice999").copy(
            product = rice(url = "https://magnit.ru/product/rice999").product
                .copy(availability = ListingAvailability.OUT_OF_STOCK)
        )
        val report = RetailScanCoverage.assess(
            queries("rice"), listOf(expired, noStock), store, city, now)
        assertEquals(2, report.rows.single().observedSkuCount)
        assertEquals(0, report.rows.single().comparablePublishedSkuCount)
        assertFalse(report.addressLocalBasketVerified)
    }

    @Test fun evenEveryPublishedSkuIsNotProvenLocalStockOrCheckout() {
        val report = RetailScanCoverage.assess(
            queries("rice"), listOf(rice()), store, city, now)
        assertEquals(1, report.ingredientsWithComparablePublishedSku)
        assertEquals(0, report.ingredientsWithStockClaims)
        assertTrue(report.unresolvedIngredientIds.isEmpty())
        assertFalse(report.addressLocalBasketVerified)
    }

    @Test fun emptyPlanCannotBePresentedAsVerifiedFullWeek() {
        val report = RetailScanCoverage.assess(emptyList(), emptyList(),
            store, city, now)
        assertEquals(0, report.requiredIngredients)
        assertFalse(report.addressLocalBasketVerified)
    }
}
