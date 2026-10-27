package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class VerifiedPurchaseUnitsTest {
    @Test fun gramsPackNormally() {
        assertEquals(2, VerifiedPurchaseUnits.packs(901, PurchasePack(PurchaseUnit.GRAM, 900)))
    }
    @Test fun unknownEggAndOilConversionsAreNotPrices() {
        assertNull(VerifiedPurchaseUnits.packs(600, PurchasePack(PurchaseUnit.PIECE, 10)))
        assertNull(VerifiedPurchaseUnits.packs(901, PurchasePack(PurchaseUnit.MILLILITRE, 900)))
    }
    @Test fun explicitEvidenceCanConvertUnits() {
        val proof = UnitConversionEvidence(50.0, "https://fdc.nal.usda.gov/")
        assertEquals(2, VerifiedPurchaseUnits.packs(600, PurchasePack(PurchaseUnit.PIECE, 10), proof))
    }
    @Test fun zeroNeedRequiresNoPacks() {
        assertEquals(0, VerifiedPurchaseUnits.packs(0, PurchasePack(PurchaseUnit.PIECE, 10)))
    }

    @Test fun bananasWithPeelAndCannedChickpeasWithBrineRemainUnknown() {
        val bananas = PurchasePack(PurchaseUnit.UNPEELED_GRAM, 1000)
        val chickpeas = PurchasePack(PurchaseUnit.NET_WITH_LIQUID_GRAM, 400)
        assertNull(VerifiedPurchaseUnits.packs(550, bananas))
        assertNull(VerifiedPurchaseUnits.packs(240, chickpeas))
        // Merely supplying an HTTPS URL is not per-SKU edible yield evidence.
        val unsupported = UnitConversionEvidence(0.65, "https://example.org/not-a-sku-proof")
        assertNull(VerifiedPurchaseUnits.packs(550, bananas, unsupported))
        assertNull(VerifiedPurchaseUnits.packs(240, chickpeas, unsupported))
        assertEquals(0, VerifiedPurchaseUnits.packs(0, bananas))
    }

    @Test fun officialGrossSkuTitlesMustMatchExactUrlWeightAndProductName() {
        fun item(id: String, name: String) : ObservedProduct {
            val entry = MagnitPackOnlySkuCatalog.entries.single { it.ingredientId == id }
            return ObservedProduct(name, entry.url, null, 5999L,
                purchasePack = entry.pack)
        }
        assertNotNull(MagnitPackOnlySkuCatalog.matching(item("banana", "Бананы 1кг")))
        assertNotNull(MagnitPackOnlySkuCatalog.matching(
            item("chickpeas", "Нут Green Ribbon 400г")))
        assertNull(MagnitPackOnlySkuCatalog.matching(item("banana", "Бананы 2кг")))
        assertNull(MagnitPackOnlySkuCatalog.matching(
            item("chickpeas", "Нут Green Ribbon 200г")))
        assertNull(MagnitPackOnlySkuCatalog.matching(item(
            "chickpeas", "Фасоль Green Ribbon 400г")))
        assertNull(MagnitPackOnlySkuCatalog.matching(
            item("banana", "Бананы 1кг").copy(productUrl =
                "https://magnit.ru/product/unrelated")))
    }
}
