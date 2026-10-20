package com.foodplan.domain

data class GroceryNeed(val ingredientId: String, val grams: Long)
data class PantryStock(val ingredientId: String, val grams: Long)
data class StoreOffer(
    val ingredientId: String, val storeId: String, val regionId: String,
    val packageGrams: Long, val packagePriceKopeks: Long, val confirmed: Boolean
)
data class BasketLine(
    val ingredientId: String, val neededGrams: Long, val pantryGramsUsed: Long,
    val packageCount: Long, val purchasedGrams: Long,
    val totalKopeks: Long, val confirmedPrice: Boolean
)
data class Basket(
    val lines: List<BasketLine>, val totalKopeks: Long,
    val isBudgetVerified: Boolean, val fitsBudget: Boolean?,
    val unavailableIngredients: List<String>
)
object BasketCalculator {
    /** M0 baseline: choose a single pack size per ingredient. M4 will handle pack mixing. */
    fun calculate(
        needs: List<GroceryNeed>, pantry: List<PantryStock>, offers: List<StoreOffer>,
        regionId: String, selectedStores: Set<String>, budgetKopeks: Long
    ): Basket {
        require(budgetKopeks >= 0)
        require(selectedStores.isNotEmpty())
        require(needs.all { it.grams >= 0 && it.ingredientId.isNotBlank() })
        require(pantry.all { it.grams >= 0 })
        require(offers.all { it.packageGrams > 0 && it.packagePriceKopeks >= 0 })
        val aggregated = needs.groupBy { it.ingredientId }
            .mapValues { (_, rows) -> rows.fold(0L) { sum, row -> Math.addExact(sum, row.grams) } }
        val available = pantry.groupBy { it.ingredientId }
            .mapValues { (_, rows) -> rows.fold(0L) { sum, row -> Math.addExact(sum, row.grams) } }
        val lines = mutableListOf<BasketLine>()
        val unavailable = mutableListOf<String>()
        for ((id, required) in aggregated.toSortedMap()) {
            val pantryUsed = minOf(required, available[id] ?: 0L)
            val buyGrams = required - pantryUsed
            if (buyGrams == 0L) {
                lines.add(BasketLine(id, required, pantryUsed, 0, 0, 0, true))
                continue
            }
            val choices = offers.filter {
                it.ingredientId == id && it.regionId == regionId && it.storeId in selectedStores
            }
            if (choices.isEmpty()) { unavailable.add(id); continue }
            val cheapest = choices.map { offer ->
                val packs = buyGrams / offer.packageGrams +
                    if (buyGrams % offer.packageGrams == 0L) 0L else 1L
                Triple(offer, packs, Math.multiplyExact(packs, offer.packagePriceKopeks))
            }.minWith(compareBy<Triple<StoreOffer, Long, Long>> { it.third }
                .thenBy { Math.multiplyExact(it.second, it.first.packageGrams) - buyGrams })
            lines.add(BasketLine(
                id, required, pantryUsed, cheapest.second,
                Math.multiplyExact(cheapest.second, cheapest.first.packageGrams),
                cheapest.third, cheapest.first.confirmed
            ))
        }
        val total = lines.fold(0L) { sum, line -> Math.addExact(sum, line.totalKopeks) }
        val verified = unavailable.isEmpty() && lines.all { it.confirmedPrice }
        return Basket(lines, total, verified, if (verified) total <= budgetKopeks else null, unavailable)
    }
}
