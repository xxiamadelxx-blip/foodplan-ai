package com.foodplan.ai

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class MagnitBrowserDomTest {
    @Test fun browserExtractsPriceAndPackSize() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val done = CountDownLatch(1)
        var result: String? = null
        var browser: WebView? = null
        instrumentation.runOnMainSync {
            browser = WebView(instrumentation.targetContext).apply {
                settings.javaScriptEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        view?.evaluateJavascript(MagnitDomReader.SCRIPT) {
                            result = it
                            done.countDown()
                        }
                    }
                }
                loadDataWithBaseURL("https://magnit.ru/catalog/64121",
                    """<html><body>
                    <article class="unit-catalog-product-preview">
                    <a href="https://magnit.ru/product/3132360213-soda_pishchevaya_500g"></a>
                    <span class="unit-catalog-product-preview-prices__regular">89,99 ₽</span>
                    <span class="unit-catalog-product-preview-title">Сода пищевая 500г</span>
                    <span class="unit-catalog-product-preview-rating-score">4.7</span>
                    <span class="unit-catalog-product-preview-rating-comments">· 150 отзывов</span>
                    </article></body></html>""","text/html","UTF-8",null)
            }
        }
        try {
            assertTrue(done.await(30,TimeUnit.SECONDS))
            val products = MagnitDomReader.parseResult(result)
            assertEquals(1, products.size)
            assertEquals(8999L,products.single().priceKopeks)
            assertEquals(500, products.single().packGrams)
        } finally {
            instrumentation.runOnMainSync { browser?.destroy() }
        }
    }
}
