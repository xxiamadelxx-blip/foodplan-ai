package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

/**
 * TDD RED: a constructed recipe needs evidence for EACH method operation.
 * One real dish name does not have to appear verbatim at a publisher's URL.
 */
class ComposedMethodEvidenceTest {
    private val recipe = OfflineRecipes.curatedDrafts.single {
        it.id == "b4_savory_carrot_oat_bake"
    }

    private fun technique(
        code: String, url: String, steps: Set<Int>, ingredients: Set<String>,
        safety: Boolean = false
    ): PublishedTechniqueEvidence {
        val excerpt = "Captured external method facts for operation $code: " +
            "published techniques explain grating, mixing and baking the named " +
            "ingredients, with variation explicitly reviewed and not copied as exact recipe."
        return PublishedTechniqueEvidence(
            techniqueCode = code,
            publishedMethodSource = url,
            methodSnapshotText = excerpt,
            methodSnapshotSha256 = RecipeEvidenceAudit.sha256Utf8(excerpt),
            coveredStepNumbers = steps,
            coveredIngredientIds = ingredients,
            applicabilityNotes = "Applies to the reviewed operation, NOT as a literal identical dish. " +
                "Masses and individual assembly steps follow FoodPlan, not publisher nutrition.",
            safetyEvidence = safety
        )
    }

    private val bake = "https://www.bda.uk.com/resource/savoury-flapjack.html"
    private val milk = "https://www.savorymomentsblog.com/2018/03/baked-carrot-cake-oatmeal.html"
    private val safety = "https://www.fsis.usda.gov/food-safety/safe-food-handling-and-preparation/eggs/shell-eggs-farm-table"

    private fun reviewed(): RecipeDocumentaryReview {
        val summary = "Composed source methods: oats, grated carrots, cheese and egg " +
            "are mixed and baked in the published savoury flapjack recipe; milk is " +
            "supported by separately published baked oatmeal; USDA verifies safety."
        return RecipeDocumentaryReview(
            recipeId = recipe.id,
            recipeFingerprintSha256 = RecipeEvidenceAudit.fingerprint(recipe),
            publishedMethodSource = bake,
            methodSnapshotSha256 = RecipeEvidenceAudit.sha256Utf8(summary),
            methodSnapshotText = summary,
            methodApplicabilityNotes = "Published component methods are combined for the FoodPlan authored " +
                "dish. Original weights and egg temperatures are NOT inferred from cooking time.",
            reviewRecordReference = "https://github.com/xxiamadelxx-blip/foodplan-ai/blob/m4/integrated-retail-20261022/docs/qa/M2_COMPOSED_METHOD_PROTOCOL_20261026.md",
            nutritionBasis = RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE,
            nutritionCalculationNotes = "Test fixture recalculation of independently pinned raw food inputs, no cooked yield.",
            independentlyRecalculatedRawNutrition = OfflineRecipes.nutrition(recipe),
            composedTechniques = listOf(
                technique("GRATE_MIX_BAKE", bake, setOf(1,2,3,4),
                    setOf("oats","carrot","cheese","egg")),
                technique("MILK_IN_EGG_OATS", milk, setOf(2), setOf("milk")),
                technique("EGG_DISH_SAFE_HEAT", safety, setOf(4), setOf("egg"), safety = true)
            )
        )
    }

    @Test fun independentlyPublishedOperationsCoverTheAuthoredDishWithoutExactDishCopy() {
        val checked = RecipeEvidenceAudit.inspect(recipe, reviewed())
        assertTrue("Composed method acceptance: ${checked.issues}", checked.documentedReady)
        assertFalse(checked.skuLabelCleared)
        assertFalse(checked.physicalTrialDocumented)
    }

    @Test fun missingStepOrIngredientCoverageFailsClosed() {
        val original = reviewed()
        val first = original.composedTechniques[0]
        val missingSteps = original.copy(composedTechniques =
            original.composedTechniques.map { it.copy(coveredStepNumbers = setOf(1,2)) })
        assertTrue(RecipeAuditIssue.TECHNIQUE_COVERAGE_INCOMPLETE in
            RecipeEvidenceAudit.inspect(recipe, missingSteps).issues)
        val noMilk = original.copy(composedTechniques =
            original.composedTechniques.filterNot { it.techniqueCode == "MILK_IN_EGG_OATS" })
        assertTrue(RecipeAuditIssue.TECHNIQUE_COVERAGE_INCOMPLETE in
            RecipeEvidenceAudit.inspect(recipe, noMilk).issues)
        val outOfRange = original.copy(composedTechniques = listOf(
            first.copy(coveredStepNumbers = setOf(0,1,2,3,4,99))) +
            original.composedTechniques.drop(1))
        assertFalse(RecipeEvidenceAudit.inspect(recipe, outOfRange).documentedReady)
    }

    @Test fun fakeSnapshotUnsafeEggAndDuplicateTechniqueCannotPass() {
        val original = reviewed()
        val tampered = original.copy(composedTechniques =
            original.composedTechniques.mapIndexed { i, t ->
                if (i == 0) t.copy(methodSnapshotText = t.methodSnapshotText + " edited") else t
            })
        assertTrue(RecipeAuditIssue.TECHNIQUE_SOURCE_INVALID in
            RecipeEvidenceAudit.inspect(recipe, tampered).issues)
        val unsafe = original.copy(composedTechniques =
            original.composedTechniques.filterNot { it.safetyEvidence })
        assertTrue(RecipeAuditIssue.TECHNIQUE_SAFETY_MISSING in
            RecipeEvidenceAudit.inspect(recipe, unsafe).issues)
        val duplicate = original.copy(composedTechniques =
            original.composedTechniques + original.composedTechniques.first())
        assertFalse(RecipeEvidenceAudit.inspect(recipe, duplicate).documentedReady)
        val invalidUrl = original.copy(composedTechniques =
            original.composedTechniques.mapIndexed { i, t ->
                if (i == 0) t.copy(publishedMethodSource = "http://invalid.example/method") else t
            })
        assertFalse(RecipeEvidenceAudit.inspect(recipe, invalidUrl).documentedReady)
    }

    @Test fun staleFingerprintAndWrongMacronutrientsStillFail() {
        val source = reviewed()
        val altered = recipe.copy(ingredients =
            recipe.ingredients.mapIndexed { i, value ->
                if (i == 0) value.copy(grams = value.grams + 1) else value
            })
        assertFalse(RecipeEvidenceAudit.inspect(altered, source).documentedReady)
        val fake = source.copy(independentlyRecalculatedRawNutrition =
            OfflineRecipes.nutrition(recipe).copy(kcal = 1.0))
        assertFalse(RecipeEvidenceAudit.inspect(recipe, fake).documentedReady)
    }
}
