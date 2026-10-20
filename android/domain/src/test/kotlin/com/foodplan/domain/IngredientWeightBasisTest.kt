package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class IngredientWeightBasisTest {
    @Test fun everyExistingIngredientHasUnambiguousWeighingBasis() {
        assertEquals(emptySet<String>(),IngredientWeightBasisRegistry.catalogueCoverage(
            OfflineRecipes.ingredients.keys))
        for(recipe in OfflineRecipes.recipes) {
            assertTrue(recipe.name,recipe.ingredients.all {
                IngredientWeightBasisRegistry.basisFor(it.id)!=null
            })
        }
    }

    @Test fun dryRawAndDrainedAreNotConfusedWithCookedFood() {
        assertEquals(IngredientWeightBasis.DRY_UNCOOKED,
            IngredientWeightBasisRegistry.basisFor("rice"))
        assertEquals(IngredientWeightBasis.DRY_UNCOOKED,
            IngredientWeightBasisRegistry.basisFor("pasta"))
        assertEquals(IngredientWeightBasis.RAW_EDIBLE,
            IngredientWeightBasisRegistry.basisFor("chicken"))
        assertEquals(IngredientWeightBasis.DRAINED_READY_TO_EAT,
            IngredientWeightBasisRegistry.basisFor("chickpeas"))
        assertEquals(IngredientWeightBasis.PREPACKAGED_READY_TO_EAT,
            IngredientWeightBasisRegistry.basisFor("yogurt"))
        assertEquals(IngredientWeightBasis.FROZEN_UNCOOKED,
            IngredientWeightBasisRegistry.basisFor("berries"))
        assertNull(IngredientWeightBasisRegistry.basisFor("not_in_catalog"))
    }

    @Test fun everyBasisHasNonEmptyRussianLabel() {
        for(basis in IngredientWeightBasis.entries) {
            assertTrue(IngredientWeightBasisRegistry.russianLabel(basis).length >= 15)
        }
    }
}
