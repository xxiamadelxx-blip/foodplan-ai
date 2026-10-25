package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class RecipeEvidenceAuditTest {
    @Test fun templatesAndAuthoredDraftsNeverCountAsVerifiedRecipes() {
        val report=RecipeEvidenceAudit.report()
        assertEquals(350,report.total)
        assertEquals(200,report.generatedTemplates)
        assertEquals(150,report.distinctDrafts)
        assertEquals(0,report.independentlyVerified)
        assertEquals(75,report.documentedReviewed)
        assertEquals(0,report.physicalTrialsRecorded)
        assertFalse(report.meetsM2Gate)
        assertEquals(350,report.issues.size)
    }

    @Test fun nutritionSourceAndBooleanCannotBypassRecipeEvidenceGate() {
        // Construct an explicit unsourced ingredient; catalogue ordering can change.
        val candidate=OfflineRecipes.curatedDrafts.first().copy(
            ingredients=listOf(RecipeIngredient("cottage_cheese",100)),
            nutritionVerified=true,
            evidenceStatus=RecipeEvidenceStatus.INDEPENDENTLY_VERIFIED,
            sourceReference="https://example.org/recipe"
        )
        val inspection=RecipeEvidenceAudit.inspect(candidate)
        assertTrue(RecipeAuditIssue.DOCUMENTARY_REVIEW_MISSING in inspection.issues)
        assertTrue(RecipeAuditNotice.SKU_LABEL_TRACES_UNKNOWN in inspection.notices)
        assertTrue(RecipeAuditNotice.COOKED_YIELD_NOT_MEASURED in inspection.notices)
        assertFalse(inspection.verified)
        assertFalse(inspection.skuLabelCleared)
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
        val audited=RecipeEvidenceAudit.inspect(draft)
        assertFalse(RecipeAuditIssue.UNVERIFIED_COOKED_YIELD in audited.issues)
        assertTrue(RecipeAuditNotice.COOKED_YIELD_NOT_MEASURED in audited.notices)

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
        assertEquals(37,OfflineRecipes.ingredients.values.count {
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
        assertEquals(37,OfflineRecipes.ingredients.values.count {
            it.nutrientSourceReference!=null
        })
        assertEquals(0,RecipeEvidenceAudit.report().independentlyVerified)
    }

    @Test fun newlyPinnedFoodsRemainUnverifiedForRussianSkuAndRecipeSafety() {
        data class Source(val number:Int,val kcal:Double,val protein:Double,val fat:Double,val carbs:Double)
        val sources=mapOf(
            "tofu" to Source(172448,78.0,9.04,4.17,2.85),
            "chickpeas" to Source(173800,139.0,7.05,2.77,22.53),
            "lemon" to Source(167746,29.0,1.1,0.3,9.32)
        )
        for ((id,e) in sources) {
            val ingredient=OfflineRecipes.ingredients.getValue(id)
            assertEquals("https://fdc.nal.usda.gov/food-details/${e.number}/nutrients",
                ingredient.nutrientSourceReference)
            assertEquals(e.kcal,ingredient.kcal100,0.0001)
            assertEquals(e.protein,ingredient.protein100,0.0001)
            assertEquals(e.fat,ingredient.fat100,0.0001)
            assertEquals(e.carbs,ingredient.carbs100,0.0001)
            assertFalse(ingredient.allergenCompositionReviewed)
        }
        assertEquals(37,OfflineRecipes.ingredients.size)
        assertEquals(37,OfflineRecipes.ingredients.values.count {
            it.nutrientSourceReference != null
        })
        assertEquals(0,RecipeEvidenceAudit.report().independentlyVerified)
    }

    @Test fun secondBatchSourcesAreSpecificAndCannotApproveRussianRetailPackages() {
        data class Expected(val fdc:Int, val kcal:Double, val protein:Double,
                            val fat:Double, val carbs:Double)
        val expected = mapOf(
            "pasta" to Expected(168927,371.0,13.04,1.51,74.67),
            "potato" to Expected(170026,77.0,2.05,0.09,17.49),
            "oats" to Expected(173904,379.0,13.15,6.52,67.7),
            "milk" to Expected(172217,61.0,3.15,3.27,4.78),
            "berries" to Expected(168173,35.0,0.43,0.11,9.13),
            "oil" to Expected(171025,884.0,0.0,100.0,0.0),
            "yogurt" to Expected(171284,61.0,3.47,3.25,4.66),
            "cheese" to Expected(171241,356.0,24.94,27.44,2.22)
        )
        for ((name,record) in expected) {
            val ingredient=OfflineRecipes.ingredients.getValue(name)
            assertEquals("https://fdc.nal.usda.gov/food-details/${record.fdc}/nutrients",
                ingredient.nutrientSourceReference)
            assertEquals(name,record.kcal,ingredient.kcal100,0.0001)
            assertEquals(name,record.protein,ingredient.protein100,0.0001)
            assertEquals(name,record.fat,ingredient.fat100,0.0001)
            assertEquals(name,record.carbs,ingredient.carbs100,0.0001)
            assertFalse(name,ingredient.allergenCompositionReviewed)
        }
        assertEquals(37,OfflineRecipes.ingredients.size)
        assertEquals(37,OfflineRecipes.ingredients.values.count {
            it.nutrientSourceReference != null
        })
        assertEquals(0,RecipeEvidenceAudit.report().independentlyVerified)
    }

    @Test fun rawMeatCutReferencesCannotSilentlyStandForEveryOtherCut() {
        data class Expected(val fdc:Int,val kcal:Double,val protein:Double,
                            val fat:Double,val carbs:Double)
        val expected=mapOf(
            "turkey" to Expected(171098,114.0,23.66,1.48,0.14),
            "beef" to Expected(168609,149.0,21.72,6.29,0.0),
            "pork" to Expected(167818,198.0,19.74,12.58,0.0)
        )
        for((name,source) in expected) {
            val ingredient=OfflineRecipes.ingredients.getValue(name)
            assertEquals(name,"https://fdc.nal.usda.gov/food-details/${source.fdc}/nutrients",
                ingredient.nutrientSourceReference)
            assertEquals(name,source.kcal,ingredient.kcal100,0.0001)
            assertEquals(name,source.protein,ingredient.protein100,0.0001)
            assertEquals(name,source.fat,ingredient.fat100,0.0001)
            assertEquals(name,source.carbs,ingredient.carbs100,0.0001)
            assertFalse(name,ingredient.allergenCompositionReviewed)
        }
        assertEquals(37,OfflineRecipes.ingredients.size)
        assertEquals(37,OfflineRecipes.ingredients.values.count {
            it.nutrientSourceReference != null
        })
        val russian=OfflineRecipes.ingredients.getValue("cottage_cheese")
        assertEquals("https://rossmol.ru/tvorog",russian.nutrientSourceReference)
        assertEquals(121.0,russian.kcal100,0.0001)
        assertEquals(16.0,russian.protein100,0.0001)
        assertEquals(5.0,russian.fat100,0.0001)
        assertEquals(3.0,russian.carbs100,0.0001)
        assertEquals(350,russian.packGrams)
        assertFalse(russian.allergenCompositionReviewed)
        assertEquals(0,RecipeEvidenceAudit.report().independentlyVerified)
    }

    @Test fun malformedOrSpoofedSourceLinksCannotQualifyAsIndependentEvidence() {
        val draft=OfflineRecipes.curatedDrafts.first()
        val invalid=listOf(
            "http://example.org/recipe",
            "https://example.org@evil.test/recipe#frag",
            "https://example.org",
            "https://example.org/recipe#fragment",
            "https://example.org/recipe "
        )
        for (link in invalid) {
            val recipe=draft.copy(
                sourceReference=link,
                cookedYieldGrams=500,
                cookedYieldSourceReference="https://example.org/measurements",
                nutritionVerified=true,
                evidenceStatus=RecipeEvidenceStatus.INDEPENDENTLY_VERIFIED
            )
            assertTrue(link, RecipeAuditIssue.INVALID_EVIDENCE_REFERENCE in
                RecipeEvidenceAudit.inspect(recipe).issues)
            assertFalse(RecipeEvidenceAudit.inspect(recipe).verified)
        }
        val badYield=draft.copy(
            sourceReference="https://example.org/recipe",
            cookedYieldGrams=500,
            cookedYieldSourceReference="http://example.org/measurements"
        )
        assertTrue(RecipeAuditIssue.UNVERIFIED_COOKED_YIELD in
            RecipeEvidenceAudit.inspect(badYield).issues)
        assertEquals(0,RecipeEvidenceAudit.report().independentlyVerified)
    }

}
