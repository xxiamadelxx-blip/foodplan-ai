package com.foodplan.domain

import kotlin.math.ceil
import kotlin.math.roundToInt

data class PlannedMeal(val recipe: OfflineRecipe, val servings: Double, val nutrition: Nutrition)
data class PlannedDay(val index: Int, val meals: List<PlannedMeal>) {
    val nutrition: Nutrition get() = meals.fold(Nutrition.ZERO) { n, meal -> n + meal.nutrition }
}
data class ShoppingNeed(val ingredient: FoodIngredient, val grams: Int, val packsToBuy: Int) {
    val purchaseGrams get() = packsToBuy * ingredient.packGrams
}
sealed class PlanningResult {
    data class Success(
        val days: List<PlannedDay>, val shopping: List<ShoppingNeed>,
        val calorieTargetEstimate: Int?, val warnings: List<String>
    ) : PlanningResult()
    data class Blocked(val reasons: List<String>) : PlanningResult()
}

/** Normalize free-text allergens to canonical categories; unrecognized terms BLOCK planning. */
object AllergenTerms {
    private val aliases = mapOf(
        "арахис" to setOf("peanut"), "peanut" to setOf("peanut"),
        "орехи" to setOf("peanut","tree_nuts"), "грецкий орех" to setOf("tree_nuts"),
        "молоко" to setOf("milk"), "молочные продукты" to setOf("milk"),
        "лактоза" to setOf("milk"), "milk" to setOf("milk"),
        "рыба" to setOf("fish"), "fish" to setOf("fish"),
        "креветки" to setOf("shellfish"), "морепродукты" to setOf("fish","shellfish"),
        "соя" to setOf("soy"), "soy" to setOf("soy"), "тофу" to setOf("soy"),
        "глютен" to setOf("wheat"), "пшеница" to setOf("wheat"),
        "овсянка" to setOf("wheat"), "яйцо" to setOf("egg"),
        "яйца" to setOf("egg"), "кунжут" to setOf("sesame"),
        "курица" to setOf("chicken"), "говядина" to setOf("beef"),
        "свинина" to setOf("pork"), "индейка" to setOf("turkey"),
        "гречка" to setOf("buckwheat"), "рис" to setOf("rice"),
        "помидоры" to setOf("tomato"), "томат" to setOf("tomato"),
        "морковь" to setOf("carrot"), "грибы" to setOf("mushroom")
    )
    data class Resolution(val terms: Set<String>, val unknown: List<String>)
    fun parse(value: String): Resolution {
        val raw = value.split(',', ';', '\n').map { it.trim().lowercase() }.filter { it.isNotBlank() }
        val unknown = raw.filterNot { it in aliases }
        return Resolution(raw.flatMap { aliases[it] ?: emptySet() }.toSet(), unknown)
    }
}

object SevenDayPlanner {
    private const val estimatedWarning = "КБЖУ предварительные: ингредиентная база пока не проверена по российской маркировке."
    private const val priceWarning = "Стоимость не подтверждена: у каталога пока нет актуальных цен выбранных магазинов."
    /**
     * Intermediate domain planning, not an individualized treatment or verified dietary plan.
     * Meals/ingredient quantities are selected without external AI.
     */
    fun plan(
        profile: FoodProfile,
        votes: Map<String, TasteRating>,
        pantryGrams: Map<String, Int> = emptyMap()
    ): PlanningResult {
        val invalid = ProfileValidator.errors(profile)
        if (invalid.isNotEmpty()) return PlanningResult.Blocked(invalid)
        val allergies = AllergenTerms.parse(profile.allergens)
        if (allergies.unknown.isNotEmpty())
            return PlanningResult.Blocked(listOf(
                "Нераспознанные аллергены: ${allergies.unknown.joinToString()}. Уточните наименование, чтобы не получить опасный рецепт."
            ))
        // No automated weight-loss meal planning for minors.
        if (profile.age.toInt() < 18)
            return PlanningResult.Blocked(listOf("Индивидуальные планы питания для несовершеннолетних пока недоступны."))

        val sanitized = profile.copy(allergens = allergies.terms.joinToString(","))
        val repeats = mutableMapOf<String, Int>()
        val dailyRecipes = mutableListOf<List<OfflineRecipe>>()
        for (day in 0..6) {
            val dayRecipes = mutableListOf<OfflineRecipe>()
            for (slot in 0 until profile.mealsPerDay.toInt()) {
                val kind = if (slot == 0) MealType.BREAKFAST else MealType.MAIN
                val candidateRecipes = OfflineRecipes.recipes.filter { it.type == kind }
                val result = RecipeRanker.rank(sanitized,
                    candidateRecipes.map(OfflineRecipes::toCandidate), votes, repeats)
                // Avoid same protein or grain in neighboring main meals where possible.
                val previous = dayRecipes.lastOrNull()
                val chosen = result.ranked
                    .sortedWith(compareByDescending<RankedCandidate> { candidate ->
                        candidate.score - if (previous != null &&
                            candidate.recipe.id.substringBeforeLast('_') ==
                            previous.id.substringBeforeLast('_')) 6 else 0
                    }.thenBy { it.recipe.id })
                    .firstOrNull()?.let { item -> candidateRecipes.first { it.id == item.recipe.id } }
                    ?: return PlanningResult.Blocked(listOf(
                        "Не нашлось совместимых блюд для ${if (kind == MealType.BREAKFAST) "завтрака" else "основного приёма пищи"}: проверьте выбранную технику, время, диету и ограничения."
                    ))
                dayRecipes.add(chosen)
                repeats[chosen.id] = (repeats[chosen.id] ?: 0) + 1
            }
            dailyRecipes.add(dayRecipes)
        }
        val kcalTarget = targetCalories(profile)
        val days = dailyRecipes.mapIndexed { day, recipes ->
            val baseKcal = recipes.sumOf { OfflineRecipes.nutrition(it).kcal }
            val scale = if (kcalTarget != null && baseKcal > 0) (kcalTarget / baseKcal).coerceIn(0.75,2.5) else 1.0
            PlannedDay(day, recipes.map { recipe ->
                PlannedMeal(recipe, scale, OfflineRecipes.nutrition(recipe) * scale)
            })
        }
        val shopping = mutableMapOf<String, Double>()
        days.flatMap { it.meals }.forEach { meal ->
            meal.recipe.ingredients.forEach { part ->
                shopping[part.id] = (shopping[part.id] ?: 0.0) + part.grams * meal.servings
            }
        }
        val lines = shopping.mapNotNull { (id, grams) ->
            val requested = ceil(grams).toInt()
            val bought = (requested - (pantryGrams[id] ?: 0).coerceAtLeast(0)).coerceAtLeast(0)
            if (bought == 0) null else {
                val ingredient = OfflineRecipes.ingredients.getValue(id)
                ShoppingNeed(ingredient, bought,
                    ((bought.toLong() + ingredient.packGrams - 1) / ingredient.packGrams).toInt())
            }
        }.sortedBy { it.ingredient.name }
        return PlanningResult.Success(days,lines,kcalTarget,listOf(estimatedWarning,priceWarning,
            "Прототип меню не является медицинским назначением. Для аллергий обязательно проверяйте состав упаковки и следы аллергенов."))
    }

    private fun targetCalories(profile: FoodProfile): Int? {
        val age = profile.age.toIntOrNull() ?: return null
        val height = profile.heightCm.toIntOrNull() ?: return null
        val weight = profile.weightKg.replace(',','.').toDoubleOrNull() ?: return null
        val sexDelta = when (profile.sex) {
            "Мужской" -> 5.0
            "Женский" -> -161.0
            else -> return null
        }
        val activity = when (profile.activity) {
            "Низкая" -> 1.2
            "Умеренная" -> 1.45
            "Высокая" -> 1.7
            "Очень высокая" -> 1.9
            else -> return null
        }
        val goalFactor = when (profile.goal) {
            "Похудение" -> 0.9
            "Суперсушка" -> 0.85
            "Набор массы" -> 1.1
            else -> 1.0
        }
        // Lower bounds are guardrails only, NOT claims of safety or sufficiency.
        return (((10*weight + 6.25*height - 5*age + sexDelta) * activity * goalFactor)
            .coerceAtLeast(if (profile.sex=="Мужской") 1500.0 else 1200.0)).roundToInt()
    }
}
