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
    private val onProgress: (String) -> Unit,
    private val onCompleted: (List<RetailProductEvidence>, List<String>) -> Unit
) {
    private enum class State { IDLE, OPEN_SEARCH, SEARCH_RESULT, LISTING, READING_LISTING, DETAIL, READING_DETAIL, FINISHED }
    private var state = State.IDLE
    private var cursor = 0
    private var pending = emptyList<ObservedProduct>()
    private var detailCursor = 0
    private val accumulated = mutableListOf<RetailProductEvidence>()
    private val failures = mutableListOf<String>()
    private var serial = 0
    private var expectedNavigationUrl: String? = null
    // These unusual suffixes are genuine, independently checked retailer
    // category URLs, NOT fixture paths (verified 2026-10-22).
    private val verifiedCategories = mapOf(
        "chicken" to "https://magnit.ru/catalog/64291-testmmkuritsa",
        "rice" to "https://magnit.ru/catalog/64143-testmmris",
        "buckwheat" to "https://magnit.ru/catalog/64139-testmmgrechka"
    )

    /** Bound every external navigation and WebView JavaScript callback. A site
     * timeout invalidates the old callback before moving to the next query.
     * Missing pages never become confirmed prices or product identities.
     */
    private fun armTimeout(stage: String) {
        val token = serial
        val expectedState = state
        val expectedQuery = cursor
        web.postDelayed({
            if (state == expectedState && token == serial &&
                cursor == expectedQuery && state != State.FINISHED) {
                val failedSku = if (expectedState == State.DETAIL ||
                    expectedState == State.READING_DETAIL)
                    pending.getOrNull(detailCursor)?.productUrl else null
                failures += "${queries[expectedQuery].genericName}: таймаут $stage" +
                    (failedSku?.let { " [$it]" } ?: "")
                if (failedSku == null) {
                    cursor++
                    advance()
                } else {
                    detailCursor++
                    nextDetail()
                }
            }
        }, 25_000L)
    }

    fun begin() {
        if (state != State.IDLE) return
        advance()
    }

    fun cancel() {
        state = State.FINISHED
        serial++
        onProgress("Автопрогон остановлен пользователем; незавершённые товары не проверены")
    }

    /** Main-frame transport failures only. Never confuse an image/font error
     * with the retailer product page itself failing to load.
     */
    fun onMainFrameError(url: String?, reason: String) {
        if (state !in setOf(State.OPEN_SEARCH, State.LISTING, State.DETAIL) ||
            url != expectedNavigationUrl) return
        val ingredient = queries.getOrNull(cursor)?.genericName ?: return
        failures += "$ingredient: ошибка страницы ($reason)" +
            (if (state == State.DETAIL) " [$url]" else "")
        if (state == State.DETAIL) {
            detailCursor++
            nextDetail()
        } else {
            cursor++
            advance()
        }
    }

    fun onPageFinished(url: String?) {
        if (state == State.FINISHED || !url.orEmpty().startsWith("https://magnit.ru/")) return
        if (state in setOf(State.OPEN_SEARCH, State.LISTING, State.DETAIL) &&
            url?.substringBefore('?') != expectedNavigationUrl?.substringBefore('?')) return
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
            onProgress("Проверено ${queries.size} продуктов. " +
                "Найдено ${accumulated.size} публичных цен, " +
                "без цен или с ошибкой: ${failures.size}.")
            onCompleted(accumulated.toList(), failures.toList())
            return
        }
        pending = emptyList()
        detailCursor = 0
        val query = queries[cursor]
        onProgress("Поиск ${cursor + 1}/${queries.size}: ${query.genericName}")
        val category = verifiedCategories[query.ingredientId]
        if (category != null) {
            state = State.LISTING
            expectedNavigationUrl = category
            armTimeout("загрузки каталога")
            web.loadUrl(category)
        } else {
            state = State.OPEN_SEARCH
            expectedNavigationUrl = "https://magnit.ru/"
            armTimeout("открытия поиска")
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
        expectedNavigationUrl = null
        serial++
        val token = serial
        armTimeout("отправки поискового запроса")
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
        state = State.READING_LISTING
        serial++
        val token = serial
        armTimeout("чтения списка товаров")
        web.evaluateJavascript(MagnitDomReader.SCRIPT) { response ->
            if (token != serial || state != State.READING_LISTING) return@evaluateJavascript
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
                .distinctBy { it.productUrl }.take(40)
            if (products.isEmpty()) {
                failures += "${queries[cursor].genericName}: нет цен в открытом каталоге"
            } else {
                // Price-only M4: a public listing title + packaged mass +
                // visible current price is sufficient. Do NOT request up to
                // 12 separate detail pages to establish manufacturer or
                // food-label equivalence. Safety is a separate strict gate.
                val now = System.currentTimeMillis()
                accumulated += products.map { product ->
                    RetailProductEvidence(product, "Магнит", city, now, false)
                }
                onProgress("${queries[cursor].genericName}: найдено ${products.size} ориентировочных цен")
            }
            cursor++
            advance()
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
        expectedNavigationUrl = pending[detailCursor].productUrl
        armTimeout("загрузки карточки товара")
        web.loadUrl(expectedNavigationUrl!!)
    }

    private fun readDetails() {
        if (state != State.DETAIL) return
        state = State.READING_DETAIL
        serial++
        val token = serial
        armTimeout("чтения состава товара")
        val product = pending[detailCursor]
        web.evaluateJavascript(MagnitProductDetailReader.SCRIPT) { response ->
            if (token != serial || state != State.READING_DETAIL) return@evaluateJavascript
            val details = MagnitProductDetailReader.parseResult(response, product.productUrl)
            if (details == null)
                failures += "${queries[cursor].genericName}: детали SKU не подтверждены [${product.productUrl}]"
            accumulated += MagnitProductDetailReader.enrich(
                product, details, "Магнит", city,
                System.currentTimeMillis(), false)
            detailCursor++
            nextDetail()
        }
    }
}
