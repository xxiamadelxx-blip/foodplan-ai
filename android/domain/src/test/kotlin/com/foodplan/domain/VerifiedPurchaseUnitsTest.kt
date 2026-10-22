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
}
