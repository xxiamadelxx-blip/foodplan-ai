package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class AllergenLabelEvidenceTest {
    private val page="https://magnit.ru/product/example-sku"
    private val productSku="TEST-SKU-ONLY"
    private fun synthetic(
        url:String=page,
        sku:String=productSku,
        components:Set<DeclaredComponent> = emptySet(),
        traces:Set<DeclaredComponent> = emptySet(),
        timestamp:Long=100000L,
        inspected:Boolean=true
    ) = ProductLabelEvidence(
        productUrl=url,sku=sku,
        labelImageSource="https://example.org/test-label.jpg",
        labelSha256="a".repeat(64),reviewedBy="synthetic-test-reviewer",
        capturedAtEpochMs=timestamp,declaredComponents=components,
        possibleTraces=traces,compositionAndTracesInspected=inspected
    )

    @Test fun regulatedComponentListHasAll15CategoriesIncludingSpecialCases() {
        assertEquals(15,DeclaredComponent.entries.size)
        assertTrue(DeclaredComponent.ASPARTAME_PHENYLALANINE in DeclaredComponent.entries)
        assertTrue(DeclaredComponent.SULPHITES_OVER_THRESHOLD in DeclaredComponent.entries)
        assertTrue(DeclaredComponent.LUPIN in DeclaredComponent.entries)
        assertTrue(DeclaredComponent.MOLLUSCS in DeclaredComponent.entries)
    }

    @Test fun declaredIngredientAlwaysOverridesAnyPriceOrReviewScore() {
        val result=AllergenLabelEvidence.assess(page,productSku,setOf(DeclaredComponent.MILK),
            synthetic(components=setOf(DeclaredComponent.MILK)),110000L)
        assertEquals(LabelDisclosureStatus.DECLARED_IN_INGREDIENTS,result.status)
        assertEquals(setOf(DeclaredComponent.MILK),result.matched)
        assertFalse(result.medicallyCertifiedSafe)
    }

    @Test fun crossContactIsNotMistakenForNoAllergen() {
        val result=AllergenLabelEvidence.assess(page,productSku,setOf(DeclaredComponent.PEANUT),
            synthetic(traces=setOf(DeclaredComponent.PEANUT)),110000L)
        assertEquals(LabelDisclosureStatus.POSSIBLE_CROSS_CONTACT,result.status)
    }

    @Test fun missingOrWrongProductEvidenceNeverClearsARestriction() {
        val requested=setOf(DeclaredComponent.SESAME)
        val missing=AllergenLabelEvidence.assess(page,productSku,requested,null,110000L)
        val unrelated=AllergenLabelEvidence.assess(page,productSku,requested,
            synthetic(url="https://magnit.ru/product/another-sku"),110000L)
        val notInspected=AllergenLabelEvidence.assess(page,productSku,requested,
            synthetic(inspected=false),110000L)
        assertEquals(LabelDisclosureStatus.MISSING_OR_UNVERIFIED_LABEL,missing.status)
        assertEquals(missing.status,unrelated.status)
        assertEquals(missing.status,notInspected.status)
        val wrongSku=AllergenLabelEvidence.assess(page,productSku,requested,
            synthetic(sku="OTHER-ARTICLE"),110000L)
        val blankRequestedSku=AllergenLabelEvidence.assess(page,"",requested,
            synthetic(),110000L)
        assertEquals(missing.status,wrongSku.status)
        assertEquals(missing.status,blankRequestedSku.status)
    }

    @Test fun staleAndFutureEvidenceFailsClosed() {
        val now=AllergenLabelEvidence.MAX_LABEL_AGE_MS + 300000L
        assertEquals(LabelDisclosureStatus.MISSING_OR_UNVERIFIED_LABEL,
            AllergenLabelEvidence.assess(page,productSku,setOf(DeclaredComponent.EGGS),
                synthetic(timestamp=100000L),now).status)
        assertEquals(LabelDisclosureStatus.MISSING_OR_UNVERIFIED_LABEL,
            AllergenLabelEvidence.assess(page,productSku,setOf(DeclaredComponent.EGGS),
                synthetic(timestamp=now+1),now).status)
    }

    @Test fun noFlagInReviewedLabelIsNotAClaimOfMedicalSafety() {
        val result=AllergenLabelEvidence.assess(page,productSku,setOf(DeclaredComponent.SOY),
            synthetic(),110000L)
        assertEquals(LabelDisclosureStatus.NO_MATCHING_DECLARATION_ON_REVIEWED_LABEL,result.status)
        assertFalse(result.medicallyCertifiedSafe)
    }
}
