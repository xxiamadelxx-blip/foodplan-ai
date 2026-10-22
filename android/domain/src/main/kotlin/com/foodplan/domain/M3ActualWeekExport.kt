package com.foodplan.domain

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/**
 * Independent M3/M4 acceptance input from the actual deterministic planner.
 * Never hand-select rice/buckwheat or invent missing retailer prices.
 * The CSV is evidence of a planned, UNVERIFIED draft shopping list, not a
 * diet recommendation or proof of nutrition, allergy or checkout prices.
 */
object M3ActualWeekExport {
    private fun csv(v: Any?): String = "\"" +
        (v?.toString() ?: "").replace("\"", "\"\"").replace("\n", " ").replace("\r", " ") + "\""

    data class Export(
        val mealRows: List<String>,
        val ingredientRows: List<String>,
        val uniqueIngredients: Int,
        val mealCount: Int,
        val days: Int
    )

    fun extract(profile: FoodProfile = fixtureProfile()): Export {
        val result = SevenDayPlanner.plan(profile) as? PlanningResult.Success
            ?: error("Deterministic week must be generated from a valid fixture profile")
        require(result.days.size == 7 && result.days.map { it.index }.toSet().size == 7)
        val mealRows = result.days.flatMap { day ->
            day.meals.mapIndexed { index, meal ->
                listOf(day.index, index, meal.recipe.id, meal.recipe.name,
                    meal.servings, meal.nutrition.kcal,
                    meal.recipe.ingredients.joinToString("|") {
                        "${it.id}:${it.grams}"
                    }).joinToString(",") { csv(it) }
            }
        }
        val aggregation = ShoppingAggregation.fromDays(
            result.days, profile.household.toInt())
        require(result.shopping == aggregation) {
            "Planner shopping list differs from canonical weekly aggregation"
        }
        val rows = aggregation.filter { it.grams > 0 }.map { need ->
            listOf(need.ingredient.id, need.ingredient.name, need.requiredGrams,
                need.pantryUsed, need.grams, need.ingredient.packGrams,
                need.packsToBuy, "UNKNOWN", "NOT_INGREDIENT_OR_ALLERGEN_CERTIFIED")
                .joinToString(",") { csv(it) }
        }
        require(rows.isNotEmpty())
        return Export(mealRows, rows, aggregation.count { it.grams > 0 },
            mealRows.size, result.days.size)
    }

    fun fixtureProfile() = FoodProfile(
        sex = "Женский", age = "30", heightCm = "165", weightKg = "65",
        activity = "Умеренная", goal = "Поддержание", budgetRub = "356",
        city = "Санкт-Петербург", stores = setOf("Магнит"),
        diet = "Всеядный", mealsPerDay = "3", cookingMinutes = "55",
        cookingEffort = CookingEffort.INVOLVED,
        appliances = setOf("Плита", "Духовка", "Сковорода", "Кастрюля")
    )

    fun csvFiles(profile: FoodProfile = fixtureProfile()): Pair<String, String> {
        val data = extract(profile)
        val meals = listOf(
            "day", "slot", "recipe_id", "recipe_name", "servings",
            "estimated_raw_ingredient_kcal", "ingredient_id_and_raw_grams"
        ).joinToString(",") { csv(it) } + "\n" +
            data.mealRows.joinToString("\n", postfix = "\n")
        val ingredients = listOf(
            "ingredient_id", "name", "required_grams", "pantry_used_grams",
            "to_buy_grams", "pack_grams", "reference_pack_count",
            "indicative_chain_price_status", "nutrition_and_sku_certification"
        ).joinToString(",") { csv(it) } + "\n" +
            data.ingredientRows.joinToString("\n", postfix = "\n")
        return meals to ingredients
    }

    @JvmStatic fun main(args: Array<String>) {
        require(args.size == 2) {
            "Provide output meal CSV and shopping CSV paths"
        }
        val (meals, ingredients) = csvFiles()
        listOf(args[0] to meals, args[1] to ingredients).forEach { (file, content) ->
            val dest = Path.of(file)
            require(dest.toString().endsWith(".csv"))
            dest.parent?.let(Files::createDirectories)
            Files.writeString(dest, content, StandardCharsets.UTF_8)
        }
        val data = extract()
        println("M3 actual deterministic week: ${data.days} days, " +
            "${data.mealCount} meals, ${data.uniqueIngredients} shopping ingredients. " +
            "All prices and cooked-yield safety UNKNOWN (M2 blocked).")
    }
}
