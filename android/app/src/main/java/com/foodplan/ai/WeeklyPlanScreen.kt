package com.foodplan.ai

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.foodplan.domain.*
import kotlin.math.roundToInt

/**
 * M2/M3 functional preview, NOT a verified nutritional or retailer-price plan.
 * Estimates and missing prices must remain visible to users.
 */
@Composable
fun WeeklyPlanScreen(
    profile: FoodProfile,
    votes: Map<String, TasteRating>,
    onClose: () -> Unit
) {
    val result = remember(profile, votes) { SevenDayPlanner.plan(profile, votes) }
    var tab by remember { mutableIntStateOf(0) }
    var dayIndex by remember { mutableIntStateOf(0) }
    var selectedMeal by remember { mutableStateOf<PlannedMeal?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Черновик меню на 7 дней", style = MaterialTheme.typography.headlineSmall)
        Text("КБЖУ приблизительные · цены магазинов не подключены",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error)
        OutlinedButton(onClick = onClose) { Text("Вернуться к анкете") }
        Spacer(Modifier.height(8.dp))
        when (result) {
            is PlanningResult.Blocked -> {
                Text("Меню не удалось составить:", style = MaterialTheme.typography.titleMedium)
                result.reasons.forEach { Text("• $it") }
            }
            is PlanningResult.Success -> {
                TabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 },
                        text = { Text("7 дней") })
                    Tab(selected = tab == 1, onClick = { tab = 1 },
                        text = { Text("Корзина") })
                }
                Spacer(Modifier.height(10.dp))
                if (tab == 0) {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (0..6).forEach { index ->
                            FilterChip(selected = dayIndex == index, onClick = { dayIndex = index },
                                label = { Text("День ${index + 1}") })
                        }
                    }
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val day = result.days[dayIndex]
                        Text(
                            "Всего ~${day.nutrition.kcal.roundToInt()} ккал · " +
                                "Б ${day.nutrition.protein.roundToInt()} / " +
                                "Ж ${day.nutrition.fat.roundToInt()} / " +
                                "У ${day.nutrition.carbs.roundToInt()} г",
                            style = MaterialTheme.typography.titleMedium
                        )
                        result.calorieTargetEstimate?.let {
                            Text("Расчётный ориентир: ~$it ккал/сутки (не назначение врача).",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        day.meals.forEachIndexed { position, meal ->
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth().clickable { selectedMeal = meal }
                            ) {
                                Column(Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text("Приём ${position + 1} · ${if (meal.recipe.type == MealType.BREAKFAST) "завтрак" else "основное блюдо"}",
                                        style = MaterialTheme.typography.labelLarge)
                                    Text(meal.recipe.name, style = MaterialTheme.typography.titleMedium)
                                    Text("~${meal.nutrition.kcal.roundToInt()} ккал · " +
                                        "${meal.recipe.preparation.totalMinutes} мин",
                                        style = MaterialTheme.typography.bodySmall)
                                    Text("Нажмите, чтобы открыть граммовки и рецепт",
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        Spacer(Modifier.height(5.dp))
                        result.warnings.forEach {
                            Text(it, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error)
                        }
                    }
                } else {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Общая корзина на 7 дней", style = MaterialTheme.typography.titleMedium)
                        Text("Выбраны: ${profile.stores.sorted().joinToString(", ")} · ${profile.city}")
                        Text("Недельный бюджет: ${profile.budgetRub} ₽. Проверка бюджета недоступна без актуальных цен и упаковок конкретного магазина.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                        result.shopping.forEach { need ->
                            HorizontalDivider()
                            Text(need.ingredient.name, style = MaterialTheme.typography.titleSmall)
                            Text("Для блюд: ${need.grams} г · ориентир: ${need.packsToBuy} уп. по ${need.ingredient.packGrams} г",
                                style = MaterialTheme.typography.bodyMedium)
                            Text("Цена: нет подтверждённых данных", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Количество упаковок предварительное. Реальные фасовки и наличие выбранного магазина пока не проверены.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
    selectedMeal?.let { meal ->
        AlertDialog(
            onDismissRequest = { selectedMeal = null },
            title = { Text(meal.recipe.name) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Состав (сырой вес, примерные количества)",
                        style = MaterialTheme.typography.titleSmall)
                    meal.recipe.ingredients.forEach {
                        Text("• ${OfflineRecipes.ingredients.getValue(it.id).name}: " +
                            "${(it.grams * meal.servings).roundToInt()} г")
                    }
                    Text("Как приготовить", style = MaterialTheme.typography.titleSmall)
                    meal.recipe.steps.forEachIndexed { index, step -> Text("${index + 1}. $step") }
                    Text("Все нутриенты оценочные; следы аллергенов требуют проверки упаковки.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedMeal = null }) { Text("Закрыть") }
            }
        )
    }
}
