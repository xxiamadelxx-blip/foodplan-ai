package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

/**
 * Synthetic review records exercise the algorithm, NOT approval of any real
 * cookbook page. The actual 150 drafted dishes still have zero vetted reviews.
 */
class RecipeDocumentaryGateTest {
    private val draft get() = OfflineRecipes.curatedDrafts.first()
    // Synthetic capture only. A matching hash is an integrity test, never
    // independent proof that example.org publishes this method.
    private val sourceCapture = "Test fixture only: whisk eggs with milk, heat tomatoes " +
        "in a skillet with oil, pour the egg mixture over them, cover the pan and " +
        "cook until fully set. This is not a real external publication."

    private fun example(
        recipe: OfflineRecipe = draft,
        basis: RecipeNutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE
    ) = RecipeDocumentaryReview(
        recipeId = recipe.id,
        recipeFingerprintSha256 = RecipeEvidenceAudit.fingerprint(recipe),
        publishedMethodSource = "https://example.org/recipes/method",
        methodSnapshotSha256 = RecipeEvidenceAudit.sha256Utf8(sourceCapture),
        methodSnapshotText = sourceCapture,
        methodApplicabilityNotes = "Fixture comparison of the same cooking steps and ingredients.",
        reviewRecordReference = "https://example.org/qa/checked-method",
        nutritionBasis = basis,
        nutritionCalculationNotes = "Sum of edible dry/raw grams times cited food values per 100 g.",
        independentlyRecalculatedRawNutrition = OfflineRecipes.nutrition(recipe)
    )

    @Test fun acceptedDocumentaryMethodNeedsNoRealCookedMeasurementOrSkuLabel() {
        val recipe = draft
        assertNull(recipe.cookedYieldGrams)
        val audit = RecipeEvidenceAudit.inspect(recipe, example(recipe))
        assertTrue(audit.issues.toString(), audit.documentedReady)
        assertTrue(audit.methodDocumented)
        assertTrue(audit.nutritionCalculationDocumented)
        assertFalse(audit.physicalTrialDocumented)
        assertFalse(audit.skuLabelCleared)
        assertTrue(RecipeAuditNotice.COOKED_YIELD_NOT_MEASURED in audit.notices)
        assertTrue(RecipeAuditNotice.FINISHED_YIELD_UNKNOWN in audit.notices)
        assertTrue(RecipeAuditNotice.RAW_INPUT_NUTRITION_APPROXIMATE in audit.notices)
        assertTrue(RecipeAuditNotice.SKU_LABEL_TRACES_UNKNOWN in audit.notices)
        assertFalse(RecipeAuditIssue.UNVERIFIED_COOKED_YIELD in audit.issues)
        val report = RecipeEvidenceAudit.report(
            listOf(recipe), mapOf(recipe.id to example(recipe)))
        assertEquals(1, report.documentedReviewed)
        assertEquals(0, report.physicalTrialsRecorded)
        assertEquals(0, report.skuLabelsClearedByRecipes)
        assertEquals(0, report.independentlyVerified)
        assertFalse(report.meetsM2Gate) // one fixture is not 150 real reviews
    }

    @Test fun arbitraryHexHashOrTamperedSourceExcerptNeverCountsAsEvidence() {
        val recipe = draft
        val arbitrary = example(recipe).copy(methodSnapshotSha256 = "a".repeat(64))
        val tampered = example(recipe).copy(
            methodSnapshotText = sourceCapture.replace("tomatoes", "potatoes"))
        val absent = example(recipe).copy(methodSnapshotText = null)
        val tooShort = example(recipe).copy(methodSnapshotText = "eggs, tomatoes")
        for (case in listOf(arbitrary, tampered, absent, tooShort)) {
            val audit = RecipeEvidenceAudit.inspect(recipe, case)
            assertFalse("A fabricated or missing snapshot must fail", audit.documentedReady)
            assertTrue(RecipeAuditIssue.METHOD_SNAPSHOT_MISMATCHED in audit.issues)
        }
        // Explicitly distinguish a syntactically well-formed hash from
        // cryptographic agreement with actual captured text.
        assertEquals(64, example(recipe).methodSnapshotSha256.length)
        assertTrue(RecipeEvidenceAudit.inspect(recipe, example(recipe)).documentedReady)
    }

    @Test fun changedIngredientsOrStepsInvalidatePreviousDocumentaryReview() {
        val recipe = draft
        val approvedInFixture = example(recipe)
        for (changed in listOf(
            recipe.copy(ingredients = recipe.ingredients.mapIndexed { idx, item ->
                if (idx == 0) item.copy(grams = item.grams + 1) else item
            }),
            recipe.copy(steps = recipe.steps + "Add a new preparation step."),
            recipe.copy(preparation = recipe.preparation.copy(totalMinutes =
                recipe.preparation.totalMinutes + 1))
        )) {
            val audit = RecipeEvidenceAudit.inspect(changed, approvedInFixture)
            assertFalse(audit.documentedReady)
            assertTrue(RecipeAuditIssue.DOCUMENTARY_REVIEW_STALE in audit.issues)
        }
    }

    @Test fun candidateLinksAndSelfVerifiedBooleanWithoutReviewNeverPass() {
        val recipe = draft.copy(
            nutritionVerified = true,
            sourceReference = "https://example.org/recipes/method",
            evidenceStatus = RecipeEvidenceStatus.INDEPENDENTLY_VERIFIED
        )
        val audit = RecipeEvidenceAudit.inspect(recipe)
        assertFalse(audit.documentedReady)
        assertTrue(RecipeAuditIssue.DOCUMENTARY_REVIEW_MISSING in audit.issues)
        assertFalse(audit.skuLabelCleared)
    }

    @Test fun invalidPublishedSourcesAndCookingFactorsFailClosed() {
        val recipe = draft
        val badUrl = example(recipe).copy(publishedMethodSource = "https://example.org")
        val badHash = example(recipe).copy(methodSnapshotSha256 = "placeholder")
        val badNotes = example(recipe).copy(methodApplicabilityNotes = "similar")
        val badReview = example(recipe).copy(reviewRecordReference = "http://example.org/qa")
        for (review in listOf(badUrl, badHash, badNotes, badReview)) {
            assertFalse(RecipeEvidenceAudit.inspect(recipe, review).documentedReady)
        }
        val badFactor = example(recipe, RecipeNutritionBasis.INGREDIENT_SPECIFIC_PUBLISHED_FACTORS)
        assertTrue(RecipeAuditIssue.COOKING_FACTORS_UNSOURCED in
            RecipeEvidenceAudit.inspect(recipe, badFactor).issues)
        val correctIngredient = recipe.ingredients.first().id
        val withCitedFactor = badFactor.copy(
            publishedCookingFactorSources = mapOf(
                correctIngredient to "https://example.org/research/retention-tables"))
        assertTrue(RecipeEvidenceAudit.inspect(recipe, withCitedFactor).documentedReady)
        val unrelated = badFactor.copy(
            publishedCookingFactorSources = mapOf(
                "not_an_ingredient" to "https://example.org/research/retention-tables"))
        assertFalse(RecipeEvidenceAudit.inspect(recipe, unrelated).documentedReady)
    }

    @Test fun independentArithmeticCannotBeReplacedByProseOrWrongNumbers() {
        val recipe = draft
        val withoutNumbers = example(recipe).copy(
            independentlyRecalculatedRawNutrition = null)
        val wronglyRecalculated = example(recipe).copy(
            independentlyRecalculatedRawNutrition =
                OfflineRecipes.nutrition(recipe).copy(kcal = 0.0))
        for (review in listOf(withoutNumbers, wronglyRecalculated)) {
            val audit = RecipeEvidenceAudit.inspect(recipe, review)
            assertFalse(audit.documentedReady)
            assertTrue(RecipeAuditIssue.NUTRITION_RECALCULATION_MISMATCHED in audit.issues)
        }
    }

    @Test fun optionalEstimatedYieldCannotMasqueradeAsMeasuredCookedMass() {
        val recipe = draft
        val invalid = example(recipe).copy(estimatedFinishedYieldGrams = 500)
        assertTrue(RecipeAuditIssue.INVALID_ESTIMATED_YIELD in
            RecipeEvidenceAudit.inspect(recipe, invalid).issues)
        val estimate = example(recipe).copy(
            estimatedFinishedYieldGrams = 500,
            estimatedYieldSourceReference = "https://example.org/research/published-yields"
        )
        val audited = RecipeEvidenceAudit.inspect(recipe, estimate)
        assertTrue(audited.documentedReady)
        assertNull(recipe.cookedYieldGrams)
        assertFalse(audited.physicalTrialDocumented)
        assertFalse(audited.skuLabelCleared)
    }

    @Test fun generatedTemplatesCannotCountAsReviewedAuthoredDishes() {
        val prototype = OfflineRecipes.generatedPrototypes.first()
        val reviewed = RecipeEvidenceAudit.inspect(prototype, example(prototype))
        assertFalse(reviewed.documentedReady)
        assertTrue(RecipeAuditIssue.UNVERIFIED_RECIPE in reviewed.issues)
        assertEquals(0, RecipeEvidenceAudit.report(
            listOf(prototype), mapOf(prototype.id to example(prototype))
        ).documentedReviewed)
    }

    @Test fun independentlyReviewedFiveMethodsDoNotPromoteRemaining145Drafts() {
        val report = RecipeEvidenceAudit.report()
        assertEquals(350, report.total)
        assertEquals(150, report.distinctDrafts)
        assertEquals(5, report.documentedReviewed)
        assertEquals(0, report.physicalTrialsRecorded)
        assertFalse(report.meetsM2Gate)
    }
}
