package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class DeterministicRetailAuditTest {
    private val now = 1_000_000L
    private fun query(id: String, grams: Int = 900): AnonymousRetailQuery =
        AnonymousRetailPlanner.build(
            listOf(ShoppingNeed(OfflineRecipes.ingredients.getValue(id), grams, 1))
        ).single()

    private fun product(
        name: String, priceRub: Int, path: String, grams: Int = 1000,
        spec: String? = "Рис длиннозерный белый шлифованный",
        ingredients: String? = "Рис шлифованный"
    ) = RetailProductEvidence(
        ObservedProduct(name, "https://magnit.ru/product/$path",
            grams, priceRub * 100L), "Магнит", "Санкт-Петербург", now, true,
        manufacturer = "Производитель $path", ingredientsText = ingredients,
        identitySpec = spec, labelSourceUrl = "https://magnit.ru/product/$path",
        labelChecked = true
    )

    @Test fun anonymousQueryDoesNotContainProfileOrAddress() {
        assertEquals(OfflineRecipes.ingredients.keys, AnonymousRetailPlanner.coverage())
        val needs = listOf(
            ShoppingNeed(OfflineRecipes.ingredients.getValue("chicken"), 800, 1),
            ShoppingNeed(OfflineRecipes.ingredients.getValue("rice"), 900, 1),
            ShoppingNeed(OfflineRecipes.ingredients.getValue("carrot"), 300, 1)
        )
        val result = AnonymousRetailPlanner.build(needs)
        assertEquals(3, result.size)
        assertTrue(result.any { it.ingredientId == "chicken" &&
            it.searchTerms.first() == "филе грудки куриное" })
        assertTrue(result.any { it.ingredientId == "rice" &&
            it.searchTerms.first() == "рис длиннозерный" })
        assertTrue(result.any { it.ingredientId == "carrot" &&
            it.genericName == "морковь" })
        assertFalse(result.toString().contains("Санкт-Петербург"))
    }

    @Test fun sameVerifiedRiceSpecDoesNotJustifyBrandPremium() {
        val rice = query("rice")
        val cheap = product("Рис длиннозерный шлифованный 1кг", 120, "cheap-rice")
        val costly = product("Рис длиннозерный шлифованный 1кг", 400, "costly-rice")
        val decision = DeterministicRetailAuditor.audit(
            rice, listOf(costly, cheap), "Магнит", "Санкт-Петербург", now
        )
        assertEquals(2, decision.candidates.count { it.status == RetailAuditStatus.COMPARABLE })
        assertTrue(decision.alternativesEquivalentOnLabels)
        assertEquals("cheap-rice", decision.cheapestComparable!!.evidence.product.productUrl.substringAfterLast('/'))
        assertEquals(12000L, decision.cheapestComparable!!.weeklyCostKopeks)
        assertEquals(120.0, decision.cheapestComparable!!.rublesPerKg ?: -1.0, 0.001)
    }

    @Test fun noLabelOrManufacturerMeansNeverDeclareEqual() {
        val withoutManufacturer = product("Рис длиннозерный 1кг", 120, "no-label")
            .copy(manufacturer = null, labelChecked = false, ingredientsText = null)
        val premium = product("Рис длиннозерный шлифованный 1кг", 400, "premium")
        val result = DeterministicRetailAuditor.audit(query("rice"),
            listOf(withoutManufacturer, premium), "Магнит", "Санкт-Петербург", now)
        assertFalse(result.alternativesEquivalentOnLabels)
        assertEquals(1, result.candidates.count { it.status == RetailAuditStatus.NEEDS_PRODUCT_DETAILS })
    }

    @Test fun chickenThighCannotReplaceBreastFillet() {
        val thigh = product("Бедро куриное 1кг", 170, "thigh", 
            spec = "Бедро куриное", ingredients = "Куриное бедро")
        val breast = product("Филе куриное грудки 1кг", 300, "breast",
            spec = "Филе грудки куриное сырое без кожи и костей", 
            ingredients = "Филе грудки куриное")
        val result = DeterministicRetailAuditor.audit(query("chicken"),
            listOf(thigh, breast), "Магнит", "Санкт-Петербург", now)
        assertEquals(RetailAuditStatus.REJECTED, result.candidates[0].status)
        assertEquals(RetailAuditStatus.COMPARABLE, result.candidates[1].status)
        assertEquals(breast.product.productUrl, result.cheapestComparable!!.evidence.product.productUrl)
    }

    @Test fun incompatibleRiceVarietyNotPriceAlternative() {
        val round = product("Рис круглозерный 1кг", 100, "round", 
            spec = "Рис круглозерный шлифованный")
        val long = product("Рис длиннозерный шлифованный 1кг", 120, "long")
        val result = DeterministicRetailAuditor.audit(query("rice"),
            listOf(round, long), "Магнит", "Санкт-Петербург", now)
        assertEquals(RetailAuditStatus.REJECTED, result.candidates[0].status)
        assertEquals(long.product.productUrl, result.cheapestComparable!!.evidence.product.productUrl)
    }

    @Test fun staleAddressMismatchAndNoPackPriceFailClosed() {
        val good = product("Рис длиннозерный 1кг", 120, "r")
        val invalid = listOf(
            good.copy(city = "Москва"),
            good.copy(observedAtEpochMs = now - 7L * 60 * 60 * 1000),
            good.copy(product = good.product.copy(packGrams = 500)),
            good.copy(product = good.product.copy(availability = ListingAvailability.OUT_OF_STOCK)),
            good.copy(product = good.product.copy(productUrl = "https://magnit.ru@evil.test/product/r"))
        )
        invalid.forEach { candidate ->
            val out = DeterministicRetailAuditor.audit(query("rice"), listOf(candidate),
                "Магнит", "Санкт-Петербург", now)
            assertEquals(candidate.toString(), RetailAuditStatus.REJECTED, out.candidates.single().status)
            assertNull(out.cheapestComparable)
        }
    }

    @Test fun exactLabelAllergensAreSeparateGate() {
        val entry = product("Рис длиннозерный 1кг", 120, "label")
        val result = DeterministicRetailAuditor.audit(query("rice"), listOf(entry),
            "Магнит", "Санкт-Петербург", now, strictAllergenVerification = true)
        assertEquals(RetailAuditStatus.NEEDS_PRODUCT_DETAILS, result.candidates.single().status)
        assertNull(result.cheapestComparable)
        val allowed = DeterministicRetailAuditor.audit(query("rice"),
            listOf(entry.copy(explicitAllergenReview = true)), "Магнит",
            "Санкт-Петербург", now, strictAllergenVerification = true)
        assertEquals(RetailAuditStatus.COMPARABLE, allowed.candidates.single().status)
    }

    @Test fun perKgStickerCannotPretendToBeOneKiloPackage() {
        val variable = product("Филе куриное грудки 1кг", 300, "variable",
            spec = "Филе грудки куриное сырое без кожи и костей",
            ingredients = "Филе куриное").copy(soldByWeight = true)
        val r = DeterministicRetailAuditor.audit(query("chicken"),
            listOf(variable), "Магнит", "Санкт-Петербург", now)
        assertEquals(RetailAuditStatus.REJECTED, r.candidates.single().status)
    }
}
