package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class IngredientRecipeNetworkTest {
    private val prep = RecipePreparation(emptySet(), 5, 10, 1, 1)

    private fun recipe(id: String, vararg parts: Pair<String, Int>) = OfflineRecipe(
        id, id, MealType.MAIN,
        parts.map { (name, grams) -> RecipeIngredient(name, grams) },
        listOf("Подготовить продукты.", "Приготовить.", "Подать на стол."),
        prep, emptySet()
    )

    private val one = recipe("only_rice", "rice" to 100, "tomato" to 100)
    private val two = recipe("rice_chicken", "rice" to 100, "chicken" to 200)
    private val other = recipe("potato_only", "potato" to 200)
    private val staples = recipe("oil_garlic", "oil" to 10, "garlic" to 5)
    private val collection = listOf(one, two, other, staples)
    private val graph = IngredientRecipeNetwork.build(collection)

    private fun profile() = FoodProfile(
        age = "30", heightCm = "170", weightKg = "70", budgetRub = "5000",
        appliances = emptySet(), cookingMinutes = "35"
    )

    @Test fun productToRecipeEdgesAreBidirectionalAndDeterministic() {
        assertEquals(listOf("only_rice", "rice_chicken"),
            graph.linksFromProduct("rice").map { it.recipeId })
        assertEquals(100,graph.linksFromProduct("rice").first().requiredGrams)
        assertTrue(graph.linksFromProduct("unknown_product").isEmpty())
        val reversed = IngredientRecipeNetwork.build(collection.reversed())
        assertEquals(graph.linksFromProduct("rice"),reversed.linksFromProduct("rice"))
    }

    @Test fun twoSubstantialLinksBeatOneLink() {
        val byId = graph.matches(mapOf("rice" to 100, "chicken" to 200))
            .associateBy { it.recipeId }
        assertEquals(1,byId.getValue("only_rice").linkCount)
        assertEquals(2,byId.getValue("rice_chicken").linkCount)
        assertTrue(byId.getValue("rice_chicken").scoreBonus >
            byId.getValue("only_rice").scoreBonus)
        assertEquals(setOf("rice","chicken"),byId.getValue("rice_chicken").linkedIngredients)
        assertEquals(setOf("tomato"),byId.getValue("only_rice").missingIngredients)
    }

    @Test fun lowImpactStaplesDoNotDominateTwoRealFoodLinks() {
        val result = graph.matches(mapOf(
            "oil" to 100, "garlic" to 100, "rice" to 100, "chicken" to 200
        ))
        assertEquals("rice_chicken",result.first().recipeId)
        val byId=result.associateBy { it.recipeId }
        assertTrue(byId.getValue("rice_chicken").scoreBonus >
            byId.getValue("oil_garlic").scoreBonus)
    }

    @Test fun incompleteQuantityIsNotCountedAsFullLink() {
        val byId=graph.matches(mapOf("rice" to 100,"chicken" to 20))
            .associateBy { it.recipeId }
        val r=byId.getValue("rice_chicken")
        assertEquals(1,r.linkCount)
        assertEquals(setOf("chicken"),r.partialIngredients)
        assertEquals(setOf("chicken"),r.missingIngredients)
    }

    @Test fun repeatedSameIngredientStillCreatesOnlyOneEdge() {
        val repeated=IngredientRecipeNetwork.build(listOf(
            recipe("repeated","rice" to 75,"rice" to 25)
        ))
        assertEquals(1,repeated.linksFromProduct("rice").size)
        assertEquals(100,repeated.linksFromProduct("rice").single().requiredGrams)
        assertEquals(0,repeated.matches(mapOf("rice" to 90)).single().linkCount)
        assertEquals(1,repeated.matches(mapOf("rice" to 100)).single().linkCount)
    }

    @Test fun graphPriorityReachesRealRankerWithoutDisablingSafety() {
        val bonus=graph.scoreBonuses(mapOf("rice" to 100,"chicken" to 200))
        val ranked=RecipeRanker.rank(profile(),listOf(one,two,other).map(OfflineRecipes::toCandidate),
            emptyMap(),ingredientNetworkBonus=bonus)
        assertEquals("rice_chicken",ranked.ranked.first().recipe.id)

        val allergic=RecipeRanker.rank(profile().copy(allergens="chicken"),
            listOf(one,two).map(OfflineRecipes::toCandidate),emptyMap(),
            ingredientNetworkBonus=bonus)
        assertFalse(allergic.ranked.any { it.recipe.id=="rice_chicken" })
        assertEquals(CandidateRejection.ALLERGEN,allergic.rejected["rice_chicken"])
    }

    @Test fun connectedSuggestionsShowLinksButRespectExclusions() {
        val matches=ConnectedRecipeSuggestions.recommend(
            profile().copy(disliked="rice"),
            mapOf("rice" to 100,"chicken" to 200,"potato" to 200),
            recipes=listOf(one,two,other))
        assertEquals(listOf("potato_only"),matches.map { it.recipe.id })
        assertEquals(setOf("potato"),matches.single().linkedProducts)
    }

    @Test fun zeroOrNegativeStockCannotCreateFalseRecommendation() {
        assertTrue(ConnectedRecipeSuggestions.recommend(profile(),emptyMap(),recipes=collection).isEmpty())
        try {
            graph.matches(mapOf("rice" to -1))
            fail("Negative inventory must be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected: fail-closed input validation.
        }
    }
}
