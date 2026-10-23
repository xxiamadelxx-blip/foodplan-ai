package com.foodplan.domain

import kotlin.math.ceil

enum class PurchaseUnit {
    GRAM, PIECE, MILLILITRE,
    /** Gross retail banana mass WITH peel, not USDA edible fruit grams. */
    UNPEELED_GRAM,
    /** Net can contents WITH brine, not drained chickpea grams. */
    NET_WITH_LIQUID_GRAM
}

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
        val capacity = when (pack.unit) {
            PurchaseUnit.GRAM -> pack.quantity.toDouble()
            PurchaseUnit.PIECE, PurchaseUnit.MILLILITRE ->
                pack.quantity * (evidence?.gramsPerUnit ?: return null)
            // A generic source URL does not establish this physical SKU's
            // drained or peeled edible yield. Never infer such a conversion.
            PurchaseUnit.UNPEELED_GRAM, PurchaseUnit.NET_WITH_LIQUID_GRAM ->
                return null
        }
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
            PurchasePack(PurchaseUnit.MILLILITRE, 900)),
        Entry("banana", "https://magnit.ru/product/9072651501-banany",
            PurchasePack(PurchaseUnit.UNPEELED_GRAM, 1000)),
        Entry("chickpeas",
            "https://magnit.ru/product/8000013399-green_ribbon_nut_400g_zh_b_s_kl",
            PurchasePack(PurchaseUnit.NET_WITH_LIQUID_GRAM, 400))
    )

    fun matching(product: ObservedProduct): Entry? = entries.singleOrNull { entry ->
        product.productUrl.trimEnd('/') == entry.url &&
            product.packGrams == null &&
            product.purchasePack == entry.pack &&
            (
                RetailerProductMatcher.matchesIngredient(entry.ingredientId, product.productName) ||
                // Official fixed canned SKU is explicitly price-only; its H1
                // omits 'консервированный'. Do NOT loosen generic matching.
                (entry.ingredientId == "chickpeas" &&
                    Regex("""^Нут\${part}+Green\${part}+Ribbon\${part}+400\${part}*г$""",
                        RegexOption.IGNORE_CASE).matches(product.productName))
            ) &&
            when (entry.pack.unit) {
                PurchaseUnit.PIECE -> Regex(
                    """(?<![\p{L}\d])${entry.pack.quantity}\s*(?:шт\.?|штук)(?![\p{L}\d])""",
                    RegexOption.IGNORE_CASE).containsMatchIn(product.productName)
                PurchaseUnit.MILLILITRE -> Regex(
                    """(?<![\p{L}\d])${entry.pack.quantity}\s*мл(?![\p{L}\d])""",
                    RegexOption.IGNORE_CASE).containsMatchIn(product.productName)
                PurchaseUnit.UNPEELED_GRAM, PurchaseUnit.NET_WITH_LIQUID_GRAM ->
                    (Regex(
                        """(?<![\${part}{L}\${part}])${entry.pack.quantity}\${part}*г(?![\${part}{L}\${part}])""",
                        RegexOption.IGNORE_CASE).containsMatchIn(product.productName) ||
                    (entry.pack.quantity % 1000 == 0 && Regex(
                        """(?<![\${part}{L}\${part}])${entry.pack.quantity / 1000}\${part}*кг(?![\${part}{L}\${part}])""",
                        RegexOption.IGNORE_CASE).containsMatchIn(product.productName)))
                PurchaseUnit.GRAM -> false
            }
    }

    fun forUrl(url: String): Entry? =
        entries.singleOrNull { it.url == url.trimEnd('/') }
}
