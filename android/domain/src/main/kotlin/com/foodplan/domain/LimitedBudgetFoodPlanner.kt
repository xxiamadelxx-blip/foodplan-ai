package com.foodplan.domain

import kotlin.math.max

/** Informational under-budget food availability, NOT a medically adequate meal plan.
 * Never invent prices. Live observations and explicitly typed prices remain distinct.
 */
enum class LimitedFoodStatus {
    PARTIAL_AFFORDABLE_FOOD, NO_RELIABLE_PRICES, NOTHING_AFFORDABLE,
    BLOCKED_SAFETY_RESTRICTIONS
}
enum class LimitedPriceOrigin { OBSERVED_LISTING, MANUALLY_ENTERED }
data class LimitedFoodLine(
    val ingredientId: String,
    val productName: String,
    val packageGrams: Int,
    val packages: Int,
    val totalKopeks: Long,
    val origin: LimitedPriceOrigin,
    val availability: ListingAvailability,
    val nutritionEstimate: Nutrition
)
data class LimitedFoodBasket(
    val status: LimitedFoodStatus,
    val budgetKopeks: Long,
    val spentKopeks: Long,
    val lines: List<LimitedFoodLine>,
    val totalNutritionEstimate: Nutrition,
    /** Energy equivalent only: NOT a number of days a person can safely eat this food. */
    val equivalentDaysByCalories: Double?,
    val remainingWeekEnergyGapKcal: Double?,
    val warnings: List<String>
)

object LimitedBudgetFoodPlanner {
    private val staples = setOf("potato","rice","pasta","buckwheat","oats",
        "millet","lentils","chickpeas")
    private val proteins = setOf("chicken","turkey","beef","pork","cod",
        "salmon","tofu","egg","lentils","chickpeas","milk","cottage_cheese")
    private val vegetables = setOf("carrot","cabbage","broccoli","zucchini",
        "tomato","bell_pepper","cucumber","onion")
    private val animalMeat = setOf("chicken","turkey","beef","pork")
    private val fish = setOf("cod","salmon")
    private val dairyOrEgg = setOf("milk","cheese","cottage_cheese","yogurt","egg")

    private data class Candidate(
        val id:String, val name:String, val grams:Int, val price:Long,
        val origin:LimitedPriceOrigin, val stock:ListingAvailability
    )

    fun choose(
        profile: FoodProfile,
        store: String,
        offers: List<RetailerOffer>,
        manualPricesKopeks: Map<String, Long>,
        nowEpochMs: Long,
        calorieTargetPerPerson: Int? = null
    ): LimitedFoodBasket {
        val budgetRub = WeeklyBudget.parse(profile.budgetRub)
            ?: return blocked(0L,LimitedFoodStatus.BLOCKED_SAFETY_RESTRICTIONS,
                "Укажите целый бюджет от 100 до 100 000 ₽.")
        val budget=budgetRub*100L
        val allergies=AllergenTerms.parse(profile.allergens)
        if (allergies.unknown.isNotEmpty() || allergies.terms.isNotEmpty())
            return blocked(budget,LimitedFoodStatus.BLOCKED_SAFETY_RESTRICTIONS,
                "Без подтверждённых этикеток конкретных товаров нельзя подбирать продукты при пищевой аллергии.")
        // Religious food requirements cannot be validated from raw-food names,
        // nominal ingredient groups or public shop search snippets. A cheap
        // fallback must not bypass the same safety gate as the weekly planner.
        if (profile.diet in setOf("Халяль", "Кошерное", "Постное"))
            return blocked(budget, LimitedFoodStatus.BLOCKED_SAFETY_RESTRICTIONS,
                "Для выбранных правил питания необходимо подтверждение состава и происхождения конкретных товаров. Без этих данных автоматический подбор недоступен.")
        val disliked=IngredientRestrictions.parse(profile.disliked)
        if (disliked.unknown.isNotEmpty())
            return blocked(budget,LimitedFoodStatus.BLOCKED_SAFETY_RESTRICTIONS,
                "Неизвестные исключённые продукты нужно уточнить перед подбором.")
        val allowed: (String)->Boolean = { id ->
            id !in disliked.ingredientIds &&
                when(profile.diet) {
                    "Веганский" -> id !in animalMeat && id !in fish && id !in dairyOrEgg
                    "Вегетарианский" -> id !in animalMeat && id !in fish
                    "Пескетарианский" -> id !in animalMeat
                    else -> true
                }
        }
        val observations = offers.filter { o ->
            o.ingredientId in OfflineRecipes.ingredients &&
                allowed(o.ingredientId) &&
                RetailerOfferChecks.isUsable(o,o.ingredientId,profile.city,store,nowEpochMs)
        }.map { Candidate(it.ingredientId,it.productName,it.packGrams,
            it.priceKopeks,LimitedPriceOrigin.OBSERVED_LISTING,it.availability) }
        val manual = manualPricesKopeks.mapNotNull { (id,price) ->
            val food=OfflineRecipes.ingredients[id] ?: return@mapNotNull null
            if (!allowed(id) || price !in 1L..10_000_000L) return@mapNotNull null
            Candidate(id,food.name,food.packGrams,price,
                LimitedPriceOrigin.MANUALLY_ENTERED,ListingAvailability.UNKNOWN)
        }
        val candidates=(observations+manual).filter { it.id!="oil" }
            .distinctBy { Triple(it.id,it.grams,it.origin) to it.price }
        if (candidates.isEmpty())
            return blocked(budget,LimitedFoodStatus.NO_RELIABLE_PRICES,
                "Нет актуальных наблюдаемых цен или введённых вручную цен для допустимых продуктов. Цены не придумываются.")
        val selected=mutableListOf<Candidate>()
        var spent=0L
        // Only priced full packages are purchased. No fractional package or free food.
        // Select one staple first, then diversify into protein and vegetables if
        // affordable; afterward prefer energy coverage and additional distinct foods.
        repeat(30) {
            val affordable=candidates.filter { it.price<=budget-spent }
            if (affordable.isEmpty()) return@repeat
            val purchasedIds=selected.map { it.id }.toSet()
            val options = when {
                selected.isEmpty() && affordable.any { it.id in staples } ->
                    affordable.filter { it.id in staples }
                selected.isNotEmpty() && purchasedIds.none { it in proteins } &&
                    affordable.any { it.id in proteins } ->
                    affordable.filter { it.id in proteins }
                selected.isNotEmpty() && purchasedIds.none { it in vegetables } &&
                    affordable.any { it.id in vegetables } ->
                    affordable.filter { it.id in vegetables }
                else -> affordable
            }
            val best=options.minWithOrNull(
                compareBy<Candidate> { it.id in purchasedIds }
                    .thenBy { it.price }
                    .thenByDescending { OfflineRecipes.ingredients.getValue(it.id).kcal100*it.grams/100.0 }
                    .thenBy { it.id }
            ) ?: return@repeat
            selected+=best
            spent+=best.price
        }
        if (selected.isEmpty())
            return blocked(budget,LimitedFoodStatus.NOTHING_AFFORDABLE,
                "По известным ценам нет ни одной доступной полной упаковки за этот бюджет.")
        // Keep pack size and unit price in the grouping key: similarly named SKUs
        // with different weights are never collapsed into an incorrect quantity.
        val lines=selected.groupBy { it }.map { (offer,items) ->
            val count=items.size
            val ingredient=OfflineRecipes.ingredients.getValue(offer.id)
            val ratio=offer.grams.toDouble()*count/100.0
            LimitedFoodLine(offer.id,offer.name,offer.grams,count,
                items.sumOf { it.price },offer.origin,offer.stock,
                Nutrition(ingredient.kcal100*ratio,ingredient.protein100*ratio,
                    ingredient.fat100*ratio,ingredient.carbs100*ratio))
        }
        val total=lines.fold(Nutrition.ZERO) { acc,line -> acc+line.nutritionEstimate }
        val persons=profile.household.toIntOrNull()?.coerceIn(1,20) ?: 1
        val reference=calorieTargetPerPerson?.takeIf { it>0 }
        val days=reference?.let { total.kcal/it/persons }
        val gap=reference?.let { max(0.0,it.toDouble()*persons*7-total.kcal) }
        return LimitedFoodBasket(LimitedFoodStatus.PARTIAL_AFFORDABLE_FOOD,budget,spent,lines,total,
            days,gap,listOf(
                "Это доступные по указанным ценам продукты, а не полноценный рацион. Нельзя считать дефицит КБЖУ безопасным.",
                "Число дней отражает только условный эквивалент калорий: не срок безопасного питания.",
                "БЖУ рассчитаны для указанного сырья до возможных пищевых отходов и потерь при готовке.",
                "Ручные цены, наличие, масса нетто и этикетки товаров требуют проверки перед покупкой."
            ))
    }
    private fun blocked(budget:Long,status:LimitedFoodStatus,reason:String) =
        LimitedFoodBasket(status,budget,0,emptyList(),Nutrition.ZERO,null,null,listOf(reason))
}
