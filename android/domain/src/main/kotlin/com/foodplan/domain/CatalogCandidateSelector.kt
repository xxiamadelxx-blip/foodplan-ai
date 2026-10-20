package com.foodplan.domain

import kotlin.math.ceil

data class SuggestedRetailerProduct(
    val ingredientId: String,
    val product: ObservedProduct,
    val requiredPacks: Long,
    val approximateCostKopeks: Long
)

/** Suggests raw-like grocery SKUs on a visible retailer page.
 * User reviews the suggestion before it is accepted into the shopping basket.
 */
object CatalogCandidateSelector {
    private val processedFoodWords = listOf(
        "готовый","готовое блюдо","салат","суп ","пирог","печенье",
        "котлет","пельмен","чипсы","соус","сухари","батончик","лапша быстрого"
    )
    fun suggest(needs: List<ShoppingNeed>, products: List<ObservedProduct>): List<SuggestedRetailerProduct> =
        needs.filter { it.grams > 0 }.mapNotNull { need ->
            val candidates = products.asSequence().filter { product ->
                val p=product.packGrams
                p != null && p in 1..50000 && product.priceKopeks > 0 &&
                    validMagnitProductUrl(product.productUrl) &&
                    RetailerProductMatcher.matchesIngredient(need.ingredient.id,product.productName) &&
                    !processedFoodWords.any { word -> word in product.productName.lowercase() }
            }.map { p ->
                val count=ceil(need.grams.toDouble()/requireNotNull(p.packGrams)).toLong()
                SuggestedRetailerProduct(
                    need.ingredient.id,p,count,
                    Math.multiplyExact(count,p.priceKopeks)
                )
            }.toList()
            candidates.minWithOrNull(compareBy<SuggestedRetailerProduct> { it.approximateCostKopeks }
                .thenBy { it.product.productUrl })
        }

    /**
     * Pulls ALL matching variants from the observed page, not only the cheapest.
     * Missing catalog coverage remains missing. Unknown stock and ratings stay unknown.
     */
    fun suggestForBudget(
        needs: List<ShoppingNeed>, products: List<ObservedProduct>,
        city: String, budgetRub: Long, capturedAt: Long
    ): List<SuggestedRetailerProduct> {
        val candidates=needs.filter { it.grams > 0 }.flatMap { need ->
            products.filter { product ->
                val grams=product.packGrams
                grams != null && grams in 1..50000 &&
                    product.priceKopeks > 0L &&
                    validMagnitProductUrl(product.productUrl) &&
                    RetailerProductMatcher.matchesIngredient(
                        need.ingredient.id,product.productName) &&
                    !processedFoodWords.any { it in product.productName.lowercase() }
            }.map { product ->
                RetailerOffer(
                    need.ingredient.id,"Магнит",city,product.productName,
                    product.productUrl,requireNotNull(product.packGrams),
                    product.priceKopeks,capturedAt,true,
                    product.availability,product.ratingStars,product.reviewCount
                )
            }
        }
        val optimized=BudgetBasketOptimizer.choose(
            needs,candidates,city,"Магнит",budgetRub,capturedAt
        )
        return optimized.items.mapNotNull { item ->
            val product=products.firstOrNull { it.productUrl==item.productUrl } ?: return@mapNotNull null
            SuggestedRetailerProduct(
                item.ingredientId,product,item.packs,item.costKopeks
            )
        }
    }


    /**
     * Preserve multiple reviewed SKUs per ingredient. Selection is a separate step
     * because affordability depends on the FULL basket, not sticker price alone.
     */
    fun allMatchingVariants(
        needs: List<ShoppingNeed>, products: List<ObservedProduct>,
        maxPerIngredient: Int = 12
    ): List<SuggestedRetailerProduct> {
        require(maxPerIngredient in 1..50)
        return needs.filter { it.grams > 0 }.flatMap { need ->
            products.asSequence().filter { product ->
                product.packGrams in 1..50000 && product.priceKopeks > 0 &&
                    validMagnitProductUrl(product.productUrl) &&
                    RetailerProductMatcher.matchesIngredient(
                        need.ingredient.id,product.productName) &&
                    !processedFoodWords.any { word -> word in product.productName.lowercase() }
            }.distinctBy { it.productUrl }.map { product ->
                val packs = ceil(need.grams.toDouble()/requireNotNull(product.packGrams)).toLong()
                SuggestedRetailerProduct(
                    need.ingredient.id,product,packs,
                    Math.multiplyExact(packs,product.priceKopeks)
                )
            }.sortedWith(compareBy<SuggestedRetailerProduct> { it.approximateCostKopeks }
                .thenBy { it.product.productUrl })
                .take(maxPerIngredient).toList()
        }
    }

    private fun validMagnitProductUrl(url: String): Boolean =
        url.matches(Regex("""^https://magnit\.ru/product/[A-Za-z0-9_-]+/?$"""))

}
