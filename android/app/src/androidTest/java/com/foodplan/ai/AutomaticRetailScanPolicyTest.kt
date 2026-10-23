package com.foodplan.ai

import com.foodplan.domain.*
import org.junit.Assert.*
import org.junit.Test

/** Pure policy tests run on Android without requiring a live retailer connection. */
class AutomaticRetailScanPolicyTest {
    private fun need(id: String, grams: Int): ShoppingNeed {
        val food = OfflineRecipes.ingredients.getValue(id)
        return ShoppingNeed(food, grams, 1, grams, 0)
    }

    @Test fun knownOfficialSkuPathsAreBundledWithoutPricesOrPrivateHosts() {
        val urls = MagnitKnownSkuCatalog.urls
        assertEquals(35, urls.size)
        assertTrue(urls.keys.containsAll(
            listOf("rice", "oats", "egg", "oil", "milk", "chicken", "banana", "chickpeas")))
        assertEquals(
            "https://magnit.ru/product/3152910005-ris_dlinnozernyy_2_sort_800g_6",
            MagnitKnownSkuCatalog.urlFor("rice"))
        assertEquals(
            "https://magnit.ru/product/3284010204-moya_tsena_krupa_gerkules_fas_400g_6",
            MagnitKnownSkuCatalog.urlFor("oats"))
        assertNull(MagnitKnownSkuCatalog.urlFor("unknown"))
        assertTrue(urls.values.all { it.startsWith("https://magnit.ru/product/") })
    }

    @Test fun startsAutomaticallyOnlyForSelectedStoreAndNewRealWeek() {
        val rice = need("rice", 900)
        val egg = need("egg", 600)
        val key = AutomaticRetailScanPolicy.shoppingKey(
            "Санкт-Петербург", listOf(egg, rice))
        assertNotNull(key)
        assertEquals(key, AutomaticRetailScanPolicy.shoppingKey(
            " Санкт-Петербург ", listOf(rice, egg)))
        assertTrue(AutomaticRetailScanPolicy.canStart(
            "Магнит", true, key, emptySet(), 0))
        assertFalse(AutomaticRetailScanPolicy.canStart(
            "Пятёрочка", true, key, emptySet(), 0))
        assertFalse(AutomaticRetailScanPolicy.canStart(
            "Магнит", false, key, emptySet(), 0))
        assertFalse(AutomaticRetailScanPolicy.canStart(
            "Магнит", true, key, setOf(requireNotNull(key)), 1))
        assertFalse(AutomaticRetailScanPolicy.canStart(
            "Магнит", true, key, emptySet(),
            AutomaticRetailScanPolicy.MAX_WEEK_RESCANS_PER_SESSION))
        // Same physical SKUs still have the same pack price after changing
        // amounts. The ordinary basket calculator handles new pack counts.
        assertEquals(key, AutomaticRetailScanPolicy.shoppingKey(
            "Санкт-Петербург", listOf(need("rice", 800), egg)))
        assertNotEquals(key, AutomaticRetailScanPolicy.shoppingKey(
            "Санкт-Петербург", listOf(need("rice", 800), egg, need("flour", 150))))
        assertNotEquals(key, AutomaticRetailScanPolicy.shoppingKey(
            "Москва", listOf(rice, egg)))
    }

    @Test fun publicMassSkuAutomaticallyEntersWeeklyPriceWithoutAnyManualTaps() {
        val now = System.currentTimeMillis()
        val rice = need("rice", 901)
        val product = ObservedProduct(
            "Рис длиннозерный 800г",
            requireNotNull(MagnitKnownSkuCatalog.urlFor("rice")),
            800, 4999L)
        val evidence = listOf(RetailProductEvidence(
            product, "Магнит", "Санкт-Петербург", now, false))
        val offers = AutomaticRetailScanPolicy.verifiedMassOffers(
            listOf(rice), evidence, "Санкт-Петербург", 356L, now)
        assertEquals(1, offers.size)
        assertEquals("rice", offers.single().ingredientId)
        assertEquals(800, offers.single().packGrams)
        val quote = RetailerBasketPricing.estimate(
            listOf(rice), offers, "Санкт-Петербург", "Магнит", "356", now,
            indicativePriceOnly = true)
        assertEquals(2L, quote.lines.single().packages)
        assertEquals(9998L, quote.lines.single().costKopeks)
        assertEquals(0, quote.missingLines)
    }

    @Test fun countedEggCartonHasKnownPackagePriceButNeverInventsGrams() {
        val now = System.currentTimeMillis()
        val egg = need("egg", 600)
        val entry = MagnitPackOnlySkuCatalog.entries.single {
            it.ingredientId == "egg" }
        val product = ObservedProduct(
            "Куриное яйцо столовое С1 10шт", entry.url, null,
            8539L, purchasePack = entry.pack)
        val evidence = listOf(RetailProductEvidence(
            product, "Магнит", "Санкт-Петербург", now, false))
        val offers = AutomaticRetailScanPolicy.verifiedMassOffers(
            listOf(egg), evidence, "Санкт-Петербург", 356L, now)
        assertTrue(offers.isEmpty())
        val quote = RetailWeekQuoter.build(
            AnonymousRetailPlanner.build(listOf(egg)),
            evidence, "Магнит", "Санкт-Петербург", 356L, now)
        assertEquals(8539L, quote.lines.single().observedPackPriceKopeks)
        assertNull(quote.lines.single().packages)
        assertNull(quote.completeEstimateKopeks)
    }

    @Test fun invalidOrStaleSkuCannotEnterLocalWeeklyBasket() {
        val now = System.currentTimeMillis()
        val rice = need("rice", 900)
        val wrong = RetailProductEvidence(ObservedProduct(
            "Макароны 800г", "https://magnit.ru/product/unknown",
            800, 5000L), "Магнит", "Санкт-Петербург", now, false)
        val stale = RetailProductEvidence(ObservedProduct(
            "Рис длиннозерный 800г",
            requireNotNull(MagnitKnownSkuCatalog.urlFor("rice")),
            800, 4999L), "Магнит", "Санкт-Петербург",
            now - RetailerBasketPricing.MAX_INDICATIVE_AGE_MS - 1, false)
        assertTrue(AutomaticRetailScanPolicy.verifiedMassOffers(
            listOf(rice), listOf(wrong, stale), "Санкт-Петербург", 356L, now)
            .isEmpty())
    }
}
