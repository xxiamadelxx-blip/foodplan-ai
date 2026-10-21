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

    @Test fun onboardingBeginsWithPersonalSettingsAndGoal() {
        rule.onNodeWithText("FoodPlan AI").assertExists()
        rule.onNodeWithText("О вас").assertExists()
        rule.onNodeWithText("Шаг 1 из 4").assertExists()
        rule.onNodeWithText("Далее").performClick()
        rule.onNodeWithText("Цель питания").assertExists()
        rule.onNodeWithText("Назад").performClick()
        rule.onNodeWithText("О вас").assertExists()
    }

    @Test fun onboardingCollectsDietAndKitchenBeforeTasteSurvey() {
        rule.onNodeWithText("Далее").performClick()
        rule.onNodeWithText("Далее").performClick()
        rule.onNodeWithText("Тип питания и ограничения").assertExists()
        rule.onNodeWithText("Аллергены (через запятую)").assertExists()
        rule.onNodeWithText("Далее").performClick()
        rule.onNodeWithText("Привычки и кухня").assertExists()
        rule.onNodeWithText("На сколько человек").assertExists()
        rule.onNodeWithText("Насколько хочешь заморочиться с рецептами?")
            .performScrollTo().assertExists()
        rule.onNodeWithText("Аэрогриль").performScrollTo().assertExists()
    }
}
