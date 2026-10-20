package com.foodplan.ai

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
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
            return {title: (name ? name.textContent : a ? a.title : '').trim(),
                    url: a ? a.href : '', priceText: price ? price.textContent.trim() : ''};
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
                    if (name.isNotBlank() && url.startsWith("https://magnit.ru/product/") &&
                        price != null && price>0) {
                        add(ObservedProduct(name,url,CatalogValueParser.packageGrams(name),price))
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
    onCaptured: (RetailerOffer) -> Unit,
    onClose: () -> Unit
) {
    val ctx = LocalContext.current
    var browser by remember { mutableStateOf<WebView?>(null) }
    var scanned by remember { mutableStateOf<List<ObservedProduct>>(emptyList()) }
    var status by remember { mutableStateOf("Откройте категорию, укажите адрес доставки на сайте Магнита, затем считайте товары.") }
    var confirmed by remember { mutableStateOf(false) }
    var assigned by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    DisposableEffect(browser) { onDispose { browser?.destroy() } }
    Column(Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Каталог Магнита · $city", style = MaterialTheme.typography.titleLarge)
        Text("Считываются видимые товары открытой страницы. Цены могут отличаться в выбранном магазине и на кассе.",
            style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onClose) { Text("В корзину") }
            Button(onClick = {
                val w = browser
                if (w == null || w.url?.startsWith("https://magnit.ru/") != true) {
                    status = "Нет открытого каталога magnit.ru."; return@Button
                }
                w.evaluateJavascript(MagnitDomReader.SCRIPT) { json ->
                    val results = MagnitDomReader.parseResult(json)
                    scanned = results
                    status = if (results.isEmpty())
                        "Не удалось извлечь товары: страница пуста, недоступна или изменилась её структура."
                    else "Прочитано ${results.size} цен с открытой страницы. Для сохранения укажите товар из корзины."
                }
            }) { Text("Считать цены") }
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
                        TextButton(
                            enabled = confirmed && assigned.isNotEmpty() &&
                                product.packGrams != null &&
                                RetailerProductMatcher.matchesIngredient(assigned,product.productName),
                            onClick = {
                                val grams = product.packGrams ?: return@TextButton
                                onCaptured(RetailerOffer(
                                    assigned,"Магнит",city,product.productName,product.productUrl,
                                    grams,product.priceKopeks,System.currentTimeMillis(),true
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
