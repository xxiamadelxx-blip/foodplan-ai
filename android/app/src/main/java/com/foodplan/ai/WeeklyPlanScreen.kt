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
    var result by remember(profile, votes, pantryGrams) {
        mutableStateOf(SevenDayPlanner.plan(profile, votes, pantryGrams))
    }
    var tab by remember { mutableIntStateOf(0) }
    var networkSource by remember { mutableIntStateOf(if (pantryGrams.values.any { it > 0 }) 0 else 1) }
    var expandedNetworkRecipe by remember { mutableStateOf<String?>(null) }
    var selectedNetworkProducts by remember { mutableStateOf<Set<String>>(emptySet()) }
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
            onClose = { showCatalogBrowser = false },
            strictAllergenVerification = profile.allergens.isNotBlank()
        )
        return
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Черновик меню на 7 дней", style = MaterialTheme.typography.headlineSmall)
        Text("Бюджет: ${profile.budgetRub} ₽ на 7 дней · цены и КБЖУ предварительные",
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
                    Tab(selected = tab == 2, onClick = { tab = 2 },
                        text = { Text("Из продуктов") })
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
                } else if (tab == 1) {
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
                        if(selectedBasket.fitsBudget != true) {
                            LimitedBudgetPanel(
                                profile=profile,store=chosenStore,
                                offers=retailerOffers,manualPricesKopeks=scopedPrices,
                                calorieTargetPerPerson=screenResult.calorieTargetEstimate
                            )
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
                } else {
                    val plannedIngredients = screenResult.shopping.associate { need ->
                        need.ingredient.id to need.requiredGrams
                    }
                    // Retail product cards provide possible shopping links, NOT pantry stock.
                    // Only this user-selected retailer and city are used.
                    val nowEpochMs = System.currentTimeMillis()
                    val matchedRetailerOffers = retailerOffers.filter { offer ->
                        RetailerOfferChecks.isUsable(
                            offer, offer.ingredientId, profile.city, chosenStore, nowEpochMs
                        ) && RetailerRecipeNamePolicy.compatible(offer)
                    }
                    val sourceIngredients = when(networkSource) {
                        0 -> pantryGrams
                        1 -> plannedIngredients
                        else -> matchedRetailerOffers.groupBy { it.ingredientId }
                            .mapValues { (_, items) -> items.maxOf { it.packGrams } }
                    }
                    val availableIngredients = if (selectedNetworkProducts.isEmpty()) sourceIngredients
                        else sourceIngredients.filterKeys { it in selectedNetworkProducts }
                    val suggestions = if (networkSource != 2)
                        ConnectedRecipeSuggestions.recommend(
                            profile, availableIngredients, votes, maxResults = 25
                        ) else emptyList()
                    val storeRecipeSuggestions = if (networkSource == 2)
                        RetailerRecipeNetwork.recommend(
                            profile, matchedRetailerOffers, chosenStore, profile.city,
                            nowEpochMs, votes, selectedNetworkProducts, maxResults = 25
                        ) else emptyList()
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Ниточки от продуктов к рецептам",
                            style = MaterialTheme.typography.titleMedium)
                        Text("Чем больше подходящих продуктов ведёт к блюду, тем выше оно в рекомендациях. " +
                            "Вкусы, ограничения и сложность приготовления тоже учитываются.",
                            style = MaterialTheme.typography.bodySmall)
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = networkSource == 0,
                                onClick = {
                                    networkSource = 0
                                    selectedNetworkProducts = emptySet()
                                }, label = { Text("Есть дома") })
                            FilterChip(selected = networkSource == 1,
                                onClick = {
                                    networkSource = 1
                                    selectedNetworkProducts = emptySet()
                                }, label = { Text("Из плана покупок") })
                            FilterChip(selected = networkSource == 2,
                                onClick = {
                                    networkSource = 2
                                    selectedNetworkProducts = emptySet()
                                }, label = { Text("Товары магазина") })
                        }
                        if (networkSource == 1) {
                            Text("Это продукты, запланированные для покупки, а не подтверждённые покупки или наличие в магазине.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error)
                        }
                        if (networkSource == 2) {
                            Text("Найденные на сайте товары · не куплены · состав упаковки и наличие по адресу не подтверждены.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error)
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                profile.stores.sorted().forEach { store ->
                                    FilterChip(selected = chosenStore == store,
                                        onClick = {
                                            chosenStore = store
                                            selectedNetworkProducts = emptySet()
                                        }, label = { Text(store) })
                                }
                            }
                        }
                        val connectedProducts = sourceIngredients.filterValues { it > 0 }.keys.sorted()
                        if (connectedProducts.isNotEmpty()) {
                            Text(if (networkSource == 2)
                                "Выберите найденные магазинные продукты, от которых строить связи с рецептами."
                                else "Отметьте один или несколько продуктов: найденные рецепты получат больше связей.",
                                style = MaterialTheme.typography.bodySmall)
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                connectedProducts.forEach { ingredientId ->
                                    FilterChip(
                                        selected = ingredientId in selectedNetworkProducts,
                                        onClick = {
                                            selectedNetworkProducts = if (ingredientId in selectedNetworkProducts)
                                                selectedNetworkProducts - ingredientId
                                            else selectedNetworkProducts + ingredientId
                                        },
                                        label = { Text(OfflineRecipes.ingredients[ingredientId]?.name ?: ingredientId) }
                                    )
                                }
                            }
                            Text("Отмечено продуктов: ${selectedNetworkProducts.size}" +
                                if (selectedNetworkProducts.isEmpty()) " · показываются все" else "",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        if (networkSource == 2) {
                            if (storeRecipeSuggestions.isEmpty()) {
                                Text("Нет подходящих актуальных карточек в выбранном магазине. " +
                                    "Откройте каталог в разделе «Корзина» и сохраните варианты. " +
                                    "Неизвестный состав, аллерген или несовпадение вида не считаются покупкой.",
                                    style = MaterialTheme.typography.bodyMedium)
                            }
                            storeRecipeSuggestions.forEach { suggestion ->
                                OutlinedCard(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Text(suggestion.recipe.name,
                                            style = MaterialTheme.typography.titleMedium)
                                        Text("Связей с товарами: ${suggestion.linkedCount} из " +
                                            "${suggestion.recipe.ingredients.map { it.id }.distinct().size}",
                                            style = MaterialTheme.typography.labelLarge)
                                        suggestion.linkedLines.forEach { line ->
                                            Text("• ${line.skuName}: ${line.packageCount} уп. по " +
                                                "${line.packageGrams} г · " +
                                                "~${ManualBasketPrices.formatKopeks(line.displayedCostKopeks)} ₽",
                                                style = MaterialTheme.typography.bodySmall)
                                        }
                                        if (suggestion.missingIngredientIds.isNotEmpty()) {
                                            Text("Товары не найдены: " + suggestion.missingIngredientIds.sorted()
                                                .joinToString(", ") { id ->
                                                    OfflineRecipes.ingredients[id]?.name ?: id
                                                }, style = MaterialTheme.typography.bodySmall)
                                        }
                                        val full = suggestion.estimatedRecipeCostKopeks
                                        Text(if (full != null)
                                            "Стоимость целых упаковок для этого приготовления: " +
                                                "~${ManualBasketPrices.formatKopeks(full)} ₽"
                                            else "Известная часть стоимости: " +
                                                "~${ManualBasketPrices.formatKopeks(suggestion.observedSubtotalKopeks)} ₽; " +
                                                "полная стоимость неизвестна.",
                                            style = MaterialTheme.typography.bodySmall)
                                        Text(if (suggestion.addressStockConfirmed)
                                            "Статус предложения: наличие заявлено на сайте, не подтверждено при оплате."
                                            else "Наличие всех товаров по адресу не подтверждено.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error)
                                        TextButton(onClick = {
                                            expandedNetworkRecipe = if (expandedNetworkRecipe == suggestion.recipe.id)
                                                null else suggestion.recipe.id
                                        }) { Text(if (expandedNetworkRecipe == suggestion.recipe.id)
                                            "Свернуть рецепт" else "Посмотреть рецепт") }
                                        if (expandedNetworkRecipe == suggestion.recipe.id) {
                                            suggestion.recipe.ingredients.forEach { part ->
                                                Text("• ${OfflineRecipes.ingredients[part.id]?.name ?: part.id}: ${part.grams} г",
                                                    style = MaterialTheme.typography.bodySmall)
                                            }
                                            suggestion.recipe.steps.forEachIndexed { index, step ->
                                                Text("${index + 1}. $step", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            if (suggestions.isEmpty()) {
                                Text(if (networkSource == 0)
                                    "Добавьте продукты и их количество в разделе «Корзина», чтобы увидеть связанные рецепты."
                                    else "Пока нет рецептов с подходящими продуктами.",
                                    style = MaterialTheme.typography.bodyMedium)
                            }
                            suggestions.forEach { suggestion ->
                            OutlinedCard(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(suggestion.recipe.name,
                                        style = MaterialTheme.typography.titleMedium)
                                    Text("Совпадений: ${suggestion.linkedProducts.size} из " +
                                        "${suggestion.recipe.ingredients.map { it.id }.distinct().size}",
                                        style = MaterialTheme.typography.labelLarge)
                                    val linkedNames = suggestion.linkedProducts.sorted().mapNotNull { id ->
                                        OfflineRecipes.ingredients[id]?.name
                                    }
                                    Text("Связанные продукты: ${linkedNames.joinToString(", ")}",
                                        style = MaterialTheme.typography.bodySmall)
                                    val missing = suggestion.recipe.ingredients.groupBy { it.id }
                                        .mapValues { (_, entries) -> entries.sumOf { it.grams } }
                                        .filter { (id, grams) ->
                                            (availableIngredients[id] ?: 0) < grams
                                        }
                                    if (missing.isNotEmpty()) {
                                        Text("Нужно добавить: " + missing.entries.joinToString(", ") { (id, grams) ->
                                            val missingGrams = (grams - (availableIngredients[id] ?: 0)).coerceAtLeast(0)
                                            "${OfflineRecipes.ingredients[id]?.name ?: id} ${missingGrams} г"
                                        }, style = MaterialTheme.typography.bodySmall)
                                    } else {
                                        Text("Все ингредиенты для одной порции присутствуют в выбранном списке.",
                                            style = MaterialTheme.typography.bodySmall)
                                    }
                                    TextButton(onClick = {
                                        expandedNetworkRecipe = if (expandedNetworkRecipe == suggestion.recipe.id)
                                            null else suggestion.recipe.id
                                    }) { Text(if (expandedNetworkRecipe == suggestion.recipe.id)
                                        "Свернуть рецепт" else "Посмотреть рецепт") }
                                    if (expandedNetworkRecipe == suggestion.recipe.id) {
                                        suggestion.recipe.ingredients.forEach { ingredient ->
                                            Text("• ${OfflineRecipes.ingredients[ingredient.id]?.name ?: ingredient.id}: ${ingredient.grams} г",
                                                style = MaterialTheme.typography.bodySmall)
                                        }
                                        suggestion.recipe.steps.forEachIndexed { index, step ->
                                            Text("${index + 1}. $step", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                        }
                        Text("Совпадение ингредиентов не подтверждает качество, пищевую безопасность или доступность товарных SKU. Рецепты пока проходят проверку M2.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error)
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
                    Text("Состав (масса до приготовления, примерные количества)",
                        style = MaterialTheme.typography.titleSmall)
                    meal.recipe.ingredients.forEach { part ->
                        val ingredient = OfflineRecipes.ingredients.getValue(part.id)
                        val basis = IngredientWeightBasisRegistry.basisFor(part.id)
                        val label = basis?.let { IngredientWeightBasisRegistry.russianLabel(it) }
                            ?: "состояние ингредиента не уточнено"
                        Text("• ${ingredient.name}: " +
                            "${(part.grams * meal.servings).roundToInt()} г ($label)")
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
