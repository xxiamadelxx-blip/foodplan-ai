package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class RecipeRankerTest {
    private val quick = RecipePreparation(setOf("Плита"), 10, 15, 1, 1)
    private fun candidate(
        id: String,
        ingredients: Set<String> = setOf("курица"),
        allergens: Set<String> = emptySet(),
        complete: Boolean = true,
        diets: Set<String> = setOf("Всеядный"),
        prep: RecipePreparation = quick,
        tags: Set<String> = emptySet()
    ) = RecipeCandidate(id, id, RecipeSafety(ingredients, allergens, complete), diets, prep, tags)

    private fun user() = FoodProfile(
        age="28", heightCm="170", weightKg="75", budgetRub="5000",
        appliances=setOf("Плита"), cookingMinutes="35"
    )

    @Test fun surveyHasExactly25UniqueDishes() {
        assertEquals(25, TasteSurvey.dishes.size)
        assertEquals(25, TasteSurvey.dishes.map { it.id }.toSet().size)
    }
    @Test fun likedTagsRankHigher() {
        val r = RecipeRanker.rank(user(), listOf(
            candidate("fish", tags=setOf("рыба")),
            candidate("chicken", tags=setOf("курица"))
        ), mapOf("chicken_katsu" to TasteRating.LIKE, "fried_cod" to TasteRating.DISLIKE))
        assertEquals(listOf("chicken","fish"), r.ranked.map { it.recipe.id })
    }
    @Test fun notTriedDoesNotPenaltyOrReward() {
        val r = RecipeRanker.rank(user(), listOf(
            candidate("a", tags=setOf("рис")), candidate("b", tags=setOf("курица"))
        ), mapOf("sushi" to TasteRating.NEVER_TRIED))
        assertEquals(listOf("a","b"), r.ranked.map { it.recipe.id })
    }
    @Test fun excludeAllergensAndUnknownComposition() {
        val p = user().copy(allergens="АРАХИС")
        val r = RecipeRanker.rank(p, listOf(
            candidate("peanuts", allergens=setOf("арахис")),
            candidate("peanut_ingredient", ingredients=setOf("арахис")),
            candidate("unknown", complete=false),
            candidate("okay")
        ), emptyMap())
        assertEquals(listOf("okay"), r.ranked.map { it.recipe.id })
        assertEquals(CandidateRejection.ALLERGEN, r.rejected["peanuts"])
        assertEquals(CandidateRejection.ALLERGEN, r.rejected["peanut_ingredient"])
        assertEquals(CandidateRejection.UNKNOWN_COMPOSITION, r.rejected["unknown"])
    }
    @Test fun enforceDietAndTimeAndEquipment() {
        val p = user().copy(diet="Веганский")
        val r = RecipeRanker.rank(p, listOf(
            candidate("animal"),
            candidate("vegan", diets=setOf("Веганский")),
            candidate("oven", diets=setOf("Веганский"), prep=quick.copy(requiredEquipment=setOf("Духовка"))),
            candidate("slow", diets=setOf("Веганский"), prep=quick.copy(totalMinutes=60))
        ), emptyMap())
        assertEquals(listOf("vegan"), r.ranked.map { it.recipe.id })
        assertEquals(CandidateRejection.DIET, r.rejected["animal"])
        assertEquals(CandidateRejection.EQUIPMENT_OR_TIME_OR_EFFORT, r.rejected["oven"])
        assertEquals(CandidateRejection.EQUIPMENT_OR_TIME_OR_EFFORT, r.rejected["slow"])
    }
    @Test fun noDuplicateMealBiasAndNoUnsafeOverride() {
        val p = user().copy(allergens="арахис")
        val r = RecipeRanker.rank(p, listOf(
            candidate("fav", tags=setOf("курица")),
            candidate("new", tags=setOf("курица")),
            candidate("unsafe", allergens=setOf("арахис"), tags=setOf("курица"))
        ), mapOf("chicken_katsu" to TasteRating.LIKE), mapOf("fav" to 2))
        assertEquals(listOf("new","fav"), r.ranked.map { it.recipe.id })
        assertEquals(CandidateRejection.ALLERGEN, r.rejected["unsafe"])
    }
}
