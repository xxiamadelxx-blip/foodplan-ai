package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class DomainTest {
    @Test fun packagingAndPantry() {
        val basket = BasketCalculator.calculate(
            listOf(GroceryNeed("rice", 400), GroceryNeed("rice", 250)),
            listOf(PantryStock("rice", 150)),
            listOf(StoreOffer("rice", "five", "spb", 900, 14900, true)),
            "spb", setOf("five"), 20000
        )
        assertEquals(14900L, basket.totalKopeks)
        assertEquals(1L, basket.lines.single().packageCount)
        assertEquals(150L, basket.lines.single().pantryGramsUsed)
        assertEquals(true, basket.fitsBudget)
    }
    @Test fun regionMismatchNotInvented() {
        val b = BasketCalculator.calculate(listOf(GroceryNeed("rice", 300)), emptyList(),
            listOf(StoreOffer("rice", "five", "moscow", 900, 10000, true)), "spb", setOf("five"), 20000)
        assertFalse(b.isBudgetVerified)
        assertNull(b.fitsBudget)
        assertEquals(listOf("rice"), b.unavailableIngredients)
    }
    @Test fun estimatedPriceNeverVerified() {
        val b = BasketCalculator.calculate(listOf(GroceryNeed("rice", 300)), emptyList(),
            listOf(StoreOffer("rice", "five", "spb", 900, 10000, false)), "spb", setOf("five"), 20000)
        assertNull(b.fitsBudget)
    }
    @Test fun expensiveConfirmedOfferRejected() {
        val b = BasketCalculator.calculate(listOf(GroceryNeed("rice", 300)), emptyList(),
            listOf(StoreOffer("rice", "five", "spb", 900, 14900, true)), "spb", setOf("five"), 5000)
        assertEquals(false, b.fitsBudget)
    }
    @Test fun allergenOverridesPreference() {
        assertEquals(Eligibility.BLOCKED_ALLERGEN, RecipeEligibility.check(
            RecipeSafety(setOf("peanut_sauce"), setOf("peanut"), true),
            setOf("peanut"), emptySet()))
    }
    @Test fun unknownCompositionRejected() {
        assertEquals(Eligibility.UNKNOWN_COMPOSITION, RecipeEligibility.check(
            RecipeSafety(setOf("sauce"), emptySet(), false), setOf("peanut"), emptySet()))
    }
    private fun valid() = FoodProfile(age="28",heightCm="170",weightKg="75",budgetRub="5000")
    @Test fun validProfile() { assertTrue(ProfileValidator.errors(valid()).isEmpty()) }
    @Test fun budgetAndShopChecked() {
        val p = valid().copy(budgetRub="0", stores=emptySet())
        val errors = ProfileValidator.errors(p)
        assertTrue(errors.any { it.contains("Бюджет") })
        assertTrue(errors.any { it.contains("магазин") })
    }
    @Test fun minorNotAllowedWeightChange() {
        val p = valid().copy(age="15", goal="Суперсушка")
        assertTrue(ProfileValidator.errors(p).any { it.contains("несовершеннолетних") })
    }
    @Test fun decimalWeightAndInvalidAge() {
        assertTrue(ProfileValidator.errors(valid().copy(weightKg="70,5")).isEmpty())
        assertTrue(ProfileValidator.errors(valid().copy(age="abc")).isNotEmpty())
    }
}
