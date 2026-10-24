package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class M2PublishedMethodReviewBatch3_20261024Test {
    private data class Expected(
        val id: String, val recipeSha: String, val excerptSha: String,
        val kcal: Double, val protein: Double, val fat: Double, val carbs: Double
    )

    // Frozen independently calculated evidence. Never generate these SHA
    // expectations or nutrients by calling the code under test.
    private val checks = listOf(
        Expected("cur_oat_pancakes",
            "18f82cf2b6ad8865df6499d99f06841c6d5c83f97a89242da522771dbccce179",
            "93bc08b734f88835aa302757b18947c8e92eaae08e008aeda9fd306719cb5917",
            500.77, 17.1865, 18.577, 69.705),
        Expected("cur_mushroom_pasta",
            "52160b8976d3f5f49ecf6bc626aece5d818ef31794844ae3ab465c4ab5ee45b7",
            "ce90189ec5572e856107d271838671269f687f4cb704a5a76dc1768146d432dd",
            504.31, 19.3454, 12.3576, 82.047),
        Expected("cur_rice_egg_veg",
            "df28e9b221b20192f3d2884d27b0573d77f678c625dc59a203c11c9e725441dc",
            "5a5030ac804b3c83a63e306de294a80e103e1707d3c994df3b5d7913ebe89156",
            611.164, 23.44908, 22.4404, 82.10552)
    )

    @Test fun threePublishedMethodsHaveFrozenRawArithmeticAndCapturedEvidence() {
        assertEquals(3, M2PublishedMethodReviewBatch3_20261024.reviews.size)
        assertEquals(8, M2PublishedMethodReviews.reviews.size)
        assertEquals(3, checks.map { it.id }.distinct().size)
        for (expected in checks) {
            val recipe = OfflineRecipes.curatedDrafts.single { it.id == expected.id }
            val review = M2PublishedMethodReviews.reviews.getValue(expected.id)
            assertEquals(expected.recipeSha, RecipeEvidenceAudit.fingerprint(recipe))
            assertEquals(expected.recipeSha, review.recipeFingerprintSha256)
            assertEquals(expected.excerptSha, review.methodSnapshotSha256)
            assertEquals(expected.excerptSha,
                RecipeEvidenceAudit.sha256Utf8(requireNotNull(review.methodSnapshotText)))
            assertTrue(review.publishedMethodSource.startsWith("https://"))
            assertEquals(RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE, review.nutritionBasis)
            val raw = requireNotNull(review.independentlyRecalculatedRawNutrition)
            val actual = OfflineRecipes.nutrition(recipe)
            assertEquals("${expected.id} recorded kcal", expected.kcal, raw.kcal, 0.000001)
            assertEquals("${expected.id} recorded protein", expected.protein, raw.protein, 0.000001)
            assertEquals("${expected.id} recorded fat", expected.fat, raw.fat, 0.000001)
            assertEquals("${expected.id} recorded carbs", expected.carbs, raw.carbs, 0.000001)
            assertEquals("${expected.id} generated kcal", expected.kcal, actual.kcal, 0.00001)
            assertEquals("${expected.id} generated protein", expected.protein, actual.protein, 0.00001)
            assertEquals("${expected.id} generated fat", expected.fat, actual.fat, 0.00001)
            assertEquals("${expected.id} generated carbs", expected.carbs, actual.carbs, 0.00001)
            val audit = RecipeEvidenceAudit.inspect(recipe, review)
            assertTrue("${expected.id}: ${audit.issues}", audit.documentedReady)
            assertFalse(audit.physicalTrialDocumented)
            assertFalse(audit.skuLabelCleared)
            assertTrue(RecipeAuditNotice.FINISHED_YIELD_UNKNOWN in audit.notices)
            assertTrue(RecipeAuditNotice.RAW_INPUT_NUTRITION_APPROXIMATE in audit.notices)
        }
    }

    @Test fun alteredRecipeExcerptOrMacrosRevokesEachReview() {
        for (expected in checks) {
            val recipe = OfflineRecipes.curatedDrafts.single { it.id == expected.id }
            val evidence = M2PublishedMethodReviews.reviews.getValue(expected.id)
            val alteredExcerpt = evidence.copy(
                methodSnapshotText = requireNotNull(evidence.methodSnapshotText) + " Altered.")
            assertTrue(RecipeAuditIssue.METHOD_SNAPSHOT_MISMATCHED in
                RecipeEvidenceAudit.inspect(recipe, alteredExcerpt).issues)
            assertFalse(RecipeEvidenceAudit.inspect(recipe, alteredExcerpt).documentedReady)
            val stale = recipe.copy(ingredients = recipe.ingredients.mapIndexed { i, ingredient ->
                if (i == 0) ingredient.copy(grams = ingredient.grams + 3) else ingredient
            })
            assertTrue(RecipeAuditIssue.DOCUMENTARY_REVIEW_STALE in
                RecipeEvidenceAudit.inspect(stale, evidence).issues)
            val numbers = requireNotNull(evidence.independentlyRecalculatedRawNutrition)
            val alteredMath = evidence.copy(independentlyRecalculatedRawNutrition =
                numbers.copy(kcal = numbers.kcal + 10.0))
            assertTrue(RecipeAuditIssue.NUTRITION_RECALCULATION_MISMATCHED in
                RecipeEvidenceAudit.inspect(recipe, alteredMath).issues)
        }
    }

    @Test fun chickpeaLemonJuiceAndFastYogurtDraftsStayUnreviewed() {
        for (id in listOf("cur_chickpea_salad", "cur_yogurt_berries_oat")) {
            assertFalse(M2PublishedMethodReviews.reviews.containsKey(id))
            val recipe = OfflineRecipes.curatedDrafts.single { it.id == id }
            assertFalse(RecipeEvidenceAudit.inspect(recipe).documentedReady)
        }
        val report = RecipeEvidenceAudit.report()
        assertEquals(8, report.documentedReviewed)
        assertEquals(142, OfflineRecipes.curatedDrafts.count {
            it.id !in M2PublishedMethodReviews.reviews
        })
        assertFalse(report.meetsM2Gate)
    }
}
