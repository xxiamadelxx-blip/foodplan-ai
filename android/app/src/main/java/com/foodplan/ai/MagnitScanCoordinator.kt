package com.foodplan.ai

import android.webkit.WebView
import com.foodplan.domain.*
import org.json.JSONObject

/**
 * Bounded user-initiated browsing in the official Magnit WebView session.
 * No unauthenticated hidden endpoints, anti-bot evasion or paid AI.
 * Search forms and DOM selectors are site-dependent: failed stages are reported.
 */
class MagnitScanCoordinator(
    private val web: WebView,
    private val queries: List<AnonymousRetailQuery>,
    private val city: String,
    private val addressConfirmed: Boolean,
    private val onProgress: (String) -> Unit,
    private val onCompleted: (List<RetailProductEvidence>, List<String>) -> Unit
) {
    private enum class State { IDLE, OPEN_SEARCH, SEARCH_RESULT, LISTING, DETAIL, FINISHED }
    private var state = State.IDLE
    private var cursor = 0
    private var pending = emptyList<ObservedProduct>()
    private var detailCursor = 0
    private val accumulated = mutableListOf<RetailProductEvidence>()
    private val failures = mutableListOf<String>()
    private var serial = 0
    private val knownCategoryUrls = mapOf(
        "chicken" to "https://magnit.ru/catalog/64291-testmmkuritsa",
        "rice" to "https://magnit.ru/catalog/64143-testmmris"
    )

    fun begin() {
        require(addressConfirmed) { "Select store delivery address before scanning" }
        if (state != State.IDLE) return
        advance()
    }

    fun cancel() {
        state = State.FINISHED
        serial++
        onProgress("Автопрогон остановлен пользователем")
    }

    fun onPageFinished(url: String?) {
        if (state == State.FINISHED || !url.orEmpty().startsWith("https://magnit.ru/")) return
        val token = serial
        when (state) {
            State.OPEN_SEARCH -> web.postDelayed({
                if (state == State.OPEN_SEARCH && token == serial) runSearch()
            }, 1300)
            State.LISTING, State.SEARCH_RESULT -> web.postDelayed({
                if (token == serial && state in setOf(State.LISTING, State.SEARCH_RESULT))
                    readListing()
            }, 1600)
            State.DETAIL -> web.postDelayed({
                if (state == State.DETAIL && token == serial) readDetails()
            }, 1300)
            else -> Unit
        }
    }

    private fun advance() {
        serial++
        if (cursor >= queries.size) {
            state = State.FINISHED
            onProgress("Прогон завершён: ${queries.size - failures.size}/${queries.size} поисковых категорий; результаты требуют проверки.")
            onCompleted(accumulated.toList(), failures.toList())
            return
        }
        pending = emptyList()
        detailCursor = 0
        val query = queries[cursor]
        onProgress("Поиск ${cursor + 1}/${queries.size}: ${query.genericName}")
        val category = knownCategoryUrls[query.ingredientId]
        if (category != null) {
            state = State.LISTING
            web.loadUrl(category)
        } else {
            state = State.OPEN_SEARCH
            web.loadUrl("https://magnit.ru/")
        }
    }

    private fun runSearch() {
        val term = queries[cursor].searchTerms.first()
        // Strictly interact with the page's ordinary visible search form.
        val script = """
            (function () {
                const term = ${JSONObject.quote(term)};
                const input = Array.from(document.querySelectorAll('input'))
                    .find(el => el.offsetParent !== null &&
                       (el.type === 'search' || /поиск|найти|искать/i.test(
                            (el.placeholder || '') + ' ' + (el.getAttribute('aria-label') || '')
                       )));
                if (!input) return 'NO_SEARCH';
                const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')?.set;
                if (setter) setter.call(input, term); else input.value = term;
                input.dispatchEvent(new Event('input',{bubbles:true}));
                input.dispatchEvent(new Event('change',{bubbles:true}));
                const form = input.closest('form');
                if (form && form.requestSubmit) form.requestSubmit();
                else input.dispatchEvent(new KeyboardEvent('keydown', {
                    key:'Enter', code:'Enter', bubbles:true
                }));
                return 'SEARCH_SENT';
            })()
        """.trimIndent()
        state = State.SEARCH_RESULT
        serial++
        val token = serial
        web.evaluateJavascript(script) { result ->
            if (token != serial || state != State.SEARCH_RESULT) return@evaluateJavascript
            if (!result.orEmpty().contains("SEARCH_SENT")) {
                failures += "${queries[cursor].genericName}: у магазина нет доступной формы поиска"
                cursor++
                advance()
            } else {
                web.postDelayed({
                    if (token == serial && state == State.SEARCH_RESULT)
                        readListing()
                }, 3200)
            }
        }
    }

    private fun readListing() {
        if (state !in setOf(State.LISTING, State.SEARCH_RESULT)) return
        state = State.IDLE
        serial++
        val token = serial
        web.evaluateJavascript(MagnitDomReader.SCRIPT) { response ->
            if (token != serial || state == State.FINISHED) return@evaluateJavascript
            val id = queries[cursor].ingredientId
            val products = MagnitDomReader.parseResult(response)
                .filter { candidate ->
                    val name = candidate.productName.lowercase().replace('ё', 'е')
                    val isCorrectCut = id != "chicken" ||
                        ("филе" in name && !listOf("бедр", "голен", "окороч",
                            "крыл", "тушка", "котлет", "наггет", "марин",
                            "копчен", "паниров").any { it in name })
                    RetailerProductMatcher.matchesIngredient(id, candidate.productName) &&
                        isCorrectCut && candidate.packGrams != null &&
                        candidate.priceKopeks > 0L
                }
                .distinctBy { it.productUrl }.take(12)
            if (products.isEmpty()) {
                failures += "${queries[cursor].genericName}: нет проверяемых товарных карточек"
                cursor++
                advance()
            } else {
                pending = products
                detailCursor = 0
                onProgress("${queries[cursor].genericName}: найдено ${pending.size} SKU; проверяю страницы производителей и составов")
                nextDetail()
            }
        }
    }

    private fun nextDetail() {
        serial++
        if (detailCursor >= pending.size) {
            cursor++
            advance()
            return
        }
        state = State.DETAIL
        web.loadUrl(pending[detailCursor].productUrl)
    }

    private fun readDetails() {
        if (state != State.DETAIL) return
        state = State.IDLE
        serial++
        val token = serial
        val product = pending[detailCursor]
        web.evaluateJavascript(MagnitProductDetailReader.SCRIPT) { response ->
            if (token != serial || state == State.FINISHED) return@evaluateJavascript
            val details = MagnitProductDetailReader.parseResult(response, product.productUrl)
            accumulated += MagnitProductDetailReader.enrich(
                product, details, "Магнит", city,
                System.currentTimeMillis(), addressConfirmed)
            detailCursor++
            nextDetail()
        }
    }
}
