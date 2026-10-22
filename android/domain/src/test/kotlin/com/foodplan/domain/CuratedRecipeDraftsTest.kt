package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class CuratedRecipeDraftsTest {
    @Test fun allDraftsAreDistinctFromGeneratedCombinationsAndStillUnverified() {
        assertEquals(150,OfflineRecipes.curatedDrafts.size)
        assertEquals(200,OfflineRecipes.generatedPrototypes.size)
        assertEquals(350,OfflineRecipes.recipes.size)
        assertEquals(350,OfflineRecipes.recipes.map { it.id }.toSet().size)
        assertTrue(OfflineRecipes.curatedDrafts.all {
            it.evidenceStatus == RecipeEvidenceStatus.CURATED_DRAFT &&
                !it.nutritionVerified && it.sourceReference == null
        })
        assertTrue(OfflineRecipes.generatedPrototypes.all {
            it.evidenceStatus == RecipeEvidenceStatus.GENERATED_TEMPLATE
        })
    }

    @Test fun curatedRecipesHaveKnownWeightsStepsAndKitchenEquipment() {
        assertTrue(OfflineRecipes.curatedDrafts.all { recipe ->
            recipe.ingredients.size >= 3 &&
                recipe.ingredients.all { it.grams in 1..1500 &&
                    it.id in OfflineRecipes.ingredients.keys } &&
                recipe.steps.size >= 3 &&
                recipe.steps.all { it.length >= 25 } &&
                recipe.preparation.totalMinutes >= recipe.preparation.activeMinutes &&
                recipe.preparation.requiredEquipment.all { it in KitchenEquipment.choices } &&
                OfflineRecipes.nutrition(recipe).kcal > 0
        })
    }

    @Test fun noDairyOrEggsInVeganRecipes() {
        val vegan = OfflineRecipes.curatedDrafts.filter { "Веганский" in OfflineRecipes.diets(it) }
        assertTrue(vegan.isNotEmpty())
        assertTrue(vegan.none { item -> item.ingredients.any { part ->
            OfflineRecipes.ingredients.getValue(part.id).group in setOf("meat","fish","dairy","egg")
        } })
        assertFalse("Веганский" in OfflineRecipes.diets(
            OfflineRecipes.curatedDrafts.first { it.id == "cur_shakshuka" }))
    }

    @Test fun curatedRecipesRespectKnownAllergens() {
        val cheeseSalad=OfflineRecipes.curatedDrafts.first { it.id=="cur_greek_salad" }
        val syrniki=OfflineRecipes.curatedDrafts.first { it.id=="cur_syrniki" }
        val tofu=OfflineRecipes.curatedDrafts.first { it.id=="cur_tofu_veg_rice" }
        assertTrue("milk" in OfflineRecipes.allergens(cheeseSalad))
        assertTrue(setOf("milk","egg","wheat").all { it in OfflineRecipes.allergens(syrniki) })
        assertTrue("soy" in OfflineRecipes.allergens(tofu))
    }

    @Test fun aCuratedBreakfastAndMainAreAvailableWithoutAppliances() {
        val noStove=FoodProfile(
            age="30",heightCm="170",weightKg="70",budgetRub="5000",
            appliances=emptySet(),cookingEffort=CookingEffort.FAST
        )
        val eligible = RecipeRanker.rank(noStove,
            OfflineRecipes.curatedDrafts.map(OfflineRecipes::toCandidate),emptyMap())
        assertTrue(eligible.ranked.any { it.recipe.id == "cur_yogurt_berries_oat" })
        assertTrue(eligible.ranked.any { it.recipe.id == "cur_chickpea_salad" })
        assertFalse(eligible.ranked.any { it.recipe.id == "cur_shakshuka" })
    }
    @Test fun threeCookingEffortLevelsActuallyHaveDistinctEligibleDishes() {
        val breakfast=OfflineRecipes.curatedDrafts.first { it.id=="cur_yogurt_berries_oat" }
        val involved=OfflineRecipes.curatedDrafts.first { it.id=="cur_tomato_lentil_pasta" }
        val advanced=OfflineRecipes.curatedDrafts.first { it.id=="cur_beef_cabbage_rolls" }
        assertEquals(CookingEffort.FAST,RecipePreparationRules.classify(breakfast.preparation))
        assertEquals(CookingEffort.INVOLVED,RecipePreparationRules.classify(involved.preparation))
        assertEquals(CookingEffort.ALL_OUT,RecipePreparationRules.classify(advanced.preparation))
        val p=FoodProfile(age="30",heightCm="170",weightKg="70",budgetRub="25000",
            cookingMinutes="200",appliances=setOf("Плита","Сковорода","Кастрюля"))
        assertFalse(RecipePreparationRules.eligible(advanced.preparation,
            p.copy(cookingEffort=CookingEffort.FAST)))
        assertFalse(RecipePreparationRules.eligible(advanced.preparation,
            p.copy(cookingEffort=CookingEffort.INVOLVED)))
        assertTrue(RecipePreparationRules.eligible(advanced.preparation,
            p.copy(cookingEffort=CookingEffort.ALL_OUT)))
    }

}
