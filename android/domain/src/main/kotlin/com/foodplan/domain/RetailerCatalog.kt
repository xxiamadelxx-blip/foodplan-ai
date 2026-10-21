package com.foodplan.domain

import kotlin.math.ceil

enum class ListingAvailability { IN_STOCK, OUT_OF_STOCK, UNKNOWN }

/**
 * Prices captured from a visible retailer catalogue page in an Android WebView.
 * They are public display observations, NOT price/stock at final checkout.
 * Ingredient association must be reviewed by the user and the delivery location
 * must be explicitly confirmed before saving.
 */
data class RetailerOffer(
    val ingredientId: String,
    val storeName: String,
    val city: String,
    val productName: String,
    val productUrl: String,
    val packGrams: Int,
    val priceKopeks: Long,
    val observedAtEpochMs: Long,
    val addressSelectedByUser: Boolean,
    val availability: ListingAvailability = ListingAvailability.UNKNOWN,
    val ratingStars: Double? = null,
    val reviewCount: Int? = null
)

data class ObservedProduct(
    val productName: String,
    val productUrl: String,
    val packGrams: Int?,
    val priceKopeks: Long,
    val availability: ListingAvailability = ListingAvailability.UNKNOWN,
    val ratingStars: Double? = null,
    val reviewCount: Int? = null
)

data class RetailerLineEstimate(
    val ingredientId: String,
    val requiredGrams: Int,
    val productName: String?,
    val productUrl: String?,
    val chosenPackGrams: Int?,
    val packages: Long,
    val costKopeks: Long?
)
data class RetailerBasketEstimate(
    val lines: List<RetailerLineEstimate>,
    val coveredLines: Int,
    val missingLines: Int,
    val knownTotalKopeks: Long,
    val budgetEstimate: Boolean?,
    val verifiedForCheckout: Boolean = false
)

object CatalogValueParser {
    /** Accepts prices such as `89,99 ₽`, but never a crossed-out discount value. */
    fun parseDisplayedRubles(raw: String): Long? {
        val cleaned = raw.replace(Regex("[\\s\\u00A0\\u2009\\u202F\\u200A\\u200B\\u200C\\u200D\\u2060 ₽]"), "")
            .trim().replace(',','.')
        if (!cleaned.matches(Regex("[0-9]{1,7}(\\.[0-9]{1,2})?"))) return null
        val parts = cleaned.split('.')
        val whole = parts[0].toLongOrNull() ?: return null
        val kopeks = if (parts.size == 1) 0L else parts[1].padEnd(2,'0').toLong()
        return whole*100L + kopeks
    }

    /**
     * Parses solid-product pack sizes. Loose per-kg goods, unit-based products,
     * and liquids (mL/L, where density is unknown) need a different quote type.
     */
    fun packageGrams(title: String): Int? {
        // Multi-packs (for example 5х80 г or 2 по 500 г) have a total
        // physical mass different from the one-bag quantity.
        val multipacks = Regex(
            """(?<![\p{L}\d])(\d{1,2})\s*(?:[xх×*]|по)\s*(\d{1,5}(?:[.,]\d{1,3})?)\s*(кг|г|гр|грамм)(?![\p{L}])""",
            RegexOption.IGNORE_CASE
        ).findAll(title).toList()
        val multipack = multipacks.lastOrNull()
        if (multipack != null) {
            val count = multipack.groupValues[1].toIntOrNull() ?: return null
            val unitSize = multipack.groupValues[2].replace(',','.').toDoubleOrNull() ?: return null
            val factor = if (multipack.groupValues[3].lowercase() == "кг") 1000.0 else 1.0
            val grams = count * unitSize * factor
            return if (count > 0 && grams in 1.0..50000.0 && grams % 1.0 == 0.0)
                grams.toInt() else null
        }
        val matches = Regex("(?<![\\p{L}\\d])(\\d{1,5}(?:[.,]\\d{1,3})?)\\s*(кг|г|гр|грамм)(?![\\p{L}])",
            RegexOption.IGNORE_CASE).findAll(title).toList()
        val m = matches.lastOrNull() ?: return null
        val quantity = m.groupValues[1].replace(',','.').toDoubleOrNull() ?: return null
        val multiplier = if (m.groupValues[2].lowercase() == "кг") 1000.0 else 1.0
        val grams = quantity * multiplier
        return if (grams in 1.0..50000.0 && grams % 1.0 == 0.0) grams.toInt() else null
    }
}

/**
 * One fail-closed policy for retailer-observed offers across weekly basket,
 * premium-SKU optimization and extreme-budget fallback.
 * A displayed price is never proof of checkout price or allergy safety.
 */
object RetailerOfferChecks {
    fun isUsable(
        offer: RetailerOffer, ingredientId: String,
        city: String, store: String, nowEpochMs: Long
    ): Boolean {
        // A prefix alone admits query-string substitutions and non-SKU paths.
        // Only canonical retailer product routes may enter ANY price/pack optimizer.
        val officialProductUrl = when (store) {
            "Магнит" -> Regex("""^https://magnit\.ru/product/[A-Za-z0-9_-]+/?$""")
            "ВкусВилл" -> Regex("""^https://vkusvill\.ru/goods/[a-z0-9-]+-[0-9]+/?$""")
            else -> return false // Only reviewed adapters; no arbitrary domains.
        }
        return offer.ingredientId == ingredientId &&
            offer.city == city && offer.storeName == store &&
            offer.addressSelectedByUser &&
            offer.availability != ListingAvailability.OUT_OF_STOCK &&
            officialProductUrl.matches(offer.productUrl) &&
            offer.packGrams in 1..50000 &&
            // A quoted full-pack price must never be used with a different mass.
            // Unclear or unit-count-only titles require explicit mass evidence.
            CatalogValueParser.packageGrams(offer.productName) == offer.packGrams &&
            offer.priceKopeks in 1L..10_000_000L &&
            offer.ratingStars?.let { it.isFinite() && it in 0.0..5.0 } != false &&
            offer.reviewCount?.let { it >= 0 } != false &&
            nowEpochMs >= offer.observedAtEpochMs &&
            nowEpochMs - offer.observedAtEpochMs <= RetailerBasketPricing.MAX_OFFER_AGE_MS &&
            RetailerProductMatcher.matchesIngredient(ingredientId,offer.productName) &&
            !RetailerVariantRiskRegistry.needsManufacturerReview(offer)
    }
}

object RetailerBasketPricing {
    const val MAX_OFFER_AGE_MS = 6L * 60 * 60 * 1000
    fun estimate(
        shopping: List<ShoppingNeed>, prices: List<RetailerOffer>,
        city: String, shop: String, budgetRub: String, nowEpochMs: Long
    ): RetailerBasketEstimate {
        val lines = shopping.map { needed ->
            if (needed.grams == 0) {
                RetailerLineEstimate(needed.ingredient.id,0,null,null,null,0,0)
            } else {
                val offers = prices.filter { offer ->
                    RetailerOfferChecks.isUsable(
                        offer, needed.ingredient.id, city, shop, nowEpochMs
                    )
                }
                val best = offers.map { offer ->
                    val count = ceil(needed.grams.toDouble()/offer.packGrams).toLong()
                    Triple(offer,count,Math.multiplyExact(count,offer.priceKopeks))
                }.minWithOrNull(compareBy<Triple<RetailerOffer,Long,Long>> { it.third }
                    .thenBy { it.first.productUrl })
                if (best == null)
                    RetailerLineEstimate(needed.ingredient.id,needed.grams,
                        null,null,null,0,null)
                else
                    RetailerLineEstimate(needed.ingredient.id,needed.grams,
                        best.first.productName,best.first.productUrl,
                        best.first.packGrams,best.second,best.third)
            }
        }
        val missing = lines.count { it.costKopeks == null }
        val sum = lines.fold(0L) { amount,line ->
            Math.addExact(amount,line.costKopeks?:0L)
        }
        val budget = WeeklyBudget.parse(budgetRub)?.let { it * 100L }
        return RetailerBasketEstimate(lines,
            lines.count { it.costKopeks != null && it.requiredGrams>0 },missing,sum,
            if (missing==0 && budget != null) sum<=budget else null)
    }
}
