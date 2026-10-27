package com.foodplan.ai

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.foodplan.domain.CookingEffort
import com.foodplan.domain.FoodProfile
import org.junit.Rule
import org.junit.Test

/** End-user regression: home stock is not silently hidden by the buy list. */
class RecipeReplacementPantryAndroidTest {
    @get:Rule val rule = createComposeRule()

    @Test fun selectingStockNotInPurchaseListShowsProductInRecipeReplacement() {
        val profile = FoodProfile(
            sex = "Женский", age = "31", heightCm = "165", weightKg = "70",
            budgetRub = "356", city = "Санкт-Петербург",
            stores = setOf("Пятёрочка"), household = "2",
            mealsPerDay = "3", cookingMinutes = "120",
            cookingEffort = CookingEffort.ALL_OUT,
            appliances = setOf("Плита", "Сковорода", "Кастрюля", "Духовка")
        )
        rule.setContent {
            MaterialTheme {
                FoodPlanHomeScreen(
                    profile, emptyMap(),
                    pantryGrams = mapOf("tofu" to 100_000),
                    automaticCatalogReadingEnabled = false
                )
            }
        }
        rule.onNodeWithText("Рецепты").performClick()
        rule.onNodeWithTag("home-meal-1").performClick()
        rule.onNodeWithTag("home-open-replacement").performClick()
        val pantryChip = rule.onNodeWithTag("home-replacement-product-tofu")
        pantryChip.assertExists()
        pantryChip.performScrollTo().performClick()
        pantryChip.assertIsSelected()
        rule.onNodeWithText(
            "Подбираем только рецепты со связями с выбранными продуктами: 1."
        ).assertExists()
    }
}
