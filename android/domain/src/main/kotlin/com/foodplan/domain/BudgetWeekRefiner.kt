package com.foodplan.domain

/**
 * A retailer-price-aware iteration over an already safety-filtered 7-day plan.
 * It never assumes unpriced goods cost zero and NEVER guarantees nutritional adequacy.
 * All variants are obtained through the same restrictions as the standard meal replacement.
 */
data class BudgetWeekResult(
    val plan: PlanningResult.Success,
    val basket: BudgetBasket,
    val numberOfMealChanges: Int,
    val status: BudgetWeekStatus
)
enum class BudgetWeekStatus { MISSING_PRICES, WITHIN_BUDGET, OVER_BUDGET }

object BudgetWeekRefiner {
    private val proteins = setOf("chicken","turkey","beef","pork","cod","salmon","tofu")

    fun refine(
        plan: PlanningResult.Success,
        profile: FoodProfile,
        votes: Map<String, TasteRating>,
        offers: List<RetailerOffer>,
        store: String,
        nowEpochMs: Long,
        pantryGrams: Map<String, Int> = emptyMap(),
        /** Explicit meal choices and multi-day batches are immutable user decisions. */
        lockedMeals: Set<Pair<Int, Int>> = emptySet()
    ): BudgetWeekResult {
        val budget=profile.budgetRub.toLongOrNull()
            ?.takeIf { it in WeeklyBudget.MIN_RUB.toLong()..WeeklyBudget.MAX_RUB.toLong() }
            ?: return BudgetWeekResult(plan,unpriced(plan,profile),0,BudgetWeekStatus.MISSING_PRICES)
        fun quote(days:List<PlannedDay>) = BudgetBasketOptimizer.choose(
            ShoppingAggregation.fromDays(days,profile.household.toInt(),pantryGrams),
            offers,profile.city,store,budget,nowEpochMs
        )
        var working=plan
        var quoted=quote(working.days)
        if (!quoted.complete) return BudgetWeekResult(
            working,quoted,0,BudgetWeekStatus.MISSING_PRICES
        )
        var changes=0
        // Attempt a bounded series of money-saving, calorie-comparable meal swaps.
        while (quoted.fitsBudget==false && changes<5) {
            var best: PlanningResult.Success?=null
            var bestQuote: BudgetBasket?=null
            var savings=0L
            for (day in working.days.indices) {
                for (slot in working.days[day].meals.indices) {
                    val meal=working.days[day].meals[slot]
                    if (isUserLocked(day,slot,meal,lockedMeals)) continue
                    val choices=MealReplacement.alternatives(
                        profile,votes,working,day,slot).take(10)
                    for (candidate in choices) {
                        if (!calorieComparable(meal.recipe,candidate)) continue
                        val alternative = MealReplacement.replace(
                            profile,votes,working,day,slot,candidate.id,pantryGrams
                        ) as? PlanningResult.Success ?: continue
                        val price=quote(alternative.days)
                        if (!price.complete) continue
                        val reduction=quoted.estimatedTotalKopeks-price.estimatedTotalKopeks
                        if (reduction>savings) {
                            savings=reduction
                            best=alternative
                            bestQuote=price
                        }
                    }
                }
            }
            if (best==null || bestQuote==null) break
            working=best
            quoted=bestQuote
            changes++
        }
        // Extra budget makes room for more food diversity, not unnecessary
        // spending on identical ingredients. All candidates must still fit.
        val minPrice=quoted.cheapestCompleteBasketKopeks ?: 0L
        if (quoted.fitsBudget==true && minPrice>0L &&
            quoted.budgetKopeks>=minPrice*2L) {
            repeat(3) {
                val seen=proteinTypes(working)
                var upgrade: PlanningResult.Success?=null
                var upgradeBasket: BudgetBasket?=null
                var newDiversity=seen.size
                for (day in working.days.indices) {
                    for (slot in working.days[day].meals.indices) {
                        val meal=working.days[day].meals[slot]
                        if (meal.recipe.type!=MealType.MAIN ||
                            isUserLocked(day,slot,meal,lockedMeals)) continue
                        // Ranking bonuses can push otherwise viable alternatives
                        // below the old 200-recipe cutoff as the catalogue grows.
                        // Search the full eligible pool, then limit only candidates
                        // that meet BOTH protein diversity and energy constraints.
                        val choices=MealReplacement.alternatives(
                            profile,votes,working,day,slot,
                            maxResults=OfflineRecipes.recipes.size)
                            .asSequence()
                            .filter { candidate ->
                                calorieComparable(meal.recipe,candidate) &&
                                candidate.ingredients.any { it.id in proteins && it.id !in seen }
                            }
                            .take(32).toList()
                        for (candidate in choices) {
                            if (!calorieComparable(meal.recipe,candidate)) continue
                            val option=MealReplacement.replace(
                                profile,votes,working,day,slot,candidate.id,pantryGrams
                            ) as? PlanningResult.Success ?: continue
                            val variety=proteinTypes(option).size
                            if (variety<=newDiversity) continue
                            val next=quote(option.days)
                            if (next.fitsBudget != true) continue
                            newDiversity=variety
                            upgrade=option
                            upgradeBasket=next
                        }
                    }
                }
                if (upgrade!=null && upgradeBasket!=null) {
                    working=upgrade
                    quoted=upgradeBasket
                    changes++
                }
            }
        }
        return BudgetWeekResult(working,quoted,changes,
            if(!quoted.complete) BudgetWeekStatus.MISSING_PRICES
            else if(quoted.fitsBudget==true) BudgetWeekStatus.WITHIN_BUDGET
            else BudgetWeekStatus.OVER_BUDGET)
    }

    private fun isUserLocked(day: Int, slot: Int, meal: PlannedMeal,
                             explicit: Set<Pair<Int,Int>>) =
        (day to slot) in explicit || meal.batchStartDay != null ||
            meal.selectedEffort != null

    private fun proteinTypes(plan: PlanningResult.Success): Set<String> =
        plan.days.flatMap { it.meals }.flatMap { it.recipe.ingredients }
            .map { it.id }.filter { it in proteins }.toSet()

    private fun calorieComparable(old: OfflineRecipe,next: OfflineRecipe): Boolean {
        val a=OfflineRecipes.nutrition(old).kcal
        val b=OfflineRecipes.nutrition(next).kcal
        return a>0 && b/a in 0.8..1.25
    }

    private fun unpriced(plan: PlanningResult.Success,profile: FoodProfile): BudgetBasket =
        BudgetBasket(
            emptyList(),plan.shopping.filter { it.grams>0 }.map { it.ingredient.id },
            (profile.budgetRub.toLongOrNull() ?: 0L).coerceAtLeast(0L)*100L,
            0L,null,null,false,0,0
        )
}
