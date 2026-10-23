package com.foodplan.ai

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    scanSource: suspend (List<ShoppingNeed>, String) -> MagnitAutoPriceResult =
        { needs, chosenCity -> MagnitAutomaticPriceAdapter().scan(needs, chosenCity) }
) {
    val latestProgress = rememberUpdatedState(onProgress)
    val latestObserved = rememberUpdatedState(onObserved)
    val latestMassOffers = rememberUpdatedState(onMassOffers)
    val latestScanSource = rememberUpdatedState(scanSource)

    LaunchedEffect(shopping, city, budgetRub, strictAllergenVerification) {
        latestProgress.value("Обновляем цены из открытого каталога Магнита")
        val result = try {
            latestScanSource.value(shopping, city)
        } catch (e: CancellationException) {
            // A user may move to another tab during a long public source scan.
            // Never report this cancelled, incomplete run as a finished price quote.
            throw e
        } catch (_: Exception) {
            MagnitAutoPriceResult(emptyList(), shopping.count { it.grams > 0 },
                shopping.filter { it.grams > 0 }.map { it.ingredient.id }.toSet(),
                "Источник цен не ответил. Потребность в продуктах сохранена.",
                true)
        }
        val queries = AnonymousRetailPlanner.build(shopping)
        val now = System.currentTimeMillis()
        val quote = RetailWeekQuoter.build(
            queries, result.observations, "Магнит", city,
            budgetRub, now, strictAllergenVerification)
        val failures = result.unavailableIngredientIds.map {
            ShoppingDisplayNames.forIngredient(it)
        }.let { missing ->
            if (result.sourceAccessBlocked)
                listOf(result.message ?: "Магазин ограничил доступ") + missing
            else missing
        }
        latestObserved.value(quote, failures)
        if (!strictAllergenVerification) {
            val massOffers = AutomaticRetailScanPolicy.verifiedMassOffers(
                shopping, result.observations, city, budgetRub, now)
            if (massOffers.isNotEmpty()) latestMassOffers.value(massOffers)
        }
    }
}
