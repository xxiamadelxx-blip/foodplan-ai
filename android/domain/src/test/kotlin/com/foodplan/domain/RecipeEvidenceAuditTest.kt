package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class RecipeEvidenceAuditTest {
    @Test fun templatesAndAuthoredDraftsNeverCountAsVerifiedRecipes() {
        val report=RecipeEvidenceAudit.report()
        assertEquals(260,report.total)
        assertEquals(200,report.generatedTemplates)
        assertEquals(60,report.distinctDrafts)
        assertEquals(0,report.independentlyVerified)
        assertFalse(report.meetsM2Gate)
        assertEquals(260,report.issues.size)
    }

    @Test fun sourceFreeRecipeCannotBePromotedByFlippingBoolean() {
        val candidate=OfflineRecipes.curatedDrafts.first().copy(
            nutritionVerified=true,
            evidenceStatus=RecipeEvidenceStatus.INDEPENDENTLY_VERIFIED,
            sourceReference="https://example.org/recipe"
        )
        val issues=RecipeEvidenceAudit.inspect(candidate).issues
        assertTrue(RecipeAuditIssue.NUTRIENTS_UNSOURCED in issues)
        assertTrue(RecipeAuditIssue.ALLERGENS_UNREVIEWED in issues)
    }

    @Test fun missingIngredientAndNegativeWeightAreHardFailures() {
        val invalid=OfflineRecipes.curatedDrafts.first().copy(
            ingredients=listOf(RecipeIngredient("missing_food",-20))
        )
        val issues=RecipeEvidenceAudit.inspect(invalid).issues
        assertTrue(RecipeAuditIssue.UNKNOWN_INGREDIENT in issues)
        assertTrue(RecipeAuditIssue.INVALID_WEIGHT in issues)
    }

    @Test fun preparationAndDuplicateIngredientsAreChecked() {
        val invalid=OfflineRecipes.curatedDrafts.first().copy(
            ingredients=listOf(RecipeIngredient("egg",80),RecipeIngredient("egg",80)),
            preparation=RecipePreparation(setOf("Волшебная палочка"),5,5,1,1)
        )
        val issues=RecipeEvidenceAudit.inspect(invalid).issues
        assertTrue(RecipeAuditIssue.DUPLICATE_INGREDIENT in issues)
        assertTrue(RecipeAuditIssue.UNKNOWN_EQUIPMENT in issues)
    }
    @Test fun twoRawIngredientsHaveTraceableFdcNutrientsButRemainAllergenUnverified() {
        val rice=OfflineRecipes.ingredients.getValue("rice")
        val chicken=OfflineRecipes.ingredients.getValue("chicken")
        assertEquals("https://fdc.nal.usda.gov/food-details/2512381/nutrients",
            rice.nutrientSourceReference)
        assertEquals(358.705,rice.kcal100,0.0001)
        assertEquals(80.31315,rice.carbs100,0.00001)
        assertEquals("https://fdc.nal.usda.gov/food-details/2646170/nutrients",
            chicken.nutrientSourceReference)
        assertEquals(106.034,chicken.kcal100,0.0001)
        assertEquals(22.525,chicken.protein100,0.0001)
        assertFalse(rice.allergenCompositionReviewed)
        assertFalse(chicken.allergenCompositionReviewed)
        assertEquals(0,RecipeEvidenceAudit.report().independentlyVerified)
    }

}
