package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

/**
 * Source-independent structural QA for the new authored cooking drafts.
 * Does NOT claim that M2 cooking yields, real SKU allergens or recipe provenance
 * were independently verified.
 */
class CuratedRecipeBatch4And5Test {
    private val added get() = CuratedRecipeBatch4.recipes + CuratedRecipeBatch5.recipes

    @Test fun fiftyNewAuthoredDishesAreUniqueAndRemainUnverified() {
        assertEquals(25, CuratedRecipeBatch4.recipes.size)
        assertEquals(25, CuratedRecipeBatch5.recipes.size)
        assertEquals(50, added.size)
        assertEquals(50, added.map { it.id }.distinct().size)
        assertEquals(50, added.map { it.name }.distinct().size)
        assertEquals(15, added.count { it.type == MealType.BREAKFAST })
        assertEquals(35, added.count { it.type == MealType.MAIN })
        assertEquals(150, OfflineRecipes.curatedDrafts.size)
        assertEquals(150, OfflineRecipes.curatedDrafts.map { it.name }.distinct().size)
        assertEquals(350, OfflineRecipes.recipes.size)
        assertEquals(350, OfflineRecipes.recipes.map { it.id }.distinct().size)
        assertTrue(added.all { it.evidenceStatus == RecipeEvidenceStatus.CURATED_DRAFT &&
            !it.nutritionVerified && it.sourceReference == null &&
            it.cookedYieldGrams == null && it.cookedYieldSourceReference == null })
    }

    @Test fun everyRecipeHasIndependentWeighedInputsAndUsableDetailedSteps() {
        val structural = setOf(
            RecipeAuditIssue.UNKNOWN_INGREDIENT,
            RecipeAuditIssue.INVALID_WEIGHT,
            RecipeAuditIssue.INCOMPLETE_STEPS,
            RecipeAuditIssue.UNKNOWN_EQUIPMENT,
            RecipeAuditIssue.INVALID_PREPARATION,
            RecipeAuditIssue.DUPLICATE_INGREDIENT,
            RecipeAuditIssue.NUTRIENTS_UNSOURCED,
            RecipeAuditIssue.UNKNOWN_WEIGHT_BASIS
        )
        for (recipe in added) {
            assertTrue(recipe.id, recipe.ingredients.size >= 3)
            assertEquals(recipe.id, recipe.ingredients.size,
                recipe.ingredients.map { it.id }.distinct().size)
            assertTrue(recipe.id, recipe.ingredients.all {
                it.id in OfflineRecipes.ingredients && it.grams in 1..2000 &&
                IngredientWeightBasisRegistry.basisFor(it.id) != null
            })
            assertTrue(recipe.id, recipe.steps.size >= 4)
            assertTrue(recipe.id, recipe.steps.all { it.length >= 25 })
            assertTrue(recipe.id, recipe.preparation.totalMinutes >=
                recipe.preparation.activeMinutes)
            assertTrue(recipe.id, recipe.preparation.requiredEquipment.all {
                it in KitchenEquipment.choices
            })
            assertTrue(recipe.id, OfflineRecipes.nutrition(recipe).kcal > 0)
            val issues = RecipeEvidenceAudit.inspect(recipe).issues
            assertTrue(recipe.id, issues.intersect(structural).isEmpty())
            assertTrue(recipe.id, RecipeAuditIssue.UNVERIFIED_RECIPE in issues)
            assertTrue(recipe.id, RecipeAuditIssue.ALLERGENS_UNREVIEWED in issues)
            assertTrue(recipe.id, RecipeAuditIssue.UNVERIFIED_COOKED_YIELD in issues)
            assertTrue(recipe.id, RecipeAuditIssue.RECIPE_SOURCE_MISSING in issues)
        }
    }

    @Test fun veganFishEggAndDairyLabelsAreNotRelaxedByRecipeExpansion() {
        val vegan=added.first { it.id == "b4_tofu_cabbage_rolls" }
        assertTrue("Веганский" in OfflineRecipes.diets(vegan))
        assertTrue("soy" in OfflineRecipes.allergens(vegan))
        val vegan2=added.first { it.id == "b5_lentil_cabbage_pepper_soup" }
        assertTrue("Веганский" in OfflineRecipes.diets(vegan2))
        val fish=added.first { it.id == "b5_cod_lentil_cucumber_salad" }
        assertTrue("fish" in OfflineRecipes.allergens(fish))
        assertFalse("Веганский" in OfflineRecipes.diets(fish))
        val egg=added.first { it.id == "b5_potato_egg_nests_oven" }
        assertTrue("egg" in OfflineRecipes.allergens(egg))
        val milk=added.first { it.id == "b4_millet_pear_pudding" }
        assertTrue("milk" in OfflineRecipes.allergens(milk))
        assertFalse("Веганский" in OfflineRecipes.diets(milk))
    }

    @Test fun newCatalogIsNotFalselyAcceptedAsOneHundredFiftyReviewedRecipes() {
        val report=RecipeEvidenceAudit.report()
        assertEquals(350, report.total)
        assertEquals(200, report.generatedTemplates)
        assertEquals(150, report.distinctDrafts)
        assertEquals(0, report.independentlyVerified)
        assertFalse(report.meetsM2Gate)
        assertEquals(350, report.issues.size)
    }
}
