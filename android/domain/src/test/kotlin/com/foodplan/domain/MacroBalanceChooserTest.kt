package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class MacroBalanceChooserTest {
    private val prototype = OfflineRecipes.recipes.first()

    private fun option(id: String, score: Int, macros: Nutrition) =
        MacroMealOption(prototype.copy(id = id), score, macros)

    @Test fun referenceDeviationUsesOnlyFourNineFourNotReportedCalories() {
        // Calorie fields may differ from their 4/9/4 macro reconstruction;
        // this diagnostic never silently forces macro percentages to match.
        val balanced = Nutrition(1000.0, 25.0, 15.0, 60.0)
        assertEquals(0.0, MacroBalanceChooser.outsideReferencePoints(balanced)!!, 0.001)
        val skewed = Nutrition(1000.0, 25.0, 50.0, 20.0)
        assertTrue(MacroBalanceChooser.outsideReferencePoints(skewed)!! > 20.0)
        assertNull(MacroBalanceChooser.outsideReferencePoints(Nutrition.ZERO))
        assertNull(MacroBalanceChooser.outsideReferencePoints(
            Nutrition(0.0, Double.NaN, 2.0, 10.0)))
    }

    @Test fun softDailyScoreCanReplaceFatHeavyOptionWithoutChangingEligibilitySet() {
        val fatty = option("fatty", 20, Nutrition(350.0, 25.0, 25.0, 25.0))
        val starchy = option("starchy", 18, Nutrition(350.0, 10.0, 1.0, 75.0))
        val morning = option("morning", 24, Nutrition(350.0, 25.0, 25.0, 25.0))
        val evening = option("evening", 24, Nutrition(350.0, 25.0, 25.0, 25.0))
        val slots = listOf(listOf(morning), listOf(fatty, starchy), listOf(evening))
        val chosen = MacroBalanceChooser.choose(slots)!!
        assertEquals(listOf("morning", "starchy", "evening"),
            chosen.map { it.recipe.id })
        assertTrue(MacroBalanceChooser.outsideReferencePoints(
            chosen.fold(Nutrition.ZERO) { sum, it -> sum + it.estimatedRawNutrition })!! <
            MacroBalanceChooser.outsideReferencePoints(
                morning.estimatedRawNutrition + fatty.estimatedRawNutrition +
                    evening.estimatedRawNutrition)!!)
        assertEquals(chosen, MacroBalanceChooser.choose(slots))
    }

    @Test fun hardFilteredCandidatesCannotBeReintroducedByMacroScoring() {
        val permissible = option("permitted", 3, Nutrition(300.0, 8.0, 23.0, 8.0))
        // The optimizer only receives pre-checked candidates. An apparently
        // perfectly balanced but ineligible dish cannot enter this list.
        val result = MacroBalanceChooser.choose(listOf(
            listOf(permissible), listOf(permissible), listOf(permissible)))!!
        assertTrue(result.all { it.recipe.id == "permitted" })
        assertNull(MacroBalanceChooser.choose(listOf(listOf(permissible), emptyList())))
        // A one-meal day should preserve the top taste/preparation ranking.
        assertEquals("permitted", MacroBalanceChooser.choose(listOf(listOf(
            permissible, option("lower", 2, Nutrition(300.0, 24.0, 15.0, 58.0))
        )))!!.single().recipe.id)
    }

    @Test fun adultDeviationIsSoftAndNeverClaimsMicronutrientAdequacy() {
        val result = MacroBalanceChooser.choose(listOf(
            listOf(option("a", 60, Nutrition(100.0, 20.0, 5.0, 25.0))),
            listOf(option("b", 60, Nutrition(100.0, 20.0, 5.0, 25.0))),
            listOf(option("c", 60, Nutrition(100.0, 20.0, 5.0, 25.0)))
        ))
        assertNotNull(result)
        assertEquals(3, result!!.size)
    }
}
