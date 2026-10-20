package com.foodplan.domain

/**
 * Store handoff: a grocery list is not a shopping cart until retailer-side
 * quantities have been checked. This represents a transferable SKU queue only.
 */
data class CartTransferItem(
    val productUrl: String,
    val productName: String,
    val units: Long,
    val packGrams: Int,
    val expectedCostKopeks: Long,
    val ingredientIds: Set<String>
)

data class CartTransferPlan(
    val store: String,
    val city: String,
    val ready: List<CartTransferItem>,
    val missing: List<String>,
    val estimatedTotalKopeks: Long
) {
    val completeCoverage get() = missing.isEmpty()
    val readyCount get() = ready.size
}

object StoreCartTransfer {
    fun build(
        needs: List<ShoppingNeed>,
        offers: List<RetailerOffer>,
        city: String,
        store: String,
        nowEpochMs: Long
    ): CartTransferPlan {
        require(store.isNotBlank() && city.isNotBlank())
        val quotation = RetailerBasketPricing.estimate(
            needs, offers, city, store, "0", nowEpochMs)
        val lines = quotation.lines.filter { it.requiredGrams > 0 }
        val ready = lines.filter { it.productUrl != null && it.costKopeks != null &&
            it.packages > 0 && it.chosenPackGrams != null }
        val missing = lines.filter { it.costKopeks == null }.map { it.ingredientId }
        // De-duplicate identical retailer SKUs when several ingredients map to one product.
        val combined = ready.groupBy { requireNotNull(it.productUrl) }.map { (url, group) ->
            val count = group.fold(0L) { sum, line -> Math.addExact(sum,line.packages) }
            val cost = group.fold(0L) { sum, line ->
                Math.addExact(sum, requireNotNull(line.costKopeks)) }
            CartTransferItem(
                url,group.first().productName.orEmpty(),count,
                requireNotNull(group.first().chosenPackGrams),cost,
                group.map { it.ingredientId }.toSet()
            )
        }.sortedBy { it.productName }
        val total = combined.fold(0L) { sum, item ->
            Math.addExact(sum,item.expectedCostKopeks) }
        return CartTransferPlan(store,city,combined,missing,total)
    }

    fun magnitProductUrlValid(url: String): Boolean =
        url.matches(Regex("^https://magnit\\.ru/product/[a-zA-Z0-9_\\-]+/?$"))
}
