package com.foodplan.ai

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.webkit.WebResourceResponse
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.foodplan.domain.*
import org.json.JSONObject
import org.json.JSONTokener

/** Reads only public product-card content from the user-visible Magnit browser page. */
object MagnitDomReader {
    const val ENTRY_URL = "https://magnit.ru/catalog/64121"
    const val SCRIPT = """
        (function() {
          const cards = Array.from(document.querySelectorAll('article.unit-catalog-product-preview')).slice(0,120);
          const items = cards.map(function(node) {
            const a = node.querySelector('a[href*="/product/"]');
            const name = node.querySelector('.unit-catalog-product-preview-title');
            const price = node.querySelector('.unit-catalog-product-preview-prices__regular');
            const rating = node.querySelector('.unit-catalog-product-preview-rating-score');
            const reviews = node.querySelector('.unit-catalog-product-preview-rating-comments');
            return {title: (name ? name.textContent : a ? a.title : '').trim(),
                    url: a ? a.href : '', priceText: price ? price.textContent.trim() : '',
                    ratingText: rating ? rating.textContent.trim() : '',
                    reviewsText: reviews ? reviews.textContent.trim() : ''};
          }).filter(item => item.title && item.url && item.priceText);
          return JSON.stringify({page: location.href, items: items});
        })()
    """

    fun parseResult(jsResult: String?): List<ObservedProduct> {
        if (jsResult.isNullOrBlank() || jsResult == "null") return emptyList()
        return try {
            val jsonPayload = JSONTokener(jsResult).nextValue() as? String ?: return emptyList()
            val page = JSONObject(jsonPayload)
            if (!page.optString("page").startsWith("https://magnit.ru/")) return emptyList()
            val array = page.optJSONArray("items") ?: return emptyList()
            buildList {
                for (i in 0 until minOf(array.length(), 120)) {
                    val item = array.optJSONObject(i) ?: continue
                    val name = item.optString("title").trim().take(240)
                    val url = item.optString("url").trim()
                    val price = CatalogValueParser.parseDisplayedRubles(item.optString("priceText"))
                    val ratingText=item.optString("ratingText").trim().replace(',','.')
                    val stars=ratingText.toDoubleOrNull()?.takeIf { it in 0.0..5.0 && it.isFinite() }
                    val reviews=Regex("""(\d[\d\s]*)\s+отзы""").find(item.optString("reviewsText"))
                        ?.groupValues?.getOrNull(1)?.replace(" ","")?.toIntOrNull()
                    if (name.isNotBlank() && url.startsWith("https://magnit.ru/product/") &&
                        price != null && price>0) {
                        add(ObservedProduct(name,url,CatalogValueParser.packageGrams(name),price,
                            ListingAvailability.UNKNOWN,stars,reviews))
                    }
                }
            }.distinctBy { it.productUrl }
        } catch (_: Exception) { emptyList() }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MagnitCatalogBrowser(
    shopping: List<ShoppingNeed>,
    city: String,
    budgetRub: Long = 0L,
    onCaptured: (RetailerOffer) -> Unit,
    onClose: () -> Unit,
    strictAllergenVerification: Boolean = false
) {
    val ctx = LocalContext.current
    var browser by remember { mutableStateOf<WebView?>(null) }
    val genericQueries = remember(shopping) { AnonymousRetailPlanner.build(shopping) }
    var automaticScan by remember { mutableStateOf<MagnitScanCoordinator?>(null) }
    var autoEvidence by remember { mutableStateOf<List<RetailProductEvidence>>(emptyList()) }
    var autoFailures by remember { mutableStateOf<List<String>>(emptyList()) }
    var autoMessage by remember { mutableStateOf("") }
    var autoRunning by remember { mutableStateOf(false) }
    var autoFinished by remember { mutableStateOf(false) }
    var submittedEstimates by remember { mutableIntStateOf(0) }
    var scanned by remember { mutableStateOf<List<ObservedProduct>>(emptyList()) }
    var status by remember { mutableStateOf("Откройте категорию, укажите адрес доставки на сайте Магнита, затем считайте товары.") }
    var confirmed by remember { mutableStateOf(false) }
    var assigned by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    var proposed by remember { mutableStateOf<List<SuggestedRetailerProduct>>(emptyList()) }
    var variants by remember { mutableStateOf<List<SuggestedRetailerProduct>>(emptyList()) }
    DisposableEffect(browser) {
        val viewToRelease = browser
        onDispose { viewToRelease?.destroy() }
    }
    Column(Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Каталог Магнита · $city", style = MaterialTheme.typography.titleLarge)
        Text("Считываются видимые товары открытой страницы. Цены могут отличаться в выбранном магазине и на кассе.",
            style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onClose) { Text("К моему списку") }
            Button(onClick = {
                val w = browser
                if (w == null || w.url?.startsWith("https://magnit.ru/") != true) {
                    status = "Нет открытого каталога magnit.ru."; return@Button
                }
                w.evaluateJavascript(MagnitDomReader.SCRIPT) { json ->
                    val results = MagnitDomReader.parseResult(json)
                    scanned = results
                    proposed = emptyList()
                    variants = emptyList()
                    status = if (results.isEmpty())
                        "Не удалось извлечь товары: страница пуста, недоступна или изменилась её структура."
                    else "Прочитано ${results.size} цен с открытой страницы. Для сохранения укажите товар из корзины."
                }
            }) { Text("Считать цены") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
                enabled = confirmed && !autoRunning && genericQueries.isNotEmpty() && browser != null,
                onClick = {
                    val view = browser ?: return@Button
                    autoEvidence = emptyList()
                    autoFailures = emptyList()
                    autoFinished = false
                    submittedEstimates = 0
                    autoRunning = true
                    val scan = MagnitScanCoordinator(
                        view, genericQueries, city, true,
                        onProgress = { autoMessage = it },
                        onCompleted = { evidence, failures ->
                            autoEvidence = evidence
                            autoFailures = failures
                            autoRunning = false
                            autoFinished = true
                            automaticScan = null
                            // Normal shopping is one action, not 37 individual
                            // "save product" taps. Only deterministic, comparable
                            // public-card estimates can enter the ordinary basket.
                            // Unknown local stock and physical labels remain unknown.
                            if (!strictAllergenVerification && budgetRub in 100L..100_000L) {
                                val quote = RetailWeekQuoter.build(
                                    genericQueries, evidence, "Магнит", city,
                                    budgetRub, System.currentTimeMillis()
                                )
                                var submitted = 0
                                quote.lines.forEach { line ->
                                    val url = line.skuUrl ?: return@forEach
                                    val evidenceRow = evidence.firstOrNull {
                                        it.product.productUrl == url
                                    } ?: return@forEach
                                    val pack = evidenceRow.product.packGrams ?: return@forEach
                                    onCaptured(RetailerOffer(
                                        line.ingredientId, "Магнит", city,
                                        evidenceRow.product.productName,
                                        evidenceRow.product.productUrl, pack,
                                        evidenceRow.product.priceKopeks,
                                        evidenceRow.observedAtEpochMs, true,
                                        evidenceRow.product.availability,
                                        evidenceRow.product.ratingStars,
                                        evidenceRow.product.reviewCount
                                    ))
                                    submitted++
                                }
                                submittedEstimates = submitted
                            }
                        }
                    )
                    automaticScan = scan
                    scan.begin()
                }
            ) { Text("Автопрогон списка") }
            if (autoRunning) {
                OutlinedButton(onClick = {
                    automaticScan?.cancel()
                    automaticScan = null
                    autoRunning = false
                }) { Text("Стоп") }
            }
        }
        if (autoMessage.isNotEmpty())
            Text(autoMessage, style = MaterialTheme.typography.bodySmall)
        if (autoFinished && submittedEstimates > 0)
            Text("Передано в продуктовый список: ${submittedEstimates} " +
                "предварительных цен. Пропущенные товары остались неизвестными.",
                style = MaterialTheme.typography.bodySmall)
        if (autoFinished && strictAllergenVerification)
            Text("При аллергии публикации магазина не заменяют проверку " +
                "этикетки конкретной упаковки. Автосохранение товаров отключено.",
                style = MaterialTheme.typography.bodySmall)
        if (autoFinished) {
            val coverage = RetailScanCoverage.assess(
                genericQueries, autoEvidence, "Магнит", city, System.currentTimeMillis())
            Text("Продуктовый список: найдено ${coverage.ingredientsWithObservedSku} из " +
                "${coverage.requiredIngredients} продуктов. По опубликованным характеристикам " +
                "сопоставимы ${coverage.ingredientsWithComparablePublishedSku}. " +
                "Наличие по адресу и итоговая цена всей корзины не подтверждены.",
                style = MaterialTheme.typography.bodyMedium)
            Text("Сопоставление без ИИ: только опубликованные карточки магазина. " +
                "Аллергены и физические упаковки требуют отдельной проверки.",
                style = MaterialTheme.typography.bodySmall)
            if (budgetRub in 100L..100_000L) {
                val quotation = RetailWeekQuoter.build(
                    genericQueries, autoEvidence, "Магнит", city,
                    budgetRub, System.currentTimeMillis(), strictAllergenVerification
                )
                val displayedTotal = quotation.completeEstimateKopeks?.let {
                    ManualBasketPrices.formatKopeks(it) + " ₽"
                } ?: "неизвестна, список неполный"
                val budgetStatus = when (quotation.estimatedWithinBudget) {
                    true -> "По опубликованным ценам укладывается в бюджет"
                    false -> "По опубликованным ценам превышает бюджет"
                    null -> "Соответствие бюджету неизвестно"
                }
                Text("Продукты с ценой: ${quotation.pricedIngredientCount} из " +
                    "${quotation.requiredIngredientCount}. Ориентировочная сумма недели: " +
                    displayedTotal + ". " + budgetStatus + ".",
                    style = MaterialTheme.typography.bodyMedium)
                if (quotation.missingIngredientIds.isNotEmpty()) {
                    Text("Без подтверждённой оценки: " +
                        quotation.lines.filter { it.estimatedCostKopeks == null }
                            .joinToString { it.ingredientName } + ".",
                        style = MaterialTheme.typography.bodySmall)
                }
                Text("Даже полный список опубликованных цен не подтверждает " +
                    "наличие по адресу, цену на кассе или безопасность при аллергии.",
                    style = MaterialTheme.typography.bodySmall)
            }
            Column(
                Modifier.fillMaxWidth().heightIn(max = 260.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                genericQueries.forEach { need ->
                    val evidence = autoEvidence.filter {
                        RetailerProductMatcher.matchesIngredient(
                            need.ingredientId, it.product.productName)
                    }
                    val result = DeterministicRetailAuditor.audit(
                        need, evidence, "Магнит", city, System.currentTimeMillis()
                    )
                    Text("${need.genericName}: ${result.candidates.size} найдено, " +
                        "${result.candidates.count { it.status == RetailAuditStatus.COMPARABLE }} сопоставимо",
                        style = MaterialTheme.typography.bodySmall)
                    val cheapest = result.cheapestComparable
                    if (cheapest != null) {
                        val p = cheapest.evidence
                        Text("${p.product.productName} · изготовитель ${p.manufacturer} · " +
                            "${ManualBasketPrices.formatKopeks(cheapest.weeklyCostKopeks ?: 0L)} ₽ за нужные упаковки · " +
                            "${"%.2f".format(cheapest.rublesPerKg ?: 0.0)} ₽/кг. " +
                            result.explanation,
                            style = MaterialTheme.typography.bodySmall)
                        TextButton(
                            enabled = !strictAllergenVerification,
                            onClick = {
                            val grams = p.product.packGrams ?: return@TextButton
                            onCaptured(RetailerOffer(need.ingredientId, "Магнит", city,
                                p.product.productName, p.product.productUrl, grams,
                                p.product.priceKopeks, System.currentTimeMillis(),
                                true, p.product.availability, p.product.ratingStars,
                                p.product.reviewCount))
                        }) { Text("Сохранить оценку цены этого товара") }
                    } else {
                        Text(result.explanation, style = MaterialTheme.typography.bodySmall)
                    }
                }
                autoFailures.forEach { Text("Не проверено: $it",
                    style = MaterialTheme.typography.bodySmall) }
            }
        }
        Text(status, style = MaterialTheme.typography.bodySmall)
        AndroidView(
            modifier = Modifier.fillMaxWidth().weight(1f),
            factory = {
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    webChromeClient = WebChromeClient()
                    webViewClient = object: WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            automaticScan?.onPageFinished(url)
                        }
                        override fun onReceivedError(
                            view: WebView?, request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            if (request?.isForMainFrame == true)
                                automaticScan?.onMainFrameError(
                                    request.url.toString(), "сеть или загрузка страницы")
                        }
                        override fun onReceivedHttpError(
                            view: WebView?, request: WebResourceRequest?,
                            errorResponse: WebResourceResponse?
                        ) {
                            super.onReceivedHttpError(view, request, errorResponse)
                            if (request?.isForMainFrame == true &&
                                (errorResponse?.statusCode ?: 0) >= 400)
                                automaticScan?.onMainFrameError(
                                    request.url.toString(), "HTTP ${errorResponse?.statusCode}")
                        }
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val uri = request?.url ?: return true
                            return !(uri.scheme == "https" &&
                                uri.host in setOf("magnit.ru","www.magnit.ru"))
                        }
                    }
                    loadUrl(MagnitDomReader.ENTRY_URL)
                    browser = this
                }
            }
        )
        Row {
            Checkbox(checked=confirmed, onCheckedChange={ confirmed=it })
            Text("Я выбрал на сайте адрес доставки для $city",
                style = MaterialTheme.typography.bodySmall)
        }
        if (scanned.isNotEmpty()) {
            Button(onClick = {
                variants=CatalogCandidateSelector.allMatchingVariants(shopping,scanned)
                proposed=CatalogCandidateSelector.suggestForBudget(
                    shopping,scanned,city,budgetRub,System.currentTimeMillis())
                status="Подобрано: ${proposed.size} из ${shopping.count { it.grams>0 }}. Бюджет ${budgetRub} ₽ на всю неделю; неполный каталог не считается готовой корзиной."
            }) { Text("Подобрать варианты под бюджет") }
            Text("Найдено альтернативных SKU: ${variants.size}. Бюджетный подбор " +
                "сможет сравнить их после сохранения. Это предложения только с открытой страницы.",
                style=MaterialTheme.typography.bodySmall)
            if (variants.isNotEmpty()) {
                Button(
                    enabled=confirmed && !strictAllergenVerification,
                    onClick={
                        val saved=variants.toList()
                        val capturedAt=System.currentTimeMillis()
                        saved.forEach { candidate ->
                            val product=candidate.product
                            onCaptured(RetailerOffer(
                                candidate.ingredientId,"Магнит",city,
                                product.productName,product.productUrl,
                                requireNotNull(product.packGrams),
                                product.priceKopeks,capturedAt,true,
                                product.availability,product.ratingStars,product.reviewCount
                            ))
                        }
                        status="Внутри FoodPlan сохранено ${saved.size} вариантов. " +
                            "Теперь можно сравнить их по бюджету и отзывам."
                        variants=emptyList()
                        proposed=emptyList()
                    }
                ) { Text("Сохранить все подходящие варианты") }
            }
            if (proposed.isNotEmpty()) {
                Text("Варианты учтены по общей недельной сумме и известным оценкам покупателей. Отсутствие отзывов не считается плохим качеством. Наличие по адресу пока не подтверждено.",
                    style=MaterialTheme.typography.bodySmall)
                Button(
                    enabled=confirmed && !strictAllergenVerification &&
                        proposed.all { it.product.packGrams != null },
                    onClick={
                        val accepted=proposed.toList()
                        accepted.forEach { choice ->
                            onCaptured(RetailerOffer(
                                choice.ingredientId,"Магнит",city,
                                choice.product.productName,choice.product.productUrl,
                                requireNotNull(choice.product.packGrams),
                                choice.product.priceKopeks,System.currentTimeMillis(),true,
                                choice.product.availability,choice.product.ratingStars,
                                choice.product.reviewCount
                            ))
                        }
                        proposed=emptyList()
                        status="Сохранено ${accepted.size} товарных предложений. Возвращайтесь в корзину, чтобы проверить список."
                    }
                ) { Text("Подтвердить весь подобранный список") }
                Column(
                    Modifier.fillMaxWidth().heightIn(max=165.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    proposed.forEach { candidate ->
                        val product=candidate.product
                        Text(
                            "${shopping.firstOrNull { it.ingredient.id==candidate.ingredientId }?.ingredient?.name} → " +
                            "${product.productName} · ${candidate.requiredPacks} уп. · " +
                            "${ManualBasketPrices.formatKopeks(candidate.approximateCostKopeks)} ₽ · " +
                            (product.ratingStars?.let { "★ $it (" + (product.reviewCount?.let { n -> "$n отзывов" } ?: "количество отзывов неизвестно") + ")" } ?: "Нет данных о рейтинге"),
                            style=MaterialTheme.typography.bodySmall
                        )
                        TextButton(
                            enabled=confirmed && !strictAllergenVerification &&
                                product.packGrams != null,
                            onClick={
                                onCaptured(RetailerOffer(
                                    candidate.ingredientId,"Магнит",city,
                                    product.productName,product.productUrl,
                                    requireNotNull(product.packGrams),
                                    product.priceKopeks,System.currentTimeMillis(),true,
                                    product.availability,product.ratingStars,product.reviewCount
                                ))
                                proposed=proposed.filterNot { it.ingredientId==candidate.ingredientId }
                                status="Подтверждено: ${product.productName}. Запись появится в корзине."
                            }
                        ) { Text("Использовать этот товар") }
                    }
                }
            }
            Box {
                OutlinedButton(onClick = { menuOpen = true }) {
                    Text(shopping.firstOrNull { it.ingredient.id==assigned }?.ingredient?.name ?:
                        "Какой продукт из корзины сопоставить?")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest={ menuOpen=false }) {
                    shopping.filter { it.grams>0 }.forEach { need ->
                        DropdownMenuItem(
                            text = { Text(need.ingredient.name) },
                            onClick = { assigned=need.ingredient.id;menuOpen=false }
                        )
                    }
                }
            }
            Text("Кнопка привязки активна только для товара, название которого совпадает с выбранным ингредиентом.",
                style = MaterialTheme.typography.bodySmall)
            Column(Modifier.fillMaxWidth().heightIn(max=180.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(5.dp)) {
                scanned.forEach { product ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(product.productName, style = MaterialTheme.typography.bodySmall)
                            Text("${ManualBasketPrices.formatKopeks(product.priceKopeks)} ₽ · ${product.packGrams?.let { "$it г" } ?: "вес не определён"}",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = {
                            if(product.productUrl.startsWith("https://magnit.ru/product/")) {
                                browser?.loadUrl(product.productUrl)
                                status = "Открыта карточка товара на сайте Магнита. " +
                                    "Там можно посмотреть реальные отзывы и подробный состав. " +
                                    "Ничего не добавляется в корзину магазина."
                            }
                        }) { Text("Карточка и отзывы") }
                        TextButton(
                            enabled = confirmed && !strictAllergenVerification &&
                                assigned.isNotEmpty() &&
                                product.packGrams != null &&
                                RetailerProductMatcher.matchesIngredient(assigned,product.productName),
                            onClick = {
                                val grams = product.packGrams ?: return@TextButton
                                onCaptured(RetailerOffer(
                                    assigned,"Магнит",city,product.productName,product.productUrl,
                                    grams,product.priceKopeks,System.currentTimeMillis(),true,
                                    product.availability,product.ratingStars,product.reviewCount
                                ))
                                status = "Добавлена цена: ${product.productName}. Можно сопоставить следующий продукт."
                            }
                        ) { Text("Связать") }
                    }
                }
            }
        }
    }
}
