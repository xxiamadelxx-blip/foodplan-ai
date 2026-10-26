package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class M2PublishedMethodReviewBatch20261024Test {
    private data class Frozen(
        val id: String, val recipeSha: String, val capturedSha: String,
        val kcal: Double, val protein: Double, val fat: Double, val carbs: Double
    )

    // Independently pinned values from the actual authored recipes and the
    // point-identified per-100g raw ingredient source records, not computed
    // by the code under test.
    private val expected = listOf(
        Frozen("cur_scrambled_mushrooms",
            "d0d9ac77337c982360d26e4ef74373380879904bd25afda7a2dc641a75e66190",
            "3de5883d1c829e60fe9dcf81a3ab9484db0b92b82388af697576431a0d3a68fa",
            251.04,18.588,18.360,5.064),
        Frozen("cur_shakshuka",
            "4294a43da5cbe099b9561f6d3d318b4707d220de0d640fb533b68eadb2090203",
            "c41fae130a069c5a7cbcab2a7eee17e69bec3725a206a21b621185a081a2b048",
            334.32,18.598,20.797,21.869),
        Frozen("cur_lentil_tomato_soup",
            "376c8e81e73e7ebe9ba5c62439373d4a8307acd579c5de5ea9c5eb06305ed444",
            "7ecc50b8ee269c16861095e43c91155471b1afcc4d17b427a2be9e14297b18d2",
            454.38,24.0165,8.537,75.0755),
        Frozen("cur_buckwheat_mushroom",
            "87e4f0555321ad29361a294795a9f245eeea8ba1b13a9100363bfcfe0566757f",
            "16fcf5b106cb62f778b24d0de8adf874915e3d1772c50b13eaf15e044f30e44e",
            487.04,18.180,14.262,81.632)
    )

    @Test fun publishedMethodAndInputNutritionEvidenceRemainFrozenForEachDish() {
        assertEquals(4, M2PublishedMethodReviewBatch20261024.reviews.size)
        val ids = expected.map { it.id }
        assertEquals(4, ids.toSet().size)
        assertEquals(91, M2PublishedMethodReviews.reviews.size)
        for (check in expected) {
            val recipe = OfflineRecipes.curatedDrafts.single { it.id == check.id }
            val review = M2PublishedMethodReviews.reviews.getValue(check.id)
            assertEquals(check.recipeSha, RecipeEvidenceAudit.fingerprint(recipe))
            assertEquals(check.recipeSha, review.recipeFingerprintSha256)
            assertEquals(check.capturedSha,
                RecipeEvidenceAudit.sha256Utf8(requireNotNull(review.methodSnapshotText)))
            assertEquals(check.capturedSha, review.methodSnapshotSha256)
            assertTrue(review.publishedMethodSource.startsWith("https://"))
            val expectedMacro = requireNotNull(review.independentlyRecalculatedRawNutrition)
            assertEquals(check.kcal, expectedMacro.kcal, 0.0000001)
            assertEquals(check.protein, expectedMacro.protein, 0.0000001)
            assertEquals(check.fat, expectedMacro.fat, 0.0000001)
            assertEquals(check.carbs, expectedMacro.carbs, 0.0000001)
            val computed = OfflineRecipes.nutrition(recipe)
            assertEquals(check.kcal, computed.kcal, 0.00001)
            assertEquals(check.protein, computed.protein, 0.00001)
            assertEquals(check.fat, computed.fat, 0.00001)
            assertEquals(check.carbs, computed.carbs, 0.00001)
            val result = RecipeEvidenceAudit.inspect(recipe, review)
            assertTrue("${check.id}: ${result.issues}", result.documentedReady)
            assertFalse(result.physicalTrialDocumented)
            assertFalse(result.skuLabelCleared)
            assertTrue(RecipeAuditNotice.FINISHED_YIELD_UNKNOWN in result.notices)
            assertTrue(RecipeAuditNotice.RAW_INPUT_NUTRITION_APPROXIMATE in result.notices)
        }
    }

    @Test fun everyNewMethodRejectsTamperingAndStaleRawQuantities() {
        for (check in expected) {
            val recipe = OfflineRecipes.curatedDrafts.single { it.id == check.id }
            val review = M2PublishedMethodReviews.reviews.getValue(check.id)
            val tampered = review.copy(methodSnapshotText =
                review.methodSnapshotText + " unverified addition")
            assertFalse(RecipeEvidenceAudit.inspect(recipe, tampered).documentedReady)
            assertTrue(RecipeAuditIssue.METHOD_SNAPSHOT_MISMATCHED in
                RecipeEvidenceAudit.inspect(recipe, tampered).issues)
            val originalMacros = requireNotNull(review.independentlyRecalculatedRawNutrition)
            val falseMath = review.copy(
                independentlyRecalculatedRawNutrition =
                    originalMacros.copy(fat = originalMacros.fat + 4.0))
            assertFalse(RecipeEvidenceAudit.inspect(recipe, falseMath).documentedReady)
            assertTrue(RecipeAuditIssue.NUTRITION_RECALCULATION_MISMATCHED in
                RecipeEvidenceAudit.inspect(recipe, falseMath).issues)
            val stale = recipe.copy(ingredients = recipe.ingredients.mapIndexed { i, p ->
                if (i == 0) p.copy(grams = p.grams + 1) else p
            })
            assertTrue(RecipeAuditIssue.DOCUMENTARY_REVIEW_STALE in
                RecipeEvidenceAudit.inspect(stale, review).issues)
        }
    }

    @Test fun fullM2GateRemainsOpenFor75UnreviewedAuthoredRecipes() {
        val report = RecipeEvidenceAudit.report()
        assertEquals(150, report.distinctDrafts)
        assertEquals(200, report.generatedTemplates)
        assertEquals(91, report.documentedReviewed)
        assertEquals(0, report.skuLabelsClearedByRecipes)
        assertFalse(report.meetsM2Gate)
        val outstanding = OfflineRecipes.curatedDrafts
            .filterNot { it.id in M2PublishedMethodReviews.reviews }
        assertEquals(59, outstanding.size)
    }
}
