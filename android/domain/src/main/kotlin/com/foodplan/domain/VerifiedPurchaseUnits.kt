package com.foodplan.domain

import kotlin.math.ceil

enum class PurchaseUnit { GRAM, PIECE, MILLILITRE }

data class PurchasePack(val unit: PurchaseUnit, val quantity: Int) {
    init { require(quantity > 0) }
}

data class UnitConversionEvidence(val gramsPerUnit: Double, val source: String) {
    init {
        require(gramsPerUnit.isFinite() && gramsPerUnit > 0)
        require(source.startsWith("https://"))
    }
}

object VerifiedPurchaseUnits {
    fun packs(requiredGrams: Int, pack: PurchasePack, evidence: UnitConversionEvidence? = null): Int? {
        require(requiredGrams >= 0)
        if (requiredGrams == 0) return 0
        val capacity = if (pack.unit == PurchaseUnit.GRAM) pack.quantity.toDouble()
            else pack.quantity * (evidence?.gramsPerUnit ?: return null)
        val count = ceil(requiredGrams / capacity)
        return if (count.isFinite() && count <= Int.MAX_VALUE) count.toInt() else null
    }
}


/**
 * Checked PUBLIC PRODUCT PAGE identifiers for pack-only prices. These are
 * discovery URLs and physical pack units, never baked-in retail prices.
 * Count/volume must be visible in the fresh on-page title at scan time.
 */
object MagnitPackOnlySkuCatalog {
    data class Entry(
        val ingredientId: String,
        val url: String,
        val pack: PurchasePack
    )

    val entries = listOf(
        Entry("egg",
            "https://magnit.ru/product/3454700001-yaytso_stolovoe_s1_10sht_boks_20",
            PurchasePack(PurchaseUnit.PIECE, 10)),
        Entry("oil",
            "https://magnit.ru/product/1000029331-maslo_podsolnechnoe_rafinirovannoe_dezodorirovannoe_900ml",
            PurchasePack(PurchaseUnit.MILLILITRE, 900))
    )

    fun matching(product: ObservedProduct): Entry? = entries.singleOrNull { entry ->
        product.productUrl.trimEnd('/') == entry.url &&
            product.packGrams == null &&
            product.purchasePack == entry.pack &&
            RetailerProductMatcher.matchesIngredient(entry.ingredientId, product.productName) &&
            when (entry.pack.unit) {
                PurchaseUnit.PIECE -> Regex(
                    """(?<![\p{L}\d])${entry.pack.quantity}\s*(?:шт\.?|штук)(?![\p{L}\d])""",
                    RegexOption.IGNORE_CASE).containsMatchIn(product.productName)
                PurchaseUnit.MILLILITRE -> Regex(
                    """(?<![\p{L}\d])${entry.pack.quantity}\s*мл(?![\p{L}\d])""",
                    RegexOption.IGNORE_CASE).containsMatchIn(product.productName)
                PurchaseUnit.GRAM -> false
            }
    }

    fun forUrl(url: String): Entry? =
        entries.singleOrNull { it.url == url.trimEnd('/') }
}
