package com.foodplan.ai

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import com.foodplan.domain.CookingEffort
import com.foodplan.domain.FoodProfile
import com.foodplan.domain.RetailerOffer
import com.foodplan.domain.ListingAvailability
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
        rule.onNodeWithText("Отмечено продуктов: 0 · показываются все").assertExists()
        rule.onNodeWithText("Рис сухой").performClick()
        rule.onNodeWithText("Куриное филе сырое").performClick()
        rule.onNodeWithText("Отмечено продуктов: 2").assertExists()
        rule.onNodeWithText("Из плана покупок").performClick()
        rule.onNodeWithText("Это продукты, запланированные для покупки, а не подтверждённые покупки или наличие в магазине.").assertExists()
    }

    @Test fun catalogRecipeThreadsNeverPretendSkuIsPurchased() {
        val recent = System.currentTimeMillis()
        val productOffers = listOf(
            RetailerOffer("rice", "Магнит", "Санкт-Петербург",
                "Рис длиннозерный 900 г", "https://magnit.ru/product/rice-test",
                900, 12000L, recent, true),
            RetailerOffer("chicken", "Магнит", "Санкт-Петербург",
                "Филе куриной грудки охлажденное 800 г", "https://magnit.ru/product/chicken-test",
                800, 24000L, recent, true, ListingAvailability.UNKNOWN)
        )
        rule.setContent { MaterialTheme {
            WeeklyPlanScreen(profile.copy(stores = setOf("Магнит"),
                city = "Санкт-Петербург"), emptyMap(),
                retailerOffers = productOffers, onClose = {})
        } }
        rule.onNodeWithText("Из продуктов").performClick()
        rule.onNodeWithText("Товары магазина").performClick()
        rule.onNodeWithText(
            "Найденные на сайте товары · не куплены · состав упаковки и наличие по адресу не подтверждены."
        ).assertExists()
        rule.onNodeWithText("Рис сухой").assertExists()
        rule.onNodeWithText("Куриное филе сырое").assertExists()
        rule.onAllNodesWithText("Наличие всех товаров по адресу не подтверждено.",
            substring = true).onFirst().assertExists()
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
