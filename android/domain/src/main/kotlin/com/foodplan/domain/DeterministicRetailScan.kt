package com.foodplan.domain

/** A search request is not an invented retailer URL. Adapter resolves it only via permitted access. */
data class NeutralGroceryNeed(
    val ingredientId: String,
    val category: String,
    val searchTerms: List<String>,
    val grams: Int
)

data class RetailScanStep(
    val ingredientId: String,
    val category: String,
    val query: String,
    val requiredGrams: Int,
    val phase: String
)

object RetailScanPlanner {
    /** Pilot chicken first, then each unique ingredient, without private brands or user profile data. */
    fun plan(needs: List<NeutralGroceryNeed>, pilotId: String = "chicken"): List<RetailScanStep> {
        val clean = needs.filter { it.grams > 0 }.groupBy { it.ingredientId }
            .map { (id, group) ->
                require(id.isNotBlank())
                require(group.all { it.category == group.first().category && it.searchTerms == group.first().searchTerms })
                val grams = group.fold(0L) { acc, n -> Math.addExact(acc, n.grams.toLong()) }
                require(grams <= Int.MAX_VALUE)
                val n = group.first()
                require(n.category.isNotBlank() && n.searchTerms.isNotEmpty())
                NeutralGroceryNeed(id, n.category, n.searchTerms, grams.toInt())
            }.sortedBy { it.ingredientId }
        val pilot = clean.firstOrNull { it.ingredientId == pilotId }
        val sorted = listOfNotNull(pilot) + clean.filterNot { it.ingredientId == pilotId }
        return sorted.mapIndexed { index, n ->
            RetailScanStep(n.ingredientId, n.category, n.searchTerms.first().trim(),
                n.grams, if (index == 0 && pilot != null) "PILOT" else "FULL_SCAN")
        }
    }
}

enum class EquivalenceStatus { EQUIVALENT, DIFFERENT, UNKNOWN }

data class RetailSkuEvidence(
    val skuId: String,
    val ingredientId: String,
    val productName: String,
    val productUrl: String,
    val producer: String?,
    val variety: String?,
    val processing: String?,
    val ingredientLabel: String?,
    val packGrams: Int?,
    val edibleGrams: Int?,
    val packagePriceKopeks: Long?,
    val allergensReviewed: Boolean,
    val observedAtEpochMs: Long,
    val store: String,
    val city: String
)

data class SkuComparison(
    val equivalence: EquivalenceStatus,
    val recommendedSkuId: String?,
    val reason: String,
    val firstTotalKopeks: Long?,
    val secondTotalKopeks: Long?
)

object DeterministicSkuComparator {
    private fun canonical(s: String?): String? = s?.lowercase()?.replace('ё','е')
        ?.trim()?.replace(Regex("\\s+"), " ")?.takeIf { it.isNotBlank() }

    fun compare(a: RetailSkuEvidence, b: RetailSkuEvidence, requiredGrams: Int,
                allergyConstraints: Boolean = false): SkuComparison {
        require(requiredGrams > 0)
        val first = total(a, requiredGrams)
        val second = total(b, requiredGrams)
        if (a.ingredientId != b.ingredientId)
            return SkuComparison(EquivalenceStatus.DIFFERENT,null,"Different ingredient IDs",first,second)
        if (allergyConstraints && (!a.allergensReviewed || !b.allergensReviewed))
            return SkuComparison(EquivalenceStatus.UNKNOWN,null,"Allergen label evidence missing",first,second)
        // Distinct food varieties, physical processing or composition => do not claim equivalence.
        val attrs = listOf(
            canonical(a.variety) to canonical(b.variety),
            canonical(a.processing) to canonical(b.processing),
            canonical(a.ingredientLabel) to canonical(b.ingredientLabel)
        )
        if (attrs.any { (x,y) -> x != null && y != null && x != y })
            return SkuComparison(EquivalenceStatus.DIFFERENT,null,"Different variety, processing or label",first,second)
        if (attrs.any { (x,y) -> x == null || y == null })
            return SkuComparison(EquivalenceStatus.UNKNOWN,null,"Variety, processing or composition unverified",first,second)
        if (first == null || second == null)
            return SkuComparison(EquivalenceStatus.UNKNOWN,null,"Package price or edible mass unknown",first,second)
        if (!officialSkuUrl(a) || !officialSkuUrl(b) || a.skuId == b.skuId)
            return SkuComparison(EquivalenceStatus.UNKNOWN,null,
                "Official retailer SKU identity not verified or duplicate",first,second)
        if (a.store != b.store || a.city != b.city ||
            a.observedAtEpochMs <= 0 || b.observedAtEpochMs <= 0)
            return SkuComparison(EquivalenceStatus.UNKNOWN,null,"Retailer, locality or observation missing",first,second)
        // Producer is documented, not a price/quality bonus. Equal evidence means lower total cost wins.
        val selected = if (first < second) a.skuId else if (second < first) b.skuId
            else minOf(a.skuId,b.skuId)
        return SkuComparison(EquivalenceStatus.EQUIVALENT,selected,
            "Equivalent evidenced food; choose lower full-pack cost",first,second)
    }

    /** Do not treat a third-party or spoofed URL as evidence of a retailer SKU. */
    private fun officialSkuUrl(s: RetailSkuEvidence): Boolean {
        val prefix = when (s.store) {
            "Магнит" -> "https://magnit.ru/product/"
            "ВкусВилл" -> "https://vkusvill.ru/goods/"
            else -> return false
        }
        if (!s.productUrl.startsWith(prefix) ||
            s.productUrl.contains('?') || s.productUrl.contains('#') ||
            s.productUrl.contains('@') || s.productUrl.contains('%') ||
            s.productUrl.contains("//", startIndex = "https://".length)) return false
        val tail = s.productUrl.removePrefix(prefix).removeSuffix("/")
        return tail.isNotBlank() && tail.matches(Regex("[a-zA-Z0-9_-]+"))
    }

    private fun total(s: RetailSkuEvidence, requiredGrams: Int): Long? {
        val grams = s.edibleGrams ?: return null
        val pack = s.packGrams ?: return null
        val price = s.packagePriceKopeks ?: return null
        if (grams <= 0 || pack < grams || pack > 50_000 || price <= 0 ||
            s.skuId.isBlank() || !s.productUrl.startsWith("https://")) return null
        val count = (requiredGrams.toLong() + grams - 1) / grams
        return try { Math.multiplyExact(count,price) } catch (_: ArithmeticException) { null }
    }
}
