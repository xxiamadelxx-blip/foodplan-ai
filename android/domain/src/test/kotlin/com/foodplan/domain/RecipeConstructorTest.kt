package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

/** Test the public production seam; all prices in these tests are synthetic fixtures. */
class RecipeConstructorTest {
    private fun profile(diet: String = "Всеядный", budget: String = "7000") = FoodProfile(
        sex = "Мужской", age = "31", heightCm = "175", weightKg = "75",
        city = "Санкт-Петербург", stores = setOf("Магнит"),
        diet = diet, budgetRub = budget, mealsPerDay = "3", household = "2",
        cookingMinutes = "90", cookingEffort = CookingEffort.ALL_OUT,
        appliances = setOf("Плита", "Кастрюля", "Сковорода", "Духовка")
    )

    @Test fun weekConstructorRespectsDietAndBuildsSingleShoppingList() {
        val p = profile(diet = "Веганский", budget = "356")
        val week = RecipeConstructor.constructWeek(p, votes = mapOf(
            "buckwheat_chicken" to TasteRating.LIKE))
        val success = week as PlanningResult.Success
        assertEquals(7, success.days.size)
        assertEquals(21, success.days.sumOf { it.meals.size })
        assertTrue(success.days.flatMap { it.meals }.all {
            "Веганский" in OfflineRecipes.diets(it.recipe)
        })
        assertEquals(ShoppingAggregation.fromDays(success.days, 2), success.shopping)
        assertTrue(success.warnings.any { it.contains("Стоимость не подтверждена") })
    }

    @Test fun productFirstReplacementUsesExistingShoppingAndPreviewIsExact() {
        val p = profile()
        val base = RecipeConstructor.constructWeek(p) as PlanningResult.Success
        val saved = base.days.map { it.meals.map { meal -> meal.recipe.id } }
        val candidates = RecipeConstructor.suggestReplacements(
            p, emptyMap(), base, 0, 1, CookingEffort.ALL_OUT,
            cookingDays = 1, maxResults = 24)
        assertTrue(candidates.isNotEmpty())
        val originalProducts = base.shopping.filter { it.requiredGrams > 0 }
            .map { it.ingredient.id }.toSet()
        assertTrue(candidates.all { option ->
            option.reusedIngredientIds.all { it in originalProducts } &&
                option.newIngredientIds.none { it in originalProducts }
        })
        assertTrue(candidates.zipWithNext().all { (first, second) ->
            first.newIngredientIds.size <= second.newIngredientIds.size
        })
        for (option in candidates.take(5)) {
            val applied = RecipeBatchPlanner.apply(p, emptyMap(), base, 0, 1,
                option.recipe.id, 1, CookingEffort.ALL_OUT) as PlanningResult.Success
            assertEquals(applied.shopping, option.previewShopping)
            assertEquals(applied.shopping.filter { item ->
                item.requiredGrams > 0 && item.ingredient.id !in originalProducts
            }.map { it.ingredient.id }.toSet(), option.newIngredientIds)
            assertEquals(null, option.fitsBudget) // no retailer evidence
        }
        assertEquals(saved, base.days.map { it.meals.map { meal -> meal.recipe.id } })
    }

    @Test fun candidateQuantityIncreaseIsNotMistakenForExistingFreeFood() {
        val p = profile()
        val base = RecipeConstructor.constructWeek(p) as PlanningResult.Success
        val candidates = RecipeConstructor.suggestReplacements(
            p, emptyMap(), base, 0, 1, CookingEffort.ALL_OUT,
            cookingDays = 1, maxResults = 40)
        val before = base.shopping.associateBy { it.ingredient.id }
        for (option in candidates) {
            val after = option.previewShopping.associateBy { it.ingredient.id }
            val expected = after.mapNotNull { (id, need) ->
                val old = before[id]?.grams ?: 0
                val growth = need.grams - old
                if (id in before && growth > 0) id to growth else null
            }.toMap()
            assertEquals(expected, option.increasedPurchaseGrams)
        }
    }

    @Test fun replacementRespectsVeganDislikesAndUnknownAllergenClearance() {
        val base = RecipeConstructor.constructWeek(profile()) as PlanningResult.Success
        val vegan = profile(diet = "Веганский").copy(disliked = "рис, курица")
        val alternatives = RecipeConstructor.suggestReplacements(
            vegan, emptyMap(), base, 0, 1, CookingEffort.ALL_OUT)
        assertTrue(alternatives.all { option ->
            "Веганский" in OfflineRecipes.diets(option.recipe) &&
                option.recipe.ingredients.none { it.id in setOf("rice", "chicken") }
        })
        assertTrue(RecipeConstructor.suggestReplacements(
            vegan.copy(allergens = "рыба"), emptyMap(), base, 0, 1,
            CookingEffort.ALL_OUT).isEmpty())
        assertTrue(RecipeConstructor.suggestReplacements(
            vegan.copy(allergens = "неизвестная смесь"), emptyMap(), base, 0, 1,
            CookingEffort.ALL_OUT).isEmpty())
    }

    @Test fun replacementSimulatesCookingDaysAndHouseholdWithoutMutatingCurrentWeek() {
        val p = profile()
        val base = RecipeConstructor.constructWeek(p) as PlanningResult.Success
        val before = base.days.map { it.meals.map { meal -> meal.recipe.id } }
        val options = RecipeConstructor.suggestReplacements(
            p, emptyMap(), base, 0, 1, CookingEffort.ALL_OUT,
            cookingDays = 3, maxResults = 8)
        assertTrue(options.isNotEmpty())
        val replacement = options.first()
        val applied = RecipeBatchPlanner.apply(p, emptyMap(), base, 0, 1,
            replacement.recipe.id, 3, CookingEffort.ALL_OUT) as PlanningResult.Success
        assertEquals(applied.shopping, replacement.previewShopping)
        assertEquals(applied.shopping, ShoppingAggregation.fromDays(applied.days, 2))
        assertEquals(replacement.recipe.id, applied.days[2].meals[1].recipe.id)
        assertEquals(before, base.days.map { it.meals.map { meal -> meal.recipe.id } })
    }

    @Test fun syntheticAndPartialPricesCannotBePresentedAsProvenAffordable() {
        val p = profile(budget = "100")
        val base = RecipeConstructor.constructWeek(p) as PlanningResult.Success
        val one = OfflineRecipes.ingredients.getValue("rice")
        val fakeOffer = RetailerOffer(one.id, "Магнит", p.city, "Рис длиннозёрный",
            "https://magnit.ru/product/fixture-rice", one.packGrams,
            20000L, 100_000L, true, ListingAvailability.IN_STOCK, null, null)
        val options = RecipeConstructor.suggestReplacements(
            p, emptyMap(), base, 0, 1, CookingEffort.ALL_OUT,
            offers = listOf(fakeOffer), nowEpochMs = 110_000L)
        assertTrue(options.isNotEmpty())
        assertTrue(options.all { it.fitsBudget == null || it.fitsBudget == false })
        assertTrue(options.any { it.fitsBudget == null })
    }

    @Test fun deterministicRankingForSameInputsAndUnsupportedSlotStaysEmpty() {
        val p = profile()
        val base = RecipeConstructor.constructWeek(p) as PlanningResult.Success
        val votes = mapOf("buckwheat_chicken" to TasteRating.LIKE)
        val a = RecipeConstructor.suggestReplacements(p, votes, base, 0, 0,
            CookingEffort.ALL_OUT, maxResults = 12)
        val b = RecipeConstructor.suggestReplacements(p, votes, base, 0, 0,
            CookingEffort.ALL_OUT, maxResults = 12)
        assertEquals(a, b)
        assertTrue(RecipeConstructor.suggestReplacements(p, votes, base, 9, 1,
            CookingEffort.ALL_OUT).isEmpty())
    }
    @Test fun unverifiedRetailerFixturesNeverProduceFalseBudgetPass() {
        val p = profile(budget = "100000")
        val base = RecipeConstructor.constructWeek(p) as PlanningResult.Success
        // Synthetic prices are declared test fixtures, not live Russian prices.
        val offers = OfflineRecipes.ingredients.values.map { food ->
            RetailerOffer(food.id, "Магнит", p.city, food.name,
                "https://magnit.ru/product/fixture-${food.id}",
                food.packGrams, 100L, 100_000L, true,
                ListingAvailability.IN_STOCK, null, null)
        }
        val options = RecipeConstructor.suggestReplacements(
            p, emptyMap(), base, 0, 1, CookingEffort.ALL_OUT,
            offers = offers, nowEpochMs = 110_000L, maxResults = 10)
        assertTrue(options.isNotEmpty())
        // A list of offers does not equal a complete, validated retailer
        // basket: product title, declared package mass and producer evidence
        // must pass the existing fail-closed RetailerOfferChecks gate.
        assertTrue(options.all { it.fitsBudget == null })
        assertTrue(options.none { it.fitsBudget == true })
        val withOnlyOnePrice = RecipeConstructor.suggestReplacements(
            p, emptyMap(), base, 0, 1, CookingEffort.ALL_OUT,
            offers = offers.take(1), nowEpochMs = 110_000L, maxResults = 10)
        assertTrue(withOnlyOnePrice.all { it.fitsBudget == null })
    }

}
