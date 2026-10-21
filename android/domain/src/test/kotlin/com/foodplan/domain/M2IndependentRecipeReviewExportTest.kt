package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class M2IndependentRecipeReviewExportTest {
    @Test fun reviewQueueContainsExactlyOneRowForEveryAuthoredRecipe() {
        val queue=M2IndependentRecipeReviewExport.records()
        assertEquals(150,queue.size)
        assertEquals(150,queue.map { it.recipeId }.distinct().size)
        assertEquals(150,queue.map { it.title }.distinct().size)
        assertEquals(OfflineRecipes.curatedDrafts.map { it.id }.toSet(),
            queue.map { it.recipeId }.toSet())
        assertEquals(150,queue.count { it.recipeTechnologySourceMissing })
        assertEquals(150,queue.count { it.cookingYieldMissing })
        assertEquals(150,queue.count { it.packageTraceEvidenceMissing })
        assertTrue(queue.all { it.independentVerdict == "PENDING_EXTERNAL_REVIEW" })
        assertTrue(queue.all { it.requiredIngredientCount ==
            it.sourceLinkedIngredientCount })
    }

    @Test fun eachRawInputIsActuallyExplainedAndFullMethodVisibleForExternalAudit() {
        val queue=M2IndependentRecipeReviewExport.records()
        for (row in queue) {
            val recipe=OfflineRecipes.curatedDrafts.first { it.id == row.recipeId }
            assertEquals(recipe.id,recipe.ingredients.sumOf { it.grams },
                row.rawInputGrams)
            assertTrue(recipe.id,row.ingredientRows.contains(" г ("))
            assertFalse(recipe.id,row.ingredientRows.contains("NO_REFERENCE_NUTRIENTS"))
            assertFalse(recipe.id,row.ingredientRows.contains("UNKNOWN_INPUT_BASIS"))
            assertEquals(recipe.id,recipe.steps.size,
                Regex("""\\d+\\. """).findAll(row.fullMethod).count())
            assertTrue(recipe.id,row.preliminaryInputKcal > 0.0)
            assertTrue(recipe.id,row.requiredEquipment.isNotBlank() ||
                recipe.preparation.requiredEquipment.isEmpty())
        }
    }

    @Test fun exportedCsvLeavesAllIndependentAttestationCellsBlank() {
        val csv=M2IndependentRecipeReviewExport.generateCsv()
        val lines=csv.trimEnd().lines()
        assertEquals(151,lines.size)
        assertTrue(lines.first().contains("full_preparation_steps"))
        assertTrue(lines.first().contains("cooked_dish_measured_weight_and_trial_source"))
        assertTrue(lines.first().contains("exact_sku_manufacturer_gtin_trace_label_evidence"))
        assertTrue(lines.first().contains("second_review_identifier"))
        assertTrue(csv.contains("b4_cod_stuffed_tomatoes"))
        assertTrue(csv.contains("b5_salmon_stuffed_peppers"))
        assertTrue(csv.contains("сухой продукт до приготовления"))
        assertTrue(csv.contains("https://fdc.nal.usda.gov/food-details/2512381/nutrients"))
        assertTrue(csv.contains("PENDING_EXTERNAL_REVIEW"))
        assertFalse(csv.contains("INDEPENDENTLY_VERIFIED"))
        assertFalse(csv.contains("NO_REFERENCE_NUTRIENTS"))
    }

    @Test fun manufacturedTemplateOrDuplicatedIdCannotMasqueradeAsDraftReview() {
        try {
            M2IndependentRecipeReviewExport.records(OfflineRecipes.generatedPrototypes)
            fail("Templates must not enter independently reviewed queue")
        } catch (_: IllegalArgumentException) {
            // expected
        }
        val duplicate=listOf(
            OfflineRecipes.curatedDrafts.first(),
            OfflineRecipes.curatedDrafts.first())
        try {
            M2IndependentRecipeReviewExport.records(duplicate)
            fail("Duplicate recipe ID must not enter queue")
        } catch (_: IllegalArgumentException) {
            // expected
        }
    }
}
