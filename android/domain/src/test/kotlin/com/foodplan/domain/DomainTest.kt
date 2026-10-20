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
    @Test fun budgetSliderBoundsAndPreciseUserAmountsAreAccepted() {
        for(value in listOf("100","356","63218","100000")) {
            assertEquals(value.toInt(),WeeklyBudget.parse(value))
            assertTrue(ProfileValidator.errors(valid().copy(budgetRub=value)).none {
                it.contains("Бюджет")
            })
        }
        for(value in listOf("0","99","100001","356.5","abc","")) {
            assertNull(WeeklyBudget.parse(value))
            assertTrue(ProfileValidator.errors(valid().copy(budgetRub=value)).any {
                it.contains("Бюджет")
            })
        }
    }
    @Test fun minorNotAllowedWeightChange() {
        val p = valid().copy(age="15", goal="Суперсушка")
        assertTrue(ProfileValidator.errors(p).any { it.contains("несовершеннолетних") })
    }
    @Test fun decimalWeightAndInvalidAge() {
        assertTrue(ProfileValidator.errors(valid().copy(weightKg="70,5")).isEmpty())
        assertTrue(ProfileValidator.errors(valid().copy(age="abc")).isNotEmpty())
    }
    @Test fun threeCookingEffortLevelsAreStable() {
        assertEquals(listOf("FAST", "INVOLVED", "ALL_OUT"), CookingEffort.entries.map { it.storageId })
        assertEquals(CookingEffort.FAST, CookingEffort.fromStorageId(null))
        assertEquals(CookingEffort.FAST, CookingEffort.fromStorageId("unknown"))
    }

    @Test fun allRequestedAppliancesAvailable() {
        assertTrue(KitchenEquipment.choices.containsAll(
            listOf("Плита", "Духовка", "Блендер", "Микроволновка", "Аэрогриль")))
        assertEquals(KitchenEquipment.choices.size, KitchenEquipment.choices.distinct().size)
    }

    @Test fun effortClassificationAndEquipmentFiltering() {
        val quick = RecipePreparation(setOf("Плита"), activeMinutes=8, totalMinutes=12, techniques=1, dishesToWash=1)
        val medium = RecipePreparation(setOf("Блендер"), activeMinutes=30, totalMinutes=40, techniques=3, dishesToWash=4)
        val hard = RecipePreparation(setOf("Духовка"), activeMinutes=80, totalMinutes=120, techniques=7, dishesToWash=9)
        assertEquals(CookingEffort.FAST, RecipePreparationRules.classify(quick))
        assertEquals(CookingEffort.INVOLVED, RecipePreparationRules.classify(medium))
        assertEquals(CookingEffort.ALL_OUT, RecipePreparationRules.classify(hard))

        val base = valid().copy(cookingMinutes="150", appliances=setOf("Плита","Блендер","Духовка"))
        assertTrue(RecipePreparationRules.eligible(quick, base))
        assertFalse(RecipePreparationRules.eligible(medium, base))
        assertFalse(RecipePreparationRules.eligible(hard, base))
        assertTrue(RecipePreparationRules.eligible(medium, base.copy(cookingEffort=CookingEffort.INVOLVED)))
        assertFalse(RecipePreparationRules.eligible(hard, base.copy(cookingEffort=CookingEffort.INVOLVED)))
        assertTrue(RecipePreparationRules.eligible(hard, base.copy(cookingEffort=CookingEffort.ALL_OUT)))
        assertFalse(RecipePreparationRules.eligible(hard, base.copy(
            cookingEffort=CookingEffort.ALL_OUT, appliances=setOf("Плита"))))
        assertFalse(RecipePreparationRules.eligible(hard, base.copy(
            cookingEffort=CookingEffort.ALL_OUT, cookingMinutes="30")))
    }

}
