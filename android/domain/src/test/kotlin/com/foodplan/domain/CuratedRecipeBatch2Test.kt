package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class CuratedRecipeBatch2Test {
    @Test fun distinctNewRecipesHaveCompleteMeasurableDraftInstructions() {
        val recipes=CuratedRecipeBatch2.recipes
        assertEquals(16,recipes.size)
        assertEquals(16,recipes.map { it.id }.distinct().size)
        assertEquals(16,recipes.map { it.name }.distinct().size)
        assertTrue(recipes.all { it.evidenceStatus==RecipeEvidenceStatus.CURATED_DRAFT })
        assertTrue(recipes.none { it.nutritionVerified || it.sourceReference != null })
        for(recipe in recipes) {
            assertTrue(recipe.name,recipe.ingredients.size>=3)
            assertTrue(recipe.name,recipe.ingredients.all { it.id in OfflineRecipes.ingredients && it.grams in 1..1500 })
            assertTrue(recipe.name,recipe.steps.size>=3 && recipe.steps.all { it.length>=25 })
            assertTrue(recipe.name,recipe.preparation.requiredEquipment.all { it in KitchenEquipment.choices })
            val audit = RecipeEvidenceAudit.inspect(recipe)
            assertTrue(recipe.name,
                RecipeAuditIssue.DOCUMENTARY_REVIEW_MISSING in audit.issues)
            assertTrue(recipe.name,
                RecipeAuditNotice.COOKED_YIELD_NOT_MEASURED in audit.notices)
            assertTrue(recipe.name,
                RecipeAuditNotice.SKU_LABEL_TRACES_UNKNOWN in audit.notices)
            assertFalse(recipe.name, audit.documentedReady)
        }
    }

    @Test fun allergyAndEquipmentFiltersPreventIncompatibleRecipes() {
        val waffles=CuratedRecipeBatch2.recipes.first { it.id=="cur_oat_waffles_pear" }
        assertTrue("Вафельница" in waffles.preparation.requiredEquipment)
        assertTrue(setOf("milk","egg").all { it in OfflineRecipes.allergens(waffles) })
        val vegan=CuratedRecipeBatch2.recipes.first { it.id=="cur_vegetable_chickpea_pilaf" }
        assertTrue("Веганский" in OfflineRecipes.diets(vegan))
        val cod=CuratedRecipeBatch2.recipes.first { it.id=="cur_cod_rice_fishballs" }
        assertTrue(setOf("fish","egg").all { it in OfflineRecipes.allergens(cod) })
    }

    @Test fun releaseGateRemainsClosedWithoutIndependentlyReviewedNutrition() {
        val report=RecipeEvidenceAudit.report()
        assertEquals(350,report.total)
        assertEquals(150,report.distinctDrafts)
        assertEquals(0,report.independentlyVerified)
        assertFalse(report.meetsM2Gate)
    }
}
