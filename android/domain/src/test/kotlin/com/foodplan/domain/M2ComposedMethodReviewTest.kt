package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

/**
 * RED/GREEN: independent source-method composition must verify EACH authored
 * preparation step, not merely a matching dish title or a publisher URL.
 */
class M2ComposedMethodReviewTest {
    private val ids = listOf("b4_savory_carrot_oat_bake", "b4_buckwheat_apple_cups")

    private data class Frozen(val id: String, val recipeSha: String,
        val methodSha: String, val kcal: Double, val p: Double, val f: Double, val c: Double)
    // Independent authoring-time pins, never computed from the production registry.
    private val pinned = listOf(
        Frozen("b4_savory_carrot_oat_bake",
            "6acfabfe57e0e9447d411f10403b4fb5144bcd440171d3f63dabce2f16ea5dfa",
            "73c55838f051c365c1fbf2b55df6de189011230760ac471ce5b90d3d322ce95c",
            537.4, 27.694, 22.1835, 58.045),
        Frozen("b4_buckwheat_apple_cups",
            "b15c308719c0ffbaf7f87750db935821d0c03fe30fe8c6da72de27aa20280eed",
            "eedaa630a321b0fcd27667b25bb8e69563c88d640efa980f2ee7335461bca46b",
            494.5, 19.811, 10.255, 86.9945)
    )

    @Test fun pinnedOriginalStepsAndIndependentlyTabulatedRawNutritionStayFixed() {
        for (fixed in pinned) {
            val recipe = OfflineRecipes.curatedDrafts.single { it.id == fixed.id }
            val review = M2PublishedMethodReviews.reviews.getValue(fixed.id)
            assertEquals(fixed.recipeSha, RecipeEvidenceAudit.fingerprint(recipe))
            assertEquals(fixed.recipeSha, review.recipeFingerprintSha256)
            assertEquals(fixed.methodSha, review.methodSnapshotSha256)
            assertEquals(fixed.methodSha,
                RecipeEvidenceAudit.sha256Utf8(requireNotNull(review.methodSnapshotText)))
            val checked = requireNotNull(review.independentlyRecalculatedRawNutrition)
            val computed = OfflineRecipes.nutrition(recipe)
            assertEquals(fixed.kcal, checked.kcal, 0.00001)
            assertEquals(fixed.p, checked.protein, 0.00001)
            assertEquals(fixed.f, checked.fat, 0.00001)
            assertEquals(fixed.c, checked.carbs, 0.00001)
            assertEquals(fixed.kcal, computed.kcal, 0.00001)
            assertEquals(fixed.p, computed.protein, 0.00001)
            assertEquals(fixed.f, computed.fat, 0.00001)
            assertEquals(fixed.c, computed.carbs, 0.00001)
        }
    }

    @Test fun twoIndependentlySourcedCompositionsNeedEveryPinnedPreparationStep() {
        assertEquals(2, M2PublishedMethodComposed20261026.reviews.size)
        assertEquals(91, M2PublishedMethodReviews.reviews.size)
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

    @Test fun compositeSourceCannotBeLaunderedAsOnePublishedMethodOrFalseMacro() {
        for (id in ids) {
            val recipe = OfflineRecipes.curatedDrafts.single { it.id == id }
            val original = M2PublishedMethodComposed20261026.reviews.getValue(id)
            val dishonestBasis = original.copy(methodBasis =
                RecipeMethodBasis.SINGLE_PUBLISHED_METHOD)
            assertTrue(RecipeAuditIssue.COMPOSED_TECHNIQUE_EVIDENCE_INVALID in
                RecipeEvidenceAudit.inspect(recipe, dishonestBasis).issues)
            assertFalse(RecipeEvidenceAudit.inspect(recipe, dishonestBasis).documentedReady)

            val correctRaw = requireNotNull(original.independentlyRecalculatedRawNutrition)
            val falsified = original.copy(independentlyRecalculatedRawNutrition =
                correctRaw.copy(carbs = correctRaw.carbs + 1.0))
            assertTrue(RecipeAuditIssue.NUTRITION_RECALCULATION_MISMATCHED in
                RecipeEvidenceAudit.inspect(recipe, falsified).issues)
            assertFalse(RecipeEvidenceAudit.inspect(recipe, falsified).documentedReady)
        }
    }

    @Test fun releaseGateStillRejectsGeneratedTemplatesAndUnknownSkuLabels() {
        val report = RecipeEvidenceAudit.report()
        assertEquals(150, report.distinctDrafts)
        assertEquals(200, report.generatedTemplates)
        assertEquals(91, report.documentedReviewed)
        assertEquals(0, report.skuLabelsClearedByRecipes)
        assertFalse(report.meetsM2Gate)
        assertEquals(59, OfflineRecipes.curatedDrafts.count {
            it.id !in M2PublishedMethodReviews.reviews
        })
    }
}
