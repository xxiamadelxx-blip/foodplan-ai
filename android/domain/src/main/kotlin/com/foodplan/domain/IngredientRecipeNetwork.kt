package com.foodplan.domain

/**
 * Deterministic bipartite graph: ingredient <-> recipe.
 *
 * An edge has the required grams for ONE recipe preparation. Edges are created from
 * authored/prototype recipe ingredients, never hallucinated from a product title.
 * A pantry/product can reinforce multiple recipes without counting duplicate IDs twice.
 *
 * This is a recommendation signal, NOT a safety, nutrition, stock or budget guarantee.
 */
data class IngredientRecipeLink(
    val ingredientId: String,
    val recipeId: String,
    val requiredGrams: Int
)

data class RecipeNetworkMatch(
    val recipeId: String,
    val linkedIngredients: Set<String>,
    val missingIngredients: Set<String>,
    val partialIngredients: Set<String>,
    val linkCount: Int,
    val scoreBonus: Int
)

/**
 * The catalog can be indexed once. Lookups from a product are O(number of recipes
 * using that ingredient) rather than rescanning every recipe on every click.
 */
class IngredientRecipeNetwork private constructor(
    private val recipes: List<OfflineRecipe>,
    private val ingredientIndex: Map<String, List<IngredientRecipeLink>>
) {
    /** Staples should not make a recipe appear high-quality just from oil + garlic. */
    private val smallSupportingIngredients = setOf("oil", "garlic")

    fun linksFromProduct(ingredientId: String): List<IngredientRecipeLink> =
        ingredientIndex[ingredientId].orEmpty()

    /**
     * A link counts as fully available only if enough grams exist for a single
     * recipe preparation. Partial stock is shown but does not earn a full link.
     */
    fun matches(availableGrams: Map<String, Int>): List<RecipeNetworkMatch> {
        require(availableGrams.values.all { it >= 0 }) { "Negative pantry/product grams" }
        return recipes.map { recipe ->
            val parts = recipe.ingredients.groupBy { it.id }.mapValues { (_, items) ->
                items.fold(0L) { sum, item -> Math.addExact(sum, item.grams.toLong()) }
            }
            val present = parts.filter { (id, need) ->
                (availableGrams[id] ?: 0).toLong() >= need && need > 0L
            }.keys
            val partial = parts.filter { (id, need) ->
                val grams = availableGrams[id] ?: 0
                grams > 0 && grams.toLong() < need
            }.keys
            val meaningful = present.count { it !in smallSupportingIngredients }
            val support = present.size - meaningful
            // A second substantial ingredient is a stronger signal than the first.
            // Minor staples are never enough to dominate a real protein+grain match.
            val score = (meaningful * 12 +
                (meaningful - 1).coerceAtLeast(0) * 8 + support * 2).coerceAtMost(60)
            RecipeNetworkMatch(recipe.id, present, parts.keys - present, partial,
                present.size, score)
        }.sortedWith(compareByDescending<RecipeNetworkMatch> { it.scoreBonus }
            .thenByDescending { it.linkCount }.thenBy { it.recipeId })
    }

    fun scoreBonuses(availableGrams: Map<String, Int>): Map<String, Int> =
        matches(availableGrams).filter { it.scoreBonus > 0 }
            .associate { it.recipeId to it.scoreBonus }

    companion object {
        fun build(recipes: List<OfflineRecipe>): IngredientRecipeNetwork {
            require(recipes.map { it.id }.distinct().size == recipes.size) {
                "Duplicate recipe IDs are not valid graph nodes"
            }
            val index = linkedMapOf<String, MutableList<IngredientRecipeLink>>()
            recipes.forEach { recipe ->
                val parts = recipe.ingredients.groupBy { it.id }
                parts.forEach { (id, values) ->
                    require(values.all { it.grams > 0 })
                    val total = values.fold(0L) { sum, item ->
                        Math.addExact(sum, item.grams.toLong())
                    }
                    require(total in 1..Int.MAX_VALUE.toLong())
                    index.getOrPut(id) { mutableListOf() }.add(
                        IngredientRecipeLink(id, recipe.id, total.toInt())
                    )
                }
            }
            return IngredientRecipeNetwork(recipes.toList(),index.mapValues { (_, links) ->
                links.sortedBy { it.recipeId }
            })
        }
    }
}

object FoodPlanRecipeNetwork {
    /** Rebuilt by application version, not by an external AI or product search. */
    val catalog: IngredientRecipeNetwork by lazy {
        IngredientRecipeNetwork.build(OfflineRecipes.recipes)
    }
}


/** Public entrypoint for the Recipes screen, product details and pantry suggestions. */
data class ConnectedRecipeRecommendation(
    val recipe: OfflineRecipe,
    val linkedProducts: Set<String>,
    val missingProducts: Set<String>,
    val partiallyAvailableProducts: Set<String>,
    val score: Int
)

object ConnectedRecipeSuggestions {
    fun recommend(
        profile: FoodProfile,
        availableGrams: Map<String, Int>,
        votes: Map<String, TasteRating> = emptyMap(),
        recipes: List<OfflineRecipe> = OfflineRecipes.recipes,
        maxResults: Int = 20
    ): List<ConnectedRecipeRecommendation> {
        require(maxResults > 0)
        if (availableGrams.values.none { it > 0 }) return emptyList()
        val graph = IngredientRecipeNetwork.build(recipes)
        val matches = graph.matches(availableGrams)
            .filter { it.linkCount > 0 }.associateBy { it.recipeId }
        if (matches.isEmpty()) return emptyList()
        val byId = recipes.associateBy { it.id }
        val ranked = RecipeRanker.rank(
            profile, recipes.map(OfflineRecipes::toCandidate), votes,
            ingredientNetworkBonus = graph.scoreBonuses(availableGrams)
        )
        return ranked.ranked.mapNotNull { candidate ->
            val match = matches[candidate.recipe.id] ?: return@mapNotNull null
            val recipe = byId[candidate.recipe.id] ?: return@mapNotNull null
            ConnectedRecipeRecommendation(recipe, match.linkedIngredients,
                match.missingIngredients, match.partialIngredients, candidate.score)
        }.take(maxResults)
    }
}
