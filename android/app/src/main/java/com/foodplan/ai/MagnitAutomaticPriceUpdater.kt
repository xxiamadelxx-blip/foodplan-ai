package com.foodplan.ai

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import com.foodplan.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Actual on-device automatic reading of public retailer HTML, by known SKU.
 * No WebView workspace, manual scan, external AI or GitHub runtime dependency.
 * Runs only when Products is selected by FoodPlanHomeScreen.
 * Official robots directives and source access denials are respected.
 */
@Composable
internal fun MagnitAutomaticPriceUpdater(
    shopping: List<ShoppingNeed>,
    city: String,
    budgetRub: Long,
    strictAllergenVerification: Boolean,
    onProgress: (String) -> Unit,
    onObserved: (RetailWeekQuote, List<String>) -> Unit,
    onMassOffers: (List<RetailerOffer>) -> Unit,
    onEvidence: (List<RetailProductEvidence>) -> Unit = {},
    scanSource: suspend (List<ShoppingNeed>, String) -> MagnitAutoPriceResult =
        { needs, chosenCity -> MagnitAutomaticPriceAdapter().scan(needs, chosenCity) }
) {
    val latestProgress = rememberUpdatedState(onProgress)
    val latestObserved = rememberUpdatedState(onObserved)
    val latestMassOffers = rememberUpdatedState(onMassOffers)
    val latestEvidence = rememberUpdatedState(onEvidence)
    val latestScanSource = rememberUpdatedState(scanSource)

    // The physical pages depend on ingredient IDs, not on changing quantities
    // or budget. Preserve a completed HTTP observation separately from quotes.
    val ingredientKey = shopping.filter { it.grams > 0 }
        .map { it.ingredient.id }.distinct().sorted().joinToString(";")
    val quantitiesKey = shopping.filter { it.grams > 0 }
        .map { "${it.ingredient.id}:${it.grams}" }.sorted().joinToString(";")
    var scanned by remember(ingredientKey, city) {
        mutableStateOf<MagnitAutoPriceResult?>(null)
    }
    LaunchedEffect(ingredientKey, city) {
        latestProgress.value("Обновляем цены из открытого каталога Магнита")
        val result = try {
            latestScanSource.value(shopping, city)
        } catch (e: CancellationException) {
            // Changing the ingredient set cancels the pending pass; no stale
            // retailer observations may be published for the new basket.
            throw e
        } catch (_: Exception) {
            val ids = shopping.filter { it.grams > 0 }.map { it.ingredient.id }.toSet()
            MagnitAutoPriceResult(emptyList(), ids.size, ids,
                "Источник цен не ответил. Потребность в продуктах сохранена.", true)
        }
        scanned = result
    }

    // A result can arrive after a user changes grams/budget while a request is
    // suspended. Quote in a separate reactive effect against current Compose
    // inputs, never against the shopping captured when the request began.
    // Once scanned, later changes to the same IDs NEVER issue another GET.
    LaunchedEffect(scanned, quantitiesKey, budgetRub,
        strictAllergenVerification, ingredientKey, city) {
        val result = scanned ?: return@LaunchedEffect
        val currentShopping = shopping
        val now = System.currentTimeMillis()
        val quote = RetailWeekQuoter.build(
            AnonymousRetailPlanner.build(currentShopping),
            result.observations, "Магнит", city,
            budgetRub, now, strictAllergenVerification)
        val failures = result.unavailableIngredientIds.map {
            ShoppingDisplayNames.forIngredient(it)
        }.let { missing ->
            if (result.sourceAccessBlocked)
                listOf(result.message ?: "Магазин ограничил доступ") + missing
            else missing
        }
        if (!strictAllergenVerification) {
            val massOffers = AutomaticRetailScanPolicy.verifiedMassOffers(
                currentShopping, result.observations, city, budgetRub, now)
            if (massOffers.isNotEmpty()) latestMassOffers.value(massOffers)
        }
        // Save source facts before the parent can unmount this scanner. Parent
        // will re-quote the observations after subsequent recipe changes.
        latestEvidence.value(result.observations)
        latestObserved.value(quote, failures)
    }
}
