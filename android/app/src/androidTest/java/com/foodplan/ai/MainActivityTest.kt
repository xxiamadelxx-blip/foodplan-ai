package com.foodplan.ai

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun onboardingMainScreenAndNavigation() {
        rule.onNodeWithText("FoodPlan AI").assertExists()
        rule.onNodeWithText("О вас").assertExists()
        rule.onNodeWithText("Далее").performClick()
        rule.onNodeWithText("Цель питания").assertExists()
        rule.onNodeWithText("Назад").performClick()
        rule.onNodeWithText("О вас").assertExists()
    }

    @Test fun storesAndAllergensVisible() {
        rule.onNodeWithText("Далее").performClick()
        rule.onNodeWithText("Далее").performClick()
        rule.onNodeWithText("Где обычно покупаете?").assertExists()
        rule.onNodeWithText("Далее").performClick()
        rule.onNodeWithText("Аллергены (через запятую)").assertExists()
    }
    @Test fun weeklyBudgetHasContinuousSliderAndExactRubleInput() {
        repeat(2) { rule.onNodeWithText("Далее").performClick() }
        rule.onNodeWithText("Бюджет на 7 дней").assertExists()
        rule.onNodeWithText("100 ₽").assertExists()
        rule.onNodeWithText("100 000 ₽").assertExists()
        rule.onNodeWithText("Точно до рубля: например 356 или 63218")
            .performScrollTo().assertExists()
    }
    @Test fun cookingEffortAndToolsAreAvailable() {
        repeat(4) { rule.onNodeWithText("Далее").performClick() }
        rule.onNodeWithText("Насколько хочешь заморочиться с рецептами?").assertExists()
        rule.onNodeWithText("Что-нибудь повыёбистее").performScrollTo().performClick()
        rule.onNodeWithText("Заебаться и угандошить всю посуду").performScrollTo().assertExists()
        rule.onNodeWithText("Аэрогриль").performScrollTo().assertExists()
        rule.onNodeWithText("Блендер").performScrollTo().assertExists()
    }

}
