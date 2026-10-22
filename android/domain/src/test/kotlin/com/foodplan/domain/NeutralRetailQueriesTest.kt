package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class NeutralRetailQueriesTest {
    @Test fun everyCatalogIngredientHasNeutralSearchPolicy() {
        assertEquals(OfflineRecipes.ingredients.keys, NeutralRetailQueries.supportedIngredientIds())
    }

    @Test fun shoppingBecomesBrandFreePilotFirstScan() {
        val rice = OfflineRecipes.ingredients.getValue("rice")
        val chicken = OfflineRecipes.ingredients.getValue("chicken")
        val steps = NeutralRetailQueries.scanPlan(listOf(
            ShoppingNeed(rice,900,1),
            ShoppingNeed(chicken,1500,2)
        ))
        assertEquals(listOf("chicken","rice"),steps.map { it.ingredientId })
        assertEquals(listOf("PILOT","FULL_SCAN"),steps.map { it.phase })
        assertEquals("куриное филе",steps[0].query)
        assertEquals("рис",steps[1].query)
        assertEquals(900,steps[1].requiredGrams)
        assertTrue(steps.none { it.query.contains("росмол",ignoreCase=true) })
    }

    @Test fun missingSearchPolicyMustNotBeSilentlyIgnored() {
        val unknown = FoodIngredient("new_ingredient", "Новое", 0.0,0.0,0.0,0.0,100)
        try {
            NeutralRetailQueries.fromShopping(listOf(ShoppingNeed(unknown,100,1)))
            fail("unknown ingredient must fail closed")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("new_ingredient"))
        }
    }
}
