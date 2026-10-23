package com.foodplan.ai

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.createComposeRule
import com.foodplan.domain.*
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/** Integration of on-device catalog observations with a changing live basket.
 * No CI retail GET or AI; the production composable is driven by injected
 * official-SKU fixture evidence.
 */
class MagnitAutomaticPriceUpdaterTest {
    @get:Rule val rule = createComposeRule()

    private fun need(grams: Int) =
        ShoppingNeed(OfflineRecipes.ingredients.getValue("rice"), grams, 2,
            grams, 0)

    @Test fun changingGramsAndBudgetRequotesOneScanWithoutSecondNetworkPass() {
        val scanned = AtomicInteger(0)
        val recomposedGrams = AtomicInteger(0)
        val sourceReady = CompletableDeferred<MagnitAutoPriceResult>()
        val mostRecentQuote = AtomicReference<RetailWeekQuote?>(null)
        val offers = AtomicReference<List<RetailerOffer>>(emptyList())
        val requestedGrams = mutableIntStateOf(901)
        val selectedBudget = mutableIntStateOf(356)
        val city = "Санкт-Петербург"
        val source: suspend (List<ShoppingNeed>, String) -> MagnitAutoPriceResult =
            { shopping, requestedCity ->
                assertEquals(city, requestedCity)
                assertEquals("rice", shopping.single().ingredient.id)
                scanned.incrementAndGet()
                sourceReady.await()
            }

        rule.setContent {
            MaterialTheme {
                val visibleGrams = requestedGrams.intValue
                SideEffect { recomposedGrams.set(visibleGrams) }
                MagnitAutomaticPriceUpdater(
                    shopping = listOf(need(visibleGrams)),
                    city = city,
                    budgetRub = selectedBudget.intValue.toLong(),
                    strictAllergenVerification = false,
                    onProgress = {},
                    onObserved = { quote, _ -> mostRecentQuote.set(quote) },
                    onMassOffers = { value -> offers.set(value) },
                    scanSource = source
                )
            }
        }
        rule.waitUntil(timeoutMillis = 10_000) { scanned.get() == 1 }
        rule.runOnIdle {
            // 901g initially needs 2 bags. Change the exact same SKU to
            // 1601g (3 bags), and change the budget while GET is pending.
            requestedGrams.intValue = 1601
            selectedBudget.intValue = 100
        }
        // Compose state propagation is asynchronous. Do not let the mocked
        // HTTP request complete before the parent has actually recomposed.
        // Otherwise this test checks a race it accidentally manufactured,
        // not whether a real screen re-uses the same public SKU price.
        rule.waitUntil(timeoutMillis = 10_000) { recomposedGrams.get() == 1601 }
        rule.waitForIdle()
        val product = ObservedProduct(
            "Рис длиннозерный 800г",
            requireNotNull(MagnitKnownProductPaths.urlFor("rice")), 800, 4999L)
        sourceReady.complete(MagnitAutoPriceResult(
            listOf(RetailProductEvidence(product,"Магнит",city,
                System.currentTimeMillis(),false)),
            1,emptySet(),null,false
        ))
        rule.waitUntil(timeoutMillis = 10_000) { mostRecentQuote.get() != null }
        rule.runOnIdle {
            val current = requireNotNull(mostRecentQuote.get())
            assertEquals(1, scanned.get())
            assertEquals(1601, current.lines.single().requiredGrams)
            assertEquals(3L, current.lines.single().packages)
            assertEquals(14997L, current.lines.single().estimatedCostKopeks)
            assertEquals(false, current.estimatedWithinBudget)
            assertEquals(1, offers.get().size)
            assertEquals(800, offers.get().single().packGrams)
        }
    }
}
