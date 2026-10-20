package com.foodplan.ai

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
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
import org.json.JSONTokener

/** Only clicks the visible retailer ADD button; never touches checkout or payment. */
object MagnitCartControl {
    const val CLICK_ONCE = """
        (function() {
            if (location.protocol !== 'https:' || location.hostname !== 'magnit.ru'
                || !location.pathname.startsWith('/product/')) return 'WRONG_PAGE';
            const preferred = document.querySelector('[data-test-id="v-product-detail-add-to-basket-button"]');
            const fallback = Array.from(document.querySelectorAll('button'))
               .find(e => /^(в корзину|добавить в корзину)$/i.test(e.textContent.trim()));
            const button = preferred || fallback;
            if (!button || button.disabled) return 'NO_AVAILABLE_ADD_BUTTON';
            button.click();
            return 'CLICK_ATTEMPTED_NOT_VERIFIED';
        })()
    """
    fun parseJavascriptResult(raw:String?): String =
        try { (JSONTokener(raw ?: "null").nextValue() as? String)
            ?.takeIf { it in setOf(
                "WRONG_PAGE","NO_AVAILABLE_ADD_BUTTON","CLICK_ATTEMPTED_NOT_VERIFIED"
            ) } ?: "INVALID_RESULT"
        } catch (_:Exception) { "INVALID_RESULT" }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MagnitCartHandoffScreen(
    shopping:List<ShoppingNeed>,
    offers:List<RetailerOffer>,
    city:String,
    onClose:()->Unit
) {
    val transfer = remember(shopping,offers,city) {
        StoreCartTransfer.build(shopping,offers,city,"Магнит",System.currentTimeMillis())
    }
    var position by remember(transfer) { mutableIntStateOf(0) }
    var browser by remember { mutableStateOf<WebView?>(null) }
    var currentHost by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Проверьте выбранный адрес доставки на сайте.") }
    var confirmedItems by remember(transfer) { mutableIntStateOf(0) }
    val ctx=LocalContext.current
    val queue=transfer.ready
    // No WebView is created when no eligible SKU was found.
    val current=queue.getOrNull(position)
    DisposableEffect(browser) {
        val view = browser
        onDispose { view?.destroy() }
    }
    Column(Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement=Arrangement.spacedBy(7.dp)) {
        Text("Корзина Магнита", style=MaterialTheme.typography.headlineSmall)
        Text("Один магазин, один адрес доставки · $city",
            style=MaterialTheme.typography.bodySmall)
        Text("Подобрано товаров: ${transfer.readyCount}; без цены или подходящего SKU: ${transfer.missing.size}.",
            style=MaterialTheme.typography.bodyMedium)
        Text("Оценка выбранных товаров: ${ManualBasketPrices.formatKopeks(transfer.estimatedTotalKopeks)} ₽. Реальная сумма определяется в корзине Магнита.",
            style=MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick=onClose) { Text("Назад к покупкам") }
        if (current == null) {
            Text(if(queue.isEmpty()) "Сначала найдите и сопоставьте продукты в каталоге Магнита."
                else "Все товары из подготовленного списка обработаны. Откройте корзину на сайте и проверьте состав заказа, количество и итоговую стоимость.",
                style=MaterialTheme.typography.bodyMedium)
            if (queue.isNotEmpty()) {
                Text("Проверены пользователем: $confirmedItems из ${queue.size}. Не найдено: ${transfer.missing.size}.",
                    style=MaterialTheme.typography.bodySmall)
            }
            if (transfer.missing.isNotEmpty()) {
                Text("Не найдены: ${transfer.missing.joinToString { OfflineRecipes.ingredients[it]?.name ?: it }}",
                    color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
            }
            return@Column
        }
        Text("Товар ${position+1} из ${queue.size}: ${current.productName}",
            style=MaterialTheme.typography.titleMedium)
        Text("Требуется ${current.units} уп. по ${current.packGrams} г. Цена каталога за эти упаковки: ${ManualBasketPrices.formatKopeks(current.expectedCostKopeks)} ₽",
            style=MaterialTheme.typography.bodySmall)
        Text("Сайт открыт: $currentHost",
            style=MaterialTheme.typography.bodySmall)
        Text(status,style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.error)
        // Create WebView only for explicitly prepared product pages.
        if (StoreCartTransfer.magnitProductUrlValid(current.productUrl)) {
            AndroidView(
                modifier=Modifier.fillMaxWidth().weight(1f),
                factory={
                    WebView(ctx).apply {
                        settings.javaScriptEnabled=true
                        settings.domStorageEnabled=true
                        settings.allowFileAccess=false
                        settings.allowContentAccess=false
                        webChromeClient=WebChromeClient()
                        webViewClient=object:WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view:WebView?, request:WebResourceRequest?
                            ):Boolean = request?.url?.scheme != "https"
                            override fun onPageFinished(view:WebView?,url:String?) {
                                currentHost=try { Uri.parse(url).host.orEmpty() } catch (_:Exception) { "" }
                            }
                        }
                        loadUrl(current.productUrl)
                        browser=this
                    }
                }
            )
        } else {
            Text("Некорректная ссылка товара. Автоматический переход заблокирован.")
        }
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            Button(
                enabled=currentHost=="magnit.ru" && isCurrentProduct(browser,current.productUrl),
                onClick={
                    val w=browser ?: return@Button
                    if (!isCurrentProduct(w,current.productUrl)) return@Button
                    w.evaluateJavascript(MagnitCartControl.CLICK_ONCE) { raw ->
                        status=when(MagnitCartControl.parseJavascriptResult(raw)) {
                            "CLICK_ATTEMPTED_NOT_VERIFIED" ->
                                "Нажатие на кнопку сайта отправлено. Проверьте на сайте, что товар появился и количество совпадает."
                            "NO_AVAILABLE_ADD_BUTTON" ->
                                "На странице нет доступной кнопки добавления. Используйте сам сайт."
                            "WRONG_PAGE" -> "Открыт не товар Магнита."
                            else -> "Невозможно подтвердить действие. Добавьте товар на сайте самостоятельно."
                        }
                    }
                }
            ) { Text("Нажать «В корзину»") }
        }
        Text("Кнопка делает только одно нажатие. Если нужно несколько упаковок, измените количество на сайте; автоматическая оплата не выполняется.",
            style=MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick={
            // Explicit user confirmation, not server-side proof.
            confirmedItems++
            position++
            status="Следующий товар. Проверьте адрес и наличие в каталоге."
            currentHost=""
            val next=queue.getOrNull(position)
            if (next!=null) browser?.loadUrl(next.productUrl)
        }) { Text("Я проверил количество на сайте · следующий") }
        Text("Важное: кнопка «Следующий» только фиксирует вашу проверку. Приложение пока не умеет независимо читать содержимое магазинной корзины.",
            style=MaterialTheme.typography.bodySmall)
    }
}

private fun isCurrentProduct(view: WebView?,expected:String):Boolean {
    val actual=view?.url ?: return false
    if (!StoreCartTransfer.magnitProductUrlValid(expected)) return false
    return try {
        val uri=Uri.parse(actual)
        uri.scheme=="https" && uri.host=="magnit.ru" &&
            uri.path == Uri.parse(expected).path
    } catch (_:Exception) { false }
}
