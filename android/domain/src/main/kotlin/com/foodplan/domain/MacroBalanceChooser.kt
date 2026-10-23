package com.foodplan.domain

import kotlin.math.max

/**
 * A SOFT adult-reference macronutrient preference between already-eligible
 * meals. Never used to clear allergies, diet, preparation or physical SKU
 * safety, and never certifies nutrient adequacy or cooked yields.
 */
data class MacroMealOption(
    val recipe: OfflineRecipe,
    val rankScore: Int,
    val estimatedRawNutrition: Nutrition
)

object MacroBalanceChooser {
    /** 4/9/4 percentage points outside adult AMDR 10-35 P, 20-35 F, 45-65 C. */
    fun outsideReferencePoints(nutrition: Nutrition): Double? {
        val p = nutrition.protein * 4.0
        val f = nutrition.fat * 9.0
        val c = nutrition.carbs * 4.0
        val denominator = p + f + c
        if (!denominator.isFinite() || denominator <= 0.0 ||
            minOf(p,f,c) < 0.0) return null
        fun deviation(value: Double, lower: Double, upper: Double): Double {
            val portion = 100.0 * value / denominator
            return max(lower - portion, 0.0) + max(portion - upper, 0.0)
        }
        return deviation(p, 10.0, 35.0) +
            deviation(f, 20.0, 35.0) +
            deviation(c, 45.0, 65.0)
    }

    /**
     * Bounded deterministic local search. Starts from the best eligible
     * user-preferred recipes and considers at most 24 alternatives per meal.
     * Up to 3 passes x 6 meals, so even a large catalog stays offline-fast.
     * Only callers that already ran RecipeRanker eligibility may pass options.
     */
    fun choose(
        eligibleSlots: List<List<MacroMealOption>>,
        calorieTargetEstimate: Int? = null
    ): List<MacroMealOption>? {
        if (eligibleSlots.isEmpty() || eligibleSlots.any { it.isEmpty() }) return null
        val shortlists = eligibleSlots.map { list ->
            val sorted = list.sortedWith(compareByDescending<MacroMealOption> { it.rankScore }
                .thenBy { it.recipe.id })
            val best = sorted.first().rankScore
            sorted.filter { it.rankScore >= best - 32 }.take(24)
        }
        val chosen = shortlists.map { it.first() }.toMutableList()
        val applyMacro = chosen.size >= 3

        // Portion normalization in SevenDayPlanner is capped at 0.75..2.5.
        // A macro-perfect menu whose raw calories cannot reach the target
        // within that range must not be preferred just for its 4/9/4 ratios.
        // This is a SOFT planning score, not a minimum-calorie safety gate.
        fun unreachableCaloriePenalty(nutrition: Nutrition): Double {
            val target = calorieTargetEstimate?.toDouble()
                ?.takeIf { it.isFinite() && it > 0.0 } ?: return 0.0
            val rawKcal = nutrition.kcal
            if (!rawKcal.isFinite() || rawKcal <= 0.0) return 0.0
            val nearestAchievable = target.coerceIn(rawKcal * 0.75, rawKcal * 2.5)
            // Two ranking points per percentage point of unavoidable
            // calorie gap. Never change hard eligibility or infer adequacy.
            return kotlin.math.abs(target - nearestAchievable) / target * 200.0
        }

        fun totalScore(items: List<MacroMealOption>): Double {
            val totalNutrition = items.fold(Nutrition.ZERO) { current, item ->
                current + item.estimatedRawNutrition
            }
            val macroPenalty = if (applyMacro)
                (outsideReferencePoints(totalNutrition) ?: 0.0) * 3.0
                else 0.0
            val duplicates = items.groupingBy { it.recipe.id }.eachCount().values
                .sumOf { (it - 1).coerceAtLeast(0) }
            val adjacent = items.zipWithNext().count { (a,b) ->
                a.recipe.id.substringBeforeLast('_') ==
                    b.recipe.id.substringBeforeLast('_')
            }
            return items.sumOf { it.rankScore }.toDouble() -
                macroPenalty - unreachableCaloriePenalty(totalNutrition) -
                duplicates * 8.0 - adjacent * 6.0
        }

        // A change is accepted only when it strictly improves the objective.
        // Equal scores retain the user's previous best-ranked choice.
        repeat(3) {
            for (slot in shortlists.indices) {
                var best = chosen[slot]
                var bestScore = totalScore(chosen)
                for (candidate in shortlists[slot]) {
                    chosen[slot] = candidate
                    val score = totalScore(chosen)
                    if (score > bestScore + 0.000001) {
                        bestScore = score
                        best = candidate
                    }
                }
                chosen[slot] = best
            }
        }
        return chosen
    }
}
