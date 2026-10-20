package com.foodplan.domain

/**
 * Public retailer product cards with multiple possible manufacturers.
 *
 * Their published nutrition/trace disclosures are supplier-dependent.
 * A URL and displayed price cannot identify the package's actual producer.
 * Reject these known cases from generic-nutrient shopping until the exact
 * manufacturer's SKU, net edible mass and package label are independently checked.
 *
 * This is a narrowly reviewed risk list, NOT proof that other product cards
 * have only one supplier or are safe for allergies.
 */
data class RetailerVariantRisk(
    val ingredientId: String,
    val retailerProductUrl: String,
    val reason: String,
    val observedSupplierCount: Int
)

object RetailerVariantRiskRegistry {
    private val known = listOf(
        RetailerVariantRisk(
            "oats",
            "https://vkusvill.ru/goods/gerkules-ovsyanye-khlopya-1248/",
            "3 поставщика с разными КБЖУ; у одного указаны возможные следы орехов, у всех — глютенсодержащие злаки.",
            3
        ),
        RetailerVariantRisk(
            "egg",
            "https://vkusvill.ru/goods/yaytso-kurinoe-s1-15544/",
            "Несколько птицефабрик: опубликованы разные КБЖУ на 100 г. Масса съедобной части 10 яиц не определена.",
            8
        )
    )

    /** Query strings can be tracking metadata; never derive a new retailer host. */
    fun riskFor(productUrl: String): RetailerVariantRisk? {
        val canonical = productUrl.substringBefore('?').trimEnd('/') + "/"
        return known.firstOrNull { it.retailerProductUrl == canonical }
    }

    fun needsManufacturerReview(offer: RetailerOffer): Boolean =
        riskFor(offer.productUrl)?.ingredientId == offer.ingredientId
}
