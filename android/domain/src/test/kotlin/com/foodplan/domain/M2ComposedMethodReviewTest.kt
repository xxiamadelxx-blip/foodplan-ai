package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

/**
 * RED/GREEN: independent source-method composition must verify EACH authored
 * preparation step, not merely a matching dish title or a publisher URL.
 */
class M2ComposedMethodReviewTest {
    private val ids = listOf("b4_savory_carrot_oat_bake", "b4_buckwheat_apple_cups")

    @Test fun twoIndependentlySourcedCompositionsNeedEveryPinnedPreparationStep() {
        assertEquals(2, M2PublishedMethodComposed20261026.reviews.size)
        assertEquals(89, M2PublishedMethodReviews.reviews.size)
        for (id in ids) {
            val dish = OfflineRecipes.curatedDrafts.single { it.id == id }
            val review = M2PublishedMethodReviews.reviews.getValue(id)
            assertEquals(RecipeMethodBasis.COMPOSED_PUBLISHED_TECHNIQUES, review.methodBasis)
            assertEquals(RecipeEvidenceAudit.fingerprint(dish), review.recipeFingerprintSha256)
            assertEquals(dish.steps.size, review.composedTechniqueSteps.size)
            assertEquals((1..dish.steps.size).toList(),
                review.composedTechniqueSteps.map { it.stepNumber }.sorted())
            assertTrue(review.composedTechniqueSteps.map { it.publishedMethodSource }.toSet().size >= 2)
            for (step in review.composedTechniqueSteps) {
                assertEquals(RecipeEvidenceAudit.sha256Utf8(dish.steps[step.stepNumber - 1]),
                    step.authoredStepSha256)
                assertEquals(RecipeEvidenceAudit.sha256Utf8(step.sourceMethodExcerpt),
                    step.sourceExcerptSha256)
            }
            assertEquals(74, review.requiredCoreTemperatureC)
            assertTrue(RecipeEvidenceAudit.inspect(dish, review).documentedReady)
            assertFalse(RecipeEvidenceAudit.inspect(dish, review).skuLabelCleared)
            assertFalse(RecipeEvidenceAudit.inspect(dish, review).physicalTrialDocumented)
        }
    }

    @Test fun missingDuplicatedOrForgedIndividualStepsFailClosed() {
        for (id in ids) {
            val dish = OfflineRecipes.curatedDrafts.single { it.id == id }
            val valid = M2PublishedMethodComposed20261026.reviews.getValue(id)
            val head = valid.composedTechniqueSteps.first()
            val variants = listOf(
                valid.copy(composedTechniqueSteps = valid.composedTechniqueSteps.dropLast(1)),
                valid.copy(composedTechniqueSteps =
                    valid.composedTechniqueSteps.dropLast(1) + head),
                valid.copy(composedTechniqueSteps =
                    valid.composedTechniqueSteps.mapIndexed { i, item ->
                        if (i == 0) item.copy(authoredStepSha256 = "0".repeat(64)) else item
                    }),
                valid.copy(composedTechniqueSteps =
                    valid.composedTechniqueSteps.mapIndexed { i, item ->
                        if (i == 0) item.copy(sourceMethodExcerpt = item.sourceMethodExcerpt + " forged") else item
                    }),
                valid.copy(composedTechniqueSteps =
                    valid.composedTechniqueSteps.mapIndexed { i, item ->
                        if (i == 0) item.copy(publishedMethodSource = "https://example.org") else item
                    }),
                valid.copy(composedTechniqueSteps =
                    valid.composedTechniqueSteps.mapIndexed { i, item ->
                        if (i == 0) item.copy(coveredIngredientIds = setOf("made_up")) else item
                    }),
                valid.copy(composedTechniqueSteps = emptyList())
            )
            for (bad in variants) {
                assertFalse("$id: bad composed source steps admitted",
                    RecipeEvidenceAudit.inspect(dish, bad).documentedReady)
            }
        }
    }

    @Test fun coreTemperatureAndSafetySourceMustStayAuditable() {
        for (id in ids) {
            val dish = OfflineRecipes.curatedDrafts.single { it.id == id }
            val good = M2PublishedMethodComposed20261026.reviews.getValue(id)
            for (bad in listOf(
                good.copy(requiredCoreTemperatureC = null),
                good.copy(requiredCoreTemperatureC = 65),
                good.copy(safeTemperatureSource = null),
                good.copy(safeTemperatureSource = "https://example.org")
            )) {
                val audit = RecipeEvidenceAudit.inspect(dish, bad)
                assertFalse("$id: egg casserole without safety basis", audit.documentedReady)
                assertTrue(RecipeAuditIssue.COMPOSED_TEMPERATURE_UNSUPPORTED in audit.issues)
            }
        }
    }

    @Test fun anyRecipeEditBreaksComposedSourceEvidence() {
        for (id in ids) {
            val dish = OfflineRecipes.curatedDrafts.single { it.id == id }
            val review = M2PublishedMethodComposed20261026.reviews.getValue(id)
            val changedStep = dish.copy(steps = dish.steps.toMutableList().also {
                it[1] = it[1] + " Changed independently."
            })
            assertFalse(RecipeEvidenceAudit.inspect(changedStep, review).documentedReady)
            val changedWeight = dish.copy(ingredients = dish.ingredients.mapIndexed { i, v ->
                if (i == 0) v.copy(grams = v.grams + 1) else v
            })
            assertFalse(RecipeEvidenceAudit.inspect(changedWeight, review).documentedReady)
        }
    }

    @Test fun releaseGateStillRejectsGeneratedTemplatesAndUnknownSkuLabels() {
        val report = RecipeEvidenceAudit.report()
        assertEquals(150, report.distinctDrafts)
        assertEquals(200, report.generatedTemplates)
        assertEquals(89, report.documentedReviewed)
        assertEquals(0, report.skuLabelsClearedByRecipes)
        assertFalse(report.meetsM2Gate)
        assertEquals(61, OfflineRecipes.curatedDrafts.count {
            it.id !in M2PublishedMethodReviews.reviews
        })
    }
}
