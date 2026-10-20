package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class RecipeEvidenceAuditTest {
    @Test fun templatesAndAuthoredDraftsNeverCountAsVerifiedRecipes() {
        val report=RecipeEvidenceAudit.report()
        assertEquals(276,report.total)
        assertEquals(200,report.generatedTemplates)
        assertEquals(76,report.distinctDrafts)
        assertEquals(0,report.independentlyVerified)
        assertFalse(report.meetsM2Gate)
        assertEquals(276,report.issues.size)
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

    @Test fun wholeEggNutrientValuesCarryFdcIdWithoutUnverifiedAllergenClaims() {
        val egg=OfflineRecipes.ingredients.getValue("egg")
        assertEquals("https://fdc.nal.usda.gov/food-details/748967/nutrients",
            egg.nutrientSourceReference)
        assertEquals(143.0,egg.kcal100,0.0001)
        assertEquals(12.4,egg.protein100,0.0001)
        assertEquals(9.96,egg.fat100,0.0001)
        assertEquals(0.96,egg.carbs100,0.0001)
        assertFalse(egg.allergenCompositionReviewed)
        assertEquals(0,RecipeEvidenceAudit.report().independentlyVerified)
    }

    @Test fun fourVegetableNutrientsHaveExplicitFoundationFoodSources() {
        val expected=mapOf(
            "broccoli" to Triple(321900,32.0,2.57),
            "cabbage" to Triple(2346407,31.4275,0.96125),
            "cucumber" to Triple(2346406,15.9075,0.625),
            "garlic" to Triple(1104647,143.0,6.62)
        )
        for ((id,info) in expected) {
            val food=OfflineRecipes.ingredients.getValue(id)
            assertEquals("https://fdc.nal.usda.gov/food-details/${info.first}/nutrients",
                food.nutrientSourceReference)
            assertEquals(info.second,food.kcal100,0.0001)
            assertEquals(info.third,food.protein100,0.0001)
            assertFalse(food.allergenCompositionReviewed)
        }
        assertEquals(0,RecipeEvidenceAudit.report().independentlyVerified)
    }

    @Test fun cookedYieldEvidenceCannotBeInferredFromInputIngredientGrams() {
        val draft=OfflineRecipes.curatedDrafts.first()
        assertNull(draft.cookedYieldGrams)
        val issues=RecipeEvidenceAudit.inspect(draft).issues
        assertTrue(RecipeAuditIssue.UNVERIFIED_COOKED_YIELD in issues)

        val fake=draft.copy(
            nutritionVerified=true,
            evidenceStatus=RecipeEvidenceStatus.INDEPENDENTLY_VERIFIED,
            sourceReference="https://example.org/recipe",
            cookedYieldGrams=0,
            cookedYieldSourceReference="https://example.org/measurements"
        )
        assertTrue(RecipeAuditIssue.UNVERIFIED_COOKED_YIELD in
            RecipeEvidenceAudit.inspect(fake).issues)
        assertFalse(RecipeEvidenceAudit.inspect(fake).verified)
    }

    @Test fun unknownIngredientWeightBasisFailsClosed() {
        val draft=OfflineRecipes.curatedDrafts.first().copy(
            ingredients=listOf(RecipeIngredient("unknown_unreviewed_ingredient",100))
        )
        val issues=RecipeEvidenceAudit.inspect(draft).issues
        assertTrue(RecipeAuditIssue.UNKNOWN_WEIGHT_BASIS in issues)
        assertTrue(RecipeAuditIssue.UNKNOWN_INGREDIENT in issues)
    }

    @Test fun fourNewFoodSourcesMatchSpecificRawOrDryFdcFoods() {
        data class Expected(val id:Int,val kcal:Double,val protein:Double,
                            val fat:Double,val carbs:Double)
        val expected=mapOf(
            "cod" to Expected(2684444,65.9855,16.06875,0.6675,0.0),
            "salmon" to Expected(2684441,197.294,20.31875,13.11,0.0),
            "pear" to Expected(2710836,63.6613,0.3125,0.3713,14.7674),
            "flour" to Expected(790018,362.0,12.0,1.7,74.6)
        )
        for((id,e) in expected) {
            val food=OfflineRecipes.ingredients.getValue(id)
            assertEquals(id,
                "https://fdc.nal.usda.gov/food-details/${e.id}/nutrients",
                food.nutrientSourceReference)
            assertEquals(id,e.kcal,food.kcal100,0.0001)
            assertEquals(id,e.protein,food.protein100,0.0001)
            assertEquals(id,e.fat,food.fat100,0.0001)
            assertEquals(id,e.carbs,food.carbs100,0.0001)
            assertFalse(id,food.allergenCompositionReviewed)
        }
        assertEquals(22,OfflineRecipes.ingredients.values.count {
            it.nutrientSourceReference!=null
        })
        assertEquals(0,RecipeEvidenceAudit.report().independentlyVerified)
    }

    @Test fun elevenAdditionalRawAndDryFoodsAreSourcedToSrLegacy() {
        val sources=mapOf(
            "buckwheat" to 170685, "millet" to 169702,
            "apple" to 171688, "banana" to 173944,
            "carrot" to 170393, "lentils" to 172420,
            "mushroom" to 169251, "onion" to 170000,
            "tomato" to 170457, "zucchini" to 169291,
            "bell_pepper" to 170108
        )
        for((id,number) in sources) {
            val ingredient=OfflineRecipes.ingredients.getValue(id)
            assertEquals("https://fdc.nal.usda.gov/food-details/${number}/nutrients",
                ingredient.nutrientSourceReference)
            assertFalse(ingredient.allergenCompositionReviewed)
        }
        assertEquals(22,OfflineRecipes.ingredients.values.count {
            it.nutrientSourceReference!=null
        })
        assertEquals(0,RecipeEvidenceAudit.report().independentlyVerified)
    }

}
