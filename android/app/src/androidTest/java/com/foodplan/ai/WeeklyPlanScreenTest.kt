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
    @Test fun recipeNetworkTabExplainsHomeStockVersusPlannedGroceries() {
        rule.setContent { MaterialTheme {
            WeeklyPlanScreen(profile, emptyMap(),
                pantryGrams = mapOf("rice" to 500, "chicken" to 500), onClose = {})
        } }
        rule.onNodeWithText("Из продуктов").performClick()
        rule.onNodeWithText("Ниточки от продуктов к рецептам").assertExists()
        rule.onNodeWithText("Есть дома").assertExists()
        rule.onNodeWithText("Из плана покупок").performClick()
        rule.onNodeWithText("Это продукты, запланированные для покупки, а не подтверждённые покупки или наличие в магазине.").assertExists()
    }

    @Test fun rendersSevenDaysAndShoppingListWithoutInventingRetailPrice() {
        rule.setContent { MaterialTheme {
            WeeklyPlanScreen(profile, emptyMap(), onClose={})
        } }
        rule.onNodeWithText("Черновик меню на 7 дней").assertExists()
        rule.onNodeWithText("День 1").assertExists()
        rule.onNodeWithText("Корзина").performClick()
        rule.onNodeWithText("Общая корзина на 7 дней").assertExists()
        rule.onNodeWithText("Недельный лимит на всю корзину: 5000 ₽. Если товаров или подтверждения наличия нет, нельзя заявлять, что бюджет выполнен.").assertExists()
    }
}
