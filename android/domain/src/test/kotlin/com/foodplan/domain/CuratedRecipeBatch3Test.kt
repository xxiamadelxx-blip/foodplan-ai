package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

/**
 * Independent structural QA, not externally sourced cookbook, cooking-yield
 * measurements or physical-package allergen validation.
 */
class CuratedRecipeBatch3Test {
    private val batch get() = CuratedRecipeBatch3.recipes

    @Test fun twentyFourDistinctDishesRemainExplicitlyUnverified() {
        assertEquals(24,batch.size)
        assertEquals(24,batch.map { it.id }.toSet().size)
        assertEquals(24,batch.map { it.name }.toSet().size)
        assertEquals(8,batch.count { it.type == MealType.BREAKFAST })
        assertEquals(16,batch.count { it.type == MealType.MAIN })
        assertTrue(batch.all {
            it.evidenceStatus == RecipeEvidenceStatus.CURATED_DRAFT &&
                !it.nutritionVerified && it.sourceReference == null &&
                it.cookedYieldGrams == null && it.cookedYieldSourceReference == null
        })
        assertEquals(150,OfflineRecipes.curatedDrafts.size)
        assertEquals(200,OfflineRecipes.generatedPrototypes.size)
        assertEquals(350,OfflineRecipes.recipes.size)
        assertEquals(350,OfflineRecipes.recipes.map { it.id }.toSet().size)
    }

    @Test fun everyAuthoredDishHasUsableMeasuredInputWeightsAndSteps() {
        for (recipe in batch) {
            assertTrue(recipe.id,recipe.ingredients.size>=3)
            assertEquals(recipe.id,recipe.ingredients.size,
                recipe.ingredients.map { it.id }.toSet().size)
            assertTrue(recipe.id,recipe.ingredients.all { it.grams in 1..1500 &&
                it.id in OfflineRecipes.ingredients &&
                IngredientWeightBasisRegistry.basisFor(it.id)!=null })
            assertTrue(recipe.id,recipe.steps.size>=3 &&
                recipe.steps.all { it.length>=25 })
            assertTrue(recipe.id,recipe.preparation.totalMinutes>=recipe.preparation.activeMinutes)
            assertTrue(recipe.id,recipe.preparation.requiredEquipment.all {
                it in KitchenEquipment.choices
            })
            assertTrue(recipe.id,OfflineRecipes.nutrition(recipe).kcal>0)
            val issues=RecipeEvidenceAudit.inspect(recipe).issues
            assertTrue(recipe.id,RecipeAuditIssue.UNVERIFIED_RECIPE in issues)
            assertTrue(recipe.id,RecipeAuditIssue.UNVERIFIED_COOKED_YIELD in issues)
            assertTrue(recipe.id,RecipeAuditIssue.ALLERGENS_UNREVIEWED in issues)
        }
    }

    @Test fun ingredientDerivedRestrictionsDoNotAllowFishEggOrDairyToLeak() {
        val vegan=batch.first { it.id=="cur_tofu_garlic_rice_cucumber" }
        assertTrue("Веганский" in OfflineRecipes.diets(vegan))
        assertTrue("soy" in OfflineRecipes.allergens(vegan))
        val fish=batch.first { it.id=="cur_lemon_cod_millet_broccoli" }
        assertTrue("fish" in OfflineRecipes.allergens(fish))
        assertFalse("Веганский" in OfflineRecipes.diets(fish))
        val egg=batch.first { it.id=="cur_potato_egg_cucumber_salad" }
        assertTrue("egg" in OfflineRecipes.allergens(egg))
        val milk=batch.first { it.id=="cur_baked_potato_cottage_cucumber" }
        assertTrue("milk" in OfflineRecipes.allergens(milk))
    }

    @Test fun publishedM2GateRemainsFailAndTemplatesAreNotCountedAsDishes() {
        val report=RecipeEvidenceAudit.report()
        assertEquals(350,report.total)
        assertEquals(200,report.generatedTemplates)
        assertEquals(150,report.distinctDrafts)
        assertEquals(0,report.independentlyVerified)
        assertFalse(report.meetsM2Gate)
    }
}
