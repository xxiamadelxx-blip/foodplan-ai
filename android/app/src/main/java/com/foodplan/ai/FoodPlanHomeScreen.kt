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
    /** Persisted user favorite IDs, separate from general taste-survey tags. */
    favoriteRecipeIds: Set<String> = emptySet(),
    onSetRecipeFavorite: (String, Boolean) -> Unit = { _, _ -> },
    /** Clear only persisted weekly selections, never saved profile, taste votes or pantry. */
    onStartNewWeek: (((Boolean) -> Unit) -> Unit)? = null,
    onSaveRetailerOffer: (RetailerOffer) -> Unit = {},
    /** Allows isolated UI instrumentation with no public network request. */
    automaticCatalogReadingEnabled: Boolean = true,
    /** Injectable only for isolated UI tests; ordinary users always get
     * the real built-in HTTP/robots adapter, no manual catalog action. */
    automaticPriceSource: suspend (List<ShoppingNeed>, String) -> MagnitAutoPriceResult =
        { needs, selectedCity -> MagnitAutomaticPriceAdapter().scan(needs, selectedCity) }
) {
    var page by remember { mutableIntStateOf(0) }
    val planningFingerprint = profile.copy(
        budgetRub = "", city = "", stores = emptySet())
    val readyForPlanning = WeeklyBudget.parse(profile.budgetRub) != null
    var planned by remember(planningFingerprint, readyForPlanning, votes, pantryGrams) {
        val initial = RecipeConstructor.constructWeek(profile, votes, pantryGrams)
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
    var showAllReplacementProducts by remember { mutableStateOf(false) }
    var selectedReplacementProducts by remember { mutableStateOf<Set<String>>(emptySet()) }
    var recipeProblem by remember { mutableStateOf<String?>(null) }
    var showNewWeekConfirmation by remember { mutableStateOf(false) }
    var resettingWeek by remember { mutableStateOf(false) }
    var newWeekError by remember { mutableStateOf<String?>(null) }

    // M4: anonymous public search directly on Android, independent from AI.
    // Name-based candidates only. No price, stock or allergy assertions.
    // Price collection is part of ordinary Products, never a separate
    // manual catalog workspace. Only source-confirmed offers enter DataStore.
    var autoObservations by remember { mutableStateOf<List<RetailProductEvidence>>(emptyList()) }
    var autoQuoteKey by remember { mutableStateOf<String?>(null) }
    var autoRunning by remember { mutableStateOf(false) }
    var autoFinished by remember { mutableStateOf(false) }
    var autoStatus by remember { mutableStateOf("") }
    var activeAutoKey by remember { mutableStateOf<String?>(null) }
    val attemptedAutoKeys = remember { mutableStateListOf<String>() }
    var publicSearchResult by remember { mutableStateOf<PublicSearchDiscovery?>(null) }
    var publicSearchLoading by remember { mutableStateOf(false) }
    var completedSearchKey by remember { mutableStateOf<String?>(null) }
    var lastSearchFinishedAtMs by remember { mutableLongStateOf(0L) }
    var lastSearchComplete by remember { mutableStateOf(false) }
    val currentShopping = (planned as? PlanningResult.Success)?.shopping.orEmpty()
    val automaticShoppingKey = AutomaticRetailScanPolicy.shoppingKey(
        profile.city, currentShopping
    )
    // After the network scan finishes the noninteractive scanner unmounts.
    // Rebuild the quoted counts and totals from observed source facts whenever
    // the current recipe quantities or exact weekly budget change. No GET.
    val autoQuote = if (autoQuoteKey != null &&
        autoQuoteKey == automaticShoppingKey &&
        profile.stores.singleOrNull() == "Магнит") {
        RetailWeekQuoter.build(
            AnonymousRetailPlanner.build(currentShopping), autoObservations,
            "Магнит", profile.city,
            WeeklyBudget.parse(profile.budgetRub)?.toLong() ?: 100L,
            System.currentTimeMillis(), profile.allergens.isNotBlank()
        )
    } else null
    LaunchedEffect(page, profile.stores, automaticShoppingKey,
        automaticCatalogReadingEnabled) {
        if (profile.stores.singleOrNull() != "Магнит" ||
            !automaticCatalogReadingEnabled) {
            activeAutoKey = null
            autoRunning = false
            return@LaunchedEffect
        }
        // Once started, keep the coroutine alive when the user opens Recipes
        // or Settings. Changing tabs is not a cancelled shopping request.
        if (page != 0) return@LaunchedEffect
        if (AutomaticRetailScanPolicy.canStart(
                profile.stores.singleOrNull(), true, automaticShoppingKey,
                attemptedAutoKeys.toSet(), attemptedAutoKeys.size)) {
            val key = requireNotNull(automaticShoppingKey)
            // Only a finished source pass consumes a completed-key slot.
            // A canceled pass must retry automatically next time.
            activeAutoKey = key
            autoQuoteKey = null
            autoFinished = false
            autoRunning = true
            autoStatus = "Обновляем цены по вашему списку из каталога Магнита"
        } else if (activeAutoKey != automaticShoppingKey) {
            activeAutoKey = null
        }
    }
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

    // Automatic on-device parser; a different week may require different
    // actual generic ingredient IDs. Never expose the browser as a fourth page.
    val activePlan = planned as? PlanningResult.Success

    Column(Modifier.fillMaxSize()) {
        val activeKey = activeAutoKey
        if (activeKey != null && activeKey == automaticShoppingKey &&
            activePlan != null && profile.stores.singleOrNull() == "Магнит") {
            key(activeKey) {
                MagnitAutomaticPriceUpdater(
                    shopping = activePlan.shopping,
                    city = profile.city,
                    budgetRub = WeeklyBudget.parse(profile.budgetRub)?.toLong() ?: 100L,
                    strictAllergenVerification = profile.allergens.isNotBlank(),
                    onProgress = { autoStatus = it },
                    onObserved = { quote, problems ->
                        // Only completed scans are remembered. Switching
                        // tabs or changing the actual ingredient set mid-scan
                        // cannot permanently suppress the next automatic read.
                        if (activeAutoKey == activeKey &&
                            activeKey !in attemptedAutoKeys) {
                            attemptedAutoKeys += activeKey
                        }
                        // Published prices cannot guarantee SKU labels/allergens.
                        autoQuoteKey = automaticShoppingKey
                        autoRunning = false
                        autoFinished = true
                        // Release the noninteractive browser as soon as the
                        // scan finishes; results remain in the ordinary basket.
                        activeAutoKey = null
                        autoStatus = if (quote.pricedIngredientCount > 0)
                            "Цены обновлены автоматически: " +
                                "${quote.pricedIngredientCount} из " +
                                "${quote.requiredIngredientCount} продуктов"
                        else if (problems.isNotEmpty())
                            "Каталог магазина сейчас недоступен. Цены неизвестны."
                        else "Не получилось прочитать цены. Повторим при новом списке."
                    },
                    onMassOffers = { fresh ->
                        fresh.forEach(onSaveRetailerOffer)
                    },
                    onEvidence = { fresh -> autoObservations = fresh },
                    scanSource = automaticPriceSource
                )
            }
        }
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
                    autoQuote = autoQuote.takeIf {
                        profile.stores.singleOrNull() == "Магнит" &&
                            autoQuoteKey == automaticShoppingKey
                    },
                    autoRunning = autoRunning,
                    autoFinished = autoFinished,
                    autoStatus = autoStatus,
                    onOpenRecipes = { page = 1 }
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
                                            selectedReplacementProducts = emptySet()
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
                                planned = RecipeConstructor.constructWeek(profile, votes, pantryGrams)
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
                title = { Text(if (choosingAnother) "Продукты текущей недели"
                    else mealLabel(slot)) },
                text = {
                    key(choosingAnother) {
                    Column(Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(if (choosingAnother) "Выберите другое блюдо" else meal.recipe.name,
                            style = MaterialTheme.typography.titleMedium)
                        if (choosingAnother) {
                            val existingProducts = current.shopping.filter { it.requiredGrams > 0 }
                            Text("Выберите продукты для нового рецепта:",
                                style = MaterialTheme.typography.labelLarge)
                            existingProducts.take(if (showAllReplacementProducts)
                                existingProducts.size else 4).forEach { item ->
                                val ingredientId = item.ingredient.id
                                FilterChip(
                                    selected = ingredientId in selectedReplacementProducts,
                                    onClick = {
                                        selectedReplacementProducts =
                                            if (ingredientId in selectedReplacementProducts)
                                                selectedReplacementProducts - ingredientId
                                            else selectedReplacementProducts + ingredientId
                                    },
                                    label = { Text(ShoppingDisplayNames.forIngredient(ingredientId)) },
                                    modifier = Modifier.testTag("home-replacement-product-$ingredientId")
                                )
                            }
                            if (existingProducts.size > 4) {
                                TextButton(onClick = {
                                    showAllReplacementProducts = !showAllReplacementProducts
                                }) {
                                    Text(if (showAllReplacementProducts) "Свернуть продукты"
                                         else "Показать все продукты (${existingProducts.size})")
                                }
                            }
                            if (selectedReplacementProducts.isNotEmpty()) {
                                TextButton(onClick = {
                                    selectedReplacementProducts = emptySet()
                                }) { Text("Сбросить выбор продуктов") }
                            }
                            Text(if (selectedReplacementProducts.isEmpty())
                                "Подбираем из всех продуктов текущей недели, с учётом диеты, вкусов и новых покупок."
                                else "Подбираем только рецепты со связями с выбранными продуктами: ${selectedReplacementProducts.size}.",
                                style = MaterialTheme.typography.bodySmall)
                        }
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
                                Text("• ${ShoppingDisplayNames.forIngredient(part.id)}: " +
                                    "${(part.grams * meal.servings).roundToInt()} г",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            Text("Приготовление:", style = MaterialTheme.typography.labelLarge)
                            meal.recipe.steps.forEachIndexed { idx, step ->
                                Text("${idx + 1}. $step", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        if (choosingAnother) {
                            val previews = remember(current, profile, votes, pantryGrams,
                                retailerOffers, dayIndex, slot, recipeEffort, cookDays,
                                selectedReplacementProducts, favoriteRecipeIds) {
                                RecipeConstructor.suggestReplacements(
                                    profile, votes, current, dayIndex, slot, recipeEffort,
                                    cookingDays = cookDays, pantryGrams = pantryGrams,
                                    offers = retailerOffers, nowEpochMs = System.currentTimeMillis(),
                                    maxResults = 12,
                                    selectedIngredientIds = selectedReplacementProducts,
                                    favoriteRecipeIds = favoriteRecipeIds
                                )
                            }
                            if (previews.isEmpty()) Text(
                                "Нет других блюд с выбранными ограничениями.",
                                style = MaterialTheme.typography.bodySmall)
                            previews.forEach { option ->
                                Row(Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(modifier = Modifier.weight(1f)
                                    .testTag("home-replacement-${option.recipe.id}"),
                                    onClick = {
                                        val updated = RecipeBatchPlanner.apply(profile,
                                            votes, current, dayIndex, slot, option.recipe.id,
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
                                    Column(Modifier.fillMaxWidth()) {
                                        Text(option.recipe.name)
                                        val reusable = option.reusedIngredientIds.size
                                        val extras = option.newIngredientIds.size
                                        Text("Уже в списке: $reusable · новых продуктов: $extras",
                                            style = MaterialTheme.typography.bodySmall)
                                        if (extras > 0) Text(
                                            "Добавить: " + option.newIngredientIds.sorted()
                                                .joinToString(", ") { ShoppingDisplayNames.forIngredient(it) },
                                            style = MaterialTheme.typography.bodySmall)
                                        if (option.increasedPurchaseGrams.isNotEmpty()) Text(
                                            "Понадобится больше: " +
                                                option.increasedPurchaseGrams.keys.sorted()
                                                    .joinToString(", ") { ShoppingDisplayNames.forIngredient(it) },
                                            style = MaterialTheme.typography.bodySmall)
                                        Text("Стоимость полной недели: " + when(option.fitsBudget) {
                                            true -> "укладывается в бюджет по имеющимся ценам"
                                            false -> "выше бюджета по имеющимся ценам"
                                            null -> "неизвестна"
                                        }, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                TextButton(
                                    modifier = Modifier.testTag("home-favorite-${option.recipe.id}"),
                                    onClick = {
                                        onSetRecipeFavorite(
                                            option.recipe.id,
                                            option.recipe.id !in favoriteRecipeIds
                                        )
                                    }
                                ) {
                                    Text(if (option.recipe.id in favoriteRecipeIds) "♥" else "♡",
                                        style = MaterialTheme.typography.titleLarge)
                                }
                                }
                            }
                        } else {
                            OutlinedButton(onClick = {
                                choosingAnother = true
                                showAllReplacementProducts = false
                                selectedReplacementProducts = emptySet()
                                recipeProblem = null
                            }) { Text("Другое блюдо") }
                        }
                        recipeProblem?.let { Text(it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall) }
                        Text("Приготовленное заранее блюдо храните по правилам пищевой безопасности.",
                            style = MaterialTheme.typography.bodySmall)
                    }
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
    autoQuote: RetailWeekQuote? = null,
    autoRunning: Boolean = false,
    autoFinished: Boolean = false,
    autoStatus: String = "",
    onOpenRecipes: () -> Unit = {}
) {
    var editingShoppingChoices by remember {
        mutableStateOf(WeeklyBudget.parse(profile.budgetRub) == null)
    }
    LaunchedEffect(profile.budgetRub, profile.city, profile.stores) {
        // After the first save, start on the actual groceries instead of
        // forcing users through the configuration form every visit.
        if (WeeklyBudget.parse(profile.budgetRub) != null) {
            editingShoppingChoices = false
        }
    }
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
        if (!editingShoppingChoices && canReadPlan) {
            Text("На неделю: ${profile.budgetRub} ₽ · ${shop}",
                style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { editingShoppingChoices = true },
                modifier = Modifier.testTag("home-edit-shopping")) {
                Text("Изменить бюджет и магазин")
            }
        } else {
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
        if (WeeklyBudget.parse(profile.budgetRub) != null && profileMatches) {
            TextButton(onClick = { editingShoppingChoices = false }) { Text("Отмена") }
        }
        }

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
                    Text(
                        if (autoRunning) "Получаем цены из каталога автоматически"
                        else if (autoFinished) autoStatus
                        else "Цены сети обновляются автоматически при составлении списка",
                        modifier = Modifier.testTag("home-auto-price-status"),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text("Примерные цены сети. Без адреса и подтверждения наличия.",
                        style = MaterialTheme.typography.bodySmall)
                } else if (shop == "ВкусВилл") {
                    Text("Открытый каталог ВкусВилла сейчас даёт только названия " +
                        "товаров. Автоматическое получение цен для этого магазина " +
                        "пока не подключено: сумма корзины неизвестна.",
                        modifier = Modifier.testTag("home-retailer-price-not-supported"),
                        style = MaterialTheme.typography.bodySmall)
                } else {
                    // Never take another shop's SKU/price just to fill the
                    // basket. Unsupported adapters are an honest UNKNOWN.
                    Text("Автоматическое чтение цен для магазина «$shop» пока " +
                        "не подключено. Список продуктов и рецепты доступны.",
                        modifier = Modifier.testTag("home-retailer-price-not-supported"),
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
                TextButton(onClick = onOpenRecipes,
                    modifier = Modifier.testTag("home-open-week-recipes")) {
                    Text("Посмотреть 7 дней и рецепты")
                }
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
                    Text(ShoppingDisplayNames.forIngredient(need.ingredient.id),
                        style = MaterialTheme.typography.titleSmall)
                    // A public 10-egg carton or 900mL bottle has a price,
                    // but never inherits a fictitious recipe-gram equivalence.
                    val unitQuote = autoQuote?.lines?.firstOrNull {
                        // A physical pack's observed unit price is
                        // independent of how many recipe grams are needed.
                        it.ingredientId == need.ingredient.id
                    }
                    val physicalPack = unitQuote?.observedPack
                    val physicalPackPrice = unitQuote?.observedPackPriceKopeks
                    val pricedLine = line?.costKopeks
                    val verifiedPackGrams = line?.chosenPackGrams
                    val verifiedPackCount = line?.packages
                    if (pricedLine != null && verifiedPackGrams != null &&
                        verifiedPackCount != null && verifiedPackCount > 0) {
                        // Shopping-first: real retailer pack count is primary.
                        Text("${verifiedPackCount} уп. по ${verifiedPackGrams} г · " +
                            "~${ManualBasketPrices.formatKopeks(pricedLine)} ₽",
                            style = MaterialTheme.typography.titleMedium)
                    } else if (physicalPack != null && physicalPackPrice != null) {
                        val (quantity, type) = when (physicalPack.unit) {
                            PurchaseUnit.PIECE -> "${physicalPack.quantity} шт." to "коробку"
                            PurchaseUnit.MILLILITRE -> "${physicalPack.quantity} мл" to "бутылку"
                            PurchaseUnit.GRAM -> "${physicalPack.quantity} г" to "упаковку"
                            PurchaseUnit.UNPEELED_GRAM ->
                                "${physicalPack.quantity} г с кожурой" to "упаковку"
                            PurchaseUnit.NET_WITH_LIQUID_GRAM ->
                                "${physicalPack.quantity} г с заливкой" to "банку"
                        }
                        Text("$quantity · ~" +
                            "${ManualBasketPrices.formatKopeks(physicalPackPrice)} ₽ " +
                            "за $type", style = MaterialTheme.typography.titleMedium)
                        Text("Количество для рецептов неизвестно без проверки массы.",
                            style = MaterialTheme.typography.bodySmall)
                    } else {
                        // Reference ingredient packGrams is NOT a verified SKU.
                        // Never portray its derived packsToBuy as actual purchases.
                        // Even offline without retailer prices, the weekly
                        // shopping list itself stays actionable. No invented
                        // retailer pack count or 0-ruble product is displayed.
                        Text("Купить ${need.grams} г",
                            style = MaterialTheme.typography.titleMedium)
                        Text("Фасовка уточняется · Цена магазина неизвестна",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    if (pricedLine != null && verifiedPackGrams != null &&
                        verifiedPackCount != null && verifiedPackCount > 0) {
                        Text("По рецептам: ${need.grams} г",
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
