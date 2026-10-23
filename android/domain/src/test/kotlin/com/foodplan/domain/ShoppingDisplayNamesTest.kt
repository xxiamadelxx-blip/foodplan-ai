package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class ShoppingDisplayNamesTest {
    @Test fun everyKnownIngredientHasHumanShoppingName() {
        assertEquals(OfflineRecipes.ingredients.keys,
            ShoppingDisplayNames.supportedIngredientIds())
        OfflineRecipes.ingredients.forEach { (id, _) ->
            val title = ShoppingDisplayNames.forIngredient(id)
            assertTrue(title.isNotBlank())
            assertFalse(title.contains("съедобн", ignoreCase = true))
            assertFalse(title.contains("USDA", ignoreCase = true))
            assertFalse(title.contains("сырой", ignoreCase = true))
            assertFalse(title.contains("сухой", ignoreCase = true))
        }
        assertEquals("Яйца", ShoppingDisplayNames.forIngredient("egg"))
        assertEquals("Подсолнечное масло", ShoppingDisplayNames.forIngredient("oil"))
    }
}
