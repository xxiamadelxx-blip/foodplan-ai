package com.foodplan.ai

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
}
