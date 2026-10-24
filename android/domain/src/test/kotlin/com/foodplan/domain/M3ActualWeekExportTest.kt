package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class M3ActualWeekExportTest {
    @Test fun exportedWeekIsProductionGeneratedAndHasNoImaginaryPrices() {
        val record = M3ActualWeekExport.extract()
        assertEquals(7, record.days)
        assertEquals(21, record.mealCount)
        assertTrue(record.uniqueIngredients > 2)
        assertEquals(record.uniqueIngredients, record.ingredientRows.size)
        assertEquals(21, record.mealRows.size)
        assertTrue(record.ingredientRows.all {
            it.contains("\"UNKNOWN\"") &&
                it.contains("\"NOT_INGREDIENT_OR_ALLERGEN_CERTIFIED\"")
        })
        val output = M3ActualWeekExport.csvFiles()
        assertEquals(22, output.first.lineSequence().filter { it.isNotBlank() }.count())
        assertEquals(record.uniqueIngredients + 1,
            output.second.lineSequence().filter { it.isNotBlank() }.count())
    }

    @Test fun shoppingAggregationUsesHouseholdButNotHandSelectedGroceries() {
        val person = M3ActualWeekExport.extract()
        val family = M3ActualWeekExport.extract(
            M3ActualWeekExport.fixtureProfile().copy(household = "2")
        )
        assertEquals(person.days, family.days)
        assertEquals(person.mealCount, family.mealCount)
        assertEquals(person.uniqueIngredients, family.uniqueIngredients)
        assertNotEquals(person.ingredientRows, family.ingredientRows)
    }
}
