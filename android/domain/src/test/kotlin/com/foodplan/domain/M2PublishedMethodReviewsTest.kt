package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

/** A real published method, not the synthetic example.org fixture. */
class M2PublishedMethodReviewsTest {
    private val actualRecipe get() = OfflineRecipes.curatedDrafts.single {
        it.id == "cur_omelette_tomato"
    }

    @Test fun firstRealSourceReviewIsContentPinnedAndMathChecked() {
        val recipe = actualRecipe
        val review = M2PublishedMethodReviews.reviews.getValue(recipe.id)
        assertEquals("8f711d7e9336b2d01cfaa37731fb51d10c784f225642d5a867fd32c7fb693592",
            RecipeEvidenceAudit.fingerprint(recipe))
        assertEquals(RecipeEvidenceAudit.fingerprint(recipe), review.recipeFingerprintSha256)
        assertEquals("14731f00cdb252a84b9bc3f0114ebe767d28bae170d434cb35864c7346b0f2ce",
            RecipeEvidenceAudit.sha256Utf8(requireNotNull(review.methodSnapshotText)))
        assertEquals(review.methodSnapshotSha256,
            RecipeEvidenceAudit.sha256Utf8(requireNotNull(review.methodSnapshotText)))
        assertEquals("https://1000.menu/cooking/50028-omlet-s-pomidorami-na-skovorode",
            review.publishedMethodSource)
        // Calculated using the four pinned raw-edible food values, not the
        // publisher's cooked nutrition/portion claims.
        val raw = requireNotNull(review.independentlyRecalculatedRawNutrition)
        assertEquals(270.64, raw.kcal, 0.000001)
        assertEquals(17.196, raw.protein, 0.000001)
        assertEquals(19.5, raw.fat, 0.000001)
        assertEquals(7.732, raw.carbs, 0.000001)
        val computed = OfflineRecipes.nutrition(recipe)
        assertEquals(raw.kcal, computed.kcal, 0.000001)
        assertEquals(raw.protein, computed.protein, 0.000001)
        assertEquals(raw.fat, computed.fat, 0.000001)
        assertEquals(raw.carbs, computed.carbs, 0.000001)
        val audit = RecipeEvidenceAudit.inspect(recipe, review)
        assertTrue(audit.issues.toString(), audit.documentedReady)
        assertFalse(audit.skuLabelCleared)
        assertFalse(audit.physicalTrialDocumented)
        assertTrue(RecipeAuditNotice.RAW_INPUT_NUTRITION_APPROXIMATE in audit.notices)
        assertTrue(RecipeAuditNotice.FINISHED_YIELD_UNKNOWN in audit.notices)
    }

    @Test fun tamperingSnapshotOrCookingInstructionsRevokesTheRealReview() {
        val recipe = actualRecipe
        val review = M2PublishedMethodReviews.reviews.getValue(recipe.id)
        assertFalse(RecipeEvidenceAudit.inspect(recipe,
            review.copy(methodSnapshotText = "invented content")).documentedReady)
        assertFalse(RecipeEvidenceAudit.inspect(recipe,
            review.copy(methodSnapshotSha256 = "f".repeat(64))).documentedReady)
        val altered = recipe.copy(steps = recipe.steps.mapIndexed { index, step ->
            if (index == 1) step + " Добавьте больше молока." else step
        })
        val invalid = RecipeEvidenceAudit.inspect(altered, review)
        assertTrue(RecipeAuditIssue.DOCUMENTARY_REVIEW_STALE in invalid.issues)
        assertFalse(invalid.documentedReady)
    }

    @Test fun catalogStillRequires145AdditionalDocumentaryReviews() {
        val audit = RecipeEvidenceAudit.report()
        assertEquals(5, audit.documentedReviewed)
        assertEquals(150, audit.distinctDrafts)
        assertEquals(200, audit.generatedTemplates)
        assertEquals(0, audit.skuLabelsClearedByRecipes)
        assertFalse(audit.meetsM2Gate)
        val other = OfflineRecipes.curatedDrafts.first {
            it.id !in M2PublishedMethodReviews.reviews
        }
        assertTrue(RecipeAuditIssue.DOCUMENTARY_REVIEW_MISSING in
            RecipeEvidenceAudit.inspect(other).issues)
    }
}
