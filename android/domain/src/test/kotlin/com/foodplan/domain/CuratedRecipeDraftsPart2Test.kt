package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

/** Independent structural checks for the new authored-draft batch. */
class CuratedRecipeDraftsPart2Test {
    private val drafts get() = CuratedRecipeDraftsPart2.recipes

    @Test fun authoredBatchIsDistinctAndNotFalselySourceVerified() {
        assertEquals(16, drafts.size)
        assertEquals(drafts.size, drafts.map { it.id }.distinct().size)
        assertTrue(drafts.all { it.evidenceStatus == RecipeEvidenceStatus.CURATED_DRAFT })
        assertTrue(drafts.none { it.nutritionVerified || it.sourceReference != null })
        assertEquals(200,OfflineRecipes.generatedPrototypes.size)
        assertEquals(60,OfflineRecipes.curatedDrafts.size)
        assertEquals(260,OfflineRecipes.recipes.size)
        assertFalse(RecipeEvidenceAudit.report().meetsM2Gate)
    }

    @Test fun everyDraftHasMeasuredRawIngredientsAndUsableCookingSteps() {
        for (r in drafts) {
            assertTrue(r.name, r.ingredients.size >= 3)
            assertEquals(r.name, r.ingredients.size,r.ingredients.map { it.id }.toSet().size)
            assertTrue(r.name,r.ingredients.all {
                it.id in OfflineRecipes.ingredients && it.grams in 1..1500
            })
            assertTrue(r.name,r.steps.size>=3 && r.steps.all { it.length >= 25 })
            assertTrue(r.name,r.preparation.totalMinutes >= r.preparation.activeMinutes)
            assertTrue(r.name,r.preparation.requiredEquipment.all { it in KitchenEquipment.choices })
            assertTrue(r.name,OfflineRecipes.nutrition(r).kcal > 0)
        }
    }

    @Test fun veganDishesExcludeAnimalProductsAndEggMilkAreLabeled() {
        val tofu=drafts.first { it.id=="cur_tofu_pepper_buckwheat" }
        assertTrue("Веганский" in OfflineRecipes.diets(tofu))
        assertTrue("soy" in OfflineRecipes.allergens(tofu))
        val sweet=drafts.first { it.id=="cur_apple_cottage_cheese_bake" }
        assertTrue(setOf("milk","egg","wheat").all { it in OfflineRecipes.allergens(sweet) })
        assertFalse("Веганский" in OfflineRecipes.diets(sweet))
        val fish=drafts.first { it.id=="cur_cod_fish_soup" }
        assertTrue("fish" in OfflineRecipes.allergens(fish))
    }

    @Test fun categoriesHaveVarietyBeyondSingleTemplate() {
        assertTrue(drafts.count { it.type==MealType.BREAKFAST } >= 4)
        assertTrue(drafts.count { it.type==MealType.MAIN } >= 10)
        assertTrue(drafts.any { "суп" in it.tags })
        assertTrue(drafts.any { "тушёное" in it.tags })
    }
}
