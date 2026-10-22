package com.foodplan.domain

/**
 * Links retailer-observed SKUs to recipes. These are shopping *candidates*,
 * never pantry inventory, confirmed checkout stock, or package-label evidence.
 *
 * Pure Kotlin: no AI, network connection or Android dependency.
 */
data class RetailerRecipeLine(
    val ingredientId: String,
    val requiredGrams: Int,
    val skuName: String,
    val skuUrl: String,
    val packageGrams: Int,
    val packageCount: Long,
    val displayedCostKopeks: Long,
    val availability: ListingAvailability
)

data class RetailerRecipeSuggestion(
    val recipe: OfflineRecipe,
    val linkedLines: List<RetailerRecipeLine>,
    val missingIngredientIds: Set<String>,
    val observedSubtotalKopeks: Long,
    /** Null if even one ingredient has no eligible, current shop offer. */
    val estimatedRecipeCostKopeks: Long?,
    /** True only if every chosen offer has explicitly confirmed IN_STOCK. */
    val addressStockConfirmed: Boolean,
    val score: Int
) {
    val linkedCount: Int get() = linkedLines.size
}

/**
 * Name-based matching only: restrictive enough to reject obvious substitutions.
 * It NEVER certifies composition, safety, origin or nutrient equivalence.
 */
object RetailerRecipeNamePolicy {
    fun compatible(offer: RetailerOffer): Boolean {
        val title = offer.productName.lowercase().replace('ё', 'е')
        return when (offer.ingredientId) {
            // The FoodPlan canonical chicken ingredient is boneless raw fillet.
            "chicken" ->
                (title.contains("филе") || title.contains("грудк")) &&
                    listOf("бедр", "голен", "крыл", "тушка", "кост", "в панировке")
                        .none { it in title }
            // Canonical rice reference is white long-grain, not arbitrary rice.
            "rice" -> title.contains("длиннозерн") &&
                listOf("пропар", "бурый", "кругл", "смесь", "для плова")
                    .none { it in title }
            else -> true
        }
    }
}

object RetailerRecipeNetwork {
    /**
     * Creates graph suggestions from one explicitly selected shop and city.
     * Hidden/blocked/stale/out-of-stock or incompatible cards cannot create links.
     *
     * No allergy-specific menu can be made here: retailer name/price observations
     * have not proven exact-SKU label or cross-contact traceability.
     */
    fun recommend(
        profile: FoodProfile,
        offers: List<RetailerOffer>,
        store: String,
        city: String,
        nowEpochMs: Long,
        votes: Map<String, TasteRating> = emptyMap(),
        selectedIngredientIds: Set<String> = emptySet(),
        recipes: List<OfflineRecipe> = OfflineRecipes.recipes,
        maxResults: Int = 25
    ): List<RetailerRecipeSuggestion> {
        require(maxResults in 1..500)
        require(nowEpochMs >= 0L)
        val allergy = AllergenTerms.parse(profile.allergens)
        if (allergy.unknown.isNotEmpty() || allergy.terms.isNotEmpty()) return emptyList()
        val dislikes = IngredientRestrictions.parse(profile.disliked)
        if (dislikes.unknown.isNotEmpty()) return emptyList()
        val normalized = profile.copy(allergens = "",
            disliked = dislikes.ingredientIds.joinToString(","))

        val usable = RetailerOfferChecks.unambiguousOffers(
            offers, city, store, nowEpochMs
        ).filter { o ->
            (selectedIngredientIds.isEmpty() || o.ingredientId in selectedIngredientIds) &&
                RetailerRecipeNamePolicy.compatible(o)
        }

        if (usable.isEmpty()) return emptyList()
        val byIngredient = usable.groupBy { it.ingredientId }
        // Recipe-network bonus counts DISTINCT matching ingredients, not duplicate
        // SKU cards or how many packs are for sale. No actual stock is assumed.
        val matchedIngredientIds = byIngredient.keys
        val graph = IngredientRecipeNetwork.build(recipes)
        val bonuses = graph.scoreBonuses(
            matchedIngredientIds.associateWith { Int.MAX_VALUE })
        val ranked = RecipeRanker.rank(
            normalized, recipes.map(OfflineRecipes::toCandidate), votes,
            ingredientNetworkBonus = bonuses)

        val recipesById = recipes.associateBy { it.id }
        return ranked.ranked.mapNotNull { entry ->
            val recipe = recipesById[entry.recipe.id] ?: return@mapNotNull null
            val demands = recipe.ingredients.groupBy { it.id }.mapValues { (_, parts) ->
                parts.fold(0L) { total, part ->
                    Math.addExact(total, part.grams.toLong())
                }
            }
            val links = demands.mapNotNull { (ingredientId, needGrams) ->
                val offer = byIngredient[ingredientId].orEmpty().mapNotNull { candidate ->
                    val count = (needGrams + candidate.packGrams.toLong() - 1L) /
                        candidate.packGrams.toLong()
                    val cost = try {
                        Math.multiplyExact(count, candidate.priceKopeks)
                    } catch (_: ArithmeticException) { return@mapNotNull null }
                    Triple(candidate, count, cost)
                }.minWithOrNull(compareBy<Triple<RetailerOffer,Long,Long>> { it.third }
                    .thenBy { it.first.productUrl }) ?: return@mapNotNull null
                RetailerRecipeLine(ingredientId, needGrams.toInt(), offer.first.productName,
                    offer.first.productUrl, offer.first.packGrams, offer.second,
                    offer.third, offer.first.availability)
            }.sortedBy { it.ingredientId }
            if (links.isEmpty()) return@mapNotNull null
            val covered = links.map { it.ingredientId }.toSet()
            val missing = demands.keys - covered
            val subtotal = links.fold(0L) { sum, item ->
                Math.addExact(sum, item.displayedCostKopeks)
            }
            RetailerRecipeSuggestion(
                recipe, links, missing, subtotal,
                if (missing.isEmpty()) subtotal else null,
                missing.isEmpty() && links.all {
                    it.availability == ListingAvailability.IN_STOCK
                },
                entry.score
            )
        }.take(maxResults)
    }
}
