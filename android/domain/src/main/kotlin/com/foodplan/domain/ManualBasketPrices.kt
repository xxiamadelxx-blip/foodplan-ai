package com.foodplan.domain

/** User-entered prices are estimates, never represented as live shop prices. */
data class PriceEstimate(
    val knownTotalKopeks: Long,
    val pricedLines: Int,
    val missingLines: Int,
    val estimateFitsBudget: Boolean?
) {
    val fullyPriced get() = missingLines == 0
    val verifiedShopPrices: Boolean get() = false
}

object ManualBasketPrices {
    fun fromRubles(text: String): Long? {
        val normalized = text.trim().replace(',','.')
        if (!normalized.matches(Regex("[0-9]{1,7}(\\.[0-9]{1,2})?"))) return null
        val parts = normalized.split('.')
        val whole = parts[0].toLong()
        val cents = if (parts.size == 1) 0L else parts[1].padEnd(2,'0').toLong()
        val value = Math.addExact(Math.multiplyExact(whole, 100L),cents)
        // A zero or implausibly large price must not turn an unpriced item into
        // a 'known' affordable SKU. Limit matches the other price engines.
        return value.takeIf { it in 1L..10_000_000L }
    }
    fun formatKopeks(value: Long): String {
        require(value >= 0)
        return "${value/100}.${(value%100).toString().padStart(2,'0')}"
    }

    fun estimate(
        shopping: List<ShoppingNeed>, pricesByIngredient: Map<String, Long>,
        budgetRub: String
    ): PriceEstimate {
        var subtotal = 0L
        var known = 0
        var missing = 0
        for (need in shopping) {
            if (need.packsToBuy == 0) continue
            val price = pricesByIngredient[need.ingredient.id]
            if (price == null || price !in 1L..10_000_000L) missing++ else {
                subtotal = Math.addExact(subtotal, Math.multiplyExact(
                    price, need.packsToBuy.toLong()))
                known++
            }
        }
        val budget = WeeklyBudget.parse(budgetRub)?.let { Math.multiplyExact(it.toLong(),100L) }
        return PriceEstimate(subtotal, known, missing,
            if (missing == 0 && budget != null) subtotal <= budget else null)
    }
}
