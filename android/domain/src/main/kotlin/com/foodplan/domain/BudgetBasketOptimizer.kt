package com.foodplan.domain

import kotlin.math.ceil

/** Price sources are estimates from publicly visible listings, never an order confirmation. */
data class BudgetSelectedItem(
    val ingredientId: String,
    val productName: String,
    val productUrl: String,
    val gramsPerPack: Int,
    val packs: Long,
    val costKopeks: Long,
    val ratingStars: Double?,
    val reviewCount: Int?,
    val availability: ListingAvailability
)
data class BudgetBasket(
    val items: List<BudgetSelectedItem>,
    val missingIngredientIds: List<String>,
    val budgetKopeks: Long,
    val estimatedTotalKopeks: Long,
    val cheapestCompleteBasketKopeks: Long?,
    val fitsBudget: Boolean?,
    val stockConfirmed: Boolean,
    val ratingCoverageCount: Int,
    val budgetResponsiveUpgrades: Int
) {
    val complete get() = missingIngredientIds.isEmpty()
}

object BudgetBasketOptimizer {
    private data class Option(val offer: RetailerOffer, val packs: Long, val cost: Long)
    private data class Upgrade(
        val index: Int, val candidate: Option, val gain: Double, val extra: Long,
        val efficiency: Double
    )

    /**
     * Grocery quantities and nutrition constraints are fixed upstream.
     * This function chooses concrete product variants, never deletes a required ingredient
     * or "spends the budget" by choosing a worse-rated, unnecessarily expensive item.
     *
     * If no price exists, budget compliance is UNKNOWN, not true.
     */
    fun choose(
        shopping: List<ShoppingNeed>, offers: List<RetailerOffer>,
        city: String, store: String, budgetRub: Long, nowEpochMs: Long
    ): BudgetBasket {
        require(budgetRub in 0L..10_000_000L)
        val budget = Math.multiplyExact(budgetRub, 100L)
        val required = shopping.filter { it.grams > 0 }
        val validOffers = RetailerOfferChecks.unambiguousOffers(
            offers, city, store, nowEpochMs
        )
        val all = required.map { need ->
            validOffers.asSequence().filter { item ->
                item.ingredientId == need.ingredient.id
            }.map { item ->
                val packs = ceil(need.grams.toDouble() / item.packGrams).toLong()
                Option(item,packs,Math.multiplyExact(packs,item.priceKopeks))
            }.sortedWith(compareBy<Option> { it.cost }
                .thenByDescending { it.offer.availability == ListingAvailability.IN_STOCK }
                .thenBy { it.offer.productUrl }).toList()
        }
        val missing = required.indices.filter { all[it].isEmpty() }
            .map { required[it].ingredient.id }
        val baseline = all.mapNotNull { it.firstOrNull() }
        val baselineSum = baseline.fold(0L) { sum, opt -> Math.addExact(sum,opt.cost) }
        val selection = baseline.toMutableList()
        var improved = 0
        if (missing.isEmpty() && baselineSum < budget && selection.isNotEmpty()) {
            val spendable = budget - baselineSum
            // Avoid consuming a whole weekly food budget on decorative SKU upgrades.
            // A generous budget allows more quality improvements, never forces spending.
            val maxUpgrade = minOf(spendable, budget / 2)
            var upgradeSpent = 0L
            while (true) {
                val candidates = selection.indices.flatMap { index ->
                    all[index].mapNotNull { option ->
                        val current = selection[index]
                        val extra = option.cost - current.cost
                        val gain = quality(option.offer) - quality(current.offer)
                        if (extra <= 0L || gain < 0.15 || extra > maxUpgrade-upgradeSpent) null
                        else Upgrade(index,option,gain,extra,gain / extra.toDouble())
                    }
                }
                // One upgrade at a time, highest credible quality improvement per ruble.
                val best = candidates.maxWithOrNull(compareBy<Upgrade> { it.efficiency }
                    .thenBy { it.gain }.thenBy { it.candidate.offer.productUrl }) ?: break
                selection[best.index] = best.candidate
                upgradeSpent += best.extra
                improved++
            }
        }
        val items = selection.map { option -> BudgetSelectedItem(
            option.offer.ingredientId,option.offer.productName,option.offer.productUrl,
            option.offer.packGrams,option.packs,option.cost,
            option.offer.ratingStars,option.offer.reviewCount,option.offer.availability
        ) }
        val total = items.fold(0L) { sum,item -> Math.addExact(sum,item.costKopeks) }
        val allPriced = missing.isEmpty()
        return BudgetBasket(
            items,missing,budget,total,if(allPriced) baselineSum else null,
            if(allPriced) total<=budget else null,
            missing.isEmpty() && items.isNotEmpty() &&
                items.all { it.availability == ListingAvailability.IN_STOCK },
            items.count { it.ratingStars != null && (it.reviewCount ?: 0) >= 10 },
            improved
        )
    }

    private fun quality(o: RetailerOffer): Double {
        val stars = o.ratingStars
        val count = o.reviewCount
        // Unknown reviews get neutral prior, NOT an invented customer rating.
        if (stars == null || count == null || count < 10 || !stars.isFinite() ||
            stars !in 0.0..5.0) return 4.0
        return (stars * count + 4.0 * 30.0) / (count + 30.0)
    }

}
