package com.foodplan.ai

import com.foodplan.domain.*

/**
 * Pure offline gate for automatic on-device retail discovery.
 * No profile identifiers, tastes, calories or addresses leave the device.
 * Only the generic ingredient name is submitted to the retailer.
 */
internal object AutomaticRetailScanPolicy {
    const val MAX_WEEK_RESCANS_PER_SESSION = 3

    fun shoppingKey(city: String, shopping: List<ShoppingNeed>): String? {
        val positive = shopping.filter { it.grams > 0 }
        if (city.isBlank() || positive.isEmpty()) return null
        return city.trim().lowercase() + "|" + positive
            .sortedBy { it.ingredient.id }
            .joinToString(";") { "${it.ingredient.id}:${it.grams}" }
    }

    fun canStart(
        selectedShop: String?, isProductsPage: Boolean,
        shoppingKey: String?, alreadyStartedKeys: Set<String>,
        countStarted: Int
    ): Boolean =
        selectedShop == "Магнит" && isProductsPage &&
            shoppingKey != null && shoppingKey !in alreadyStartedKeys &&
            countStarted < MAX_WEEK_RESCANS_PER_SESSION

    /**
     * Only current source-backed mass-priced SKU results are persisted.
     * Count-only/volume-only packages remain quoted without false grams.
     * A retailer price NEVER clears physical allergen trace requirements.
     */
    fun verifiedMassOffers(
        shopping: List<ShoppingNeed>, evidence: List<RetailProductEvidence>,
        city: String, budgetRub: Long, nowEpochMs: Long
    ): List<RetailerOffer> {
        val queries = AnonymousRetailPlanner.build(shopping)
        val quote = RetailWeekQuoter.build(
            queries, evidence, "Магнит", city, budgetRub, nowEpochMs)
        return quote.lines.mapNotNull { line ->
            if (line.estimatedCostKopeks == null) return@mapNotNull null
            val source = evidence.singleOrNull { proof ->
                proof.product.productUrl == line.skuUrl &&
                    proof.product.productName == line.skuName &&
                    proof.product.packGrams != null
            } ?: return@mapNotNull null
            val item = source.product
            val grams = item.packGrams ?: return@mapNotNull null
            RetailerOffer(
                line.ingredientId, source.storeName, source.city,
                item.productName, item.productUrl, grams, item.priceKopeks,
                source.observedAtEpochMs, false, item.availability,
                item.ratingStars, item.reviewCount
            ).takeIf { offer ->
                RetailerOfferChecks.isUsable(
                    offer, line.ingredientId, city, "Магнит", nowEpochMs,
                    indicativePriceOnly = true)
            }
        }
    }
}
