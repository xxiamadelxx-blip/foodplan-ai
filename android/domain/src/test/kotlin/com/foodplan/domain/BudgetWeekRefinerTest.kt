package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class BudgetWeekRefinerTest {
    private fun profile(budget:String) = FoodProfile(
        age="31",sex="Женский",heightCm="170",weightKg="70",
        budgetRub=budget,city="Санкт-Петербург",stores=setOf("Магнит"),
        cookingEffort=CookingEffort.INVOLVED,cookingMinutes="55",
        appliances=setOf("Плита","Кастрюля","Сковорода","Духовка")
    )
    private fun syntheticCatalog(): List<RetailerOffer> =
        OfflineRecipes.ingredients.values.map { ingredient ->
            // Fixtures are synthetic, NOT current prices.
            val packPrice=when(ingredient.id) {
                "salmon" -> 110000L
                "beef" -> 75000L
                "chicken" -> 30000L
                "turkey" -> 40000L
                "cod" -> 60000L
                "pork" -> 46000L
                "tofu" -> 23000L
                else -> 11000L
            }
            RetailerOffer(ingredient.id,"Магнит","Санкт-Петербург",
                ingredient.name.replace(" сухой","").replace(" сырое","")+
                    " ${ingredient.packGrams}г",
                "https://magnit.ru/product/fixture-${ingredient.id}",
                ingredient.packGrams,packPrice,100000L,true,
                ListingAvailability.IN_STOCK,4.5,100)
        }
    @Test fun incompletePricingNeverClaimsBudgetIsMet() {
        val p=profile("25000")
        val initial=SevenDayPlanner.plan(p) as PlanningResult.Success
        val result=BudgetWeekRefiner.refine(initial,p,emptyMap(),emptyList(),
            "Магнит",110000L)
        assertEquals(BudgetWeekStatus.MISSING_PRICES,result.status)
        assertNull(result.basket.fitsBudget)
        assertEquals(0,result.numberOfMealChanges)
    }

    @Test fun budgetNeverExceededWhenDeclaredAffordable() {
        val p=profile("25000")
        val initial=SevenDayPlanner.plan(p) as PlanningResult.Success
        val result=BudgetWeekRefiner.refine(initial,p,emptyMap(),syntheticCatalog(),
            "Магнит",110000L)
        if(result.status==BudgetWeekStatus.WITHIN_BUDGET) {
            assertTrue(result.basket.estimatedTotalKopeks <= 2_500_000L)
            assertTrue(result.plan.days.size==7)
        }
        assertTrue(result.basket.items.all { it.packs>0 })
    }

    @Test fun tooSmallBudgetNeverGetsFakePass() {
        val p=profile("100")
        val initial=SevenDayPlanner.plan(p) as PlanningResult.Success
        val result=BudgetWeekRefiner.refine(initial,p,emptyMap(),syntheticCatalog(),
            "Магнит",110000L)
        assertNotEquals(BudgetWeekStatus.WITHIN_BUDGET,result.status)
    }
    @Test fun budget2500CannotPretendToFitOverpricedFullWeek() {
        val p=profile("2500")
        val initial=SevenDayPlanner.plan(p) as PlanningResult.Success
        val checked=BudgetWeekRefiner.refine(initial,p,emptyMap(),syntheticCatalog(),
            "Магнит",110000L)
        if (checked.status==BudgetWeekStatus.WITHIN_BUDGET) {
            assertTrue(checked.basket.estimatedTotalKopeks<=250000L)
        } else {
            assertTrue(checked.status in setOf(
                BudgetWeekStatus.OVER_BUDGET,BudgetWeekStatus.MISSING_PRICES))
        }
    }

    @Test fun generousBudgetSearchesBeyondFirstTwelveSimilarRecipeCandidates() {
        val p=profile("25000")
        val breakfast=OfflineRecipes.recipes.first { it.id=="breakfast_oats_apple_water" }
        val chicken=OfflineRecipes.recipes.first { it.id=="main_chicken_rice_broccoli_pan" }
        val days=(0..6).map { day ->
            PlannedDay(day,listOf(
                PlannedMeal(breakfast,1.0,OfflineRecipes.nutrition(breakfast)),
                PlannedMeal(chicken,1.0,OfflineRecipes.nutrition(chicken))
            ))
        }
        val initial=PlanningResult.Success(days,
            ShoppingAggregation.fromDays(days,p.household.toInt()),null,emptyList())
        val initialKinds=initial.days.flatMap { it.meals }
            .flatMap { it.recipe.ingredients }.map { it.id }
            .filter { it in setOf("chicken","turkey","beef","pork","cod","salmon","tofu") }.toSet()
        assertEquals(setOf("chicken"),initialKinds)
        val optimized=BudgetWeekRefiner.refine(initial,p,emptyMap(),syntheticCatalog(),
            "Магнит",110000L)
        assertEquals(BudgetWeekStatus.WITHIN_BUDGET,optimized.status)
        val optimizedKinds=optimized.plan.days.flatMap { it.meals }
            .flatMap { it.recipe.ingredients }.map { it.id }
            .filter { it in setOf("chicken","turkey","beef","pork","cod","salmon","tofu") }.toSet()
        assertTrue("With a generous budget, protein variety should grow",optimizedKinds.size>initialKinds.size)
        assertTrue(optimized.basket.estimatedTotalKopeks<=optimized.basket.budgetKopeks)
    }

    @Test fun automaticallyOptimizesOnlyWhenFullRealOfferSetIsAvailable() {
        val p=profile("2500")
        val initial=SevenDayPlanner.plan(p) as PlanningResult.Success
        val incomplete=BudgetWeekRefiner.refine(initial,p,emptyMap(),
            syntheticCatalog().filter { it.ingredientId == "rice" },
            "Магнит",110000L)
        assertEquals(BudgetWeekStatus.MISSING_PRICES,incomplete.status)
        assertEquals(0,incomplete.numberOfMealChanges)
        assertEquals(initial.days,incomplete.plan.days)
        assertNull(incomplete.basket.fitsBudget)
    }

    @Test fun automaticOptimizerNeverRewritesExplicitMealChoicesEvenIfAboveBudget() {
        val p=profile("100")
        val original=SevenDayPlanner.plan(p) as PlanningResult.Success
        val lockedDays=original.days.map { day ->
            day.copy(meals=day.meals.map { meal ->
                meal.copy(selectedEffort=CookingEffort.INVOLVED)
            })
        }
        val locked=original.copy(days=lockedDays,
            shopping=ShoppingAggregation.fromDays(lockedDays,p.household.toInt()))
        val result=BudgetWeekRefiner.refine(locked,p,emptyMap(),
            syntheticCatalog(),"Магнит",110000L)
        assertEquals(0,result.numberOfMealChanges)
        assertEquals(locked.days,result.plan.days)
        assertNotEquals(BudgetWeekStatus.WITHIN_BUDGET,result.status)
    }

    @Test fun explicitBatchIsNotSilentlyBrokenByGenerousBudgetDiversitySearch() {
        val p=profile("25000")
        val breakfast=OfflineRecipes.recipes.first { it.id=="breakfast_oats_apple_water" }
        val chicken=OfflineRecipes.recipes.first { it.id=="main_chicken_rice_broccoli_pan" }
        val days=(0..6).map { day ->
            PlannedDay(day,listOf(
                PlannedMeal(breakfast,1.0,OfflineRecipes.nutrition(breakfast)),
                PlannedMeal(chicken,1.0,OfflineRecipes.nutrition(chicken))
            ))
        }
        val original=PlanningResult.Success(days,
            ShoppingAggregation.fromDays(days,p.household.toInt()),null,emptyList())
        val batched=RecipeBatchPlanner.apply(p,emptyMap(),original,0,1,
            chicken.id,3,CookingEffort.INVOLVED) as PlanningResult.Success
        val refined=BudgetWeekRefiner.refine(batched,p,emptyMap(),
            syntheticCatalog(),"Магнит",110000L)
        (0..2).forEach { day ->
            assertEquals(chicken.id,refined.plan.days[day].meals[1].recipe.id)
            assertEquals(3,refined.plan.days[day].meals[1].batchDays)
            assertEquals(0,refined.plan.days[day].meals[1].batchStartDay)
            assertEquals(CookingEffort.INVOLVED,
                refined.plan.days[day].meals[1].selectedEffort)
        }
        assertTrue(refined.basket.estimatedTotalKopeks <= refined.basket.budgetKopeks ||
            refined.status != BudgetWeekStatus.WITHIN_BUDGET)
    }

}
