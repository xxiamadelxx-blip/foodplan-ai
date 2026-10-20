package com.foodplan.ai

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.foodplan.domain.CookingEffort
import com.foodplan.domain.FoodProfile
import org.junit.Rule
import org.junit.Test

class WeeklyPlanScreenTest {
    @get:Rule val rule = createComposeRule()
    private val profile = FoodProfile(
        sex="Женский", age="30", heightCm="165", weightKg="65",
        activity="Умеренная", budgetRub="5000", cookingMinutes="55",
        cookingEffort=CookingEffort.INVOLVED,
        appliances=setOf("Плита","Кастрюля","Сковорода","Духовка")
    )
    @Test fun rendersSevenDaysAndShoppingListWithoutInventingRetailPrice() {
        rule.setContent { MaterialTheme {
            WeeklyPlanScreen(profile, emptyMap(), onClose={})
        } }
        rule.onNodeWithText("Черновик меню на 7 дней").assertExists()
        rule.onNodeWithText("День 1").assertExists()
        rule.onNodeWithText("Корзина").performClick()
        rule.onNodeWithText("Общая корзина на 7 дней").assertExists()
        rule.onNodeWithText("Недельный бюджет: 5000 ₽. Проверка бюджета недоступна без актуальных цен и упаковок конкретного магазина.").assertExists()
    }
}
