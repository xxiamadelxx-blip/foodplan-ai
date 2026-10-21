package com.foodplan.ai

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

    @Test fun weeklyBudgetAcceptsExactUnroundedRubleAmountsDuringUse() {
        rule.setContent { MaterialTheme { FoodPlanHomeScreen(profile,emptyMap()) } }
        val field=rule.onNodeWithTag("home-weekly-budget-exact-input")
        for (value in listOf("100","356","63218","100000")) {
            field.performTextReplacement(value)
            rule.onNodeWithText("$value ₽").assertExists()
        }
        rule.onNodeWithTag("home-weekly-budget-slider").assertExists()
        rule.onNodeWithText("Магазин").assertExists()
        rule.onNodeWithText("Собрать список на неделю").assertExists()
    }

    @Test fun mealCanBePreparedForThreeDaysWithoutShowingEngineDetails() {
        rule.setContent { MaterialTheme { FoodPlanHomeScreen(profile,emptyMap()) } }
        rule.onNodeWithText("Рецепты").performClick()
        rule.onNodeWithTag("home-meal-0").performClick()
        rule.onNodeWithText("Готовить на сколько дней?").assertExists()
        rule.onNodeWithText("3 дн.").performClick()
        rule.onNodeWithText("Сохранить").performClick()
        rule.onNodeWithText("Приготовить сразу на 3 дня").assertExists()
        rule.onNodeWithText("День 2").performClick()
        rule.onNodeWithText("Блюдо готовится заранее на 3 дня").assertExists()
    }
}
