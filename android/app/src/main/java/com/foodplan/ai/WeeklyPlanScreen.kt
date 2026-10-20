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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    retailerOffers: List<RetailerOffer> = emptyList(),
    onSaveRetailerOffer: (RetailerOffer) -> Unit = {},
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
    var showCatalogBrowser by remember { mutableStateOf(false) }

    var chosenStore by remember(profile.stores) { mutableStateOf(profile.stores.sorted().firstOrNull() ?: "") }
    val planningScope = rememberCoroutineScope()
    var budgetOptimizationRunning by remember { mutableStateOf(false) }
    var budgetOptimizationStatus by remember(profile) { mutableStateOf<String?>(null) }

    if (showCatalogBrowser && result is PlanningResult.Success) {
        val plan = result as PlanningResult.Success
        MagnitCatalogBrowser(
            shopping = ShoppingAggregation.fromDays(
                plan.days,profile.household.toInt(),pantryGrams),
            city = profile.city,
            budgetRub = profile.budgetRub.toLongOrNull() ?: 0L,
            onCaptured = onSaveRetailerOffer,
            onClose = { showCatalogBrowser = false }
        )
        return
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Черновик меню на 7 дней", style = MaterialTheme.typography.headlineSmall)
        Text("КБЖУ предварительные · цены и оценки доступны только для найденных товаров",
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
                    // User selects which Russian store a manual price refers to.
                    val scopedPrices = priceEntries.filterKeys {
                        it.startsWith("${profile.city}|${chosenStore}|")
                    }.mapKeys { it.key.substringAfterLast('|') }
                    val cost = ManualBasketPrices.estimate(
                        screenResult.shopping, scopedPrices, profile.budgetRub
                    )
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Общая корзина на 7 дней", style = MaterialTheme.typography.titleMedium)
                        Text("Выбран магазин для ручных цен:")
                        Row(Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            profile.stores.sorted().forEach { name ->
                                FilterChip(
                                    selected = chosenStore == name,
                                    onClick = { chosenStore = name },
                                    label = { Text(name) }
                                )
                            }
                        }
                        if (chosenStore == "Магнит") {
                            OutlinedButton(onClick = { showCatalogBrowser = true }) {
                                Text("Открыть каталог Магнита и считать цены")
                            }
                            Text("Подбор товаров, цены, наличие и отзывы остаются в FoodPlan AI.",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        val liveQuote = RetailerBasketPricing.estimate(
                            screenResult.shopping,retailerOffers,
                            profile.city,chosenStore,profile.budgetRub,System.currentTimeMillis()
                        )
                        val selectedBasket = BudgetBasketOptimizer.choose(
                            screenResult.shopping,retailerOffers,
                            profile.city,chosenStore,
                            profile.budgetRub.toLongOrNull() ?: 0L,
                            System.currentTimeMillis()
                        )
                        Text("Подбор по недельному бюджету: ${profile.budgetRub} ₽",
                            style = MaterialTheme.typography.titleMedium)
                        Text("Вариантов в списке: ${selectedBasket.items.size}; " +
                            "не найдены: ${selectedBasket.missingIngredientIds.size}.")
                        Text("Сумма выбранных упаковок: ~${ManualBasketPrices.formatKopeks(selectedBasket.estimatedTotalKopeks)} ₽.")
                        when(selectedBasket.fitsBudget) {
                            true -> Text("В пределах бюджета по известным ценам. " +
                                "Наличие и доставка проверяются отдельно.")
                            false -> Text("Минимальная найденная корзина превышает бюджет. " +
                                "Нужно пересмотреть рецепты или магазин.",
                                color = MaterialTheme.colorScheme.error)
                            null -> Text("Нельзя проверить бюджет: есть продукты без актуальных цен.",
                                color = MaterialTheme.colorScheme.error)
                        }
                        Text("Подобрано улучшений по известным отзывам: ${selectedBasket.budgetResponsiveUpgrades}. " +
                            "Товаров с оценкой и минимум 10 отзывами: ${selectedBasket.ratingCoverageCount}.")
                        Text("Больший бюджет разрешает разнообразие и лучшие проверенные варианты, " +
                            "но не требует тратить весь лимит.",
                            style = MaterialTheme.typography.bodySmall)
                        Button(
                            enabled=!budgetOptimizationRunning &&
                                retailerOffers.any { it.storeName==chosenStore },
                            onClick={
                                val from=result as? PlanningResult.Success ?: return@Button
                                budgetOptimizationRunning=true
                                planningScope.launch {
                                    try {
                                        val refined=withContext(Dispatchers.Default) {
                                            BudgetWeekRefiner.refine(
                                                from,profile,votes,retailerOffers,chosenStore,
                                                System.currentTimeMillis(),pantryGrams
                                            )
                                        }
                                        if(refined.status==BudgetWeekStatus.MISSING_PRICES) {
                                            budgetOptimizationStatus =
                                                "Нельзя оптимизировать меню: не хватает актуальных цен на ингредиенты."
                                        } else {
                                            result=refined.plan
                                            budgetOptimizationStatus=if(refined.status==BudgetWeekStatus.WITHIN_BUDGET)
                                                "Пересчёт выполнен. Изменено блюд: ${refined.numberOfMealChanges}. " +
                                                "Предварительная корзина укладывается в бюджет."
                                            else "Даже после замен неделя превышает бюджет. " +
                                                "Нужны другие продукты или рецепты, не утверждаем обратного."
                                        }
                                    } catch (_:Exception) {
                                        budgetOptimizationStatus="Ошибка проверки бюджета. Меню сохранено без изменений."
                                    } finally {
                                        budgetOptimizationRunning=false
                                    }
                                }
                            }
                        ) {
                            Text(if(budgetOptimizationRunning) "Пересчитываю неделю..." else
                                "Подогнать меню под бюджет и ассортимент")
                        }
                        budgetOptimizationStatus?.let {
                            Text(it,style=MaterialTheme.typography.bodySmall,
                                color=MaterialTheme.colorScheme.primary)
                        }
                        if(!selectedBasket.stockConfirmed) {
                            Text("Наличие части товаров по адресу не подтверждено.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall)
                        }
                        selectedBasket.items.forEach { item ->
                            Text("${item.productName}: ${item.packs} уп. · " +
                                "${ManualBasketPrices.formatKopeks(item.costKopeks)} ₽ · " +
                                (item.ratingStars?.let { "★ $it (" + (item.reviewCount?.toString() ?: "отзывов нет данных") + ")" }
                                    ?: "нет данных об отзывах") +
                                " · ${when(item.availability) {
                                    ListingAvailability.IN_STOCK -> "в наличии"
                                    ListingAvailability.OUT_OF_STOCK -> "нет"
                                    ListingAvailability.UNKNOWN -> "наличие неизвестно"
                                }}",
                                style=MaterialTheme.typography.bodySmall)
                        }
                        Text("Из каталога сайта: ${liveQuote.coveredLines} позиций; " +
                            "без подходящей цены: ${liveQuote.missingLines}.")
                        Text("Сумма по найденным фасовкам: ~${ManualBasketPrices.formatKopeks(liveQuote.knownTotalKopeks)} ₽.",
                            style = MaterialTheme.typography.bodyMedium)
                        liveQuote.budgetEstimate?.let { fits ->
                            Text(if (fits) "По наблюдаемым ценам корзина ориентировочно в бюджете."
                                else "По наблюдаемым ценам корзина выходит за бюджет.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Text("Это наблюдения каталога, а не подтверждение цены и наличия при оформлении.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error)
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
                        Text("Недельный лимит на всю корзину: ${profile.budgetRub} ₽. " +
                            "Если товаров или подтверждения наличия нет, нельзя заявлять, что бюджет выполнен.",
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
