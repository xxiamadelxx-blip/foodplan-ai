package com.foodplan.ai

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.foodplan.domain.SavedWeekSelection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.foodplan.domain.CookingEffort
import com.foodplan.domain.FoodProfile
import org.junit.Rule
import org.junit.Test

class FoodPlanHomeScreenTest {
    @get:Rule val rule = createComposeRule()
    private val profile = FoodProfile(
        sex="Мужской", age="30",heightCm="175",weightKg="75",
        budgetRub="5000", city="Санкт-Петербург", stores=setOf("Пятёрочка"),
        household="2",mealsPerDay="3",cookingMinutes="90",
        cookingEffort=CookingEffort.ALL_OUT,
        appliances=setOf("Плита","Кастрюля","Сковорода","Духовка")
    )

    @Test fun onlyThreeUserSectionsNoExposedSkuOrRecipeGraphNavigation() {
        rule.setContent { MaterialTheme { FoodPlanHomeScreen(profile,emptyMap()) } }
        rule.onNodeWithText("Продукты").assertExists()
        rule.onNodeWithText("Рецепты").assertExists()
        rule.onNodeWithText("Настройки").assertExists()
        rule.onNodeWithText("Из продуктов").assertDoesNotExist()
        rule.onNodeWithText("Ниточки от продуктов к рецептам").assertDoesNotExist()
        rule.onNodeWithText("Рецепты").performClick()
        rule.onNodeWithText("День 1").assertExists()
        rule.onNodeWithText("Завтрак").assertExists()
        rule.onNodeWithText("Обед").assertExists()
        rule.onNodeWithText("Ужин").assertExists()
        rule.onNodeWithText("Настройки").performClick()
        rule.onNodeWithText("Изменить настройки").assertExists()
        rule.onNodeWithText("Пересмотреть пищевые предпочтения").assertExists()
    }

    @Test fun cancelNewWeekKeepsExistingMenuAndConfirmClearsOnlyWeek() {
        var clears = 0
        var savedProfile = 0
        rule.setContent { MaterialTheme {
            FoodPlanHomeScreen(
                profile,emptyMap(),
                onSaveProfile = { savedProfile++ },
                onStartNewWeek = { done -> clears++; done(true) }
            )
        } }
        rule.onNodeWithText("Рецепты").performClick()
        rule.onNodeWithTag("home-meal-0").performClick()
        rule.onNodeWithText("3 дн.").performClick()
        rule.onNodeWithText("Сохранить").performClick()
        rule.onNodeWithText("Приготовить сразу на 3 дня").assertExists()

        rule.onNodeWithText("Настройки").performClick()
        rule.onNodeWithTag("home-start-new-week").performScrollTo().performClick()
        rule.onNodeWithText("Начать новую неделю?").assertExists()
        rule.onNodeWithText("Отмена").performClick()
        rule.onNodeWithText("Рецепты").performClick()
        rule.onNodeWithText("Приготовить сразу на 3 дня").assertExists()

        rule.onNodeWithText("Настройки").performClick()
        rule.onNodeWithTag("home-start-new-week").performScrollTo().performClick()
        rule.onNodeWithText("Начать заново").performClick()
        rule.onNodeWithText("День 1").assertExists()
        rule.onNodeWithText("Приготовить сразу на 3 дня").assertDoesNotExist()
        rule.runOnIdle {
            org.junit.Assert.assertEquals(1,clears)
            org.junit.Assert.assertEquals(0,savedProfile)
        }
    }

    @Test fun failedNewWeekStorageResetDoesNotDeleteCurrentMealChoices() {
        rule.setContent { MaterialTheme {
            FoodPlanHomeScreen(profile,emptyMap(),onStartNewWeek = { done ->
                done(false)
            })
        } }
        rule.onNodeWithText("Рецепты").performClick()
        rule.onNodeWithTag("home-meal-0").performClick()
        rule.onNodeWithText("3 дн.").performClick()
        rule.onNodeWithText("Сохранить").performClick()
        rule.onNodeWithText("Настройки").performClick()
        rule.onNodeWithTag("home-start-new-week").performScrollTo().performClick()
        rule.onNodeWithText("Начать заново").performClick()
        rule.onNodeWithText(
            "Не получилось очистить предыдущую неделю. Данные сохранены; попробуйте ещё раз."
        ).assertExists()
        rule.onNodeWithText("Рецепты").performClick()
        rule.onNodeWithText("Приготовить сразу на 3 дня").assertExists()
    }


    @Test fun compatibleFreshStorePricesTriggerAutomaticMealOptimizationWithoutAi() {
        val options=profile.copy(
            budgetRub="25000",stores=setOf("Магнит"),mealsPerDay="2",
            cookingMinutes="240",cookingEffort=CookingEffort.ALL_OUT,
            appliances=com.foodplan.domain.KitchenEquipment.choices.toSet()
        )
        val breakfast=com.foodplan.domain.OfflineRecipes.recipes.first {
            it.id=="breakfast_oats_apple_water"
        }
        val chicken=com.foodplan.domain.OfflineRecipes.recipes.first {
            it.id=="main_chicken_rice_broccoli_pan"
        }
        val days=(0..6).map { index ->
            com.foodplan.domain.PlannedDay(index,listOf(
                com.foodplan.domain.PlannedMeal(breakfast,1.0,
                    com.foodplan.domain.OfflineRecipes.nutrition(breakfast)),
                com.foodplan.domain.PlannedMeal(chicken,1.0,
                    com.foodplan.domain.OfflineRecipes.nutrition(chicken))
            ))
        }
        val plan=com.foodplan.domain.PlanningResult.Success(
            days,com.foodplan.domain.ShoppingAggregation.fromDays(days,2),
            null, emptyList()
        )
        val saved=com.foodplan.domain.SavedWeekPlanner.capture(
            plan,options,emptyMap(),emptyMap())
        val now=System.currentTimeMillis()
        val offers=com.foodplan.domain.OfflineRecipes.ingredients.values.map { ingredient ->
            val cost=when(ingredient.id) {
                "salmon" -> 110000L
                "beef" -> 75000L
                "chicken" -> 30000L
                "turkey" -> 40000L
                "cod" -> 60000L
                "pork" -> 46000L
                "tofu" -> 23000L
                else -> 11000L
            }
            com.foodplan.domain.RetailerOffer(
                ingredient.id,"Магнит","Санкт-Петербург",
                ingredient.name.replace(" сухой","").replace(" сырое","")+
                    " ${ingredient.packGrams}г",
                "https://magnit.ru/product/test-${ingredient.id}",
                ingredient.packGrams,cost,now,true,
                com.foodplan.domain.ListingAvailability.IN_STOCK,4.5,100
            )
        }
        var savedAutomatically=false
        rule.setContent { MaterialTheme {
            FoodPlanHomeScreen(options,emptyMap(),retailerOffers=offers,
                savedWeekSelection=saved,
                onSaveWeekSelection={ savedAutomatically=true })
        } }
        rule.waitUntil(timeoutMillis=90_000) {
            rule.onAllNodesWithTag("home-budget-auto-refined")
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("home-budget-auto-refined").assertExists()
        rule.runOnIdle {
            org.junit.Assert.assertTrue("Automatic refinement must persist",savedAutomatically)
        }
    }

    @Test fun weeklyBudgetAcceptsExactUnroundedRubleAmountsDuringUse() {
        rule.setContent { MaterialTheme { FoodPlanHomeScreen(profile,emptyMap()) } }
        val field=rule.onNodeWithTag("home-weekly-budget-exact-input")
        for (value in listOf("100","356","63218","100000")) {
            field.performTextReplacement(value)
            rule.onAllNodesWithText("$value ₽").onFirst().assertExists()
        }
        rule.onNodeWithTag("home-weekly-budget-slider").assertExists()
        rule.onNodeWithText("Магазин").assertExists()
        rule.onNodeWithText("Собрать список на неделю").assertExists()
    }

    @Test fun homeRestoresSelectedMealsAfterComposableRecreation() {
        val generation = mutableIntStateOf(0)
        val snapshot = mutableStateOf<SavedWeekSelection?>(null)
        rule.setContent { MaterialTheme {
            key(generation.intValue) {
                FoodPlanHomeScreen(profile, emptyMap(),
                    savedWeekSelection = snapshot.value,
                    onSaveWeekSelection = {
                        snapshot.value = it
                        generation.intValue++
                    })
            }
        } }
        rule.onNodeWithText("Рецепты").performClick()
        rule.onNodeWithTag("home-meal-0").performClick()
        rule.onNodeWithText("3 дн.").performClick()
        rule.onNodeWithText("Сохранить").performClick()
        // Simulates destroy/recreate: all remember state gone, only a captured
        // profile-scoped saved selection survives.
        rule.onNodeWithText("Рецепты").performClick()
        rule.onNodeWithText("Приготовить сразу на 3 дня").assertExists()
        rule.onNodeWithText("День 2").performClick()
        rule.onNodeWithText("Блюдо готовится заранее на 3 дня").assertExists()
        rule.runOnIdle { org.junit.Assert.assertNotNull(snapshot.value) }
    }

    @Test fun mealCanBePreparedForThreeDaysWithoutShowingEngineDetails() {
        rule.setContent { MaterialTheme { FoodPlanHomeScreen(profile,emptyMap()) } }
        rule.onNodeWithText("Рецепты").performClick()
        rule.onNodeWithTag("home-meal-0").performClick()
        rule.onNodeWithText("Готовить на сколько дней?").assertExists()
        rule.onNodeWithText("3 дн.").performClick()
        rule.onNodeWithText("Выбрано: готовить на 3 дн.").assertExists()
        rule.onNodeWithText("Сохранить").performClick()
        rule.onNodeWithText("Готовить на сколько дней?").assertDoesNotExist()
        rule.onNodeWithText("Приготовить сразу на 3 дня").assertExists()
        rule.onNodeWithText("День 2").performClick()
        rule.onNodeWithText("Блюдо готовится заранее на 3 дня").assertExists()
    }
}
