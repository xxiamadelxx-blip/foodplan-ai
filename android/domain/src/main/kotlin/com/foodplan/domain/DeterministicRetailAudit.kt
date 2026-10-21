package com.foodplan.domain

import kotlin.math.ceil

/**
 * M4 deterministic discovery plan. No user profile, health history, taste votes,
 * name, address or budget is sent in a search term. Only a food name is searched.
 * Individual vegetables remain individual needs; "овощи" is not a purchasable SKU.
 */
data class AnonymousRetailQuery(
    val ingredientId: String,
    val genericName: String,
    val family: String,
    val requiredGrams: Int,
    val searchTerms: List<String>
)

object AnonymousRetailPlanner {
    private val terms = mapOf(
        "rice" to listOf("рис длиннозерный", "рис"),
        "chicken" to listOf("филе грудки куриное", "филе цыпленка"),
        "buckwheat" to listOf("гречка ядрица"),
        "pasta" to listOf("макароны"),
        "potato" to listOf("картофель"),
        "oats" to listOf("овсяные хлопья"),
        "millet" to listOf("пшено"),
        "milk" to listOf("молоко"),
        "apple" to listOf("яблоки"),
        "banana" to listOf("бананы"),
        "pear" to listOf("груши"),
        "berries" to listOf("клубника замороженная"),
        "turkey" to listOf("филе индейки"),
        "beef" to listOf("говядина"),
        "pork" to listOf("свинина корейка"),
        "cod" to listOf("треска"),
        "salmon" to listOf("лосось"),
        "tofu" to listOf("тофу"),
        "chickpeas" to listOf("нут консервированный"),
        "broccoli" to listOf("брокколи"),
        "carrot" to listOf("морковь"),
        "zucchini" to listOf("кабачки"),
        "cabbage" to listOf("капуста белокочанная"),
        "tomato" to listOf("помидоры"),
        "mushroom" to listOf("шампиньоны"),
        "oil" to listOf("масло подсолнечное"),
        "egg" to listOf("яйцо куриное"),
        "cottage_cheese" to listOf("творог"),
        "flour" to listOf("мука пшеничная"),
        "onion" to listOf("лук репчатый"),
        "garlic" to listOf("чеснок"),
        "cucumber" to listOf("огурцы"),
        "bell_pepper" to listOf("перец сладкий"),
        "lemon" to listOf("лимоны"),
        "lentils" to listOf("чечевица"),
        "yogurt" to listOf("йогурт натуральный"),
        "cheese" to listOf("сыр гауда")
    )
    fun coverage(): Set<String> = terms.keys

    fun build(needs: List<ShoppingNeed>): List<AnonymousRetailQuery> =
        needs.filter { it.grams > 0 }.groupBy { it.ingredient.id }.map { (id, parts) ->
            val food = OfflineRecipes.ingredients.getValue(id)
            val searches = requireNotNull(terms[id]) { "No search policy for $id" }
            AnonymousRetailQuery(id, searches.first(), food.group,
                parts.sumOf { it.grams }, searches)
        }.sortedWith(compareBy<AnonymousRetailQuery> {
            when (it.ingredientId) { "chicken" -> 0; "rice" -> 1; else -> 2 }
        }.thenBy { it.family }.thenBy { it.genericName })
}

/**
 * Detail evidence must come from the exact retail product / label, not a
 * generic USDA nutrient record. Missing fields are UNKNOWN, never inferred.
 */
data class RetailProductEvidence(
    val product: ObservedProduct,
    val storeName: String,
    val city: String,
    val observedAtEpochMs: Long,
    val addressSelectedByUser: Boolean,
    val manufacturer: String? = null,
    val ingredientsText: String? = null,
    val identitySpec: String? = null,
    val labelSourceUrl: String? = null,
    val labelChecked: Boolean = false,
    val barcode: String? = null,
    val soldByWeight: Boolean = false,
    val explicitAllergenReview: Boolean = false
)

enum class RetailAuditStatus {
    REJECTED, NEEDS_PRODUCT_DETAILS, COMPARABLE, NO_OFFERS
}
data class RetailAuditedOffer(
    val evidence: RetailProductEvidence,
    val status: RetailAuditStatus,
    val reasons: List<String>,
    val rublesPerKg: Double?,
    val packageCount: Long?,
    val weeklyCostKopeks: Long?
)
data class RetailPriceDecision(
    val ingredientId: String,
    val candidates: List<RetailAuditedOffer>,
    val cheapestComparable: RetailAuditedOffer?,
    /** true only within an explicitly identical verified spec and ingredient list. */
    val alternativesEquivalentOnLabels: Boolean,
    val explanation: String
)

/**
 * Selection never uses brand prestige, price or star count as proof of quality.
 * A checked composition/spec means "comparable on these label attributes",
 * NOT laboratory-identical quality, guaranteed safety or checkout availability.
 */
object DeterministicRetailAuditor {
    private const val MAX_AGE_MS = 6L * 60 * 60 * 1000
    private val normalizedSpace = Regex("\\s+")
    private val nonWord = Regex("[^\\p{L}\\p{N}]+")
    private fun normalized(text: String): String =
        text.lowercase().replace('ё', 'е').trim()
            .replace(nonWord, " ").replace(normalizedSpace, " ").trim()

    /** Hard state/type constraints required by the underlying reference food. */
    private fun specCompatible(id: String, spec: String): Boolean {
        val s = normalized(spec)
        return when (id) {
            "rice" -> ("длиннозерный" in s || "длиннозерное" in s) &&
                ("шлифованный" in s || "белый" in s) &&
                !listOf("бурый", "пропаренный", "круглозерный", "среднезерный").any { it in s }
            "chicken" -> ("филе" in s || "грудка без кости" in s) &&
                ("кури" in s || "цыплен" in s) &&
                !listOf("бедро", "голень", "крыло", "тушка", "с костью", "маринад").any { it in s }
            else -> s.isNotBlank()
        }
    }

    private fun canonicalSpec(id: String, spec: String): String {
        val s = normalized(spec)
        return when (id) {
            // Retail titles include unrelated branding; compare actual variety,
            // grain treatment, and composition instead of the brand.
            "rice" -> if (specCompatible(id, s)) "rice/white-longgrain/polished/dry" else s
            "chicken" -> if (specCompatible(id, s) &&
                listOf("без кожи", "без костей").all { it in s })
                "chicken/breast-fillet/skinless/boneless" else s
            else -> s // Unknown kinds are NOT safely brand-normalized.
        }
    }

    private fun supportedUrl(store: String, url: String): Boolean = when (store) {
        "Магнит" -> Regex("^https://magnit\\.ru/product/[A-Za-z0-9_/-]+(?:\\?[A-Za-z0-9_=&%-]+)?$").matches(url)
        "ВкусВилл" -> Regex("^https://vkusvill\\.ru/goods/[A-Za-z0-9_/-]+(?:\\?[A-Za-z0-9_=&%-]+)?$").matches(url)
        else -> false
    }

    fun audit(
        need: AnonymousRetailQuery,
        offers: List<RetailProductEvidence>,
        store: String,
        city: String,
        nowEpochMs: Long,
        strictAllergenVerification: Boolean = false
    ): RetailPriceDecision {
        val entries = offers.map { input ->
            val p = input.product
            val reasons = mutableListOf<String>()
            if (input.storeName != store || input.city != city ||
                !input.addressSelectedByUser) reasons += "Магазин или адрес не подтверждён"
            if (!supportedUrl(store, p.productUrl)) reasons += "Недоверенная ссылка на SKU"
            if (nowEpochMs < input.observedAtEpochMs ||
                nowEpochMs - input.observedAtEpochMs > MAX_AGE_MS)
                reasons += "Цена устарела или дата некорректна"
            if (p.availability == ListingAvailability.OUT_OF_STOCK)
                reasons += "Нет в наличии"
            if (p.priceKopeks !in 1..10_000_000 ||
                p.packGrams == null || p.packGrams !in 1..50000 ||
                CatalogValueParser.packageGrams(p.productName) != p.packGrams ||
                input.soldByWeight) reasons += "Не подтверждена цена целой упаковки и масса"
            if (!RetailerProductMatcher.matchesIngredient(need.ingredientId, p.productName))
                reasons += "Другое сырьё, часть туши, сорт или обработка"
            val risk = RetailerVariantRiskRegistry.riskFor(p.productUrl)
            if (risk?.ingredientId == need.ingredientId)
                reasons += "Карточка объединяет разных поставщиков"
            if (reasons.isNotEmpty()) {
                RetailAuditedOffer(input, RetailAuditStatus.REJECTED, reasons,
                    null, null, null)
            } else {
                val grams = requireNotNull(p.packGrams)
                val count = ceil(need.requiredGrams.toDouble() / grams).toLong()
                val total = Math.multiplyExact(count, p.priceKopeks)
                val unit = p.priceKopeks.toDouble() * 10.0 / grams
                val missing = mutableListOf<String>()
                if (input.manufacturer.isNullOrBlank()) missing += "Не указан производитель"
                if (input.ingredientsText.isNullOrBlank()) missing += "Нет состава с этикетки"
                if (input.identitySpec.isNullOrBlank() ||
                    !specCompatible(need.ingredientId, input.identitySpec))
                    missing += "Не проверены сорт, часть продукта и обработка"
                if (!input.labelChecked ||
                    input.labelSourceUrl != p.productUrl)
                    missing += "Нет подтверждения данных именно этого SKU"
                if (strictAllergenVerification && !input.explicitAllergenReview)
                    missing += "Не проверены аллергены конкретной этикетки"
                RetailAuditedOffer(input,
                    if (missing.isEmpty()) RetailAuditStatus.COMPARABLE
                    else RetailAuditStatus.NEEDS_PRODUCT_DETAILS,
                    missing, unit, count, total)
            }
        }
        val comparable = entries.filter { it.status == RetailAuditStatus.COMPARABLE }
        // Different specs/compositions are NOT interchangeable: compare only
        // the cheapest group with the identical normalized labels.
        val groups = comparable.groupBy {
            canonicalSpec(need.ingredientId, requireNotNull(it.evidence.identitySpec)) + "|" +
                normalized(requireNotNull(it.evidence.ingredientsText))
        }
        val selectedGroup = groups.values.minWithOrNull(
            compareBy<List<RetailAuditedOffer>> { group ->
                group.minOf { it.weeklyCostKopeks ?: Long.MAX_VALUE }
            }.thenBy { group -> group.minOf { it.evidence.product.productUrl } }
        ).orEmpty()
        val cheapest = selectedGroup.minWithOrNull(
            compareBy<RetailAuditedOffer> { it.weeklyCostKopeks ?: Long.MAX_VALUE }
                .thenBy { it.evidence.product.productUrl }
        )
        val sameLabel = selectedGroup.size >= 2
        val explanation = when {
            entries.isEmpty() -> "Товары по запросу не получены"
            cheapest == null -> "Не хватает проверенного состава, изготовителя или характеристик: не объявлять дорогой и дешёвый товар одинаковыми"
            groups.size > 1 -> "Есть разные составы или характеристики: сравнение только внутри каждой группы"
            sameLabel -> "По проверенным данным на этикетках товары сопоставимы; доплата за бренд не доказана. Органолептика и безопасность не гарантируются"
            else -> "Есть одна проверенная позиция; цена конкурентов пока не подтверждена"
        }
        return RetailPriceDecision(need.ingredientId, entries, cheapest,
            sameLabel, explanation)
    }
}
