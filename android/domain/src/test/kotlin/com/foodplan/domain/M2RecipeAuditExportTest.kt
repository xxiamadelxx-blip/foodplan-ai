package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class M2RecipeAuditExportTest {
    @Test fun exportContainsEveryRecipeWithSourceLinksAndUnverifiedRecipeEvidence() {
        val csv=M2RecipeAuditExport.generateCsv()
        val lines=csv.trimEnd().lines()
        assertEquals(277,lines.size)
        assertTrue(lines.first().contains("audit_issues"))
        assertTrue(csv.contains("cur_oat_waffles_pear"))
        assertTrue(csv.contains("сухой продукт до приготовления"))
        assertTrue(csv.contains("готовый продукт без жидкости"))
        assertFalse(csv.contains("NUTRIENTS_UNSOURCED"))
        assertTrue(csv.contains("ALLERGENS_UNREVIEWED"))
        assertTrue(csv.contains("UNVERIFIED_RECIPE"))
        assertFalse(csv.contains("UNKNOWN_BASIS"))
    }

    @Test fun quoteEscapingProtectsCommaSeparatedAudit() {
        val csv=M2RecipeAuditExport.generateCsv()
        assertTrue(csv.lines()[0].startsWith("\"recipe_id\",\"title\""))
        val ids=OfflineRecipes.recipes.map { it.id }.toSet()
        for(id in ids) assertTrue(csv.contains("\"$id\""))
    }
    @Test fun exportKeepsCookedYieldUnverifiedDespiteCompleteIngredientSourceCoverage() {
        val csv=M2RecipeAuditExport.generateCsv()
        val header=csv.lineSequence().first()
        assertTrue(header.contains("cooked_yield_grams"))
        assertTrue(header.contains("cooked_yield_source_reference"))
        assertTrue(header.contains("ingredient_nutrient_sources"))
        assertFalse(csv.contains("NOT_SOURCED"))
        assertTrue(csv.contains("https://fdc.nal.usda.gov/food-details/2512381/nutrients"))
        assertTrue(csv.contains("https://rossmol.ru/tvorog"))
        assertTrue(csv.contains("UNVERIFIED_COOKED_YIELD"))
        assertEquals(277,csv.trimEnd().lines().size)
    }

}
