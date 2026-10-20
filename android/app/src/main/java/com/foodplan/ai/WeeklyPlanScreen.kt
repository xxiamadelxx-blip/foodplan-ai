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
    priceEntries: Map<String, Long> = emptyMap(),
    pantryGrams: Map<String, Int> = emptyMap(),
    onSavePantry: (String, Int) -> Unit = { _, _ -> },
    onSavePrice: (String, String, Long) -> Unit = { _, _, _ -> },
    onClose: () -> Unit
) {
    var result by remember(profile, votes) { mutableStateOf(SevenDayPlanner.plan(profile, votes)) }
    var tab by remember { mutableIntStateOf(0) }
    var dayIndex by remember { mutableIntStateOf(0) }
    var selectedMeal by remember { mutableStateOf<PlannedMeal?>(null) }
    var selectedMealIndex by remember { mutableIntStateOf(0) }
    var showAlternatives by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Черновик меню на 7 дней", style = MaterialTheme.typography.headlineSmall)
        Text("КБЖУ приблизительные · цены магазинов не подключены",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error)
        OutlinedButton(onClick = onClose) { Text("Вернуться к анкете") }
        Spacer(Modifier.height(8.dp))
        val screenResult = when (val raw = result) {
            is PlanningResult.Success -> raw.copy(shopping =
                ShoppingAggregation.fromDays(raw.days,profile.household.toInt(),pantryGrams))
            is PlanningResult.Blocked -> raw
        }
        when (screenResult) {
            is PlanningResult.Blocked -> {
                Text("Меню не удалось составить:", style = MaterialTheme.typography.titleMedium)
                screenResult.reasons.forEach { Text("• $it") }
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
                        val day = screenResult.days[dayIndex]
                        Text(
                            "Всего ~${day.nutrition.kcal.roundToInt()} ккал · " +
                                "Б ${day.nutrition.protein.roundToInt()} / " +
                                "Ж ${day.nutrition.fat.roundToInt()} / " +
                                "У ${day.nutrition.carbs.roundToInt()} г",
                            style = MaterialTheme.typography.titleMedium
                        )
                        screenResult.calorieTargetEstimate?.let {
                            Text("Расчётный ориентир: ~$it ккал/сутки (не назначение врача).",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        day.meals.forEachIndexed { position, meal ->
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    selectedMealIndex = position
                                    selectedMeal = meal
                                }
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
                        screenResult.warnings.forEach {
                            Text(it, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error)
                        }
                    }
                } else {
                    val chosenStore = profile.stores.sorted().firstOrNull() ?: ""
                    val scopedPrices = priceEntries.filterKeys {
                        it.startsWith("${profile.city}|${chosenStore}|")
                    }.mapKeys { it.key.substringAfterLast('|') }
                    val cost = ManualBasketPrices.estimate(
                        screenResult.shopping, scopedPrices, profile.budgetRub
                    )
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Общая корзина на 7 дней", style = MaterialTheme.typography.titleMedium)
                        Text("Ручная оценка по магазину: $chosenStore")
                        Text("По введённым ценам: ${ManualBasketPrices.formatKopeks(cost.knownTotalKopeks)} ₽; " +
                            "без цены: ${cost.missingLines} позиций.",
                            style = MaterialTheme.typography.bodyMedium)
                        cost.estimateFitsBudget?.let { fits ->
                            Text(if (fits) "По ручным ценам сумма не превышает бюджет."
                                else "По ручным ценам бюджет превышен.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium)
                        }
                        Text("Выбраны: ${profile.stores.sorted().joinToString(", ")} · ${profile.city}")
                        Text("Недельный бюджет: ${profile.budgetRub} ₽. Проверка бюджета недоступна без актуальных цен и упаковок конкретного магазина.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                        screenResult.shopping.forEach { need ->
                            HorizontalDivider()
                            Text(need.ingredient.name, style = MaterialTheme.typography.titleSmall)
                            Text("Нужно всего: ${need.requiredGrams} г · дома: ${need.pantryUsed} г · докупить: ${need.grams} г",
                                style = MaterialTheme.typography.bodyMedium)
                            Text("Ориентир покупки: ${need.packsToBuy} уп. по ${need.ingredient.packGrams} г",
                                style = MaterialTheme.typography.bodyMedium)
                            var pantryText by remember(need.ingredient.id, pantryGrams[need.ingredient.id]) {
                                mutableStateOf((pantryGrams[need.ingredient.id] ?: 0).toString())
                            }
                            OutlinedTextField(
                                value = pantryText, onValueChange = { pantryText = it },
                                label = { Text("Есть дома, г") }, singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            val available = pantryText.toIntOrNull()?.takeIf { it in 0..500000 }
                            TextButton(
                                enabled = available != null && available != pantryGrams[need.ingredient.id],
                                onClick = { if (available != null) onSavePantry(need.ingredient.id,available) }
                            ) { Text("Сохранить домашний остаток") }
                            val current = scopedPrices[need.ingredient.id]
                            var rubles by remember(need.ingredient.id, current) {
                                mutableStateOf(current?.let(ManualBasketPrices::formatKopeks) ?: "")
                            }
                            OutlinedTextField(
                                value = rubles,
                                onValueChange = { rubles = it },
                                label = { Text("Ручная цена упаковки, ₽") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            val cents = ManualBasketPrices.fromRubles(rubles)
                            TextButton(onClick = {
                                if (cents != null) onSavePrice(chosenStore, need.ingredient.id, cents)
                            }, enabled = cents != null && cents != current) {
                                Text("Запомнить введённую цену")
                            }
                            Text("Не проверено: остатки, фасовка и цена в магазине.",
                                style = MaterialTheme.typography.bodySmall)
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
                TextButton(onClick = {
                    selectedMeal = null
                    showAlternatives = true
                }) { Text("Заменить блюдо") }
                TextButton(onClick = { selectedMeal = null }) { Text("Закрыть") }
            }
        )
    }
    if (showAlternatives && result is PlanningResult.Success) {
        val current = result as PlanningResult.Success
        val choices = remember(profile, votes, dayIndex, selectedMealIndex, current) {
            MealReplacement.alternatives(profile,votes,current,dayIndex,selectedMealIndex)
        }
        AlertDialog(
            onDismissRequest = { showAlternatives = false },
            title = { Text("Выбрать другое блюдо") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    choices.forEach { replacement ->
                        TextButton(onClick = {
                            result = MealReplacement.replace(
                                profile,votes,current,dayIndex,selectedMealIndex,replacement.id,pantryGrams)
                            showAlternatives = false
                        }) {
                            Text(replacement.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAlternatives = false }) { Text("Отмена") }
            }
        )
    }
}
