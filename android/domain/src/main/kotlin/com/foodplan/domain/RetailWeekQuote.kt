package com.foodplan.domain

/**
 * A seven-day, whole-package price quote from published retailer SKU cards.
 * NOT a confirmed purchase, delivery-address inventory, physical ingredient
 * label, medical allergy clearance, or verified nutritional adequacy.
 */
data class RetailWeekQuoteLine(
    val ingredientId: String,
    val ingredientName: String,
    val requiredGrams: Int,
    val skuName: String?,
    val skuUrl: String?,
    val packages: Long?,
    val estimatedCostKopeks: Long?,
    val explanation: String
)

data class RetailWeekQuote(
    val lines: List<RetailWeekQuoteLine>,
    val pricedIngredientCount: Int,
    val requiredIngredientCount: Int,
    val knownSubtotalKopeks: Long,
    /** Only non-null if EVERY required product has a consistent quoted SKU. */
    val completeEstimateKopeks: Long?,
    val missingIngredientIds: List<String>,
    /** Null unless complete, since missing products can overrun any budget. */
    val estimatedWithinBudget: Boolean?,
    /** The retailer card alone cannot certify address-local price or stock. */
    val actualAddressBasketVerified: Boolean = false,
    /** Merchant copy is not a physical package allergen trace check. */
    val physicalAllergenClearance: Boolean = false
) {
    val hasCompleteQuotedWeek: Boolean get() =
        requiredIngredientCount > 0 && missingIngredientIds.isEmpty() &&
            completeEstimateKopeks != null
}

/** Pure Kotlin. No user profile, budget, allergy or health information leaves device. */
object RetailWeekQuoter {
    fun build(
        requests: List<AnonymousRetailQuery>,
        observed: List<RetailProductEvidence>,
        store: String,
        city: String,
        budgetRub: Long,
        nowEpochMs: Long,
        strictAllergenVerification: Boolean = false
    ): RetailWeekQuote {
        require(budgetRub in 100L..100_000L)
        require(requests.distinctBy { it.ingredientId }.size == requests.size)
        require(requests.all { it.requiredGrams > 0 })

        val quoted = requests.map { need ->
            val matched = observed.filter {
                RetailerProductMatcher.matchesIngredient(
                    need.ingredientId, it.product.productName)
            }
            val audit = DeterministicRetailAuditor.audit(
                need, matched, store, city, nowEpochMs, strictAllergenVerification)
            val best = audit.cheapestComparable
            RetailWeekQuoteLine(
                need.ingredientId, need.genericName, need.requiredGrams,
                best?.evidence?.product?.productName,
                best?.evidence?.product?.productUrl,
                best?.packageCount, best?.weeklyCostKopeks, audit.explanation
            )
        }

        // Never use one vendor SKU to fulfil unrelated food ingredients
        // merely because both textual matchers happened to recognize it.
        val sharedSkuUrls = quoted.filter { it.skuUrl != null }
            .groupBy { it.skuUrl }
            .filterValues { rows -> rows.map { it.ingredientId }.distinct().size > 1 }
            .keys
        val safeLines = quoted.map { line ->
            if (line.skuUrl in sharedSkuUrls)
                line.copy(
                    skuName = null, skuUrl = null, packages = null,
                    estimatedCostKopeks = null,
                    explanation = "Один SKU неоднозначно сопоставлен разным продуктам"
                )
            else line
        }
        val knownSubtotal = safeLines.fold(0L) { total, row ->
            Math.addExact(total, row.estimatedCostKopeks ?: 0L)
        }
        val missing = safeLines.filter { it.estimatedCostKopeks == null }
            .map { it.ingredientId }
        val complete = safeLines.isNotEmpty() && missing.isEmpty()
        val total = knownSubtotal.takeIf { complete }
        val budgetKopeks = Math.multiplyExact(budgetRub, 100L)
        return RetailWeekQuote(
            safeLines, safeLines.size - missing.size, safeLines.size,
            knownSubtotal, total, missing,
            total?.let { it <= budgetKopeks }
        )
    }
}
