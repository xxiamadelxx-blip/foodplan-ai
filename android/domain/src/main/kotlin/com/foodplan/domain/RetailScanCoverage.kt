package com.foodplan.domain

/**
 * User-visible progress for the entire requested basket. Scanner completion is
 * never inferred from the number of errors: one query may have many failing
 * SKU pages, while another query may have zero matching items.
 */
data class RetailCoverageItem(
    val ingredientId: String,
    val name: String,
    val observedSkuCount: Int,
    val comparablePublishedSkuCount: Int,
    val listedInStockCount: Int,
    val missingReliableOffer: Boolean
)

data class RetailCoverageReport(
    val requiredIngredients: Int,
    val ingredientsWithObservedSku: Int,
    val ingredientsWithComparablePublishedSku: Int,
    val ingredientsWithStockClaims: Int,
    val rows: List<RetailCoverageItem>,
    val unresolvedIngredientIds: List<String>,
    /**
     * This scanner has no verified address-local inventory or checkout price.
     * Even 37/37 public product cards do not satisfy the real-store release gate.
     */
    val addressLocalBasketVerified: Boolean = false
)

/** Deterministic coverage by expected ingredient, not by SKU or error count. */
object RetailScanCoverage {
    fun assess(
        queries: List<AnonymousRetailQuery>,
        evidence: List<RetailProductEvidence>,
        store: String,
        city: String,
        nowEpochMs: Long
    ): RetailCoverageReport {
        require(queries.distinctBy { it.ingredientId }.size == queries.size) {
            "One query per ingredient required"
        }
        val rows = queries.map { query ->
            val matching = evidence.filter {
                RetailerProductMatcher.matchesIngredient(
                    query.ingredientId, it.product.productName)
            }.distinctBy { it.product.productUrl }
            val decision = DeterministicRetailAuditor.audit(
                query, matching, store, city, nowEpochMs)
            val comparable = decision.candidates.filter {
                it.status == RetailAuditStatus.COMPARABLE
            }
            // IN_STOCK is the website's published claim, not verification of
            // address-local inventory or the available physical package.
            RetailCoverageItem(
                query.ingredientId,
                query.genericName,
                matching.size,
                comparable.size,
                comparable.count { it.evidence.product.availability == ListingAvailability.IN_STOCK },
                comparable.isEmpty()
            )
        }
        return RetailCoverageReport(
            requiredIngredients = rows.size,
            ingredientsWithObservedSku = rows.count { it.observedSkuCount > 0 },
            ingredientsWithComparablePublishedSku = rows.count {
                it.comparablePublishedSkuCount > 0
            },
            ingredientsWithStockClaims = rows.count { it.listedInStockCount > 0 },
            rows = rows,
            unresolvedIngredientIds = rows.filter { it.missingReliableOffer }.map { it.ingredientId }
        )
    }
}
