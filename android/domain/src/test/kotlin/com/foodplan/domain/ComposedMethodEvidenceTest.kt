package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

/**
 * The step-based published technique gate is fail-closed, and a plausible
 * looking HTTPS URL must NOT be a substituted food-safety authority.
 */
class ComposedMethodEvidenceTest {
    private val dish = OfflineRecipes.curatedDrafts.single {
        it.id == "b4_savory_carrot_oat_bake"
    }
    private val accepted = M2PublishedMethodComposed20261026.reviews
        .getValue("b4_savory_carrot_oat_bake")

    @Test fun independentTechniqueEvidenceSurvivesWithoutExactDishTitleCopy() {
        val outcome = RecipeEvidenceAudit.inspect(dish, accepted)
        assertTrue("Technique review issues: ${outcome.issues}", outcome.documentedReady)
        assertEquals(RecipeMethodBasis.COMPOSED_PUBLISHED_TECHNIQUES, accepted.methodBasis)
        assertEquals(dish.steps.size, accepted.composedTechniqueSteps.size)
        assertFalse(outcome.skuLabelCleared)
        assertFalse(outcome.physicalTrialDocumented)
    }

    @Test fun counterfeitHttpsSafetySourceMustNotSatisfyTemperatureProof() {
        for (url in listOf(
            "https://example.org/food-safety-chart",
            "https://fsis.usda.gov.evil.example/safe-temperature-chart"
        )) {
            val forged = accepted.copy(safeTemperatureSource = url)
            val result = RecipeEvidenceAudit.inspect(dish, forged)
            assertTrue("Forged safety authority accepted: $url",
                RecipeAuditIssue.COMPOSED_TEMPERATURE_UNSUPPORTED in result.issues)
            assertFalse(result.documentedReady)
        }
    }

    @Test fun authoredStepHashAndExcerptIntegrityCannotBeReusedAfterModification() {
        val originallyReviewed = accepted.composedTechniqueSteps.first()
        val changedAuthoredText = dish.copy(steps = dish.steps.toMutableList().also {
            it[0] = it[0] + " Independent alteration."
        })
        assertFalse(RecipeEvidenceAudit.inspect(changedAuthoredText, accepted).documentedReady)
        val badExcerpt = accepted.copy(composedTechniqueSteps =
            listOf(originallyReviewed.copy(sourceMethodExcerpt =
                originallyReviewed.sourceMethodExcerpt + " fabricated")) +
                accepted.composedTechniqueSteps.drop(1))
        assertTrue(RecipeAuditIssue.COMPOSED_TECHNIQUE_EVIDENCE_INVALID in
            RecipeEvidenceAudit.inspect(dish, badExcerpt).issues)
    }

    @Test fun compositionCannotFallBackToLegacySingleLinkGate() {
        val hidden = accepted.copy(methodBasis = RecipeMethodBasis.SINGLE_PUBLISHED_METHOD)
        assertFalse(RecipeEvidenceAudit.inspect(dish, hidden).documentedReady)
        val missing = accepted.copy(composedTechniqueSteps =
            accepted.composedTechniqueSteps.dropLast(1))
        assertTrue(RecipeAuditIssue.COMPOSED_TECHNIQUE_COVERAGE_MISSING in
            RecipeEvidenceAudit.inspect(dish, missing).issues)
    }
}
