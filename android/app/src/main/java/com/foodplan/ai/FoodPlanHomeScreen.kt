package com.foodplan.ai

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.foodplan.domain.*
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val groceryStores = listOf(
    "Пятёрочка", "Магнит", "Лента", "Перекрёсток", "Дикси", "О’КЕЙ",
    "ВкусВилл", "Ашан", "Самокат", "Яндекс Лавка", "Ozon fresh"
)

/**
 * Ordinary FoodPlan experience. Three screens only: shopping, meals, settings.
 * Search/graph/SKU comparators remain internal engine services, not UI features.
 *
 * The app is still an engineering preview: real retailer completeness and the
 * independent M2 recipe evidence gate are not yet met.
 */
@Composable
fun FoodPlanHomeScreen(
    profile: FoodProfile,
    votes: Map<String, TasteRating>,
    pantryGrams: Map<String, Int> = emptyMap(),
    retailerOffers: List<RetailerOffer> = emptyList(),
    onSaveProfile: (FoodProfile) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onReviewTastes: () -> Unit = {},
    savedWeekSelection: SavedWeekSelection? = null,
    onSaveWeekSelection: (SavedWeekSelection) -> Unit = {},
    /** Clear only persisted weekly selections, never saved profile, taste votes or pantry. */
    onStartNewWeek: (((Boolean) -> Unit) -> Unit)? = null,
    onSaveRetailerOffer: (RetailerOffer) -> Unit = {}
) {
    var page by remember { mutableIntStateOf(0) }
    val planningFingerprint = profile.copy(
        budgetRub = "", city = "", stores = emptySet())
    val readyForPlanning = WeeklyBudget.parse(profile.budgetRub) != null
    var planned by remember(planningFingerprint, readyForPlanning, votes, pantryGrams) {
        val initial = SevenDayPlanner.plan(profile, votes, pantryGrams)
        mutableStateOf(SavedWeekPlanner.restore(profile, votes, pantryGrams,
            initial, savedWeekSelection))
    }
    var weekRevision by remember { mutableIntStateOf(0) }
    var newWeekSequence by remember { mutableIntStateOf(0) }
    var automaticMealChanges by remember { mutableIntStateOf(0) }
    fun commitWeekPlan(updated: PlanningResult.Success, automaticChanges: Int = 0) {
        weekRevision++
        automaticMealChanges = automaticChanges
        planned = updated
        onSaveWeekSelection(SavedWeekPlanner.capture(updated, profile, votes, pantryGrams))
    }
    var dayIndex by remember { mutableIntStateOf(0) }
    var selectedSlot by remember { mutableStateOf<Int?>(null) }
    var recipeEffort by remember { mutableStateOf(profile.cookingEffort) }
    var cookDays by remember { mutableIntStateOf(1) }
    var choosingAnother by remember { mutableStateOf(false) }
    var recipeProblem by remember { mutableStateOf<String?>(null) }
    var showNewWeekConfirmation by remember { mutableStateOf(false) }
    var resettingWeek by remember { mutableStateOf(false) }
    var newWeekError by remember { mutableStateOf<String?>(null) }

    // M4: anonymous public search directly on Android, independent from AI.
    // Name-based candidates only. No price, stock or allergy assertions.
    var showMagnitPrices by remember { mutableStateOf(false) }
    var publicSearchResult by remember { mutableStateOf<PublicSearchDiscovery?>(null) }
    var publicSearchLoading by remember { mutableStateOf(false) }
    var completedSearchKey by remember { mutableStateOf<String?>(null) }
    var lastSearchFinishedAtMs by remember { mutableLongStateOf(0L) }
    var lastSearchComplete by remember { mutableStateOf(false) }
    val currentShopping = (planned as? PlanningResult.Success)?.shopping.orEmpty()
    LaunchedEffect(page, profile.city, profile.stores, currentShopping) {
        val selectedStore = profile.stores.singleOrNull()
        val key = if (selectedStore == "ВкусВилл") {
            profile.city + "|" + currentShopping.filter { it.grams > 0 }
                .joinToString(";") { it.ingredient.id + ":" + it.grams }
        } else null
        if (page != 0) return@LaunchedEffect
        if (key == null || currentShopping.none { it.grams > 0 }) {
            publicSearchResult = null
            completedSearchKey = null
            lastSearchFinishedAtMs = 0L
            lastSearchComplete = false
            publicSearchLoading = false
            return@LaunchedEffect
        }
        if (!PublicSearchRefreshPolicy.shouldRescan(
                shoppingKey = key,
                previousKey = completedSearchKey,
                lastFinishedEpochMs = lastSearchFinishedAtMs,
                previousComplete = lastSearchComplete,
                nowEpochMs = System.currentTimeMillis()
            )) return@LaunchedEffect
        publicSearchResult = null
        publicSearchLoading = true
        try {
            val result = withContext(Dispatchers.IO) {
                VkusvillPublicSearchAdapter().scan(currentShopping)
            }
            publicSearchResult = result
            // Avoid repeated requests on every tab switch within a session.
            completedSearchKey = key
            lastSearchFinishedAtMs = System.currentTimeMillis()
            lastSearchComplete = result.complete
        } finally {
            publicSearchLoading = false
        }
    }


    // M3: deterministic, offline-friendly budget refinement without AI or an API
    // key. Only actual eligible, non-stale retailer observations permit changes.
    // User-selected meals / batch cooking are hard locks in BudgetWeekRefiner.
    // Run CPU-heavy multi-recipe search off the UI thread. A concurrent manual
    // change or new-week reset invalidates the in-flight result.
    LaunchedEffect(profile.budgetRub, profile.city, profile.stores,
        planningFingerprint, votes, pantryGrams, retailerOffers, newWeekSequence) {
        automaticMealChanges = 0
        val current = planned as? PlanningResult.Success ?: return@LaunchedEffect
        val store = profile.stores.singleOrNull() ?: return@LaunchedEffect
        if (retailerOffers.isEmpty() || WeeklyBudget.parse(profile.budgetRub) == null)
            return@LaunchedEffect
        val revisionAtStart = weekRevision
        val calculated = withContext(Dispatchers.Default) {
            BudgetWeekRefiner.refine(current, profile, votes, retailerOffers,
                store, System.currentTimeMillis(), pantryGrams)
        }
        if (calculated.numberOfMealChanges > 0 && weekRevision == revisionAtStart &&
            planned === current) {
            commitWeekPlan(calculated.plan, calculated.numberOfMealChanges)
        }
    }

    // The shopper enters the official store session from their ordinary
    // Purchases tab. Never route the released experience through the retired
    // WeeklyPlanScreen; the same saved shopping list is used on both screens.
    val activePlan = planned as? PlanningResult.Success
    if (showMagnitPrices && activePlan != null && profile.stores.singleOrNull() == "Магнит") {
        MagnitCatalogBrowser(
            shopping = activePlan.shopping,
            city = profile.city,
            budgetRub = WeeklyBudget.parse(profile.budgetRub)?.toLong() ?: 0L,
            onCaptured = onSaveRetailerOffer,
            onClose = { showMagnitPrices = false },
            strictAllergenVerification = profile.allergens.isNotBlank()
        )
        return
    }

    Column(Modifier.fillMaxSize()) {
        Text("FoodPlan AI", style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp))
        Text(when (page) {
            0 -> "Покупки на неделю"
            1 -> "Меню и рецепты"
            else -> "Ваши настройки"
        }, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp))

        Box(Modifier.weight(1f)) {
            when (page) {
                0 -> GroceriesHome(
                    profile, pantryGrams, retailerOffers, planned,
                    onSaveProfile, automaticMealChanges,
                    publicSearchResult, publicSearchLoading,
                    onOpenMagnitPrices = { showMagnitPrices = true }
                )
                1 -> {
                    when (val result = planned) {
                        is PlanningResult.Blocked -> {
                            Column(Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Пока не получилось составить меню",
                                    style = MaterialTheme.typography.titleMedium)
                                if (!readyForPlanning) {
                                    Text("Сначала выберите недельный бюджет в разделе «Продукты».")
                                    Button(onClick = { page = 0 }) {
                                        Text("Выбрать бюджет")
                                    }
                                } else {
                                    result.reasons.forEach { Text(it) }
                                    TextButton(onClick = { page = 2 }) {
                                        Text("Перейти в настройки")
                                    }
                                }
                            }
                        }
                        is PlanningResult.Success -> {
                            Column(Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    (0 until result.days.size).forEach { idx ->
                                        FilterChip(selected = dayIndex == idx,
                                            onClick = { dayIndex = idx },
                                            label = { Text("День ${idx + 1}") })
                                    }
                                }
                                Text("На ${profile.household} чел. · ${profile.mealsPerDay} приёма пищи в день",
                                    style = MaterialTheme.typography.bodySmall)
                                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    val day = result.days[dayIndex]
                                    day.meals.forEachIndexed { slot, meal ->
                                        OutlinedCard(Modifier.fillMaxWidth().testTag("home-meal-$slot").clickable {
                                            selectedSlot = slot
                                            recipeEffort = meal.selectedEffort ?: profile.cookingEffort
                                            cookDays = meal.batchDays.takeIf {
                                                meal.batchStartDay == dayIndex
                                            } ?: 1
                                            choosingAnother = false
                                            recipeProblem = null
                                        }) {
                                            Column(Modifier.padding(14.dp),
                                                verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                                Text(mealLabel(slot),
                                                    style = MaterialTheme.typography.labelLarge)
                                                Text(meal.recipe.name,
                                                    style = MaterialTheme.typography.titleMedium)
                                                Text("${meal.recipe.preparation.totalMinutes} мин · " +
                                                    "~${meal.nutrition.kcal.roundToInt()} ккал",
                                                    style = MaterialTheme.typography.bodySmall)
                                                if (meal.batchDays > 1) Text(
                                                    if (meal.batchStartDay == dayIndex)
                                                        "Приготовить сразу на ${meal.batchDays} дня"
                                                    else "Блюдо готовится заранее на ${meal.batchDays} дня",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary)
                                                Text("Рецепт · изменить блюдо · готовить на несколько дней",
                                                    style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                    Text("КБЖУ приблизительные. Для аллергий нужна проверка состава упаковки.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
                else -> {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        .padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Личные данные и питание",
                            style = MaterialTheme.typography.titleLarge)
                        Text("Цель: ${profile.goal}")
                        Text("Тип питания: ${profile.diet}")
                        Text("Аллергии: ${profile.allergens.ifBlank { "не указаны" }}")
                        Text("Не люблю: ${profile.disliked.ifBlank { "не указано" }}")
                        Text("Человек: ${profile.household}")
                        Text("Техника: ${profile.appliances.sorted().joinToString(", ")}")
                        Text("Оценено блюд: ${votes.size} из 25")
                        Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                            Text("Изменить настройки")
                        }
                        OutlinedButton(onClick = onReviewTastes,
                            modifier = Modifier.fillMaxWidth()) {
                            Text("Пересмотреть пищевые предпочтения")
                        }
                        HorizontalDivider()
                        Text("Новая неделя", style = MaterialTheme.typography.titleMedium)
                        Text("Можно составить исходное меню заново, сохранив профиль, оценки блюд, домашние продукты, бюджет и выбранный магазин.",
                            style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(
                            enabled = !resettingWeek && onStartNewWeek != null,
                            modifier = Modifier.fillMaxWidth().testTag("home-start-new-week"),
                            onClick = {
                                newWeekError = null
                                showNewWeekConfirmation = true
                            }
                        ) { Text("Начать новую неделю") }
                        newWeekError?.let { message ->
                            Text(message, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error)
                        }
                        Text("Параметры сохраняются на телефоне. Изменения применяются к меню и продуктам.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        NavigationBar {
            listOf("Продукты", "Рецепты", "Настройки").forEachIndexed { idx, label ->
                NavigationBarItem(
                    selected = page == idx,
                    onClick = { page = idx },
                    icon = { Text(listOf("🛒","🍽","⚙")[idx]) },
                    label = { Text(label) },
                    alwaysShowLabel = true
                )
            }
        }
    }

    if (showNewWeekConfirmation) {
        AlertDialog(
            onDismissRequest = {
                if (!resettingWeek) showNewWeekConfirmation = false
            },
            title = { Text("Начать новую неделю?") },
            text = { Text("Заменённые блюда и настройки приготовления для текущей недели будут сброшены. " +
                "Новое меню на семь дней будет рассчитано из ваших настроек. " +
                "Профиль, вкусы, запасы продуктов, бюджет и магазин останутся.") },
            confirmButton = {
                Button(
                    enabled = !resettingWeek && onStartNewWeek != null,
                    onClick = {
                        resettingWeek = true
                        newWeekError = null
                        onStartNewWeek?.invoke { persisted ->
                            resettingWeek = false
                            if (persisted) {
                                // Clear in memory only after DataStore confirms deletion.
                                planned = SevenDayPlanner.plan(profile, votes, pantryGrams)
                                weekRevision++
                                newWeekSequence++
                                automaticMealChanges = 0
                                dayIndex = 0
                                selectedSlot = null
                                showNewWeekConfirmation = false
                                recipeProblem = null
                                page = 1
                            } else {
                                showNewWeekConfirmation = false
                                newWeekError = "Не получилось очистить предыдущую неделю. Данные сохранены; попробуйте ещё раз."
                            }
                        }
                    }
                ) {
                    Text(if (resettingWeek) "Сохраняем изменения" else "Начать заново")
                }
            },
            dismissButton = {
                TextButton(enabled = !resettingWeek,
                    onClick = { showNewWeekConfirmation = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    val slot = selectedSlot
    val current = planned as? PlanningResult.Success
    if (slot != null && current != null) {
        val meal = current.days.getOrNull(dayIndex)?.meals?.getOrNull(slot)
        if (meal != null) {
            AlertDialog(
                onDismissRequest = { selectedSlot = null },
                title = { Text(mealLabel(slot)) },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(if (choosingAnother) "Выберите другое блюдо" else meal.recipe.name,
                            style = MaterialTheme.typography.titleMedium)
                        Text("Сколько усилий хотите потратить?",
                            style = MaterialTheme.typography.labelLarge)
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CookingEffort.entries.forEach { level ->
                                FilterChip(selected = recipeEffort == level,
                                    onClick = { recipeEffort = level },
                                    label = { Text(level.title) })
                            }
                        }
                        Text("Готовить на сколько дней?",
                            style = MaterialTheme.typography.labelLarge)
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            (1..(current.days.size - dayIndex)).forEach { count ->
                                FilterChip(selected = cookDays == count,
                                    onClick = { cookDays = count },
                                    label = { Text("$count дн.") })
                            }
                        }
                        Text("Выбрано: готовить на $cookDays дн.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary)
                        Text("На ${profile.household} чел. Потребность в продуктах пересчитается.",
                            style = MaterialTheme.typography.bodySmall)
                        if (!choosingAnother) {
                            Text("Ингредиенты на 1 человека до приготовления:",
                                style = MaterialTheme.typography.labelLarge)
                            meal.recipe.ingredients.forEach { part ->
                                Text("• ${OfflineRecipes.ingredients[part.id]?.name ?: part.id}: " +
                                    "${(part.grams * meal.servings).roundToInt()} г",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            Text("Приготовление:", style = MaterialTheme.typography.labelLarge)
                            meal.recipe.steps.forEachIndexed { idx, step ->
                                Text("${idx + 1}. $step", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        if (choosingAnother) {
                            val choices = RecipeBatchPlanner.alternatives(profile, votes,
                                current, dayIndex, slot, recipeEffort)
                            if (choices.isEmpty()) Text(
                                "Нет других блюд с выбранными ограничениями.",
                                style = MaterialTheme.typography.bodySmall)
                            choices.forEach { choice ->
                                TextButton(modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        val updated = RecipeBatchPlanner.apply(profile,
                                            votes, current, dayIndex, slot, choice.id,
                                            cookDays, recipeEffort, pantryGrams)
                                        when (updated) {
                                            is PlanningResult.Success -> {
                                                commitWeekPlan(updated)
                                                selectedSlot = null
                                            }
                                            is PlanningResult.Blocked ->
                                                recipeProblem = updated.reasons.joinToString("; ")
                                        }
                                    }) {
                                    Text(choice.name)
                                }
                            }
                        } else {
                            OutlinedButton(onClick = {
                                choosingAnother = true
                                recipeProblem = null
                            }) { Text("Другое блюдо") }
                        }
                        recipeProblem?.let { Text(it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall) }
                        Text("Приготовленное заранее блюдо храните по правилам пищевой безопасности.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                },
                confirmButton = {
                    if (choosingAnother) {
                        TextButton(onClick = { choosingAnother = false }) {
                            Text("К рецепту")
                        }
                    } else {
                        Button(onClick = {
                            val updated = RecipeBatchPlanner.apply(profile,votes,
                                current,dayIndex,slot,meal.recipe.id,cookDays,recipeEffort,pantryGrams)
                            when (updated) {
                                is PlanningResult.Success -> {
                                    commitWeekPlan(updated)
                                    selectedSlot = null
                                }
                                is PlanningResult.Blocked ->
                                    recipeProblem = updated.reasons.joinToString("; ")
                            }
                        }) { Text("Сохранить") }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedSlot = null }) { Text("Закрыть") }
                }
            )
        }
    }
}

private fun mealLabel(index: Int): String = when(index) {
    0 -> "Завтрак"
    1 -> "Обед"
    2 -> "Ужин"
    3 -> "Полдник"
    else -> "Приём пищи ${index + 1}"
}

@Composable
private fun GroceriesHome(
    profile: FoodProfile,
    pantryGrams: Map<String, Int>,
    retailerOffers: List<RetailerOffer>,
    plan: PlanningResult,
    onSaveProfile: (FoodProfile) -> Unit,
    automaticMealChanges: Int = 0,
    publicSearchResult: PublicSearchDiscovery? = null,
    publicSearchLoading: Boolean = false,
    onOpenMagnitPrices: () -> Unit = {}
) {
    var budgetInput by remember(profile.budgetRub) { mutableStateOf(profile.budgetRub) }
    var cityInput by remember(profile.city) { mutableStateOf(profile.city) }
    var shop by remember(profile.stores) {
        mutableStateOf(profile.stores.sorted().firstOrNull() ?: "Пятёрочка")
    }
    val chosenReady = WeeklyBudget.parse(budgetInput) != null &&
        cityInput.trim().length >= 2 && shop.isNotBlank()
    val profileMatches = profile.budgetRub == budgetInput &&
        profile.city == cityInput.trim() && profile.stores == setOf(shop)
    val canReadPlan = chosenReady && profileMatches

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Бюджет на неделю", style = MaterialTheme.typography.titleMedium)
        val amount = WeeklyBudget.parse(budgetInput)
        Text(if (amount == null) "Укажите сумму" else "$amount ₽",
            style = MaterialTheme.typography.headlineSmall)
        Slider(
            value = (budgetInput.toFloatOrNull() ?: 100f).coerceIn(100f,100000f),
            onValueChange = { budgetInput = it.roundToInt().coerceIn(100,100000).toString() },
            valueRange = 100f..100000f,
            modifier = Modifier.fillMaxWidth().testTag("home-weekly-budget-slider")
        )
        Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceBetween) {
            Text("100 ₽", style = MaterialTheme.typography.labelSmall)
            Text("100 000 ₽", style = MaterialTheme.typography.labelSmall)
        }
        OutlinedTextField(
            value = budgetInput, onValueChange = { text ->
                if (text.isEmpty() || text.length <= 6 && text.all(Char::isDigit))
                    budgetInput = text
            },
            label = { Text("Точная сумма, ₽") },
            modifier = Modifier.fillMaxWidth().testTag("home-weekly-budget-exact-input"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = budgetInput.isNotBlank() && amount == null,
            singleLine = true
        )
        OutlinedTextField(value = cityInput, onValueChange = { cityInput = it },
            label = { Text("Город") }, singleLine = true,
            modifier = Modifier.fillMaxWidth())
        Text("Магазин", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            groceryStores.forEach { name ->
                FilterChip(selected = shop == name,
                    onClick = { shop = name },
                    label = { Text(name) })
            }
        }
        Button(
            enabled = chosenReady,
            onClick = { if (chosenReady) onSaveProfile(profile.copy(
                budgetRub = budgetInput, city = cityInput.trim(), stores = setOf(shop))) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Собрать список на неделю") }

        if (!canReadPlan) {
            Text("Выберите бюджет и магазин, затем соберите список продуктов.",
                style = MaterialTheme.typography.bodyMedium)
            return@Column
        }
        when (plan) {
            is PlanningResult.Blocked -> {
                Text("Пока невозможно составить безопасное меню:",
                    style = MaterialTheme.typography.titleMedium)
                plan.reasons.forEach { Text(it) }
            }
            is PlanningResult.Success -> {
                if (shop == "Магнит") {
                    Button(onClick = onOpenMagnitPrices,
                        modifier = Modifier.fillMaxWidth()
                            .testTag("home-magnit-prices")) {
                        Text("Обновить цены по моему списку")
                    }
                    Text("Примерные цены по сети Магнит. Адрес магазина указывать не нужно.",
                        style = MaterialTheme.typography.bodySmall)
                }
                val quotes = RetailerBasketPricing.estimate(
                    plan.shopping, retailerOffers, profile.city, shop,
                    profile.budgetRub, System.currentTimeMillis(),
                    indicativePriceOnly = true
                )
                if (shop == "ВкусВилл" && publicSearchLoading) {
                    Text("Подбираем варианты из каталога ВкусВилла. " +
                        "Цена и наличие пока неизвестны.",
                        style = MaterialTheme.typography.bodySmall)
                } else if (shop == "ВкусВилл" && publicSearchResult?.message != null) {
                    Text(publicSearchResult?.message.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                }
                Text("Продукты на 7 дней", style = MaterialTheme.typography.titleLarge)
                if (automaticMealChanges > 0) {
                    Text("Меню автоматически скорректировано под бюджет: " +
                        "$automaticMealChanges замен блюд по наблюдаемым ценам.",
                        modifier = Modifier.testTag("home-budget-auto-refined"),
                        style = MaterialTheme.typography.bodySmall)
                }
                Text("На ${profile.household} чел. · ${plan.shopping.count { it.grams > 0 }} продуктов",
                    style = MaterialTheme.typography.bodyMedium)
                if (quotes.missingLines > 0) {
                    Text("Для ${quotes.missingLines} позиций пока нет примерной цены в каталоге. " +
                        "Стоимость всей корзины неизвестна.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                    if (quotes.knownSubtotalExceedsBudget)
                        Text("Даже известные цены уже превышают ваш бюджет.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("Предварительная стоимость: " +
                        "${ManualBasketPrices.formatKopeks(quotes.knownTotalKopeks)} ₽",
                        style = MaterialTheme.typography.titleMedium)
                    if (quotes.budgetEstimate == false) Text(
                        "Корзина превышает бюджет. Попробуйте заменить блюда.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                }
                plan.shopping.filter { it.grams > 0 }.forEach { need ->
                    HorizontalDivider()
                    val line = quotes.lines.firstOrNull { it.ingredientId == need.ingredient.id }
                    Text(need.ingredient.name, style = MaterialTheme.typography.titleSmall)
                    Text("Нужно купить: ${need.grams} г",
                        style = MaterialTheme.typography.bodyMedium)
                    val pricedLine = line?.costKopeks
                    if (pricedLine != null) {
                        Text("${line.packages} уп. по ${line.chosenPackGrams} г · " +
                            "~${ManualBasketPrices.formatKopeks(pricedLine)} ₽",
                            style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text("Ориентир: ${need.packsToBuy} уп. · Цена пока неизвестна",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    if (shop == "ВкусВилл") {
                        val references = publicSearchResult?.candidates
                            ?.get(need.ingredient.id).orEmpty()
                        if (references.isNotEmpty()) {
                            Text("Похожие товары в каталоге (только по названию):",
                                style = MaterialTheme.typography.bodySmall)
                            references.take(2).forEach { reference ->
                                Text("• " + reference.title + " · цена неизвестна",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                if (quotes.budgetEstimate != true) {
                    val manualPrices = emptyMap<String,Long>()
                    val fallback = LimitedBudgetFoodPlanner.choose(profile,shop,
                        retailerOffers,manualPrices,System.currentTimeMillis(),
                        plan.calorieTargetEstimate)
                    if (fallback.status == LimitedFoodStatus.PARTIAL_AFFORDABLE_FOOD) {
                        HorizontalDivider()
                        Text("Что можно купить в пределах бюджета",
                            style = MaterialTheme.typography.titleMedium)
                        fallback.lines.forEach { item ->
                            Text("• ${item.productName}: ${item.packages} уп. · " +
                                "${ManualBasketPrices.formatKopeks(item.totalKopeks)} ₽")
                        }
                        Text("Это частичный набор продуктов, не полноценный рацион на 7 дней.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                    } else if (quotes.missingLines > 0) {
                        Text("Пока нет достаточных актуальных цен, чтобы показать " +
                            "достоверную доступную корзину даже на небольшой бюджет.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text("Это примерные цены сети, чек может отличаться. " +
                    "При пищевых ограничениях проверяйте состав упаковок.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
