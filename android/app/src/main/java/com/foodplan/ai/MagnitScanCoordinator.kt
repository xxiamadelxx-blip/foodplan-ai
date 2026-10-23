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
    private enum class State { IDLE, OPEN_SEARCH, SEARCH_RESULT, LISTING, READING_LISTING, DIRECT_PRODUCT, READING_DIRECT_PRODUCT, DETAIL, READING_DETAIL, FINISHED }
    private var state = State.IDLE
    private var cursor = 0
    private var pending = emptyList<ObservedProduct>()
    private var detailCursor = 0
    private val accumulated = mutableListOf<RetailProductEvidence>()
    private val failures = mutableListOf<String>()
    private var consecutiveUnavailable = 0
    private val maxConsecutiveUnavailable = 3

    private fun recordFailure(message: String) {
        failures += message
        consecutiveUnavailable++
    }

    private fun recordPriceObserved() {
        consecutiveUnavailable = 0
    }

    private fun stopAfterHostFailure(message: String) {
        if (state == State.FINISHED) return
        failures += message
        state = State.FINISHED
        serial++
        onProgress("Магазин ограничил доступ. Непроверенные цены неизвестны.")
        onCompleted(accumulated.toList(), failures.toList())
    }

    private var serial = 0
    private var expectedNavigationUrl: String? = null
    // These unusual suffixes are genuine, independently checked retailer
    // category URLs, NOT fixture paths (verified 2026-10-22).
    private val verifiedCategories = mapOf(
        "chicken" to "https://magnit.ru/catalog/64291-testmmkuritsa",
        "rice" to "https://magnit.ru/catalog/64143-testmmris",
        "buckwheat" to "https://magnit.ru/catalog/64139-testmmgrechka",
        "pasta" to "https://magnit.ru/catalog/114259-makarony_copy",
        // One public category feeds multiple vegetables. Cache the rendered
        // listing once instead of repeating a network request per ingredient.
        "potato" to "https://magnit.ru/catalog/63905-testmmovoshchi_i_frukty",
        "carrot" to "https://magnit.ru/catalog/63905-testmmovoshchi_i_frukty",
        "onion" to "https://magnit.ru/catalog/63905-testmmovoshchi_i_frukty",
        "banana" to "https://magnit.ru/catalog/63905-testmmovoshchi_i_frukty",
        "cucumber" to "https://magnit.ru/catalog/63905-testmmovoshchi_i_frukty",
        "tomato" to "https://magnit.ru/catalog/63905-testmmovoshchi_i_frukty"
    )
    private val publicListingCache = mutableMapOf<String, List<ObservedProduct>>()
    // Checked official URLs are discovery entrypoints, NEVER bundled prices.
    // Actual observed price is always parsed from the fresh visible product
    // page. No store address, special retailer account, or invented offers.
    // All known official paths are part of the installed APK; real prices
    // NEVER are. If a known SKU is inaccessible, the need stays unpriced.
    private val checkedDirectSku = MagnitKnownSkuCatalog.urls

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
                recordFailure("${queries[expectedQuery].genericName}: таймаут $stage" +
                    (failedSku?.let { " [$it]" } ?: ""))
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
        if (state !in setOf(State.OPEN_SEARCH, State.LISTING, State.DIRECT_PRODUCT,
                State.DETAIL) || url != expectedNavigationUrl) return
        if (reason.contains("HTTP 403") || reason.contains("HTTP 429") ||
            reason.contains("HTTP 401")) {
            stopAfterHostFailure("Доступ к сайту ограничен ($reason)")
            return
        }
        val ingredient = queries.getOrNull(cursor)?.genericName ?: return
        recordFailure("$ingredient: ошибка страницы ($reason)" +
            (if (state == State.DETAIL) " [$url]" else ""))
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
        if (state in setOf(State.OPEN_SEARCH, State.LISTING, State.DIRECT_PRODUCT,
                State.DETAIL) &&
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
            State.DIRECT_PRODUCT -> web.postDelayed({
                if (state == State.DIRECT_PRODUCT && token == serial)
                    readDirectPrice()
            }, 1300)
            State.DETAIL -> web.postDelayed({
                if (state == State.DETAIL && token == serial) readDetails()
            }, 1300)
            else -> Unit
        }
    }

    private fun advance() {
        serial++
        if (consecutiveUnavailable >= maxConsecutiveUnavailable) {
            stopAfterHostFailure("Несколько страниц подряд недоступны; остальные цены неизвестны")
            return
        }
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
        val directUrl = checkedDirectSku[query.ingredientId]
        if (directUrl != null) {
            state = State.DIRECT_PRODUCT
            expectedNavigationUrl = directUrl
            armTimeout("загрузки публичной карточки")
            web.loadUrl(directUrl)
            return
        }
        val category = verifiedCategories[query.ingredientId]
        if (category != null) {
            val cached = publicListingCache[category]
            if (cached != null) {
                acceptPriceListings(cached)
                return
            }
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
                recordFailure("${queries[cursor].genericName}: у магазина нет доступной формы поиска")
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
        val cachedUrl = if (state == State.LISTING) expectedNavigationUrl else null
        state = State.READING_LISTING
        serial++
        val token = serial
        armTimeout("чтения списка товаров")
        web.evaluateJavascript(MagnitDomReader.SCRIPT) { response ->
            if (token != serial || state != State.READING_LISTING) return@evaluateJavascript
            val products = MagnitDomReader.parseResult(response)
            if (cachedUrl != null) publicListingCache[cachedUrl] = products
            acceptPriceListings(products)
        }
    }

    private fun acceptPriceListings(listing: List<ObservedProduct>) {
        val id = queries[cursor].ingredientId
        val products = listing.filter { candidate ->
            val name = candidate.productName.lowercase().replace('ё', 'е')
            val correctCut = id != "chicken" ||
                ("филе" in name && "грудк" in name &&
                    !listOf("бедр", "голен", "окороч", "крыл", "тушка",
                            "котлет", "наггет", "марин", "копчен", "паниров")
                        .any { it in name })
            val officialUnit = MagnitPackOnlySkuCatalog.matching(candidate)
            val exactPriceOnly = officialUnit?.ingredientId == id
            val permittedMass = MagnitPackOnlySkuCatalog.forUrl(
                candidate.productUrl) == null &&
                RetailerProductMatcher.matchesIngredient(
                    id, candidate.productName) && candidate.packGrams != null
            correctCut && (exactPriceOnly || permittedMass) &&
                candidate.priceKopeks > 0L
        }.distinctBy { it.productUrl }.take(40)
        if (products.isEmpty()) {
            recordFailure("${queries[cursor].genericName}: нет цен в открытом каталоге")
        } else {
            val now = System.currentTimeMillis()
            recordPriceObserved()
            accumulated += products.map { product ->
                RetailProductEvidence(product, "Магнит", city, now, false)
            }
            onProgress("${queries[cursor].genericName}: найдено ${products.size} ориентировочных цен")
        }
        cursor++
        advance()
    }

    private fun readDirectPrice() {
        if (state != State.DIRECT_PRODUCT) return
        val expected = expectedNavigationUrl ?: return
        val ingredient = queries.getOrNull(cursor) ?: return
        state = State.READING_DIRECT_PRODUCT
        serial++
        val token = serial
        armTimeout("чтения открытого ценника")
        web.evaluateJavascript(MagnitPublicSkuPriceReader.SCRIPT) { response ->
            if (state != State.READING_DIRECT_PRODUCT || serial != token) {
                return@evaluateJavascript
            }
            val observed = MagnitPublicSkuPriceReader.parseResult(response, expected)
            val matchesOfficialFixedSku = observed?.let { product ->
                MagnitPackOnlySkuCatalog.matching(product)?.ingredientId ==
                    ingredient.ingredientId
            } ?: false
            if (observed == null || !(matchesOfficialFixedSku ||
                    RetailerProductMatcher.matchesIngredient(
                        ingredient.ingredientId, observed.productName))) {
                recordFailure("${ingredient.genericName}: публичная карточка не подтвердила цену и фасовку")
            } else {
                recordPriceObserved()
                accumulated += RetailProductEvidence(
                    observed, "Магнит", city, System.currentTimeMillis(), false
                )
                onProgress("${ingredient.genericName}: прочитан примерный ценник сети")
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
                recordFailure("${queries[cursor].genericName}: детали SKU не подтверждены [${product.productUrl}]")
            accumulated += MagnitProductDetailReader.enrich(
                product, details, "Магнит", city,
                System.currentTimeMillis(), false)
            detailCursor++
            nextDetail()
        }
    }
}
