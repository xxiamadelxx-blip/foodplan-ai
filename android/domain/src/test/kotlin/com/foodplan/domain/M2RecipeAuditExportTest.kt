package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class M2RecipeAuditExportTest {
    @Test fun exportContainsEveryRecipeWithExplicitUnverifiedEvidenceAndMassBasis() {
        val csv=M2RecipeAuditExport.generateCsv()
        val lines=csv.trimEnd().lines()
        assertEquals(277,lines.size)
        assertTrue(lines.first().contains("audit_issues"))
        assertTrue(csv.contains("cur_oat_waffles_pear"))
        assertTrue(csv.contains("сухой продукт до приготовления"))
        assertTrue(csv.contains("готовый продукт без жидкости"))
        assertTrue(csv.contains("NUTRIENTS_UNSOURCED"))
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
}
