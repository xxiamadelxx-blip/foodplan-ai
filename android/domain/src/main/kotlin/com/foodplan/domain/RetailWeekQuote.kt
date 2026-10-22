package com.foodplan.domain

/**
 * A seven-day, whole-package price quote from published retailer SKU cards.
 * NOT a confirmed purchase, local inventory, physical ingredient label,
 * medical allergy clearance, or verified nutritional adequacy. No address needed.
 */
data class RetailWeekQuoteLine(
    val ingredientId: String,
    val ingredientName: String,
    val requiredGrams: Int,
    val skuName: String?,
    val skuUrl: String?,
    val packages: Long?,
    val estimatedCostKopeks: Long?,
    val explanation: String,
    /** Observed physical units even when edible-gram conversion is UNKNOWN. */
    val observedPack: PurchasePack? = null,
    /** Price for ONE published physical package, never inferred week cost. */
    val observedPackPriceKopeks: Long? = null
)

data class RetailWeekQuote(
    val lines: List<RetailWeekQuoteLine>,
    val pricedIngredientCount: Int,
    val requiredIngredientCount: Int,
    val knownSubtotalKopeks: Long,
    /** Only non-null if EVERY required product has a consistent quoted SKU. */
    val completeEstimateKopeks: Long?,
    val missingIngredientIds: List<String>,
    /** Null unless complete, since missing products can overrun any budget. */
    val estimatedWithinBudget: Boolean?,
    /** Legacy flag: public chain prices never certify actual checkout. */
    val actualAddressBasketVerified: Boolean = false,
    /** Merchant copy is not a physical package allergen trace check. */
    val physicalAllergenClearance: Boolean = false,
    /** A strict lower bound. Missing items can only ADD cost, not reduce it. */
    val knownSubtotalExceedsBudget: Boolean = false
) {
    val hasCompleteQuotedWeek: Boolean get() =
        requiredIngredientCount > 0 && missingIngredientIds.isEmpty() &&
            completeEstimateKopeks != null
}

/** Pure Kotlin. No user profile, budget, allergy or health information leaves device. */
object RetailWeekQuoter {
    fun build(
        requests: List<AnonymousRetailQuery>,
        observed: List<RetailProductEvidence>,
        store: String,
        city: String,
        budgetRub: Long,
        nowEpochMs: Long,
        strictAllergenVerification: Boolean = false
    ): RetailWeekQuote {
        require(budgetRub in 100L..100_000L)
        require(requests.distinctBy { it.ingredientId }.size == requests.size)
        require(requests.all { it.requiredGrams > 0 })

        // Price-only selection: retailer title, whole-package mass, actual
        // visible price, store host and observation age. Manufacturer, label,
        // address and local inventory are NOT required to estimate what a
        // grocery chain typically charges. They remain distinct from food
        // safety, nutrition equivalence or an actual shopping confirmation.
        // Allergies affect eligibility to eat or recommend a product,
        // not whether the store's visible number can be shown as a PRICE.
        // physicalAllergenClearance remains false for all price-only quotes.
        val prices = observed.mapNotNull { evidence ->
            val product = evidence.product
            val grams = product.packGrams ?: return@mapNotNull null
            val matched = requests.filter {
                RetailerProductMatcher.matchesIngredient(
                    it.ingredientId, product.productName)
            }
            if (matched.size != 1) return@mapNotNull null
            RetailerOffer(
                matched.single().ingredientId, evidence.storeName, evidence.city,
                product.productName, product.productUrl, grams,
                product.priceKopeks, evidence.observedAtEpochMs,
                false, product.availability, product.ratingStars, product.reviewCount
            )
        }
        val usable = RetailerOfferChecks.unambiguousOffers(
            prices, city, store, nowEpochMs, indicativePriceOnly = true)

        // Count/volume package observations are a DIFFERENT unit dimension.
        // Preserve a checked per-carton/per-bottle price, but never invent a
        // conversion from recipe edible grams to 10 eggs or 900 millilitres.
        // Conflicting prices for one public URL invalidate that URL entirely.
        val unitObservations = observed.asSequence().filter { evidence ->
            val p = evidence.product
            evidence.storeName == "Магнит" && store == "Магнит" &&
                nowEpochMs >= evidence.observedAtEpochMs &&
                nowEpochMs - evidence.observedAtEpochMs <=
                    RetailerBasketPricing.MAX_INDICATIVE_AGE_MS &&
                p.priceKopeks in 1L..10_000_000L &&
                MagnitPackOnlySkuCatalog.matching(p) != null
        }.groupBy { it.product.productUrl }.values.mapNotNull { entries ->
            val first = entries.first().product
            if (entries.any { e ->
                    e.product.productName != first.productName ||
                        e.product.priceKopeks != first.priceKopeks ||
                        e.product.purchasePack != first.purchasePack
                }) null
            else entries.maxByOrNull { it.observedAtEpochMs }
        }
        val quoted = requests.map { need ->
            val best = usable.asSequence().filter {
                it.ingredientId == need.ingredientId
            }.map { offer ->
                val count = (need.requiredGrams.toLong() + offer.packGrams - 1L) /
                    offer.packGrams
                val price = Math.multiplyExact(count, offer.priceKopeks)
                Triple(offer, count, price)
            }.minWithOrNull(compareBy<Triple<RetailerOffer, Long, Long>> { it.third }
                .thenBy { it.first.productUrl })
            val packOnly = if (best == null) unitObservations.singleOrNull { evidence ->
                MagnitPackOnlySkuCatalog.matching(evidence.product)?.ingredientId ==
                    need.ingredientId
            } else null
            val pack = packOnly?.product?.purchasePack
            val unknownCount = if (pack != null)
                VerifiedPurchaseUnits.packs(need.requiredGrams, pack)
            else null
            // Unit conversion is not independently certified, so count
            // intentionally stays null. A known single-pack price is useful,
            // but NOT a full-week shopping quote.
            RetailWeekQuoteLine(
                need.ingredientId, need.genericName, need.requiredGrams,
                best?.first?.productName ?: packOnly?.product?.productName,
                best?.first?.productUrl ?: packOnly?.product?.productUrl,
                best?.second ?: unknownCount?.toLong(), best?.third,
                when {
                    best != null ->
                        "Примерная цена сети; состав и аллергены этим не подтверждаются"
                    packOnly != null ->
                        "Цена одной упаковки известна, количество упаковок за неделю неизвестно без проверенного пересчёта единиц"
                    else ->
                        "У сети нет подходящего предложения с читаемой ценой и фасовкой"
                },
                observedPack = pack,
                observedPackPriceKopeks = packOnly?.product?.priceKopeks
            )
        }

        // Never use one vendor SKU to fulfil unrelated food ingredients
        // merely because both textual matchers happened to recognize it.
        val sharedSkuUrls = quoted.filter { it.skuUrl != null }
            .groupBy { it.skuUrl }
            .filterValues { rows -> rows.map { it.ingredientId }.distinct().size > 1 }
            .keys
        val safeLines = quoted.map { line ->
            if (line.skuUrl in sharedSkuUrls)
                line.copy(
                    skuName = null, skuUrl = null, packages = null,
                    estimatedCostKopeks = null,
                    observedPack = null, observedPackPriceKopeks = null,
                    explanation = "Один SKU неоднозначно сопоставлен разным продуктам"
                )
            else line
        }
        val knownSubtotal = safeLines.fold(0L) { total, row ->
            Math.addExact(total, row.estimatedCostKopeks ?: 0L)
        }
        val missing = safeLines.filter { it.estimatedCostKopeks == null }
            .map { it.ingredientId }
        val complete = safeLines.isNotEmpty() && missing.isEmpty()
        val total = knownSubtotal.takeIf { complete }
        val budgetKopeks = Math.multiplyExact(budgetRub, 100L)
        return RetailWeekQuote(
            safeLines, safeLines.size - missing.size, safeLines.size,
            knownSubtotal, total, missing,
            total?.let { it <= budgetKopeks },
            knownSubtotalExceedsBudget = knownSubtotal > budgetKopeks
        )
    }
}
