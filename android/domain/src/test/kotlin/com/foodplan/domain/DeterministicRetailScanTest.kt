package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class DeterministicRetailScanTest {
    private fun sku(id: String, price: Long, label: String? = "Рис", variety: String? = "длиннозерный",
                    processing: String? = "шлифованный", edible: Int? = 900,
                    producer: String? = "Производитель", allergensReviewed: Boolean = true) =
        RetailSkuEvidence(id, "rice", "Рис 900 г", "https://magnit.ru/product/" + id, producer,
            variety, processing, label, 900, edible, price, allergensReviewed,
            100_000L, "Магнит", "Санкт-Петербург")

    @Test fun pilotChickenBeforeWholeNeutralListAndMergeRepeatedNeed() {
        val steps = RetailScanPlanner.plan(listOf(
            NeutralGroceryNeed("rice", "крупы", listOf("рис"), 500),
            NeutralGroceryNeed("chicken", "мясо", listOf("курица"), 1500),
            NeutralGroceryNeed("rice", "крупы", listOf("рис"), 400),
            NeutralGroceryNeed("carrot", "овощи", listOf("морковь"), 0)
        ))
        assertEquals(listOf("chicken", "rice"),steps.map { it.ingredientId })
        assertEquals(listOf("PILOT", "FULL_SCAN"),steps.map { it.phase })
        assertEquals(900,steps[1].requiredGrams)
    }

    @Test fun cheapRiceWinsOnlyWhenComparable() {
        val compared = DeterministicSkuComparator.compare(sku("cheap",12000),sku("expensive",40000),900)
        assertEquals(EquivalenceStatus.EQUIVALENT,compared.equivalence)
        assertEquals("cheap",compared.recommendedSkuId)
        assertEquals(12000L,compared.firstTotalKopeks)
        assertEquals(40000L,compared.secondTotalKopeks)
    }

    @Test fun manufacturerDoesNotAutomaticallyMakeRiceSuperior() {
        val compared = DeterministicSkuComparator.compare(
            sku("a",12000,producer="Unknown maker"),sku("b",40000,producer="Popular maker"),900)
        assertEquals("a",compared.recommendedSkuId)
    }

    @Test fun differentVarietyCannotBeCalledIdentical() {
        val compared = DeterministicSkuComparator.compare(sku("a",12000),
            sku("b",40000,variety="басмати"),900)
        assertEquals(EquivalenceStatus.DIFFERENT,compared.equivalence)
        assertNull(compared.recommendedSkuId)
    }

    @Test fun unknownCompositionFailsClosed() {
        val compared = DeterministicSkuComparator.compare(sku("a",12000),
            sku("b",40000,label=null),900)
        assertEquals(EquivalenceStatus.UNKNOWN,compared.equivalence)
        assertNull(compared.recommendedSkuId)
    }

    @Test fun allergyRequiresReviewedLabelForBothSides() {
        val compared = DeterministicSkuComparator.compare(sku("a",12000),
            sku("b",40000,allergensReviewed=false),900,allergyConstraints=true)
        assertEquals(EquivalenceStatus.UNKNOWN,compared.equivalence)
    }

    @Test fun packageMathUsesWholePacksAndEdibleMass() {
        val compared = DeterministicSkuComparator.compare(sku("a",12000,edible=700),
            sku("b",40000,edible=900),900)
        assertEquals(24000L,compared.firstTotalKopeks)
        assertEquals(40000L,compared.secondTotalKopeks)
    }

    @Test fun noZeroOrUnknownPricesSilentlyMatch() {
        val compared = DeterministicSkuComparator.compare(sku("a",0),sku("b",40000),900)
        assertEquals(EquivalenceStatus.UNKNOWN,compared.equivalence)
        assertNull(compared.recommendedSkuId)
    }
}
