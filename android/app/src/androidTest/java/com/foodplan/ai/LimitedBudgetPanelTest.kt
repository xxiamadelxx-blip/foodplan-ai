package com.foodplan.ai

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.foodplan.domain.FoodProfile
import org.junit.Rule
import org.junit.Test

class LimitedBudgetPanelTest {
    @get:Rule val rule=createComposeRule()
    @Test fun shows100RublesAsIncompleteFoodAndDoesNotInventPrices() {
        rule.setContent { MaterialTheme {
            LimitedBudgetPanel(
                profile=FoodProfile(budgetRub="100",age="30",heightCm="165",
                    weightKg="65",sex="Женский",city="Санкт-Петербург"),
                store="Магнит",offers=emptyList(),
                manualPricesKopeks=mapOf("potato" to 10000L),
                calorieTargetPerPerson=2000
            )
        } }
        rule.onNodeWithText("Что доступно на мой бюджет").assertExists()
        rule.onNodeWithText("Оценка: 100.00 ₽ из 100.00 ₽").assertExists()
        rule.onNodeWithText("не означает",substring=true).assertExists()
    }
}
